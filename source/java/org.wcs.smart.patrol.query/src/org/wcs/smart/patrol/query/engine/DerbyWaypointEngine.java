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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolWaypointAttachmentResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointResultItem;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.WaypointQueryEngine;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for executing lazy queries using derby.
 * This engines create temporary tables that one to one correspond with the table
 * that user see. {@link DerbyPagedObservationResult} obtains the name of this table and is
 * responsible for all other operations (fetching/sorting/deleting tables)
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyWaypointEngine extends AbstractPatrolQueryEngine implements WaypointQueryEngine<PatrolWaypointResultItem>, IDerbyWaypointEngine {

	private String queryDataTable;
	private Session session;
	private List<String> patrolAttributes = null;
	
	@Override
	public boolean canExecute(String querytype) {
		return PatrolWaypointQuery.KEY.equals(querytype);
	}
	
	@Override
	public Session getCurrentConnection() {
		return session;
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
		session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
	
		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		final DerbyPagedWaypointResult result = new DerbyPagedWaypointResult(this);
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyQueryEngine2_Progress_RunningQuery, 2);
				
				IFilterProcessor filterer = null;
				try{
					filterer = DerbyWaypointEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, query);
				}catch (Exception ex){
					throw new SQLException (ex);
				}
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));
				
				//turn on auto-commit because we want ddl to commit immediately so we don't lock up the database
				//need to make sure we cleanup all temp tables correctly
				c.setAutoCommit(true);
				try {			
					ConservationAreaFilter cafilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter,  cafilter, false, true, progress.split(1));
					
					populateTemporaryTableExtra(c, session, progress.split(1));
					
					progress.subTask(Messages.DerbyObservationEngine_Progress_FetchSize);
					updateResultCount(session, result);
					
					progress.subTask(Messages.DerbyObservationEngine_LoadingResultTask);
				}catch( OperationCanceledException ex) {
					return ;
				}catch (Exception ex){
					throw new SQLException(ex);
				} finally {
					filterer.dropTemporaryTables(c);
					if (progress.isCanceled()) dropTables(c);
					c.setAutoCommit(false);
				}
			}

		});
		return result;
	}
	
	@Override
	public void updateResultCount(Session s, IUpdateableResultSet result){
		//setting result size
		DerbyPagedWaypointResult results = (DerbyPagedWaypointResult)result;
		
		//setting result size
		Integer count = (Integer) s.createNativeQuery("select count(*) from " + queryDataTable).uniqueResult(); //$NON-NLS-1$
		results.setItemCount(count);
	}

	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	@Override
	public void dropTables(Connection c) throws SQLException {
		dropTable(c, queryDataTable);
	}

	private void populateTemporaryTableNameObjExtra(String uuidColumn, String nameColumn, Connection c, Session session) throws SQLException {
		String sql = "SELECT DISTINCT ca_uuid, "+uuidColumn+" FROM "+queryDataTable;  //$NON-NLS-1$//$NON-NLS-2$
		QueryPlugIn.logSql(sql);
		
		try(ResultSet rs = c.createStatement().executeQuery(sql)) {
			PreparedStatement statement = c.prepareStatement("UPDATE "+ queryDataTable +" SET "+nameColumn+" = ? where "+uuidColumn+" = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			int count = 0;
			while (rs.next()) {
				byte[] ca_uuid = rs.getBytes(1);
				byte[] uuid = rs.getBytes(2);
				if (uuid == null || ca_uuid == null)
					continue;
				String name = getName(UuidUtils.byteToUUID(uuid), UuidUtils.byteToUUID(ca_uuid), session);
				statement.setString(1, name);
				statement.setBytes(2, uuid);
				statement.addBatch();
				count ++;
				if (count > 100){
					statement.executeBatch();
					count = 0;
				}				
			}
			statement.executeBatch();
			
		}
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session, IProgressMonitor monitor) throws SQLException {
		SubMonitor progress = SubMonitor.convert(monitor, 18);
		
		//NOTE: does 50 worked for monitor in total
		String[][] columnsToAdd = new String[][]{
				{"p_station","varchar(1024)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{"p_team","varchar(1024)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{"p_mandate","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"p_transporttype","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"p_leader","varchar(164)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"p_pilot","varchar(164)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"wp_lastmodifiedbyname", "varchar(512)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		progress.subTask(Messages.DerbyObservationEngine_Progress_StationData);
		progress.split(3);
		populateTemporaryTableNameObjExtra("p_station_uuid", "p_station", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		
		progress.subTask(Messages.DerbyObservationEngine_Progress_TeamData);
		progress.split(3);
		populateTemporaryTableNameObjExtra("p_team_uuid", "p_team", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		
		progress.subTask(Messages.DerbyObservationEngine_Progress_MandateData);
		progress.split(3);
		populateTemporaryTableNameObjExtra("pl_mandate_uuid", "p_mandate", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		
		progress.subTask(Messages.DerbyObservationEngine_Progress_TransportData);
		progress.split(3);
		populateTemporaryTableNameObjExtra("pl_transport_uuid", "p_transporttype", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		
		progress.subTask(Messages.DerbyObservationEngine_Progress_LeaderPilotData);
		progress.split(4);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT plm_leader FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" UNION SELECT DISTINCT plm_pilot FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" UNION SELECT DISTINCT wp_lastmodifiedby FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		QueryPlugIn.logSql(sql.toString());
		
		String updateSql = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		
		String q1 = updateSql + "p_leader = ? where plm_leader = ?"; //$NON-NLS-1$
		String q2 = updateSql + "p_pilot = ? where plm_pilot = ?"; //$NON-NLS-1$
		String q3 = updateSql + "wp_lastmodifiedbyname = ? where wp_lastmodifiedby = ?"; //$NON-NLS-1$
		QueryPlugIn.logSql(q1);
		QueryPlugIn.logSql(q2);
		QueryPlugIn.logSql(q3);
		PreparedStatement leaderSt = c.prepareStatement(q1);
		PreparedStatement pilotSt = c.prepareStatement(q2);
		PreparedStatement lastmodifiedSt = c.prepareStatement(q3);
		int cnt = 0;
		try (ResultSet rs = c.createStatement().executeQuery(sql.toString())){
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				String name = getEmployeeName(UuidUtils.byteToUUID(uuid), session);
				
				if (name != null) {
					leaderSt.setString(1, name);
					leaderSt.setBytes(2, uuid);
					leaderSt.addBatch();

					pilotSt.setString(1, name);
					pilotSt.setBytes(2, uuid);
					pilotSt.addBatch();
					
					lastmodifiedSt.setString(1, name);
					lastmodifiedSt.setBytes(2, uuid);
					lastmodifiedSt.addBatch();
					
					cnt++;
					if (cnt >= 100){
						pilotSt.executeBatch();
						leaderSt.executeBatch();
						lastmodifiedSt.executeBatch();
						cnt = 0;
					}
				}
			}
			pilotSt.executeBatch();
			leaderSt.executeBatch();
			lastmodifiedSt.executeBatch();
		}
		
		//ca information
		progress.split(2);
		if (SmartDB.isMultipleAnalysis()){
			//ca id and names are only used for cross-ca analysis
			progress.subTask(Messages.DerbyObservationEngine_Progress_CaInfo);
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(AbstractPatrolQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(AbstractPatrolQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
		
		patrolAttributes = addPatrolAttributesToQueryResult(queryDataTable, c, session);

		
	}

	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".station_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".team_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".objective, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".mandate_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".patrol_type, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".is_armed, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Patrol.class) + ".end_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLeg.class) + ".transport_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegDay.class) + ".uuid, "); //$NON-NLS-1$

		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".last_modified, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".last_modified_by, "); //$NON-NLS-1$
//		sql.append(prefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
//		sql.append(prefix(WaypointObservation.class) + ".category_uuid, "); //$NON-NLS-1$

		sql.append(tablePrefix(PatrolLegMember.class) + "_leader.employee_uuid as leader_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(PatrolLegMember.class) + "_pilot.employee_uuid as pilot_uuid "); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_id varchar(256),"); //$NON-NLS-1$
		sql.append("p_station_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_team_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_objective varchar(8192),"); //$NON-NLS-1$
		sql.append("pl_mandate_uuid  char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_type varchar(6),"); //$NON-NLS-1$
		sql.append("p_armed boolean,"); //$NON-NLS-1$
		sql.append("p_startdate date,"); //$NON-NLS-1$
		sql.append("p_enddate date,"); //$NON-NLS-1$
		sql.append("pl_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("p_legid varchar(50),"); //$NON-NLS-1$
		sql.append("pl_transport_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("pld_uuid char(16) for bit data,"); //$NON-NLS-1$

		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_id varchar(256),"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("wp_lastmodified timestamp,"); //$NON-NLS-1$
		sql.append("wp_lastmodifiedby char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_leader char(16) for bit data,"); //$NON-NLS-1$
		sql.append("plm_pilot char(16) for bit data"); //$NON-NLS-1$

		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public PatrolWaypointAttachmentResultItem asQueryAttachmentResultItem(ResultSet rs, Session session) throws SQLException{
		PatrolWaypointAttachmentResultItem item = (PatrolWaypointAttachmentResultItem) asQueryResultItemInternal(true,  rs, session);
		
		UUID auuid = UuidUtils.byteToUUID(rs.getBytes("attach_uuid")); //$NON-NLS-1$
		ISmartAttachment a = session.get(ObservationAttachment.class, auuid);
		if (a == null) {
			a = session.get(WaypointAttachment.class, auuid);
		}
		try {
			a.computeFileLocation(session);
		} catch (Exception e) {
			PatrolQueryPlugIn.log(e.getMessage(), e);
		}
		item.setAttachment(a);
		return item;
	}

	public PatrolWaypointResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		return asQueryResultItemInternal(false,  rs, session);
	}
	
	protected PatrolWaypointResultItem asQueryResultItemInternal(boolean isAttachment, ResultSet rs, Session session) throws SQLException{
		
		PatrolWaypointResultItem it;
		if (isAttachment) {
			it = new PatrolWaypointAttachmentResultItem();
		}else {
			it = new PatrolWaypointResultItem();	
		}
		
		it.setConservationAreaUuid(UuidUtils.byteToUUID(rs.getBytes("ca_uuid"))); //$NON-NLS-1$
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setPatrolUuid(UuidUtils.byteToUUID(rs.getBytes("p_uuid"))); //$NON-NLS-1$
		it.setPatrolId(rs.getString("p_id")); //$NON-NLS-1$
		it.setPatrolStartDate(rs.getDate("p_startdate").toLocalDate()); //$NON-NLS-1$
		it.setPatrolEndDate(rs.getDate("p_enddate").toLocalDate()); //$NON-NLS-1$
		it.setStation(rs.getString("p_station"));				 //$NON-NLS-1$
		it.setTeam(rs.getString("p_team"));	 //$NON-NLS-1$
		it.setObjective(rs.getString("p_objective")); //$NON-NLS-1$
		it.setMandate(rs.getString("p_mandate")); //$NON-NLS-1$
		it.setPatrolType(PatrolType.Type.valueOf(rs.getString("p_type"))); //$NON-NLS-1$
		it.setArmed(rs.getBoolean("p_armed")); //$NON-NLS-1$
		it.setTransportType(rs.getString("p_transporttype")); //$NON-NLS-1$
		it.setPatrolLegId(rs.getString("p_legid")); //$NON-NLS-1$
		
		it.setLeader(rs.getString("p_leader")); //$NON-NLS-1$
		it.setPilot(rs.getString("p_pilot")); //$NON-NLS-1$
		it.setWaypointUuid(UuidUtils.byteToUUID(rs.getBytes("wp_uuid"))); //$NON-NLS-1$
		it.setWaypointId(rs.getString("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointDateTime(rs.getTimestamp("wp_time").toLocalDateTime()); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setLastModifiedDate(rs.getTimestamp("wp_lastmodified").toLocalDateTime()); //$NON-NLS-1$
		it.setLastModifiedBy(rs.getString("wp_lastmodifiedbyname")); //$NON-NLS-1$
		
		if (patrolAttributes != null) {
			for (String s : patrolAttributes) {
				it.setPatrolAttribute(s.substring(PatrolAttributeQueryColumn.PREFIX.length()+1), rs.getObject(s));
			}
		}
		
		return it;
	}
	
	@Override
	public void createTemporaryTableIndexes(Connection c, String tableName) throws SQLException {
	}

	@Override
	public String getQueryDataTable() {
		return queryDataTable;
	}
	
	
}
