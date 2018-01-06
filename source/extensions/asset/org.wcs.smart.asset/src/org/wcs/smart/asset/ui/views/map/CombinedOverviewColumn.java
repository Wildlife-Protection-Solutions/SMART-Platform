package org.wcs.smart.asset.ui.views.map;

import java.util.Date;
import java.util.HashMap;

import org.hibernate.Session;
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
	public String getValue(StationData data) {
		
		Object value1 = data.getData(column1);
		Object value2 = data.getData(column2);
		if (value1 == null || value2 == null) return String.valueOf(0);
		
		if (value1 != null && value2 != null && value1 instanceof Number && value2 instanceof Number) {
			if (((Number)value2).doubleValue() == 0) return String.valueOf(Double.NaN);
			return String.valueOf (   ((Number)value1).doubleValue() / ((Number)value2).doubleValue() );
			
		}
		return "Cannot combine non-numeric values";
	}

	@Override
	public HashMap<AssetStation, Object> computeValuesByStation(Session session, Date[] dFilter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<AssetStationLocation, Object> computeValuesByStationLocation(Session session, Date[] dFilter) {
		// TODO Auto-generated method stub
		return null;
	}

}
