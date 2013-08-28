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
import org.wcs.smart.query.parser.internal.filter.CategoryFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.AttributeGroupBy;
import org.wcs.smart.query.parser.internal.summary.AttributeValueItem;
import org.wcs.smart.query.parser.internal.summary.CategoryGroupBy;
import org.wcs.smart.query.parser.internal.summary.CategoryValueItem;
import org.wcs.smart.query.parser.internal.summary.CombinedValueItem;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.parser.internal.summary.SumQueryDefinition;

public class CategoryProcessor implements IDataProcessor{
	
	public void fixCategories(Connection c) throws Exception{

		//read all categorys from the database and group by the
		//category parent so keys are unique across the same level of the tree
		HashMap<ByteWrapper, List<KeyItem>> items = new HashMap<ByteWrapper, List<KeyItem>>();
		HashMap<ByteWrapper, List<KeyItem>> caitems = new HashMap<ByteWrapper, List<KeyItem>>();
		
		String sql = "SELECT a.ca_uuid, case when a.parent_category_uuid is null then a.ca_uuid else a.parent_category_uuid end, a.uuid, a.keyid, b.language_uuid, b.value, a.parent_category_uuid, a.hkey FROM smart.dm_category a, smart.i18n_label b where b.element_uuid = a.uuid";
		ResultSet rs = c.createStatement().executeQuery(sql);
		while(rs.next()){
			byte[] cauuid = rs.getBytes(1);
			byte[] groupbyUuid = rs.getBytes(2);
			byte[] uuid = rs.getBytes(3);
			String key = rs.getString(4);
			byte[] lang = rs.getBytes(5);
			String name = rs.getString(6);
			byte[] parentuuid = rs.getBytes(7);
			String hkey = rs.getString(8);
			
			ByteWrapper groupBw = new ByteWrapper(groupbyUuid);
			List<KeyItem> its = items.get(groupBw);
			if (its == null){
				its = new ArrayList<KeyItem>();
				items.put(groupBw,  its);
			}
			KeyItem keyItem  = null;
			for (KeyItem it : its){
				if (Arrays.equals(uuid, it.getUuid())){
					keyItem = it;
				}
			}
			if (keyItem == null){
				keyItem = new KeyItem(uuid, key, cauuid, parentuuid, hkey);
				its.add(keyItem);
			}
			keyItem.addName(lang, name);
			
			ByteWrapper caBw = new ByteWrapper(cauuid);
			List<KeyItem> caits = caitems.get(caBw);
			if (caits == null){
				caits = new ArrayList<KeyItem>();
				caitems.put(caBw,  caits);
			}
			caits.add(keyItem);
		}
		rs.close();
		
		
		FixDataModel.fixItems(items);
		
		///update keys and names as required
		PreparedStatement ps = c.prepareStatement("UPDATE smart.dm_category set keyid = ? WHERE uuid = ?");
		PreparedStatement psname = c.prepareStatement("UPDATE smart.i18n_label set value = ? WHERE language_uuid = ? and element_uuid = ?");
		
		
		for (Iterator<List<KeyItem>> iterator = caitems.values().iterator(); iterator.hasNext();) {
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
					ps.setString(1, k.getNewKey());
					ps.setBytes(2, k.getUuid());
					ps.execute();
					updateHkey(k,kids,uuidToItem,c);
				}
				for (KeyItem.KeyName lname : k.getNames()){
					if (lname.isChanged()){
						psname.setString(1, lname.getNewName());
						psname.setBytes(2, lname.getLanguage());
						psname.setBytes(3, k.getUuid());
						psname.execute();
					}
				}
			}
		}
		
	}
	
	
	private void updateHkey(KeyItem item, HashMap<ByteWrapper, ArrayList<KeyItem>> kids, HashMap<ByteWrapper, KeyItem> uuidToItem, Connection c) throws Exception{
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
		PreparedStatement ps = c.prepareStatement("UPDATE smart.dm_category set hkey = ? WHERE uuid = ?");
		for (Iterator<Entry<KeyItem, String>> iterator = updatedKeys.entrySet().iterator(); iterator.hasNext();) {
			Entry<KeyItem, String> entry = (Entry<KeyItem, String>) iterator.next();
			ps.setString(1, entry.getValue());
			ps.setBytes(2, entry.getKey().getUuid());
			ps.execute();
		}
		
		FixDataModel.updateQueries(updatedKeys, this, c);
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
			if (av.getCategoryKey() != null && av.getCategoryKey().equals(oldKey)){
				av.setCategoryKey(newKey);
				changed = true;
			}
		}
		if (item instanceof CategoryValueItem){
			CategoryValueItem cv = (CategoryValueItem)item;
			if (cv.getCategoryHKey() != null && cv.getCategoryHKey().equals(oldKey)){
				cv.setCategoryHKey(newKey);
				changed = true;
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
			if (f instanceof CategoryFilter){
				CategoryFilter cf = (CategoryFilter)f;
				if (cf.getCategoryKey() != null && cf.getCategoryKey().equals(oldKey)){
					cf.setCategoryKey(newKey);
					changed = true;
				}
			}
			if (f != null && f.getChildren() != null){
				filters.addAll(f.getChildren());
			}
		}
		return changed;
	}
	
	
	@Override
	public  boolean processGroupBy(IGroupBy filter, String oldKey, String newKey){
		boolean changed = false;
		
		if (filter instanceof AttributeGroupBy){
			AttributeGroupBy gb = (AttributeGroupBy)filter;
			if (gb.getCategoryHkey() != null && gb.getCategoryHkey().equals(oldKey)){
				gb.setCategoryHkey(newKey);
				changed = true;
			}
		}
		if (filter instanceof CategoryGroupBy){
			CategoryGroupBy gb = (CategoryGroupBy)filter;
			for (int i = 0; i < gb.getFilterHkeys().length; i ++){
				if (gb.getFilterHkeys()[i].equals(oldKey)){
					gb.getFilterHkeys()[i] = newKey;
					changed = true;
				}
			}
		}
		return changed;
	}
}
