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
package org.wcs.smart.asset.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetObservationAttachmentResultItem;
import org.wcs.smart.asset.query.model.AssetObservationQuery;
import org.wcs.smart.asset.query.model.AssetObservationResultItem;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.ObservationQueryEngine;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for executing lazy queries using derby.
 * This engines create temporary tables that one to one correspond with the table
 * that user see. {@link AssetPagedObservationResult} obtains the name of this table and is
 * responsible for all other operations (fetching/sorting/deleting tables)
 * 
 * @since 1.0.0
 */
public class AssetObservationEngine extends AssetQueryEngine implements ObservationQueryEngine<AssetObservationResultItem>, IDerbyWaypointEngine {

	private String queryDataTable;
	private int categoryCount;
	private Session session;
	
	@Override
	public boolean canExecute(String querytype) {
		return AssetObservationQuery.KEY.equals(querytype);
	}
	
	/**
	 * Runs the given asset query and retrieves the results from the database.
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
		session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		
		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		final AssetPagedObservationResult result = new AssetPagedObservationResult(this);
		

		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyQueryEngine2_Progress_RunningQuery, 10);
				
				IFilterProcessor filterer = null;
				try{
					filterer = AssetObservationEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, query);
				}catch (Exception ex){
					throw new SQLException(ex);
				}
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				//turn on auto-commit because we want ddl to commit immediately so we don't lock up the database
				//need to make sure we cleanup all temp tables correctly
				c.setAutoCommit(true);
				try {			
					try{
						ConservationAreaFilter cafilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
						filterer.processFilter(c, query.getFilter().getFilter(), dFilter, cafilter, true, true, progress.split(6));
					}catch (Exception ex){
						throw new SQLException (ex);
					}
					
					populateTemporaryTableExtra(c, session, progress.split(3));
					
					progress.checkCanceled();
					//lookup for columns that have data
					progress.subTask(Messages.DerbyObservationEngine_FindDataColumns);
					Set<String> dataColumns = new HashSet<>();
					
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
					progress.worked(1);
					
					progress.subTask(Messages.DerbyObservationEngine_Progress_FetchSize);
					updateResultCount(session, result);
					
					progress.subTask(Messages.DerbyObservationEngine_LoadingResultTask);
				}catch ( OperationCanceledException ex) {
					return;
				}catch (Exception ex){
					checkForOutOfMemory(ex);
					throw new SQLException(ex.getMessage(), ex);
				} finally {
					if (c.isValid(500)) {
						if (filterer != null) filterer.dropTemporaryTables(c);
						if (progress.isCanceled()) dropTables(c);
						c.setAutoCommit(false);
					}
					
				}
			}
		});
		return result;
	}

	@Override
	public void updateResultCount(Session s, IUpdateableResultSet result){
		//setting result size
		AssetPagedObservationResult results = (AssetPagedObservationResult)result;
		
		Integer count = s.createNativeQuery("select count(*) from " + queryDataTable, Integer.class).uniqueResult(); //$NON-NLS-1$
		results.setItemCount(count);
		
		Integer wcount = s.createNativeQuery("select count(*) from (SELECT DISTINCT WP_UUID from " + queryDataTable + ") wp", Integer.class).uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$
		results.setWpCount(wcount);
	}
	
	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	public void dropTables(Connection c) throws SQLException {
		//original table
		dropTable(c, getQueryDataTable());
		dropTable(c, getObservationLabelTable());
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
	
	private void populateTemporaryTableExtra(Connection c, Session session, IProgressMonitor monitor) throws SQLException {
		SubMonitor progress = SubMonitor.convert(monitor, 28);
		
		//NOTE: does 50 worked for monitor in total
		String[][] columnsToAdd = new String[][]{
				{"asset_station","varchar(1024)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{"asset_location","varchar(32000)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{"asset_asset","varchar(32000)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"incident_length", "integer"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"wp_lastmodifiedbyname","varchar(512)"}, //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		if (monitor.isCanceled()){
			return;
		}
		
		//populate incident length field
		progress.subTask(Messages.AssetObservationEngine_IncidentLengthSubTask); 
		progress.split(1);
		
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append(queryDataTable);
		sb.append(" SET incident_length = (SELECT incident_length FROM "); //$NON-NLS-1$
		sb.append(" ( SELECT wp_uuid, max(incident_length) as incident_length FROM "); //$NON-NLS-1$
		sb.append(tableName(AssetWaypoint.class));
		sb.append(" GROUP BY wp_uuid) foo WHERE foo.wp_uuid = "); //$NON-NLS-1$
		sb.append(queryDataTable);
		sb.append(".wp_uuid)"); //$NON-NLS-1$
		QueryPlugIn.logSql(sb.toString());
		session.createNativeMutationQuery(sb.toString()).executeUpdate();
		
		progress.subTask(Messages.AssetObservationEngine_AssetDetailsSubTask); 
		progress.split(3);
		
		sb = new StringBuilder();
		sb.append("SELECT DISTINCT tmp.wp_uuid, "); //$NON-NLS-1$
		sb.append(tablePrefix(Asset.class) + ".id, "); //$NON-NLS-1$
		sb.append(tablePrefix(AssetStation.class) + ".id, "); //$NON-NLS-1$
		sb.append(tablePrefix(AssetStationLocation.class) + ".id "); //$NON-NLS-1$
		sb.append(" FROM " + queryDataTable + " tmp "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(AssetWaypoint.class));
		sb.append(" ON " + tablePrefix(AssetWaypoint.class) + ".wp_uuid = tmp.wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(AssetDeployment.class));
		sb.append(" ON " + tablePrefix(AssetWaypoint.class) + ".asset_deployment_uuid = " + tablePrefix(AssetDeployment.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(AssetStationLocation.class));
		sb.append(" ON " + tablePrefix(AssetStationLocation.class) + ".uuid = " + tablePrefix(AssetDeployment.class) + ".station_location_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(AssetStation.class));
		sb.append(" ON " + tablePrefix(AssetStation.class) + ".uuid = " + tablePrefix(AssetStationLocation.class) + ".station_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Asset.class));
		sb.append(" ON " + tablePrefix(Asset.class) + ".uuid = " + tablePrefix(AssetDeployment.class) + ".asset_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" ORDER BY tmp.wp_uuid "); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sb.toString());
		PreparedStatement updatePs = c.prepareStatement("UPDATE " + queryDataTable + " SET asset_station = ?, asset_location = ?, asset_asset = ? WHERE wp_uuid = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		try(Statement s = c.createStatement()){
			byte[] lastWp = null;
			Set<String> asset = new HashSet<>();
			Set<String> station = new HashSet<>();
			Set<String> location = new HashSet<>();
			try(ResultSet rs = s.executeQuery(sb.toString())){
				while(rs.next()) {
					byte[] uuid = rs.getBytes(1);
					if (lastWp != null &&  !Arrays.equals(lastWp,  uuid) ) {
						updatePs.setString(1, AssetQueryPlugIn.asString(station));
						updatePs.setString(2, AssetQueryPlugIn.asString(location));
						updatePs.setString(3, AssetQueryPlugIn.asString(asset));
						updatePs.setBytes(4,  lastWp);
						updatePs.addBatch();
						asset.clear();
						station.clear();
						location.clear();
					}
					
					asset.add(rs.getString(2));
					station.add(rs.getString(3));
					location.add(rs.getString(4));
					lastWp = uuid;
				}
			}
			updatePs.setString(1, AssetQueryPlugIn.asString(station));
			updatePs.setString(2, AssetQueryPlugIn.asString(location));
			updatePs.setString(3, AssetQueryPlugIn.asString(asset));
			updatePs.setBytes(4,  lastWp);
			updatePs.addBatch();
			
			updatePs.executeBatch();
		}
		
		//ca information
		progress.split(1);
		if (SmartDB.isMultipleAnalysis()){
			//ca id and names are only used for cross-ca analysis
			progress.subTask(Messages.DerbyObservationEngine_Progress_CaInfo);
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(AssetQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(AssetQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
		
		//last modified
		progress.split(1);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT wp_lastmodifiedby FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		QueryPlugIn.logSql(sql.toString());
				
		sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append( queryDataTable );
		sb.append(" SET wp_lastmodifiedbyname = ? WHERE wp_lastmodifiedby = ?"); //$NON-NLS-1$
		QueryPlugIn.logSql(sb.toString());
				
		PreparedStatement lastmodified = c.prepareStatement(sb.toString());
		int cnt = 0;
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
		progress.split(6);
		populateTemporaryTableCategory(c, session);
		
		progress.subTask(Messages.DerbyObservationEngine_Progress_ListAttributesData);
		progress.split(4);
		populateListTreeDataTable(c, session);
	}

	

	/**
	 * Add a label to the temporary attribute list label table
	 * @param s
	 * @param item
	 */
	public void addListLabel(Session s, AttributeListItem item){
		addLabelInternal(s, item);
	}
	
	private void addLabelInternal(Session s, DmObject item){
			
		if (item == null) return;
		String sql = "SELECT count(*) FROM " + getObservationLabelTable() + " WHERE uuid = :uuid "; //$NON-NLS-1$ //$NON-NLS-2$
		NativeQuery<Integer> q = s.createNativeQuery(sql, Integer.class);
		q.setParameter("uuid", item.getUuid()); //$NON-NLS-1$
		if (q.uniqueResult() == 0){
			sql = " INSERT INTO " + getObservationLabelTable() + " (uuid, value) values (:uuid, :label)"; //$NON-NLS-1$ //$NON-NLS-2$
			s.createNativeMutationQuery(sql)
				.setParameter("uuid", item.getUuid()) //$NON-NLS-1$
				.setParameter("label",  item.getName()) //$NON-NLS-1$
				.executeUpdate();
		}
	}
	
	/**
	 * Add a label to the temporary attribute list label table
	 * @param s
	 * @param item
	 */
	public void addTreeLabel(Session s, AttributeTreeNode item){
		addLabelInternal(s, item);
	}

	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".last_modified, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".last_modified_by, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationGroup.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".category_uuid "); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_id varchar(256),"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("wp_lastmodified timestamp,"); //$NON-NLS-1$
		sql.append("wp_lastmodifiedby char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_group_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_category_uuid char(16) for bit data"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}
	
	public AssetObservationAttachmentResultItem asQueryAttachmentResultItem(ResultSet rs, Session session) throws SQLException{
		AssetObservationAttachmentResultItem item = (AssetObservationAttachmentResultItem)asQueryResultItemInternal(true, rs, session);
		
		UUID auuid = UuidUtils.byteToUUID(rs.getBytes("attach_uuid")); //$NON-NLS-1$
		ISmartAttachment a = session.get(ObservationAttachment.class, auuid);
		if (a == null) {
			a = session.get(WaypointAttachment.class, auuid);
		}
		try {
			a.computeFileLocation(session);
		} catch (Exception e) {
			AssetQueryPlugIn.log(e.getMessage(), e);
		}
		item.setAttachment(a);
		
		return item;
	}
	
	public AssetObservationResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		return asQueryResultItemInternal(false, rs, session);
	}
	
	public String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		StringBuilder sb = new StringBuilder();

		String[] selectFields = new String[] {
			"ca_uuid", "ca_id","ca_name","wp_uuid", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"wp_id","wp_x","wp_y","wp_time", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"wp_direction","wp_distance","wp_comment","asset_asset", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"asset_station","asset_location","incident_length", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"wp_lastmodified","wp_lastmodifiedbyname" //$NON-NLS-1$ //$NON-NLS-2$
		};
		for (String s : selectFields) {
			sb.append(prefix);
			sb.append(s);
			sb.append(","); //$NON-NLS-1$
		}
		
		if (includeObservation) {
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
			sb.append("cast(null as char(16) for bit data) as ob_uuid,"); //$NON-NLS-1$
			sb.append("cast(null as char(16) for bit data) as wp_group_uuid"); //$NON-NLS-1$
			for (int i = 0; i < categoryCount; i ++){
				sb.append(",cast(null as varchar(32000)) as category_" + i); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}
	
	private AssetObservationResultItem asQueryResultItemInternal(boolean isAttachment, ResultSet rs, Session session) throws SQLException{
		AssetObservationResultItem it = null;
		if (isAttachment) {
			it = new AssetObservationAttachmentResultItem();
		}else {
			it= new AssetObservationResultItem();
		}
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setConservationAreaUuid(UuidUtils.byteToUUID(rs.getBytes("ca_uuid"))); //$NON-NLS-1$
		it.setWaypointUuid(UuidUtils.byteToUUID(rs.getBytes("wp_uuid"))); //$NON-NLS-1$
		it.setWaypointId(rs.getString("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointDateTime(rs.getTimestamp("wp_time").toLocalDateTime()); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		
		it.setAssets(rs.getString("asset_asset")); //$NON-NLS-1$
		it.setStation(rs.getString("asset_station")); //$NON-NLS-1$
		it.setLocations(rs.getString("asset_location")); //$NON-NLS-1$
		
		it.setIncidentLength(rs.getInt("incident_length")); //$NON-NLS-1$
		it.setLastModifiedDate(rs.getTimestamp("wp_lastmodified").toLocalDateTime()); //$NON-NLS-1$
		it.setLastModifiedBy(rs.getString("wp_lastmodifiedbyname")); //$NON-NLS-1$
		
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

	public int getCategoryCount(){
		return this.categoryCount;
	}
	
	@Override
	public void createTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
		createObsIndex(c, tableName);
		
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
	public Session getCurrentConnection() {
		return session;
	}

	@Override
	public String getQueryDataTable() {
		return queryDataTable;
	}

	@Override
	public String getObservationLabelTable() {
		return queryDataTable +"_labels"; //$NON-NLS-1$
	}
}
