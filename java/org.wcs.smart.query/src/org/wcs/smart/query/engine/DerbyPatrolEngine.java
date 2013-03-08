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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
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
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.PatrolQuery;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.internal.filter.AreaFilter;

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
				monitor.beginTask(Messages.DerbyPatrolEngine_Progress_RunningQuery, 4);

				try {
					monitor.subTask(Messages.DerbyPatrolEngine_Progress_CreatingObservationTable);
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

					monitor.subTask(Messages.DerbyPatrolEngine_Progress_CreatingTempTable);
					createTemporaryTable(c);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbyPatrolEngine_Progress_PopulatingResults);
					populateTemporaryTable(query.getFilter(), query.getDateFilter(), query.getConservationAreaFilterAsFilter(), false, c, false);
					monitor.worked(1);
					if (monitor.isCanceled()){
						return;
					}
					
					monitor.subTask(Messages.DerbyPatrolEngine_Progress_LoadingResults);
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
	protected List<QueryResultItem> getResults(Connection c, Session session)
			throws SQLException {
		List<QueryResultItem> items = new ArrayList<QueryResultItem>();

		StringBuilder sql = new StringBuilder();
		
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(buildSelectClause());
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(buildFromClause());
		sql.append(" ORDER BY p_id, pl_uuid "); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());

		try {
			byte[] lastPlUuid = null;
			QueryResultItem lastItem = null;
			while (rs.next()) {
				
				byte[] pluuid = rs.getBytes(21);
				if (Arrays.equals(pluuid, lastPlUuid)){
					lastItem.addTrack(rs.getBytes(20));
				}else{
					QueryResultItem it = new QueryResultItem();
					byte[] cauuid = rs.getBytes(3);
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
					it.setPatrolLegStartDate(rs.getDate(16));
					it.setPatrolLegEndDate(rs.getDate(17));
					it.setLeader(getEmployeeName(rs.getBytes(18), session));
					it.setPilot(getEmployeeName(rs.getBytes(19), session));
					it.addTrack(rs.getBytes(20));
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
		String[] ca = {"id", "name"}; //$NON-NLS-1$ //$NON-NLS-2$
		
		String[] results = {"p_ca_uuid", "p_uuid", "p_id", "p_start_date", "p_end_date", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				"p_station_uuid", "p_team_uuid",  //$NON-NLS-1$ //$NON-NLS-2$
				"p_objective", "p_mandate_uuid", "p_type", "p_is_armed", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"pl_transport_uuid", "pl_id", //$NON-NLS-1$ //$NON-NLS-2$
				"pl_start_date", //$NON-NLS-1$
				"pl_end_date", //$NON-NLS-1$
				//"pld_patrol_day", 
				"plm_leader",  //$NON-NLS-1$
				"plm_pilot", "track", "pl_uuid" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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
		return sb.toString();
	}


	/**
	 * Builds the from clause
	 */
	private String buildFromClause() {
		StringBuilder sql = new StringBuilder();
		sql.append(queryTempTable);
		sql.append(" r"); //$NON-NLS-1$

		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(tableNames.get(ConservationArea.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(tablePrefix.get(ConservationArea.class));
		sql.append(" on " + tablePrefix.get(ConservationArea.class) //$NON-NLS-1$
				+ ".uuid = r.p_ca_uuid "); //$NON-NLS-1$

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
		sql.append("pl_start_date date,"); //$NON-NLS-1$
		sql.append("pl_end_date date,"); //$NON-NLS-1$
		sql.append("pld_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pld_patrol_day date,"); //$NON-NLS-1$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_leader char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_pilot char(16) for bit data,"); //$NON-NLS-1$
		sql.append("track blob)"); //$NON-NLS-1$

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		//-- add indexes 
		sql = new StringBuilder();
//		sql.append("CREATE INDEX " + queryTempTable + "_wp_uuid_idx on " + QUERY_TEMP_SCHEMA + "." + queryTempTable + "(wp_uuid)");
		sql.append("CREATE INDEX " + queryTempTable + "_wp_uuid_idx on " + queryTempTable + "(wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
//		sql.append("CREATE INDEX " + queryTempTable + "_ob_uuid_idx on " + QUERY_TEMP_SCHEMA + "." + queryTempTable + "(ob_uuid)");
		sql.append("CREATE INDEX " + queryTempTable + "_ob_uuid_idx on " +  queryTempTable + "(ob_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
//		sql.append(tablePrefix.get(Patrol.class) + ".objective_rating, ");
		sql.append(tablePrefix.get(Patrol.class) + ".objective, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".mandate_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".patrol_type, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".is_armed, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Patrol.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".transport_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLeg.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegDay.class) + ".patrol_day, "); //$NON-NLS-1$
		
		sql.append("cast(null as char for bit data), ");	//waypoint uuid //$NON-NLS-1$
		sql.append("cast(null as char for bit data), "); 	//observation uuid //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_leader.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(PatrolLegMember.class) + "_pilot.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Track.class) + ".geometry"); //$NON-NLS-1$

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
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(filter);
			}
		}
		sql.append(" left join "); //$NON-NLS-1$
		sql.append(tableNames.get(Track.class));
		sql.append(" "); //$NON-NLS-1$
		sql.append(tablePrefix.get(Track.class));
		sql.append(" on " + tablePrefix.get(Track.class) + ".patrol_leg_day_uuid = " //$NON-NLS-1$ //$NON-NLS-2$
				+ tablePrefix.get(PatrolLegDay.class) + ".uuid "); //$NON-NLS-1$
		
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
		
		if (queryFilter.hasCategoryFilter() || queryFilter.hasAttributeFilter()) {
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(tableNames.get(Waypoint.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(Waypoint.class));
			sql.append(" on " + tablePrefix.get(PatrolLegDay.class) //$NON-NLS-1$
					+ ".uuid = " + tablePrefix.get(Waypoint.class) //$NON-NLS-1$
					+ ".leg_day_uuid "); //$NON-NLS-1$
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(tableNames.get(WaypointObservation.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(tablePrefix.get(WaypointObservation.class));
			sql.append(" on " + tablePrefix.get(Waypoint.class) + ".uuid = " //$NON-NLS-1$ //$NON-NLS-2$
					+ tablePrefix.get(WaypointObservation.class) + ".wp_uuid "); //$NON-NLS-1$

			if (queryFilter != IFilter.EMPTY_FILTER) {
				if (queryFilter.hasAttributeFilter()
						|| queryFilter.hasCategoryFilter()) {
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
						sql.append(observationTempTable
								+ " qa on qa.observation_uuid = "); //$NON-NLS-1$
						sql.append(tablePrefix.get(WaypointObservation.class)
								+ ".uuid"); //$NON-NLS-1$

					}
				}
			}
		}
		
		// area filters
		LinkedList<IFilter> kidsToProcess = new LinkedList<IFilter>();
		kidsToProcess.add(queryFilter);
		Set<String> processedAreaFilters = new HashSet<String>();
		while (kidsToProcess.size() > 0) {
			IFilter kid = kidsToProcess.poll();
			if (kid instanceof AreaFilter) {
				AreaFilter ff = (AreaFilter) kid;
				String tableName = ff.getType().name() + "_" + ff.getKey(); //$NON-NLS-1$
				if (!processedAreaFilters.contains(tableName)) {
					processedAreaFilters.add(tableName);
					// TODO: escape special characters from the key
					sql.append(" left join "); //$NON-NLS-1$
					sql.append(tableNames.get(Area.class));
					sql.append(" as "); //$NON-NLS-1$
					sql.append(tableName);
					sql.append(" on "); //$NON-NLS-1$
					sql.append(tableName + ".ca_uuid = " //$NON-NLS-1$
							+ tablePrefix.get(Patrol.class) + ".ca_uuid and "); //$NON-NLS-1$
					sql.append(tableName + ".area_type = '" //$NON-NLS-1$
							+ ff.getType().name() + "' and "); //$NON-NLS-1$
					sql.append(tableName + ".keyid = '" + ff.getKey() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			if (kid.getChildren() != null) {
				kidsToProcess.addAll(kid.getChildren());
			}
		}
				
		// ---- WHERE CLAUSE -----
		if (queryFilter != IFilter.EMPTY_FILTER) {
			String filter = queryFilter.asSql(tablePrefix);
			if (filter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(filter);
			}
		}

		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
}
