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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;

/**
 * Query engine for executing lazy queries using derby.
 * This engines create temporary tables that one to one correspond with the table
 * that user see. {@link DerbyPagedObservationResult} obtains the name of this table and is
 * responsible for all other operations (fetching/sorting/deleting tables)
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyWaypointEngine extends DerbySurveyQueryEngine {

	private String queryDataTable;
	
	
	public IPagedQueryResultSet executeDerbyQuery(final SurveyWaypointQuery query, final Session session, final IProgressMonitor monitor) throws SQLException {

		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		final DerbyPagedWaypointResult result = new DerbyPagedWaypointResult(queryDataTable, this);
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask("Running Query.", 80);
				
				SurveyDesignFilter filter = null;
				if (query.getSurveyDesign() != null){
					filter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
				}
				IFilterProcessor filterer = DerbyWaypointEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, filter);
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				
				try {			
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							query.getConservationAreaFilterAsFilter(), 
							false, true, new SubProgressMonitor(monitor, 50));
					
					if (monitor.isCanceled()) return;
					populateTemporaryTableExtra(c, session,  new SubProgressMonitor(monitor, 20));
					
					if (monitor.isCanceled()) return;
					monitor.subTask("Fetching size data");
					//setting result size
					ResultSet rs = c.createStatement().executeQuery("select count(*) from " + queryDataTable); //$NON-NLS-1$
					try {
						if (rs.next()) { 
							result.setItemCount(rs.getInt(1));
						}
					} finally {
						rs.close();
					}
					monitor.worked(10);
					
				}catch (Exception ex){
					throw new SQLException(ex);
				} finally {
					filterer.dropTemporaryTables(c);
					dropTemporaryTables(c, monitor.isCanceled());
					monitor.done();
				}
				c.commit();
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
	private void dropTemporaryTables(Connection c, boolean fullDrop) throws SQLException {
		if (!fullDrop)
			return;

		dropTable(c, queryDataTable);
	}

	private void populateTemporaryTableNameObjExtra(String uuidColumn, String nameColumn, Connection c, Session session) throws SQLException {
		String sql = "SELECT DISTINCT ca_uuid, " + uuidColumn + " FROM " + queryDataTable;  //$NON-NLS-1$//$NON-NLS-2$
		QueryPlugIn.logSql(sql);
		ResultSet rs = c.createStatement().executeQuery(sql);
		try {
			PreparedStatement statement = c.prepareStatement("UPDATE " + queryDataTable + " SET " + nameColumn + " = ? where " + uuidColumn + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			int count = 0;
			while (rs.next()) {
				byte[] ca_uuid = rs.getBytes(1);
				byte[] uuid = rs.getBytes(2);
				if (uuid == null || ca_uuid == null)
					continue;
				String name = getName(uuid, ca_uuid, session);
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
			
		} finally {
			rs.close();
		}
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session, IProgressMonitor monitor) throws SQLException {
		monitor.beginTask("Populating additional data", 3);
		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"survey_design_name","varchar(1024)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}

		monitor.subTask(Messages.DerbyObservationEngine_progress3);
		populateTemporaryTableNameObjExtra("survey_design_uuid", "survey_design_name", c, session);  //$NON-NLS-1$//$NON-NLS-2$
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}
		
		//ca information
		if (SmartDB.isMultipleAnalysis()){
			//ca id and names are only used for cross-ca analysis
			monitor.subTask(Messages.DerbyObservationEngine_progress4);
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(DerbySurveyQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(DerbySurveyQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}
	}

	@Override
	protected String getTemporaryTableSelectClause(boolean includeObservations) {
		
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyDesign.class) + ".end_date, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(Survey.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".start_date, "); //$NON-NLS-1$
		sql.append(tablePrefix(Survey.class) + ".end_date, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(Mission.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".start_datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class) + ".end_datetime, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(SamplingUnit.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnit.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnit.class) + ".buffer, "); //$NON-NLS-1$

		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment "); //$NON-NLS-1$
		
		return sql.toString();
	}

	@Override
	protected String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		
		sql.append("survey_design_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("survey_design_start date,"); //$NON-NLS-1$
		sql.append("survey_design_end date,"); //$NON-NLS-1$
		
		sql.append("survey_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("survey_id varchar(128),"); //$NON-NLS-1$
		sql.append("survey_start date,"); //$NON-NLS-1$
		sql.append("survey_end date,"); //$NON-NLS-1$
		
		sql.append("mission_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("mission_id varchar(128),"); //$NON-NLS-1$
		sql.append("mission_start timestamp,"); //$NON-NLS-1$
		sql.append("mission_end timestamp,"); //$NON-NLS-1$
		
		sql.append("sampling_unit_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("sampling_unit_id varchar(128),"); //$NON-NLS-1$
		sql.append("sampling_unit_buffer double,"); //$NON-NLS-1$

		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_date timestamp,"); //$NON-NLS-1$ 
		sql.append("wp_comment varchar(4096)"); //$NON-NLS-1$
		
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	protected SurveyQueryResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		SurveyQueryResultItem it = new SurveyQueryResultItem();

		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		
		it.setSurveyDesign(rs.getString("survey_design_name")); //$NON-NLS-1$
		it.setSurveyDesignEnd(rs.getDate("survey_design_end")); //$NON-NLS-1$
		it.setSurveyDesignStart(rs.getDate("survey_design_start")); //$NON-NLS-1$
		
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
		it.setSurveyStart(rs.getDate("survey_start")); //$NON-NLS-1$
		it.setSurveyEnd(rs.getDate("survey_end")); //$NON-NLS-1$
		
		it.setMissionUuid(rs.getBytes("mission_uuid")); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionEnd(rs.getDate("mission_start")); //$NON-NLS-1$
		it.setMissionStart(rs.getDate("mission_end")); //$NON-NLS-1$
		
		it.setSamplingUnitUuid(rs.getBytes("sampling_unit_uuid")); //$NON-NLS-1$
		it.setSamplingUnitId(rs.getString("sampling_unit_id")); //$NON-NLS-1$
		it.setSamplingUnitBuffer(rs.getDouble("sampling_unit_buffer")); //$NON-NLS-1$
		
		it.setWaypointUuid(rs.getBytes("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointTime(rs.getTime("wp_date")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		
		return it;
	}
	
	
	@Override
	protected void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
	}
}
