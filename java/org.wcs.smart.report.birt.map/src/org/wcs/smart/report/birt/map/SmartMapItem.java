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
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.extension.ReportItem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.report.birt.map.internal.Messages;

/**
 * A Smart map report item.
 * @author Emily
 *
 */
public class SmartMapItem extends ReportItem {

	public static final String DEFAULT_BASEMAP_KEY = "default"; //$NON-NLS-1$
	/**
	 * Smart map item extension name
	 */
	public static final String EXTENSION_NAME = "org.wcs.smart.report.birt.SmartMap";  //$NON-NLS-1$

	/**
	 * Basemap option property
	 */
	public static final String SMART_BASEMAP_PROP = "org.wcs.smart.birt.map.basemap"; //$NON-NLS-1$
	
	/**
	 * List of layers; layers are represented by
	 * query string of the layer they represent
	 */
	public static final String SMART_LAYER_PROP = "org.wcs.smart.birt.map.layers"; //$NON-NLS-1$
	/**
	 * List of layer names
	 */
	public static final String SMART_LAYERNAME_PROP = "org.wcs.smart.birt.map.layerNames"; //$NON-NLS-1$
	/**
	 * List of layer styles; encoded as SLD xmlstring
	 */
	public static final String SMART_LAYERSTYLE_PROP = "org.wcs.smart.birt.map.layerStyles"; //$NON-NLS-1$
	
	/**
	 * List of layer dataset names;
	 */
	public static final String SMART_DATASET_PROP = "org.wcs.smart.birt.map.layerDataSet"; //$NON-NLS-1$
	/**
	 * xmin map bounds property
	 */
	public static final String SMART_BOUNDS_XMIN_PROP = "org.wcs.smart.report.birt.map.bounds.xmin"; //$NON-NLS-1$
	
	/**
	 * xmax map bounds property
	 */
	public static final String SMART_BOUNDS_XMAX_PROP = "org.wcs.smart.report.birt.map.bounds.xmax"; //$NON-NLS-1$
	/**
	 * ymin map bounds property
	 */
	public static final String SMART_BOUNDS_YMIN_PROP = "org.wcs.smart.report.birt.map.bounds.ymin"; //$NON-NLS-1$
	/**
	 * ymax map bounds property
	 */
	public static final String SMART_BOUNDS_YMAX_PROP = "org.wcs.smart.report.birt.map.bounds.ymax"; //$NON-NLS-1$
	/**
	 * srid map bounds property
	 */
	public static final String SMART_BOUNDS_SRID_PROP = "org.wcs.smart.report.birt.map.bounds.srid"; //$NON-NLS-1$
	/**
	 * map bounds property group
	 */
	public static final String SMART_BOUNDS_GROUP = "org.wcs.smart.report.birt.map.bounds"; //$NON-NLS-1$
	
	private ExtendedItemHandle handle;

	/**
	 * Creates a new smart map item
	 * @param item
	 */
	public SmartMapItem(ExtendedItemHandle item) {		
		this.handle = item;
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
	 * 
	 * @Deprecated 2.0 we are going to use dataset names now instead of
	 * queries 
	 */
	public void setLayers(List<String> layers) throws SemanticException{
		handle.setProperty(SMART_LAYER_PROP, layers);
	}
	
	/**
	 * 
	 * @return map layer names
	 * 
	 *  @Deprecated 2.0 we are going to use dataset names now instead of
	 * queries
	 */
	public List<String> getLayerNames(){
		return handle.getListProperty(SMART_LAYERNAME_PROP);
	}
	
	/**
	 * Set map datasets
	 * @param layers
	 * @throws SemanticException
	 */
	public void setDatasets(List<String> layers) throws SemanticException{
		handle.setProperty(SMART_DATASET_PROP, layers);
	}
	
	/**
	 * @return map dataset layers
	 */
	public List<String> getDatasets(){
		List<String> ds = handle.getListProperty(SMART_DATASET_PROP);
		if (ds == null){
			ds = new ArrayList<String>();
		}
		return ds;
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
			SmartMapItemPlugIn.log(Messages.SmartMapItem_CouldNotParseCrs + srs, ex);
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
	
	public IBirtMapLayerManager findMapLayerManager(DataSetHandle handle){
		List<IBirtMapLayerManager> birtlayers = BirtMapUtils.getMapLayerExtensions();
		for (IBirtMapLayerManager l : birtlayers){
			if (l.canAddToMap(handle)){
				return l;
			}
		}	
		return null;
	}
	
}
