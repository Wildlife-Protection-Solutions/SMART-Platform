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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
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
public class DerbyObservationEngine extends DerbySurveyQueryEngine {

	private String queryDataTable;
	private int categoryCount;
	
	public DerbyPagedObservationResult executeDerbyQuery(final SurveyObservationQuery query, final Session session, final IProgressMonitor monitor) throws SQLException {
		
		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		final DerbyPagedObservationResult result = new DerbyPagedObservationResult(queryDataTable, this);
		

		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyObservationEngine_progress1, 80);
				SurveyDesignFilter filter = null;
				if (query.getSurveyDesign() != null){
					filter = SurveyDesignFilter.createStringFilter(query.getSurveyDesign());
				}
				IFilterProcessor filterer = DerbyObservationEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, filter);
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				
				SamplingUnitFilterProcessor.updateSamplingUnitFilter(query.getFilter().getFilter(), SamplingUnitFilter.Source.OBSERVATION);
				try {
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							query.getConservationAreaFilterAsFilter(), 
							true, true, new SubProgressMonitor(monitor, 50));
					
					if (monitor.isCanceled()) return;
					populateTemporaryTableExtra(c, session, query, new SubProgressMonitor(monitor, 20));
					
					if (monitor.isCanceled()) return;
					monitor.subTask(Messages.DerbyObservationEngine_progress2);
					//setting result size
					ResultSet rs = c.createStatement().executeQuery("select count(*) from " + queryDataTable); //$NON-NLS-1$
					try {
						if (rs.next()) { 
							result.setItemCount(rs.getInt(1));
						}
					} finally {
						rs.close();
					}

					//setting waypoint count
					rs = c.createStatement().executeQuery("select count(*) from (SELECT DISTINCT WP_UUID from " + queryDataTable + ") wp"); //$NON-NLS-1$ //$NON-NLS-2$
					try {
						if (rs.next()) { 
							result.setWpCount(rs.getInt(1));
						}
					} finally {
						rs.close();
					}
					monitor.worked(10);
					
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
		//original table
		dropTable(c, queryDataTable);
		dropTable(c, queryDataTable + "_LIST"); //$NON-NLS-1$
		dropTable(c, queryDataTable + "_TREE"); //$NON-NLS-1$
	}

	private void populateTemporaryTableNameObjExtra(String uuidColumn, String nameColumn, Connection c, Session session) throws SQLException {
		String sql = "SELECT DISTINCT ca_uuid, "+uuidColumn+" FROM "+queryDataTable;  //$NON-NLS-1$//$NON-NLS-2$
		QueryPlugIn.logSql(sql);
		ResultSet rs = c.createStatement().executeQuery(sql);
		try {
			PreparedStatement statement = c.prepareStatement("UPDATE "+ queryDataTable +" SET "+nameColumn+" = ? where "+uuidColumn+" = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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

	
	private void populateTemporaryTableCategory(Connection c, Session session) throws SQLException {
		
		// add data model category columns
		categoryCount = QueryDataModelManager.getInstance().getActiveDepth();
		if (categoryCount < 0){
			return;			//nothing to update
		}
		
		for (int i = 0; i <= categoryCount; i++) {
			String sql = "ALTER TABLE "+queryDataTable+" ADD category_"+i+" varchar(1024)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		Map<Integer, PreparedStatement> num2Statement = new HashMap<Integer, PreparedStatement>();
		String sql = "SELECT DISTINCT OB_CATEGORY_UUID FROM "+queryDataTable;  //$NON-NLS-1$
		QueryPlugIn.logSql(sql);
		ResultSet rs = c.createStatement().executeQuery(sql);
		
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid == null)
					continue;
				String[] names = getCategoryLabels(uuid, session);
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
					QueryPlugIn.logSql(sql);
					statement = c.prepareStatement(sql);
					
					num2Statement.put(count, statement);
				}
				
				for (int i = 0; i <  depth; i++) {
					statement.setString(i+1, names[i]);
				}
				statement.setBytes( depth+1, uuid);
				statement.executeUpdate();
			}
		} finally {
			rs.close();
		}
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session, 
			SurveyObservationQuery query, IProgressMonitor monitor) throws SQLException {

		monitor.beginTask(Messages.DerbyObservationEngine_ProgressAdditionalData, 9);
		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"surveydesign_name","varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ob_observer", "varchar(512)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"mission_leader", "varchar(256)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		if (monitor.isCanceled()){
			return;
		}

		monitor.subTask(Messages.DerbyObservationEngine_progress3);
		populateTemporaryTableNameObjExtra("surveydesign_uuid", "surveydesign_name", c, session);  //$NON-NLS-1$//$NON-NLS-2$
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
		
		//add observers
		monitor.subTask(Messages.DerbyObservationEngine_populatingObserverProgress);
		StringBuilder sqla = new StringBuilder();
		sqla.append("SELECT DISTINCT ob_observer_uuid FROM "); //$NON-NLS-1$
		sqla.append(queryDataTable);
		QueryPlugIn.logSql(sqla.toString());

		ResultSet rs = c.createStatement().executeQuery(sqla.toString());
		String updateSql = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		String q1 = updateSql + "ob_observer = ? where ob_observer_uuid = ?"; //$NON-NLS-1$
		QueryPlugIn.logSql(q1);
		PreparedStatement observerSt = c.prepareStatement(q1);
		int cnt = 0;
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				String name = getEmployeeName(uuid, session);

				if (name != null) {
					observerSt.setString(1, name);
					observerSt.setBytes(2, uuid);
					observerSt.addBatch();
					cnt++;
					if (cnt >= 100) {
						observerSt.executeBatch();
						cnt = 0;
					}
				}
			}
			observerSt.executeBatch();
		} finally {
			rs.close();
		}
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}
		
		//mission leader
		monitor.subTask(Messages.DerbyObservationEngine_ProgressLeader);
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

		QueryPlugIn.logSql(sql.toString());

		rs = c.createStatement().executeQuery(sql.toString());
		updateSql = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		q1 = updateSql + "mission_leader = ? where mission_uuid = ?"; //$NON-NLS-1$
		QueryPlugIn.logSql(q1);
		PreparedStatement leaderSt = c.prepareStatement(q1);

		cnt = 0;
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				String name = getEmployeeName(uuid, session);

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
		} finally {
			rs.close();
		}
		monitor.worked(1);
		if (monitor.isCanceled()) {
			return;
		}
				
		//populating categories
		monitor.subTask(Messages.DerbyObservationEngine_progress5);
		populateTemporaryTableCategory(c, session);
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}

		monitor.subTask(Messages.DerbyObservationEngine_progress6);
		WpoaLinkedData listData = new WpoaLinkedData("_list", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, byte[] cauuid, byte[] uuid) {
				return QueryDataModelManager.getInstance().getAttributeListItemLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, listData);
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}
		
		monitor.subTask(Messages.DerbyObservationEngine_progress7);
		WpoaLinkedData treeData = new WpoaLinkedData("_tree", "tree_node_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, byte[] cauuid, byte[] uuid) {
				return QueryDataModelManager.getInstance().getAttributeTreeNodeLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, treeData);
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}
		
		//mission_list
		monitor.subTask(Messages.DerbyObservationEngine_progress6);
		WpoaLinkedData mListData = new WpoaLinkedData("_mlist", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, byte[] cauuid, byte[] uuid) {
				return Label.getDescription(uuid);
			}
		};
		populateAdditionalMissionTable(c, session, mListData);
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}
		
		//sampling unit list
		monitor.subTask(Messages.DerbyObservationEngine_SuAttributeProgress);
		WpoaLinkedData suListData = new WpoaLinkedData("_sulist", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, byte[] cauuid, byte[] uuid) {
				return Label.getDescription(uuid);
			}
		};
		populateAdditionalSuTable(c, session, suListData);
		monitor.worked(1);
		if (monitor.isCanceled()){
			return;
		}
		
		
		monitor.done();
	}

	private void populateAdditionalWpoaTable(Connection c, Session session, WpoaLinkedData linkedData) throws SQLException {
		String sql = "CREATE TABLE " + queryDataTable + linkedData.getPostfix() + " (uuid char(16) for bit data, value varchar(1024))"; //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql);

		sql = "SELECT DISTINCT wpoa."+linkedData.getUuidColumn()+", r.CA_UUID FROM smart.wp_observation_attributes wpoa inner join "+queryDataTable+" r on wpoa.OBSERVATION_UUID = r.OB_UUID"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql);
		
		sql = "INSERT INTO "+queryDataTable+linkedData.getPostfix()+" VALUES (?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		PreparedStatement statement = c.prepareStatement(sql);
		int count = 0;
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid != null) {
					byte[] cauuid = rs.getBytes(2);
					String value = linkedData.getLabel(session, cauuid, uuid);
					statement.setBytes(1, uuid);
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
		} finally {
			rs.close();
		}
	}

	private void populateAdditionalMissionTable(Connection c, Session session, WpoaLinkedData linkedData) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(queryDataTable + linkedData.getPostfix());
		sql.append(" (uuid char(16) for bit data, value varchar(1024))"); //$NON-NLS-1$ 
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append("." + linkedData.getUuidColumn()); //$NON-NLS-1$
		sql.append(", r.ca_uuid FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionPropertyValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" r on r.mission_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".mission_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append("." + linkedData.getUuidColumn()); //$NON-NLS-1$
		sql.append(" is not null "); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());
		
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append( queryDataTable + linkedData.getPostfix());
		sql.append(" VALUES (?, ?)"); //$NON-NLS-1$ 
		QueryPlugIn.logSql(sql.toString());
		PreparedStatement statement = c.prepareStatement(sql.toString());
		int count = 0;
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid != null) {
					byte[] cauuid = rs.getBytes(2);
					String value = linkedData.getLabel(session, cauuid, uuid);
					statement.setBytes(1, uuid);
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
		} finally {
			rs.close();
		}
	}

	private void populateAdditionalSuTable(Connection c, Session session, WpoaLinkedData linkedData) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(queryDataTable + linkedData.getPostfix());
		sql.append(" (uuid char(16) for bit data, value varchar(1024))"); //$NON-NLS-1$ 
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append("." + linkedData.getUuidColumn()); //$NON-NLS-1$
		sql.append(", r.ca_uuid FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(SamplingUnitAttributeValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" r on r.samplingunit_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append(".su_attribute_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append("." + linkedData.getUuidColumn()); //$NON-NLS-1$
		sql.append(" is not null "); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		ResultSet rs = c.createStatement().executeQuery(sql.toString());
		
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append( queryDataTable + linkedData.getPostfix());
		sql.append(" VALUES (?, ?)"); //$NON-NLS-1$ 
		QueryPlugIn.logSql(sql.toString());
		PreparedStatement statement = c.prepareStatement(sql.toString());
		int count = 0;
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid != null) {
					byte[] cauuid = rs.getBytes(2);
					String value = linkedData.getLabel(session, cauuid, uuid);
					statement.setBytes(1, uuid);
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
		} finally {
			rs.close();
		}
	}
	
	/**
	 * Wrapper class for populating linked data (additional columns)
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private abstract class WpoaLinkedData {
		private String postfix;
		private String uuidColumn;

		public WpoaLinkedData(String postfix, String uuidColumn) {
			super();
			this.postfix = postfix;
			this.uuidColumn = uuidColumn;
		}

		public String getPostfix() {
			return postfix;
		}

		public String getUuidColumn() {
			return uuidColumn;
		}
		
		public abstract String getLabel(Session session, byte[] cauuid, byte[] keyuuid);
	}

	@Override
	protected String getTemporaryTableSelectClause(boolean includeObservations) {
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
	protected String getTemporaryTableCreateClause(String tableName) {
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
		sql.append("mission_enddate timestamp,"); //$NON-NLS-1$
		
		sql.append("samplingunit_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("samplingunit_id varchar(128),"); //$NON-NLS-1$
		
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_date timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$

		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_observer_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_category_uuid char(16) for bit data"); //$NON-NLS-1$
		
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}
	
	@Override
	protected SurveyQueryResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		SurveyQueryResultItem it = new SurveyQueryResultItem();
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		
		it.setMissionUuid(rs.getBytes("mission_uuid")); //$NON-NLS-1$
		it.setMissionEnd(rs.getDate("mission_enddate")); //$NON-NLS-1$
		it.setMissionId(rs.getString("mission_id")); //$NON-NLS-1$
		it.setMissionStart(rs.getDate("mission_startdate")); //$NON-NLS-1$
		it.setMissionLeader(rs.getString("mission_leader")); //$NON-NLS-1$
		
		it.setSurveyDesign(rs.getString("surveydesign_name")); //$NON-NLS-1$
		it.setSurveyDesignEnd(rs.getDate("surveydesign_enddate")); //$NON-NLS-1$
		it.setSurveyDesignStart(rs.getDate("surveydesign_startdate")); //$NON-NLS-1$
		
		it.setSurveyId(rs.getString("survey_id")); //$NON-NLS-1$
		it.setSurveyEnd(rs.getDate("survey_enddate")); //$NON-NLS-1$
		it.setSurveyStart(rs.getDate("survey_startdate")); //$NON-NLS-1$
		
		it.setSamplingUnitUuid(rs.getBytes("samplingunit_uuid")); //$NON-NLS-1$
		it.setSamplingUnitId(rs.getString("samplingunit_id")); //$NON-NLS-1$
		
		it.setWpDateTime(rs.getDate("wp_date")); //$NON-NLS-1$
		
		it.setWaypointUuid(rs.getBytes("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointTime(rs.getDate("wp_date")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setWaypointObserver(rs.getString("ob_observer")); //$NON-NLS-1$
		it.setObservationUuid(rs.getBytes("ob_uuid")); //$NON-NLS-1$
		
		//build categories
		List<String> categories = new ArrayList<String>();
		for (int i = 0; i < categoryCount; i ++){
			String category = rs.getString("category_"+i); //$NON-NLS-1$
			if (category == null){
				break;
			}
			categories.add(category);
		}
		
		it.setCategory(categories.toArray(new String[categories.size()]));
//		
//		for (MissionAttribute ma : missionAttributes){
//			it.addAttribute(ma.getKeyId(), rs.getObject("ma_" + ma.getKeyId())); //$NON-NLS-1$
//		}
//		
		return it;
	}

	@Override
	protected void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
		super.buildTemporaryTableIndexes(c, tableName);
		
		StringBuilder sql = new StringBuilder();
		sql.append("create index "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("_ob_category_uuid_idx on "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("(ob_category_uuid)"); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
	}
	
	@Override
	public String getFilterTablesJoinColum(){
		return "wp_uuid"; //$NON-NLS-1$
	}
}
