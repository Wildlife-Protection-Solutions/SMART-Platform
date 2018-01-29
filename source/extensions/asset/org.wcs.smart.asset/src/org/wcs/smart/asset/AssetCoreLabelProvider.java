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
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetWaypointSource;

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
			case BOOLEAN: return "BOOLEAN";
			case DATE: return "DATE";
			case LIST: return "LIST";
			case NUMERIC: return "NUMERIC";
			case POSITION: return "POSITION";
			case TEXT: return "TEXT";			
			}
		}
		if (item instanceof AssetWaypointSource) {
			return "Asset";
		}
		if (item == ASSET_TABLE_NAME) return "Assets";
		if (item == STATION_TABLE_NAME) return "Stations";
		if (item == STATIONLOCATION_TABLE_NAME) return "Station Locations";
		
		
		if (item == ID_COL_NAME) return "ID";
		if (item == ASSET_TYPE_COL_NAME) return "Type";
		if (item == STATUS_COL_NAME) return "Status";
		if (item == STATUSKEY_COL_NAME) return "Status Key";
		if (item == ASSET_TYPEKEY_COL_NAME) return "Type Key";
		if (item == POSITION_COL_NAME) return "Position";
		
		return null;
	}
}
