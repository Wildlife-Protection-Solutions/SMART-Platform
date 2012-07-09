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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.patrol.PatrolQuery;
import org.wcs.smart.query.parser.internal.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.filter.DateFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;

/**
 * Query engine for patrol queries.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyPatrolEngine extends DerbyQueryEngine2{

	private List<QueryResultItem> myResults;
	

	/**
	 * Runs the given patrol query and retrieves the results from the database.
	 * 
	 * @param query
	 * @param session
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	public List<QueryResultItem> executeQuery(
			final PatrolQuery query,
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
					populateTemporaryTable(query.getFilter(), query.getDateFilter(), query.getConservationAreaFilter(), false, c);
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
	 * Reads the results from the temporary query table
	 * and loads them into internal memory store
	 * 
	 * @param c database connection 
	 * @param session hibernate session
	 * @return list of query results
	 * 
	 * @throws SQLException
	 */
	private List<QueryResultItem> getResults(Connection c, Session session)
			throws SQLException {
		List<QueryResultItem> items = new ArrayList<QueryResultItem>();

		StringBuilder sql = new StringBuilder();
		
		sql.append(" SELECT ");
		sql.append(buildSelectClause());
		sql.append(" FROM ");
		sql.append(buildFromClause());
		sql.append(" ORDER BY p_id, pl_uuid ");
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());

		try {
			byte[] lastPlUuid = null;
			QueryResultItem lastItem = null;
			while (rs.next()) {
				
				byte[] pluuid = rs.getBytes(18);
				if (Arrays.equals(pluuid, lastPlUuid)){
					lastItem.addTrack(rs.getBytes(17));
				}else{
					QueryResultItem it = new QueryResultItem();
					it.setPatrolUuid(rs.getBytes(1));
					it.setPatrolId(rs.getString(2));
					it.setPatrolStartDate(rs.getDate(3));
					it.setPatrolEndDate(rs.getDate(4));
					it.setStation(getStationName(rs.getBytes(5), session));				
					it.setTeam(getTeamName(rs.getBytes(6), session));				
					it.setObjective(rs.getString(7));
					it.setMandate(getMandateName(rs.getBytes(8), session));
					it.setPatrolType(PatrolType.Type.valueOf(rs.getString(9)));
					it.setArmed(rs.getBoolean(10));
					it.setTransportType(getTransportType(rs.getBytes(11), session));
					it.setPatrolLegId(rs.getString(12));
					it.setPatrolLegStartDate(rs.getDate(13));
					it.setPatrolLegEndDate(rs.getDate(14));
					it.setLeader(getEmployeeName(rs.getBytes(15), session));
					it.setPilot(getEmployeeName(rs.getBytes(16), session));
					it.addTrack(rs.getBytes(17));
					items.add(it);
					lastItem = it;
				}
				lastPlUuid = pluuid;
			
				
			}
		} finally {
			rs.close();
		}
		return items;
	}
	
	
	/**
	 * Build select clause 
	 * 
	 * @param includeObservations if observations should be included
	 * @return select clause
	 */
	private String buildSelectClause() {
		String[] results = { "p_uuid", "p_id", "p_start_date", "p_end_date",
				"p_station_uuid", "p_team_uuid", 
				"p_objective", "p_mandate_uuid", "p_type", "p_is_armed",
				"pl_transport_uuid", "pl_id",
				"pl_start_date",
				"pl_end_date",
				//"pld_patrol_day", 
				"plm_leader", 
				"plm_pilot", "track", "pl_uuid" };

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < results.length; i++) {
			if (i != 0) {
				sb.append(",");
			}
			sb.append("r." + results[i] + " as r_" + results[i]);
		}
		return sb.toString();
	}


	/**
	 * Builds the from clause
	 */
	private String buildFromClause() {
		StringBuilder sql = new StringBuilder();
		sql.append(queryTempTable);
		sql.append(" r");

		return sql.toString();
	}
	
	/**
	 * Creates the temporary table that holds the query results.
	 * 
	 * @param c database connection
	 * @throws SQLException
	 */
	@Override
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
		sql.append("pl_start_date date,");
		sql.append("pl_end_date date,");
		sql.append("pld_uuid char(16) for bit data,");
		sql.append("pld_patrol_day date,");
		sql.append("wp_uuid char(16) for bit data,");
		sql.append("ob_uuid char(16) for bit data,");
		sql.append("plm_leader char(16) for bit data,");
		sql.append("plm_pilot char(16) for bit data,");
		sql.append("track blob)");

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		//-- add indexes 
		sql = new StringBuilder();
//		sql.append("CREATE INDEX " + queryTempTable + "_wp_uuid_idx on " + QUERY_TEMP_SCHEMA + "." + queryTempTable + "(wp_uuid)");
		sql.append("CREATE INDEX " + queryTempTable + "_wp_uuid_idx on " + queryTempTable + "(wp_uuid)");
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
//		sql.append("CREATE INDEX " + queryTempTable + "_ob_uuid_idx on " + QUERY_TEMP_SCHEMA + "." + queryTempTable + "(ob_uuid)");
		sql.append("CREATE INDEX " + queryTempTable + "_ob_uuid_idx on " +  queryTempTable + "(ob_uuid)");
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	/**
	 * Populates the query temporary table.
	 * 
	 * @param q the query filter
	 * @param c the database connection
	 * 
	 * @throws SQLException
	 */
	@Override
	protected void populateTemporaryTable(IFilter queryFilter, 
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + queryTempTable );
		// ---- SELECT CLAUSE -----
		sql.append(" SELECT ");
		sql.append(tablePrefix.get(Patrol.class) + ".uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".id, ");
		sql.append(tablePrefix.get(Patrol.class) + ".station_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".team_uuid, ");
//		sql.append(tablePrefix.get(Patrol.class) + ".objective_rating, ");
		sql.append(tablePrefix.get(Patrol.class) + ".objective, ");
		sql.append(tablePrefix.get(Patrol.class) + ".mandate_uuid, ");
		sql.append(tablePrefix.get(Patrol.class) + ".patrol_type, ");
		sql.append(tablePrefix.get(Patrol.class) + ".is_armed, ");
		sql.append(tablePrefix.get(Patrol.class) + ".start_date, ");
		sql.append(tablePrefix.get(Patrol.class) + ".end_date, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".uuid, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".id, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".transport_uuid, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".start_date, ");
		sql.append(tablePrefix.get(PatrolLeg.class) + ".end_date, ");
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".uuid, ");
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".patrol_day, ");
		
		sql.append("cast(null as char for bit data), ");	//waypoint uuid
		sql.append("cast(null as char for bit data), "); 	//observation uuid
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.employee_uuid, ");
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.employee_uuid, ");
		sql.append(tablePrefix.get(Track.class) + ".geometry");

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
				sql.append(" AND ");
				sql.append(filter);
			}
		}
		sql.append(" left join ");
		sql.append(tableNames.get(Track.class));
		sql.append(" ");
		sql.append(tablePrefix.get(Track.class));
		sql.append(" on " + tablePrefix.get(Track.class) + ".patrol_leg_day_uuid = "
				+ tablePrefix.get(PatrolLegDay.class) + ".uuid ");
		
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
		
		if (queryFilter.hasCategoryFilter() || queryFilter.hasAttributeFilter()) {

			sql.append(tableNames.get(Waypoint.class));
			sql.append(" ");
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(PatrolLegDay.class)
					+ ".uuid = " + tablePrefix.get(Waypoint.class)
					+ ".leg_day_uuid ");
			sql.append(" left join ");
			sql.append(tableNames.get(WaypointObservation.class));
			sql.append(" ");
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = "
					+ tablePrefix.get(WaypointObservation.class) + ".wp_uuid ");

			if (queryFilter != IFilter.EMPTY_FILTER) {
				if (queryFilter.hasAttributeFilter()
						|| queryFilter.hasCategoryFilter()) {
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
						sql.append(observationTempTable
								+ " qa on qa.observation_uuid = ");
						sql.append(tablePrefix.get(WaypointObservation.class)
								+ ".uuid");

					}
				}
			}
		}
		// ---- WHERE CLAUSE -----
		if (queryFilter != IFilter.EMPTY_FILTER) {
			String filter = queryFilter.asSql(tablePrefix);
			if (filter.length() > 0) {
				sql.append(" WHERE ");
				sql.append(filter);
			}
		}

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
}
