package org.wcs.smart.asset.ui.views.map;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;

public class CombinedOverviewColumn implements IOverviewTableColumn{

	private IOverviewTableColumn column1;
	private IOverviewTableColumn column2;
	
	private String name;
	
	public CombinedOverviewColumn(String name, IOverviewTableColumn column1, IOverviewTableColumn column2) {
		this.name = name;
		this.column1 = column1;
		this.column2 = column2;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getValue(StationData data) {
		
		Object value1 = data.getData(column1);
		Object value2 = data.getData(column2);
		if (value1 == null || value2 == null) return 0;
		
		if (value1 != null && value2 != null && value1 instanceof Number && value2 instanceof Number) {
			if (((Number)value2).doubleValue() == 0) return String.valueOf(Double.NaN);
			return ((Number)value1).doubleValue() / ((Number)value2).doubleValue() ;
			
		}
		return Double.NaN;
	}

	@Override
	public HashMap<AssetStation, Object> computeValuesByStation(Session session, Date[] dFilter) {
		return null;
	}

	@Override
	public HashMap<AssetStationLocation, Object> computeValuesByStationLocation(Session session, Date[] dFilter) {
		return null;
	}

	@Override
	public ColumnType getType() {
		return IOverviewTableColumn.ColumnType.NUMBER;
	}

	@Override
	public String getKey() {
		return column1.getKey() + "_" + column2.getKey();
	}
	
	@Override
	public JSONObject serialize() {
		JSONObject json = new JSONObject();
		json.put("type", "combined");
		json.put("name", getName());
		json.put("column1", column1.getKey());
		json.put("column2", column2.getKey());
		return json;
	}
	
	public static CombinedOverviewColumn deserialize(JSONObject json, List<IOverviewTableColumn> columns) {
		if (json.containsKey("type") && json.containsKey("column1") && json.containsKey("column2")) {
			if (json.get("type").equals("combined")) {
				String key1 = (String) json.get("column1");
				String key2 = (String) json.get("column2");
				IOverviewTableColumn c1 = null;
				IOverviewTableColumn c2 = null;
				for(IOverviewTableColumn c : columns) {
					if (c.getKey().equals(key1)) c1 = c;
					if (c.getKey().equals(key2)) c2 = c;
				}
				
				String name = (String)json.get("name");
				if (c1 != null && c2 != null) {
					return new CombinedOverviewColumn(name, c1, c2);
				}	
			}
		}
		return null;
	}
	
	public static List<CombinedOverviewColumn> getDefaultColumns(List<IOverviewTableColumn> columns){
		IOverviewTableColumn c1 = null;
		IOverviewTableColumn c2 = null;
		
		String key1 = (new FixedColumn(FixedColumn.Column.INCIDENTS)).getKey();
		String key2 = (new FixedColumn(FixedColumn.Column.ASSET_DAYS)).getKey();
		
		for (IOverviewTableColumn c : columns) {
			if (c.getKey().equals(key1)) c1 = c;
			if (c.getKey().equals(key2)) c2 = c;
		}
		if (c1 == null || c2 == null) return Collections.emptyList();
		
		CombinedOverviewColumn i = new CombinedOverviewColumn("Total Incidents per Asset Day", c1,c2);
		return Collections.singletonList(i);
		
		
	}
}
