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

public class AttributeListItemProcessor implements IDataProcessor {
	public void fixAttributesListItems(Connection c) throws Exception{
		
		//read all attribute keys from the database and store in temporary structure 
		//that links ca to attributes
		
		
		HashMap<ByteWrapper, List<KeyItem>> items = new HashMap<ByteWrapper, List<KeyItem>>();
		
		//group list items by attribute so that all list items
		//associated with the same category are fixed together; ensure
		//correct keys
		String sql = "SELECT a.attribute_uuid, a.uuid, a.keyid, b.language_uuid, b.value, c.ca_uuid " +
					" FROM smart.dm_attribute_list a, smart.i18n_label b, smart.dm_attribute c " +
					" WHERE b.element_uuid = a.uuid and c.uuid = a.attribute_uuid ";
		ResultSet rs = c.createStatement().executeQuery(sql);
		while(rs.next()){
			byte[] attributeuuid = rs.getBytes(1);
			byte[] uuid = rs.getBytes(2);
			String key = rs.getString(3);
			byte[] lang = rs.getBytes(4);
			String name = rs.getString(5);
			byte[] cauuid = rs.getBytes(6);
			
			ByteWrapper ca = new ByteWrapper(attributeuuid);
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
				keyItem = new KeyItem(uuid, key, cauuid);
				its.add(keyItem);
			}
			keyItem.addName(lang, name);
		}
		rs.close();
		
		FixDataModel.fixItems(items);
		
		///update keys and names as required
		PreparedStatement ps = c.prepareStatement("UPDATE smart.dm_attribute_list set keyid = ? WHERE uuid = ?");
		PreparedStatement psname = c.prepareStatement("UPDATE smart.i18n_label set value = ? WHERE language_uuid = ? and element_uuid = ?");
		
		List<KeyItem> updated = new ArrayList<KeyItem>();
		for (Iterator<List<KeyItem>> iterator = items.values().iterator(); iterator.hasNext();) {
			List<KeyItem> keys = (List<KeyItem>) iterator.next();
			for (KeyItem k : keys){
				if (k.isChanged()){
					ps.setString(1, k.getNewKey());
					ps.setBytes(2, k.getUuid());
					ps.execute();
					updated.add(k);
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
		
		FixDataModel.updateQueries(updated,this,c);
	}
	
	@Override
	public boolean processValueItem(IValueItem item, String oldKey, String newKey){
		if (item instanceof CombinedValueItem){
			CombinedValueItem cv = (CombinedValueItem)item;
			return processValueItem(cv.getPart1(), oldKey, newKey) || 
					processValueItem(cv.getPart2(), oldKey, newKey);
		}
		
		boolean changed = false;
		if (item instanceof AttributeValueItem){
			AttributeValueItem av = (AttributeValueItem)item;
			if (av.getAttributeType().equals("l")){
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
	public boolean processFilter(IFilter filter, String oldKey, String newKey){
		ArrayList<IFilter> filters = new ArrayList<IFilter>();
		filters.add(filter);
		boolean changed = false;
		while(filters.size() > 0){
			IFilter f = filters.remove(0);
			if (f instanceof AttributeFilter){
				AttributeFilter af = (AttributeFilter)f;
				if (af.getAttributeType().equals("l")){
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
			if (gb.getAttributeType().equals("l")){
				if (gb.getFilterKeys() != null){
					for (int i = 0; i < gb.getFilterKeys().length; i ++){
						if (gb.getFilterKeys()[i].equals(oldKey)){
							gb.getFilterKeys()[i] = newKey;
							changed = true;
						}
					}
				}
			}
		}
		return changed;
	}
}
