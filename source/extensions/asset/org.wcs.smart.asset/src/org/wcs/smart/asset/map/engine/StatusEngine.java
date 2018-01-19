package org.wcs.smart.asset.map.engine;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.QueryFactory;

public class StatusEngine {

	
	public HashMap<Object, Set<Long>> computeStatus(Session session, Date[] dFilter, ConservationArea ca, GroupByOption groupBy){
		if (groupBy == GroupByOption.LOCATION) {
			return computeStationLocationStatus(session, ca, dFilter);
		}else {
			return computeStationStatus(session, ca, dFilter);
		}
	}
	
	private HashMap<Object, Set<Long>> computeStationStatus(Session session, ConservationArea ca, Date[] dFilter){
		HashMap<Object, Set<Long>> data = computeStatus(session, dFilter, true, ca);
		List<AssetStation> stations = QueryFactory.buildQuery(session,  AssetStation.class, 
				new Object[] {"conservationArea", ca}).list();
		
		for (AssetStation ss : stations) {
			if (data.containsKey(ss)) continue;
			data.put(ss, new HashSet<>());
		}
		return data;
	}
	
	private HashMap<Object, Set<Long>> computeStationLocationStatus(Session session, ConservationArea ca, Date[] dFilter){
		HashMap<Object, Set<Long>> data = computeStatus(session, dFilter, false, ca);
		List<AssetStation> stations = QueryFactory.buildQuery(session,  AssetStation.class, 
				new Object[] {"conservationArea", ca}).list();
		
		for (AssetStation ss : stations) {
			ss.computeStatus(session);
			for (AssetStationLocation l : ss.getLocations()) {
				if (data.containsKey(l)) continue;
				data.put(l, new HashSet<>());
			}
		}
		return data;
	}
	
	@SuppressWarnings("unchecked")
	private <T> HashMap<T, Set<Long>> computeStatus(Session session, Date[] dFilter, boolean isStation, ConservationArea ca){
		
		Query<AssetDeployment> query = null;
		if (dFilter != null) {
			String hql = "FROM AssetDeployment a WHERE a.asset.conservationArea = :ca and startDate <= :endDate and (endDate is null or endDate >= :startDate)";
			query = session.createQuery(hql, AssetDeployment.class)
				.setParameter("ca", ca)
				.setParameter("endDate", dFilter[1])
				.setParameter("startDate", dFilter[0]);
		}else {
			String hql = "FROM AssetDeployment a WHERE a.asset.conservationArea = :ca";
			query = session.createQuery(hql, AssetDeployment.class)
					.setParameter("ca", ca);
		}
		
		HashMap<T, Set<Long>> data = new HashMap<>();
		
		try(ScrollableResults results = query.scroll()){
			while(results.next()) {
				AssetDeployment d = (AssetDeployment) results.get(0);
				
				LocalDate startDate = new java.sql.Date( d.getStartDate().getTime() ).toLocalDate();
				LocalDate endDate = LocalDate.now();
				
				if (d.getEndDate() != null) {
					endDate = new java.sql.Date( d.getEndDate().getTime() ).toLocalDate();
				}
				
				T s = (T) getId(d, isStation, session);
				Set<Long> items = data.get(s);
				if (items == null) {
					items = new HashSet<>();
					data.put(s, items);
				}
				LocalDate temp = LocalDate.from(startDate);
				while(!temp.isAfter(endDate)) {
					items.add(temp.toEpochDay());
					temp = temp.plus(1, ChronoUnit.DAYS);
				}
			}
		}
		return data;
	}

	private Object getId(AssetDeployment d, boolean isStation, Session session) {
		d.getStationLocation().computeStatus(session);
		d.getStationLocation().getStation().computeStatus(session);
		if (isStation) return d.getStationLocation().getStation();
		return d.getStationLocation();
	}
	
	
}
