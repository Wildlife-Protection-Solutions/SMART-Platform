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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.common.model.SimpleQuery;
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
 */
public class AssetWaypointEngine extends AssetQueryEngine implements IDerbyWaypointEngine {

	private String queryDataTable;
	private Session session;
	
	@Override
	public boolean canExecute(String querytype) {
		return AssetWaypointQuery.KEY.equals(querytype);
	}
	
	@Override
	public Session getCurrentConnection() {
		return session;
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
		final AssetPagedWaypointResult result = new AssetPagedWaypointResult(queryDataTable, this);
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyQueryEngine2_Progress_RunningQuery, 2);
				
				IFilterProcessor filterer = null;
				try{
					filterer = AssetWaypointEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable, query);
				}catch (Exception ex){
					throw new SQLException (ex);
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
					ConservationAreaFilter cafilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter,  cafilter, false, true, progress.split(1));
					
					populateTemporaryTableExtra(c, session, progress.split(1));
					
					progress.subTask(Messages.DerbyObservationEngine_Progress_FetchSize);
					updateResultCount(session, result);
					
					progress.subTask(Messages.DerbyObservationEngine_LoadingResultTask);
				}catch( OperationCanceledException ex) {
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
	
	@Override
	public void updateResultCount(Session s, IUpdateableResultSet result){
		//setting result size
		AssetPagedWaypointResult results = (AssetPagedWaypointResult)result;
		
		//setting result size
		Integer count = (Integer) s.createNativeQuery("select count(*) from " + queryDataTable).uniqueResult(); //$NON-NLS-1$
		results.setItemCount(count);
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
	}


	private String toString(Set<String> strings) {
		if (strings.size() == 0) return "";
		if (strings.size() == 1) return strings.iterator().next();
		
		ArrayList<String> sorted = new ArrayList<>();
		sorted.addAll(strings);
		Collections.sort(sorted, (a,b)->Collator.getInstance().compare(a, b));
		StringBuilder s = new StringBuilder();
		s.append(sorted.get(0));
		for (int i = 1; i < sorted.size(); i ++) {
			s.append(", ");
			s.append(sorted.get(i));
		}
		return s.toString();
	}
	
	private void populateTemporaryTableExtra(Connection c, Session session, IProgressMonitor monitor) throws SQLException {
		SubMonitor progress = SubMonitor.convert(monitor, 18);
		
		String[][] columnsToAdd = new String[][]{
			{"asset_station","varchar(1024)"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"asset_location","varchar(32000)"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"asset_asset","varchar(32000)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
		};
	
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		if (monitor.isCanceled()){
			return;
		}
		
		progress.subTask("Populate Station, Location, and Asset Fields");
		progress.split(3);
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT DISTINCT tmp.wp_uuid, ");
		sb.append(tablePrefix(Asset.class) + ".id, ");
		sb.append(tablePrefix(AssetStation.class) + ".id, ");
		sb.append(tablePrefix(AssetStationLocation.class) + ".id ");
		sb.append(" FROM " + queryDataTable + " tmp ");
		sb.append(" JOIN ");
		sb.append(tableNamePrefix(AssetWaypoint.class));
		sb.append(" ON " + tablePrefix(AssetWaypoint.class) + ".wp_uuid = tmp.wp_uuid ");
		sb.append(" JOIN ");
		sb.append(tableNamePrefix(AssetDeployment.class));
		sb.append(" ON " + tablePrefix(AssetWaypoint.class) + ".asset_deployment_uuid = " + tablePrefix(AssetDeployment.class) + ".uuid ");
		sb.append(" JOIN ");
		sb.append(tableNamePrefix(AssetStationLocation.class));
		sb.append(" ON " + tablePrefix(AssetStationLocation.class) + ".uuid = " + tablePrefix(AssetDeployment.class) + ".station_location_uuid ");
		sb.append(" JOIN ");
		sb.append(tableNamePrefix(AssetStation.class));
		sb.append(" ON " + tablePrefix(AssetStation.class) + ".uuid = " + tablePrefix(AssetStationLocation.class) + ".station_uuid ");
		sb.append(" JOIN ");
		sb.append(tableNamePrefix(Asset.class));
		sb.append(" ON " + tablePrefix(Asset.class) + ".uuid = " + tablePrefix(AssetDeployment.class) + ".asset_uuid");
		sb.append(" ORDER BY tmp.wp_uuid ");
		
		QueryPlugIn.logSql(sb.toString());
		PreparedStatement updatePs = c.prepareStatement("UPDATE " + queryDataTable + " SET asset_station = ?, asset_location = ?, asset_asset = ? WHERE wp_uuid = ?");
		try(Statement s = c.createStatement()){
			byte[] lastWp = null;
			Set<String> asset = new HashSet<>();
			Set<String> station = new HashSet<>();
			Set<String> location = new HashSet<>();
			try(ResultSet rs = s.executeQuery(sb.toString())){
				while(rs.next()) {
					byte[] uuid = rs.getBytes(1);
					if (lastWp != null &&  !Arrays.equals(lastWp,  uuid) ) {
						updatePs.setString(1,  toString(station));
						updatePs.setString(2, toString(location));
						updatePs.setString(3, toString(asset));
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
			updatePs.setString(1,  toString(station));
			updatePs.setString(2, toString(location));
			updatePs.setString(3, toString(asset));
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
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(AssetQueryEngine.tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
		
	}

	@Override
	protected String getTemporaryTableSelectClause(boolean includeObservations) {
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
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid "); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	protected String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_date timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("wp_ca_uuid char(16) for bit data "); //$NON-NLS-1$

		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	protected AssetQueryResultItem asQueryResultItem(ResultSet rs, Session session) throws SQLException{
		AssetQueryResultItem it = new AssetQueryResultItem();

		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setWaypointDate(rs.getTimestamp("wp_date")); //$NON-NLS-1$		
		it.setWaypointUuid(UuidUtils.byteToUUID(rs.getBytes("wp_uuid"))); //$NON-NLS-1$
		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setAssets(rs.getString("asset_asset")); //$NON-NLS-1$
		it.setStation(rs.getString("asset_station")); //$NON-NLS-1$
		it.setLocations(rs.getString("asset_location")); //$NON-NLS-1$
		return it;
	}
	
	
	@Override
	protected void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
	}
}
