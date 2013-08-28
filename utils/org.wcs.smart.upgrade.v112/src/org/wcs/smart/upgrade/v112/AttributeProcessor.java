package org.wcs.smart.upgrade.v112;

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
import org.wcs.smart.query.parser.internal.summary.AttributeGroupBy;
import org.wcs.smart.query.parser.internal.summary.AttributeValueItem;
import org.wcs.smart.query.parser.internal.summary.CombinedValueItem;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.query.parser.internal.summary.IValueItem;

public class AttributeProcessor implements IDataProcessor {

	
	public void fixAttributes(Connection c) throws Exception{
		
		
		
		HashMap<ByteWrapper, List<KeyItem>> items = new HashMap<ByteWrapper, List<KeyItem>>();

		//groups attributes by ca so keys are not duplicated across attributes
		//in a given ca but can be duplicated across cas
		String sql = "SELECT a.ca_uuid, a.uuid, a.keyid " +
					" FROM smart.dm_attribute a ";
		ResultSet rs = c.createStatement().executeQuery(sql);
		while(rs.next()){
			byte[] cauuid = rs.getBytes(1);
			byte[] uuid = rs.getBytes(2);
			String key = rs.getString(3);
			
			ByteWrapper ca = new ByteWrapper(cauuid);
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
		}
		rs.close();
		
		
		FixDataModel.fixItems(items);
		
		
		
		///update keys and names as required
		PreparedStatement ps = c.prepareStatement("UPDATE smart.dm_attribute set keyid = ? WHERE uuid = ?");
		PreparedStatement psname = c.prepareStatement("UPDATE smart.i18n_label set value = ? WHERE language_uuid = ? and element_uuid = ?");
		
		ArrayList<KeyItem> updatedItems = new ArrayList<KeyItem>();
		for (Iterator<List<KeyItem>> iterator = items.values().iterator(); iterator.hasNext();) {
			List<KeyItem> keys = (List<KeyItem>) iterator.next();
			for (KeyItem k : keys){
				if (k.isChanged()){
					ps.setString(1, k.getNewKey());
					ps.setBytes(2, k.getUuid());
					ps.execute();
					updatedItems.add(k);
				}
			}
		}
		
		FixDataModel.updateQueries(updatedItems,this,c);
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
			if (av.getAttributeKey().equals(oldKey)){
				av.setAttributeKey(newKey);
				changed = true;
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
				if (af.getAttributeKey().equals(oldKey)){
					af.setAttributeKey(newKey);
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
	public boolean processGroupBy(IGroupBy filter, String oldKey, String newKey){
		boolean changed = false;
		
		if (filter instanceof AttributeGroupBy){
			AttributeGroupBy gb = (AttributeGroupBy)filter;
			if (gb.getAttributeKey().equals(oldKey)){
				gb.setAttributeKey(newKey);
				changed = true;
			}
		}
		return changed;
	}
}
