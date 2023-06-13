/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.asset.ui.views.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Configuration for the columns available for the asset
 * overview map
 * 
 * @author Emily
 *
 */
public class AssetMapColumnConfiguration {

	private List<OverviewTableColumnWrapper> allColumns;
	
	/**
	 * Creates a new configuration with no columns
	 */
	public AssetMapColumnConfiguration() {
		allColumns = Collections.emptyList();
	}
	
	/**
	 * 
	 * @return the defined columns
	 */
	public List<OverviewTableColumnWrapper> getColumns(){
		return this.allColumns;
	}
	
	/**
	 * removes a column from the configuration
	 * @param column
	 */
	public void removeColumn(OverviewTableColumnWrapper column) {
		if (column.isFixed()) return;
		allColumns.remove(column);
		reorder();
	}
	
	/**
	 * moves columns within the configuration
	 * 
	 * @param columns
	 * @param amount values less than 0 move items back in the list; values greater then 0 move items forward in the list
	 */
	public void moveColumns(List<OverviewTableColumnWrapper> columns, int amount) {
		if (amount > 0) Collections.reverse(columns);
		for (OverviewTableColumnWrapper c : columns) {
			if (!allColumns.contains(c)) continue;
		
			int index = allColumns.indexOf(c);
			index += amount;
			if (index >= allColumns.size()) index = allColumns.size() - 1;
			if (index < 0) index = 0;
			
			allColumns.remove(c);
			allColumns.add(index, c);
		}
		reorder();
	}
	
	/*
	 * set order value for all columns
	 */
	private void reorder() {
		for (int i = 0; i < allColumns.size(); i ++) {
			allColumns.get(i).setOrder(i);
		}
	}
	
	/**
	 * Adds a new (non-fixed) column to the configuration.  If the new
	 * column doesn't have a key, a unique key will be generated for the configuration.
	 * 
	 * @param newColumn
	 */
	public OverviewTableColumnWrapper  addColumn(IOverviewTableColumn newColumn) {
		if (newColumn.getKey() == null) {
			//generate key
			Set<String> existingKeys = allColumns.stream().map(e->e.getColumn().getKey()).collect(Collectors.toSet());
			String key = newColumn.getName().toLowerCase().trim().replaceAll("[^a-z0-9]", ""); //$NON-NLS-1$ //$NON-NLS-2$
			int cnt=1;
			String newKey = key;
			while(existingKeys.contains(newKey)) {
				newKey = key + "" + cnt; //$NON-NLS-1$
				cnt++;
			}
			newColumn.setKey(newKey);
			if (newColumn.getKey() == null || newColumn.getKey().isEmpty()) {
				//should never happen
				throw new IllegalStateException(Messages.AssetMapColumnConfiguration_keyRequired);
			}
		}
		OverviewTableColumnWrapper wrapper = new OverviewTableColumnWrapper(newColumn, false);
		wrapper.setVisible(true);
		allColumns.add(wrapper);
		
		reorder();
		return wrapper;
	}
	
	/**
	 * Save the configuration to the database
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean saveConfiguration() {
		try(Session session = HibernateManager.openSession()){ 
			AssetModuleSettings savedMap = QueryFactory.buildQuery(session, AssetModuleSettings.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"keyId", AssetModuleSettings.OVERVIEW_MAP_COLUMN_KEY} ).uniqueResult(); //$NON-NLS-1$
			if (savedMap == null) {
				savedMap = new AssetModuleSettings();
				savedMap.setConservationArea(SmartDB.getCurrentConservationArea());
				savedMap.setKeyId(AssetModuleSettings.OVERVIEW_MAP_COLUMN_KEY);
			}
			
			JSONArray columns = new JSONArray();
			for (int i = 0; i < allColumns.size(); i ++) {
				OverviewTableColumnWrapper column = allColumns.get(i);
				
				JSONObject jsoncolumn = new JSONObject();
				jsoncolumn.put("isvisible", column.isVisible()); //$NON-NLS-1$
				jsoncolumn.put("isfixed", column.isFixed()); //$NON-NLS-1$
				jsoncolumn.put("order", i); //$NON-NLS-1$
				jsoncolumn.put("definition", column.getColumn().serialize()); //$NON-NLS-1$
				
				columns.add(jsoncolumn);
			}
			savedMap.setValue(columns.toJSONString());
			session.beginTransaction();
			try {
				HibernateManager.saveOrMerge(session, savedMap);
				session.getTransaction().commit();
			}catch (Exception ex) {
				AssetPlugIn.displayLog(Messages.AssetMapColumnConfiguration_SaveError + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return false;
			}
			return true;
		}
		
	}
	
	
	/**
	 * load the columns defined in the database into the current configuration
	 * @param session
	 */
	public void loadColumnConfiguration(Session session) {
		AssetModuleSettings savedMap = QueryFactory.buildQuery(session, AssetModuleSettings.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"keyId", AssetModuleSettings.OVERVIEW_MAP_COLUMN_KEY} ).uniqueResult(); //$NON-NLS-1$
		allColumns = new ArrayList<>();
		
		if (savedMap != null) {
			
			//parse map settings
			String settingsValue = savedMap.getValue();
			try {
				JSONArray json = (JSONArray) (new JSONParser()).parse(settingsValue);
				List<JSONObject> tryAgain = new ArrayList<>();
				List<OverviewTableColumnWrapper> columns = new ArrayList<>();
				for (int i = 0; i < json.size(); i ++) {
					JSONObject obj = (JSONObject) json.get(i);
					if (!obj.containsKey("definition")) continue; //$NON-NLS-1$
					
					JSONObject definition = (JSONObject) obj.get("definition"); //$NON-NLS-1$
					IOverviewTableColumn column = FixedColumn.deserialize(definition);
					if (column == null) column = CategoryOverviewColumn.deserialize(definition);
					if (column == null) column = CombinedOverviewColumn.deserialize(definition);
					
					if (column == null) {
						//try again
					}
					if (column != null) {
						columns.add(createWrapper(obj, column));
					}else {
						tryAgain.add(obj);
					}
				}
				
				//update the fixed combined columns 
				List<CombinedOverviewColumn> fixedOverviewColumns = CombinedOverviewColumn.getDefaultColumns(columns.stream().map(e->e.getColumn()).collect(Collectors.toList()));
				for (OverviewTableColumnWrapper w : columns) {
					for (CombinedOverviewColumn c : fixedOverviewColumns) {
						if (w.getColumn().getKey().equalsIgnoreCase(c.getKey())) {
							w.setColumn(c);
							break;
						}
					}
				}
				columns.sort((a,b)->Integer.compare(a.getOrder(), b.getOrder()));
				allColumns.addAll(columns);
			}catch (Exception ex) {
				AssetPlugIn.displayLog(Messages.AssetMapColumnConfiguration_LoadError + ex.getMessage(), ex);
			}		
		}
		
		//if we still have no columns we default to the fixed columns
		if (allColumns.isEmpty()) {
			resetToDefault();
		}
	}
	
	public void resetToDefault() {
		allColumns.clear();
		for (FixedColumn.Column c : FixedColumn.Column.values()) {
			IOverviewTableColumn oc = new FixedColumn(c);
			OverviewTableColumnWrapper wrapper = new OverviewTableColumnWrapper(oc, true);
			wrapper.setVisible(c.defaultVisibility);
			allColumns.add(wrapper);
		}
		
		for (CombinedOverviewColumn c : CombinedOverviewColumn.getDefaultColumns(allColumns.stream().map(e->e.getColumn()).collect(Collectors.toList()))) {
			OverviewTableColumnWrapper wrapper = new OverviewTableColumnWrapper(c, true);
			wrapper.setVisible(true);
			allColumns.add(wrapper);
		}
	}
	private static OverviewTableColumnWrapper createWrapper(JSONObject outerField, IOverviewTableColumn column) {
		boolean isVisible = true;
		boolean isFixed = true;
		if (outerField.containsKey("isvisible")) { //$NON-NLS-1$
			isVisible = (Boolean)outerField.get("isvisible"); //$NON-NLS-1$
		}
		if (outerField.containsKey("isfixed")) { //$NON-NLS-1$
			isFixed= (Boolean)outerField.get("isfixed"); //$NON-NLS-1$
		}
		int order = ((Long) outerField.get("order")).intValue(); //$NON-NLS-1$
		OverviewTableColumnWrapper c = new OverviewTableColumnWrapper(column, isFixed);
		c.setVisible(isVisible);
		c.setOrder(order);
		return c;
	}
	
}
