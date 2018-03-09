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
package org.wcs.smart.i2.birt.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.PointSymbolizer;
import org.opengis.style.PolygonSymbolizer;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.Rule;
import org.opengis.style.Style;
import org.opengis.style.Symbolizer;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.report.birt.map.IBirtLayerStyleProvider;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Map layer for intelligence query results
 * 
 * @author Emily
 *
 */
public class QueryMapLayer implements IBirtMapLayerManager, IBirtLayerStyleProvider { 

	//SLD Layer style 
	private static final String SLDID = "org.locationtech.udig.style.sld"; //$NON-NLS-1$
	
	public QueryMapLayer() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)) {
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(IntelQueryDataset.DATASET_TYPE)) {
			String queryText = odaHandle.getQueryText();
			String[] bits = queryText.split(IntelQueryDataset.QUERY_DEF_SEP);
			String queryType = bits[0];
			if (queryType.equals(IntelRecordObservationQuery.KEY)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(DataSetHandle handle)
			throws Exception {
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(IntelQueryDataset.DATASET_TYPE)) {
			List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
			
			layers.add( new MapLayerInfo(null, null, LayerType.POINT, FixedQueryColumn.Column.LOC_GEOMTRY.key) );
			layers.add( new MapLayerInfo(null, null, LayerType.POLYGON, FixedQueryColumn.Column.LOC_GEOMTRY.key) );
			
			return layers;
		}
		return null;
	}
	
	@Override
	public StyleBlackboard getStyle(String extensionId, String queryText, MapLayerInfo.LayerType layerType, Session s) {
		if (!extensionId.equals(IntelQueryDataset.DATASET_TYPE)) return null;
	
		UUID uuid = null;
		try {
			uuid = UuidUtils.stringToUuid( queryText.split(IntelQueryDataset.QUERY_DEF_SEP) [1]);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if (uuid == null) return null;
		
		IntelRecordObservationQuery q = s.get(IntelRecordObservationQuery.class, uuid);
		String styleQuery = q.getStyle();
		if (styleQuery == null) return null;
		
		Map<String, StyleBlackboard> styles = null;
		
		try {	
			styles = StyleManager.INSTANCE.fromStringMap(styleQuery);
		}catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		if (styles == null) return null;
		
		//search for style with correct symbolizer
		for (StyleBlackboard sb : styles.values()) {
			Style ss = (Style) sb.get(SLDID);
			for (FeatureTypeStyle fts: ss.featureTypeStyles()) {
				for (Rule r : fts.rules()) {
					for (Symbolizer sym : r.symbolizers()) {
						if (sym instanceof PointSymbolizer && layerType == MapLayerInfo.LayerType.POINT) return sb;
						if (sym instanceof PointSymbolizer && layerType == MapLayerInfo.LayerType.MULTIPOINT) return sb;
						
						if (sym instanceof LineSymbolizer && layerType == MapLayerInfo.LayerType.LINE) return sb;
						if (sym instanceof LineSymbolizer && layerType == MapLayerInfo.LayerType.MULTILINE) return sb;
						
						if (sym instanceof PolygonSymbolizer && layerType == MapLayerInfo.LayerType.POLYGON) return sb;
						if (sym instanceof PolygonSymbolizer && layerType == MapLayerInfo.LayerType.MULTIPOLYGON) return sb;
						
						if (sym instanceof RasterSymbolizer && layerType == MapLayerInfo.LayerType.RASTER) return sb;
					}
				}
			}
		}
		return null;
	}


}
