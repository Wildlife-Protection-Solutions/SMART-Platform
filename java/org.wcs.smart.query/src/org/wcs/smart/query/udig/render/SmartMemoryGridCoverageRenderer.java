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

import net.refractions.udig.project.internal.StyleBlackboard;
import net.refractions.udig.project.render.RenderException;
import net.refractions.udig.render.internal.gridcoverage.basic.MemoryGridCoverageRenderer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridGeometry;
import org.wcs.smart.query.QueryPlugIn;

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
    	
    	super.render(graphics, monitor);
    	
    	//now try to render grid around points    	
		StyleBlackboard styleBlackboard = (StyleBlackboard) getContext().getLayer().getStyleBlackboard();
		Style style = (Style) styleBlackboard.get(SmartGridCellStyleContent.STYLE_ID);
		if (style == null)
			return;

		LineSymbolizer ls = SLD.lineSymbolizer(style);
		if (ls == null)
			return;
         
         
		ReferencedEnvelope bounds = getContext().getLayer().getBounds(monitor,
				getContext().getCRS());
		try {
			GridCoverage gc = getContext().getLayer().getGeoResource().resolve(GridCoverage.class, monitor);

			GridGeometry ggc = gc.getGridGeometry();
			int numXCells = ggc.getGridRange().getHigh(0) - ggc.getGridRange().getLow(0) + 1;
			int numYCells = ggc.getGridRange().getHigh(1) - ggc.getGridRange().getLow(1) + 1;
			double cellXSize = bounds.getWidth() / numXCells;
			double cellYSize = bounds.getHeight() / numYCells;

			graphics.setColor(SLD.lineColor(ls));
			graphics.setStroke(new BasicStroke(SLD.lineWidth(ls)));

			for (int i = 0; i <= numXCells; i++) {
				Coordinate start = new Coordinate(bounds.getMinimum(0) + i * cellXSize, bounds.getMinimum(1));
				Coordinate end = new Coordinate(bounds.getMinimum(0) + i * cellXSize, bounds.getMaximum(1));
				Point s = getContext().getViewportModel().worldToPixel(start);
				Point e = getContext().getViewportModel().worldToPixel(end);
				graphics.drawLine(s.x, s.y, e.x, e.y);
			}

			for (int i = 0; i <= numYCells; i++) {
				Coordinate start = new Coordinate(bounds.getMinimum(0), bounds.getMinimum(1) + i * cellYSize);
				Coordinate end = new Coordinate(bounds.getMaximum(0), bounds.getMinimum(1) + i * cellYSize);
				Point s = getContext().getViewportModel().worldToPixel(start);
				Point e = getContext().getViewportModel().worldToPixel(end);
				graphics.drawLine(s.x, s.y, e.x, e.y);
			}

		} catch (IOException e) {
			QueryPlugIn.log(e.getMessage(), e);
		}
	}

}
