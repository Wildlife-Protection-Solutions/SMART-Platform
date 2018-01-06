package org.wcs.smart.asset.ui.views.map;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

public class StatisticComputer {

	/**
	 * 
	 * @param column
	 * @param dFilter may be null if should include all dates
	 * @param groupBy
	 * @return
	 */
	public static List<StationData> computeStatistics(List<IOverviewTableColumn> column, Date[] dFilter, IOverviewTableColumn.GroupByOption groupBy) {
		try(Session session = HibernateManager.openSession()){
			
			List<IOverviewTableColumn> toCompute = new ArrayList<>();
			for (IOverviewTableColumn c : column) {
				if (!(c instanceof CombinedOverviewColumn)) {
					toCompute.add(c);
				}
			}
			
			List<StationData> data = new ArrayList<>();
			if (groupBy == GroupByOption.STATION) {
				data.addAll(computeValuesByStation(toCompute, session, dFilter).values());
			}
			if (groupBy == GroupByOption.LOCATION) {
				data.addAll(computeValuesByStationLocation(toCompute, session, dFilter).values());
			}
			//TODO: sort by station id???
			return data;
		}
	}
	
	private static HashMap<AssetStation, StationData> computeValuesByStation(List<IOverviewTableColumn> toCompute, Session session, Date[] dFilter) {
		HashMap<AssetStation, StationData> results = new HashMap<>();
		
		for (IOverviewTableColumn c : toCompute) {
			try {
				HashMap<AssetStation, Object> columnData = c.computeValuesByStation(session, dFilter);
				
				for (AssetStation key : columnData.keySet()) {
					StationData data = results.get(key);
					if (data == null) {
						data = new StationData(key);
						results.put(key,  data);
					}
					data.setData(c, columnData.get(key));
				}
			}catch (Exception ex) {
				//TODO:
				AssetPlugIn.log(ex.getMessage(), ex);
			}
		}
		return results;
	}
	
	private static HashMap<AssetStationLocation, StationData> computeValuesByStationLocation(List<IOverviewTableColumn> toCompute, Session session, Date[] dFilter) {
		HashMap<AssetStationLocation, StationData> results = new HashMap<>();
		
		for (IOverviewTableColumn c : toCompute) {
			try {
				HashMap<AssetStationLocation, Object> columnData = c.computeValuesByStationLocation(session, dFilter);
				
				for (AssetStationLocation key : columnData.keySet()) {
					StationData data = results.get(key);
					if (data == null) {
						data = new StationData(key);
						results.put(key,  data);
					}
					data.setData(c, columnData.get(key));
				}
			}catch (Exception ex) {
				//TODO:
				AssetPlugIn.log(ex.getMessage(), ex);
			}
		}
		return results;
	}
	
	public static HashMap<AssetStation, Object> computeNumberOfDaysPerStation(Session session, Date[] dFilter){
		
		Query<AssetDeployment> query = null;
		if (dFilter != null) {
			String hql = "FROM AssetDeployment a WHERE startDate <= :endDate and (endDate is null or endDate >= :startDate)";
			query = session.createQuery(hql, AssetDeployment.class)
				.setParameter("endDate", dFilter[1])
				.setParameter("startDate", dFilter[0]);
		}else {
			String hql = "FROM AssetDeployment a ";
			query = session.createQuery(hql, AssetDeployment.class);
		}
		
		
		HashMap<AssetStation, HashSet<Long>> daysPerStation = new HashMap<>();
		
		try(ScrollableResults results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = (AssetDeployment) results.get(0);
				
				Long startDate = SharedUtils.getDatePart(d.getStartDate(), false).getTime();
				Long endDate = (new Date()).getTime();
				if (d.getEndDate() != null) {
					endDate = SharedUtils.getDatePart(d.getEndDate(), false).getTime();
				}
				
				AssetStation s = d.getStationLocation().getStation();
				HashSet<Long> dates= daysPerStation.get(s);
				if ( dates == null) {
					dates = new HashSet<>();
					daysPerStation.put(s, dates);
				}
				while(startDate < endDate) {
					if (dFilter == null) {
						dates.add(startDate);
					}else if (startDate >= dFilter[0].getTime() && startDate <= dFilter[1].getTime()) {
						dates.add(startDate);
					}
					startDate += 86400000;
				}
			}
		}
		
		HashMap<AssetStation, Object> results = new HashMap<>();
		for (Entry<AssetStation, HashSet<Long>> e : daysPerStation.entrySet()) {
			results.put(e.getKey(), e.getValue().size());
		}
		return results;
	}
	
	public static  HashMap<AssetStationLocation, Object> computeNumberOfDaysPerStationLocation(Session session, Date[] dFilter){
		
		Query<AssetDeployment> query = null;
		if (dFilter != null) {
			String hql = "FROM AssetDeployment a WHERE startDate <= :endDate and (endDate is null or endDate >= :startDate)";
			query = session.createQuery(hql, AssetDeployment.class)
				.setParameter("endDate", dFilter[1])
				.setParameter("startDate", dFilter[0]);
		}else {
			String hql = "FROM AssetDeployment a ";
			query = session.createQuery(hql, AssetDeployment.class);
		}
				
		
		HashMap<AssetStationLocation, HashSet<Long>> daysPerStation = new HashMap<>();
		
		try(ScrollableResults results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = (AssetDeployment) results.get(0);
				
				Long startDate = SharedUtils.getDatePart(d.getStartDate(), false).getTime();
				Long endDate = (new Date()).getTime();
				if (d.getEndDate() != null) {
					endDate = SharedUtils.getDatePart(d.getEndDate(), false).getTime();
				}
				
				AssetStationLocation s = d.getStationLocation();
				HashSet<Long> dates= daysPerStation.get(s);
				if ( dates == null) {
					dates = new HashSet<>();
					daysPerStation.put(s, dates);
				}
				while(startDate < endDate) {
					if (dFilter == null) {
						dates.add(startDate);
					}else if (startDate >= dFilter[0].getTime() && startDate <= dFilter[1].getTime()) {
						dates.add(startDate);
					}
					startDate += 86400000;
				}
			}
		}
		
		HashMap<AssetStationLocation, Object> results = new HashMap<>();
		for (Entry<AssetStationLocation, HashSet<Long>> e : daysPerStation.entrySet()) {
			results.put(e.getKey(), e.getValue().size());
		}
		return results;
	}
	
	
	public static HashMap<AssetStation, Object> computeAssetDaysPerStation(Session session, Date[] dFilter){
		
		Query<AssetDeployment> query = null;
		if (dFilter != null) {
			String hql = "FROM AssetDeployment a WHERE startDate <= :endDate and (endDate is null or endDate >= :startDate)";
			query = session.createQuery(hql, AssetDeployment.class)
				.setParameter("endDate", dFilter[1])
				.setParameter("startDate", dFilter[0]);
		}else {
			String hql = "FROM AssetDeployment a ";
			query = session.createQuery(hql, AssetDeployment.class);
		}
		
		HashMap<AssetStation, Object> daysCnt = new HashMap<>();
		
		try(ScrollableResults results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = (AssetDeployment) results.get(0);
				
				Long startDate = SharedUtils.getDatePart(d.getStartDate(), false).getTime();
				Long endDate = (new Date()).getTime();
				if (d.getEndDate() != null) {
					endDate = SharedUtils.getDatePart(d.getEndDate(), false).getTime();
				}
				
				AssetStation s = d.getStationLocation().getStation();
				int cnt = 0;
				while(startDate < endDate) {
					if (dFilter == null) {
						cnt++;
					}else if (startDate >= dFilter[0].getTime() && startDate <= dFilter[1].getTime()) {
						cnt++;
					}
					
					startDate += 86400000;
				}
				Integer days = (Integer) daysCnt.get(s);
				if (days == null) {
					days = cnt;
				}else {
					days += cnt;
				}
				daysCnt.put(s, days);
				
			}
		}
		return daysCnt;
	}
	
	public static HashMap<AssetStationLocation, Object> computeAssetDaysPerStationLocation(Session session, Date[] dFilter){
		
		Query<AssetDeployment> query = null;
		if (dFilter != null) {
			String hql = "FROM AssetDeployment a WHERE startDate <= :endDate and (endDate is null or endDate >= :startDate)";
			query = session.createQuery(hql, AssetDeployment.class)
				.setParameter("endDate", dFilter[1])
				.setParameter("startDate", dFilter[0]);
		}else {
			String hql = "FROM AssetDeployment a ";
			query = session.createQuery(hql, AssetDeployment.class);
		}
		
		
		HashMap<AssetStationLocation, Object> daysCnt = new HashMap<>();
		
		try(ScrollableResults results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = (AssetDeployment) results.get(0);
				
				Long startDate = SharedUtils.getDatePart(d.getStartDate(), false).getTime();
				Long endDate = (new Date()).getTime();
				if (d.getEndDate() != null) {
					endDate = SharedUtils.getDatePart(d.getEndDate(), false).getTime();
				}
				
				AssetStationLocation s = d.getStationLocation();
				int cnt = 0;
				while(startDate < endDate) {
					if (dFilter == null) {
						cnt++;
					}else if (startDate >= dFilter[0].getTime() && startDate <= dFilter[1].getTime()) {
						cnt++;
					}
					startDate += 86400000;
				}
				Integer days = (Integer) daysCnt.get(s);
				if (days == null) {
					days = cnt;
				}else {
					days += cnt;
				}
				daysCnt.put(s, days);
				
			}
		}
		return daysCnt;
	}

	public static HashMap<AssetStation, Object> incidentsPerStation(Session session, Date[] dFilter){
		
		HashMap<AssetStation, Object> results = new HashMap<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT uuid, count(wp_uuid) FROM ");
		sb.append("(");
		sb.append("SELECT DISTINCT s.uuid as uuid, wp.uuid as wp_uuid ");
		sb.append("FROM smart.asset_waypoint a JOIN smart.waypoint wp ON a.wp_uuid = wp.uuid ");
		sb.append(" JOIN smart.asset_deployment d on a.asset_deployment_uuid = d.uuid ");
		sb.append(" JOIN smart.asset_station_location l on d.station_location_uuid = l.uuid ");
		sb.append(" JOIN smart.asset_station s on l.station_uuid = s.uuid ");
		if (dFilter != null) {
			sb.append(" WHERE ");
			sb.append(" wp.dateTime >= :startDate and wp.dateTime <= :endDate " );
		}
		sb.append(") as foo ");
		sb.append(" GROUP BY uuid");
		
		Query<?> query = session.createNativeQuery(sb.toString());
		if (dFilter != null) {
				query.setParameter("startDate", dFilter[0])
				.setParameter("endDate", dFilter[1]);
		}
		List<?> qresults = query.list();
		for (Object result : qresults) {
			UUID stationUuid = UuidUtils.byteToUUID( (byte[])((Object[])result)[0] );
			Integer cnt = (Integer) ((Object[])result)[1];
			
			AssetStation s = session.get(AssetStation.class, stationUuid);
			if (s != null) {
				results.put(s, cnt);
			}
		}
		return results;
		
	}
	

	public static HashMap<AssetStationLocation, Object> incidentsPerLocation(Session session, Date[] dFilter){
		
		HashMap<AssetStationLocation, Object> results = new HashMap<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT uuid, count(wp_uuid) FROM ");
		sb.append("(");
		sb.append("SELECT DISTINCT l.uuid as uuid, wp.uuid as wp_uuid ");
		sb.append("FROM smart.asset_waypoint a JOIN smart.waypoint wp ON a.wp_uuid = wp.uuid ");
		sb.append(" JOIN smart.asset_deployment d on a.asset_deployment_uuid = d.uuid ");
		sb.append(" JOIN smart.asset_station_location l on d.station_location_uuid = l.uuid ");
		if (dFilter != null) {
			sb.append(" WHERE ");
			sb.append(" wp.dateTime >= :startDate and wp.dateTime <= :endDate " );
		}
		sb.append(") as foo ");
		sb.append(" GROUP BY uuid");
		
		Query<?> query = session.createNativeQuery(sb.toString());
		if (dFilter != null) {
				query.setParameter("startDate", dFilter[0])
				.setParameter("endDate", dFilter[1]);
		}
		List<?> qresults = query.list();
		for (Object result : qresults) {
			UUID locationUuid = UuidUtils.byteToUUID( (byte[])((Object[])result)[0] );
			Integer cnt = (Integer) ((Object[])result)[1];
			
			AssetStationLocation s = session.get(AssetStationLocation.class, locationUuid);
			if (s != null) {
				results.put(s, cnt);
			}
		}
		return results;
		
	}
	
}
