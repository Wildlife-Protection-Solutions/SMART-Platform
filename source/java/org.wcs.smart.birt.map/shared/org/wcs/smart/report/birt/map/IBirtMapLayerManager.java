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
package org.wcs.smart.report.birt.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.data.engine.odaconsumer.Connection;
import org.eclipse.birt.data.engine.odaconsumer.ConnectionManager;
import org.eclipse.birt.data.engine.odaconsumer.PreparedStatement;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.OdaResultSetColumn;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

/**
 * This provides a presentation link between any BIRT datasethandle
 * and a BIRT map.  Implementations should not store any state in these
 * objects.  The objects are resued for multiple reports.
 * 
 * @author Emily
 *
 */
public interface IBirtMapLayerManager {
	
	/**
	 *  Determine if a dataset handle can be added to a map
	 *  
	 * @param handle
	 * @return
	 */
	public boolean canAddToMap(DataSetHandle handle);
	
	/**
	 * Returns the list of geometry columns that are supported.  Only the
	 * geometry column name and layer type must be provided.  
	 * 
	 * @param handle
	 * @param context may be null if no context set
	 * @return
	 * @throws Exception
	 */
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle) throws Exception;

	
	/**
	 * Looks at the columns associated with a dataset handle and find ones
	 * with a geometry column type
	 * 
	 * @param odaHandle
	 * @return
	 */
	public default List<MapLayerInfo> findGeometryColumnsInResultSet(OdaDataSetHandle odaHandle){
		return geometryColumnsInResultSet(odaHandle);
		
	}
	
	public static List<MapLayerInfo> geometryColumnsInResultSet(OdaDataSetHandle odaHandle){
		List<MapLayerInfo> maplayers = new ArrayList<>();
		
		HashMap<String,String> names = new HashMap<>();
		List<ColumnHint> hints = odaHandle.getListProperty("columnHints"); //$NON-NLS-1$
		if (hints != null) {
			for (ColumnHint h : hints) {
				String column = h.getStringProperty(odaHandle.getModule(), "columnName"); //$NON-NLS-1$
				String display = h.getStringProperty(odaHandle.getModule(), "displayName"); //$NON-NLS-1$
				names.put(column, display);
			}
		}
				
		List<OdaResultSetColumn> items = (List<OdaResultSetColumn>)odaHandle.getListProperty("resultSet"); //$NON-NLS-1$
		
		boolean requiresMetadata = false;
		for (OdaResultSetColumn c : items) {
			if (c.getNativeDataType() == null) {
				//native data types do not exist; this is a problem as we use these
				//to determine geometry columns
				//this happens when your right click and refresh a dataset
				//not sure why this happens but in this case we 
				//cannot search for the geometry columns in the dataset using the nativedatatype
				//need to look at the computed metadata
				requiresMetadata = true;
				break;
			}
		}
		
		if (requiresMetadata) {

			try {
				
				Map<?,?> appContext = new HashMap<>();
				java.util.Properties connProps = new Properties();
				
				Connection connection = ConnectionManager.getInstance().openConnection(
						 odaHandle.getDataSource().getStringProperty("extensionID"), connProps, appContext);  //$NON-NLS-1$
				try {
					PreparedStatement ps = connection.prepareStatement(odaHandle.getQueryText(),
							odaHandle.getExtensionID());
					try {
						IResultClass md = ps.getResultSet().getMetaData();
						for (int i = 1; i <= md.getFieldCount(); i ++) {
							String name = md.getFieldLabel(i);
							if (name == null) name = md.getFieldName(i);
							LayerType type = null;
							String nativeName = md.getFieldNativeTypeName(i);
							
							for (LayerType lt : LayerType.values()) {
								if (lt.getOdaType().equalsIgnoreCase(nativeName)) {
									type = lt;
									break;
								}
							}
							if (type != null) {
								MapLayerInfo cc = new MapLayerInfo(name, null, type, md.getFieldName(i));
								maplayers.add(cc);
							}
						}
					}finally {
						ps.close();
					}
				}finally {
					connection.close();
				}
			}catch (Exception ex) {
				Logger.getLogger(IBirtMapLayerManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
			}
			return maplayers;
		}else {
			for (OdaResultSetColumn c : items) {
				
				MapLayerInfo.LayerType type = null;
				if (c.getNativeDataType() == null) continue;
				
				if (c.getNativeDataType() == IGeometryColumn.Type.POINT.birtDataType) {
					type = MapLayerInfo.LayerType.POINT;
				}else if (c.getNativeDataType() == IGeometryColumn.Type.MULTIPOINT.birtDataType) {
					type = MapLayerInfo.LayerType.MULTIPOINT;
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
					if (name == null) name = c.getColumnName();
					MapLayerInfo cc = new MapLayerInfo(name, null, type, c.getColumnName());
					maplayers.add(cc);
				}					
			}
			return maplayers;
		}
	}

}
