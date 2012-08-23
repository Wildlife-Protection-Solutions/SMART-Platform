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
package org.wcs.smart.birt.map;

import java.util.List;

import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.extension.ReportItem;
import org.eclipse.birt.report.model.api.metadata.DimensionValue;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A Smart map report item.
 * @author Emily
 *
 */
public class SmartMapItem extends ReportItem {

	/**
	 * Smart query dataset ids
	 */
	public static final String SMART_QUERY_ID = "org.wcs.smart.data.oda.smart.smartQueryDataset";
	
	/**
	 * Smart map item extension name
	 */
	public static final String EXTENSION_NAME = "Smart Map";

	/**
	 * Basemap option property
	 */
	public static final String SMART_BASEMAP_PROP = "org.wcs.smart.birt.map.basemap";
	
	/**
	 * List of layers; layers are represented by
	 * the hex encoded uuid of the query they represent
	 */
	public static final String SMART_LAYER_PROP = "org.wcs.smart.birt.map.layers";
	/**
	 * List of layer names
	 */
	public static final String SMART_LAYERNAME_PROP = "org.wcs.smart.birt.map.layerNames";
	/**
	 * List of layer styles; encoded as SLD xmlstring
	 */
	public static final String SMART_LAYERSTYLE_PROP = "org.wcs.smart.birt.map.layerStyles";
	
	/**
	 * xmin map bounds property
	 */
	public static final String SMART_BOUNDS_XMIN_PROP = "org.wcs.smart.report.birt.map.bounds.xmin";
	
	/**
	 * xmax map bounds property
	 */
	public static final String SMART_BOUNDS_XMAX_PROP = "org.wcs.smart.report.birt.map.bounds.xmax";
	/**
	 * ymin map bounds property
	 */
	public static final String SMART_BOUNDS_YMIN_PROP = "org.wcs.smart.report.birt.map.bounds.ymin";
	/**
	 * ymax map bounds property
	 */
	public static final String SMART_BOUNDS_YMAX_PROP = "org.wcs.smart.report.birt.map.bounds.ymax";
	/**
	 * srid map bounds property
	 */
	public static final String SMART_BOUNDS_SRID_PROP = "org.wcs.smart.report.birt.map.bounds.srid";
	/**
	 * map bounds property group
	 */
	public static final String SMART_BOUNDS_GROUP = "org.wcs.smart.report.birt.map.bounds";
	
	private ExtendedItemHandle handle;

	/**
	 * Creates a new smart map item
	 * @param item
	 */
	public SmartMapItem(ExtendedItemHandle item) {
		this.handle = item;
		DimensionValue dm = (DimensionValue)handle.getWidth().getValue();
		if (dm == null || dm.getMeasure() == 0){
			try{
				handle.setWidth("50px");
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
		dm = (DimensionValue)handle.getHeight().getValue();
		if (dm == null || dm.getMeasure() == 0){
			try{
				handle.setHeight("50px");
			}catch (Exception ex){}
		}
	}

	/**
	 * @return the basemap layer name
	 */
	public String getBasemapName() {
		return handle.getStringProperty(SMART_BASEMAP_PROP);
	}

	/**
	 * Sets the baemap name
	 * @param basemapName
	 * @throws SemanticException
	 */
	public void setBasemapName(String basemapName) throws SemanticException {
		handle.setStringProperty(SMART_BASEMAP_PROP, basemapName);
	}

	/**
	 * @return list of layers on map
	 */
	public List<String> getLayers(){
		return handle.getListProperty(SMART_LAYER_PROP);
	}
	
	/**
	 * Set map layers
	 * @param layers
	 * @throws SemanticException
	 */
	public void setLayers(List<String> layers) throws SemanticException{
		handle.setProperty(SMART_LAYER_PROP, layers);
	}
	
	/**
	 * @return map layer names 
	 */
	public List<String> getLayerNames(){
		return handle.getListProperty(SMART_LAYERNAME_PROP);
	}
	
	/**
	 * Sets the map layer names
	 * @param layers
	 * @throws SemanticException
	 */
	public void setLayerNames(List<String> layers) throws SemanticException{
		handle.setProperty(SMART_LAYERNAME_PROP, layers);
	}
	
	/**
	 * @return the layer styles
	 */
	public List<String> getLayerStyles(){
		return handle.getListProperty(SMART_LAYERSTYLE_PROP);
	}
	
	/**
	 * Sets the layer styles
	 * @param layers
	 * @throws SemanticException
	 */
	public void setLayerStyles(List<String> layers) throws SemanticException{
		handle.setProperty(SMART_LAYERSTYLE_PROP, layers);
	}
	
	/**
	 * @return the map bounds
	 */
	public ReferencedEnvelope getMapBounds(){
		String srs = handle.getStringProperty(SMART_BOUNDS_SRID_PROP);
		if (srs == null){
			return null;
		}
		CoordinateReferenceSystem crs = null;
		try{
			crs = CRS.parseWKT(handle.getStringProperty(SMART_BOUNDS_SRID_PROP));
		}catch (Exception ex){
			SmartMapItemPlugIn.log("Could not parse crs for report. " + srs, ex);
			return null;
		}
		
		double x1 = handle.getFloatProperty(SMART_BOUNDS_XMIN_PROP);
		double x2 = handle.getFloatProperty(SMART_BOUNDS_XMAX_PROP);
		double y1 = handle.getFloatProperty(SMART_BOUNDS_YMIN_PROP);
		double y2 = handle.getFloatProperty(SMART_BOUNDS_YMAX_PROP);
		
		return new ReferencedEnvelope(x1, x2, y1, y2, crs);
	}
	
	/**
	 * Sets the map bounds
	 * @param bounds
	 * @throws SemanticException
	 */
	public void setMapBounds(ReferencedEnvelope e) throws SemanticException{
		
		if (e == null){
			handle.setProperty(SMART_BOUNDS_XMIN_PROP, null);
			handle.setProperty(SMART_BOUNDS_XMAX_PROP, null);
			handle.setProperty(SMART_BOUNDS_YMIN_PROP, null);
			handle.setProperty(SMART_BOUNDS_YMAX_PROP, null);
			handle.setProperty(SMART_BOUNDS_SRID_PROP, null);
		}else{
			handle.setFloatProperty(SMART_BOUNDS_XMIN_PROP, e.getMinX());
			handle.setFloatProperty(SMART_BOUNDS_XMAX_PROP, e.getMaxX());
			handle.setFloatProperty(SMART_BOUNDS_YMIN_PROP, e.getMinY());
			handle.setFloatProperty(SMART_BOUNDS_YMAX_PROP, e.getMaxY());
			handle.setProperty(SMART_BOUNDS_SRID_PROP, e.getCoordinateReferenceSystem().toWKT());
		}
	}
	
}
