package org.wcs.smart.upgrade.v112;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.internal.filter.AttributeFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.AttributeGroupBy;
import org.wcs.smart.query.parser.internal.summary.AttributeValueItem;
import org.wcs.smart.query.parser.internal.summary.CombinedValueItem;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.parser.internal.summary.SumQueryDefinition;

public class AttributeTreeItemProcessor implements IDataProcessor{

	public void fixAttributeTreeItems(Connection c) throws Exception{
		
		//read all attribute keys from the database and store in temporary structure 
		//that links ca to attributes
		
		
		HashMap<ByteWrapper, List<KeyItem>> items = new HashMap<ByteWrapper, List<KeyItem>>();
		HashMap<ByteWrapper, List<KeyItem>> attributeItems = new HashMap<ByteWrapper, List<KeyItem>>();

		//group by attribute tree level so keys are unique across tree level
		String sql = "SELECT a.attribute_uuid, " +
				"case when a.parent_uuid is null then a.attribute_uuid else a.parent_uuid end, " +
				"a.uuid, a.keyid, a.parent_uuid, a.hkey, c.ca_uuid " +
				"FROM smart.dm_attribute_tree a JOIN smart.dm_attribute c on c.uuid = a.attribute_uuid ";
		ResultSet rs = c.createStatement().executeQuery(sql);
		while(rs.next()){
			byte[] attributeUuid = rs.getBytes(1);
			byte[] groupByUuuid = rs.getBytes(2);
			byte[] uuid = rs.getBytes(3);
			String key = rs.getString(4);
			byte[] parentUuid = rs.getBytes(5);
			String hkey = rs.getString(6);
			byte[] cauuid = rs.getBytes(7);

			
			ByteWrapper ca = new ByteWrapper(groupByUuuid);
			List<KeyItem> its = items.get(ca);
			if (its == null){
				its = new ArrayList<KeyItem>();
				items.put(ca,  its);
			}
			KeyItem keyItem  = null;
			for (KeyItem it : its){
				if (Arrays.equals(uuid, it.getUuid())){
					keyItem = it;
				}
			}
			if (keyItem == null){
				keyItem = new KeyItem(uuid, key, cauuid, parentUuid, hkey);
				its.add(keyItem);
			}
			
			ByteWrapper caBw = new ByteWrapper(attributeUuid);
			List<KeyItem> caits = attributeItems.get(caBw);
			if (caits == null){
				caits = new ArrayList<KeyItem>();
				attributeItems.put(caBw,  caits);
			}
			caits.add(keyItem);
		}
		rs.close();
		
		FixDataModel.fixItems(items);
		
		///update keys and names as required
		PreparedStatement ps = c.prepareStatement("UPDATE smart.dm_attribute_tree set keyid = ? WHERE uuid = ?");
		PreparedStatement psname = c.prepareStatement("UPDATE smart.i18n_label set value = ? WHERE language_uuid = ? and element_uuid = ?");
				
				
		for (Iterator<List<KeyItem>> iterator = attributeItems.values().iterator(); iterator.hasNext();) {
			List<KeyItem> keys = (List<KeyItem>) iterator.next();
					
			HashMap<ByteWrapper, KeyItem> uuidToItem = new HashMap<ByteWrapper, KeyItem>();
			for (KeyItem k : keys) {
				uuidToItem.put(new ByteWrapper(k.getUuid()), k);	
			}
					
			HashMap<ByteWrapper, ArrayList<KeyItem>> kids = new HashMap<ByteWrapper, ArrayList<KeyItem>>();
			for (KeyItem k : keys) {
				if (k.getParentItem() != null){
					KeyItem parentItem = uuidToItem.get(new ByteWrapper(k.getParentItem()));
					ArrayList<KeyItem> ks = kids.get(new ByteWrapper(parentItem.getUuid()));
					if (ks == null){
						ks = new ArrayList<KeyItem>();
						kids.put(new ByteWrapper(parentItem.getUuid()), ks);
					}
					ks.add(k);
				}
					
			}
					
			for (KeyItem k : keys){
				if (k.isChanged()){
					//System.out.println(k.getNewKey() + " : " + k.getOriginalKey());
					ps.setString(1, k.getNewKey());
					ps.setBytes(2, k.getUuid());
					ps.execute();
					updateHkey(k,kids,uuidToItem,c);
				}
			}
		}
	}
	
	

	private void updateHkey(KeyItem item, HashMap<ByteWrapper, ArrayList<KeyItem>> kids,
			HashMap<ByteWrapper, KeyItem> uuidToItem, Connection c) throws Exception{
		
		HashMap<KeyItem,String> updatedKeys = new HashMap<KeyItem, String>();
		
		String newHkey = computeHkey(item, uuidToItem, c);
		updatedKeys.put(item, newHkey);
		
		//update database
		
		//compute hkeys for all kids
		List<KeyItem> kidsToProcess = new ArrayList<KeyItem>();
		ArrayList<KeyItem> lkids = kids.get(new ByteWrapper(item.getUuid()));
		if (lkids != null){
			kidsToProcess.addAll(lkids);
		}
		while(kidsToProcess.size() > 0){
			KeyItem child = kidsToProcess.remove(0);
			lkids = kids.get(new ByteWrapper(child.getUuid()));
			if (lkids != null){
				kidsToProcess.addAll(lkids);
			}
			
			newHkey = computeHkey(child, uuidToItem, c);
			updatedKeys.put(child, newHkey);
		}
		
		//update database
		PreparedStatement ps = c.prepareStatement("UPDATE smart.dm_attribute_tree set hkey = ? WHERE uuid = ?");
		for (Iterator<Entry<KeyItem, String>> iterator = updatedKeys.entrySet().iterator(); iterator.hasNext();) {
			Entry<KeyItem, String> entry = (Entry<KeyItem, String>) iterator.next();
			ps.setString(1, entry.getValue());
			ps.setBytes(2, entry.getKey().getUuid());
			ps.execute();
		}
		
		
		FixDataModel.updateQueries(updatedKeys,this,c);
	}
	

	private String computeHkey(KeyItem item, HashMap<ByteWrapper, KeyItem> uuidToItem, Connection c){
		if (item.getParentItem() == null){
			return item.getCurrentKey() + FixDataModel.HKEY_SEPERATOR;
		}
		String parent = computeHkey(uuidToItem.get(new ByteWrapper(item.getParentItem())), uuidToItem, c);
		return parent + item.getCurrentKey() + FixDataModel.HKEY_SEPERATOR ;
	}
	
	
	
	
	@Override
	public  boolean processValueItem(IValueItem item, String oldKey, String newKey){
		if (item instanceof CombinedValueItem){
			CombinedValueItem cv = (CombinedValueItem)item;
			return processValueItem(cv.getPart1(), oldKey, newKey) || 
					processValueItem(cv.getPart2(), oldKey, newKey);
		}
		
		boolean changed = false;
		if (item instanceof AttributeValueItem){
			AttributeValueItem av = (AttributeValueItem)item;
			if (av.getAttributeType().equals("t")){
				//< SUM_ATTRIBUTE_VALUE_LISTTREE_KEY : "attribute:" ("t" | "l") ":sum:" ("obs" | "wp") ":" < DM_KEY > >
				if (av.getItemKey().equals(oldKey)){
					av.setItemKey(newKey);
					changed = true;
				}
			}
		}
		
		return changed;
	}
	
	@Override
	public  boolean processFilter(IFilter filter, String oldKey, String newKey){
		ArrayList<IFilter> filters = new ArrayList<IFilter>();
		filters.add(filter);
		boolean changed = false;
		while(filters.size() > 0){
			IFilter f = filters.remove(0);
			if (f instanceof AttributeFilter){
				AttributeFilter af = (AttributeFilter)f;
				if (af.getAttributeType().equals("t")){
					String currentKey = (String) af.getValue();
					if (currentKey.equals(oldKey)){
						af.setValue(newKey);
						changed = true;
					}
				}
			}
			if (f != null && f.getChildren() != null){
				filters.addAll(f.getChildren());
			}
		}
		return changed;
	}
	
	
	@Override
	public boolean processGroupBy(IGroupBy filter, String oldKey, String newKey){
		boolean changed = false;

		if (filter instanceof AttributeGroupBy){
			AttributeGroupBy gb = (AttributeGroupBy)filter;
			if (gb.getAttributeType().equals("t")){
				for (int i = 0; i < gb.getFilterKeys().length; i ++){
					if (gb.getFilterKeys()[i].equals(oldKey)){
						gb.getFilterKeys()[i] = newKey;
						changed = true;
					}
				}
			}
		}
		return changed;
	}
}
