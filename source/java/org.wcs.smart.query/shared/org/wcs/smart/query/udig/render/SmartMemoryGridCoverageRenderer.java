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
package org.wcs.smart.query.udig.render;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.render.RenderException;
import org.locationtech.udig.render.internal.gridcoverage.basic.MemoryGridCoverageRenderer;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.udig.style.SmartGridCellStyleContent;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Extension of a memory grid coverage renderer
 * that also attempts to draw the grid cell bounds
 * if a line symbolizer is included in the style.
 * 
 */
public class SmartMemoryGridCoverageRenderer extends MemoryGridCoverageRenderer {
    
    /**
     * Renderer using a GridCoverageLoader to render out of memory
     * (used to load jpeg into memory; or can be used when user requests cache)
     */
    public SmartMemoryGridCoverageRenderer(){        
    }
    
	public synchronized void render( Graphics2D graphics, IProgressMonitor monitor )
            throws RenderException {
    	
		BoundingBox layerBounds = getContext().getLayer().getBounds(monitor, getContext().getCRS());
		try {
			layerBounds = layerBounds.toBounds(getContext().getImageBounds().getCoordinateReferenceSystem());
		} catch (TransformException e1) {
			throw new RenderException(e1);
		}
		if (!getContext().getImageBounds().intersects((BoundingBox)layerBounds)){
			//does not overlap image bounds do not render;  otherwise a npe exception is thrown
			return;
		}
		
    	super.render(graphics, monitor);
    	
    	//now try to render grid around points    	
		StyleBlackboard styleBlackboard = (StyleBlackboard) getContext().getLayer().getStyleBlackboard();
		Style style = (Style) styleBlackboard.get(SmartGridCellStyleContent.STYLE_ID);
		if (style == null)
			return;

		LineSymbolizer ls = SLD.lineSymbolizer(style);
		if (ls == null)
			return;
         
         

		try {
			
			GridCoverage gc = getContext().getLayer().getGeoResource().resolve(GridCoverage.class, monitor);
			
			if (gc == null){
				return;
			}
			GridGeometry ggc = gc.getGridGeometry();
			int numXCells = ggc.getGridRange().getHigh(0) - ggc.getGridRange().getLow(0) + 1;
			int numYCells = ggc.getGridRange().getHigh(1) - ggc.getGridRange().getLow(1) + 1;
			double cellXSize = layerBounds.getWidth() / numXCells;
			double cellYSize = layerBounds.getHeight() / numYCells;

			graphics.setColor(SLD.lineColor(ls));
			graphics.setStroke(new BasicStroke(SLD.lineWidth(ls), 0, 0, 1, SLD.lineDash(ls), 0));
			
			for (int i = 0; i <= numXCells; i++) {
				Coordinate start = new Coordinate(layerBounds.getMinimum(0) + i * cellXSize, layerBounds.getMinimum(1));
				Coordinate end = new Coordinate(layerBounds.getMinimum(0) + i * cellXSize, layerBounds.getMaximum(1));
				Point s = getContext().getViewportModel().worldToPixel(start);
				Point e = getContext().getViewportModel().worldToPixel(end);
				graphics.drawLine(s.x, s.y, e.x, e.y);
			}

			for (int i = 0; i <= numYCells; i++) {
				Coordinate start = new Coordinate(layerBounds.getMinimum(0), layerBounds.getMinimum(1) + i * cellYSize);
				Coordinate end = new Coordinate(layerBounds.getMaximum(0), layerBounds.getMinimum(1) + i * cellYSize);
				Point s = getContext().getViewportModel().worldToPixel(start);
				Point e = getContext().getViewportModel().worldToPixel(end);
				graphics.drawLine(s.x, s.y, e.x, e.y);
			}

			
		} catch (IOException e) {
			
			Logger.getLogger(SmartMemoryGridCoverageRenderer.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
	}

}
