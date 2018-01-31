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

import org.wcs.smart.asset.IAssetLabelProvider;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.connect.i18n.Messages;

/**
 * Asset label provider.
 * 
 * @author Emily
 *
 */
public class AssetLabelProvider implements IAssetLabelProvider {

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
		
		return null;
	}

}
