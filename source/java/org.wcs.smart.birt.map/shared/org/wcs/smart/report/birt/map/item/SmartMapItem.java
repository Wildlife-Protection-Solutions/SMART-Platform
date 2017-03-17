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
package org.wcs.smart.report.birt.map.item;

import java.util.List;

import org.eclipse.birt.report.model.api.CommandStack;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.eclipse.birt.report.model.api.extension.ReportItem;
import org.eclipse.birt.report.model.api.metadata.IChoice;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
	 * List of layers
	 */
	public static final String SMART_LAYER_PROP2 = "org.wcs.smart.birt.map.layers2"; //$NON-NLS-1$
	
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
	/**
	 * map dpi setting
	 */
	public static final String SMART_DPI = "org.wcs.smart.birt.map.dpi"; //$NON-NLS-1$
	
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
	 * Gets the layers property handle.
	 * 
	 * @return measures property handle
	 */

	public PropertyHandle getLayersProperty( ){
		return handle.getPropertyHandle( SMART_LAYER_PROP2 );
	}
	
	/**
	 * Adds a new layer handle
	 * @param layerHandle
	 * @throws Exception
	 */
	public void addLayers(List<LayerItem> layerHandles) throws Exception{
		CommandStack stack = handle.getModuleHandle().getCommandStack();
		stack.startTrans( "Insert Layers" ); //$NON-NLS-1$
		try{
			int cnt = 0;
			for (LayerItem i : layerHandles){
				getLayersProperty().add(i.getHandle(), cnt++);	
			}
		}catch ( Exception e ){
			stack.rollback( );
			throw e;
		}
		stack.commit( );
	}
	
	public void moveLayer(LayerItem layerHandles, int offset) throws Exception{
		CommandStack stack = handle.getModuleHandle().getCommandStack();
		stack.startTrans( "Move Layers" ); //$NON-NLS-1$
		try{
			int oldindex = getLayersProperty().getListValue().indexOf(layerHandles.getHandle());
			getLayersProperty().moveItem(oldindex, oldindex+offset);
		}catch ( Exception e ){
			stack.rollback( );
			throw e;
		}
		stack.commit( );
	}
	
	
	/**
	 * Gets the measure view with the given index. Position index is 0-based
	 * integer.
	 * 
	 * @param index
	 *            a 0-based integer of the measure position
	 * @return the measure view handle if found, otherwise null
	 */
	public LayerItem getLayer( int index ){
		DesignElementHandle element = getLayersProperty( ).getContent( index );
		if ( !( element instanceof ExtendedItemHandle ) ) return null;
		ExtendedItemHandle extendedItem = (ExtendedItemHandle) element;
		try{
			return (LayerItem) extendedItem.getReportItem( );
		}
		catch ( ExtendedElementException e )
		{
			return null;
		}
	}
	
	/**
	 * 
	 * @param index
	 * @return the layer item handle for the layer at the given index
	 */
	public ExtendedItemHandle getLayerHandle(int index){
		DesignElementHandle element = getLayersProperty( ).getContent( index );
		if ( !( element instanceof ExtendedItemHandle ) )
			return null;
		ExtendedItemHandle extendedItem = (ExtendedItemHandle) element;
		return extendedItem;
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
			throw new IllegalStateException("Could not parse coordinate reference system.", ex); //$NON-NLS-1$
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

	/**
	 * Gets the DPI setting as an integer
	 * @return
	 */
	public Integer getDPI(){
		String key = handle.getStringProperty(SMART_DPI);
		for (IChoice c : handle.getChoices(SMART_DPI)){
			if (c.getName().equals(key)){
				return Integer.valueOf((String)c.getValue());
			}
		}
		return null;
	}
	
	/**
	 * Sets the map dpi setting
	 * @param dpi
	 * @throws SemanticException
	 */
	public void setDPI(int dpi) throws SemanticException{
		handle.setProperty(SMART_DPI, String.valueOf(dpi));
	}
	
	
}
