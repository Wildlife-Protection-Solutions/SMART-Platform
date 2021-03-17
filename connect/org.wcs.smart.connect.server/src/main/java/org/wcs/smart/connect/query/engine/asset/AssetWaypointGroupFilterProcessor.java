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
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.asset.query.engine.visitors.AreaFilterVisitor;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter.Source;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.CategoryAttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class AssetWaypointGroupFilterProcessor implements IFilterProcessor{

	private final Logger logger = Logger.getLogger(AssetWaypointGroupFilterProcessor.class.getName());

	private String tableName;
	private String waypointTable;
	
	private AssetQueryEngine engine;
	private Query query;
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public AssetWaypointGroupFilterProcessor(String tableName, AssetQueryEngine engine, Query query){
		this.tableName = tableName;
		this.engine = engine;
		this.waypointTable = engine.createTempTableName();
		this.query = query;
	}
	
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 * @throws SQLException 
	 */
	@Override
	public void dropTemporaryTables(Connection c) throws SQLException{
		engine.dropTable(c, waypointTable);
		
		for (FilterTable tableName: engine.filterTables.values()){
			engine.dropTable(c,  tableName.tablename);
		}
	}

	/**
	 * 
	 * @param c database connection
	 * @param queryFilter query filter
	 * @param dateFilter date filter
	 * @param caFilter conservation area filter
	 * @param populateObservation if observation fields (wp_uuid, wp_ob_uuid) are to be populated
	 * @param includeEmptyObservations if waypoints with no observations should be included
	 * @param monitor
	 * @throws SQLException
	 */
	@Override
	public void processFilter(Connection c, IFilter queryFilter, 
			DateFilter dateFilter, Query query, ConservationAreaFilter caFilter, 
			boolean populateObservation,
			boolean includeEmptyObservations) throws SQLException{
		
		IFilter qFilter = queryFilter;
		if (qFilter == null) qFilter = EmptyFilter.INSTANCE;
		createWaypointFilterTables(c, qFilter, dateFilter, caFilter);
		createTemporaryTable(c);
		populateTemporaryTable(qFilter, dateFilter, caFilter, includeEmptyObservations, c, populateObservation);
		
	}
	
	
	/*
	 * creates the query observation flattened table
	 */
	private void createTemporaryTable(Connection c) throws SQLException {
		String createTableStatement = engine.getTemporaryTableCreateClause(tableName);
		logger.finest(createTableStatement);
		c.createStatement().execute(createTableStatement);
		engine.buildTemporaryTableIndexes(c, tableName);
	}
	
	/*
	 * return the sql prefix for the given class
	 */
	private String prefix(Class<?> clazz){
		return engine.tablePrefix(clazz);
	}
	
	/*
	 * combine the table name with the table prefix for
	 * the given class
	 * Asset.cass = "smart.asset a"
	 */
	private String namePrefix(Class<?> clazz){
		return engine.tableNamePrefix(clazz);
	}
	
	/**
	 * Populates the query temporary table.
	 * 
	 * @param queryFilter the query filter
	 * @param dateFilter the date filter
	 * @param caFilter the conservation area filter
	 * @param onlyObservations if only observation asset records with observations
	 * are to be returned,  false will return all asset records
	 * even if they don't have an observation
	 * @param c database connection
	 * @param populateObservation if the processing requires the observation
	 * information attached to the results (otherwise ob_uuid will be populated
	 * with null)
	 * 
	 * @param c the database connection
	 * 
	 * @throws SQLException
	 */
	private void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c,
			boolean populateObservation)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		
		engine.clearParameters();
		
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));

		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(namePrefix(Waypoint.class));
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservationGroup.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationGroup.class) + ".wp_uuid"); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(waypointTable + " as waypointTable "); //$NON-NLS-1$
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationGroup.class) + ".uuid = "); //$NON-NLS-1$
		sql.append("waypointTable.wp_group_uuid "); //$NON-NLS-1$
		
		if (caFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
			if (filter.length() > 0) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	
		
		if (dateFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
			if (filter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(filter);
			}
		}
		
		if (populateObservation){
			
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class) + ".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$
		}
			
		for (Entry<IFilter, FilterTable> cols : engine.filterTables.entrySet()){
			FilterTable t = cols.getValue();
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(t.tablename);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(t.tablename +"." + t.columnname + " = "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(prefix(WaypointObservationGroup.class) + ".uuid "); //$NON-NLS-1$
		}
			
			
		AreaFilterVisitor av = new AreaFilterVisitor(sql, engine, query.getConservationArea());
		queryFilter.accept(av);

		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
			    sql.append(filter);
			}
		}
		logger.finest(sql.toString());
		try(NamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	
	private void createWaypointFilterTables(Connection c, IFilter filter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter)
			throws SQLException {
		
		engine.createWaypointGroupTable(c, waypointTable,
				Collections.singleton(WaypointSourceEngine.INSTANCE.getSource(AssetWaypointSource.KEY)),  
				caFilter, dateFilter);

		IFilterVisitor attProcessor = new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if ( filter instanceof AttributeFilter ||
					filter instanceof CategoryFilter  ||	
					filter instanceof CategoryAttributeFilter ||
					filter instanceof AssetFilter ||
					filter instanceof AssetAttributeFilter){						
					
					String colName = engine.createTempTableName();
					engine.filterTables.put(filter, new FilterTable(colName, "wp_group_uuid")); //$NON-NLS-1$
				}
			}
		};
		filter.accept(attProcessor);
		
		for (Entry<IFilter, FilterTable> cols : engine.filterTables.entrySet()){
			IFilter lfilter = cols.getKey();
			FilterTable t = cols.getValue();
			
			engine.createFilterTable(c, t);
			
			if ( lfilter instanceof AttributeFilter ||
					lfilter instanceof CategoryFilter  ||	
					lfilter instanceof CategoryAttributeFilter ){
				engine.processWaypointGroupDataModelFilter(t, lfilter, waypointTable, c);
			}else if (lfilter instanceof AssetAttributeFilter) {
				processAssetAttributeFilter((AssetAttributeFilter)lfilter, t, c);
			}else if (lfilter instanceof AssetFilter) {
				processAssetFilter((AssetFilter)lfilter, t, c);
			}else {
				throw new UnsupportedOperationException("Filter not supported for observation queries"); //$NON-NLS-1$
			}
		}
		
	}
	
	private void processAssetFilter(AssetFilter assetFilter, FilterTable t, Connection c) throws SQLException {
		//create temporary table for attribute observations
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(t.tablename + " (" + t.columnname + ")"); //$NON-NLS-1$ //$NON-NLS-2$	
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append("a.wp_group_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(waypointTable + " a"); //$NON-NLS-1$
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetWaypoint.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append("a.wp_uuid = " + prefix(AssetWaypoint.class) + ".wp_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		
		sql.append(" LEFT JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetDeployment.class));
		sql.append(" ON " ); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class) + ".uuid = " + prefix(AssetWaypoint.class) + ".asset_deployment_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
		StringBuilder where = new StringBuilder();
		
		if (assetFilter.getAssetOption() == AssetFilterOption.ASSETTYPE) {
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(Asset.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".asset_uuid = " + prefix(Asset.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
			String key = engine.addParameterValue(assetFilter.getValue());
			where.append(prefix(Asset.class) + ".asset_type_uuid = " + key); //$NON-NLS-1$
		}
		if (assetFilter.getAssetOption() == AssetFilterOption.ASSET) {
			String key = engine.addParameterValue(assetFilter.getValue());
			where.append(prefix(AssetDeployment.class) + ".asset_uuid = " + key); //$NON-NLS-1$
		}
		
		if (assetFilter.getAssetOption() == AssetFilterOption.STATIONLOCATION) {
			String key = engine.addParameterValue(assetFilter.getValue());
			where.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + key); //$NON-NLS-1$
		}
		if (assetFilter.getAssetOption() == AssetFilterOption.STATION) {
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStationLocation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			String key = engine.addParameterValue(assetFilter.getValue());
			where.append(prefix(AssetStationLocation.class) + ".station_uuid = " + key); //$NON-NLS-1$
		}
		sql.append (" WHERE "); //$NON-NLS-1$
		sql.append(where);
		
		logger.finest(sql.toString());
		try(NamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
		
	private void processAssetAttributeFilter(AssetAttributeFilter assetFilter, FilterTable t, Connection c) throws SQLException {
		//create temporary table for attribute observations
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(t.tablename + " (" + t.columnname + ")"); //$NON-NLS-1$ //$NON-NLS-2$	
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append("a.wp_group_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(waypointTable + " a"); //$NON-NLS-1$
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservationGroup.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append("a.wp_group_uuid = " + prefix(WaypointObservationGroup.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetWaypoint.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationGroup.class) + ".wp_uuid = " + prefix(AssetWaypoint.class) + ".wp_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		
		sql.append(" LEFT JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetDeployment.class));
		sql.append(" ON " ); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class) + ".uuid = " + prefix(AssetWaypoint.class) + ".asset_deployment_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
		StringBuilder where = new StringBuilder();
		
		Class<?> valueClass = null;
		if (assetFilter.getSource() == Source.ASSET) {
			valueClass = AssetAttributeValue.class;
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(Asset.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".asset_uuid = " + prefix(Asset.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(Asset.class) + ".uuid = " + prefix(valueClass) + ".asset_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			
		}else if (assetFilter.getSource() == Source.STATION) {
			valueClass = AssetStationAttributeValue.class;
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStationLocation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStationLocation.class) + ".station_uuid = " + prefix(AssetStation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStation.class) + ".uuid = " + prefix(valueClass) + ".station_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			
			 
		}else if (assetFilter.getSource() == Source.STATIONLOCATION) {
			valueClass = AssetStationLocationAttributeValue.class;
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStationLocation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStationLocation.class) + ".uuid = " + prefix(valueClass) + ".station_location_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
		}else if (assetFilter.getSource() == Source.DEPLOYMENT) {
			valueClass = AssetDeploymentAttributeValue.class;
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".uuid = " + prefix(valueClass) + ".asset_deployment_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		}
			
		sql.append(" LEFT JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetAttribute.class));
		sql.append(" ON " ); //$NON-NLS-1$
		sql.append(prefix(valueClass) + ".attribute_uuid = " + prefix(AssetAttribute.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
		String tprefix = prefix(valueClass);
			
		if (assetFilter.getAttributeType() == AttributeType.LIST) {
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetAttributeListItem.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(valueClass) + ".list_item_uuid = " + prefix(AssetAttributeListItem.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				
			tprefix = prefix(AssetAttributeListItem.class);
		}
		
		String q = PsqlFilterToSqlGenerator.INSTANCE.asSql(assetFilter, tprefix, engine);
		
		String key = engine.addParameterValue(assetFilter.getAttributeKey());
		if (assetFilter.getAttributeType() == AttributeType.DATE) {
			where.append("CASE WHEN " + prefix(AssetAttribute.class) + ".keyid = " + key); //$NON-NLS-1$ //$NON-NLS-2$
			where.append(" THEN "); //$NON-NLS-1$
			where.append(q);
			where.append(" ELSE null END"); //$NON-NLS-1$
		}else {
			where.append(prefix(AssetAttribute.class) + ".keyid = " + key); //$NON-NLS-1$
			where.append(" AND "); //$NON-NLS-1$
			where.append(q);
		}
		
		sql.append (" WHERE "); //$NON-NLS-1$
		sql.append(where);
		
		logger.finest(sql.toString());
		engine.parseQueryString(c, sql.toString()).executeUpdate();
	}		
	
}
