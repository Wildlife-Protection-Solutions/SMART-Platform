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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Label;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.observation.model.Waypoint;
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
public class PsqlErWaypointEngine extends PsqlErEngine {

	private String queryDataTable;
	private SurveyWaypointQuery query;
	
	@Override
	public boolean canExecute(String querytype) {
		return SurveyWaypointQuery.KEY.equals(querytype);
	}
	
	public String getQueryDataTable(){
		return this.queryDataTable;
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

		query = (SurveyWaypointQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());
		locale = (Locale) parameters.get(Locale.class.getName());

		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
				SurveyDesignFilter filter = null;
				if (query.getSurveyDesign() != null){
					filter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
				}
				IFilterProcessor filterer = null;
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				try {			
					filterer = PsqlErWaypointEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, filter);
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, 
							true, true);
					
					populateTemporaryTableExtra(c, session, caFilter, filter);

					//setting result size
					
					
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				} finally {
					filterer.dropTemporaryTables(c);
					dropTemporaryTables(c, false);
				}
				c.commit();
			}

		});
		ErWaypointQueryResult results = new ErWaypointQueryResult(this);
		return results;
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
		dropTable(c, queryDataTable + "_mlist"); //$NON-NLS-1$
		dropTable(c, queryDataTable + "_sulist"); //$NON-NLS-1$
	}

	
	private void populateTemporaryTableExtra(Connection c, Session session,
			ConservationAreaFilter caFilter,
			SurveyDesignFilter sdFilter) throws SQLException {
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

		//survey design
		populateTemporaryTableNameObjExtra("surveydesign_uuid", "surveydesign_name", queryDataTable, c, session);  //$NON-NLS-1$//$NON-NLS-2$

		//ca information
		populateCaDetails(c, queryDataTable, query);

		
		// mission leader
		populateMissionLeader(c, session, queryDataTable);

		//mission attributes
		WpoaLinkedData mListData = new WpoaLinkedData("_mlist", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return Label.getDescription(uuid, session);
			}
		};
		populateAdditionalMissionTable(c, session, sdFilter, caFilter, queryDataTable, mListData);
		
		//sampling unit attributes
		WpoaLinkedData suListData = new WpoaLinkedData("_sulist", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return Label.getDescription(uuid, session);
			}
		};
		populateAdditionalSuTable(c, session, sdFilter, caFilter, queryDataTable, suListData);

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
		sql.append(tablePrefix(Mission.class) + ".end_datetime, "); //$NON-NLS-1$
		
		sql.append(tablePrefix(SamplingUnit.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnit.class) + ".id, "); //$NON-NLS-1$

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
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("ca_uuid UUID,"); //$NON-NLS-1$
		
		sql.append("surveydesign_uuid UUID,"); //$NON-NLS-1$
		sql.append("surveydesign_startdate date,"); //$NON-NLS-1$
		sql.append("surveydesign_enddate date,"); //$NON-NLS-1$
		
		sql.append("survey_uuid UUID,"); //$NON-NLS-1$
		sql.append("survey_id varchar(128),"); //$NON-NLS-1$
		sql.append("survey_startdate date,"); //$NON-NLS-1$
		sql.append("survey_enddate date,"); //$NON-NLS-1$
		
		sql.append("mission_uuid UUID,"); //$NON-NLS-1$
		sql.append("mission_id varchar(128),"); //$NON-NLS-1$
		sql.append("mission_startdate timestamp,"); //$NON-NLS-1$
		sql.append("mission_enddate timestamp,"); //$NON-NLS-1$
		
		sql.append("samplingunit_uuid UUID,"); //$NON-NLS-1$
		sql.append("samplingunit_id varchar(128),"); //$NON-NLS-1$

		sql.append("wp_uuid UUID,"); //$NON-NLS-1$
		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double precision,"); //$NON-NLS-1$
		sql.append("wp_y double precision,"); //$NON-NLS-1$
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
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$
		it.setSurveyDesignEnd(rs.getDate("surveydesign_enddate")); //$NON-NLS-1$
		it.setSurveyDesignStart(rs.getDate("surveydesign_startdate")); //$NON-NLS-1$
		
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
		it.setSurveyStart(rs.getDate("survey_startdate")); //$NON-NLS-1$
		it.setSurveyEnd(rs.getDate("survey_enddate")); //$NON-NLS-1$
		
		it.setMissionUuid((UUID)rs.getObject("mission_uuid")); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionEnd(rs.getDate("mission_startdate")); //$NON-NLS-1$
		it.setMissionStart(rs.getDate("mission_enddate")); //$NON-NLS-1$
		it.setMissionLeader(rs.getString("mission_leader")); //$NON-NLS-1$
		
		it.setSamplingUnitUuid((UUID)rs.getObject("samplingunit_uuid")); //$NON-NLS-1$
		it.setSamplingUnitId(rs.getString("samplingunit_id")); //$NON-NLS-1$
		
		it.setWaypointUuid((UUID)rs.getObject("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointDateTime(rs.getTimestamp("wp_date")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		
		return it;
	}
	
	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException{
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
