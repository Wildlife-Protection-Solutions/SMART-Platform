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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
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
public class PsqlErMissionEngine extends PsqlErEngine {

	private final Logger logger = Logger.getLogger(PsqlErMissionEngine.class.getName());
	
	private String queryDataTable;
	private MissionQuery query;
	
	public String getQueryDataTable(){
		return queryDataTable;
	}
	
	@Override
	public boolean canExecute(String  querytype) {
		return MissionQuery.KEY.equals(querytype);
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
		locale = (Locale) parameters.get(Locale.class.getName());
		
		if (query.getDateFilter() == null){
			return null;
		}
		queryDataTable = createTempTableName();
		return session.doReturningWork(new ReturningWork<ErMissionQueryResult>() {
			@Override
			public ErMissionQueryResult execute(Connection c) throws SQLException {
				ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
				if (caFilter.getConservationAreaFilterIds().size() > 1){
					throw new SQLException(MessageFormat.format(Messages.getString("PsqlErMissionEngine.QueryTypeNotSupported", getLocale()), query.getTypeKey())); //$NON-NLS-1$
				}
				
				SurveyDesignFilter sdFilter = null;
				if (query.getSurveyDesign() != null){
					sdFilter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
				}

				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				IFilterProcessor filterer = null;
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				
				try {
					filterer = getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, sdFilter);
					
					SurveyHasObservationFilterVisitor vv = new SurveyHasObservationFilterVisitor();
					boolean needsObservations = false;
					if (query.getFilter() != null && query.getFilter().getFilter() != null){
						query.getFilter().getFilter().accept(vv);
						needsObservations = vv.hasObservationFilter();
					}
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, 
							needsObservations, false);
					
					populateTemporaryTableExtra(c, session, query, sdFilter, caFilter);
				
					c.commit();
					int itemcnt;
					try(ResultSet rs = c.createStatement().executeQuery("select count(*) FROM " + getQueryDataTable())){ //$NON-NLS-1$ 
						rs.next();
						itemcnt = rs.getInt(1);
					}
					return new ErMissionQueryResult(PsqlErMissionEngine.this, itemcnt);
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				} finally {
					if (filterer != null) filterer.dropTemporaryTables(c);
				}
				
			}

		});
	}

	private void populateTemporaryTableExtra(Connection c, Session session, 
			MissionQuery query,
			SurveyDesignFilter sdFilter,
			ConservationAreaFilter caFilter) throws SQLException {

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
		updateLabel(c, queryDataTable, "surveydesign_uuid", "surveydesign_name"); //$NON-NLS-1$ //$NON-NLS-2$
		
		//ca information
		populateCaDetails(c, queryDataTable, "ca_uuid", query); //$NON-NLS-1$
		
		//mission leader
		populateMissionLeader(c, session, queryDataTable);
		
		//mission attributes
		populateAdditionalMissionTable(c,session, sdFilter, caFilter, queryDataTable, queryDataTable+ "_mlist", "list_element_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
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
		sql.append("mission_enddate timestamp"); //$NON-NLS-1$
	
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException{
		
	}
	
	@Override
	public void cleanUp(Session session) throws SQLException{
		dropTable(session, queryDataTable);
		dropTable(session, queryDataTable + "_mlist"); //$NON-NLS-1$
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
	
	@Override
	public String getDateFilterTable() throws SQLException{
		return tablePrefix(MissionDay.class);
	}
	
	@Override
	public String getDateFilterField() throws SQLException{
		return "mission_day"; //$NON-NLS-1$
	}
}
