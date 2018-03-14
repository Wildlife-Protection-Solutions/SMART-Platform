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
package org.wcs.smart.asset.report.map;

import java.util.Collections;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.wcs.smart.asset.report.table.LocationTable;
import org.wcs.smart.asset.report.table.StationTable;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

/**
 * Map Layers for asset station and asset station location BIRT tables.
 * 
 * @author Emily
 *
 */
public class StationLocationTableMapLayer implements IBirtMapLayerManager {

	public StationLocationTableMapLayer() {
	}

	
	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(SmartTableQuery.SMART_DATASET_TYPE)) {
			if (odaHandle.getQueryText().startsWith("asset:assetstationlocation") || //$NON-NLS-1$
					odaHandle.getQueryText().startsWith("asset:assetstation") ){ //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}



	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle)
			throws Exception {
		if (handle instanceof OdaDataSetHandle){
			String geomColumn = null;
			if (((OdaDataSetHandle)handle).getQueryText().startsWith("asset:assetstationlocation")){ //$NON-NLS-1$
				geomColumn = LocationTable.COLUMN_PREFIX + LocationTable.Column.POSITION.name().toLowerCase();				
			}else if (((OdaDataSetHandle)handle).getQueryText().startsWith("asset:assetstation")){ //$NON-NLS-1$
				geomColumn = StationTable.COLUMN_PREFIX + StationTable.Column.POSITION.name().toLowerCase();
			}
			if (geomColumn == null) return null;
			return Collections.singletonList(new MapLayerInfo(null, null, LayerType.POINT, geomColumn));		
		}
		return null;
		
	}

}
