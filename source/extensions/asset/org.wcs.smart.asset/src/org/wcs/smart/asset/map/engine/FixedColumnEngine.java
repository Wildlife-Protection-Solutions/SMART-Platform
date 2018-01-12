package org.wcs.smart.asset.map.engine;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.views.map.FixedColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

public class FixedColumnEngine implements IColumnEngine {

	private Session session;
	private Date[] dFilter;
	
	public FixedColumnEngine(Session session, Date[] dFilter) {
		this.session = session;
		this.dFilter = dFilter;
	}
	
	
	@Override
	public boolean canProcess(IOverviewTableColumn column) {
		if (column instanceof FixedColumn) {
			switch(((FixedColumn) column).getColumn()) {
			case ACTIVE_DAYS:
			case ASSET_DAYS:
			case INCIDENTS:
				return true;
			}
		}
		return false;
	}
	
	@Override
	public HashMap<UUID, Object> computeValues(IOverviewTableColumn toCompute, GroupByOption groupBy) {
		FixedColumn c = (FixedColumn) toCompute;
		
		if (groupBy == GroupByOption.STATION) {
			switch(c.getColumn()) {
			case ACTIVE_DAYS:
				return computeNumberOfDaysPerStation(session, dFilter);
			case ASSET_DAYS:
				return computeAssetDaysPerStation(session, dFilter);
			case INCIDENTS:
				return incidentsPerStation(session, dFilter);
			}
		}else if (groupBy == GroupByOption.LOCATION) {
			switch(c.getColumn()) {
			case ACTIVE_DAYS:
				return computeNumberOfDaysPerStationLocation(session, dFilter);
			case ASSET_DAYS:
				return computeAssetDaysPerStationLocation(session, dFilter);
			case INCIDENTS:
				return incidentsPerLocation(session, dFilter);
			}
		}
		return new HashMap<>();
	}



	
	private HashMap<UUID, Object> computeNumberOfDaysPerStation(Session session, Date[] dFilter){
		
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
				Long endDate = SharedUtils.getDatePart(new Date(), false).getTime();
				if (d.getEndDate() != null) {
					endDate = SharedUtils.getDatePart(d.getEndDate(), false).getTime();
				}
				
				AssetStation s = d.getStationLocation().getStation();
				HashSet<Long> dates= daysPerStation.get(s);
				if ( dates == null) {
					dates = new HashSet<>();
					daysPerStation.put(s, dates);
				}
				while(startDate <= endDate) {
					if (dFilter == null) {
						dates.add(startDate);
					}else if (startDate >= dFilter[0].getTime() && startDate <= dFilter[1].getTime()) {
						dates.add(startDate);
					}
					startDate += 86400000;
				}
			}
		}
		
		HashMap<UUID, Object> results = new HashMap<>();
		for (Entry<AssetStation, HashSet<Long>> e : daysPerStation.entrySet()) {
			results.put(e.getKey().getUuid(), e.getValue().size());
		}
		return results;
	}
	
	private  HashMap<UUID, Object> computeNumberOfDaysPerStationLocation(Session session, Date[] dFilter){
		
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
				Long endDate = SharedUtils.getDatePart(new Date(), false).getTime();
				if (d.getEndDate() != null) {
					endDate = SharedUtils.getDatePart(d.getEndDate(), false).getTime();
				}
				
				AssetStationLocation s = d.getStationLocation();
				HashSet<Long> dates= daysPerStation.get(s);
				if ( dates == null) {
					dates = new HashSet<>();
					daysPerStation.put(s, dates);
				}
				while(startDate <= endDate) {
					if (dFilter == null) {
						dates.add(startDate);
					}else if (startDate >= dFilter[0].getTime() && startDate <= dFilter[1].getTime()) {
						dates.add(startDate);
					}
					startDate += 86400000;
				}
			}
		}
		
		HashMap<UUID, Object> results = new HashMap<>();
		for (Entry<AssetStationLocation, HashSet<Long>> e : daysPerStation.entrySet()) {
			results.put(e.getKey().getUuid(), e.getValue().size());
		}
		return results;
	}
	
	
	private  HashMap<UUID, Object> computeAssetDaysPerStation(Session session, Date[] dFilter){
		
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
		
		HashMap<UUID, Object> daysCnt = new HashMap<>();
		
		try(ScrollableResults results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = (AssetDeployment) results.get(0);
				
				Long startDate = SharedUtils.getDatePart(d.getStartDate(), false).getTime();
				Long endDate = SharedUtils.getDatePart(new Date(), false).getTime();
				if (d.getEndDate() != null) {
					endDate = SharedUtils.getDatePart(d.getEndDate(), false).getTime();
				}
				
				AssetStation s = d.getStationLocation().getStation();
				int cnt = 0;
				
				while(startDate <= endDate) {
					if (dFilter == null) {
						cnt++;
					}else if (startDate >= dFilter[0].getTime() && startDate <= dFilter[1].getTime()) {
						cnt++;
					}
					startDate += 86400000;
				}

				Integer days = (Integer) daysCnt.get(s.getUuid());
				if (days == null) {
					days = cnt;
				}else {
					days += cnt;
				}
				daysCnt.put(s.getUuid(), days);
				
			}
		}
		return daysCnt;
	}
	
	private HashMap<UUID, Object> computeAssetDaysPerStationLocation(Session session, Date[] dFilter){
		
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
		
		
		HashMap<UUID, Object> daysCnt = new HashMap<>();
		
		try(ScrollableResults results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = (AssetDeployment) results.get(0);
				
				Long startDate = SharedUtils.getDatePart(d.getStartDate(), false).getTime();
				Long endDate = SharedUtils.getDatePart(new Date(), false).getTime();
				if (d.getEndDate() != null) {
					endDate = SharedUtils.getDatePart(d.getEndDate(), false).getTime();
				}
				
				AssetStationLocation s = d.getStationLocation();
				int cnt = 0;
				while(startDate <= endDate) {
					if (dFilter == null) {
						cnt++;
					}else if (startDate >= dFilter[0].getTime() && startDate <= dFilter[1].getTime()) {
						cnt++;
					}
					startDate += 86400000;
				}
				Integer days = (Integer) daysCnt.get(s.getUuid());
				if (days == null) {
					days = cnt;
				}else {
					days += cnt;
				}
				daysCnt.put(s.getUuid(), days);
				
			}
		}
		return daysCnt;
	}

	private HashMap<UUID, Object> incidentsPerStation(Session session, Date[] dFilter){
		
		HashMap<UUID, Object> results = new HashMap<>();
		
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
			results.put(stationUuid, cnt);
			
		}
		return results;
		
	}
	

	public static HashMap<UUID, Object> incidentsPerLocation(Session session, Date[] dFilter){
		
		HashMap<UUID, Object> results = new HashMap<>();
		
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
			results.put(locationUuid, cnt);
		}
		return results;
		
	}
	
}
