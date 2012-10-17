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
package org.wcs.smart.util;

import java.util.HashMap;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.hibernate.SmartDB;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Database functions for reprojecting waypoints in 4326
 * and computing tile ids for gridded analysis.
 * 
 * @author egouge
 *
 */
public class ReprojectUtils {

	//static map for looking up projections
	//based on wkt string so doesn't have to continually parse projection
	/**
	 * This map should be cleared by any functions with use this class.
	 */
	public static final HashMap<String, MathTransform> transformMap = new HashMap<String, MathTransform>();
	
	/**
	 * Computes the tile id of a given coordinate in long/lat (4326);
	 * @param x x coordinate (longitude)
	 * @param y y coordinate (latitude)
	 * @param destCrsWkt CRS of the grid
	 * @param originX grid x origin
	 * @param originY grid y origin
	 * @param gridSize grid cell size
	 * @return grid tile id in the form tileidx_tileidy
	 * 
	 * @throws Exception
	 */
	public static String computeTileId(double x, double y, String destCrsWkt,
			double originX, double originY, double gridSize) throws Exception{
		Coordinate c = reproject(x, y, destCrsWkt);
		int tileidx = (int)Math.floor( (c.x - originX) / gridSize) + 1;
		int tileidy = (int)Math.floor( (c.y - originY) / gridSize) + 1;

		return tileidx + "_" + tileidy;
	}
	
	/**
	 * Re-projects a coordinate from long/lat(4326) to destination 
	 * crs provided as wkt.
	 * 
	 * @param x longitude
	 * @param y latitude 
	 * @param destCrsWkt destination coordinate reference system
	 * @return reprojected point
	 * @throws Exception
	 */
	public static Coordinate reproject(double x, double y, String destCrsWkt) throws Exception{
		MathTransform t = transformMap.get(destCrsWkt);
		if (t == null){
			t = CRS.findMathTransform(SmartDB.DATABASE_CRS, CRS.parseWKT(destCrsWkt));
			transformMap.put(destCrsWkt, t);
		}
		Coordinate transformed = JTS.transform(new Coordinate(x,y), null, t);
		return transformed;
	}
}
