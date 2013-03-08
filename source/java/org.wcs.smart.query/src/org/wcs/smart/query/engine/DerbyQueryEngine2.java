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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.internal.filter.AreaFilter;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DerbyQueryEngine2 implements QueryEngine {

	protected static final String QUERY_TEMP_TABLE_PREFIX = "query_results_"; //$NON-NLS-1$
	protected static final String QUERY_OB_TEMP_TABLE_PREFIX = "query_attributes_"; //$NON-NLS-1$
	protected static final String QUERY_GRID_TEMP_TABLE_PREFIX = "grid_intermediate_"; //$NON-NLS-1$
	
	/**
	 * Maps database tables to a prefix to use in the query.
	 */
	protected static HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
	static {
		tablePrefix = new HashMap<Class<?>, String>();
		tablePrefix.put(ConservationArea.class, "ca"); //$NON-NLS-1$
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(PatrolLeg.class, "pl"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegDay.class, "pld"); //$NON-NLS-1$
		tablePrefix.put(Waypoint.class, "wp"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservation.class, "wpo"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservationAttribute.class, "wpoa"); //$NON-NLS-1$
		tablePrefix.put(Attribute.class, "a"); //$NON-NLS-1$
		tablePrefix.put(Category.class, "c"); //$NON-NLS-1$
		tablePrefix.put(AttributeTreeNode.class, "atn"); //$NON-NLS-1$
		tablePrefix.put(AttributeListItem.class, "ali"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegMember.class, "plm"); //$NON-NLS-1$
		tablePrefix.put(Track.class, "t"); //$NON-NLS-1$
		tablePrefix.put(Area.class, "ar"); //$NON-NLS-1$
	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	protected static HashMap<Class<?>, String> tableNames = new HashMap<Class<?>, String>();
	static {
		tableNames = new HashMap<Class<?>, String>();
		tableNames.put(ConservationArea.class, "smart.conservation_area"); //$NON-NLS-1$
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(PatrolLeg.class, "smart.patrol_leg"); //$NON-NLS-1$
		tableNames.put(PatrolLegDay.class, "smart.patrol_leg_day"); //$NON-NLS-1$
		tableNames.put(Waypoint.class, "smart.waypoint"); //$NON-NLS-1$
		tableNames.put(WaypointObservation.class, "smart.wp_observation"); //$NON-NLS-1$
		tableNames.put(WaypointObservationAttribute.class, "smart.wp_observation_attributes"); //$NON-NLS-1$
		tableNames.put(Attribute.class, "smart.dm_attribute"); //$NON-NLS-1$
		tableNames.put(Category.class, "smart.dm_category"); //$NON-NLS-1$
		tableNames.put(AttributeTreeNode.class, "smart.dm_attribute_tree"); //$NON-NLS-1$
		tableNames.put(AttributeListItem.class, "smart.dm_attribute_list"); //$NON-NLS-1$
		tableNames.put(PatrolLegMember.class, "smart.patrol_leg_members"); //$NON-NLS-1$
		tableNames.put(Track.class, "smart.track"); //$NON-NLS-1$
		tableNames.put(Area.class, "smart.area_geometries"); //$NON-NLS-1$
	}
	
	protected String queryTempTable = ""; //$NON-NLS-1$
	protected String observationTempTable = ""; //$NON-NLS-1$
	
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
	public Collection<QueryResultItem> executeQuery(final SimpleQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {

		queryTempTable = QUERY_TEMP_TABLE_PREFIX + System.nanoTime();
		observationTempTable = QUERY_OB_TEMP_TABLE_PREFIX + System.nanoTime();
		

		myResults = null;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyQueryEngine2_Progress_RunningQuery, 4);

				try {			
					monitor.subTask(Messages.DerbyQueryEngine2_Progress_CreatingObservationTable);
					IFilter qFilter = query.getFilter();
					if (qFilter == null){
						return;
					}
					if (qFilter != IFilter.EMPTY_FILTER && qFilter.hasAttributeFilter()) {
						createObservationTable(c, query.getFilter(), query.getDateFilter(), query.getConservationAreaFilterAsFilter());
					}
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}

					monitor.subTask(Messages.DerbyQueryEngine2_Progress_CreatingTempTable);
					createTemporaryTable(c);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbyQueryEngine2_Progress_PopulatingResults);
					populateTemporaryTable(query.getFilter(), query.getDateFilter(), query.getConservationAreaFilterAsFilter(), true, c, true);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbyQueryEngine2_Progress_LoadingError);
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

			String sql = "DROP TABLE " + observationTempTable; //$NON-NLS-1$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}

		try {
			String sql = "DROP TABLE " + queryTempTable; //$NON-NLS-1$
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
		sql.append("CREATE TABLE " + observationTempTable + " (observation_uuid char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		for (AttributeInfo key : keys) {
			sql.append(", " + key.getKey() + " " //$NON-NLS-1$ //$NON-NLS-2$
					+ this.getDataType(key.getType()));
		}
		sql.append(")"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		sql = new StringBuilder();
		sql.append("INSERT INTO " + observationTempTable + " SELECT observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		for (AttributeInfo key : keys) {
			sql.append(", max(" + key.getKey() + ") as " + key.getKey() + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		sql.append("FROM ("); //$NON-NLS-1$
		sql.append("SELECT " + tablePrefix.get(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		boolean list = false;
		boolean tree = false;
		for (AttributeInfo key : keys) {
			if (key.getType() == AttributeType.LIST) {
				list = true;
				sql.append(", case when " + tablePrefix.get(Attribute.class) + ".keyid = '" + key.getKey() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" then l.keyid else null end as " + key.getKey() + " "); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (key.getType() == AttributeType.TREE) {
				tree = true;
				sql.append(", case when " + tablePrefix.get(Attribute.class) + ".keyid = '" + key.getKey() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" then t.hkey else null end as "); //$NON-NLS-1$
				sql.append(key.getKey() + " "); //$NON-NLS-1$
				
			} else {
				sql.append(", case when " + tablePrefix.get(Attribute.class) + ".keyid = '" + key.getKey() //$NON-NLS-1$ //$NON-NLS-2$
						+ "' then " + tablePrefix.get(WaypointObservationAttribute.class) + "." + key.getColumn() + " else null end as " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ key.getKey() + " "); //$NON-NLS-1$
			}
			inlist.append("'" + key.getKey() + "',"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sql.append("FROM "); //$NON-NLS-1$
		boolean on = false;
		if (dateFilter != null && (dateFilter.getDateFieldOption() == DateFilter.DATE_FIELD_OP.PATROL_END || dateFilter.getDateFieldOption() == DateFilter.DATE_FIELD_OP.PATROL_START ) ){
			sql.append(tableNames.get(Patrol.class) + " as " + tablePrefix.get(Patrol.class)); //$NON-NLS-1$
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNames.get(PatrolLeg.class) + " as " + tablePrefix.get(PatrolLeg.class)); //$NON-NLS-1$
	 		sql.append(" ON " +tablePrefix.get(Patrol.class) + ".uuid = " + tablePrefix.get(PatrolLeg.class) + ".patrol_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	 		sql.append(" join "); //$NON-NLS-1$
			on = true;
		}
		sql.append(tableNames.get(PatrolLegDay.class) + " as " + tablePrefix.get(PatrolLegDay.class)); //$NON-NLS-1$
		if (on){
			sql.append(" ON " + tablePrefix.get(PatrolLegDay.class) + ".patrol_leg_uuid = " + tablePrefix.get(PatrolLeg.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNames.get(Waypoint.class) + " as " + tablePrefix.get(Waypoint.class)); //$NON-NLS-1$
		sql.append(" on " + tablePrefix.get(PatrolLegDay.class) + ".uuid = " + tablePrefix.get(Waypoint.class) + ".leg_day_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		if (dateFilter != null){
			String dfilter = dateFilter.asSql(tablePrefix);
			if (dfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(dfilter);
			}
		}
		
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNames.get(WaypointObservation.class) + " as " + tablePrefix.get(WaypointObservation.class)); //$NON-NLS-1$
		sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = " + tablePrefix.get(WaypointObservation.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNames.get(WaypointObservationAttribute.class) + " as " + tablePrefix.get(WaypointObservationAttribute.class)); //$NON-NLS-1$
		sql.append(" on " + tablePrefix.get(WaypointObservation.class) + ".uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNames.get(Attribute.class) + " as " + tablePrefix.get(Attribute.class)); //$NON-NLS-1$
		sql.append(" on " + tablePrefix.get(Attribute.class) + ".uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (caFilter != null){
			String cfilter = caFilter.asSql(tablePrefix.get(Attribute.class));
			if (cfilter.length() > 0){
				sql.append(" and "); //$NON-NLS-1$
				sql.append(cfilter);
			}
		}
		
		if (list) {
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(tableNames.get(AttributeListItem.class));
			sql.append(" l on l.uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (tree){
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(tableNames.get(AttributeTreeNode.class));
			sql.append(" t on t.uuid = " + tablePrefix.get(WaypointObservationAttribute.class) + ".tree_node_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sql.append("WHERE ("); //$NON-NLS-1$
		sql.append(" " + tablePrefix.get(Attribute.class) + ".keyid in ("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(inlist.substring(0, inlist.length() - 1));
		sql.append("))"); //$NON-NLS-1$
		sql.append(") foo GROUP BY observation_uuid "); //$NON-NLS-1$

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
		sql.append("CREATE TABLE " + queryTempTable + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_id varchar(23),"); //$NON-NLS-1$
		sql.append("p_station_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_team_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_objective varchar(8192),"); //$NON-NLS-1$
		sql.append("p_mandate_uuid  char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_type varchar(6),"); //$NON-NLS-1$
		sql.append("p_is_armed boolean,"); //$NON-NLS-1$
		sql.append("p_start_date date,"); //$NON-NLS-1$
		sql.append("p_end_date date,"); //$NON-NLS-1$
		sql.append("pl_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pl_id varchar(50),"); //$NON-NLS-1$
		sql.append("pl_transport_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pld_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pld_patrol_day date,"); //$NON-NLS-1$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_leader char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_pilot char(16) for bit data"); //$NON-NLS-1$

		sql.append(")"); //$NON-NLS-1$

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		//-- add indexes 
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + queryTempTable + "_wp_uuid_idx on " + queryTempTable + "(wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("CREATE INDEX " + queryTempTable + "_ob_uuid_idx on " +  queryTempTable + "(ob_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		
		
		
		sql.append("INSERT INTO " + queryTempTable ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".station_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".team_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".objective, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".mandate_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".patrol_type, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".is_armed, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".transport_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".patrol_day, "); //$NON-NLS-1$
		if (needsObservations){
			sql.append(tablePrefix.get(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as char for bit data),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as char for bit data),");	//wpob_uuid //$NON-NLS-1$
		}
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.employee_uuid "); //$NON-NLS-1$

		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNames.get(Patrol.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(tableNames.get(PatrolLeg.class));
		sql.append(" " + tablePrefix.get(PatrolLeg.class)); //$NON-NLS-1$
		sql.append(" on " + tablePrefix.get(Patrol.class) + ".uuid = " //$NON-NLS-1$ //$NON-NLS-2$
				+ tablePrefix.get(PatrolLeg.class) + ".patrol_uuid "); //$NON-NLS-1$
		
		if (caFilter != null) {
			String filter = caFilter.asSql(tablePrefix);
			if (filter.length() > 0) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(tableNames.get(PatrolLegDay.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegDay.class));
		sql.append(" on " + tablePrefix.get(PatrolLeg.class) + ".uuid = " //$NON-NLS-1$ //$NON-NLS-2$
				+ tablePrefix.get(PatrolLegDay.class) + ".patrol_leg_uuid "); //$NON-NLS-1$
		
		if (dateFilter != null) {
			String filter = dateFilter.asSql(tablePrefix);
			if (filter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(filter);
			}
		}
		
		if (needsObservations){
			if (onlyObservations){
				sql.append(" inner join "); //$NON-NLS-1$
			}else{
				sql.append(" left join "); //$NON-NLS-1$
			}
			sql.append(tableNames.get(Waypoint.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(PatrolLegDay.class) + ".uuid = " //$NON-NLS-1$ //$NON-NLS-2$
				+ tablePrefix.get(Waypoint.class) + ".leg_day_uuid "); //$NON-NLS-1$
		
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(tableNames.get(WaypointObservation.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = " //$NON-NLS-1$ //$NON-NLS-2$
				+ tablePrefix.get(WaypointObservation.class) + ".wp_uuid "); //$NON-NLS-1$
		}
		sql.append(" left join "); //$NON-NLS-1$
		sql.append(tableNames.get(PatrolLegMember.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader "); //$NON-NLS-1$
		sql.append(" on " + tablePrefix.get(PatrolLeg.class) + ".uuid = "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.patrol_leg_uuid and  "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.is_leader "); //$NON-NLS-1$
		sql.append(" left join "); //$NON-NLS-1$
		sql.append(tableNames.get(PatrolLegMember.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot "); //$NON-NLS-1$
		sql.append(" on " + tablePrefix.get(PatrolLeg.class) + ".uuid = "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.patrol_leg_uuid and  "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.is_pilot "); //$NON-NLS-1$
				
		if (queryFilter != IFilter.EMPTY_FILTER) {
			if (queryFilter.hasAttributeFilter() || queryFilter.hasCategoryFilter()) {
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(tableNames.get(Category.class));
				sql.append(" "); //$NON-NLS-1$
				sql.append(tablePrefix.get(Category.class));
				
				sql.append(" on " + tablePrefix.get(Category.class) //$NON-NLS-1$
						+ ".uuid = " //$NON-NLS-1$
						+ tablePrefix.get(WaypointObservation.class)
						+ ".category_uuid "); //$NON-NLS-1$

				if (queryFilter.hasAttributeFilter()) {
					sql.append(" left join "); //$NON-NLS-1$
					sql.append(observationTempTable + " qa on qa.observation_uuid = "); //$NON-NLS-1$
					sql.append(tablePrefix.get(WaypointObservation.class)
							+ ".uuid"); //$NON-NLS-1$

				}
			}
		}
		
		// area filters
		LinkedList<IFilter> kidsToProcess = new LinkedList<IFilter>();
		kidsToProcess.add(queryFilter);
		Set<String> processedAreaFilters = new HashSet<String>();
		boolean joinedTracks = false;
		while(kidsToProcess.size() > 0){
			IFilter kid = kidsToProcess.poll();
			if (kid instanceof AreaFilter){
				AreaFilter ff = (AreaFilter)kid;
				String tableName = ff.getType().name() + "_" + ff.getKey(); //$NON-NLS-1$
				if (!processedAreaFilters.contains(tableName)) {
					processedAreaFilters.add(tableName);
					// TODO: escape special characters from the key
					sql.append(" left join "); //$NON-NLS-1$
					sql.append(tableNames.get(Area.class));
					sql.append(" as "); //$NON-NLS-1$
					sql.append( tableName);
					sql.append(" on "); //$NON-NLS-1$
					sql.append( tableName +".ca_uuid = " + tablePrefix.get(Patrol.class) + ".ca_uuid and "); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append( tableName +".area_type = '" + ff.getType().name() + "' and "); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(tableName + ".keyid = '" + ff.getKey() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					if (ff.getGeometryType() == AreaFilter.AreaFilterGeometryType.TRACK && !joinedTracks){
						//add join to track geom
						joinedTracks = true;
						sql.append(" left join " + tableNames.get(Track.class)+ " " + tablePrefix.get(Track.class) );  //$NON-NLS-1$ //$NON-NLS-2$
						sql.append(" ON " + tablePrefix.get(Track.class) + ".patrol_leg_day_uuid = " + tablePrefix.get(PatrolLegDay.class) + ".uuid" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
				sql.append(" WHERE "); //$NON-NLS-1$
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
		sql.append("SELECT * FROM ("); //$NON-NLS-1$
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(SelectClause(true));
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(FromClause(true));
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(WhereClause(true));
		sql.append(" UNION "); //$NON-NLS-1$
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(SelectClause(false));
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(FromClause(false));
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(WhereClause(false));
		sql.append(" ) as foo"); //$NON-NLS-1$

		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());

		QueryResultItem last = null;
		session.flush();
		try {
			
			/*Field order:
			 * 1 - "ca_uuid"
			 * 2 - "ca id"
			 * 3 - "ca name"
			 * 4 - "p_uuid"
			 * 5 - "p_id"
			 * 6 - "p_start_date"
			 * 7 - "p_end_date"
			 * 8 - "p_station_uuid"
			 * 9 - "p_team_uuid"
			 * 10 - "p_objective"
			 * 11 - "p_mandate_uuid"
			 * 12 - "p_type"
			 * 13 - "p_is_armed"
			 * 14 - "pl_transport_uuid"
			 * 15 - "pl_id"
			 * 16 - "pld_patrol_day"
			 * 17 - "plm_leader"
			 * 18 - "plm_pilot"
			 * 19 - "uuid"
			 * 20 - "id"
			 * 21 - "x"
			 * 22 - "y"
			 * 23 - "time"
			 * 24 - "direction"
			 * 25 - "distance"
			 * 26 - "wp_comment"
			 * 27 - "uuid"
			 * 28 - "category_uuid"
			 * 29 - "attribute_uuid"
			 * 30 - "number_value"
			 * 31 - "string_value"
			 * 32 - "list_element_uuid"
			 * 33 - "tree_node_uuid"
			 * 34 - "attribute key"
			 *
			 */

			while (rs.next()) {
				byte[] cauuid = rs.getBytes(3);
				byte[] wpouuid = rs.getBytes(27);
				if (wpouuid != null && last != null
						&& last.getObservationUuid() != null
						&& Arrays.equals(wpouuid, last.getObservationUuid())) {
					//same observation new attribute
					String key = rs.getString(34);
					if (key != null){
						Object value = getAttributeValue(rs, cauuid, session);
						last.addAttribute(key, value);
					}

					continue;
				}

				QueryResultItem it = new QueryResultItem();
//				it.setCaUuid(rs.getBytes(1));
				it.setConservationAreaId(rs.getString(1));
				it.setConservationAreaName(rs.getString(2));
				
				it.setPatrolUuid(rs.getBytes(4));
				it.setPatrolId(rs.getString(5));
				it.setPatrolStartDate(rs.getDate(6));
				it.setPatrolEndDate(rs.getDate(7));
				it.setStation(getName(rs.getBytes(8), cauuid, session));				
				it.setTeam(getName(rs.getBytes(9), cauuid, session));	
				it.setObjective(rs.getString(10));
				it.setMandate(getName(rs.getBytes(11), cauuid, session));
				it.setPatrolType(PatrolType.Type.valueOf(rs.getString(12)));
				it.setArmed(rs.getBoolean(13));
				it.setTransportType(getName(rs.getBytes(14), cauuid, session));
				it.setPatrolLegId(rs.getString(15));
				it.setWpDateTime(rs.getDate(16));
				
				it.setLeader(getEmployeeName(rs.getBytes(17), session));
				it.setPilot(getEmployeeName(rs.getBytes(18), session));
				it.setWaypointUuid(rs.getBytes(19));
				it.setWaypointId(rs.getInt(20));
				it.setWaypointX(rs.getDouble(21));
				it.setWaypointY(rs.getDouble(22));
				it.setWaypointTime(rs.getTime(23));
				it.setWaypointDirection(rs.getFloat(24));
				it.setWaypointDistance(rs.getFloat(25));
				it.setWaypointComment(rs.getString(26));
				it.setObservationUuid(wpouuid);
				it.setCategory(getCategoryLabels(rs.getBytes(28), session));
				
//				Attribute att = getAttribute(rs.getBytes(27), session);
				String key = rs.getString(34);
				if (key != null){
					Object value = getAttributeValue(rs, cauuid, session);
					it.addAttribute(key, value);
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
	protected Object getAttributeValue(ResultSet rs, byte[] cauuid, Session session) throws SQLException {
		if (rs.getObject(30) != null){
			return rs.getDouble(30);
		}else if (rs.getString(31) != null){
			return rs.getString(31);
		}else if (rs.getBytes(33) != null){
			return QueryDataModelManager.getInstance().getAttributeTreeNodeLabel(session, cauuid, rs.getBytes(33));
		}else if (rs.getBytes(32) != null){
			return QueryDataModelManager.getInstance().getAttributeListItemLabel(session, cauuid, rs.getBytes(32));
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
	protected String[] getCategoryLabels(byte[] uuid, Session session){
		if (uuid != null){
			return QueryDataModelManager.getInstance().getFullCategoryLabel(session, uuid);
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
	
	protected String getName(byte[] uuid, byte[] cauuid, Session session){
		if (SmartDB.isMultipleAnalysis()){
			//need find label for the given conservation area
			return Label.getDescription(uuid, cauuid);	
		}else{
			return Label.getDescription(uuid);
		}
	}
	
	/**
	 * Build select clause 
	 * 
	 * @param includeObservations if observations should be included
	 * @return select clause
	 */
	protected String SelectClause(boolean includeObservations) {
		String[] ca = {"id", "name"};  //$NON-NLS-1$ //$NON-NLS-2$
		
		String[] results = { "p_ca_uuid", "p_uuid", "p_id", "p_start_date", "p_end_date", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				"p_station_uuid", "p_team_uuid",  //$NON-NLS-1$ //$NON-NLS-2$
				"p_objective", "p_mandate_uuid", "p_type", "p_is_armed", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"pl_transport_uuid", "pl_id", "pld_patrol_day", "plm_leader", "plm_pilot" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

		String[] waypoints = { "uuid", "id", "x", "y", "time", "direction", "distance", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
				"wp_comment" }; //$NON-NLS-1$
		
		String[] observations = { "uuid", "category_uuid" }; //$NON-NLS-1$ //$NON-NLS-2$
		
		String[] obAttributes = { "attribute_uuid", "number_value", //$NON-NLS-1$ //$NON-NLS-2$
				"string_value", "list_element_uuid", "tree_node_uuid" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String[] attributes = {"keyid"}; //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < ca.length; i++) {
			if (i != 0) {
				sb.append(","); //$NON-NLS-1$
			}
			sb.append(tablePrefix.get(ConservationArea.class) + "." + ca[i] + " as ca_" + ca[i]); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		for (int i = 0; i < results.length; i++) {
			sb.append(","); //$NON-NLS-1$
			sb.append("r." + results[i] + " as r_" + results[i]); //$NON-NLS-1$ //$NON-NLS-2$
		}

		for (int i = 0; i < waypoints.length; i++) {
			sb.append(","); //$NON-NLS-1$
			sb.append(tablePrefix.get(Waypoint.class) + "." + waypoints[i] //$NON-NLS-1$
					+ " as w_" + waypoints[i]); //$NON-NLS-1$
		}

		if (includeObservations) {
			for (int i = 0; i < observations.length; i++) {
				sb.append(","); //$NON-NLS-1$
				sb.append(tablePrefix.get(WaypointObservation.class) + "." //$NON-NLS-1$
						+ observations[i] + " as o_" + observations[i]); //$NON-NLS-1$
			}
			for (int i = 0; i < obAttributes.length; i++) {
				sb.append(","); //$NON-NLS-1$
				sb.append(tablePrefix.get(WaypointObservationAttribute.class)
						+ "." + obAttributes[i] + " as a_" + obAttributes[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for (int i = 0; i < attributes.length; i++) {
				sb.append(","); //$NON-NLS-1$
				sb.append(tablePrefix.get(Attribute.class)
						+ "." + attributes[i] + " as att_" + attributes[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			for (int i = 0; i < observations.length; i++) {
				sb.append(","); //$NON-NLS-1$
				sb.append(" cast(null as char(16) for bit data)"); //$NON-NLS-1$
			}
			for (int i = 0; i < obAttributes.length; i++) {
				sb.append(","); //$NON-NLS-1$
				if (i == 1) {
					sb.append(" cast(null as double)"); //$NON-NLS-1$
				} else if (i == 2) {
					sb.append(" cast(null as varchar(1024))"); //$NON-NLS-1$
				} else {
					sb.append(" cast(null as char(16) for bit data)"); //$NON-NLS-1$
				}
			}
			for (int i = 0; i < attributes.length; i++) {
				sb.append(","); //$NON-NLS-1$
				sb.append(" cast(null as varchar(128))"); //$NON-NLS-1$
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
			return "r.ob_uuid is not null"; //$NON-NLS-1$
		} else {
			return "r.ob_uuid is null"; //$NON-NLS-1$
		}
	}

	/**
	 * Builds the from clause
	 * @param includeObservations
	 * @return
	 */
	protected String FromClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();

		sql.append(queryTempTable);
		sql.append(" r"); //$NON-NLS-1$

		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(tableNames.get(ConservationArea.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(tablePrefix.get(ConservationArea.class));
		sql.append(" on " + tablePrefix.get(ConservationArea.class) //$NON-NLS-1$
				+ ".uuid = r.p_ca_uuid "); //$NON-NLS-1$

		if (includeObservations) {
			sql.append(" inner join "); //$NON-NLS-1$
			sql.append(tableNames.get(WaypointObservation.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(" on " + tablePrefix.get(WaypointObservation.class) //$NON-NLS-1$
					+ ".uuid = r.ob_uuid "); //$NON-NLS-1$

			sql.append(" inner join "); //$NON-NLS-1$
			sql.append(tableNames.get(Waypoint.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = " //$NON-NLS-1$ //$NON-NLS-2$
					+ tablePrefix.get(WaypointObservation.class) + ".wp_uuid "); //$NON-NLS-1$
		} else {
			sql.append(" inner join "); //$NON-NLS-1$
			sql.append(tableNames.get(Waypoint.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class) //$NON-NLS-1$
					+ ".uuid = r.wp_uuid "); //$NON-NLS-1$
		}

		if (includeObservations) {
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(tableNames.get(WaypointObservationAttribute.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservationAttribute.class));
			sql.append(" on " + tablePrefix.get(WaypointObservation.class) //$NON-NLS-1$
					+ ".uuid = " //$NON-NLS-1$
					+ tablePrefix.get(WaypointObservationAttribute.class)
					+ ".observation_uuid"); //$NON-NLS-1$
			
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(tableNames.get(Attribute.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Attribute.class));
			sql.append(" on " + tablePrefix.get(Attribute.class) //$NON-NLS-1$
					+ ".uuid = " //$NON-NLS-1$
					+ tablePrefix.get(WaypointObservationAttribute.class)
					+ ".attribute_uuid"); //$NON-NLS-1$
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
			return "varchar(128)"; //keyid //$NON-NLS-1$
		case TREE:
			return "varchar(32672)"; ///hkey //$NON-NLS-1$
		case NUMERIC:
			return "double"; //$NON-NLS-1$
		case BOOLEAN:
			return "double"; //$NON-NLS-1$
		case TEXT:
			return "varchar(1024)"; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$

	}
}
