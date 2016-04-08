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

import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.extension.ReportItem;
import org.wcs.smart.report.birt.map.MapLayerInfo;

/**
 * Birt Map Layer Item
 * @author Emily
 *
 */
public class LayerItem extends ReportItem {

	/**
	 * Smart map item extension name
	 */
	public static final String EXTENSION_NAME = "org.wcs.smart.report.birt.MapLayer";  //$NON-NLS-1$

	
	/**
	 * Layer Name
	 */
	public static final String SMART_LAYERNAME_PROP = "org.wcs.smart.birt.map.layerName"; //$NON-NLS-1$
	
	/**
	 * Layer Style
	 */
	public static final String SMART_LAYERSTYLE_PROP = "org.wcs.smart.birt.map.layerStyle"; //$NON-NLS-1$
	
	/**
	 * Layer Type
	 */
	public static final String SMART_LAYERTYPE_PROP = "org.wcs.smart.birt.map.layerType"; //$NON-NLS-1$
	
	/**
	 * Geometry Column
	 */
	public static final String SMART_GEOMCOLUMN_PROP = "org.wcs.smart.birt.map.geomColumn"; //$NON-NLS-1$
	
	private ExtendedItemHandle handle;
	
	/**
	 * Creates a new smart map item
	 * @param item
	 */
	public LayerItem(ExtendedItemHandle item) {		
		this.handle = item;
	}
 
	public ExtendedItemHandle getHandle(){
		return handle;
	}
	
	/**
	 * 
	 * @return map layer names
	 * 
	 *  @Deprecated 2.0 we are going to use dataset names now instead of
	 * queries
	 */
	public String getLayerName(){
		return handle.getStringProperty(SMART_LAYERNAME_PROP);
	}
	
	/**
	 * Sets the map layer names
	 * @param layers
	 * @throws SemanticException
	 */
	public void setLayerName(String name) throws SemanticException{
		handle.setProperty(SMART_LAYERNAME_PROP, name);
	}
	
	/**
	 * @return the layer styles
	 */
	public String getLayerStyles(){
		return handle.getStringProperty(SMART_LAYERSTYLE_PROP);
	}
	
	/**
	 * Sets the layer styles
	 * @param layers
	 * @throws SemanticException
	 */
	public void setLayerStyles(String layer) throws SemanticException{
		handle.setProperty(SMART_LAYERSTYLE_PROP, layer);
	}
	
	/**
	 * @return the layer geometry type
	 */
	public MapLayerInfo.LayerType getLayerType(){
		String v = handle.getStringProperty(SMART_LAYERTYPE_PROP);
		if (v == null) return null;
		return MapLayerInfo.LayerType.valueOf(handle.getStringProperty(SMART_LAYERTYPE_PROP));
	}
	
	/**
	 * Sets the layer geometry type
	 * @param layers
	 * @throws SemanticException
	 */
	public void setLayerType(MapLayerInfo.LayerType type) throws SemanticException{
		handle.setProperty(SMART_LAYERTYPE_PROP, type.toString());
	}
	
	/**
	 * @return the layer geometry column
	 */
	public String getGeometryColumn(){
		return handle.getStringProperty(SMART_GEOMCOLUMN_PROP);
	}
	
	/**
	 * Sets the layer geometry type
	 * @param layers
	 * @throws SemanticException
	 */
	public void setGeometryColumn(String geomColumn) throws SemanticException{
		handle.setProperty(SMART_GEOMCOLUMN_PROP, geomColumn);
	}
}
