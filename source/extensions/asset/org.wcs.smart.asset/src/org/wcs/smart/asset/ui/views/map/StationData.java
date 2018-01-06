package org.wcs.smart.asset.ui.views.map;

import java.util.HashMap;

import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;

public class StationData {

	private AssetStation station;
	private AssetStationLocation location;
	
	private HashMap<IOverviewTableColumn, Object> values = new HashMap<>();
	
	public StationData(AssetStation station) {
		this.station = station;
	}
	
	public StationData(AssetStationLocation location) {
		this.location = location;
	}
	
	public Object getData(IOverviewTableColumn column) {
		return values.get(column);
	}
	
	public String getIdField() {
		if (station != null) return station.getId();
		if (location != null) return location.getId();
		return "";
	}
	
	public AssetStation getStation() {
		return this.station;
	}
	
	public AssetStationLocation getStationLocation() {
		return this.location;
	}
	public void setData(IOverviewTableColumn column, Object value) {
		values.put(column, value);
	}
	
}
