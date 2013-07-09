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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
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

/**
 * Query engine for patrol queries.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyPatrolEngine extends DerbyQueryEngine2{

	private List<QueryResultItem> myResults;
	private String queryDataTable;

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
		queryDataTable = createTempTableName();
		
		myResults = null;
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyPatrolEngine_Progress_RunningQuery, 4);
				FilterProcessor data = new FilterProcessor(queryDataTable, DerbyPatrolEngine.this);
				
				try {
					data.processFilter(c, query.getFilter(), query.getDateFilter(), 
							query.getConservationAreaFilterAsFilter(), 
							false, false, monitor);
					
					monitor.subTask(Messages.DerbyPatrolEngine_Progress_LoadingResults);
					myResults = getResults(c, session);
					
					monitor.worked(1);
				} finally {
					// ensure temporary tables get dropped
					data.dropTemporaryTables(c);
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
		sql.append(queryDataTable);
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
	protected String getTemporaryTableCreateClause(String tableName) {

		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_id varchar(32),"); //$NON-NLS-1$
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
		return sql.toString();
	}
	
	
	/**
	 * Creates the temporary table that holds the query results.
	 * 
	 * @param <code>true</code> if observation (waypoint uuid, waypoint observation uuid) need to
	 * be populated; otherwise
	 * these fields will be left blank.
	 */
	@Override
	protected String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".id, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".station_uuid, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".team_uuid, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".objective, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".mandate_uuid, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".patrol_type, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".is_armed, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(prefix(Patrol.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(prefix(PatrolLeg.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(prefix(PatrolLeg.class) + ".id, "); //$NON-NLS-1$
		sql.append(prefix(PatrolLeg.class) + ".transport_uuid, "); //$NON-NLS-1$
		sql.append(prefix(PatrolLeg.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(prefix(PatrolLeg.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegDay.class) + ".patrol_day, "); //$NON-NLS-1$
		
		if (includeObservations){
			sql.append(prefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as char for bit data),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as char for bit data),");	//wpob_uuid //$NON-NLS-1$
		}
		sql.append(prefix(PatrolLegMember.class) + "_leader.employee_uuid, "); //$NON-NLS-1$
		sql.append(prefix(PatrolLegMember.class) + "_pilot.employee_uuid, "); //$NON-NLS-1$
		sql.append(prefix(Track.class) + ".geometry"); //$NON-NLS-1$
		return sql.toString();
	}
	
	protected String appendFromClause(HashSet<Class<?>> tables){
		if (!tables.contains(Track.class)){
			StringBuilder sb = new StringBuilder();
			sb.append(" left join "); //$NON-NLS-1$
			sb.append(namePrefix(Track.class));
			sb.append(" on "); //$NON-NLS-1$
			sb.append(prefix(Track.class));
			sb.append(".patrol_leg_day_uuid = "); //$NON-NLS-1$
			sb.append(prefix(PatrolLegDay.class));
			sb.append(".uuid"); //$NON-NLS-1$
			tables.add(Track.class);
			return sb.toString();
		}
		return ""; //$NON-NLS-1$
	}
	
	protected void  buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException{
		super.buildTemporaryTableIndexes(c, tableName);
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_wp_uuid_idx on " +  tableName + "(wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
}
