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
package org.wcs.smart.entity.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
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
public class DerbyEntityObservationEngine extends DerbyEntityQueryEngine {

	private String queryDataTable;
	private int categoryCount;
	private SimpleQuery query;
	
	private DerbyPagedObservationResult result;
	@Override
	public boolean canExecute(String querytype) {
		return EntityObservationQuery.KEY.equals(querytype);
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

		this.query = (SimpleQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
	
		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		try{
			result = new DerbyPagedObservationResult(queryDataTable, this, query);
		}catch (Exception ex){
			throw new SQLException(ex);
		}
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask(Messages.DerbyQueryEngine2_Progress_RunningQuery, 70);
				IFilterProcessor filterer = null;
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				try {			
					filterer = DerbyEntityObservationEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable);
					ConservationAreaFilter caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, 
							true, true, monitor);
					
					if (monitor.isCanceled()) return;
					populateTemporaryTableExtra(c, session, monitor);
					
					if (monitor.isCanceled()) return;
					monitor.subTask(Messages.DerbyObservationEngine_Progress_FetchSize);
					//setting result size
					try(ResultSet rs = c.createStatement().executeQuery("select count(*) from " + queryDataTable)) { //$NON-NLS-1$
						if (rs.next()) { 
							result.setItemCount(rs.getInt(1));
						}
					}
					//setting waypoint count
					try (ResultSet rs = c.createStatement().executeQuery("select count(*) from (SELECT DISTINCT WP_UUID from " + queryDataTable + ") wp")){ //$NON-NLS-1$ //$NON-NLS-2$
						if (rs.next()) { 
							result.setWpCount(rs.getInt(1));
						}
					}
				}catch (Exception ex){
					throw new SQLException(ex);
				} finally {
					if (filterer != null) filterer.dropTemporaryTables(c);
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


	
	private void populateTemporaryTableCategory(Connection c, Session session) throws SQLException {
		
		// add data model category columns
		categoryCount = QueryDataModelManager.getInstance().getActiveDepth();
		if (categoryCount < 0){
			//nothing to update
			return;
		}
		
		for (int i = 0; i <= categoryCount; i++) {
			String sql = "ALTER TABLE "+queryDataTable+" ADD category_"+i+" varchar(1024)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		Map<Integer, PreparedStatement> num2Statement = new HashMap<Integer, PreparedStatement>();
		String sql = "SELECT DISTINCT OB_CATEGORY_UUID FROM "+queryDataTable;  //$NON-NLS-1$
		QueryPlugIn.logSql(sql);
		
		
		try(ResultSet rs = c.createStatement().executeQuery(sql)) {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid == null)
					continue;
				String[] names = getCategoryLabels(UuidUtils.byteToUUID(uuid), session);
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
		}
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session, IProgressMonitor monitor) throws Exception {
		//NOTE: does 50 worked for monitor in total
		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ob_observer", "varchar(512)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		if (monitor.isCanceled()){
			return;
		}
		
		monitor.worked(12);
		if (monitor.isCanceled()){
			return;
		}
		
		StringBuilder sql;
		//ca information
		if (SmartDB.isMultipleAnalysis()){
			//ca id and names are only used for cross-ca analysis
			monitor.subTask(Messages.DerbyObservationEngine_Progress_CaInfo);
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(DerbyEntityQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(DerbyEntityQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
		// add observers
		monitor.subTask(Messages.DerbyEntityObservationEngine_ObserverProgress);
		sql = new StringBuilder();
		sql.append("SELECT DISTINCT ob_observer_uuid FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		QueryPlugIn.logSql(sql.toString());

		
		String updateSql = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		String q1 = updateSql + "ob_observer = ? where ob_observer_uuid = ?"; //$NON-NLS-1$
		QueryPlugIn.logSql(q1);
		PreparedStatement observerSt = c.prepareStatement(q1);
		int cnt = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				String name = getEmployeeName(UuidUtils.byteToUUID(uuid), session);

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
		}
		monitor.worked(12);
		if (monitor.isCanceled()) {
			return;
		}		
		//populating categories
		monitor.subTask(Messages.DerbyObservationEngine_Progress_CategoryData);
		populateTemporaryTableCategory(c, session);
		monitor.worked(13);
		if (monitor.isCanceled()){
			return;
		}

		monitor.subTask(Messages.DerbyObservationEngine_Progress_ListAttributesData);
		WpoaLinkedData listData = new WpoaLinkedData("_list", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return QueryDataModelManager.getInstance().getAttributeListItemLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, listData);
		monitor.worked(3);
		if (monitor.isCanceled()){
			return;
		}
		
		monitor.subTask(Messages.DerbyObservationEngine_Progress_TreeAttributesData);
		WpoaLinkedData treeData = new WpoaLinkedData("_tree", "tree_node_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return QueryDataModelManager.getInstance().getAttributeTreeNodeLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, treeData);
		monitor.worked(3);
		if (monitor.isCanceled()){
			return;
		}
	}

	private void populateAdditionalWpoaTable(Connection c, Session session, WpoaLinkedData linkedData) throws Exception {
		String sql = "CREATE TABLE " + queryDataTable + linkedData.getPostfix() + " (uuid char(16) for bit data, value varchar(1024))"; //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql);

		sql = "SELECT DISTINCT wpoa."+linkedData.getUuidColumn() //$NON-NLS-1$
				+", r.P_CA_UUID FROM "  //$NON-NLS-1$
				+ tableNamePrefix(WaypointObservationAttribute.class) + " inner join " //$NON-NLS-1$
				+ queryDataTable + " r on " //$NON-NLS-1$
				+ tablePrefix(WaypointObservationAttribute.class) + ".OBSERVATION_UUID = r.OB_UUID"; //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		
		
		String sql2 = "INSERT INTO "+queryDataTable+linkedData.getPostfix()+" VALUES (?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql2.toString());
		PreparedStatement statement = c.prepareStatement(sql2);
		int count = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql)) {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid != null) {
					byte[] cauuid = rs.getBytes(2);
					String value = linkedData.getLabel(session, UuidUtils.byteToUUID(cauuid), UuidUtils.byteToUUID(uuid));
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
		}
	
		//do the same thing for entity list and tree attributes
		final List<String> entityTypes = new ArrayList<String>();
		query.getFilter().getFilter().accept(new IFilterVisitor() {			
			@Override
			public void visit(IFilter filter) {
				if (filter instanceof EntityAttributeFilter){
					entityTypes.add(((EntityAttributeFilter) filter).getEntityKey());
				}
			}
		});
		if (entityTypes.size() == 0){
			return;
		}
		
		StringBuilder s = new StringBuilder();
		clearParameters();
		s.append("SELECT DISTINCT "); //$NON-NLS-1$
		s.append(tablePrefix(EntityAttributeValue.class) + "." + linkedData.getUuidColumn()); //$NON-NLS-1$
		s.append(", "); //$NON-NLS-1$
		s.append(tablePrefix(EntityType.class) + ".ca_uuid "); //$NON-NLS-1$
		s.append(" FROM "); //$NON-NLS-1$
		s.append(tableNamePrefix(EntityAttributeValue.class ));
		s.append(" join "); //$NON-NLS-1$
		s.append(tableNamePrefix(Entity.class ));
		s.append(" on "); //$NON-NLS-1$
		s.append(tablePrefix(EntityAttributeValue.class ) + ".entity_uuid = "); //$NON-NLS-1$
		s.append(tablePrefix(Entity.class ) + ".uuid "); //$NON-NLS-1$
		s.append(" join "); //$NON-NLS-1$
		s.append(tableNamePrefix(EntityType.class ));
		s.append(" on "); //$NON-NLS-1$
		s.append(tablePrefix(EntityType.class ) + ".uuid = "); //$NON-NLS-1$
		s.append(tablePrefix(Entity.class ) + ".entity_type_uuid "); //$NON-NLS-1$
		s.append(" WHERE keyid IN ("); //$NON-NLS-1$
		for (String et : entityTypes){
			String p1 = addParameterValue(et);
			s.append(p1 + ","); //$NON-NLS-1$
			
		}
		s.deleteCharAt(s.length()-1);
		s.append(")"); //$NON-NLS-1$
		s.append(" AND ca_uuid IN ("); //$NON-NLS-1$
		if (SmartDB.isMultipleAnalysis()){
			for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				String p1 = addParameterValue(ca.getUuid());
				s.append(p1 + ",");	 //$NON-NLS-1$
			}
			s.deleteCharAt(s.length()-1);	
		}else{
			String p1 = addParameterValue(SmartDB.getCurrentConservationArea().getUuid());
			s.append(p1);
		}
		s.append(")"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(s.toString());
		try(ResultSet rs = parseQueryString(c, s.toString()).executeQuery()) {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid != null) {
					byte[] cauuid = rs.getBytes(2);
					String value = linkedData.getLabel(session, UuidUtils.byteToUUID(cauuid), UuidUtils.byteToUUID(uuid));
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
		
		public abstract String getLabel(Session session, UUID cauuid, UUID keyuuid);
	}

	@Override
	protected String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".source, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".category_uuid "); //$NON-NLS-1$

		return sql.toString();
	}

	@Override
	protected String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$ 
		sql.append("wp_source varchar(16),"); //$NON-NLS-1$
		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("ob_observer_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_category_uuid char(16) for bit data"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}
	
	@Override
	protected EntityQueryResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		EntityQueryResultItem it = new EntityQueryResultItem();
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
		it.setWaypointUuid(UuidUtils.byteToUUID(rs.getBytes("wp_uuid"))); //$NON-NLS-1$
		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWpDateTime(rs.getTimestamp("wp_time")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setWaypointObserver(rs.getString("ob_observer")); //$NON-NLS-1$
		it.setObservationUuid(UuidUtils.byteToUUID(rs.getBytes("ob_uuid"))); //$NON-NLS-1$
		
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
}
