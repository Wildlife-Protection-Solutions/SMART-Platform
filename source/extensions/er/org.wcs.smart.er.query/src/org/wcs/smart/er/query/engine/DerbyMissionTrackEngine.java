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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
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
public class DerbyMissionTrackEngine extends DerbySurveyQueryEngine {

	private String queryDataTable;
	
	@Override
	public boolean canExecute(String querytype) {
		return MissionTrackQuery.KEY.equals(querytype);
	}
	
	public String getQueryDataTable() {
		return this.queryDataTable;
	}
	
	public String getQueryLabelTable() {
		return this.queryDataTable + "_labels";
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

		final MissionTrackQuery query = (MissionTrackQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		
		if (query.getDateFilter() == null){
			return null;
		}
		queryDataTable = createTempTableName();
		final DerbyPagedMissionResult result = new DerbyPagedMissionResult(this);	
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyObservationEngine_progress1, 80);
				SurveyDesignFilter filter = null;
				if (query.getSurveyDesign() != null){
					filter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
				}
				IFilterProcessor filterer = new FilterProcessorMission(queryDataTable, DerbyMissionTrackEngine.this, filter, query);
				
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));
				//turn on auto-commit because we want ddl to commit immediately so we don't lock up the database
				c.setAutoCommit(true);
				try {
					ConservationAreaFilter caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, false, false, progress.split(50));
					
					populateTemporaryTableExtra(c, session, query, progress.split(20));
					
					progress.checkCanceled();
					progress.subTask(Messages.DerbyObservationEngine_progress2);
					//setting result size
					try (ResultSet rs = c.createStatement().executeQuery("select count(*) from " + queryDataTable)){ //$NON-NLS-1$
						if (rs.next()) { 
							result.setItemCount(rs.getInt(1));
						}
					} 
					
					try(ResultSet rs = c.createStatement().executeQuery("select count(distinct(mission_uuid)) from " + queryDataTable)) { //$NON-NLS-1$
						if (rs.next()) { 
							result.setMissionCnt(rs.getInt(1));
						}
					}
					progress.worked(10);
				}catch(OperationCanceledException ex) {
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

	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	@Override
	public void dropTables(Connection c) throws SQLException {
		//original table
		dropTable(c, getQueryLabelTable());
		dropTable(c, getQueryDataTable());
	}

	private void populateTemporaryTableNameObjExtra(String uuidColumn, String nameColumn, Connection c, Session session) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT DISTINCT ca_uuid,"); //$NON-NLS-1$
		sb.append(uuidColumn);
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append(queryDataTable);
		QueryPlugIn.logSql(sb.toString());
		
		try(ResultSet rs = c.createStatement().executeQuery(sb.toString())) {
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

	private void populateTemporaryTableExtra(Connection c, Session session, 
			MissionTrackQuery query, IProgressMonitor monitor) throws SQLException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyMissionEngine_ProgressAdditionalData, 4);
		
		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"surveydesign_name","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		progress.split(1);
		

		//survey design name
		monitor.subTask(Messages.DerbyObservationEngine_progress3);
		populateTemporaryTableNameObjExtra("surveydesign_uuid", "surveydesign_name", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		progress.split(1);
		
		//ca information
		if (SmartDB.isMultipleAnalysis()){
			//ca id and names are only used for cross-ca analysis
			monitor.subTask(Messages.DerbyObservationEngine_progress4);
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
			
		monitor.subTask(Messages.DerbyMissionEngine_ProgressMissionProperties);
		SurveyPagedResultUtils.populateAdditionalAttributeTable(true, true, true, getQueryDataTable(), getQueryLabelTable(), this, c, session);
		progress.split(1);

	}

	
	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".uuid, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(Survey.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".id, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(Mission.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".end_date, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(MissionDay.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionDay.class) + ".mission_day, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(MissionTrack.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionTrack.class) + ".track_type, "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionTrack.class) + ".id, "); //$NON-NLS-1$
		sql.append("smart.distanceInMeter(" + tablePrefix(MissionTrack.class) + ".geometry) / 1000.0, "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(tablePrefix(MissionTrack.class) + ".geometry, "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnit.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnit.class) + ".id "); //$NON-NLS-1$
		
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
	
		sql.append("missionday_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("missionday_date date,"); //$NON-NLS-1$
		
		sql.append("mission_trackuuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("mission_tracktype varchar(32),"); //$NON-NLS-1$
		sql.append("mission_trackid varchar(128),"); //$NON-NLS-1$
		sql.append("mission_tracklength double,"); //$NON-NLS-1$
		sql.append("mission_track blob,"); //$NON-NLS-1$
		
		sql.append("samplingunit_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("samplingunit_id varchar(128)"); //$NON-NLS-1$
		
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}
	
	
	protected MissionTrackResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		MissionTrackResultItem it = new MissionTrackResultItem();
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setConservationAreaUuid(UuidUtils.byteToUUID(rs.getBytes("ca_uuid"))); //$NON-NLS-1$
		
		it.setMissionUuid(UuidUtils.byteToUUID(rs.getBytes("mission_uuid"))); //$NON-NLS-1$
		it.setMissionEnd(rs.getTimestamp("mission_enddate").toLocalDateTime()); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionStart(rs.getTimestamp("mission_startdate").toLocalDateTime()); //$NON-NLS-1$
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
		
		it.setTrackUuid(UuidUtils.byteToUUID(rs.getBytes("mission_trackuuid"))); //$NON-NLS-1$
		it.setTrackType(TrackType.valueOf(rs.getString("mission_tracktype"))); //$NON-NLS-1$
		it.setTrackDate(rs.getDate("missionday_date").toLocalDate()); //$NON-NLS-1$
		it.setTrackId(rs.getString("mission_trackid")); //$NON-NLS-1$
		it.setTrackLength(rs.getDouble("mission_tracklength")); //$NON-NLS-1$
		
		it.setSamplingUnitId(rs.getString("samplingunit_id")); //$NON-NLS-1$
		it.setSamplingUnitUuid(UuidUtils.byteToUUID(rs.getBytes("samplingunit_uuid"))); //$NON-NLS-1$
		
		it.setGeometry(rs.getBytes("mission_track")); //$NON-NLS-1$
		
		return it;
	}
	

	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
	}
	
}
