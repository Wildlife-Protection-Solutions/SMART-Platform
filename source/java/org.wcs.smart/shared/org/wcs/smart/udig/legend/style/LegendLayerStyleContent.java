/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 *
 */
package org.wcs.smart.udig.legend.style;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.mapgraphic.MapGraphic;
import org.locationtech.udig.project.StyleContent;

/**
 * Style to identify in the layer should appear in the legend mapgraphic or not
 * 
 * @author Emily
 */
public class LegendLayerStyleContent extends StyleContent {
	
	/** extension id */
	public static final String ID = "org.locationtech.udig.legend.layer.style"; //$NON-NLS-1$
    
	//momento keys
    private static final String VISIBLE = "isvisible"; //$NON-NLS-1$
    
    /**
     * Legend location style.
     */
    public LegendLayerStyleContent(){
        super( ID );
    }
    
    public Class<?> getStyleClass() {
        return Point.class;
    }
    
    public Object load( IMemento memento ) {
        boolean isVisible = memento.getBoolean(VISIBLE);
        
        LegendLayerStyle style = LegendLayerStyleContent.createDefaultStyle();
        style.isVisible = isVisible;
        
        return style;
    }

    public void save( IMemento memento, Object item ) {
    	LegendLayerStyle style = (LegendLayerStyle) item;
        memento.putBoolean(VISIBLE, style.isVisible);        
    }
    
    public Object createDefaultStyle(IGeoResource resource, Color colour,  IProgressMonitor monitor) throws IOException {
        if( !resource.canResolve(MapGraphic.class))
            return null;

        if( resource.canResolve(LegendLayerStyle.class) ){
            // lets assume this is the best location for this resource
        	LegendLayerStyle style = resource.resolve(LegendLayerStyle.class, monitor);
            if( style !=null ){
                return style;
            }
        }
        
        return createDefaultStyle();
	}
	
    public Object load( URL url, IProgressMonitor monitor) throws IOException {
        return null;
    }

    public static LegendLayerStyle createDefaultStyle() {
    	LegendLayerStyle style = new LegendLayerStyle();
    	style.isVisible = true;
    	
    	return style;
    }
    
}
