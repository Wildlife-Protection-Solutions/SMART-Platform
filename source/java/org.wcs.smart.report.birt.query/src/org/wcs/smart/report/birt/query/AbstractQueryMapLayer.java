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
package org.wcs.smart.report.birt.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.OdaResultSetColumn;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.SmartQuery;
import org.wcs.smart.query.model.IGeometryColumn;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;

/**
 * Abstract map layer for linking a SMART query to a BIRT map layer.  This
 * implements default behaviour for common SMART queries.
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryMapLayer implements IBirtMapLayerManager {
	
	/**
	 * Return true if the given query type had layers that can 
	 * be added to the map.
	 * 
	 * @param queryTypeKey
	 * @return
	 */
	public abstract boolean canAddToMap(String queryTypeKey);
	
	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)) {
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(AbstractSmartBirtQuery.SMART_DATASET_TYPE)) {
			String queryText = odaHandle.getQueryText();
			String queryTypeKey = queryText.split(":")[0]; //$NON-NLS-1$
			if (canAddToMap(queryTypeKey)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Add additional map layers that are represented by a query column
	 * (for raster queries)
	 * 
	 * @param queryTypeKey
	 * @return a list or empty list, never null
	 */
	public List<MapLayerInfo> getGeometryOptions(String queryTypeKey){
		return Collections.emptyList();
	}
	
	/**
	 * Computes the possible geometry columns based on the results
	 * set metadata.
	 */
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle) throws Exception{
		
		List<MapLayerInfo> maplayers = new ArrayList<>();
		
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(SmartQuery.SMART_DATASET_TYPE)) {
			if (canAddToMap(odaHandle)) {
				
				String queryTypeKey = odaHandle.getQueryText().split(":")[0]; //$NON-NLS-1$
				maplayers.addAll(getGeometryOptions(queryTypeKey));
				
				HashMap<String,String> names = new HashMap<>();
				List<ColumnHint> hints = odaHandle.getListProperty("columnHints");
				if (hints != null) {
					for (ColumnHint h : hints) {
						String column = h.getStringProperty(odaHandle.getModule(), "columnName");
						String display = h.getStringProperty(odaHandle.getModule(), "displayName");
						names.put(column, display);
					}
				}
				
				List<OdaResultSetColumn> items = (List<OdaResultSetColumn>)odaHandle.getListProperty("resultSet");
				for (OdaResultSetColumn c : items) {
					
					MapLayerInfo.LayerType type = null;
						
					if (c.getNativeDataType() == IGeometryColumn.Type.POINT.birtDataType) {
						type = MapLayerInfo.LayerType.POINT;
					}else if (c.getNativeDataType() == IGeometryColumn.Type.LINESTRING.birtDataType) { 
						type = MapLayerInfo.LayerType.LINE;
					}else if (c.getNativeDataType() == IGeometryColumn.Type.MULTILINESTRING.birtDataType) {
						type = MapLayerInfo.LayerType.MULTILINE;
					}else if (c.getNativeDataType() == IGeometryColumn.Type.POLYGON.birtDataType) {
						type = MapLayerInfo.LayerType.POLYGON;
					}else if (c.getNativeDataType() == IGeometryColumn.Type.MULTIPOLYGON.birtDataType) {
						type = MapLayerInfo.LayerType.MULTIPOLYGON;
					}
					if (type != null) {
						String name = names.get(c.getColumnName());
						if (c == null) name = c.getColumnName();
						MapLayerInfo cc = new MapLayerInfo(name, null, type, c.getColumnName());
						maplayers.add(cc);
					}					
				}
			}
		}
		return maplayers;
	}
	

}
