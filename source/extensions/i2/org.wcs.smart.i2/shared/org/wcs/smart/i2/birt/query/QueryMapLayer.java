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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.PointSymbolizer;
import org.opengis.style.PolygonSymbolizer;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.Rule;
import org.opengis.style.Style;
import org.opengis.style.Symbolizer;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeGeometryStyle;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.report.birt.map.IBirtLayerStyleProvider;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.MapLayerInfo;
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
		
		return findGeometryColumnsInResultSet((OdaDataSetHandle) handle);		
	}
	
	@Override
	public StyleBlackboard getStyle(String extensionId, String queryText,
			MapLayerInfo info, ConservationArea ca, Session s) {
		
		if (!extensionId.equals(IntelQueryDataset.DATASET_TYPE)) return null;
	
		if (info.getGeometryColumnId().startsWith( "attribute:")) { //$NON-NLS-1$
			//lets see if we can find a datamodel geometry attribute as associated styling information
			String attkey = info.getGeometryColumnId().substring(info.getGeometryColumnId().indexOf(":")+1); //$NON-NLS-1$
			Attribute a = s.createQuery("FROM Attribute WHERE keyId = :key and conservationArea = :ca", Attribute.class) //$NON-NLS-1$
			.setParameter("key", attkey) //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.uniqueResult();
			if (a != null && a.getType().isGeometry() && a.getRegex() != null) {
				StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
				sb.put(SLDID, AttributeGeometryStyle.fromAttribute(a).toStyle());
				return sb;
			}
		}
		if (info.getGeometryColumnId().equalsIgnoreCase(FixedQueryColumn.Column.LOC_POINT.key) ||
				info.getGeometryColumnId().equalsIgnoreCase(FixedQueryColumn.Column.LOC_POLYGON.key)) {
			UUID uuid = null;
			try {
				uuid = UuidUtils.stringToUuid( queryText.split(IntelQueryDataset.QUERY_DEF_SEP) [1]);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if (uuid == null) return null;
			
			IntelRecordObservationQuery q = s.get(IntelRecordObservationQuery.class, uuid);
			String styleQuery = q.getStyle();
			if (styleQuery != null) {
				
				Map<String, StyleBlackboard> styles = null;
				
				try {	
					styles = StyleManager.INSTANCE.fromStringMap(styleQuery);
				}catch (Exception ex) {
					ex.printStackTrace();
					return null;
				}
				if (styles != null) {
					
					//search for style with correct symbolizer
					for (StyleBlackboard sb : styles.values()) {
						Style ss = (Style) sb.get(SLDID);
						for (FeatureTypeStyle fts: ss.featureTypeStyles()) {
							for (Rule r : fts.rules()) {
								for (Symbolizer sym : r.symbolizers()) {
									if (sym instanceof PointSymbolizer && info.getLayerType() == MapLayerInfo.LayerType.POINT) return sb;
									if (sym instanceof PointSymbolizer && info.getLayerType() == MapLayerInfo.LayerType.MULTIPOINT) return sb;
									
									if (sym instanceof LineSymbolizer && info.getLayerType() == MapLayerInfo.LayerType.LINE) return sb;
									if (sym instanceof LineSymbolizer && info.getLayerType() == MapLayerInfo.LayerType.MULTILINE) return sb;
									
									if (sym instanceof PolygonSymbolizer && info.getLayerType() == MapLayerInfo.LayerType.POLYGON) return sb;
									if (sym instanceof PolygonSymbolizer && info.getLayerType() == MapLayerInfo.LayerType.MULTIPOLYGON) return sb;
									
									if (sym instanceof RasterSymbolizer && info.getLayerType() == MapLayerInfo.LayerType.RASTER) return sb;
								}
							}
						}
					}
				}
			}
			
			//find default style
			SmartStyle style = null;
			if (info.getGeometryColumnId().equalsIgnoreCase(FixedQueryColumn.Column.LOC_POINT.key)) {
				style = StyleManager.INSTANCE.getMapLayerDefaultStyle(ca, IntelRecordObservationQuery.POINT_DEFAULT_STYLE_KEY, s);
			}else if (info.getGeometryColumnId().equalsIgnoreCase(FixedQueryColumn.Column.LOC_POLYGON.key)) {
				style = StyleManager.INSTANCE.getMapLayerDefaultStyle(ca, IntelRecordObservationQuery.POLYGON_DEFAULT_STYLE_KEY, s);				
			}
			if (style != null) {
				try {
					return StyleManager.INSTANCE.fromString(style.getStyleString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}


}
