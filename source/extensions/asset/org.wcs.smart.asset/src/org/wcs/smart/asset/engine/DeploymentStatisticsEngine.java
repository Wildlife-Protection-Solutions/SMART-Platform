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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Engine for compting asset deployment statistics.
 * 
 * @author Emily
 *
 */
public enum DeploymentStatisticsEngine {

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
		NUMBER_INCIDENTS ("Number of Incidents"),
		/**
		 * Returns a long representing the number of incidents with no observations 
		 */
		NUMBER_UNTAGGED ("Number of Untagged Incidents"),
		/**
		 * Returns a map of category to long that represents the number of incidents per category for
		 * all categories that have at least one observation 
		 */
		INCIDENTS_PER_CAT ("Incidents per Category");
		
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
			}
			return "";
		}catch (Throwable ex) {
			AssetPlugIn.log("Error computing statistic value: " + item.name(), ex);
			return "Error computing statistic";
		}
	}
	
	private Long computeNumberOfIncidents(Session session, AssetDeployment deployment) {
		return QueryFactory.buildCountQuery(session, AssetWaypoint.class, new Object[] {"id.assetDeployment", deployment});
	}
	
	private Long computeNumberOfUnTagged(Session session, AssetDeployment deployment) {
		String hql = "SELECT count(*) FROM AssetWaypoint aw JOIN aw.id.waypoint w WHERE aw.id.assetDeployment=:deployment AND w.uuid NOT IN (SELECT waypoint.uuid FROM WaypointObservation)";
		Query<?> query = session.createQuery(hql).setParameter("deployment",  deployment);
		Long cnt = (Long) query.uniqueResult();
		return cnt;
	}
	
	private List<Object[]> computeStatsPerCategory(Session session, AssetDeployment deployment) {
		String sql = "SELECT c.category_uuid, count(*) FROM  (SELECT distinct a.uuid, c.CATEGORY_UUID FROM smart.asset_waypoint aw join smart.waypoint a on aw.wp_uuid = a.uuid join smart.WP_OBSERVATION c on a.uuid = c.WP_UUID and aw.asset_deployment_uuid = :uuid) c group by c.category_uuid";
		
		List<Object> data = session.createNativeQuery(sql).setParameter("uuid", deployment.getUuid()).list();
		
		List<Object[]> categoryCnts = new ArrayList<>();
		HashMap<Category, Long> cnts = new HashMap<>();
		
		for (Object item : data) {
			Object[] items = (Object[])item;
			UUID categoryUuid = (UUID)items[0];
			Long cnt = (Long) items[1];
			
			Category c = session.get(Category.class, categoryUuid);
			
			Category cc = c;
			while(cc != null) {
				Long value = cnts.get(cc);
				if (value != null) {
					value += cnt;
				}else {
					value = cnt;
				}
				cnts.put(cc, value);
				cc = cc.getParent();
				cc.getParent().getFullCategoryName();
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
