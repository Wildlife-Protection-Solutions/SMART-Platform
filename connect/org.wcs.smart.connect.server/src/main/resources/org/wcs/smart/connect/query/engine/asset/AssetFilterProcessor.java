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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
import org.wcs.smart.asset.query.engine.visitors.AssetFilterCollector;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter.Source;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.connect.query.engine.PsqlNamedPreparedStatement;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.common.engine.visitors.AttributeFilterCollectorVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationFilterVisitor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class AssetFilterProcessor implements IFilterProcessor {

	private final Logger logger = Logger.getLogger(AssetFilterProcessor.class.getName());

	private String tableName;
	private String observationTable;
	
	private AssetQueryEngine engine;
	private Query query;
	
	private HasObservationFilterVisitor observationFilterVisitor = new HasObservationFilterVisitor();
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public AssetFilterProcessor(String tableName, AssetQueryEngine engine, Query query){
		this.tableName = tableName;
		this.engine = engine;
		this.observationTable = engine.createTempTableName();
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
		engine.dropTable(c, observationTable);
		
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
	 * @param includeEmptyObservations if waypoints with no observations should be included <- igorned by this query engine
	 * @param monitor
	 * @throws SQLException
	 */
	@Override
	public void processFilter(Connection c, IFilter queryFilter, 
			DateFilter dateFilter, Query query, ConservationAreaFilter caFilter, 
			boolean populateObservation,
			boolean includeEmptyObservations) throws SQLException{
		
		IFilter qFilter = queryFilter;
		if (qFilter == null){
			qFilter = EmptyFilter.INSTANCE;
		}
		qFilter.accept(observationFilterVisitor);	
		
		Map<IFilter, FilterTable> assetFilterToTableName = processAssetTables(c, queryFilter, dateFilter, caFilter);
		
		if (observationFilterVisitor.hasAttributeFilter()){
			createObservationTable(c, qFilter, dateFilter, caFilter);
		}

		createTemporaryTable(c);
		
		populateTemporaryTable(qFilter, dateFilter, caFilter, c, populateObservation, assetFilterToTableName);

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
	 * return the table name for the associate object 
	 */
	private String name(Class<?> clazz){
		return engine.tableName(clazz);
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
	 * Populates the query temporary table.  This populates a table with
	 * all waypoints that match the conservation area filter and date filter.
	 * 
	 * 
	 * @param queryFilter the query filter
	 * @param dateFilter the date filter
	 * @param caFilter the conservation area filter
	 * @param c database connection
	 * @param populateObservation if the processing requires the observation
	 * information attached to the results (otherwise ob_uuid will be populated
	 * with null)
	 * 
	 * @param c the database connection
	 * 
	 * @throws Exception
	 */
	private void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			Connection c,
			boolean populateObservation, Map<IFilter,FilterTable> assetFilterTables)
			throws SQLException {

		AssetFilterCollector assetVisitor = new AssetFilterCollector();
		assetVisitor.visit(queryFilter);
		
		StringBuilder sql = new StringBuilder();
		
		engine.clearParameters();
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));
		
		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$

		sql.append(namePrefix(Waypoint.class));
		
		for (FilterTable t : assetFilterTables.values()) {
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(t.tablename);
			sql.append(" on " + t.tablename + "." + t.columnname + " = " + prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		
		engine.filterTables.clear();
		engine.filterTables.putAll(assetFilterTables);
		
		if (populateObservation || 
				observationFilterVisitor.hasAttributeFilter() || 
				observationFilterVisitor.hasCategoryFilter()){
		
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservationGroup.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class));
			sql.append(".wp_uuid "); //$NON-NLS-1$
			
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class));
			sql.append(".wp_group_uuid "); //$NON-NLS-1$
		}	
		
		if (observationFilterVisitor.hasAttributeFilter() || 
				observationFilterVisitor.hasCategoryFilter()){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(name(Category.class));
			sql.append(" "); //$NON-NLS-1$
			sql.append(prefix(Category.class));
			
			sql.append(" on " + prefix(Category.class) //$NON-NLS-1$
					+ ".uuid = " //$NON-NLS-1$
					+ prefix(WaypointObservation.class)
					+ ".category_uuid "); //$NON-NLS-1$
			if (observationFilterVisitor.hasAttributeFilter()){
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(observationTable + " qa on qa.observation_uuid = "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservation.class) + ".uuid"); //$NON-NLS-1$
			}
		}

		// area filters
		AreaFilterVisitor areaVisitor = new AreaFilterVisitor(sql, engine, query.getConservationArea());
		queryFilter.accept(areaVisitor);
		
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class) + ".source = '" + AssetWaypointSource.KEY + "' "); //$NON-NLS-1$ //$NON-NLS-2$
		
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
		
		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" and ( "); //$NON-NLS-1$
			    sql.append(filter);
			    sql.append( " ) "); //$NON-NLS-1$
			}
		}
		logger.finest(sql.toString());
		try(PsqlNamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	/**
	 * Creates an observation table that contain wp uuid (filtered by conservation are
	 * and dates) and one column for each unique attribute in the filter.
	 *  
	 * @param c
	 * @param filter
	 * @param dateFilter
	 * @param caFilter
	 * @param monitor
	 * @throws SQLException
	 */
	private void createObservationTable(Connection c, IFilter filter, DateFilter dateFilter, ConservationAreaFilter caFilter) throws SQLException {
		
		AttributeFilterCollectorVisitor collector = new AttributeFilterCollectorVisitor();
		filter.accept(collector);
		Collection<AttributeInfo> keys = collector.getAttributeInfo();
		
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + observationTable + " (observation_uuid uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		for (AttributeInfo key : keys) {
			sql.append(", " + key.getKey() + " " //$NON-NLS-1$ //$NON-NLS-2$
					+ engine.getDataType(key.getType()));
		}
		sql.append(")"); //$NON-NLS-1$
		
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + engine.getIndexName(observationTable) + "_obuuid_idx on " + observationTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
		String attributeTempTable = engine.createTempTableName();
		for (AttributeInfo key : keys){
			//create temporary table for attribute observations
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(attributeTempTable); 
			sql.append("(observation_uuid uuid, value "); //$NON-NLS-1$
			sql.append( engine.getDataType(key.getType()) );
			sql.append( ")"); //$NON-NLS-1$
			logger.finest(sql.toString());
			
			c.createStatement().execute(sql.toString());
			
			try {
				engine.clearParameters();
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" SELECT "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".observation_uuid, "); //$NON-NLS-1$

				if (key.getType() == AttributeType.LIST) {
					sql.append("l.keyid "); //$NON-NLS-1$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append("t.hkey "); //$NON-NLS-1$
				} else {
					sql.append(prefix(WaypointObservationAttribute.class)
							+ "." + key.getColumn()); //$NON-NLS-1$						
				}
				sql.append(" as "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" "); //$NON-NLS-1$

				sql.append("FROM "); //$NON-NLS-1$
				sql.append(namePrefix(Waypoint.class));
			
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservationGroup.class));
				sql.append(" on " + prefix(Waypoint.class) + ".uuid = " + prefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservation.class));
				sql.append(" on " + prefix(WaypointObservationGroup.class) + ".uuid = " + prefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(WaypointObservationAttribute.class)
						+ " as " + prefix(WaypointObservationAttribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(WaypointObservation.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(Attribute.class)
						+ " as " + prefix(Attribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(Attribute.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (key.getType() == AttributeType.LIST) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(AttributeListItem.class));
					sql.append(" l on l.uuid = " + prefix(WaypointObservationAttribute.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(AttributeTreeNode.class));
					sql.append(" t on t.uuid = " + prefix(WaypointObservationAttribute.class) + ".tree_node_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sql.append("WHERE "); //$NON-NLS-1$
				
				sql.append(prefix(Waypoint.class) + ".source = '" + AssetWaypointSource.KEY + "' "); //$NON-NLS-1$ //$NON-NLS-2$
				
				if (caFilter != null) {
					String cfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
					if (cfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(cfilter);
					}
				}
				
				if (dateFilter != null) {
					String dfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
					if (dfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(dfilter);
					}
				}
				
				String p = engine.addParameterValue(key.getKey());
				sql.append(" AND (" + prefix(Attribute.class) + ".keyid = " + p + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				logger.finest(sql.toString());
				try(PsqlNamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
					ps.executeUpdate();
				}

				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(engine.getIndexName(attributeTempTable));
				sql.append("__observation_uuid_idx on "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("(observation_uuid)"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());

				// - add observation to main table
				// join existing readings
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" set "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT a.value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE a.observation_uuid = "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(".observation_uuid)"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());
				
				// add missing observations
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append("(observation_uuid, "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(")"); //$NON-NLS-1$
				sql.append("(SELECT  observation_uuid, value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE NOT EXISTS (SELECT observation_uuid FROM "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" b WHERE b.observation_uuid = a.observation_uuid))"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + attributeTempTable); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
		
	}
	
	
	
	/**
	 * Creates an observation table that contain wp uuid (filtered by conservation are
	 * and dates) and one column for each unique attribute in the filter.
	 *  
	 * @param c
	 * @param filter
	 * @param dateFilter
	 * @param caFilter
	 * @param monitor
	 * @throws SQLException
	 */
	private Map<IFilter, FilterTable> processAssetTables(Connection c, IFilter filter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter)
			throws SQLException {

		AssetFilterCollector collector = new AssetFilterCollector();
		filter.accept(collector);
		Collection<IFilter> filters = collector.getFilters();
		
		Map<IFilter, FilterTable> filterToTableName = new HashMap<>();
		
		for (IFilter part : filters) {
			engine.clearParameters();
			
			String assetTable = engine.createTempTableName();
			filterToTableName.put(part, new FilterTable(assetTable, "wp_uuid")); //$NON-NLS-1$

			// -- build temporary table
			StringBuilder sql = new StringBuilder();
			sql.append("CREATE TABLE " + assetTable + " (wp_uuid uuid) "); //$NON-NLS-1$ //$NON-NLS-2$
		
			logger.finest(sql.toString());
			c.createStatement().execute(sql.toString());

			// -- create index
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + engine.getIndexName(assetTable) + "_wpuuid_idx on " + assetTable + " (wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql.toString());
			c.createStatement().execute(sql.toString());
		
			
			//create temporary table for attribute observations
			sql = new StringBuilder();
			sql.append("INSERT INTO  "); //$NON-NLS-1$
			sql.append(assetTable); 
			sql.append(" SELECT "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$
			sql.append(" FROM "); //$NON-NLS-1$
			sql.append(namePrefix(Waypoint.class));
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetWaypoint.class));
			sql.append(" ON "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class) + ".uuid = " + prefix(AssetWaypoint.class) + ".wp_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (caFilter != null) {
				String cfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
				if (cfilter.length() > 0) {
					sql.append(" and "); //$NON-NLS-1$
					sql.append(cfilter);
				}
			}
				
			if (dateFilter != null) {
				String dfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
				if (dfilter.length() > 0) {
					sql.append(" and "); //$NON-NLS-1$
					sql.append(dfilter);
				}
			}
			
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetDeployment.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".uuid = " + prefix(AssetWaypoint.class) + ".asset_deployment_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			StringBuilder where = new StringBuilder();
			
			if (part instanceof AssetFilter) {
				AssetFilter assetFilter = (AssetFilter)part;
				
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
			}else if (part instanceof AssetAttributeFilter) {
				AssetAttributeFilter aFilter = (AssetAttributeFilter)part;
				
				Class<?> valueClass = null;
				if (aFilter.getSource() == Source.ASSET) {
					valueClass = AssetAttributeValue.class;
					
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(namePrefix(Asset.class));
					sql.append(" ON " ); //$NON-NLS-1$
					sql.append(prefix(AssetDeployment.class) + ".asset_uuid = " + prefix(Asset.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
					
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(namePrefix(valueClass));
					sql.append(" ON " ); //$NON-NLS-1$
					sql.append(prefix(Asset.class) + ".uuid = " + prefix(valueClass) + ".asset_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
					
					
				}else if (aFilter.getSource() == Source.STATION) {
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
					
					
					 
				}else if (aFilter.getSource() == Source.STATIONLOCATION) {
					valueClass = AssetStationLocationAttributeValue.class;
					
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(namePrefix(AssetStationLocation.class));
					sql.append(" ON " ); //$NON-NLS-1$
					sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
					
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(namePrefix(valueClass));
					sql.append(" ON " ); //$NON-NLS-1$
					sql.append(prefix(AssetStationLocation.class) + ".uuid = " + prefix(valueClass) + ".station_location_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
					
				}else if (aFilter.getSource() == Source.DEPLOYMENT) {
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
				String key = engine.addParameterValue(aFilter.getAttributeKey());
				sql.append(" AND " + prefix(AssetAttribute.class) + ".keyid = " + key);  //$NON-NLS-1$//$NON-NLS-2$
				
				String tprefix = prefix(valueClass);
					
				if (aFilter.getAttributeType() == AttributeType.LIST) {
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(namePrefix(AssetAttributeListItem.class));
					sql.append(" ON " ); //$NON-NLS-1$
					sql.append(prefix(valueClass) + ".list_item_uuid = " + prefix(AssetAttributeListItem.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
						
					tprefix = prefix(AssetAttributeListItem.class);
				}
				
				key = engine.addParameterValue(aFilter.getAttributeKey());
				where.append(prefix(AssetAttribute.class) + ".keyid = " + key); //$NON-NLS-1$
					
				where.append(" AND "); //$NON-NLS-1$
				String q = PsqlFilterToSqlGenerator.INSTANCE.asSql(aFilter, tprefix, engine);
				where.append(q);
				
			}
			
			
			sql.append (" WHERE "); //$NON-NLS-1$
			sql.append(where);
			
			logger.finest(sql.toString());
			try(PsqlNamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
				ps.executeUpdate();
			}

		}
		return filterToTableName;
		
	}
}
