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
package org.wcs.smart.asset.engine;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Engine for compting asset deployment statistics.
 * 
 * @author Emily
 *
 */
public enum StatisticsEngine {

	INSTANCE;
	
	/**
	 * Supported statistics
	 * 
	 * @author Emily
	 *
	 */
	public static enum Statistic{
		/**
		 * Returns a long representing the number of incidents 
		 */
		NUMBER_INCIDENTS (Messages.StatisticsEngine_NumIncStatName),
		/**
		 * Returns a long representing the number of incidents with no observations 
		 */
		NUMBER_UNTAGGED (Messages.StatisticsEngine_NumUntaggedStatName),
		/**
		 * Returns  along representing the number of incidents that have not been validated
		 */
		NUMBER_NOT_VLIDATED(Messages.StatisticsEngine_NotValidateStatName),
		
		/**
		 * Returns a map of category to long that represents the number of incidents per category for
		 * all categories that have at least one observation 
		 */
		INCIDENTS_PER_CAT (Messages.StatisticsEngine_IncPerCatStatName);
		
		public String guiName;
		
		private Statistic(String guiName) {
			this.guiName = guiName;
		}
	}
	
	/**
	 * Compute the statistics returning the values appropriate for each statistic
	 * @param toCompute
	 * @param deployment
	 * @return
	 */
	public Map<Statistic, Object> computeStatistics(Set<Statistic> toCompute, AssetDeployment deployment) {
		Map<Statistic, Object> results = new HashMap<>();
		try(Session session = HibernateManager.openSession()){
			for (Statistic stat : toCompute) {
				Object value = computeStatistic(stat, deployment, session);
				results.put(stat,  value);
			}
		}
		return results;
	}
	
	private Object computeStatistic(Statistic item, AssetDeployment deployment, Session session) {
		try {
			switch(item) {
			case INCIDENTS_PER_CAT:
				return computeStatsPerCategory(session, deployment);
			case NUMBER_INCIDENTS:
				return computeNumberOfIncidents(session, deployment);
			case NUMBER_UNTAGGED:
				return computeNumberOfUnTagged(session, deployment);
			case NUMBER_NOT_VLIDATED:
				return computeNumberNotValidated(session, deployment);
			}
			return ""; //$NON-NLS-1$
		}catch (Throwable ex) {
			AssetPlugIn.log("Error computing statistic value: " + item.name(), ex); //$NON-NLS-1$
			return MessageFormat.format(Messages.StatisticsEngine_ComputeError, item.name());
		}
	}
	
	private Long computeNumberOfIncidents(Session session, AssetDeployment deployment) {
		String hql = "SELECT count(distinct aw.id.waypoint) FROM AssetWaypoint aw WHERE assetDeployment = :deployment"; //$NON-NLS-1$
		Query<?> query = session.createQuery(hql).setParameter("deployment", deployment); //$NON-NLS-1$
		Long cnt = (Long) query.uniqueResult();
		return cnt;
	}
	
	private Long computeNumberNotValidated(Session session, AssetDeployment deployment) {
		String hql = "SELECT count(distinct aw.id.waypoint) FROM AssetWaypoint aw WHERE state = :state and aw.assetDeployment = :deployment"; //$NON-NLS-1$
		Query<?> query = session.createQuery(hql)
				.setParameter("state",  AssetWaypoint.State.DIRTY) //$NON-NLS-1$
				.setParameter("deployment", deployment); //$NON-NLS-1$
		Long cnt = (Long) query.uniqueResult();
		return cnt;
	}
	
	private Long computeNumberOfUnTagged(Session session, AssetDeployment deployment) {
		String hql = "SELECT count(distinct aw.id.waypoint) FROM AssetWaypoint aw JOIN aw.id.waypoint w WHERE aw.assetDeployment=:deployment AND w.uuid NOT IN (SELECT waypoint.uuid FROM WaypointObservationGroup)"; //$NON-NLS-1$
		Query<?> query = session.createQuery(hql).setParameter("deployment",  deployment); //$NON-NLS-1$
		Long cnt = (Long) query.uniqueResult();
		return cnt;
	}
	
	@SuppressWarnings("unchecked")
	private List<Object[]> computeStatsPerCategory(Session session, AssetDeployment deployment) {
		String sql = "SELECT c.category_uuid, count(*) FROM  (SELECT distinct a.uuid, c.CATEGORY_UUID FROM smart.asset_waypoint aw join smart.waypoint a on aw.wp_uuid = a.uuid join smart.wp_observation_group g on a.uuid = g.wp_uuid join smart.WP_OBSERVATION c on g.uuid = c.wp_group_uuid and aw.asset_deployment_uuid = :uuid) c group by c.category_uuid"; //$NON-NLS-1$
		
		List<Object> data = session.createNativeQuery(sql).setParameter("uuid", deployment.getUuid()).list(); //$NON-NLS-1$
		
		List<Object[]> categoryCnts = new ArrayList<>();
		HashMap<Category, Long> cnts = new HashMap<>();
		
		for (Object item : data) {
			Object[] items = (Object[])item;
			UUID categoryUuid = UuidUtils.byteToUUID((byte[])items[0]);
			Integer cnt = (Integer) items[1];
			
			Category c = session.get(Category.class, categoryUuid);
			
			Category cc = c;
			while(cc != null) {
				Long value = cnts.get(cc);
				if (value != null) {
					value += cnt;
				}else {
					value = cnt.longValue();
				}
				cnts.put(cc, value);
				cc = cc.getParent();
				if (cc != null) cc.getFullCategoryName();
			}
		}
		List<Category> allCategories = new ArrayList<>();
		allCategories.addAll(cnts.keySet());
		allCategories.sort((a,b)->{
			Integer l1 = Category.hkeyLength(a.getHkey());
			Integer l2 = Category.hkeyLength(b.getHkey());
			if (l1.equals(l2)) {
				return Integer.valueOf(a.getCategoryOrder()).compareTo(b.getCategoryOrder());
			}
			return l2.compareTo(l1);
		});
		
		for (Category c : allCategories) {
			categoryCnts.add(new Object[] {c, cnts.get(c)});
		}
		
		return categoryCnts;
	}
	
	
	
	
	/**
	 * Compute the statistics for current all active deployments at the given station.  Only
	 * active deployments are included.
	 * 
	 * @param toCompute
	 * @param deployment
	 * @return
	 */
	public Map<Statistic, Object> computeActiveStatistics(Set<Statistic> toCompute, AssetStation station) {
		Map<Statistic, Object> results = new HashMap<>();
		try(Session session = HibernateManager.openSession()){
			for (Statistic stat : toCompute) {
				Object value = computeStatistic(stat, station, session);
				results.put(stat,  value);
			}
		}
		return results;
	}
	
	private Object computeStatistic(Statistic item, AssetStation station, Session session) {
		try {
			switch(item) {
			case INCIDENTS_PER_CAT:
				return computeStatsPerCategory(session, station);
			case NUMBER_INCIDENTS:
				return computeNumberOfIncidents(session, station);
			case NUMBER_UNTAGGED:
				return computeNumberOfUnTagged(session, station);
			case NUMBER_NOT_VLIDATED:
				return computeNumberNotValidated(session, station);
			}
			
			return ""; //$NON-NLS-1$
		}catch (Throwable ex) {
			AssetPlugIn.log("Error computing statistic value: " + item.name(), ex); //$NON-NLS-1$
			return MessageFormat.format(Messages.StatisticsEngine_ComputeError, item.name());
		}
	}
	
	private Long computeNumberOfIncidents(Session session, AssetStation station) {
		String hql = "SELECT count(distinct a.id.waypoint.uuid) FROM AssetWaypoint a join a.id.assetDeployment b WHERE b.stationLocation.station = :station"; //$NON-NLS-1$
		return (Long)session.createQuery(hql).setParameter("station", station).uniqueResult(); //$NON-NLS-1$
		
	}
	
	private Long computeNumberNotValidated(Session session, AssetStation station) {
		String hql = "SELECT count(distinct a.id.waypoint.uuid) FROM AssetWaypoint a join a.id.assetDeployment b WHERE a.state = :state and b.stationLocation.station = :station"; //$NON-NLS-1$
		Query<?> query = session.createQuery(hql).setParameter("state",  AssetWaypoint.State.DIRTY).setParameter("station", station); //$NON-NLS-1$ //$NON-NLS-2$
		Long cnt = (Long) query.uniqueResult();
		return cnt;
	}
	
	private Long computeNumberOfUnTagged(Session session, AssetStation station) {
		String hql = "SELECT count(distinct aw.id.waypoint.uuid) FROM AssetWaypoint aw JOIN aw.id.assetDeployment d JOIN aw.id.waypoint w WHERE d.stationLocation.station=:station AND w.uuid NOT IN (SELECT waypoint.uuid FROM WaypointObservationGroup)"; //$NON-NLS-1$
		Query<?> query = session.createQuery(hql).setParameter("station",  station); //$NON-NLS-1$
		Long cnt = (Long) query.uniqueResult();
		return cnt;
	}
	
	@SuppressWarnings("unchecked")
	private List<Object[]> computeStatsPerCategory(Session session, AssetStation station) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT c.category_uuid, count(*) FROM  ("); //$NON-NLS-1$
		sb.append("SELECT distinct a.uuid, c.CATEGORY_UUID ");  //$NON-NLS-1$
		sb.append(" FROM smart.asset_waypoint aw join smart.ASSET_DEPLOYMENT d on aw.asset_deployment_uuid = d.uuid ");  //$NON-NLS-1$
		sb.append(" join smart.asset_station_location l on l.uuid = d.station_location_uuid and l.station_uuid = :station ");  //$NON-NLS-1$
		sb.append(" join smart.waypoint a on aw.wp_uuid = a.uuid "); //$NON-NLS-1$
		sb.append(" join smart.wp_observation_group g on g.wp_uuid = a.uuid "); //$NON-NLS-1$
		sb.append(" join smart.WP_OBSERVATION c on g.uuid = c.wp_group_uuid ) c group by c.category_uuid"); //$NON-NLS-1$
		
		List<Object> data = session.createNativeQuery(sb.toString()).setParameter("station", station.getUuid()).list(); //$NON-NLS-1$
		
		List<Object[]> categoryCnts = new ArrayList<>();
		HashMap<Category, Long> cnts = new HashMap<>();
		
		for (Object item : data) {
			Object[] items = (Object[])item;
			UUID categoryUuid = UuidUtils.byteToUUID((byte[])items[0]);
			Integer cnt = (Integer) items[1];
			
			Category c = session.get(Category.class, categoryUuid);
			
			Category cc = c;
			while(cc != null) {
				Long value = cnts.get(cc);
				if (value != null) {
					value += cnt;
				}else {
					value = cnt.longValue();
				}
				cnts.put(cc, value);
				cc = cc.getParent();
				if (cc != null) cc.getFullCategoryName();
			}
		}
		List<Category> allCategories = new ArrayList<>();
		allCategories.addAll(cnts.keySet());
		allCategories.sort((a,b)->{
			Integer l1 = Category.hkeyLength(a.getHkey());
			Integer l2 = Category.hkeyLength(b.getHkey());
			if (l1.equals(l2)) {
				return Integer.valueOf(a.getCategoryOrder()).compareTo(b.getCategoryOrder());
			}
			return l2.compareTo(l1);
		});
		
		for (Category c : allCategories) {
			categoryCnts.add(new Object[] {c, cnts.get(c)});
		}
		
		return categoryCnts;
	}
	
	
	
	/**
	 * Compute the statistics for current all active deployments at the given station.  Only
	 * active deployments are included.
	 * 
	 * @param toCompute
	 * @param deployment
	 * @return
	 */
	public Map<Statistic, Object> computeActiveStatistics(Set<Statistic> toCompute, AssetStationLocation location) {
		Map<Statistic, Object> results = new HashMap<>();
		try(Session session = HibernateManager.openSession()){
			for (Statistic stat : toCompute) {
				Object value = computeStatistic(stat, location, session);
				results.put(stat,  value);
			}
		}
		return results;
	}
	
	private Object computeStatistic(Statistic item, AssetStationLocation location, Session session) {
		try {
			switch(item) {
			case INCIDENTS_PER_CAT:
				return computeStatsPerCategory(session, location);
			case NUMBER_INCIDENTS:
				return computeNumberOfIncidents(session, location);
			case NUMBER_UNTAGGED:
				return computeNumberOfUnTagged(session, location);
			case NUMBER_NOT_VLIDATED:
				return computeNumberNotValidated(session, location);
			}
			return ""; //$NON-NLS-1$
		}catch (Throwable ex) {
			AssetPlugIn.log("Error computing statistic value: " + item.name(), ex); //$NON-NLS-1$
			return MessageFormat.format(Messages.StatisticsEngine_ComputeError, item.name());
		}
	}
	
	private Long computeNumberOfIncidents(Session session, AssetStationLocation location) {
		String hql = "SELECT count(distinct a.id.waypoint.uuid) FROM AssetWaypoint a join a.id.assetDeployment b WHERE b.stationLocation = :location"; //$NON-NLS-1$
		return (Long)session.createQuery(hql).setParameter("location", location).uniqueResult(); //$NON-NLS-1$
		
	}
	private Long computeNumberNotValidated(Session session, AssetStationLocation location) {
		String hql = "SELECT count(distinct a.id.waypoint.uuid) FROM AssetWaypoint a join a.id.assetDeployment b WHERE a.state = :state and b.stationLocation = :location"; //$NON-NLS-1$
		Query<?> query = session.createQuery(hql).setParameter("state",  AssetWaypoint.State.DIRTY).setParameter("location", location); //$NON-NLS-1$ //$NON-NLS-2$
		Long cnt = (Long) query.uniqueResult();
		return cnt;
	}
	
	private Long computeNumberOfUnTagged(Session session, AssetStationLocation location) {
		String hql = "SELECT count(distinct aw.id.waypoint.uuid) FROM AssetWaypoint aw JOIN aw.id.assetDeployment d JOIN aw.id.waypoint w WHERE d.stationLocation=:location AND w.uuid NOT IN (SELECT waypoint.uuid FROM WaypointObservationGroup)"; //$NON-NLS-1$
		Query<?> query = session.createQuery(hql).setParameter("location",  location); //$NON-NLS-1$
		Long cnt = (Long) query.uniqueResult();
		return cnt;
	}
	
	@SuppressWarnings("unchecked")
	private List<Object[]> computeStatsPerCategory(Session session, AssetStationLocation location) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT c.category_uuid, count(*) FROM  ("); //$NON-NLS-1$
		sb.append("SELECT distinct a.uuid, c.CATEGORY_UUID ");  //$NON-NLS-1$
		sb.append(" FROM smart.asset_waypoint aw join smart.ASSET_DEPLOYMENT d on aw.asset_deployment_uuid = d.uuid ");  //$NON-NLS-1$
		sb.append(" AND d.station_location_uuid = :location ");  //$NON-NLS-1$
		sb.append(" join smart.waypoint a on aw.wp_uuid = a.uuid "); //$NON-NLS-1$
		sb.append(" join smart.wp_observation_group g on g.wp_uuid = a.uuid "); //$NON-NLS-1$
		sb.append(" join smart.WP_OBSERVATION c on g.uuid = c.wp_group_uuid ) c group by c.category_uuid"); //$NON-NLS-1$
		
		List<Object> data = session.createNativeQuery(sb.toString()).setParameter("location", location.getUuid()).list(); //$NON-NLS-1$
		
		List<Object[]> categoryCnts = new ArrayList<>();
		HashMap<Category, Long> cnts = new HashMap<>();
		
		for (Object item : data) {
			Object[] items = (Object[])item;
			UUID categoryUuid = UuidUtils.byteToUUID((byte[])items[0]);
			Integer cnt = (Integer) items[1];
			
			Category c = session.get(Category.class, categoryUuid);
			
			Category cc = c;
			while(cc != null) {
				Long value = cnts.get(cc);
				if (value != null) {
					value += cnt;
				}else {
					value = cnt.longValue();
				}
				cnts.put(cc, value);
				cc = cc.getParent();
				if (cc != null) cc.getFullCategoryName();
			}
		}
		List<Category> allCategories = new ArrayList<>();
		allCategories.addAll(cnts.keySet());
		allCategories.sort((a,b)->{
			Integer l1 = Category.hkeyLength(a.getHkey());
			Integer l2 = Category.hkeyLength(b.getHkey());
			if (l1.equals(l2)) {
				return Integer.valueOf(a.getCategoryOrder()).compareTo(b.getCategoryOrder());
			}
			return l2.compareTo(l1);
		});
		
		for (Category c : allCategories) {
			categoryCnts.add(new Object[] {c, cnts.get(c)});
		}
		
		return categoryCnts;
	}
}
