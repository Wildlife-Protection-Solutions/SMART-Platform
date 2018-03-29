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
package org.wcs.smart.asset;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.asset.model.mapping.ExifMetadataField;

/**
 * Label provider for core asset items.
 * 
 * @author Emily
 *
 */
public class AssetCoreLabelProvider implements IAssetLabelProvider {

	public static Image getStatusImage(Asset asset) {
		return getStatusImage(asset.getCachedStatus());
	}
	
	public static Image getStatusImage(Asset.Status status) {
		switch(status) {
		case ACTIVE:
			return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATUS_ACTIVE);
		case INACTIVE:
			return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATUS_INACTIVE);
		case RETIRED:
			return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATUS_RETIRED);
		}
		return null;
	}
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof AssetAttribute.AttributeType) {
			switch( ((AssetAttribute.AttributeType)item) ) {
			case BOOLEAN: return Messages.AssetCoreLabelProvider_BooleanAttributeType;
			case DATE: return Messages.AssetCoreLabelProvider_DateAttributeType;
			case LIST: return Messages.AssetCoreLabelProvider_ListAttributeType;
			case NUMERIC: return Messages.AssetCoreLabelProvider_NumberAttributeType;
			case POSITION: return Messages.AssetCoreLabelProvider_PositionAttributeType;
			case TEXT: return Messages.AssetCoreLabelProvider_TextAttributeType;			
			}
		}
		if (item instanceof AssetWaypointSource) {
			return Messages.AssetCoreLabelProvider_AssetSource;
		}
		if (item == ASSET_TABLE_NAME) return Messages.AssetCoreLabelProvider_AssetsTable;
		if (item == STATION_TABLE_NAME) return Messages.AssetCoreLabelProvider_StationsTable;
		if (item == STATIONLOCATION_TABLE_NAME) return Messages.AssetCoreLabelProvider_StationLocationTable;
		
		
		if (item == ID_COL_NAME) return Messages.AssetCoreLabelProvider_IdColumnName;
		if (item == ASSET_TYPE_COL_NAME) return Messages.AssetCoreLabelProvider_TypeColumnName;
		if (item == STATUS_COL_NAME) return Messages.AssetCoreLabelProvider_StatusColumnName;
		if (item == STATUSKEY_COL_NAME) return Messages.AssetCoreLabelProvider_StatusKeyColumnName;
		if (item == ASSET_TYPEKEY_COL_NAME) return Messages.AssetCoreLabelProvider_TypeKeyColumnName;
		if (item == POSITION_COL_NAME) return Messages.AssetCoreLabelProvider_PositionColumnName;
		
		if (item instanceof FileProcessor.ErrorMessages) {
			switch((FileProcessor.ErrorMessages)item) {
				case ASSET_NOT_FOUND: return Messages.AssetCoreLabelProvider_AssetNotFound;
				case BOOLEAN_PARSE_ERROR: return Messages.AssetCoreLabelProvider_BooleanParseError1;
				case BOOLEAN_TAG_PARSE_ERROR: return Messages.AssetCoreLabelProvider_BooleanParseError2;
				case CATEGORY_NOT_FOUND: return Messages.AssetCoreLabelProvider_CategoryNotFound;
				case DATE_PARSE_ERROR: return Messages.AssetCoreLabelProvider_DateParseError1;
				case DATE_TAG_PARSE_ERROR: return Messages.AssetCoreLabelProvider_DateParseError2;
				case LIST_ITEM_PARSE_ERROR: return Messages.AssetCoreLabelProvider_ListItemParseError1;
				case LIST_ITEM_TAG_PARSE_ERROR: return Messages.AssetCoreLabelProvider_ListItemParseError2;
				case METADATA_PARSE: return Messages.AssetCoreLabelProvider_MetadataParseError;
				case MULTIPLE_DEPLOYMENTS: return Messages.AssetCoreLabelProvider_MultiDeployments;
				case NUMBER_PARSE_ERROR: return Messages.AssetCoreLabelProvider_NumberParseError1;
				case NUMBER_TAG_PARSE_ERROR: return Messages.AssetCoreLabelProvider_NumberParseError2;
				case STATION_LOCATION_NOT_FOUND: return Messages.AssetCoreLabelProvider_LocationNotFound;
				case STATION_NOT_FOUND: return Messages.AssetCoreLabelProvider_StationNotFound;
				case STATION_OVERWRITE: return Messages.AssetCoreLabelProvider_LocationDoesNotMatchStation;
				case TREE_NODE_PARSE_ERROR: return Messages.AssetCoreLabelProvider_TreeNodeParseError;				
				case TREE_NODE_TAG_PARSE_ERROR: return Messages.AssetCoreLabelProvider_TreeNodeParseError2;
			}
		}
		
		if (item instanceof FileProxy.ErrorMessage) {
			switch((FileProxy.ErrorMessage)item) {
				case ASSET_NOT_FOUND: return Messages.AssetCoreLabelProvider_NoAsset;
				case DATE_NOT_FOUND: return Messages.AssetCoreLabelProvider_NoDate;
				case LOCATION_NOT_FOUND: return Messages.AssetCoreLabelProvider_NoStationLocation;
				case POSITION_NOT_FOUND: return Messages.AssetCoreLabelProvider_NoPosition;
				case STATION_NOT_FOUND: return Messages.AssetCoreLabelProvider_NoStation;
				case NEW_STATION: return Messages.AssetCoreLabelProvider_NewStation;
				case NEW_STATION_LOCATION: return Messages.AssetCoreLabelProvider_NewLocation;
				
			}
		}
		
		if (item instanceof Asset.Status) {
			switch((Asset.Status)item) {
				case ACTIVE: return Messages.AssetCoreLabelProvider_AssetActive;
				case INACTIVE: return Messages.AssetCoreLabelProvider_AssetInActive;
				case RETIRED: return Messages.AssetCoreLabelProvider_AssetRetired;
			}
		}
		
		if (item == ExifMetadataField.TAG_FORMAT_1) return Messages.AssetCoreLabelProvider_TagFormat0;
		if (item == ExifMetadataField.TAG_FORMAT_2) return Messages.AssetCoreLabelProvider_TagFormat1;
		return null;
	}
}
