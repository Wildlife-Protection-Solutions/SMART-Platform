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
package org.wcs.smart.query.model.gridded;

import java.awt.Point;

import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * This class is responsible of calculating the raster coordinates.
 * 
 * Raster Coordinates in pixel values
 * (0,0) ----------- > Max X
 *   |
 *   |
 *   |
 *   |
 *   V
 * Max Y 
 * </pre> 
 * 
 *  
 *  
 * @author Mauricio Pazos
 *
 */
final class RasterCoordsCalculator  {

	
	private RasterCoordsCalculator(){
		//utility class
	}
	
	/**
	 * The x,y provided are coordinates for a point in WGS84 (EPSG:4326) reference system. This
	 * function calculates the associated raster coordinates 
	 * 
	 * @param x long
	 * @param y lat
	 * @param envelop that contains x,y 
	 * @return {@link Point} the raster coordinates
	 */
	public static Point compute(final float x, final float y, final ReferencedEnvelope bounds ){
		
		int tileX = (int) Math.round( Math.abs( bounds.getMinX() - x ) );

		int tileY = (int) Math.round( Math.abs( bounds.getMaxY() - y) );
		
		Point rasterPoint = new Point(tileX, tileY);
		
		return rasterPoint;
	}
	
}
