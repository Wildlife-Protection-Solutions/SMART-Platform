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
package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.SurveyWaypointAttachmentResultItem;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.er.query.model.SurveyWaypointResultItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.query.ObservationQueryPlugIn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.test.WaypointQueryEngine;
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
public class DerbyWaypointEngine extends DerbySurveyQueryEngine implements WaypointQueryEngine<SurveyWaypointResultItem> {

	private String queryDataTable;
	
	
	@Override
	public boolean canExecute(String querytype) {
		return SurveyWaypointQuery.KEY.equals(querytype);
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

		final SurveyWaypointQuery query = (SurveyWaypointQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());

		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		final DerbyPagedWaypointResult result = new DerbyPagedWaypointResult(this);
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyWaypointEngine_RunQueryProgress, 80);
				ConservationAreaFilter caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
				designFilter = null;
				if (query.getSurveyDesign() != null){
					designFilter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
				}
				IFilterProcessor filterer = null;
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));
				//turn on auto-commit because we want ddl to commit immediately so we don't lock up the database
				c.setAutoCommit(true);
				try {			
					filterer = DerbyWaypointEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, query);
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, 
							true, true, progress.split(50));
					
					populateTemporaryTableExtra(c, session,  progress.split(20));
					
					progress.checkCanceled();
					progress.subTask(Messages.DerbyWaypointEngine_CountProgress);
					//setting result size
					
					try(ResultSet rs = c.createStatement().executeQuery("select count(*) from " + queryDataTable)){ //$NON-NLS-1$
						if (rs.next()) { 
							result.setItemCount(rs.getInt(1));
						}
					}
					progress.worked(10);
				}catch(OperationCanceledException ex) {
					return ;
				}catch (Exception ex){
					throw new SQLException(ex);
				} finally {
					if (filterer != null) filterer.dropTemporaryTables(c);
					if (progress.isCanceled()) dropTables(c);
					c.setAutoCommit(false);
				}
			}
		});
		return result;
	}

	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	@Override
	public void dropTables(Connection c) throws SQLException {
		dropTable(c, getLabelDataTable());
		dropTable(c, getLabelDataTable());
		
	}

	private void populateTemporaryTableNameObjExtra(String uuidColumn, String nameColumn, Connection c, Session session) throws SQLException {
		String sql = "SELECT DISTINCT ca_uuid, " + uuidColumn + " FROM " + queryDataTable;  //$NON-NLS-1$//$NON-NLS-2$
		QueryPlugIn.logSql(sql);
		
		try(ResultSet rs = c.createStatement().executeQuery(sql)) {
			PreparedStatement statement = c.prepareStatement("UPDATE " + queryDataTable + " SET " + nameColumn + " = ? where " + uuidColumn + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyWaypointEngine_AdditionalDataProgress, 5);
		progress.split(1);
		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"surveydesign_name","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"mission_leader", "varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"wp_lastmodifiedbyname", "varchar(512)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		progress.split(1);

		progress.subTask(Messages.DerbyObservationEngine_progress3);
		populateTemporaryTableNameObjExtra("surveydesign_uuid", "surveydesign_name", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		progress.split(1);
		
		//ca information
		if (SmartDB.isMultipleAnalysis()){
			//ca id and names are only used for cross-ca analysis
			progress.subTask(Messages.DerbyObservationEngine_progress4);
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(DerbySurveyQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(DerbySurveyQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
		progress.split(1);
		
		// mission leader
		progress.subTask(Messages.DerbyWaypointEngine_LeaderProgress);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionMember.class));
		sql.append(".employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionMember.class));
		sql.append(".mission_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" a join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionMember.class));
		sql.append(" on a.mission_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionMember.class));
		sql.append(".mission_uuid"); //$NON-NLS-1$
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionMember.class));
		sql.append(".is_leader"); //$NON-NLS-1$
		
		String updateSql = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		updateSql += "mission_leader = ? where mission_uuid = ?"; //$NON-NLS-1$
		
		QueryPlugIn.logSql(updateSql);
		
		PreparedStatement leaderSt = c.prepareStatement(updateSql);
		
		int cnt = 0;
		QueryPlugIn.logSql(sql.toString());
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				String name = getEmployeeName(UuidUtils.byteToUUID(uuid), session);

				if (name != null) {
					leaderSt.setString(1, name);
					leaderSt.setBytes(2, rs.getBytes(2));
					leaderSt.addBatch();

					cnt++;
					if (cnt >= 100) {
						leaderSt.executeBatch();
						cnt = 0;
					}
				}
			}
			leaderSt.executeBatch();
		}
		
		//last modified
		sql = new StringBuilder();
		sql.append("SELECT DISTINCT wp_lastmodifiedby FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		
		String updateSql2 = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		updateSql2 += "wp_lastmodifiedbyname = ? where wp_lastmodifiedby = ?"; //$NON-NLS-1$
		
		QueryPlugIn.logSql(updateSql2);
		PreparedStatement modifiedSt = c.prepareStatement(updateSql2);
		cnt = 0;
		QueryPlugIn.logSql(sql.toString());
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				String name = getEmployeeName(UuidUtils.byteToUUID(uuid), session);

				if (name != null) {
					
					modifiedSt.setString(1, name);
					modifiedSt.setBytes(2, uuid);
					modifiedSt.addBatch();

					cnt++;
					if (cnt >= 100) {
						modifiedSt.executeBatch();
						cnt = 0;
					}
				}
			}
			modifiedSt.executeBatch();
		}
		
		progress.split(1);
		progress.subTask(Messages.DerbyWaypointEngine_ProgressMissionProperties);
		
		SurveyPagedResultUtils.populateAdditionalAttributeTable(true, true, true, getQueryDataTable(), getLabelDataTable(), this, c, session);
		progress.checkCanceled();
	}

	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".uuid, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(Survey.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".id, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(Mission.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".end_date, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(SamplingUnit.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnit.class) + ".id, "); //$NON-NLS-1$

		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".last_modified, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".last_modified_by "); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		
		sql.append("surveydesign_uuid char(16) for bit data,"); //$NON-NLS-1$
		
		sql.append("survey_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("survey_id varchar(128),"); //$NON-NLS-1$
		
		sql.append("mission_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("mission_id varchar(128),"); //$NON-NLS-1$
		sql.append("mission_startdate date,"); //$NON-NLS-1$
		sql.append("mission_enddate date,"); //$NON-NLS-1$
		
		sql.append("samplingunit_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("samplingunit_id varchar(128),"); //$NON-NLS-1$

		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_id varchar(32),"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$ 
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("wp_lastmodified timestamp,"); //$NON-NLS-1$
		sql.append("wp_lastmodifiedby char(16) for bit data"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public SurveyWaypointResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		return asQueryResultItemInternal(rs, false, session);
	}
	
	@Override
	public SurveyWaypointAttachmentResultItem asQueryAttachmentResultItem(ResultSet rs, Session session) throws SQLException{
		SurveyWaypointAttachmentResultItem item = (SurveyWaypointAttachmentResultItem) asQueryResultItemInternal(rs, true, session);
		UUID auuid = UuidUtils.byteToUUID(rs.getBytes("attach_uuid")); //$NON-NLS-1$
		ISmartAttachment a = session.get(ObservationAttachment.class, auuid);
		if (a == null) {
			a = session.get(WaypointAttachment.class, auuid);
		}
		try {
			a.computeFileLocation(session);
		} catch (Exception e) {
			ObservationQueryPlugIn.log(e.getMessage(), e);
		}
		item.setAttachment(a);
		return item;
	}
	protected SurveyWaypointResultItem asQueryResultItemInternal(ResultSet rs,boolean isAttachment, Session session) throws SQLException{
		
		SurveyWaypointResultItem it = null;
		if (isAttachment) {
			it = new SurveyWaypointAttachmentResultItem();
		}else {
			it = new SurveyWaypointResultItem();
		}

		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setConservationAreaUuid(UuidUtils.byteToUUID(rs.getBytes("ca_uuid"))); //$NON-NLS-1$
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$	
		
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
		
		it.setMissionUuid(UuidUtils.byteToUUID(rs.getBytes("mission_uuid"))); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionStart(rs.getTimestamp("mission_startdate").toLocalDateTime()); //$NON-NLS-1$
		it.setMissionEnd(rs.getTimestamp("mission_enddate").toLocalDateTime()); //$NON-NLS-1$
		it.setMissionLeader(rs.getString("mission_leader")); //$NON-NLS-1$
		
		it.setSamplingUnitUuid(UuidUtils.byteToUUID(rs.getBytes("samplingunit_uuid"))); //$NON-NLS-1$
		it.setSamplingUnitId(rs.getString("samplingunit_id")); //$NON-NLS-1$
		
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
		
		return it;
	}
	
	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
	}

	@Override
	public String getQueryDataTable() {
		return queryDataTable;
	}
	
	public String getLabelDataTable() {
		return queryDataTable + "_labels"; //$NON-NLS-1$
	}
}
