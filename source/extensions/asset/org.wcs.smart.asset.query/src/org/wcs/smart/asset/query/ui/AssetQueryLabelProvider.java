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
package org.wcs.smart.asset.query.ui;

import java.util.Locale;

import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetFormatOption;
import org.wcs.smart.asset.query.model.AssetValueOption;
import org.wcs.smart.asset.query.model.PointGeometryQueryColumn;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.asset.query.parser.internal.filter.AssetDeploymentDateField;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;

/**
 * Implementation for asset query label provider.
 * 
 * @author Emily
 *
 */
public class AssetQueryLabelProvider implements IQueryAssetLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof FixedQueryColumn.FixedColumns){
			switch((FixedQueryColumn.FixedColumns)item){
			case CA_ID: return Messages.FixedQueryColumn_CaIdColumnName;
			case CA_NAME: return Messages.FixedQueryColumn_CaNameColumnName;
			case WAYPOINT_ID: return Messages.FixedQueryColumn_WaypointIdColumnName;
			case WAYPOINT_DATE: return Messages.FixedQueryColumn_WaypointDateColumnName;
			case WAYPOINT_TIME: return Messages.FixedQueryColumn_WaypointTimeColumnName;
			case WAYPOINT_X: return Messages.FixedQueryColumn_xColumnName;
			case WAYPOINT_Y: return Messages.FixedQueryColumn_yColumnName;
//			case WAYPOINT_DIRECTION: return Messages.FixedQueryColumn_DirectionColumnName;
//			case WAYPOINT_DISTANCE: return Messages.FixedQueryColumn_DistanceColumnName;
			case WAYPOINT_COMMENT: return Messages.FixedQueryColumn_CommentColumnName;
			case ASSET: return Messages.AssetQueryLabelProvider_AssetsColumnName;
			case LOCATION: return Messages.AssetQueryLabelProvider_LocationsColumnName;
			case STATION: return Messages.AssetQueryLabelProvider_StationColumnName;
			case INCIDENT_LENGTH: return Messages.AssetQueryLabelProvider_IncidentLengthColumnName;
			case WAYPOINT_LASTMODIFIED: return Messages.AssetQueryLabelProvider_LastModifiedColumnName;
			case WAYPOINT_LASTMODIFIEDBY: return Messages.AssetQueryLabelProvider_LastModifiedByColumnName;
			case OBS_GROUP_ID: return Messages.AssetQueryLabelProvider_ObsGroupColumnName;
			}
		}
		if (item instanceof AssetValueOption){
			switch((AssetValueOption)item){
			case ASSET_HOURS: return Messages.AssetQueryLabelProvider_TotalHoursColumnName;
			case ASSET_ACTIVEHOURS: return Messages.AssetQueryLabelProvider_TotalActiveHours;
			}
		}
		if (item instanceof AssetFormatOption){
			switch((AssetFormatOption)item){
			case DAYS_HOUR: return Messages.AssetQueryLabelProvider_DaysHoursFormat;
			case SECOND: return Messages.AssetQueryLabelProvider_SecondsFormat;
			}
		}
		if (item instanceof AssetFilterOption){
			switch((AssetFilterOption)item){
				case STATION: return Messages.AssetQueryOptions_QueryOpStation;
				case CONSERVATION_AREA: return Messages.AssetQueryOptions_CaGroupByOptionName;
			case ASSET: return Messages.AssetQueryLabelProvider_AssetFilterOption;
			case ASSETTYPE: return Messages.AssetQueryLabelProvider_AssetTypeFilterOption;
			case STATIONLOCATION: return Messages.AssetQueryLabelProvider_StationLocationFilterOptoin;
			}
		}
		
		if (item instanceof AssetDeploymentDateField) return Messages.AssetQueryLabelProvider_DeploymentDateFilter;

		if (item == PointGeometryQueryColumn.KEY) return "Waypoint";
		
		return null;
	}

}
