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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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

/**
 * Engine for computing various status values for stations and locations
 * 
 * @author Emily
 *
 */
public class StatusEngine {

	
	/**
	 * Depending  on the group by option this will return a map of the station or location
	 * to the epoch dates that contain active deployments for the station/location
	 *  
	 * @param session
	 * @param dFilter
	 * @param ca
	 * @param groupBy
	 * @return
	 */
	public HashMap<Object, Set<Long>> computeStatus(Session session, LocalDate[] dFilter, ConservationArea ca, GroupByOption groupBy){
		if (groupBy == GroupByOption.LOCATION) {
			return computeStationLocationStatus(session, ca, dFilter);
		}else {
			return computeStationStatus(session, ca, dFilter);
		}
	}
	
	private HashMap<Object, Set<Long>> computeStationStatus(Session session, ConservationArea ca, LocalDate[] dFilter){
		HashMap<Object, Set<Long>> data = computeStatus(session, dFilter, true, ca);
		List<AssetStation> stations = QueryFactory.buildQuery(session,  AssetStation.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		
		for (AssetStation ss : stations) {
			if (data.containsKey(ss)) continue;
			data.put(ss, new HashSet<>());
		}
		return data;
	}
	
	private HashMap<Object, Set<Long>> computeStationLocationStatus(Session session, ConservationArea ca, LocalDate[] dFilter){
		HashMap<Object, Set<Long>> data = computeStatus(session, dFilter, false, ca);
		List<AssetStation> stations = QueryFactory.buildQuery(session,  AssetStation.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		
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
	private <T> HashMap<T, Set<Long>> computeStatus(Session session, LocalDate[] dFilter, boolean isStation, ConservationArea ca){
		
		Query<AssetDeployment> query = null;
		if (dFilter != null) {
			String hql = "FROM AssetDeployment a WHERE a.asset.conservationArea = :ca and startDate <= :endDate and (endDate is null or endDate >= :startDate)"; //$NON-NLS-1$
			query = session.createQuery(hql, AssetDeployment.class)
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("endDate", dFilter[1].atStartOfDay()) //$NON-NLS-1$
				.setParameter("startDate", dFilter[0].atTime(LocalTime.MAX)); //$NON-NLS-1$
		}else {
			String hql = "FROM AssetDeployment a WHERE a.asset.conservationArea = :ca"; //$NON-NLS-1$
			query = session.createQuery(hql, AssetDeployment.class)
					.setParameter("ca", ca); //$NON-NLS-1$
		}
		
		HashMap<T, Set<Long>> data = new HashMap<>();
		
		for (AssetDeployment d : query.list()) {
			LocalDate startDate = d.getStartDate().toLocalDate();
			LocalDate endDate = LocalDate.now();
			
			if (d.getEndDate() != null) {
				endDate = d.getEndDate().toLocalDate();
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
		
		return data;
	}

	private Object getId(AssetDeployment d, boolean isStation, Session session) {
		d.getStationLocation().computeStatus(session);
		d.getStationLocation().getStation().computeStatus(session);
		if (isStation) return d.getStationLocation().getStation();
		return d.getStationLocation();
	}
	
	
}
