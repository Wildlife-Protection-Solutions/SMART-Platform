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

import java.text.MessageFormat;
import java.util.Locale;

import org.wcs.smart.asset.IAssetLabelProvider;
import org.wcs.smart.asset.data.importer.DuplicateFileWarning;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.asset.model.mapping.ExifMetadataField;
import org.wcs.smart.connect.i18n.Messages;

/**
 * Asset label provider.
 * 
 * @author Emily
 *
 */
public class AssetLabelProvider implements IAssetLabelProvider {

	@Override
	public String formatTime(double timeInSeconds, Locale l) {
		int days = (int) Math.floor( timeInSeconds / 86_400.0 );
		
		double remainder = timeInSeconds - days * 86_400.0;
		double hours = remainder / 3_600.0;
			
		if (timeInSeconds == 0) return Messages.getString("AssetDeploymentSummaryEngine.zeroDaysFormat", l); //$NON-NLS-1$
		return MessageFormat.format(Messages.getString("AssetDeploymentSummaryEngine.DaysHoursFormat", l), days, hours); //$NON-NLS-1$
	}
	
	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item instanceof AssetAttribute.AttributeType) {
			switch( ((AssetAttribute.AttributeType)item) ) {
				case BOOLEAN: return Messages.getString("AssetLabelProvider.BooleanAttName", l); //$NON-NLS-1$
				case DATE: return Messages.getString("AssetLabelProvider.DateAttName", l); //$NON-NLS-1$
				case LIST: return Messages.getString("AssetLabelProvider.ListAttName", l); //$NON-NLS-1$
				case NUMERIC: return Messages.getString("AssetLabelProvider.NumberAttName", l); //$NON-NLS-1$
				case POSITION: return Messages.getString("AssetLabelProvider.PositionAttName", l); //$NON-NLS-1$
				case TEXT: return Messages.getString("AssetLabelProvider.TextAttName", l);			 //$NON-NLS-1$
			}
		}
		
		if (item instanceof AssetWaypointSource) return Messages.getString("AssetLabelProvider.AssetWpSourceName", l); //$NON-NLS-1$
		
		if (item == ASSET_TABLE_NAME) return Messages.getString("AssetLabelProvider.AssetTableName", l); //$NON-NLS-1$
		if (item == STATION_TABLE_NAME) return Messages.getString("AssetLabelProvider.StationsTableName", l); //$NON-NLS-1$
		if (item == STATIONLOCATION_TABLE_NAME) return Messages.getString("AssetLabelProvider.LocationsTableName", l); //$NON-NLS-1$
		
		if (item == ID_COL_NAME) return Messages.getString("AssetLabelProvider.IdColumnName", l); //$NON-NLS-1$
		if (item == ASSET_TYPE_COL_NAME) return Messages.getString("AssetLabelProvider.TypeColumnName", l); //$NON-NLS-1$
		if (item == STATUS_COL_NAME) return Messages.getString("AssetLabelProvider.StatusColumnName", l); //$NON-NLS-1$
		if (item == STATUSKEY_COL_NAME) return Messages.getString("AssetLabelProvider.StatusKeyColumnName", l); //$NON-NLS-1$
		if (item == ASSET_TYPEKEY_COL_NAME) return Messages.getString("AssetLabelProvider.TypeKeyColumName", l); //$NON-NLS-1$
		if (item == POSITION_COL_NAME) return Messages.getString("AssetLabelProvider.PositionColumnName", l); //$NON-NLS-1$
		
		
		if (item instanceof FileProcessor.ErrorMessages) {
			switch((FileProcessor.ErrorMessages)item) {
				case ASSET_NOT_FOUND: return "No sensor found with ID ''{0}''";
				case BOOLEAN_PARSE_ERROR: return "Could not parse boolean value from {0} for mapping to boolean attribute {1}";
				case BOOLEAN_TAG_PARSE_ERROR: return "Could not parse boolean value from the tag {0} for mapping to boolean attribute {1}.";
				case CATEGORY_NOT_FOUND: return "No data model category found for mapping of attribute {0} with value: {1}.";
				case DATE_PARSE_ERROR: return "Could not parse date from the value {0} for mapping to date attribute {1}. Date format unsupported.";
				case DATE_TAG_PARSE_ERROR: return "Could not parse date from the tag {0} for mapping to date attribute {1}";
				case LIST_ITEM_PARSE_ERROR: return "Could not parse attribute list item from the value {0} for mapping to attribute {1}. No list item with this value found.";
				case LIST_ITEM_TAG_PARSE_ERROR: return "Could not parse the tag {0} for mapping to list attribute {1}. No matching list item found.";
				case METADATA_PARSE: return "Could not parse metadata from file: {0}";
				case MULTIPLE_DEPLOYMENTS: return "Multiple deployments at the same time for sensor {0}. This state is not valid, cannot continue.";
				case NUMBER_PARSE_ERROR: return "Could not parse number from the value {0} for mapping to attribute {1}";
				case NUMBER_TAG_PARSE_ERROR: return "Could not parse number from the tag {0} for mapping to number attribute {1}";
				case STATION_LOCATION_NOT_FOUND: return "No unit location found with id ''{0}''";
				case STATION_NOT_FOUND: return "No station found for id ''{0}''";
				case STATION_OVERWRITE: return "Unit location {1} is not associated with the station {0} found for the file. The unit location will take precedence and the station overwritten.";
				case TREE_NODE_PARSE_ERROR: return "Could not parse attribute tree node from the value {0} for mapping to attribute {1}. No tree node with this value found.";				
				case TREE_NODE_TAG_PARSE_ERROR: return "Could parse the tag {0} for mapping to tree attribute {1}. No matching tree node.";
			}
		}
		
		if (item instanceof FileProxy.ErrorMessage) {
			switch((FileProxy.ErrorMessage)item) {
				case ASSET_NOT_FOUND: return "No field sensor found";
				case DATE_NOT_FOUND: return "No date found";
				case LOCATION_NOT_FOUND: return "=No unit location found";
				case POSITION_NOT_FOUND: return "No position found";
				case STATION_NOT_FOUND: return "No station found";
				case NEW_STATION: return "** NEW STATION {0} **";
				case NEW_STATION_LOCATION: return "** NEW LOCATION {0} **";
				
			}
		}
		
		if (item instanceof Asset.Status) {
			switch((Asset.Status)item) {
				case ACTIVE: return "Active";
				case INACTIVE: return "Active";
				case RETIRED: return "Retired";
			}
		}
		
		if (item == ExifMetadataField.TAG_FORMAT_1) return "Tag: {0}";
		if (item == ExifMetadataField.TAG_FORMAT_2) return "Tag: {0} ({1})";
		if (item.equals(DuplicateFileWarning.class)) return "Potential duplicate data. A file with the name {0} has already been loaded into the Conservation Area.";
		
		return null;
	}

}
