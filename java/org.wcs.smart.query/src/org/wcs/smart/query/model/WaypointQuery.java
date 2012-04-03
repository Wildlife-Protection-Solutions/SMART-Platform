/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.DerbyQueryEngine;
import org.wcs.smart.query.parser.internal.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.Filter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.ui.querytable.QueryResultsTableColumn;
import org.wcs.smart.query.ui.querytable.column.QueryTableColumn;

/**
 * 
 * @author Emily
 * @since 1.0.0
 */
public class WaypointQuery extends Query{
	
	private String strQueryFilter;
	private String strDateFilter;
	
	private Filter queryFilter;
	private ConservationAreaFilter caFilter;
	
	private List<QueryTableColumn> tableColumns = new ArrayList<QueryTableColumn>();
	
	
	/**
	 * Creates a new waypoint query with the default
	 * conservation area filter. 
	 */
	public WaypointQuery(){
		
		caFilter = new ConservationAreaFilter();
		caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		
		super.setName("<No Name Query>");
	}
	
	/**
	 * Creates a new waypoint query the given filter and
	 * the default conservation area filter.
	 * 
	 * @param pFilter query filter
	 */
	public WaypointQuery(Filter pFilter){
		this();
		this.queryFilter = pFilter;
		
	}
	
	public static WaypointQuery createQuery(Filter pfilter){
		return new WaypointQuery(pfilter);
	}
	
	public List<QueryTableColumn> getTableColumns(){
		return this.tableColumns;
	}
	
	public void setTableColumns(List<QueryTableColumn> columns){
		this.tableColumns = columns;
	}
	
	
	public ConservationAreaFilter getConservationAreaFilter(){
		return this.caFilter;
	}
	public void setConservationAreaFilter(ConservationAreaFilter filter){
		this.caFilter = filter;
	}
	
	/**
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	public void setQueryFilter(String filter){
		this.strQueryFilter = filter;
		this.queryFilter = null;
	}
	
	public String getQueryFilter(){
		if (this.strQueryFilter == null){
			return "";
		}
		return this.strQueryFilter;
	}
	
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return
	 */
	public Filter parseQueryFilter() throws Exception {		
		InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes());
		Parser parser = new Parser(is);
		Filter myQuery = parser.Expression();
		is.close();
		return myQuery;
	}
	
	/**
	 * 
	 * @return the query filter in the filter format.  Will
	 * attempt to parse the query if it has not been parsed
	 */
	public Filter getFilter(){
		if (queryFilter == null){
			try{
				queryFilter = parseQueryFilter();
			} catch (Exception ex) {
				QueryPlugIn.displayLog("Could not parse query.", ex);
			}
		}
		return queryFilter;	
	}
	
	
	/**
	 * Runs the query and returns the results.
	 * @return
	 * @throws Exception
	 */
	public List<QueryResultItem> getQueryResults(IProgressMonitor progressMonitor) throws Exception{
		List<QueryResultItem> items  = null;
		Session session = HibernateManager.openSession();

		try{
			items = getQueryResults(session, progressMonitor);
		}finally{
			session.close();
		}
		return items;
	}
	
	/** public for testing purposes only */
	public List<QueryResultItem> getQueryResults(Session session, IProgressMonitor progressMonitor) throws Exception{
		DerbyQueryEngine engine = new DerbyQueryEngine();
		return engine.executeQuery(this, session, progressMonitor);
	}
	
	/*
	public List<QueryResultItem> getQueryResultsHibernate(){
		Session session = HibernateManager.openSession();
		List<QueryResultItem> items = new ArrayList<QueryResultItem>();
		
		try{
			org.hibernate.Query q = getHibernateQuery(session);
			q.setMaxResults(100);	//limit to 100 results for now
			System.out.println("start");
			List results = q.list();
			System.out.println("end");
			
			
			QueryResultItem lastItem = null;
			WaypointObservation lastWo = null;
			for (Iterator iterator = results.iterator(); iterator.hasNext();) {
				Object[] object = (Object[]) iterator.next();
				
				Patrol patrol = (Patrol) object[0];
				PatrolLeg patrolleg = (PatrolLeg) object[1];
				PatrolLegDay patrollegday = (PatrolLegDay) object[2];
				Waypoint waypoint = (Waypoint)object[3];
				WaypointObservation wo = (WaypointObservation)object[4];
				WaypointObservationAttribute woa = (WaypointObservationAttribute)object[5];
				
				
				if (lastWo != null && lastWo.equals(wo) ){
					lastItem.addAttribute(woa);
				}else{
					QueryResultItem item = new QueryResultItem();
					item.setPatrolValues(patrol);
					item.setPatrolLegValues(patrolleg);
					item.setPatrolLegDayValues(patrollegday);
					item.setWaypointValues(waypoint);
					if (wo != null){
						item.setCategory(wo.getCategory());
					}
					if (woa != null){
						item.addAttribute(woa);
					}
					items.add(item);					
					lastItem = item;
				}
				lastWo = wo;
			}
			
			
			
		}finally{
			session.close();
		}
		
		return items;
	}
	public Filter getFilter(){
		return this.queryFilter;
	}
	
	// public for test purposes 
	public org.hibernate.Query getHibernateQuery(Session session){
		HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
		tablePrefix.put(Patrol.class, "p");
		tablePrefix.put(PatrolLeg.class, "pl");
		tablePrefix.put(PatrolLegDay.class, "pld");
		tablePrefix.put(Waypoint.class, "wp");
		tablePrefix.put(WaypointObservation.class, "wpo");
		tablePrefix.put(WaypointObservationAttribute.class, "wpoa");
		tablePrefix.put(Attribute.class, "a");
		tablePrefix.put(Category.class, "c");
		tablePrefix.put(AttributeTreeNode.class, "at");
		tablePrefix.put(AttributeListItem.class, "al");
		
		
		HashMap<Class<?>, String> tablePrefix2 = new HashMap<Class<?>, String>();
		tablePrefix2.put(Patrol.class, "p2");
		tablePrefix2.put(PatrolLeg.class, "pl2");
		tablePrefix2.put(PatrolLegDay.class, "pld2");
		tablePrefix2.put(Waypoint.class, "wp2");
		tablePrefix2.put(WaypointObservation.class, "wpo2");
		tablePrefix2.put(WaypointObservationAttribute.class, "wpoa2");
		tablePrefix2.put(Attribute.class, "a2");
		tablePrefix2.put(Category.class, "c2");
		tablePrefix2.put(AttributeTreeNode.class, "at2");
		tablePrefix2.put(AttributeListItem.class, "al2");
		
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(createSelectClause(tablePrefix));
		sb.append(" FROM ");
		sb.append(createFromClauseHql(tablePrefix));
		sb.append(" WHERE ");
		sb.append(tablePrefix.get(WaypointObservation.class));
		sb.append(".uuid IN (");
		sb.append(" SELECT ");
		sb.append(tablePrefix2.get(WaypointObservation.class));
		sb.append(".uuid FROM ");
		sb.append(createFromClauseHql(tablePrefix2));
		sb.append(" WHERE ");
		sb.append(queryFilter.asHql(tablePrefix2, parameters));
		sb.append(")");
		sb.append(" ORDER BY ");
		sb.append(tablePrefix.get(WaypointObservation.class) + ".uuid, ");
		sb.append(tablePrefix.get(PatrolLegDay.class) + ".date,");
		sb.append(tablePrefix.get(Waypoint.class) + ".time");
		
		//TODO: ORDER BY
		
		// create query and add parameters 
		org.hibernate.Query q = session.createQuery(sb.toString());
		for (Iterator<Entry<String,Object>> iterator = parameters.entrySet().iterator(); iterator.hasNext();) {
			Entry<String,Object> type = (Entry<String,Object>) iterator.next();
			q.setParameter(type.getKey(), type.getValue());
		}
		return q;
	}
	
	private String createSelectClause(HashMap<Class<?>, String> tablePrefix ){
//		StringBuilder sb = new StringBuilder();
//		sb.append(tablePrefix.get(Waypoint.class) + ".id");
//		return sb.toString();
		StringBuilder sb = new StringBuilder();
		sb.append(tablePrefix.get(Patrol.class));
		sb.append(", ");
		sb.append(tablePrefix.get(PatrolLeg.class));
		sb.append(", ");
		sb.append(tablePrefix.get(PatrolLegDay.class));
		sb.append(", ");
		sb.append(tablePrefix.get(Waypoint.class));
		sb.append(", ");
		sb.append(tablePrefix.get(WaypointObservation.class));
		sb.append(", ");
		sb.append(tablePrefix.get(WaypointObservationAttribute.class));
		
		return sb.toString();
	}
	
	private String createFromClauseSql(HashMap<Class<?>, String> tablePrefix ){
		StringBuilder sb = new StringBuilder();
		
		//patrol
		sb.append("smart.patrol");
		sb.append(" ");
		sb.append(tablePrefix.get(Patrol.class));
		sb.append(" join ");
		
		//patrol leg 
		sb.append("smart.patrol_leg");
		sb.append(" ");
		sb.append(tablePrefix.get(PatrolLeg.class));
		sb.append(" on ");
		sb.append(tablePrefix.get(Patrol.class));
		sb.append(".uuid ");
		sb.append(" = ");
		sb.append(tablePrefix.get(PatrolLeg.class));
		sb.append(".patrol_uuid");
		sb.append(" join ");
		
		
		//patrol leg day
		sb.append("smart.patrol_leg_day");
		sb.append(" ");
		sb.append(tablePrefix.get(PatrolLegDay.class));
		sb.append(" on ");
		sb.append(tablePrefix.get(PatrolLegDay.class));
		sb.append(".patrol_leg_uuid");
		sb.append(" = ");
		sb.append(tablePrefix.get(PatrolLeg.class));
		sb.append(".uuid");
		sb.append(" join ");
		
		//waypoint
		sb.append("smart.waypoint");
		sb.append(" ");
		sb.append(tablePrefix.get(Waypoint.class));
		sb.append(" on ");
		sb.append(tablePrefix.get(PatrolLegDay.class));
		sb.append(".uuid");
		sb.append(" = ");
		sb.append(tablePrefix.get(Waypoint.class));
		sb.append(".leg_day_uuid");
		sb.append(" left join ");
		
		//waypoint observation
		sb.append("smart.wp_observation");
		sb.append(" ");
		sb.append(tablePrefix.get(WaypointObservation.class));
		sb.append(" on ");
		sb.append(tablePrefix.get(WaypointObservation.class));
		sb.append(".wp_uuid");
		sb.append(" = ");
		sb.append(tablePrefix.get(Waypoint.class));
		sb.append(".uuid");
		sb.append(" left join ");
		
		//category 
		sb.append("smart.dm_category");
		sb.append(" ");
		sb.append(tablePrefix.get(Category.class));
		sb.append(" on ");
		sb.append(tablePrefix.get(WaypointObservation.class));
		sb.append(".category_uuid");
		sb.append(" = ");
		sb.append(tablePrefix.get(Category.class));
		sb.append(".uuid");
		
		
		sb.append(" left join ");
		
		//wp observation attribute
		
		sb.append("smart.wp_observation_attributes");
		sb.append(" ");
		sb.append(tablePrefix.get(WaypointObservationAttribute.class));
		sb.append(" left join ");		
		sb.append(" on ");
		sb.append(tablePrefix.get(WaypointObservation.class));
		sb.append(".uuid");
		sb.append(" = ");
		sb.append(tablePrefix.get(WaypointObservationAttribute.class));
		sb.append(".observation_uuid");
		
		//attribute
		sb.append(" left join ");
		sb.append("smart.dm_attribute");
		sb.append(" ");
		sb.append(tablePrefix.get(Attribute.class));
		sb.append(" on ");
		sb.append(tablePrefix.get(WaypointObservationAttribute.class));
		sb.append(".attribute_uuid");
		sb.append(" = ");
		sb.append(tablePrefix.get(Attribute.class));
		sb.append(".uuid");
		
		//TODO: add list item and tree item tables if necessary
		
		return sb.toString();
	}
	
	
	private String createFromClauseHql(HashMap<Class<?>, String> tablePrefix ){
		StringBuilder sb = new StringBuilder();
		
		//patrol
		sb.append(Patrol.class.getSimpleName());
		sb.append(" ");
		sb.append(tablePrefix.get(Patrol.class));
		sb.append(" join ");
		
		//patrol leg 
		sb.append(tablePrefix.get(Patrol.class));
		sb.append(".legs ");
		sb.append(tablePrefix.get(PatrolLeg.class));
		
		//patrol leg days
		sb.append(" join ");
		sb.append(tablePrefix.get(PatrolLeg.class));
		sb.append(".patrolLegDays ");
		sb.append(tablePrefix.get(PatrolLegDay.class));
		
		//waypoints
		sb.append(" join ");
		sb.append(tablePrefix.get(PatrolLegDay.class));
		sb.append(".waypoints ");
		sb.append(tablePrefix.get(Waypoint.class));
		
		//observations
		sb.append(" left join ");
		sb.append(tablePrefix.get(Waypoint.class));
		sb.append(".observations ");
		sb.append(tablePrefix.get(WaypointObservation.class));

		//category
		sb.append(" left join ");
		sb.append(tablePrefix.get(WaypointObservation.class));
		sb.append(".category ");
		sb.append(tablePrefix.get(Category.class));
	
		//attributes
		sb.append(" left join ");
		sb.append(tablePrefix.get(WaypointObservation.class));
		sb.append(".attributes ");
		sb.append(tablePrefix.get(WaypointObservationAttribute.class));
		
		//attribute
		sb.append(" left join ");
		sb.append(tablePrefix.get(WaypointObservationAttribute.class));
		sb.append(".id.attribute ");
		sb.append(tablePrefix.get(Attribute.class));

		if (queryFilter.hasAttributeTreeItemFilter()){
			sb.append(" left join ");
			sb.append(tablePrefix.get(Attribute.class));
			sb.append(".tree ");
			sb.append(tablePrefix.get(AttributeTreeNode.class));
		}
		if (queryFilter.hasAttributeListItemFilter()){
			sb.append(" left join ");
			sb.append(tablePrefix.get(Attribute.class));
			sb.append(".attributeList ");
			sb.append(tablePrefix.get(AttributeListItem.class));
		}
		
		return sb.toString();
	}
	*/
}
