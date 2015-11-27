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
package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.engine.visitors.SurveyHasObservationFilterVisitor;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
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
public class PsqlErMissionEngine extends AbstractQueryEngine {

	private final Logger logger = Logger.getLogger(PsqlErMissionEngine.class.getName());
	
	private String queryDataTable;
	private MissionQuery query;
	private Session session;
	private Locale l = Locale.getDefault();
	
	public String getQueryDataTable(){
		return queryDataTable;
	}
	
	@Override
	public boolean canExecute(String  querytype) {
		return MissionQuery.KEY.equals(querytype);
	}
	
	public Locale getLocale(){
		return this.l;
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

		query = (MissionQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());
		this.l = (Locale) parameters.get(Locale.class.getName());
		
		if (query.getDateFilter() == null){
			return null;
		}
		queryDataTable = createTempTableName();
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SurveyDesignFilter filter = null;
				if (query.getSurveyDesign() != null){
					filter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
				}
				
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				IFilterProcessor filterer = null;
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				
				try {
					filterer = getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, filter);
					
					SurveyHasObservationFilterVisitor vv = new SurveyHasObservationFilterVisitor();
					boolean needsObservations = false;
					if (query.getFilter() != null && query.getFilter().getFilter() != null){
						query.getFilter().getFilter().accept(vv);
						needsObservations = vv.hasObservationFilter();
					}
					ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, 
							needsObservations, false);
					
					populateTemporaryTableExtra(c, session, query);
					
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				} finally {
					if (filterer != null) filterer.dropTemporaryTables(c);
					dropTemporaryTables(c, false);
				}
				c.commit();
			}

		});
		ErMissionQueryResult result = new ErMissionQueryResult(this);
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
		//original table
		dropTable(c, queryDataTable);
	}

	private void populateTemporaryTableNameObjExtra(String uuidColumn, String nameColumn, Connection c, Session session) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT DISTINCT ca_uuid,"); //$NON-NLS-1$
		sb.append(uuidColumn);
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append(queryDataTable);
		logger.finest(sb.toString());
		
		try (ResultSet rs = c.createStatement().executeQuery(sb.toString())){
			PreparedStatement statement = c.prepareStatement("UPDATE "+ queryDataTable +" SET "+nameColumn+" = ? where "+uuidColumn+" = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			int count = 0;
			while (rs.next()) {
				UUID ca_uuid = (UUID)rs.getObject(1);
				UUID uuid = (UUID)rs.getObject(2);
				if (uuid == null || ca_uuid == null)
					continue;
				String name = getName(uuid, ca_uuid, session);
				statement.setString(1, name);
				statement.setObject(2, uuid);
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
			MissionQuery query) throws SQLException {

		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"surveydesign_name","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"mission_leader", "varchar(256)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);
		}

		//survey design name
		populateTemporaryTableNameObjExtra("surveydesign_uuid", "surveydesign_name", c, session);  //$NON-NLS-1$//$NON-NLS-2$

		//ca information
		if (query.getConservationArea().equals(ConservationArea.MULTIPLE_CA)){
			//ca id and names are only used for cross-ca analysis
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			logger.finest(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			logger.finest(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}

		//mission leader
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
		String q1 = updateSql + "mission_leader = ? where mission_uuid = ?"; //$NON-NLS-1$
		logger.finest(q1);
		PreparedStatement leaderSt = c.prepareStatement(q1);
		
		int cnt = 0;
		logger.finest(sql.toString());
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				String name = getEmployeeName(uuid, session);
				if (name != null) {
					leaderSt.setString(1, name);
					leaderSt.setObject(2, rs.getObject(2));
					leaderSt.addBatch();
					
					cnt++;
					if (cnt >= 100){
						leaderSt.executeBatch();
						cnt = 0;
					}
				}
			}
			leaderSt.executeBatch();
		}
		populateAdditionalMissionTable(c,session);
	}

	private void populateAdditionalMissionTable(Connection c, Session session) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(queryDataTable + "_mlist"); //$NON-NLS-1$
		sql.append(" (uuid char(16) for bit data, value varchar(1024))"); //$NON-NLS-1$ 
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".list_element_uuid"); //$NON-NLS-1$
		sql.append(", r.ca_uuid FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionPropertyValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" r on r.mission_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".mission_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".list_element_uuid"); //$NON-NLS-1$
		sql.append(" is not null "); //$NON-NLS-1$
			
		StringBuilder sql2 = new StringBuilder();
		sql2.append("INSERT INTO "); //$NON-NLS-1$
		sql2.append( queryDataTable + "_mlist"); //$NON-NLS-1$
		sql2.append(" VALUES (?, ?)"); //$NON-NLS-1$ 
		logger.finest(sql2.toString());
		PreparedStatement statement = c.prepareStatement(sql2.toString());
		int count = 0;
		logger.finest(sql.toString());
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				
				if (uuid != null) {
					String value = Label.getDescription(uuid, session);
//					UUID caUuid = (UUID)rs.getObject(2);
//					String value = SmartLabelProvider.getDescription(UuidUtils.byteToUUID(uuid), UuidUtils.byteToUUID(cauuid));
					statement.setObject(1, uuid);
					statement.setString(2, value);
					statement.addBatch();
					count++;
					if (count >= 100){
						statement.executeBatch();
						count = 0;
					}
				}
			}
			statement.executeBatch();
		}
	}
	
	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT DISTINCT "); //$NON-NLS-1$
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
		sql.append(tablePrefix(Mission.class) + ".end_datetime "); //$NON-NLS-1$
		
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		
		sql.append("surveydesign_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("surveydesign_startdate date,"); //$NON-NLS-1$
		sql.append("surveydesign_enddate date,"); //$NON-NLS-1$
		
		sql.append("survey_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("survey_id varchar(128),"); //$NON-NLS-1$
		sql.append("survey_startdate date,"); //$NON-NLS-1$
		sql.append("survey_enddate date,"); //$NON-NLS-1$
		
		sql.append("mission_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("mission_id varchar(128),"); //$NON-NLS-1$
		sql.append("mission_startdate timestamp,"); //$NON-NLS-1$
		sql.append("mission_enddate timestamp"); //$NON-NLS-1$
	
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public void cleanUp(Session session) {
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				dropTemporaryTables(c, true);
			}});
	}

	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return "wp_uuid"; //$NON-NLS-1$
	}

	protected IFilterProcessor getFilterProcessor(FilterType filterType,
			String queryDataTable,
			SurveyDesignFilter sdFilter) {

		if (filterType == IFilter.FilterType.OBSERVATION){
			return new ErFilterProcessor(queryDataTable, this, sdFilter);
		}else{
			return new ErWaypointFilterProcessor(queryDataTable, this, sdFilter);
		}
	}
}
