/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetFormatOption;
import org.wcs.smart.asset.query.model.AssetValueOption;
import org.wcs.smart.asset.query.model.PointGeometryQueryColumn;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.asset.query.parser.internal.filter.AssetDeploymentDateField;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;
import org.wcs.smart.connect.i18n.Messages;

/**
 * Asset query label provider
 * 
 * @author Emily
 *
 */
public class AssetQueryLabelProvider implements IQueryAssetLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof FixedQueryColumn.FixedColumns){
			switch((FixedQueryColumn.FixedColumns)item){
			case CA_ID: return Messages.getString("AssetQueryLabelProvider.CaIdColumnName", l); //$NON-NLS-1$
			case CA_NAME: return Messages.getString("AssetQueryLabelProvider.CaNameColumnName", l); //$NON-NLS-1$
			case WAYPOINT_ID: return Messages.getString("AssetQueryLabelProvider.WpIdColumnName", l); //$NON-NLS-1$
			case WAYPOINT_DATE: return Messages.getString("AssetQueryLabelProvider.WpDateColumnName", l); //$NON-NLS-1$
			case WAYPOINT_TIME: return Messages.getString("AssetQueryLabelProvider.WpTimeColumnName", l); //$NON-NLS-1$
			case WAYPOINT_X: return Messages.getString("AssetQueryLabelProvider.WpXColumnName", l); //$NON-NLS-1$
			case WAYPOINT_Y: return Messages.getString("AssetQueryLabelProvider.WpYColumnName", l); //$NON-NLS-1$
//			case WAYPOINT_DIRECTION: return Messages.getString("AssetQueryLabelProvider.WpDirColumnName", l); //$NON-NLS-1$
//			case WAYPOINT_DISTANCE: return Messages.getString("AssetQueryLabelProvider.WpDisColumnName", l); //$NON-NLS-1$
			case WAYPOINT_COMMENT: return Messages.getString("AssetQueryLabelProvider.WpCommentColumnName", l); //$NON-NLS-1$
			case ASSET: return Messages.getString("AssetQueryLabelProvider.AssetsColumnName", l); //$NON-NLS-1$
			case LOCATION: return Messages.getString("AssetQueryLabelProvider.LocationsColumnName", l); //$NON-NLS-1$
			case STATION: return Messages.getString("AssetQueryLabelProvider.StationsColumnName", l); //$NON-NLS-1$
			case INCIDENT_LENGTH: return Messages.getString("AssetQueryLabelProvider.IncidentLengthColumnName", l); //$NON-NLS-1$
			case WAYPOINT_LASTMODIFIED: return Messages.getString("AssetQueryLabelProvider.LastModifiedColumnName", l); //$NON-NLS-1$
			case WAYPOINT_LASTMODIFIEDBY: return Messages.getString("AssetQueryLabelProvider.LastModifiedByColumnName", l); //$NON-NLS-1$
			case OBS_GROUP_ID: return Messages.getString("AssetQueryLabelProvider.ObsGroupColumnName", l); //$NON-NLS-1$
			}
		}
		if (item instanceof AssetValueOption){
			switch((AssetValueOption)item){
			case ASSET_HOURS: return Messages.getString("AssetQueryLabelProvider.TotalAssetHoursLabel", l); //$NON-NLS-1$
			case ASSET_ACTIVEHOURS: return Messages.getString("AssetQueryLabelProvider.TotalActiveHours", l); //$NON-NLS-1$
			}
		}
		if (item instanceof AssetFormatOption){
			switch((AssetFormatOption)item){
			case DAYS_HOUR: return "days, hours";
			case SECOND: return "seconds";
			}
		}
		if (item instanceof AssetFilterOption){
			switch((AssetFilterOption)item){
			case STATION: return Messages.getString("AssetQueryLabelProvider.StationFilterOp", l); //$NON-NLS-1$
			case CONSERVATION_AREA: return Messages.getString("AssetQueryLabelProvider.CaFilteROp", l); //$NON-NLS-1$
			case ASSET: return Messages.getString("AssetQueryLabelProvider.AssetFilterOp", l); //$NON-NLS-1$
			case ASSETTYPE: return Messages.getString("AssetQueryLabelProvider.AssetTypeFilterOp", l); //$NON-NLS-1$
			case STATIONLOCATION: return Messages.getString("AssetQueryLabelProvider.StationLocationFilterOp", l); //$NON-NLS-1$
			}
		}
		if (item instanceof AssetDeploymentDateField) return Messages.getString("AssetQueryLabelProvider.DeploymentDateFilter", l); //$NON-NLS-1$

		if (item == PointGeometryQueryColumn.KEY) return "Waypoint";

		return null;
	}
}