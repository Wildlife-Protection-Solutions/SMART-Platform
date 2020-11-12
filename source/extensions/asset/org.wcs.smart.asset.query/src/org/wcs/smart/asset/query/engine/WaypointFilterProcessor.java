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
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter.Source;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class WaypointFilterProcessor extends org.wcs.smart.observation.query.engine.WaypointFilterProcessor {

	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public WaypointFilterProcessor(String tableName, AbstractQueryEngine engine, Query query){
		super(tableName, engine, query);
	}
	
	@Override
	protected DerbyFilterToSqlGenerator getSqlGenerator() {
		return AssetFilterSqlGenerator.INSTANCE;
	}
	
	
	@Override
	protected void createWaypointTable(Connection c,  
			DateFilter dateFilter, ConservationAreaFilter caFilter, IProgressMonitor monitor)
			throws SQLException {
		
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		progress.subTask(Messages.WaypointFilterProcessor_TableTaskName);
		
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + waypointTable + " (wp_uuid char(16) for bit data)"); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + waypointTable + "_wpuuid_idx on " + waypointTable + " (wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		engine.clearParameters();
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(waypointTable);
		sql.append("(wp_uuid) SELECT "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class));
		sql.append(".uuid "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$
		sql.append(namePrefix(Waypoint.class));
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class) + ".source = '" + AssetWaypointSource.KEY + "' "); //$NON-NLS-1$ //$NON-NLS-2$
		
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

		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	@Override
	protected boolean columnRequired(IFilter filter) {
		if (super.columnRequired(filter)) return true;
		return (filter instanceof AssetFilter || filter instanceof AssetAttributeFilter);
	}
	
	@Override
	protected void processFilter(IFilter filter,  ConservationAreaFilter caFilter, FilterTable table, Connection c) throws SQLException {
		
		if (filter instanceof AssetAttributeFilter) {
			processAssetAttributeFilter((AssetAttributeFilter)filter, table, c);
		}else if (filter instanceof AssetFilter) {
			processAssetFilter((AssetFilter)filter, table, c);
		}else {
			super.processFilter(filter, caFilter, table, c);
		}
	}
	
	private void processAssetFilter(AssetFilter assetFilter, FilterTable table, Connection c) throws SQLException {
		engine.clearParameters();
		createTemporaryFilterTable(table, c);
		
		//create temporary table for attribute observations
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(table.tablename + " (" + table.columnname + ")"); //$NON-NLS-1$ //$NON-NLS-2$	
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append("a.wp_uuid "); //$NON-NLS-1$
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
		
		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	private void processAssetAttributeFilter(AssetAttributeFilter assetFilter, FilterTable table, Connection c) throws SQLException {
		engine.clearParameters();
		createTemporaryFilterTable(table, c);

		//create temporary table for attribute observations
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(table.tablename + " (" + table.columnname + ")"); //$NON-NLS-1$ //$NON-NLS-2$	
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append("a.wp_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(waypointTable + " a"); //$NON-NLS-1$
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetWaypoint.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append("a.wp_uuid = " + prefix(AssetWaypoint.class) + ".wp_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetDeployment.class));
		sql.append(" ON " ); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class) + ".uuid = " + prefix(AssetWaypoint.class) + ".asset_deployment_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
		StringBuilder where = new StringBuilder();
		
		Class<?> valueClass = null;
		if (assetFilter.getSource() == Source.ASSET) {
			valueClass = AssetAttributeValue.class;
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(Asset.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".asset_uuid = " + prefix(Asset.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(Asset.class) + ".uuid = " + prefix(valueClass) + ".asset_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			
		}else if (assetFilter.getSource() == Source.STATION) {
			valueClass = AssetStationAttributeValue.class;
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStationLocation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStationLocation.class) + ".station_uuid = " + prefix(AssetStation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStation.class) + ".uuid = " + prefix(valueClass) + ".station_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			
			 
		}else if (assetFilter.getSource() == Source.STATIONLOCATION) {
			valueClass = AssetStationLocationAttributeValue.class;
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStationLocation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStationLocation.class) + ".uuid = " + prefix(valueClass) + ".station_location_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
		}else if (assetFilter.getSource() == Source.DEPLOYMENT) {
			valueClass = AssetDeploymentAttributeValue.class;
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".uuid = " + prefix(valueClass) + ".asset_deployment_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		}
			
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetAttribute.class));
		sql.append(" ON " ); //$NON-NLS-1$
		sql.append(prefix(valueClass) + ".attribute_uuid = " + prefix(AssetAttribute.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
		String key = engine.addParameterValue(assetFilter.getAttributeKey());
		sql.append(" AND " + prefix(AssetAttribute.class) + ".keyid = " + key);  //$NON-NLS-1$//$NON-NLS-2$
		
		String tprefix = prefix(valueClass);
		if (assetFilter.getAttributeType() == AttributeType.LIST) {
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetAttributeListItem.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(valueClass) + ".list_item_uuid = " + prefix(AssetAttributeListItem.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				
			tprefix = prefix(AssetAttributeListItem.class);
		}
		
		key = engine.addParameterValue(assetFilter.getAttributeKey());
		where.append(prefix(AssetAttribute.class) + ".keyid = " + key); //$NON-NLS-1$
			
		String q = ((AssetFilterSqlGenerator)getSqlGenerator()).toSql(assetFilter, tprefix, engine);
		where.append(" AND "); //$NON-NLS-1$
		where.append(q);
		
		sql.append (" WHERE "); //$NON-NLS-1$
		sql.append(where);

		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
		
}
