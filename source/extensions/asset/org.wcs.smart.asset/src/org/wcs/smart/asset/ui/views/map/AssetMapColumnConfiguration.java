package org.wcs.smart.asset.ui.views.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

public class AssetMapColumnConfiguration {

	private List<OverviewTableColumnWrapper> allColumns;
	
	
	public AssetMapColumnConfiguration() {
		allColumns = Collections.emptyList();
	}
	
	public List<OverviewTableColumnWrapper> getColumns(){
		return this.allColumns;
	}
	
	public boolean saveConfiguration() {
		try(Session session = HibernateManager.openSession()){ 
			AssetModuleSettings savedMap = QueryFactory.buildQuery(session, AssetModuleSettings.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
					new Object[] {"keyId", AssetModuleSettings.OVERVIEW_MAP_COLUMN_KEY} ).uniqueResult();
			if (savedMap == null) {
				savedMap = new AssetModuleSettings();
				savedMap.setConservationArea(SmartDB.getCurrentConservationArea());
				savedMap.setKeyId(AssetModuleSettings.OVERVIEW_MAP_COLUMN_KEY);
			}
			
			JSONArray columns = new JSONArray();
			for (int i = 0; i < allColumns.size(); i ++) {
				OverviewTableColumnWrapper column = allColumns.get(i);
				
				JSONObject jsoncolumn = new JSONObject();
				jsoncolumn.put("isvisible", column.isVisible());
				jsoncolumn.put("isfixed", column.isFixed());
				jsoncolumn.put("order", i);
				jsoncolumn.put("definition", column.getColumn().serialize());
				
				columns.add(jsoncolumn);
			}
			savedMap.setValue(columns.toJSONString());
			session.beginTransaction();
			try {
				session.saveOrUpdate(savedMap);
				session.getTransaction().commit();
			}catch (Exception ex) {
				AssetPlugIn.displayLog("Unable to save changes to the asset overview map columns. " + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return false;
			}
			return true;
		}
		
	}
	
	
	public void loadColumnConfiguration(Session session) {
		AssetModuleSettings savedMap = QueryFactory.buildQuery(session, AssetModuleSettings.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
				new Object[] {"keyId", AssetModuleSettings.OVERVIEW_MAP_COLUMN_KEY} ).uniqueResult();
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
					if (!obj.containsKey("definition")) continue;
					
					JSONObject definition = (JSONObject) obj.get("definition");
					IOverviewTableColumn column = FixedColumn.deserialize(definition);
					if (column == null) {
						//try again;
					}
					if (column != null) {
						columns.add(createWrapper(obj, column));
					}else {
						tryAgain.add(obj);
					}
					
				}
				
				List<IOverviewTableColumn> overviewColumns = columns.stream().map(e->e.getColumn()).collect(Collectors.toList());
				for (JSONObject obj : tryAgain) {
					JSONObject definition = (JSONObject) obj.get("definition");
					if (!obj.containsKey("definition")) continue;
					IOverviewTableColumn column = CombinedOverviewColumn.deserialize(definition, overviewColumns);
					if (column != null) {
						columns.add(createWrapper(obj, column));
					}
				}
				
				columns.sort((a,b)->Integer.compare(a.getOrder(), b.getOrder()));
				allColumns.addAll(columns);
			}catch (Exception ex) {
				AssetPlugIn.displayLog("Unable to load asset overview map settings from database. Default columns will be used. " + ex.getMessage(), ex);
			}
				
		}
		
		//if we still have no columns we default to the fixed columns
		if (allColumns.isEmpty()) {
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
	}
	
	private static OverviewTableColumnWrapper createWrapper(JSONObject outerField, IOverviewTableColumn column) {
		boolean isVisible = true;
		boolean isFixed = true;
		if (outerField.containsKey("isvisible")) {
			isVisible = (Boolean)outerField.get("isvisible");
		}
		if (outerField.containsKey("isfixed")) {
			isFixed= (Boolean)outerField.get("isfixed");
		}
		int order = ((Long) outerField.get("order")).intValue();
		OverviewTableColumnWrapper c = new OverviewTableColumnWrapper(column, isFixed);
		c.setVisible(isVisible);
		c.setOrder(order);
		return c;
	}
	
}
