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
package org.wcs.smart.patrol.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for patrol queries.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyPatrolEngine extends DerbyPatrolQueryEngine{

	private MemoryQueryResult<PatrolQueryResultItem> myResults;
	private String queryDataTable;

	@Override
	public boolean canExecute(String querytype) {
		return PatrolQuery.KEY.equals(querytype);
	}
	
	/**
	 * Runs the given patrol query and retrieves the results from the database.
	 * 
	 * @param query
	 * @param session
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	@Override
	public IQueryResult executeQuery(
			Query lquery,
			HashMap<String, Object> parameters) throws SQLException{

		final SimpleQuery query = (SimpleQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
	
		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		myResults = null;

		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyPatrolEngine_Progress_RunningQuery, 4);
				IFilterProcessor filterer = null;
				try{
					filterer = DerbyPatrolEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable);
				}catch (Exception ex){
					throw new SQLException (ex);
				}
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				
				try {
					try{
						ConservationAreaFilter cafilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
						filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							cafilter, false, false, monitor);
					}catch (Exception ex){
						throw new SQLException (ex);
					}
					if (monitor.isCanceled()){
						return;
					}
					monitor.subTask(Messages.DerbyPatrolEngine_Progress_LoadingResults);
					myResults = new MemoryQueryResult<PatrolQueryResultItem>(getResults(c, session));
					
					monitor.worked(1);
				} finally {
					// ensure temporary tables get dropped
					filterer.dropTemporaryTables(c);
					monitor.done();
				}
				c.commit();
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
	protected List<PatrolQueryResultItem> getResults(Connection c, Session session)
			throws SQLException {
		List<PatrolQueryResultItem> items = new ArrayList<PatrolQueryResultItem>();

		StringBuilder sql = new StringBuilder();
		
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(buildSelectClause());
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(buildFromClause());
		sql.append(" ORDER BY p_id, pl_uuid, pld_uuid "); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		

		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			byte[] lastPlUuid = null;
			byte[] lastPldUuid = null;
			PatrolQueryResultItem lastItem = null;
			while (rs.next()) {
				byte[] pluuid = rs.getBytes("r_pl_uuid"); //$NON-NLS-1$
				byte[] plduuid = rs.getBytes("r_pld_uuid"); //$NON-NLS-1$
				if (Arrays.equals(pluuid, lastPlUuid)){
					if (!Arrays.equals(plduuid, lastPldUuid)){
						//same patrol; different leg
						lastItem.addTrack(rs.getBytes(20));
					}
				}else{
					PatrolQueryResultItem it = asQueryResultItem(rs, session);
					items.add(it);
					lastItem = it;
				}
				lastPlUuid = pluuid;
				lastPldUuid = plduuid;
			}
		}
		return items;
	}
	
	
	/**
	 * Build select clause 
	 * 
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
				"plm_pilot", "track", "pl_uuid", "pld_uuid" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

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
		sql.append(tablePrefix(Patrol.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".station_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".team_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".objective, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".mandate_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".patrol_type, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".is_armed, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".transport_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegDay.class) + ".patrol_day, "); //$NON-NLS-1$
		
		if (includeObservations){
			sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		}else{
			sql.append("cast(null as char for bit data),");	//wp_uuid //$NON-NLS-1$
			sql.append("cast(null as char for bit data),");	//wpob_uuid //$NON-NLS-1$
		}
		sql.append(tablePrefix(PatrolLegMember.class) + "_leader.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegMember.class) + "_pilot.employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Track.class) + ".geometry"); //$NON-NLS-1$
		return sql.toString();
	}
	
	protected String appendFromClause(HashSet<Class<?>> tables){
		if (!tables.contains(Track.class)){
			StringBuilder sb = new StringBuilder();
			sb.append(" left join "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Track.class));
			sb.append(" on "); //$NON-NLS-1$
			sb.append(tablePrefix(Track.class));
			sb.append(".patrol_leg_day_uuid = "); //$NON-NLS-1$
			sb.append(tablePrefix(PatrolLegDay.class));
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

	@Override
	protected PatrolQueryResultItem asQueryResultItem(ResultSet rs, Session session)
			throws SQLException {
		
		PatrolQueryResultItem it = new PatrolQueryResultItem();
		UUID cauuid = UuidUtils.byteToUUID(rs.getBytes("r_p_ca_uuid")); //$NON-NLS-1$
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setPatrolUuid(UuidUtils.byteToUUID(rs.getBytes("r_p_uuid"))); //$NON-NLS-1$
		it.setPatrolId(rs.getString("r_p_id")); //$NON-NLS-1$
		it.setPatrolStartDate(rs.getDate("r_p_start_date")); //$NON-NLS-1$
		it.setPatrolEndDate(rs.getDate("r_p_end_date")); //$NON-NLS-1$
		it.setStation(getName(UuidUtils.byteToUUID(rs.getBytes("r_p_station_uuid")), cauuid, session));				 //$NON-NLS-1$
		it.setTeam(getName(UuidUtils.byteToUUID(rs.getBytes("r_p_team_uuid")), cauuid, session));				 //$NON-NLS-1$
		it.setObjective(rs.getString("r_p_objective")); //$NON-NLS-1$
		it.setMandate(getName(UuidUtils.byteToUUID(rs.getBytes("r_p_mandate_uuid")), cauuid, session)); //$NON-NLS-1$
		it.setPatrolType(PatrolType.Type.valueOf(rs.getString("r_p_type"))); //$NON-NLS-1$
		it.setArmed(rs.getBoolean("r_p_is_armed")); //$NON-NLS-1$
		it.setTransportType(getName(UuidUtils.byteToUUID(rs.getBytes("r_pl_transport_uuid")), cauuid, session)); //$NON-NLS-1$
		it.setPatrolLegId(rs.getString("r_pl_id")); //$NON-NLS-1$
		it.setPatrolLegStartDate(rs.getDate("r_pl_start_date")); //$NON-NLS-1$
		it.setPatrolLegEndDate(rs.getDate("r_pl_end_date")); //$NON-NLS-1$
		it.setLeader(getEmployeeName(UuidUtils.byteToUUID(rs.getBytes("r_plm_leader")), session)); //$NON-NLS-1$
		it.setPilot(getEmployeeName(UuidUtils.byteToUUID(rs.getBytes("r_plm_pilot")), session)); //$NON-NLS-1$
		it.addTrack(rs.getBytes("r_track")); //$NON-NLS-1$
	

		return it;
	}
	
}
