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
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.render.RenderException;
import org.locationtech.udig.render.internal.gridcoverage.basic.MemoryGridCoverageRenderer;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.udig.style.SmartGridCellStyleContent;

/**
 * Extension of a memory grid coverage renderer
 * that also attempts to draw the grid cell bounds
 * if a line symbolizer is included in the style.
 * 
 */
public class SmartMemoryGridCoverageRenderer extends MemoryGridCoverageRenderer {
    
	/**
	 * Vender-specific option that identified is cells with data (not 0, and not no-data)
	 * should have border drawn.
	 */
	public static final String DRAW_NOTZERO_OP = "DRAW_NOTZERO"; //$NON-NLS-1$
	/**
	 * Vender-specific option that identified is cells with nodata 
	 * should have border drawn.
	 */
	public static final String DRAW_NODATA_OP = "DRAW_NODATA"; //$NON-NLS-1$
	/**
	 * Vender-specific option that identified is cells with a value of zero 
	 * should have border drawn.
	 */
	public static final String DRAW_ZERODATA_OP = "DRAW_ZERODATA"; //$NON-NLS-1$
	
    /**
     * Renderer using a GridCoverageLoader to render out of memory
     * (used to load jpeg into memory; or can be used when user requests cache)
     */
    public SmartMemoryGridCoverageRenderer(){        
    }
    
	public synchronized void render( Graphics2D graphics, IProgressMonitor monitor )
            throws RenderException {
    	
		//render image
		super.render(graphics, monitor);
		
		//add style
		StyleBlackboard styleBlackboard = (StyleBlackboard) getContext().getLayer().getStyleBlackboard();
		Style style = (Style) styleBlackboard.get(SmartGridCellStyleContent.STYLE_ID);
		if (style == null)
			return;
		
		LineSymbolizer ls = SLD.lineSymbolizer(style);
		if (ls == null) return;
		
		boolean drawZero = false;
		boolean drawData = false;
		boolean drawNoData = false;
		
		String str = style.featureTypeStyles().get(0).getOptions().get(DRAW_NOTZERO_OP);
    	if (str != null) drawData = Boolean.valueOf(str);
    	
    	str = style.featureTypeStyles().get(0).getOptions().get(DRAW_ZERODATA_OP);
    	if (str != null) drawZero = Boolean.valueOf(str);
    	
    	str = style.featureTypeStyles().get(0).getOptions().get(DRAW_NODATA_OP);
    	if (str != null) drawNoData = Boolean.valueOf(str);

    	//check that we have something to draw
    	if (!drawData && !drawZero && !drawNoData) return;
    	
		
		BoundingBox layerBounds = getContext().getLayer().getBounds(monitor, getContext().getImageBounds().getCoordinateReferenceSystem());
		if (!getContext().getImageBounds().intersects((BoundingBox)layerBounds)){
			//does not overlap image bounds do not render;  otherwise a npe exception is thrown
			return;
		}
		
		try {
			GridCoverage gc = getContext().getLayer().getGeoResource().resolve(GridCoverage.class, monitor);
			if (gc == null) return;
			
			double[] nodata = gc.getSampleDimension(0).getNoDataValues();
			if (nodata == null) nodata = new double[] {};
			
			GridGeometry ggc = gc.getGridGeometry();

			graphics.setColor(SLD.lineColor(ls));
			graphics.setStroke(new BasicStroke(SLD.lineWidth(ls), 0, 0, 1, SLD.lineDash(ls), 0));

			
			int numXCells = ggc.getGridRange().getHigh(0) - ggc.getGridRange().getLow(0) + 1;
			int numYCells = ggc.getGridRange().getHigh(1) - ggc.getGridRange().getLow(1) + 1;
			double cellXSize = (gc.getEnvelope().getMaximum(0) - gc.getEnvelope().getMinimum(0)) / numXCells;
			double cellYSize = (gc.getEnvelope().getMaximum(1) - gc.getEnvelope().getMinimum(1))/ numYCells;
			
			DataBuffer db = gc.getRenderedImage().getData().getDataBuffer();
			
			//track cells that are drawn so we don't re-draw border
			//as rounding leads to poor results
			HashSet<String> drawn = new HashSet<>();
			
			MathTransform transform = null;
			try {
				transform = CRS.findMathTransform(gc.getCoordinateReferenceSystem(), getContext().getViewportModel().getCRS());
			} catch (FactoryException e1) {
				Logger.getLogger(SmartMemoryGridCoverageRenderer.class.getName()).log(Level.WARNING, e1.getMessage(), e1);
				return;
			}
			
			for (int i = 0; i < db.getSize(); i ++) {
				double value = db.getElemDouble(i);
			
				boolean isnodata = false;
				for (int j = 0; j < nodata.length; j ++) {
					if (value == nodata[j]) {
						isnodata=true;
						break;
					}
				}
				if (isnodata && !drawNoData) continue;
				if (value == 0 && !drawZero) continue;
				if (value != 0 && !isnodata && !drawData) continue;
				
				int y = (int)Math.floor( i / (double)gc.getRenderedImage().getWidth() );
				int x = i - (y * gc.getRenderedImage().getWidth());
				
				double gx = x * cellXSize + gc.getEnvelope().getMinimum(0);
				double gy = gc.getEnvelope().getMaximum(1) - y * cellYSize ;

				Coordinate x1 = new Coordinate(gx, gy);
				Coordinate x2 = new Coordinate(gx + cellXSize, gy);
				Coordinate x3 = new Coordinate(gx + cellXSize, gy - cellYSize);
				Coordinate x4 = new Coordinate(gx, gy - cellYSize);
				
				try {
					JTS.transform(x1, x1, transform);
					JTS.transform(x2, x2, transform);
					JTS.transform(x3, x3, transform);
					JTS.transform(x4, x4, transform);
				}catch (Exception ex) {
					continue;
				}
				
				Point p1 = getContext().getViewportModel().worldToPixel(x1);
				Point p2 = getContext().getViewportModel().worldToPixel(x2);
				Point p3 = getContext().getViewportModel().worldToPixel(x3);
				Point p4 = getContext().getViewportModel().worldToPixel(x4);
				
				
				if (!drawn.contains((x-1) + "_" + y)) { //$NON-NLS-1$
					//draw left side
					graphics.drawLine(p1.x, p1.y, p4.x, p4.y);
				}
				if (!drawn.contains(x + "_" + (y-1))) { //$NON-NLS-1$
					//drop top
					graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
				}
				if (!drawn.contains((x+1) + "_" + y)) { //$NON-NLS-1$
					//draw right
					graphics.drawLine(p2.x, p2.y, p3.x, p3.y);
				}
				if (!drawn.contains(x + "_" + (y+1))) { //$NON-NLS-1$
					//draw bottom
					graphics.drawLine(p3.x, p3.y, p4.x, p4.y);
				}
				
				drawn.add(x + "_" + y); //$NON-NLS-1$
			}
			
		} catch (IOException e) {
			Logger.getLogger(SmartMemoryGridCoverageRenderer.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
	}

}
