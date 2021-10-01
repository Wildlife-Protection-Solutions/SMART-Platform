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
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
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
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter.Source;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class FilterProcessor extends org.wcs.smart.observation.query.engine.FilterProcessor{

	
	private Map<IFilter, FilterTable> assetFilterTables;
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public FilterProcessor(String tableName, AssetQueryEngine engine, Query query){
		super(tableName, engine, query);
	}

	@Override
	protected DerbyFilterToSqlGenerator getSqlGenerator() {
		return AssetFilterSqlGenerator.INSTANCE;
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
			DateFilter dateFilter, ConservationAreaFilter caFilter, 
			boolean populateObservation,
			boolean includeEmptyObservations,
			IProgressMonitor monitor) throws SQLException{
		
		SubMonitor progress = SubMonitor.convert(monitor, 4);
		progress.subTask(Messages.DerbySummaryEngine_Progress_CreatingObservationTable);		
		
		IFilter qFilter = queryFilter;
		if (qFilter == null) qFilter = EmptyFilter.INSTANCE;
		
		assetFilterTables = processAssetTables(c, qFilter, dateFilter, caFilter, progress.split(1));
		super.processFilter(c, queryFilter, dateFilter, caFilter, populateObservation, includeEmptyObservations, monitor);
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
	@Override
	protected void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c,
			boolean populateObservation)
			throws SQLException {
		

		AssetFilterCollector assetVisitor = new AssetFilterCollector();
		assetVisitor.visit(queryFilter);
		
		StringBuilder sql = new StringBuilder();
		
		engine.clearParameters();
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));

		HashSet<Class<?>> usedTables = new HashSet<Class<?>>();
		
		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$

		usedTables.add(Waypoint.class);
		sql.append(namePrefix(Waypoint.class));
		
		for (FilterTable t : assetFilterTables.values()) {
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(t.tablename);
			sql.append(" on " + t.tablename + "." + t.primarykey + " = " + prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		
		engine.filterTables.putAll(assetFilterTables);
		
		if (populateObservation || 
				observationFilterVisitor.hasAttributeFilter() || 
				observationFilterVisitor.hasCategoryFilter()){
		
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservationGroup.class));
			usedTables.add(WaypointObservationGroup.class);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class));
			sql.append(".wp_uuid "); //$NON-NLS-1$
			
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			usedTables.add(WaypointObservation.class);
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
			usedTables.add(Category.class);
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
			String filter = getSqlGenerator().toSql(caFilter, engine);
			if (filter.length() > 0) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (dateFilter != null) {
			String filter = getSqlGenerator().toSql(dateFilter, engine);
			if (filter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(filter);
			}
		}
		
		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = getSqlGenerator().toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" and ( "); //$NON-NLS-1$
			    sql.append(filter);
			    sql.append( " ) "); //$NON-NLS-1$
			}
		}
		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	@Override
	protected Collection<IWaypointSource> getWaypointSources(){
		return Collections.singletonList( WaypointSourceEngine.INSTANCE.getSource(AssetWaypointSource.KEY) );
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
			DateFilter dateFilter, ConservationAreaFilter caFilter, IProgressMonitor monitor)
			throws SQLException {
		
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		progress.subTask(Messages.FilterProcessor_FiltersTaskName);
		
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
			sql.append("CREATE TABLE " + assetTable + " (wp_uuid char(16) for bit data) "); //$NON-NLS-1$ //$NON-NLS-2$
		
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());

			// -- create index
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + assetTable + "_wpuuid_idx on " + assetTable + " (wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql.toString());
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
				String cfilter = getSqlGenerator().toSql(caFilter, engine);
				if (cfilter.length() > 0) {
					sql.append(" and "); //$NON-NLS-1$
					sql.append(cfilter);
				}
			}
				
			if (dateFilter != null) {
				String dfilter = getSqlGenerator().toSql(dateFilter, engine);
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
			
			if (part instanceof AssetFilter ) {
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
				sql.append (" WHERE "); //$NON-NLS-1$
				sql.append(where);
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
				String q = ((AssetFilterSqlGenerator)getSqlGenerator()).toSql(aFilter, tprefix, engine);
				where.append(q);
				
				
				sql.append (" WHERE "); //$NON-NLS-1$
				sql.append(where);
			}
			
			QueryPlugIn.logSql(sql.toString());
			try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
				ps.executeUpdate();
			}

		}
		return filterToTableName;
		
	}
}

