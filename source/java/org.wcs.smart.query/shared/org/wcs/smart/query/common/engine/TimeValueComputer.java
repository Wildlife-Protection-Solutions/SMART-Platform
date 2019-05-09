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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.Tile;

/**
 * Value computer that computes
 * the time a linestring spent within
 * a given grid cell.  Time is computed in hours. 
 * 
 * @author egouge
 *
 */
public class TimeValueComputer implements IValueComputer<Double> {

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
		
		GeometryFactory gf = GeometryFactoryProvider.getFactory();
		Geometry bbox = gf.toGeometry(env);

		//#1798
		//previously this was doing with ls.intersection(bbox); however this
		//throw topology exceptions in some cases, produced the wrong results when the track
		//doubled back on itself and was substantially slower
		double time = 0;
		for (int i = 1; i < ls.getNumPoints(); i ++){
			Coordinate c1 = ls.getCoordinateN(i-1);
			Coordinate c2 = ls.getCoordinateN(i);
			if (env.intersects(new Envelope(c1, c2))){
				Geometry part = gf.createLineString(new Coordinate[]{c1,c2});
				boolean contains1 = env.contains(c1);
				boolean contains2 = env.contains(c2);
				if (contains2 && contains1) {
					//both are inside
					time += (c2.z - c1.z);
				}else {
					//figure out what percentage of the line is inside and
					//use the same percentage to compute hours
					double l1 =  part.getLength();
					part =  part.intersection(bbox);
					double l2 = part.getLength();
					
					double t2 = (c2.z - c1.z);
					time += (l2 / l1) * t2;
				}
			}
		}
		
		return time / 3_600_000.0;
	}
	
}
