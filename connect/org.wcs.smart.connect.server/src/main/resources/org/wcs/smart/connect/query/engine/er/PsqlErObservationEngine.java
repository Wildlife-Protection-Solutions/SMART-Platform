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
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.Label;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
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
public class PsqlErObservationEngine extends PsqlErEngine {

	private final Logger logger = Logger.getLogger(PsqlErObservationEngine.class.getName());
	
	private String queryDataTable;
	private SurveyObservationQuery query;
	
	public String getQueryDataTable(){
		return this.queryDataTable;
	}

	@Override
	public boolean canExecute(String querytype) {
		return SurveyObservationQuery.KEY.equals(querytype);
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

		query = (SurveyObservationQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());
		locale = (Locale) parameters.get(Locale.class.getName());

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
				IFilterProcessor filterer = null;
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				
				try {
					filterer = getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, filter);
					ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, 
							true, true);
					
					populateTemporaryTableExtra(c, session,filter, caFilter,  query);
				}catch(Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				} finally {
					if (filterer != null) filterer.dropTemporaryTables(c);
					dropTemporaryTables(c, false);
				}
				c.commit();
			}

		});
		ErObservationQueryResult result = new ErObservationQueryResult(this);
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
		dropTable(c, queryDataTable + "_LIST"); //$NON-NLS-1$
		dropTable(c, queryDataTable + "_TREE"); //$NON-NLS-1$
		dropTable(c, queryDataTable + "_SULIST"); //$NON-NLS-1$
		dropTable(c, queryDataTable + "_MLIST"); //$NON-NLS-1$
	}



	
	private void populateTemporaryTableCategory(Connection c, Session session) throws SQLException {
		// add data model category columns
		int categoryCount = QueryManager.INSTANCE.getCategoryDepth(session, query.getConservationArea().getUuid());
		if (categoryCount < 0){
			return;			//nothing to update
		}
		
		for (int i = 0; i <= categoryCount; i++) {
			String sql = "ALTER TABLE "+queryDataTable+" ADD category_"+i+" varchar(1024)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);
		}
		
		Map<Integer, PreparedStatement> num2Statement = new HashMap<Integer, PreparedStatement>();
		String sql = "SELECT DISTINCT OB_CATEGORY_UUID FROM "+queryDataTable;  //$NON-NLS-1$
		logger.finest(sql);
		try(ResultSet rs = c.createStatement().executeQuery(sql)) {
			while (rs.next()) {
				UUID uuid = (UUID) rs.getObject(1);
				if (uuid == null)
					continue;
				String[] names = getCategoryLabels(uuid,locale, session);
				int count = names.length;
				int depth = Math.min(categoryCount + 1, count);	//the full category name may be longer than the number of columns in cross-ca analysis 
				PreparedStatement statement = num2Statement.get(count); //try to reuse already created prepare statement
				if (statement == null) {
					//that means that we didn't create update statement for this number of columns to update -> create one
					StringBuilder colunms = new StringBuilder();
					for (int j = 0; j < depth; j++) {
						if (j > 0){
							colunms.append(", "); //$NON-NLS-1$
						}
						colunms.append("category_").append(j).append("=?"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sql = "UPDATE "+queryDataTable+" SET "+colunms.toString()+" where OB_CATEGORY_UUID = ?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					logger.finest(sql);
					statement = c.prepareStatement(sql);
					
					num2Statement.put(count, statement);
				}
				
				for (int i = 0; i <  depth; i++) {
					statement.setString(i+1, names[i]);
				}
				statement.setObject( depth+1, uuid);
				statement.executeUpdate();
			}
		}
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session,
			SurveyDesignFilter sdFilter,
			ConservationAreaFilter caFilter,
			SurveyObservationQuery query) throws SQLException {

		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"surveydesign_name","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ob_observer", "varchar(512)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"mission_leader", "varchar(256)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);
		}
		populateTemporaryTableNameObjExtra("surveydesign_uuid", "surveydesign_name", queryDataTable, c, session);  //$NON-NLS-1$//$NON-NLS-2$

		//ca information
		populateCaDetails(c, queryDataTable, query);

		//add observers
		StringBuilder sqla = new StringBuilder();
		sqla.append("SELECT DISTINCT ob_observer_uuid FROM "); //$NON-NLS-1$
		sqla.append(queryDataTable);

		String updateSql = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		String q1 = updateSql + "ob_observer = ? where ob_observer_uuid = ?"; //$NON-NLS-1$
		logger.finest(q1);
		PreparedStatement observerSt = c.prepareStatement(q1);
		int cnt = 0;
		logger.finest(sqla.toString());
		try (ResultSet rs = c.createStatement().executeQuery(sqla.toString())){
			while (rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				String name = getEmployeeName(uuid, session);
				if (name != null) {
					observerSt.setString(1, name);
					observerSt.setObject(2, uuid);
					observerSt.addBatch();
					cnt++;
					if (cnt >= 100) {
						observerSt.executeBatch();
						cnt = 0;
					}
				}
			}
			observerSt.executeBatch();
		}
		
		//mission leader
		populateMissionLeader(c, session, queryDataTable);
		
		//populating categories
		populateTemporaryTableCategory(c, session);

		//waypoint observation list attributes
		WpoaLinkedData listData = new WpoaLinkedData("_list", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return Label.getDescription(uuid, session);
			}
		};
		populateAdditionalWpoaTable(c, session, queryDataTable, listData);

		//waypoint observation tree attributes
		WpoaLinkedData treeData = new WpoaLinkedData("_tree", "tree_node_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return Label.getDescription(uuid, session);
			}
		};
		populateAdditionalWpoaTable(c, session, queryDataTable, treeData);

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
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".category_uuid "); //$NON-NLS-1$

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
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$

		sql.append("ob_uuid UUID,"); //$NON-NLS-1$
		sql.append("ob_observer_uuid UUID,"); //$NON-NLS-1$
		sql.append("ob_category_uuid UUID"); //$NON-NLS-1$
		
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
		super.buildTemporaryTableIndexes(c, tableName);
		
		StringBuilder sql = new StringBuilder();
		sql.append("create index "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("_ob_category_uuid_idx on "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("(ob_category_uuid)"); //$NON-NLS-1$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
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
