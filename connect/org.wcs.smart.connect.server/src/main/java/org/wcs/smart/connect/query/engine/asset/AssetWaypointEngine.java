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
package org.wcs.smart.connect.query.engine.asset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.asset.query.model.AssetWaypointResultItem;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.IWOEngine;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;

/**
 * Query engine for executing lazy queries using derby.
 * This engines create temporary tables that one to one correspond with the table
 * that user see. {@link AssetObservationResult} obtains the name of this table and is
 * responsible for all other operations (fetching/sorting/deleting tables)
 * 
 */
public class AssetWaypointEngine extends AssetQueryEngine implements IWOEngine<AssetWaypointResultItem>{

	private final Logger logger = Logger.getLogger(AssetWaypointEngine.class.getName());

	private String queryDataTable;
	private Session session;
	private SimpleQuery query;
	
	@Override
	public boolean canExecute(String querytype) {
		return AssetWaypointQuery.KEY.equals(querytype);
	}
	
	@Override
	public String getQueryDataTable(){
		return this.queryDataTable;
	}
	
	@Override
	public String getObservationLabelTable(){
		return this.queryDataTable + "_labels"; //$NON-NLS-1$
	}

	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return null;
	}

	@Override
	public void cleanUp(Session session) throws SQLException {
		dropTable(session, getQueryDataTable());
		dropTable(session, getObservationLabelTable());
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

		query = (SimpleQuery) lquery;
		session = (Session) parameters.get(Session.class.getName());

		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();
		
		
		return session.doReturningWork(new ReturningWork<AssetWaypointResult>() {
			@Override
			public AssetWaypointResult execute(Connection c) throws SQLException {
				
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

				try {			
					parseConservationAreaFilterInternal(query);

					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, query, caFilter, false, true);
					
					populateTemporaryTableExtra(c, session);
					
					//item cnt
					int itemcnt = 0;
					try(ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM " + getQueryDataTable())){ //$NON-NLS-1$
						rs.next();
						itemcnt = rs.getInt(1);
					}
					c.commit();
					
					return new AssetWaypointResult(AssetWaypointEngine.this, itemcnt, getIncludeUuids(parameters));
				}catch (Exception ex){
					c.rollback();
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					if (ex instanceof SQLException) throw (SQLException)ex;
					throw new SQLException(ex);

				} finally {
					try{
						if (filterer != null) filterer.dropTemporaryTables(c);
						c.commit();
					}catch (Exception ex){
						c.rollback();
						logger.log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
			}
		});
	}
	

	private void populateTemporaryTableExtra(Connection c, Session session) throws SQLException {
		String[][] columnsToAdd = new String[][]{
			{"asset_station","varchar(1024)"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"asset_location","varchar(32000)"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"asset_asset","varchar(32000)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"incident_length","integer"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"wp_lastmodifiedbyname","varchar(512)"}, //$NON-NLS-1$ //$NON-NLS-2$

		};
	
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append(queryDataTable);
		sb.append(" SET incident_length = (SELECT incident_length FROM "); //$NON-NLS-1$
		sb.append(" ( SELECT wp_uuid, max(incident_length) as incident_length FROM "); //$NON-NLS-1$
		sb.append(tableName(AssetWaypoint.class));
		sb.append(" GROUP BY wp_uuid) foo WHERE foo.wp_uuid = "); //$NON-NLS-1$
		sb.append(queryDataTable);
		sb.append(".wp_uuid)"); //$NON-NLS-1$
		logger.finest(sb.toString());
		session.createNativeMutationQuery(sb.toString()).executeUpdate();
		
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
		
		logger.finest(sb.toString());
		PreparedStatement updatePs = c.prepareStatement("UPDATE " + queryDataTable + " SET asset_station = ?, asset_location = ?, asset_asset = ? WHERE wp_uuid = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		try(Statement s = c.createStatement()){
			UUID lastWp = null;
			Set<String> asset = new HashSet<>();
			Set<String> station = new HashSet<>();
			Set<String> location = new HashSet<>();
			try(ResultSet rs = s.executeQuery(sb.toString())){
				while(rs.next()) {
					UUID uuid = (UUID)rs.getObject(1);
					if (lastWp != null &&  !lastWp.equals(uuid) ) {
						updatePs.setString(1,  AssetQueryEngine.asString(station));
						updatePs.setString(2, AssetQueryEngine.asString(location));
						updatePs.setString(3, AssetQueryEngine.asString(asset));
						updatePs.setObject(4,  lastWp);
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
			updatePs.setString(1,  AssetQueryEngine.asString(station));
			updatePs.setString(2, AssetQueryEngine.asString(location));
			updatePs.setString(3, AssetQueryEngine.asString(asset));
			updatePs.setObject(4,  lastWp);
			updatePs.addBatch();
			
			updatePs.executeBatch();
		}
		
		//ca information
		populateCaDetails(c, queryDataTable, "ca_uuid",query); //$NON-NLS-1$
		populatedLastModifiedName(c, session, queryDataTable);
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
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid "); //$NON-NLS-1$
		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("wp_uuid uuid,"); //$NON-NLS-1$
		sql.append("wp_id varchar(256),"); //$NON-NLS-1$
		sql.append("wp_x double precision,"); //$NON-NLS-1$
		sql.append("wp_y double precision,"); //$NON-NLS-1$
		sql.append("wp_direction double precision,"); //$NON-NLS-1$
		sql.append("wp_distance double precision,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("wp_lastmodified timestamp,"); //$NON-NLS-1$
		sql.append("wp_lastmodifiedby uuid,"); //$NON-NLS-1$
		sql.append("ca_uuid uuid "); //$NON-NLS-1$

		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}

	
	protected void setFields(AssetWaypointResultItem it, ResultSet rs) throws SQLException{
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setConservationAreaUuid((UUID)rs.getObject("ca_uuid")); //$NON-NLS-1$
		it.setWaypointDateTime(rs.getTimestamp("wp_time").toLocalDateTime()); //$NON-NLS-1$		
		it.setWaypointUuid((UUID)rs.getObject("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getString("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setAssets(rs.getString("asset_asset")); //$NON-NLS-1$
		it.setStation(rs.getString("asset_station")); //$NON-NLS-1$
		it.setLocations(rs.getString("asset_location")); //$NON-NLS-1$
		it.setIncidentLength(rs.getInt("incident_length")); //$NON-NLS-1$
		it.setLastModifiedDate(rs.getTimestamp("wp_lastmodified").toLocalDateTime()); //$NON-NLS-1$
		it.setLastModifiedBy(rs.getString("wp_lastmodifiedbyname")); //$NON-NLS-1$
	}
	
	
	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException {
	}
}
