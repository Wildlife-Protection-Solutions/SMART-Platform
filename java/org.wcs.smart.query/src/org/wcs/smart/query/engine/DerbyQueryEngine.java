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
package org.wcs.smart.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.WaypointQuery;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class DerbyQueryEngine implements QueryEngine {

	private static HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
	static{
	tablePrefix = new HashMap<Class<?>, String>();
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
	}
	
	private List<QueryResultItem > myResults = null;
	public List<QueryResultItem> executeQuery(final WaypointQuery query, 
			final Session session,
			final IProgressMonitor monitor) throws SQLException{

		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask("Running Query.", 3);
				monitor.subTask("Creating temporary table");
				createTemporaryTable(c);
				monitor.worked(1);
				monitor.subTask("Populating results table");
				populateTemporaryTable(query, c);
				monitor.worked(1);
				monitor.subTask("Loading results into editor");
				myResults = getResults(c, session);
				monitor.worked(1);
				monitor.done();
				
			}});
		return myResults;
		
	}
	
	private void createTemporaryTable(Connection c) throws SQLException{
		
		try{
			//TODO: fix this for running multiple queries.
			c.createStatement().execute("DROP TABLE SMART.QUERY_RESULTS");
		}catch (Exception ex){
			ex.printStackTrace();
			//eatme
		}
		
		
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE SMART.QUERY_RESULTS (");
		sql.append("p_uuid char(16) for bit data,");
		sql.append("p_id varchar(23),");
		sql.append("p_station_uuid char(16) for bit data,");
		sql.append("p_team_uuid char(16) for bit data,");
		sql.append("p_objective_rating smallint,");
		sql.append("p_objective varchar(8192),");
		sql.append("p_mandate_uuid  char(16) for bit data,");
		sql.append("p_type varchar(6),");
		sql.append("p_is_armed boolean,");
		sql.append("p_start_date date,");
		sql.append("p_end_date date,");
		sql.append("pl_id varchar(50),");
		sql.append("pl_transport_uuid char(16) for bit data,");
		sql.append("pld_patrol_day date,");
		sql.append("wp_uuid char(16) for bit data,");
		sql.append("ob_uuid char(16) for bit data)");
		
//		sql.append("CREATE TABLE SMART.QUERY_RESULTS (");
//		sql.append("ob_uuid char(16) for bit data,");
//		sql.append("wp_uuid char(16) for bit data not null)");
////		sql.append("pld_uuid char(16) for bit data not null,");
////		sql.append("pl_uuid char(16) for bit data not null,");
////		sql.append("p_uuid char(16) for bit data not null)  on commit preserve rows not logged");

		System.out.println(sql.toString());
		c.createStatement().execute(sql.toString());
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX query_results_wp_uuid_idx on SMART.QUERY_RESULTS(wp_uuid)");
		System.out.println(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("CREATE INDEX query_results_ob_uuid_idx on SMART.QUERY_RESULTS(ob_uuid)");
		System.out.println(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	
	private void populateTemporaryTable(WaypointQuery q, Connection c) throws SQLException{
		
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO SMART.QUERY_RESULTS ");
		sql.append(" SELECT ");
		sql.append(tablePrefix.get(Patrol.class) + ".uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".id, ");
		sql.append(tablePrefix.get(Patrol.class) + ".station_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".team_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".objective_rating, ");
		sql.append(tablePrefix.get(Patrol.class) + ".objective, ");
		sql.append(tablePrefix.get(Patrol.class) + ".mandate_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".patrol_type, ");
		sql.append(tablePrefix.get(Patrol.class) + ".is_armed, ");
		sql.append(tablePrefix.get(Patrol.class) + ".start_date, ");
		sql.append(tablePrefix.get(Patrol.class) + ".end_date, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".id, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".transport_uuid, ");
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".patrol_day, ");
		sql.append(tablePrefix.get(Waypoint.class) + ".uuid, ");
		sql.append(tablePrefix.get(WaypointObservation.class) + ".uuid ");
		
		sql.append(" FROM ");
		sql.append(" smart.patrol " + tablePrefix.get(Patrol.class) );
		sql.append(" inner join ");
		sql.append(" smart.patrol_leg " + tablePrefix.get(PatrolLeg.class) );
		sql.append(" on " + tablePrefix.get(Patrol.class)  + ".uuid = " +  tablePrefix.get(PatrolLeg.class) + ".patrol_uuid ");
		sql.append(" inner join ");
		sql.append(" smart.patrol_leg_day " + tablePrefix.get(PatrolLegDay.class) );
		sql.append(" on " + tablePrefix.get(PatrolLeg.class)  + ".uuid = " +  tablePrefix.get(PatrolLegDay.class) + ".patrol_leg_uuid ");
		sql.append(" inner join ");
		sql.append(" smart.waypoint " + tablePrefix.get(Waypoint.class) );
		sql.append(" on " + tablePrefix.get(PatrolLegDay.class)  + ".uuid = " +  tablePrefix.get(Waypoint.class) + ".leg_day_uuid ");
		sql.append(" left join ");
		sql.append(" smart.wp_observation " + tablePrefix.get(WaypointObservation.class) );
		sql.append(" on " + tablePrefix.get(Waypoint.class)  + ".uuid = " +  tablePrefix.get(WaypointObservation.class) + ".wp_uuid ");
		
		if (q.getFilter().hasAttributeFilter() || q.getFilter().hasCategoryFilter()){
			sql.append(" left join ");
			sql.append(" smart.dm_category " + tablePrefix.get(Category.class) );
			sql.append(" on " + tablePrefix.get(Category.class)  + ".uuid = " +  tablePrefix.get(WaypointObservation.class) + ".category_uuid ");
			
			if (q.getFilter().hasAttributeFilter()){
				sql.append(" left join ");
				sql.append(" smart.wp_observation_attributes " + tablePrefix.get(WaypointObservationAttribute.class) );
				sql.append(" on " + tablePrefix.get(WaypointObservation.class)  + ".uuid = " +  tablePrefix.get(WaypointObservationAttribute.class) + ".observation_uuid ");
				sql.append(" left join ");
				sql.append(" smart.dm_attribute " + tablePrefix.get(Attribute.class) );
				sql.append(" on " + tablePrefix.get(Attribute.class)  + ".uuid = " +  tablePrefix.get(WaypointObservationAttribute.class) + ".attribute_uuid ");
				
				if (q.getFilter().hasAttributeListItemFilter()){
					//TODO: join to list item table
				}
				if (q.getFilter().hasAttributeTreeItemFilter()){
					//TODO: join to tree table
				}
			}
		}
		
		sql.append(" WHERE ");
		boolean and = false;
		if (q.getConservationAreaFilter() != null ){
			String filter = q.getConservationAreaFilter().asSql(tablePrefix);
			if (filter.length() > 0){
				sql.append( "(" + filter + ")");
				and = true;
			}
		}
		String filter = q.getFilter().asSql(tablePrefix);
		if (filter.length() > 0){
			if (and){
				sql.append(" AND ");
			}
			
			sql.append(" ( " + filter + " ) ");
			and = true;
			
		}
		
		System.out.println(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	private List<QueryResultItem> getResults(Connection c, Session session) throws SQLException{
		List<QueryResultItem> items = new ArrayList<QueryResultItem>();
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM (");
		sql.append(" SELECT ");
		sql.append(SelectClause(true));
		sql.append(" FROM ");
		sql.append(FromClause(true));
		sql.append(" WHERE ");
		sql.append(WhereClause(true));
		sql.append( " UNION ");
		sql.append(" SELECT ");
		sql.append(SelectClause(false));
		sql.append(" FROM ");
		sql.append(FromClause(false));
		sql.append(" WHERE ");
		sql.append(WhereClause(false));
		sql.append( " ) as foo");
		
		System.out.println(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());
		
		QueryResultItem last = null;
		try{
		while(rs.next()){
			byte[] wpouuid = rs.getBytes(22);
			
			if (wpouuid != null && last != null && 
				last.getObservationUuid() != null &&
				Arrays.equals(wpouuid, last.getObservationUuid())){
				
				
				byte[] attributeuuid = rs.getBytes(23);
				if (attributeuuid != null){
					Attribute att = (Attribute) session.load(Attribute.class, attributeuuid);
					Object value = null;
					if (att == null){
						//TODO
						System.out.println("ERROR");
					}else{
						switch(att.getType()){
						case NUMERIC:
							value = rs.getDouble(24);
							break;
						case BOOLEAN:
							value = (rs.getDouble(24) >= 0.5);
							break;
						case TEXT:
							value = rs.getDouble(25);
							break;
						case TREE:
							byte[] nodeuuid = rs.getBytes(27);
							if (nodeuuid != null){
								AttributeTreeNode i = (AttributeTreeNode) session.load(AttributeTreeNode.class, nodeuuid);
								value = i.getName();
							}
							break;
						case LIST:
							byte[] listuuid = rs.getBytes(26);
							if (listuuid != null){
								AttributeListItem i = (AttributeListItem) session.load(AttributeListItem.class, listuuid);
								value = i.getName();
							}
							break;
						}
					}

					last.addAttribute(att.getKeyId(), value);
				}
				
				continue;
			}
					
			
			
			
			QueryResultItem it = new QueryResultItem();
			it.setPatrolUuid(rs.getBytes(1));
			it.setPatrolId(rs.getString(2));
			it.setPatrolStartDate(rs.getDate(3));
			it.setPatrolEndDate(rs.getDate(4));
			
			byte[] suuid = rs.getBytes(5);
			if (suuid == null){
				it.setStation(null);
			}else{
				Station x = (Station)session.load(Station.class, suuid);
				if (x == null){
					it.setStation("ERROR");
				}else{
					it.setStation(x.getName());
				}
			}
			
			byte[] tuuid = rs.getBytes(6);
			if (tuuid == null){
				it.setTeam(null);
			}else{
				Team x = (Team)session.load(Team.class, tuuid);
				if (x == null){
					it.setTeam("ERROR");
				}else{
					it.setTeam(x.getName());
				}
			}
			
			it.setObjectiveRating(rs.getInt(7));
			it.setObjective(rs.getString(8));
			
			byte[] muuid = rs.getBytes(9);
			if (muuid == null){
				it.setMandate(null);
			}else{
				PatrolMandate x = (PatrolMandate)session.load(PatrolMandate.class, muuid);
				if (x == null){
					it.setMandate("ERROR");
				}else{
					it.setMandate(x.getName());
				}
			}
			
			it.setPatrolType(PatrolType.Type.valueOf(rs.getString(10)));
			it.setArmed(rs.getBoolean(11));
			
			
			byte[] ttuuid = rs.getBytes(12);
			if (ttuuid == null){
				it.setTransportType(null);
			}else{
				PatrolTransportType x = (PatrolTransportType)session.load(PatrolTransportType.class, ttuuid);
				if (x == null){
					it.setTransportType("ERROR");
				}else{
					it.setTransportType(x.getName());
				}
			}
			
			it.setPatrolLegId(rs.getString(13));
			it.setWpDateTime(rs.getDate(14));
			it.setWaypointId(rs.getInt(15));
			it.setWaypointX(rs.getDouble(16));
			it.setWaypointY(rs.getDouble(17));
			it.setWaypointTime(rs.getTime(18));
			it.setWaypointDirection(rs.getFloat(19));
			it.setWaypointDistance(rs.getFloat(20));
			it.setWaypointComment(rs.getString(21));
			
			//byte[] wpouuid = rs.getBytes(22);
			it.setObservationUuid(wpouuid);
			byte[] catuuid = rs.getBytes(23);
			if (catuuid != null){
				Category x = (Category)session.load(Category.class, catuuid);
				if (x != null){
					it.setCategory(x);
				}
			}
			
			
			byte[] attributeuuid = rs.getBytes(24);
			if (attributeuuid != null){
				Attribute att = (Attribute) session.load(Attribute.class, attributeuuid);
				Object value = null;
				if (att == null){
					//TODO
					System.out.println("ERROR");
				}else{
					switch(att.getType()){
					case NUMERIC:
						value = rs.getDouble(25);
						break;
					case BOOLEAN:
						value = (rs.getDouble(25) >= 0.5);
						break;
					case TEXT:
						value = rs.getDouble(26);
						break;
					case TREE:
						byte[] nodeuuid = rs.getBytes(28);
						if (nodeuuid != null){
							AttributeTreeNode i = (AttributeTreeNode) session.load(AttributeTreeNode.class, nodeuuid);
							value = i.getName();
						}
						break;
					case LIST:
						byte[] listuuid = rs.getBytes(27);
						if (listuuid != null){
							AttributeListItem i = (AttributeListItem) session.load(AttributeListItem.class, listuuid);
							value = i.getName();
						}
						break;
					}
				}

				it.addAttribute(att.getKeyId(), value);
			}
			
			
			items.add(it);
			last = it;
		}
		}finally{
			rs.close();
		}
		return items;
	}
	
	
	private String SelectClause(boolean includeObservations){
		String[] results = {"p_uuid", "p_id", "p_start_date", "p_end_date", "p_station_uuid", "p_team_uuid", "p_objective_rating", "p_objective", "p_mandate_uuid", "p_type", "p_is_armed",
				"pl_transport_uuid", "pl_id", "pld_patrol_day"};
		
		String[] waypoints = {"id", "x", "y", "time", "direction", "distance", "wp_comment"};
		String[] observations = {"uuid", "category_uuid"};
		String[] attributes = {"attribute_uuid", "number_value", "string_value", "list_element_uuid", "tree_node_uuid"};
	
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < results.length; i ++){
			if (i != 0){
				sb.append(",");
			}
			sb.append("r." + results[i] + " as r_" + results[i]);
		}
		
//		for (int i = 0; i < legs.length; i ++){
//			sb.append(",");
//			sb.append(tablePrefix.get(PatrolLeg.class) + "." + legs[i] + " as l_" + legs[i]);
//		}
//		
//		for (int i = 0; i < legdays.length; i ++){
//			sb.append(",");
//			sb.append(tablePrefix.get(PatrolLegDay.class) + "." + legdays[i] + " as d_" + legdays[i]);
//		}
		
		for (int i = 0; i < waypoints.length; i ++){
			sb.append(",");
			sb.append(tablePrefix.get(Waypoint.class) + "." + waypoints[i] + " as w_" + waypoints[i]);
		}
		
		if (includeObservations){
			for (int i = 0; i < observations.length; i ++){
				sb.append(",");
				sb.append(tablePrefix.get(WaypointObservation.class) + "." + observations[i] + " as o_" + observations[i]);
			}
			
			for (int i = 0; i < attributes.length; i ++){
				sb.append(",");
				sb.append(tablePrefix.get(WaypointObservationAttribute.class) + "." + attributes[i] + " as a_" + attributes[i]);
			}
		}else{
			for (int i = 0; i < observations.length; i ++){
				sb.append(",");
				sb.append(" cast(null as char(16) for bit data)");
			}	
			for (int i = 0; i < attributes.length; i ++){
				sb.append(",");
				if (i == 1){
					sb.append(" cast(null as double)");
				}else if (i == 2){
					sb.append(" cast(null as varchar(1024))");
				}else{
					sb.append(" cast(null as char(16) for bit data)");
				}
				
			}

		}
		
		return sb.toString();
	}
	
	
	private String WhereClause(boolean includeObservations){
		if (includeObservations){
			return "r.ob_uuid is not null";
		}else{
			return "r.ob_uuid is null";
		}
	}
	private String FromClause(boolean includeObservations){
		StringBuilder sql = new StringBuilder();
		
		sql.append("smart.query_results r");
		
		if (includeObservations){
			sql.append(" inner join ");
			sql.append("smart.wp_observation " + tablePrefix.get(WaypointObservation.class));
			sql.append( " on " + tablePrefix.get(WaypointObservation.class) + ".uuid = r.ob_uuid ");
			
			sql.append(" inner join ");
			sql.append("smart.waypoint " + tablePrefix.get(Waypoint.class));
			sql.append( " on " + tablePrefix.get(Waypoint.class) + ".uuid = " + tablePrefix.get(WaypointObservation.class) + ".wp_uuid ");
		}else{
			sql.append(" inner join ");
			sql.append("smart.waypoint " + tablePrefix.get(Waypoint.class));
			sql.append( " on " + tablePrefix.get(Waypoint.class) + ".uuid = r.wp_uuid ");
		}
		
//		sql.append(" inner join ");
//		sql.append("smart.patrol_leg_day " + tablePrefix.get(PatrolLegDay.class));
//		sql.append( " on " + tablePrefix.get(PatrolLegDay.class) + ".uuid = " + tablePrefix.get(Waypoint.class) + ".leg_day_uuid ");
//		
//		sql.append(" inner join ");
//		sql.append("smart.patrol_leg " + tablePrefix.get(PatrolLeg.class));
//		sql.append( " on " + tablePrefix.get(PatrolLeg.class) + ".uuid = " + tablePrefix.get(PatrolLegDay.class) + ".patrol_leg_uuid ");
//		
//		sql.append(" inner join ");
//		sql.append("smart.patrol " + tablePrefix.get(Patrol.class));
//		sql.append( " on " + tablePrefix.get(Patrol.class) + ".uuid = " + tablePrefix.get(PatrolLeg.class) + ".patrol_uuid ");
		
//		sql.append("smart.patrol " + tablePrefix.get(Patrol.class));
//		sql.append(" inner join ");
//		sql.append("smart.patrol_leg " + tablePrefix.get(PatrolLeg.class));
//		sql.append( " on " + tablePrefix.get(Patrol.class) + ".uuid = " + tablePrefix.get(PatrolLeg.class) + ".patrol_uuid ");
//		sql.append(" inner join ");
//		sql.append("smart.patrol_leg_day " + tablePrefix.get(PatrolLegDay.class));
//		sql.append( " on " + tablePrefix.get(PatrolLeg.class) + ".uuid = " + tablePrefix.get(PatrolLegDay.class) + ".patrol_leg_uuid ");
//		sql.append(" inner join ");
//		sql.append("smart.waypoint " + tablePrefix.get(Waypoint.class));
//		sql.append( " on " + tablePrefix.get(PatrolLegDay.class) + ".uuid = " + tablePrefix.get(Waypoint.class) + ".leg_day_uuid ");
		if (includeObservations){
//			sql.append(" inner join ");
//			sql.append("smart.wp_observation " + tablePrefix.get(WaypointObservation.class));
//			sql.append( " on " + tablePrefix.get(WaypointObservation.class) + ".wp_uuid = " + tablePrefix.get(Waypoint.class) + ".uuid ");
//			sql.append(" inner join ");
//			sql.append(" smart.query_results r on r.ob_uuid = " + tablePrefix.get(WaypointObservation.class) + ".uuid ");
			
			sql.append(" left join ");
			sql.append(" smart.wp_observation_attributes " + tablePrefix.get(WaypointObservationAttribute.class)  );
			sql.append(" on " + tablePrefix.get(WaypointObservation.class) + ".uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".observation_uuid");
//		}else{
//			sql.append(" inner join ");
//			
//			sql.append(" smart.query_results r on r.wp_uuid = " + tablePrefix.get(Waypoint.class) + ".uuid ");
		}
		return sql.toString();
	}
}
