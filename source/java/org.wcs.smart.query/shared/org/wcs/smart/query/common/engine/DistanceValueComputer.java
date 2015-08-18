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
package org.wcs.smart.query.common.engine;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.Tile;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Value computer that computes
 * the distance of the linestring
 * in the given tile.
 * 
 * @author egouge
 *
 */
public class DistanceValueComputer implements IValueComputer<Double> {

	/**
	 * Computes the distance of the linestring
	 * in the tile cell.
	 * 
	 * @see org.wcs.smart.query.common.engine.IValueComputer#computeValue(java.lang.Object, org.org.wcs.smart.query.common.model.Tile, org.org.wcs.smart.query.common.model.Grid, com.vividsolutions.jts.geom.LineString)
	 */
	@Override
	public Double computeValue(Double existingValue, Tile t, Grid gridDef, LineString ls) throws Exception{
		if (existingValue != null){
			//already computed for this cell
			return existingValue;
		}
		
		Envelope env = t.getBounds(gridDef);
		
		GeometryFactory gf = new GeometryFactory();
		Geometry g = ls.intersection(gf.toGeometry(env));

		
		double newlength = processGeometry(g, gridDef.getCrs());
		return newlength;
	}
	
	private double processGeometry(Geometry g, CoordinateReferenceSystem crs) throws Exception{
		if (g instanceof GeometryCollection ){
			double distance = 0;
			int cnt = ((GeometryCollection)g).getNumGeometries();
			for (int i = 0; i < cnt; i++){
				distance += processGeometry(((GeometryCollection)g).getGeometryN(i), crs);
			}
			return distance;
		}else if (g instanceof LineString){
			return GeometryUtils.distanceInMeters((LineString)g, crs ) / 1000.0;
		}
		return 0;
	}

}
