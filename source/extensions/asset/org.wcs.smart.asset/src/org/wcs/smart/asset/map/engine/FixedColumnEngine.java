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
package org.wcs.smart.asset.map.engine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentDisruption;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.views.map.FixedColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;

/**
 * Engine for computing the values for fixed columns.
 * 
 * @author Emily
 *
 */
public class FixedColumnEngine implements IColumnEngine {

	private LocalDate[] dFilter;
	private IOverviewTableColumn.GroupByOption groupBy;
	private Session session;
	private ConservationArea ca;
	
	private HashMap<UUID, Object> numberOfDaysStation = null;
	private HashMap<UUID, Object> numberOfAssetDaysStation  = null;
	private HashMap<UUID, Object> numberOfDaysLocation  = null;
	private HashMap<UUID, Object> numberOfAssetDaysLocation = null;
	
	public FixedColumnEngine(LocalDate[] dFilter, IOverviewTableColumn.GroupByOption groupBy, ConservationArea ca, Session session) {
		this.dFilter = dFilter;
		this.session = session;
		this.groupBy = groupBy;
		this.ca = ca;
	}
	
	
	@Override
	public boolean canProcess(IOverviewTableColumn column) {
		if (column instanceof FixedColumn) {
			switch(((FixedColumn) column).getColumn()) {
			case ACTIVE_DAYS:
			case ASSET_DAYS:
			case INCIDENTS:
				return true;
			default:
				return false;
			}
			
		}
		return false;
	}
	
	@Override
	public HashMap<UUID, Object> computeValues(IOverviewTableColumn column) {		
		FixedColumn c = (FixedColumn) column;
		if (groupBy == GroupByOption.STATION) {
			switch(c.getColumn()) {
			case ACTIVE_DAYS:
				computeNumberOfPerStation(session, dFilter);
				return numberOfDaysStation;
			case ASSET_DAYS:
				computeNumberOfPerStation(session, dFilter);
				return numberOfAssetDaysStation;
			case INCIDENTS:
				return incidentsPerStation(session, dFilter);
			default:
			}
		}else if (groupBy == GroupByOption.LOCATION) {
			switch(c.getColumn()) {
			case ACTIVE_DAYS:
				computeNumberOfPerLocation(session, dFilter);
				return numberOfDaysLocation;
			case ASSET_DAYS:
				computeNumberOfPerLocation(session, dFilter);
				return numberOfAssetDaysLocation;
			case INCIDENTS:
				return incidentsPerLocation(session, dFilter);
			default:
			}
		}
		return new HashMap<>();
	}
	
	private void computeNumberOfPerStation(Session session, LocalDate[] dFilter){
		if (numberOfDaysStation != null) return;
		
		Query<AssetDeployment> query = null;
		LocalDate filterStart = null;
		LocalDate filterEnd = null;
		if (dFilter != null) {
			String hql = "FROM AssetDeployment a WHERE a.asset.conservationArea = :ca and startDate <= :endDate and (endDate is null or endDate >= :startDate)"; //$NON-NLS-1$
			query = session.createQuery(hql, AssetDeployment.class)
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("endDate", dFilter[1].atTime(LocalTime.MAX)) //$NON-NLS-1$
				.setParameter("startDate", dFilter[0].atStartOfDay()); //$NON-NLS-1$
			
			filterStart = dFilter[0];
			filterEnd= dFilter[1];
		}else {
			String hql = "FROM AssetDeployment a WHERE a.asset.conservationArea = :ca "; //$NON-NLS-1$
			query = session.createQuery(hql, AssetDeployment.class)
					.setParameter("ca",  ca);	 //$NON-NLS-1$
		}
		
		HashMap<AssetStation, HashSet<Long>> daysPerStation = new HashMap<>();
		HashMap<UUID, Object> daysCnt = new HashMap<>();

		try(ScrollableResults<AssetDeployment> results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = results.get();
				
				LocalDate startDate = d.getStartDate().toLocalDate();
				LocalDate endDate = LocalDate.now();
				if (d.getEndDate() != null && d.getEndDate().toLocalDate().isBefore(LocalDate.now())) {
					endDate = d.getEndDate().toLocalDate();
				}
				
				AssetStation s = d.getStationLocation().getStation();
				HashSet<Long> dates= daysPerStation.get(s);
				if ( dates == null) {
					dates = new HashSet<>();
					daysPerStation.put(s, dates);
				}
				int cnt = 0;
				while(!startDate.isAfter(endDate)) {
					
					//if startDate is wholly contained within a asset disruption then we don't want to 
					//count this day
					LocalDateTime startOfDay = startDate.atStartOfDay();
					LocalDateTime endOfDay = startDate.atTime(LocalTime.MAX);
					boolean skipDay = false;
					
					for (AssetDeploymentDisruption dd : d.getDisruptions()) {
						
						if (startOfDay.isAfter(dd.getStartDate())
								&& endOfDay.isBefore(dd.getEndDate()) ) {
							//skip this day 
							skipDay = true;
							break;
						}
					}
					
					if (!skipDay) {
						if (dFilter == null) {
							dates.add(startDate.toEpochDay());
							cnt++;
						}else if (!startDate.isBefore(filterStart) && !startDate.isAfter(filterEnd)) {
							dates.add(startDate.toEpochDay());
							cnt++;
						}
					}
					startDate = startDate.plus(1, ChronoUnit.DAYS);
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
		
		HashMap<UUID, Object> results = new HashMap<>();
		for (Entry<AssetStation, HashSet<Long>> e : daysPerStation.entrySet()) {
			results.put(e.getKey().getUuid(), e.getValue().size());
		}
		numberOfAssetDaysStation = daysCnt;
		numberOfDaysStation = results;
		return;
	}
	private void computeNumberOfPerLocation(Session session, LocalDate[] dFilter){
		if (numberOfDaysLocation!= null) return;
		
		Query<AssetDeployment> query = null;
		LocalDate filterStart = null;
		LocalDate filterEnd = null;
		if (dFilter != null) {
			String hql = "FROM AssetDeployment a WHERE a.asset.conservationArea = :ca  AND startDate <= :endDate and (endDate is null or endDate >= :startDate)"; //$NON-NLS-1$
			query = session.createQuery(hql, AssetDeployment.class)
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("endDate", dFilter[1].atTime(LocalTime.MAX)) //$NON-NLS-1$
				.setParameter("startDate", dFilter[0].atStartOfDay()); //$NON-NLS-1$
			
			filterStart = dFilter[0];
			filterEnd= dFilter[1];
		}else {
			String hql = "FROM AssetDeployment a WHERE a.asset.conservationArea = :ca "; //$NON-NLS-1$
			query = session.createQuery(hql, AssetDeployment.class)
					.setParameter("ca", ca);	 //$NON-NLS-1$
		}
		
		HashMap<AssetStationLocation, HashSet<Long>> daysPerStation = new HashMap<>();
		HashMap<UUID, Object> daysCnt = new HashMap<>();

		try(ScrollableResults<AssetDeployment> results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = results.get();
				
				LocalDate startDate = d.getStartDate().toLocalDate();
				LocalDate endDate = LocalDate.now();
				if (d.getEndDate() != null && d.getEndDate().toLocalDate().isBefore(LocalDate.now())) {
					endDate = d.getEndDate().toLocalDate();
				}
				
				AssetStationLocation s = d.getStationLocation();
				HashSet<Long> dates= daysPerStation.get(s);
				if ( dates == null) {
					dates = new HashSet<>();
					daysPerStation.put(s, dates);
				}
				int cnt = 0;
				while(!startDate.isAfter(endDate)) {
					
					LocalDateTime startOfDay = startDate.atStartOfDay();
					LocalDateTime endOfDay = startDate.atTime(LocalTime.MAX);
					boolean skipDay = false;
					for (AssetDeploymentDisruption dd : d.getDisruptions()) {
						
						if (startOfDay.isAfter(dd.getStartDate())
								&& endOfDay.isBefore(dd.getEndDate())) {
							//skip this day 
							skipDay = true;
							break;
						}
					}
					
					if (!skipDay) {
						if (dFilter == null) {
							dates.add(startDate.toEpochDay());
							cnt++;
						}else if (!startDate.isBefore(filterStart) && !startDate.isAfter(filterEnd)) {
							dates.add(startDate.toEpochDay());
							cnt++;
						}
					}
					startDate = startDate.plus(1, ChronoUnit.DAYS);
					
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
		
		HashMap<UUID, Object> results = new HashMap<>();
		for (Entry<AssetStationLocation, HashSet<Long>> e : daysPerStation.entrySet()) {
			results.put(e.getKey().getUuid(), e.getValue().size());
		}
		numberOfAssetDaysLocation = daysCnt;
		numberOfDaysLocation = results;
		return;
	}

	private HashMap<UUID, Object> incidentsPerStation(Session session, LocalDate[] dFilter){
		
		HashMap<UUID, Object> results = new HashMap<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT uuid, count(wp_uuid) FROM "); //$NON-NLS-1$
		sb.append("("); //$NON-NLS-1$
		sb.append("SELECT DISTINCT s.uuid as uuid, wp.uuid as wp_uuid "); //$NON-NLS-1$
		sb.append("FROM smart.asset_waypoint a JOIN smart.waypoint wp ON a.wp_uuid = wp.uuid "); //$NON-NLS-1$
		sb.append(" JOIN smart.asset_deployment d on a.asset_deployment_uuid = d.uuid "); //$NON-NLS-1$
		sb.append(" JOIN smart.asset_station_location l on d.station_location_uuid = l.uuid "); //$NON-NLS-1$
		sb.append(" JOIN smart.asset_station s on l.station_uuid = s.uuid "); //$NON-NLS-1$
		sb.append(" WHERE s.ca_uuid = :ca "); //$NON-NLS-1$
		if (dFilter != null) {
			sb.append(" AND "); //$NON-NLS-1$
			sb.append(" wp.dateTime >= :startDate and wp.dateTime <= :endDate " ); //$NON-NLS-1$
		}
		sb.append(") as foo "); //$NON-NLS-1$
		sb.append(" GROUP BY uuid"); //$NON-NLS-1$
		
		Query<Tuple> query = session.createNativeQuery(sb.toString(), Tuple.class);
		query.setParameter("ca", ca.getUuid()); //$NON-NLS-1$
		if (dFilter != null) {
				query.setParameter("startDate", dFilter[0].atStartOfDay()) //$NON-NLS-1$
				.setParameter("endDate", dFilter[1].atTime(LocalTime.MAX)); //$NON-NLS-1$
		}
		List<Tuple> qresults = query.list();
		for (Tuple result : qresults) {
			UUID stationUuid = UuidUtils.byteToUUID( (byte[])result.get(0) );
			Integer cnt = (Integer) result.get(1);
			results.put(stationUuid, cnt);
			
		}
		return results;
		
	}
	

	private HashMap<UUID, Object> incidentsPerLocation(Session session, LocalDate[] dFilter){
		
		HashMap<UUID, Object> results = new HashMap<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT uuid, count(wp_uuid) FROM "); //$NON-NLS-1$
		sb.append("("); //$NON-NLS-1$
		sb.append("SELECT DISTINCT l.uuid as uuid, wp.uuid as wp_uuid "); //$NON-NLS-1$
		sb.append("FROM smart.asset_waypoint a JOIN smart.waypoint wp ON a.wp_uuid = wp.uuid "); //$NON-NLS-1$
		sb.append(" JOIN smart.asset_deployment d on a.asset_deployment_uuid = d.uuid "); //$NON-NLS-1$
		sb.append(" JOIN smart.asset_station_location l on d.station_location_uuid = l.uuid "); //$NON-NLS-1$
		sb.append(" JOIN smart.asset_station s on l.station_uuid = s.uuid "); //$NON-NLS-1$
		sb.append(" WHERE s.ca_uuid = :ca "); //$NON-NLS-1$
		if (dFilter != null) {		
			sb.append(" AND wp.dateTime >= :startDate and wp.dateTime <= :endDate " ); //$NON-NLS-1$
		}
		sb.append(") as foo "); //$NON-NLS-1$
		sb.append(" GROUP BY uuid"); //$NON-NLS-1$
		
		Query<Tuple> query = session.createNativeQuery(sb.toString(), Tuple.class);
		query.setParameter("ca", ca); //$NON-NLS-1$
		if (dFilter != null) {
				query.setParameter("startDate", dFilter[0]) //$NON-NLS-1$
				.setParameter("endDate", dFilter[1]); //$NON-NLS-1$
		}
		List<Tuple> qresults = query.list();
		for (Tuple result : qresults) {
			UUID locationUuid = UuidUtils.byteToUUID( (byte[])result.get(0) );
			Integer cnt = (Integer) result.get(1);
			results.put(locationUuid, cnt);
		}
		return results;
		
	}
	
}
