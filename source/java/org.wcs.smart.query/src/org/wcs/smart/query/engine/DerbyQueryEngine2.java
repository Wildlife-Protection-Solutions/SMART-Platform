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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.internal.filter.AreaFilter;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;
import org.wcs.smart.query.parser.internal.filter.IFilter;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DerbyQueryEngine2 implements QueryEngine {

	protected static final String QUERY_TEMP_TABLE_PREFIX = "query_results_";
	protected static final String QUERY_OB_TEMP_TABLE_PREFIX = "query_attributes_";
	protected static final String QUERY_GRID_TEMP_TABLE_PREFIX = "grid_intermediate_";
	
	/**
	 * Maps database tables to a prefix to use in the query.
	 */
	protected static HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
	static {
		tablePrefix = new HashMap<Class<?>, String>();
		tablePrefix.put(Patrol.class, "p");
		tablePrefix.put(PatrolLeg.class, "pl");
		tablePrefix.put(PatrolLegDay.class, "pld");
		tablePrefix.put(Waypoint.class, "wp");
		tablePrefix.put(WaypointObservation.class, "wpo");
		tablePrefix.put(WaypointObservationAttribute.class, "wpoa");
		tablePrefix.put(Attribute.class, "a");
		tablePrefix.put(Category.class, "c");
		tablePrefix.put(AttributeTreeNode.class, "atn");
		tablePrefix.put(AttributeListItem.class, "ali");
		tablePrefix.put(PatrolLegMember.class, "plm");
		tablePrefix.put(Track.class, "t");
		tablePrefix.put(Area.class, "ar");
	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	protected static HashMap<Class<?>, String> tableNames = new HashMap<Class<?>, String>();
	static {
		tableNames = new HashMap<Class<?>, String>();
		tableNames.put(Patrol.class, "smart.patrol");
		tableNames.put(PatrolLeg.class, "smart.patrol_leg");
		tableNames.put(PatrolLegDay.class, "smart.patrol_leg_day");
		tableNames.put(Waypoint.class, "smart.waypoint");
		tableNames.put(WaypointObservation.class, "smart.wp_observation");
		tableNames.put(WaypointObservationAttribute.class, "smart.wp_observation_attributes");
		tableNames.put(Attribute.class, "smart.dm_attribute");
		tableNames.put(Category.class, "smart.dm_category");
		tableNames.put(AttributeTreeNode.class, "smart.dm_attribute_tree");
		tableNames.put(AttributeListItem.class, "smart.dm_attribute_list");
		tableNames.put(PatrolLegMember.class, "smart.patrol_leg_members");
		tableNames.put(Track.class, "smart.track");
		tableNames.put(Area.class, "smart.area_geometries");
	}
	
	protected String queryTempTable = "";
	protected String observationTempTable = "";
	
	private List<QueryResultItem> myResults = null;
	
	
	/**
	 * Executes the given query.
	 * 
	 * @param query
	 *            the query to execute
	 * @param session
	 *            open hibernate session
	 * @param monitor
	 *            progress monitor
	 * 
	 * @return the results of the query
	 * @throws SQLException
	 */
	/*
	 * The query execute process is as follows:
	 * 
	 * 1) If the query includes attributes then create a "cross join" table
	 * of all observations and the required attributes. This table (observationTempTable)
	 * looks as follows:
	 * observation_uuid | attribute1 | attribute 2 | attribute 3 etc.
	 * 
	 * 2) A temporary table (queryTempTable) is created for holding all observations which
	 * match the required filter.  This table contains all the patrol
	 * to waypoint attributes and the observation id.  IT does 
	 * not contain any of the matched attributes.
	 * 
	 * 3) Join together the temporary results table with the observations
	 * to get all attributes associated with a matching observations.
	 */
	@Override
	public List<QueryResultItem> executeQuery(final SimpleQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {

		queryTempTable = QUERY_TEMP_TABLE_PREFIX + System.nanoTime();
		observationTempTable = QUERY_OB_TEMP_TABLE_PREFIX + System.nanoTime();
		

		myResults = null;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask("Running Query.", 4);

				try {
					
//					String myquery = "SELECT p.uuid, p.id, p.station_uuid, p.team_uuid, p.objective, p.mandate_uuid, p.patrol_type, p.is_armed, p.start_date, p.end_date, pl.uuid, pl.id, pl.transport_uuid, pld.uuid, pld.patrol_day, wp.uuid, wpo.uuid, plm_leader.employee_uuid, plm_pilot.employee_uuid  FROM smart.patrol p inner join smart.patrol_leg pl on p.uuid = pl.patrol_uuid  inner join smart.patrol_leg_day pld on pl.uuid = pld.patrol_leg_uuid  inner join smart.waypoint wp on pld.uuid = wp.leg_day_uuid  left join smart.wp_observation wpo on wp.uuid = wpo.wp_uuid  left join smart.patrol_leg_members plm_leader  on pl.uuid = plm_leader.patrol_leg_uuid and  plm_leader.is_leader  left join smart.patrol_leg_members plm_pilot  on pl.uuid = plm_pilot.patrol_leg_uuid and  plm_pilot.is_pilot , (select geom from smart.area_geometries where keyid = 'conservationarea' and area_type = 'CA' and ca_uuid = x'd5f41e3ab1e04108aacd4920e85bad94') as CA_conservationarea WHERE  ( pld.patrol_day >= '2012-06-05' )  AND (p.ca_uuid IN (x'd5f41e3ab1e04108aacd4920e85bad94')) AND  ( smart.pointinpolygon(wp.x, wp.y, CA_conservationarea.geom) ) ";
//					ResultSet rs = c.createStatement().executeQuery(myquery);
//					while(rs.next()){
//						System.out.println(rs.getString(1));
//					}
//					rs.close();
					
					monitor.subTask("Creating observation table");
					IFilter qFilter = query.getFilter();
					if (qFilter == null){
						return;
					}
					if (qFilter != IFilter.EMPTY_FILTER && qFilter.hasAttributeFilter()) {
						createObservationTable(c, query.getFilter(), query.getDateFilter(), query.getConservationAreaFilter());
					}
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}

					monitor.subTask("Creating temporary table");
					createTemporaryTable(c);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Populating results table");
					populateTemporaryTable(query.getFilter(), query.getDateFilter(), query.getConservationAreaFilter(), true, c, true);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask("Loading results into editor");
					myResults = getResults(c, session);
					
					monitor.worked(1);
				} finally {
					// ensure temporary tables get dropped
					dropTemporaryTables(c);
					monitor.done();
				}
			}
		});
		return myResults;

	}

	
	
	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	protected void dropTemporaryTables(Connection c) throws SQLException {
		try {
//			String sql = "DROP TABLE " + QUERY_TEMP_SCHEMA + "." + observationTempTable;

			String sql = "DROP TABLE " + observationTempTable;
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}

		try {
			String sql = "DROP TABLE " + queryTempTable;
//			String sql = "DROP TABLE " + QUERY_TEMP_SCHEMA + "." + queryTempTable;
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}
	}

	/**
	 * Create the temporary table that contains all attribute
	 * in the query.
	 * 
	 * @param c the database connection
	 * @param query the query 
	 * @throws SQLException
	 */
	protected void createObservationTable(Connection c, IFilter filter, DateFilter dateFilter, ConservationAreaFilter caFilter)
			throws SQLException {
		HashSet<AttributeInfo> keys = new HashSet<AttributeInfo>();
		filter.getAttributeFilters(keys);

		// -- build temporary table
		StringBuilder inlist = new StringBuilder();
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + observationTempTable + " (observation_uuid char(16) for bit data");
		for (AttributeInfo key : keys) {
			sql.append(", " + key.getKey() + " "
					+ this.getDataType(key.getType()));
		}
		sql.append(")");
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		sql = new StringBuilder();
		sql.append("INSERT INTO " + observationTempTable + " SELECT observation_uuid ");
		for (AttributeInfo key : keys) {
			sql.append(", max(" + key.getKey() + ") as " + key.getKey() + " ");
		}
		sql.append("FROM (");
		sql.append("SELECT " + tablePrefix.get(WaypointObservationAttribute.class) + ".observation_uuid ");
		boolean list = false;
		boolean tree = false;
		for (AttributeInfo key : keys) {
			if (key.getType() == AttributeType.LIST) {
				list = true;
				sql.append(", case when " + tablePrefix.get(Attribute.class) + ".keyid = '" + key.getKey() + "'");
				sql.append(" then l.keyid else null end as " + key.getKey() + " ");
			} else if (key.getType() == AttributeType.TREE) {
				tree = true;
				sql.append(", case when " + tablePrefix.get(Attribute.class) + ".keyid = '" + key.getKey() + "'");
				sql.append(" then t.hkey else null end as ");
				sql.append(key.getKey() + " ");
				
			} else {
				sql.append(", case when " + tablePrefix.get(Attribute.class) + ".keyid = '" + key.getKey()
						+ "' then " + tablePrefix.get(WaypointObservationAttribute.class) + "." + key.getColumn() + " else null end as "
						+ key.getKey() + " ");
			}
			inlist.append("'" + key.getKey() + "',");
		}
		sql.append("FROM ");
		sql.append(tableNames.get(PatrolLegDay.class) + " as " + tablePrefix.get(PatrolLegDay.class));
		sql.append(" join ");
		sql.append(tableNames.get(Waypoint.class) + " as " + tablePrefix.get(Waypoint.class));
		sql.append(" on " + tablePrefix.get(PatrolLegDay.class) + ".uuid = " + tablePrefix.get(Waypoint.class) + ".leg_day_uuid ");
		
		if (dateFilter != null){
			String dfilter = dateFilter.asSql(tablePrefix);
			if (dfilter.length() > 0) {
				sql.append(" and ");
				sql.append(dfilter);
			}
		}
		
		sql.append(" join ");
		sql.append(tableNames.get(WaypointObservation.class) + " as " + tablePrefix.get(WaypointObservation.class));
		sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = " + tablePrefix.get(WaypointObservation.class) + ".wp_uuid ");
		
		sql.append(" join ");
		sql.append(tableNames.get(WaypointObservationAttribute.class) + " as " + tablePrefix.get(WaypointObservationAttribute.class));
		sql.append(" on " + tablePrefix.get(WaypointObservation.class) + ".uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".observation_uuid ");
		sql.append(" join ");
		sql.append(tableNames.get(Attribute.class) + " as " + tablePrefix.get(Attribute.class));
		sql.append(" on " + tablePrefix.get(Attribute.class) + ".uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".attribute_uuid ");
		if (caFilter != null){
			String cfilter = caFilter.asSql(tablePrefix.get(Attribute.class));
			if (cfilter.length() > 0){
				sql.append(" and ");
				sql.append(cfilter);
			}
		}
		
		if (list) {
			sql.append(" LEFT JOIN ");
			sql.append(tableNames.get(AttributeListItem.class));
			sql.append(" l on l.uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".list_element_uuid ");
		}
		if (tree){
			sql.append(" LEFT JOIN ");
			sql.append(tableNames.get(AttributeTreeNode.class));
			sql.append(" t on t.uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".tree_node_uuid ");
		}
		sql.append("WHERE (");
		sql.append(" " + tablePrefix.get(Attribute.class) + ".keyid in (");
		sql.append(inlist.substring(0, inlist.length() - 1));
		sql.append("))");
		sql.append(") foo GROUP BY observation_uuid ");

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	
	/**
	 * Creates the temporary table that holds the query results.
	 * 
	 * @param c database connection
	 * @throws SQLException
	 */
	protected void createTemporaryTable(Connection c) throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + queryTempTable + "(");
		sql.append("p_uuid char(16) for bit data,");
		sql.append("p_id varchar(23),");
		sql.append("p_station_uuid char(16) for bit data,");
		sql.append("p_team_uuid char(16) for bit data,");
		sql.append("p_objective varchar(8192),");
		sql.append("p_mandate_uuid  char(16) for bit data,");
		sql.append("p_type varchar(6),");
		sql.append("p_is_armed boolean,");
		sql.append("p_start_date date,");
		sql.append("p_end_date date,");
		sql.append("pl_uuid char(16) for bit data,");
		sql.append("pl_id varchar(50),");
		sql.append("pl_transport_uuid char(16) for bit data,");
		sql.append("pld_uuid char(16) for bit data,");
		sql.append("pld_patrol_day date,");
		sql.append("wp_uuid char(16) for bit data,");
		sql.append("ob_uuid char(16) for bit data,");
		sql.append("plm_leader char(16) for bit data,");
		sql.append("plm_pilot char(16) for bit data");

		sql.append(")");

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		//-- add indexes 
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + queryTempTable + "_wp_uuid_idx on " + queryTempTable + "(wp_uuid)");
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("CREATE INDEX " + queryTempTable + "_ob_uuid_idx on " +  queryTempTable + "(ob_uuid)");
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	/**
	 * Populates the query temporary table.
	 * 
	 * @param queryFilter the query filter
	 * @param dateFilter the date filter
	 * @param caFilter the conservation area filter
	 * @param onlyObservations if only observation patrol records with observations
	 * are to be returned,  false will return all patrol records
	 * even if they don't have an observation
	 * @param c database connection
	 * @param needsObservations if the processing requires the observation
	 * information attached to the results (otherwise ob_uuid will be populated
	 * with null)
	 * 
	 * @param c the database connection
	 * 
	 * @throws SQLException
	 */
	protected void populateTemporaryTable(IFilter queryFilter, 
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c,
			boolean needsObservations)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		
		
		
		sql.append("INSERT INTO " + queryTempTable );
		// ---- SELECT CLAUSE -----
		sql.append(" SELECT ");
		sql.append(tablePrefix.get(Patrol.class) + ".uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".id, ");
		sql.append(tablePrefix.get(Patrol.class) + ".station_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".team_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".objective, ");
		sql.append(tablePrefix.get(Patrol.class) + ".mandate_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".patrol_type, ");
		sql.append(tablePrefix.get(Patrol.class) + ".is_armed, ");
		sql.append(tablePrefix.get(Patrol.class) + ".start_date, ");
		sql.append(tablePrefix.get(Patrol.class) + ".end_date, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".uuid, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".id, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".transport_uuid, ");
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".uuid, ");
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".patrol_day, ");
		if (needsObservations){
			sql.append(tablePrefix.get(Waypoint.class) + ".uuid, ");
			sql.append(tablePrefix.get(WaypointObservation.class) + ".uuid, ");
		}else{
			sql.append("cast(null as char for bit data),");	//wp_uuid
			sql.append("cast(null as char for bit data),");	//wpob_uuid
		}
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.employee_uuid, ");
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.employee_uuid ");

		// ---- FROM CLAUSE -----
		sql.append(" FROM ");
		sql.append(tableNames.get(Patrol.class));
		sql.append(" ");
		sql.append(tablePrefix.get(Patrol.class));
		sql.append(" inner join ");
		sql.append(tableNames.get(PatrolLeg.class));
		sql.append(" " + tablePrefix.get(PatrolLeg.class));
		sql.append(" on " + tablePrefix.get(Patrol.class) + ".uuid = "
				+ tablePrefix.get(PatrolLeg.class) + ".patrol_uuid ");
		
		if (caFilter != null) {
			String filter = caFilter.asSql(tablePrefix);
			if (filter.length() > 0) {
				sql.append(" AND ");
				sql.append("(" + filter + ")");
			}
		}
		
		
		sql.append(" inner join ");
		sql.append(tableNames.get(PatrolLegDay.class));
		sql.append(" ");
		sql.append(tablePrefix.get(PatrolLegDay.class));
		sql.append(" on " + tablePrefix.get(PatrolLeg.class) + ".uuid = "
				+ tablePrefix.get(PatrolLegDay.class) + ".patrol_leg_uuid ");
		
		if (dateFilter != null) {
			String filter = dateFilter.asSql(tablePrefix);
			if (filter.length() > 0) {
				sql.append(" and ");
				sql.append(filter);
			}
		}
		
		if (needsObservations){
			if (onlyObservations){
				sql.append(" inner join ");
			}else{
				sql.append(" left join ");
			}
			sql.append(tableNames.get(Waypoint.class));
			sql.append(" ");
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(PatrolLegDay.class) + ".uuid = "
				+ tablePrefix.get(Waypoint.class) + ".leg_day_uuid ");
		
			sql.append(" left join ");
			sql.append(tableNames.get(WaypointObservation.class));
			sql.append(" ");
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = "
				+ tablePrefix.get(WaypointObservation.class) + ".wp_uuid ");
		}
		sql.append(" left join ");
		sql.append(tableNames.get(PatrolLegMember.class));
		sql.append(" ");
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader ");
		sql.append(" on " + tablePrefix.get(PatrolLeg.class) + ".uuid = ");
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.patrol_leg_uuid and  ");
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.is_leader ");
		sql.append(" left join ");
		sql.append(tableNames.get(PatrolLegMember.class));
		sql.append(" ");
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot ");
		sql.append(" on " + tablePrefix.get(PatrolLeg.class) + ".uuid = ");
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.patrol_leg_uuid and  ");
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.is_pilot ");
				
		if (queryFilter != IFilter.EMPTY_FILTER) {
			if (queryFilter.hasAttributeFilter() || queryFilter.hasCategoryFilter()) {
				sql.append(" left join ");
				sql.append(tableNames.get(Category.class));
				sql.append(" ");
				sql.append(tablePrefix.get(Category.class));
				
				sql.append(" on " + tablePrefix.get(Category.class)
						+ ".uuid = "
						+ tablePrefix.get(WaypointObservation.class)
						+ ".category_uuid ");

				if (queryFilter.hasAttributeFilter()) {
					sql.append(" left join ");
					sql.append(observationTempTable + " qa on qa.observation_uuid = ");
					sql.append(tablePrefix.get(WaypointObservation.class)
							+ ".uuid");

				}
			}
		}
		
		// area filters
		LinkedList<IFilter> kidsToProcess = new LinkedList<IFilter>();
		kidsToProcess.add(queryFilter);
		Set<String> processedAreaFilters = new HashSet<String>();
		while(kidsToProcess.size() > 0){
			IFilter kid = kidsToProcess.poll();
			if (kid instanceof AreaFilter){
				AreaFilter ff = (AreaFilter)kid;
				String tableName = ff.getType().name() + "_" + ff.getKey();
				if (!processedAreaFilters.contains(tableName)) {
					processedAreaFilters.add(tableName);
					// TODO: escape special characters from the key
					sql.append(" left join ");
					sql.append(tableNames.get(Area.class));
					sql.append(" as ");
					sql.append( tableName);
					sql.append(" on ");
					sql.append( tableName +".ca_uuid = " + tablePrefix.get(Patrol.class) + ".ca_uuid and ");
					sql.append( tableName +".area_type = '" + ff.getType().name() + "' and ");
					sql.append(tableName + ".keyid = '" + ff.getKey() + "' ");
					if (ff.getGeometryType() == AreaFilter.AreaFilterGeometryType.TRACK){
						//add join to track geom
						sql.append(" left join " + tableNames.get(Track.class)+ " " + tablePrefix.get(Track.class) ); 
						sql.append(" ON " + tablePrefix.get(Track.class) + ".patrol_leg_day_uuid = " + tablePrefix.get(PatrolLegDay.class) + ".uuid" );
					}
				}
			}
			if (kid.getChildren() != null){
				kidsToProcess.addAll(kid.getChildren());
			}
		}

		
		// ---- WHERE CLAUSE -----
		if (queryFilter != IFilter.EMPTY_FILTER) {
			String filter = queryFilter.asSql(tablePrefix);
			if (filter != null && filter.length() > 0) {
				sql.append(" WHERE ");
			    sql.append(filter);
			}

		}

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	/**
	 * Reads the results from the temporary query table
	 * and loads them into internal memory store
	 * 
	 * @param c database connection 
	 * @param session hibernate session
	 * @return list of query results
	 * 
	 * @throws SQLException
	 */
	protected List<QueryResultItem> getResults(Connection c, Session session)
			throws SQLException {
		List<QueryResultItem> items = new ArrayList<QueryResultItem>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM (");
		sql.append(" SELECT ");
		sql.append(SelectClause(true));
		sql.append(" FROM ");
		sql.append(FromClause(true));
		sql.append(" WHERE ");
		sql.append(WhereClause(true));
		sql.append(" UNION ");
		sql.append(" SELECT ");
		sql.append(SelectClause(false));
		sql.append(" FROM ");
		sql.append(FromClause(false));
		sql.append(" WHERE ");
		sql.append(WhereClause(false));
		sql.append(" ) as foo");

		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());

		QueryResultItem last = null;
		try {
			while (rs.next()) {
				byte[] wpouuid = rs.getBytes(23);
				if (wpouuid != null && last != null
						&& last.getObservationUuid() != null
						&& Arrays.equals(wpouuid, last.getObservationUuid())) {
					//same observation new attribute
					Attribute att = getAttribute(rs.getBytes(25), session);
					if (att != null){
						Object value = getAttributeValue(att, rs, session);
						last.addAttribute(att.getKeyId(), value);
					}
					continue;
				}

				QueryResultItem it = new QueryResultItem();
				it.setPatrolUuid(rs.getBytes(1));
				it.setPatrolId(rs.getString(2));
				it.setPatrolStartDate(rs.getDate(3));
				it.setPatrolEndDate(rs.getDate(4));
				it.setStation(getStationName(rs.getBytes(5), session));				
				it.setTeam(getTeamName(rs.getBytes(6), session));				
//				it.setObjectiveRating(rs.getInt(7));
				it.setObjective(rs.getString(7));
				it.setMandate(getMandateName(rs.getBytes(8), session));
				it.setPatrolType(PatrolType.Type.valueOf(rs.getString(9)));
				it.setArmed(rs.getBoolean(10));
				it.setTransportType(getTransportType(rs.getBytes(11), session));
				it.setPatrolLegId(rs.getString(12));
				it.setWpDateTime(rs.getDate(13));
				
				it.setLeader(getEmployeeName(rs.getBytes(14), session));
				it.setPilot(getEmployeeName(rs.getBytes(15), session));
				
				it.setWaypointId(rs.getInt(16));
				it.setWaypointX(rs.getDouble(17));
				it.setWaypointY(rs.getDouble(18));
				it.setWaypointTime(rs.getTime(19));
				it.setWaypointDirection(rs.getFloat(20));
				it.setWaypointDistance(rs.getFloat(21));
				it.setWaypointComment(rs.getString(22));
				it.setObservationUuid(wpouuid);
				it.setCategory(getCategory(rs.getBytes(24), session));
				Attribute att = getAttribute(rs.getBytes(25), session);
				if (att != null){
					Object value = getAttributeValue(att, rs, session);
					it.addAttribute(att.getKeyId(), value);
				}

				items.add(it);
				last = it;
			}
		} finally {
			rs.close();
		}
		return items;
	}
	
	/**
	 * Gets the attribute value from the result set for the given attribute.
	 * 
	 * @param att
	 * @param rs
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	protected Object getAttributeValue(Attribute att, ResultSet rs,
			Session session) throws SQLException {
		Object value = null;
		switch (att.getType()) {
		case NUMERIC:
			value = rs.getDouble(26);
			break;
		case BOOLEAN:
			value = (rs.getDouble(26) >= 0.5);
			break;
		case TEXT:
			value = rs.getString(27);
			break;
		case TREE:
			byte[] nodeuuid = rs.getBytes(29);
			if (nodeuuid != null) {
				AttributeTreeNode i = (AttributeTreeNode) session.load(
						AttributeTreeNode.class, nodeuuid);
				value = i.getName();
			}
			break;
		case LIST:
			byte[] listuuid = rs.getBytes(28);
			if (listuuid != null) {
				AttributeListItem i = (AttributeListItem) session.load(
						AttributeListItem.class, listuuid);
				value = i.getName();
			}
			break;
		}
		return value;
	}
	
	
	/**
	 * Loads the attribute object from the session
	 * 
	 * @param uuid
	 * @param session
	 * @return
	 */
	protected Attribute getAttribute(byte[] uuid, Session session){
		if (uuid != null){
			Attribute att = (Attribute) session.load(Attribute.class, uuid);
			return att;
		}
		return null;
	}
	/**
	 * Loads the category object from the session
	 * 
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected Category getCategory(byte[] uuid, Session session){
		if (uuid != null){
			Category x = (Category) session.load(Category.class, uuid);
			return x;
		}
		return null;
	}
	
	/**
	 * Loads the station object from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String getStationName(byte[] suuid, Session session){
		if (suuid != null){
			Station x = (Station) session.load(Station.class, suuid);
			if (x != null) {
				return x.getName();
			}
		}
		return null;
	}
	
	/**
	 * Loads the team object from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String getTeamName(byte[] tuuid, Session session){
		if (tuuid != null){
			Team x = (Team) session.load(Team.class, tuuid);
			if (x != null) {
				return x.getName();
			}
		}
		return null;
	}
	
	/**
	 * Loads the team object from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String getEmployeeName(byte[] uuid, Session session){
		if (uuid != null){
			Employee x = (Employee) session.load(Employee.class, uuid);
			if (x != null) {
				return x.getLabel();
			}
		}
		return null;
	}
	
	/**
	 * Loads the team mandate from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String getMandateName(byte[] muuid, Session session){
		if (muuid != null){
			PatrolMandate x = (PatrolMandate) session.load(PatrolMandate.class, muuid);
			if (x != null) {
				return x.getName();
			}
		}
		return null;
	}
	
	/**
	 * Loads the transport type from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String getTransportType(byte[] uuid, Session session){
		if (uuid != null){
			PatrolTransportType x = (PatrolTransportType) session.load(
					PatrolTransportType.class, uuid);
			if (x != null){
				return x.getName();
			}
		}
		return null;
	}
	
	
	/**
	 * Build select clause 
	 * 
	 * @param includeObservations if observations should be included
	 * @return select clause
	 */
	protected String SelectClause(boolean includeObservations) {
		String[] results = { "p_uuid", "p_id", "p_start_date", "p_end_date",
				"p_station_uuid", "p_team_uuid", 
				"p_objective", "p_mandate_uuid", "p_type", "p_is_armed",
				"pl_transport_uuid", "pl_id", "pld_patrol_day", "plm_leader", "plm_pilot" };

		String[] waypoints = { "id", "x", "y", "time", "direction", "distance",
				"wp_comment" };
		
		String[] observations = { "uuid", "category_uuid" };
		
		String[] attributes = { "attribute_uuid", "number_value",
				"string_value", "list_element_uuid", "tree_node_uuid" };

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < results.length; i++) {
			if (i != 0) {
				sb.append(",");
			}
			sb.append("r." + results[i] + " as r_" + results[i]);
		}

		for (int i = 0; i < waypoints.length; i++) {
			sb.append(",");
			sb.append(tablePrefix.get(Waypoint.class) + "." + waypoints[i]
					+ " as w_" + waypoints[i]);
		}

		if (includeObservations) {
			for (int i = 0; i < observations.length; i++) {
				sb.append(",");
				sb.append(tablePrefix.get(WaypointObservation.class) + "."
						+ observations[i] + " as o_" + observations[i]);
			}
			for (int i = 0; i < attributes.length; i++) {
				sb.append(",");
				sb.append(tablePrefix.get(WaypointObservationAttribute.class)
						+ "." + attributes[i] + " as a_" + attributes[i]);
			}
		} else {
			for (int i = 0; i < observations.length; i++) {
				sb.append(",");
				sb.append(" cast(null as char(16) for bit data)");
			}
			for (int i = 0; i < attributes.length; i++) {
				sb.append(",");
				if (i == 1) {
					sb.append(" cast(null as double)");
				} else if (i == 2) {
					sb.append(" cast(null as varchar(1024))");
				} else {
					sb.append(" cast(null as char(16) for bit data)");
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Builds the where clause 
	 * @param includeObservations
	 * @return
	 */
	protected String WhereClause(boolean includeObservations) {
		if (includeObservations) {
			return "r.ob_uuid is not null";
		} else {
			return "r.ob_uuid is null";
		}
	}

	/**
	 * Builds the from clause
	 * @param includeObservations
	 * @return
	 */
	protected String FromClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();

//		sql.append(QUERY_TEMP_SCHEMA + "." + queryTempTable);
		sql.append(queryTempTable);
		sql.append(" r");

		if (includeObservations) {
			sql.append(" inner join ");
			sql.append(tableNames.get(WaypointObservation.class));
			sql.append(" ");
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(" on " + tablePrefix.get(WaypointObservation.class)
					+ ".uuid = r.ob_uuid ");

			sql.append(" inner join ");
			sql.append(tableNames.get(Waypoint.class));
			sql.append(" ");
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = "
					+ tablePrefix.get(WaypointObservation.class) + ".wp_uuid ");
		} else {
			sql.append(" inner join ");
			sql.append(tableNames.get(Waypoint.class));
			sql.append(" ");
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class)
					+ ".uuid = r.wp_uuid ");
		}

		if (includeObservations) {
			sql.append(" left join ");
			sql.append(tableNames.get(WaypointObservationAttribute.class));
			sql.append(" ");
			sql.append(tablePrefix.get(WaypointObservationAttribute.class));
			sql.append(" on " + tablePrefix.get(WaypointObservation.class)
					+ ".uuid = "
					+ tablePrefix.get(WaypointObservationAttribute.class)
					+ ".observation_uuid");
		}
		return sql.toString();
	}

	/**
	 * Returns the database data type for a given 
	 * attribute type.
	 * @param type the attribute type
	 * @return the database datatype for the observation
	 * temporary table
	 */
	protected String getDataType(AttributeType type) {
		switch (type) {
		case LIST:
			return "varchar(128)"; //keyid
		case TREE:
			return "varchar(32672)"; ///hkey
		case NUMERIC:
			return "double";
		case BOOLEAN:
			return "double";
		case TEXT:
			return "varchar(1024)";
		}
		return "";

	}
}
