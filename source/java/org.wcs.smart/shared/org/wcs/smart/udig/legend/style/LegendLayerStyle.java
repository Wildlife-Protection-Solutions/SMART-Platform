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

/**
 * Style to identify in the layer should appear in the legend mapgraphic or not
 * 
 * @author Emily
 */
public class LegendLayerStyle {
    
	public boolean isVisible;
    
    /**
     * Creates a new empty legend style
     */
    public LegendLayerStyle() {
    	
    }
    
    
	/**
	 * Creates a new legend style copying the values
	 * from the old legend style
	 * @param oldStyle
	 */
	public LegendLayerStyle(LegendLayerStyle oldStyle) {
		super();
		this.isVisible = oldStyle.isVisible;
	}
    
}
