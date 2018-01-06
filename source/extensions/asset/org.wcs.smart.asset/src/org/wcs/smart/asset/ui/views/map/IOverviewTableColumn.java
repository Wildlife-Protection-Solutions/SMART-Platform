package org.wcs.smart.asset.ui.views.map;

import java.util.Date;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;

public interface IOverviewTableColumn {

	public static enum GroupByOption{
		STATION,LOCATION
	};
	
	public String getName();
	
	public String getValue(StationData data);
	
	/**
	 * 
	 * @param session
	 * @param dFilter may be null if all dates should be included
	 * @return
	 */
	public HashMap<AssetStation, Object> computeValuesByStation(Session session, Date[] dFilter);
	
	/**
	 * 
	 * @param session
	 * @param dFilter may be null if all dates should be included
	 * @return
	 */
	public HashMap<AssetStationLocation, Object> computeValuesByStationLocation(Session session, Date[] dFilter);
	
}
