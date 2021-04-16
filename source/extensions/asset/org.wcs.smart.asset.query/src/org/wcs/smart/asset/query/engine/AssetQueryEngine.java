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
import java.sql.SQLException;

import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetDeploymentDisruption;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.FilterType;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class AssetQueryEngine extends AbstractQueryEngine implements IAssetQueryEngine{
	
	static {
		tablePrefix.put(AssetWaypoint.class, "aw"); //$NON-NLS-1$
		tablePrefix.put(AssetDeployment.class, "ad"); //$NON-NLS-1$
		tablePrefix.put(AssetDeploymentDisruption.class, "adda"); //$NON-NLS-1$
		tablePrefix.put(Asset.class, "aa"); //$NON-NLS-1$
		tablePrefix.put(AssetStation.class, "astn"); //$NON-NLS-1$
		tablePrefix.put(AssetStationLocation.class, "asl"); //$NON-NLS-1$
		
		tablePrefix.put(AssetDeploymentAttributeValue.class, "adav"); //$NON-NLS-1$\
		tablePrefix.put(AssetAttributeValue.class, "aav"); //$NON-NLS-1$\
		tablePrefix.put(AssetStationAttributeValue.class, "asav"); //$NON-NLS-1$\
		tablePrefix.put(AssetStationLocationAttributeValue.class, "aslav"); //$NON-NLS-1$\
		tablePrefix.put(AssetAttribute.class, "aat"); //$NON-NLS-1$
		tablePrefix.put(AssetAttributeListItem.class, "aal"); //$NON-NLS-1$
	}
	
	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(AssetWaypoint.class, "smart.asset_waypoint"); //$NON-NLS-1$
		tableNames.put(AssetDeployment.class, "smart.asset_deployment"); //$NON-NLS-1$
		tableNames.put(Asset.class, "smart.asset"); //$NON-NLS-1$
		tableNames.put(AssetStation.class, "smart.asset_station"); //$NON-NLS-1$
		tableNames.put(AssetStationLocation.class, "smart.asset_station_location"); //$NON-NLS-1$\
		tableNames.put(AssetDeploymentDisruption.class, "smart.asset_deployment_disruption"); //$NON-NLS-1$
		
		tableNames.put(AssetDeploymentAttributeValue.class, "smart.asset_deployment_attribute_value"); //$NON-NLS-1$\
		tableNames.put(AssetAttributeValue.class, "smart.asset_attribute_value"); //$NON-NLS-1$\
		tableNames.put(AssetStationAttributeValue.class, "smart.asset_station_attribute_value"); //$NON-NLS-1$\
		tableNames.put(AssetStationLocationAttributeValue.class, "smart.asset_station_location_attribute_value"); //$NON-NLS-1$\
		tableNames.put(AssetAttribute.class, "smart.asset_attribute"); //$NON-NLS-1$
		tableNames.put(AssetAttributeListItem.class, "smart.asset_attribute_list_item"); //$NON-NLS-1$
	}

	

	
	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	public IFilterProcessor getFilterProcessor(FilterType filterType, String queryDataTable, Query query){
		if (filterType == FilterType.OBSERVATION){
			return new FilterProcessor(queryDataTable, this, query);
		}else if (filterType == FilterType.GROUP) {
			return new WaypointGroupFilterProcessor(queryDataTable, this, query);
		}else{
			return new WaypointFilterProcessor(queryDataTable, this, query);
		}
	}
	
	/**
	 * Drop all tables created to support query result set.
	 * @param c
	 * @throws SQLException
	 */
	public abstract void dropTables(Connection c) throws SQLException;

}
