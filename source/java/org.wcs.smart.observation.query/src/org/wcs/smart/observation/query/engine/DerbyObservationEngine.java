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
package org.wcs.smart.observation.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.observation.query.ObservationQueryPlugIn;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationAttachmentQueryResultItem;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.ui.SmartLabelProvider;
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
public class DerbyObservationEngine extends AbstractDerbyObservationQueryEngine {

	private String queryDataTable;
	private int categoryCount;
	
	@Override
	public boolean canExecute(String querytype) {
		return ObsObservationQuery.KEY.equals(querytype);
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
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
	
		if (query.getDateFilter() == null){
			return null;
		}
		queryDataTable = createTempTableName();
		final DerbyPagedObservationResult result = new DerbyPagedObservationResult(queryDataTable, this);
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyQueryEngine2_Progress_RunningQuery, 7);
				IFilterProcessor filterer = null;		
				//turn on auto-commit because we want ddl to commit immediately so we don't lock up the database
				c.setAutoCommit(true);
				try {
					filterer = DerbyObservationEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, query);
					
					//create a date filter that caches the dates so the same
					//dates are used for all parts of the query;
					//otherwise different date filters will be computed
					//for different parts of the queries
					DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));
					
					ConservationAreaFilter cafilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, cafilter, true, true, progress.split(4));
					
					populateTemporaryTableExtra(c, session, progress.split(3));
					
					//setting result size
					progress.subTask(Messages.DerbyObservationEngine_Progress_FetchSize);
					progress.split(1);
					try(ResultSet rs = c.createStatement().executeQuery("select count(*) from " + queryDataTable)) { //$NON-NLS-1$
						if (rs.next()) { 
							result.setItemCount(rs.getInt(1));
						}
					}

					//setting waypoint count
					try(ResultSet rs = c.createStatement().executeQuery("select count(*) from (SELECT DISTINCT WP_UUID from " + queryDataTable + ") wp")){ //$NON-NLS-1$ //$NON-NLS-2$
						if (rs.next()) { 
							result.setWpCount(rs.getInt(1));
						}
					}
					
					//lookup for columns that have data
					progress.subTask(Messages.DerbyObservationEngine_FindDataColumns);
					HashSet<String> dataColumns = new HashSet<>();
					
					//looking for attributes that have at least one value
					try(ResultSet rs = c.createStatement().executeQuery("select distinct a.keyid from "+queryDataTable+" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid")) { //$NON-NLS-1$ //$NON-NLS-2$
						while (rs.next()) { 
							dataColumns.add(AttributeQueryColumn.KEY_PREFIX + rs.getString(1));
						}
					}
					//looking for fixed columns that have at least one value
					for (FixedQueryColumn.FixedColumns fc : FixedQueryColumn.FixedColumns.values()) {
						String dbColumn = FixedQueryColumn.getDbColumnName(fc.getKey());
						if (checkColumnHasValues(c, queryDataTable, dbColumn)) {
							dataColumns.add(fc.getKey());
						}
					}

					//looking for category columns that have at least one value
					int numCategory = QueryDataModelManager.getInstance().getActiveDepth();
					for (int i = 0; i < numCategory; i++) {
						String key = CategoryQueryColumn.KEY_PREFIX + i;
						String dbColumn = CategoryQueryColumn.getDbColumnName(key);
						if (checkColumnHasValues(c, queryDataTable, dbColumn)) {
							dataColumns.add(key);
						}
					}
					
					result.setDataColumns(dataColumns);
				}catch(OperationCanceledException ex) {
					return;
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
				byte[] temp = rs.getBytes(1);
				if (temp == null) continue;
				
				UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
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
				statement.setBytes(depth+1, UuidUtils.uuidToByte((UUID)uuid));
				statement.executeUpdate();
			}
		}
	}
	
	/**
	 * Loads the team object from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String getEmployeeName(UUID uuid, Session session){
		if (uuid != null){
			Employee x = (Employee) session.load(Employee.class, uuid);
			if (x != null) {
				return SmartLabelProvider.getShortLabel(x);
			}
		}
		return null;
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session, IProgressMonitor monitor) throws SQLException {
		SubMonitor progress = SubMonitor.convert(monitor, 45);
		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ob_observer","varchar(512)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"wp_lastmodifiedbyname","varchar(512)"}, //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		progress.split(5);
		
		StringBuilder sql;
		//ca information
		if (SmartDB.isMultipleAnalysis()){
			//ca id and names are only used for cross-ca analysis
			progress.subTask(Messages.DerbyObservationEngine_Progress_CaInfo);
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(AbstractDerbyObservationQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(AbstractDerbyObservationQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
		
		//add observers
		progress.split(10);
		progress.subTask(Messages.DerbyObservationEngine_Observers);
		sql = new StringBuilder();
		sql.append("SELECT DISTINCT ob_observer_uuid FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		QueryPlugIn.logSql(sql.toString());

		
		String updateSql = "UPDATE "+queryDataTable+" SET "; //$NON-NLS-1$ //$NON-NLS-2$
		String q1 = updateSql + "ob_observer = ? where ob_observer_uuid = ?"; //$NON-NLS-1$
		QueryPlugIn.logSql(q1);
		PreparedStatement observerSt = c.prepareStatement(q1);
		int cnt = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())){
			while (rs.next()) {
				byte[] tmp = rs.getBytes(1);
				if (tmp == null) continue;
				UUID uuid = UuidUtils.byteToUUID(tmp);
				String name = getEmployeeName(uuid, session);
				
				if (name != null) {
					observerSt.setString(1, name);
					observerSt.setBytes(2, UuidUtils.uuidToByte((UUID)uuid));
					observerSt.addBatch();
					cnt++;
					if (cnt >= 100){
						observerSt.executeBatch();
						cnt = 0;
					}
				}
			}
			observerSt.executeBatch();
		}
		
		//add last modified
		progress.split(10);
		progress.subTask(Messages.DerbyObservationEngine_Observers);
		sql = new StringBuilder();
		sql.append("SELECT DISTINCT wp_lastmodifiedby FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		QueryPlugIn.logSql(sql.toString());

		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append( queryDataTable );
		sb.append(" SET wp_lastmodifiedbyname = ? WHERE wp_lastmodifiedby = ?"); //$NON-NLS-1$
		QueryPlugIn.logSql(sb.toString());
		
		PreparedStatement lastmodified = c.prepareStatement(sb.toString());
		cnt = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())){
			while (rs.next()) {
				byte[] tmp = rs.getBytes(1);
				if (tmp == null) continue;
				UUID uuid = UuidUtils.byteToUUID(tmp);
				String name = getEmployeeName(uuid, session);
					
				if (name != null) {
					lastmodified.setString(1, name);
					lastmodified.setBytes(2, UuidUtils.uuidToByte((UUID)uuid));
					lastmodified.addBatch();
					cnt++;
					if (cnt >= 100){
						lastmodified.executeBatch();
						cnt = 0;
					}
				}
			}
			lastmodified.executeBatch();
		}
				
				
		//populating categories
		progress.subTask(Messages.DerbyObservationEngine_Progress_CategoryData);
		progress.split(10);
		populateTemporaryTableCategory(c, session);
		
		progress.subTask(Messages.DerbyObservationEngine_Progress_ListAttributesData);
		progress.split(5);
		WpoaLinkedData listData = new WpoaLinkedData("_list", "list_element_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return QueryDataModelManager.getInstance().getAttributeListItemLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, listData);
	
		
		progress.subTask(Messages.DerbyObservationEngine_Progress_TreeAttributesData);
		progress.split(5);
		WpoaLinkedData treeData = new WpoaLinkedData("_tree", "tree_node_uuid") { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			public String getLabel(Session session, UUID cauuid, UUID uuid) {
				return QueryDataModelManager.getInstance().getAttributeTreeNodeLabel(session, cauuid, uuid);
			}
		};
		populateAdditionalWpoaTable(c, session, treeData);
		
	}

	private void populateAdditionalWpoaTable(Connection c, Session session, WpoaLinkedData linkedData) throws SQLException {
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
				byte[] t = rs.getBytes(1);
				if (t == null) continue;
				
				UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
				if (uuid == null) continue;
				
				UUID cauuid = UuidUtils.byteToUUID(rs.getBytes(2));
				String value = linkedData.getLabel(session, cauuid, uuid);
				statement.setBytes(1,  UuidUtils.uuidToByte((UUID)uuid));
				statement.setString(2, value);
				statement.addBatch();
				count++;
				if (count >= 100){
					statement.executeBatch();
					count = 0;
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
		sql.append(" SELECT DISTINCT "); //$NON-NLS-1$
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
		sql.append(tablePrefix(Waypoint.class) + ".last_modified, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".last_modified_by, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationGroup.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".employee_uuid, "); //$NON-NLS-1$
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
		sql.append("wp_lastmodified timestamp,"); //$NON-NLS-1$
		sql.append("wp_lastmodifiedby char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_group_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_observer_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_category_uuid char(16) for bit data"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}
	
	@Override
	protected ObservationAttachmentQueryResultItem asQueryAttachmentResultItem(ResultSet rs, Session session) throws SQLException{
		
		ObservationAttachmentQueryResultItem item = (ObservationAttachmentQueryResultItem) asQueryResultItemInternal(true,  rs, session);
		
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
	
	@Override
	protected ObservationQueryResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		return asQueryResultItemInternal(false, rs, session);
	}
	
	
	
	protected ObservationQueryResultItem asQueryResultItemInternal(boolean isAttachment, ResultSet rs, Session session) throws SQLException{
		
		ObservationQueryResultItem it = null;
		if (isAttachment) {
			it = new ObservationAttachmentQueryResultItem();
		}else {
			it = new ObservationQueryResultItem();
		}
		
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
		it.setWaypointUuid(UuidUtils.byteToUUID(rs.getBytes("wp_uuid"))); //$NON-NLS-1$
		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWpDateTime(rs.getTimestamp("wp_time")); //$NON-NLS-1$
		it.setLastModifiedDate(rs.getTimestamp("wp_lastmodified")); //$NON-NLS-1$
		it.setLastModifiedBy(rs.getString("wp_lastmodifiedbyname")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setWaypointObserver(rs.getString("ob_observer")); //$NON-NLS-1$
		byte[] t = rs.getBytes("ob_uuid"); //$NON-NLS-1$
		if (t == null){
			it.setObservationUuid(null);
		}else{
			it.setObservationUuid(UuidUtils.byteToUUID(t)); 
		}
		
		t = rs.getBytes("wp_group_uuid"); //$NON-NLS-1$
		if (t == null){
			it.setObservationGroupUuid(null);
		}else{
			it.setObservationGroupUuid(UuidUtils.byteToUUID(t)); 
		}
		
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

	String getDistinctWaypointQuery(String prefix, boolean includeObs) {
		String[] fields = new String[] {"ca_id","ca_name","wp_source","wp_uuid", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"wp_id","wp_x","wp_y","wp_time", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"wp_lastmodified","wp_lastmodifiedbyname","wp_direction", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"wp_distance","wp_comment"}; //$NON-NLS-1$ //$NON-NLS-2$
		StringBuilder sb = new StringBuilder();
		for (String s : fields) {
			sb.append(prefix);
			sb.append(s);
			sb.append(","); //$NON-NLS-1$
		}
		if (includeObs){
			sb.append(prefix);
			sb.append("ob_observer,"); //$NON-NLS-1$
			sb.append(prefix);
			sb.append("ob_uuid,"); //$NON-NLS-1$
			sb.append(prefix);
			sb.append("wp_group_uuid"); //$NON-NLS-1$

			for (int i = 0; i < categoryCount; i ++){
				sb.append(","); //$NON-NLS-1$
				sb.append(prefix);
				sb.append("category_" + i); //$NON-NLS-1$
			}
			
		}else {
			sb.append("cast(null as varchar(32000)) as ob_observer,"); //$NON-NLS-1$
			sb.append("cast(null as char(16) for bit data) as ob_uuid,"); //$NON-NLS-1$
			sb.append("cast(null as char(16) for bit data) as wp_group_uuid"); //$NON-NLS-1$

			for (int i = 0; i < categoryCount; i ++){
				sb.append(","); //$NON-NLS-1$
				sb.append("cast(null as varchar(32000)) as category_" + i); //$NON-NLS-1$
			}			
		}
		return sb.toString();
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
