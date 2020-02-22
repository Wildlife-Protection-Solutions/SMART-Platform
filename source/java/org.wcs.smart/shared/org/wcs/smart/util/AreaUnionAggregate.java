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

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

import org.apache.derby.agg.Aggregator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.map.GeometryFactoryProvider;

/**
 * Apache derby aggregate function for unioning geometries then computing
 * the area.
 * 
 * @author Emily
 *
 */
public class AreaUnionAggregate implements Aggregator<Blob, Double, AreaUnionAggregate> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	List<Geometry> geoms = null;
	Geometry union = null;
	WKBReader reader = null;
	
	@Override
	public void accumulate(Blob b) {
		Geometry g = readBlob(b);
		if (g == null) return;
		geoms.add(readBlob(b));
		if (geoms.size() > 50) {
			UnaryUnionOp op = new UnaryUnionOp(geoms);
			g = op.union();
			geoms.clear();
			geoms.add(g);
		}
	}

	private Geometry readBlob(Blob b) {
		try {
			Geometry g = reader.read(b.getBytes(1, (int)b.length()));
			if (g.isEmpty()) return null;
			return g;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void init() {
		reader = new WKBReader(GeometryFactoryProvider.getFactory());
		geoms = new ArrayList<>();
	}

	@Override
	public void merge(AreaUnionAggregate agg) {
		geoms.addAll(agg.geoms);
		if (geoms.size() > 50) {
			UnaryUnionOp op = new UnaryUnionOp(geoms);
			Geometry g = op.union();
			geoms.clear();
			geoms.add(g);
		}
		
	}

	@Override
	public Double terminate() {
		if (geoms.isEmpty()) return 0.0;
		if (geoms.size() != 0) {
			UnaryUnionOp op = new UnaryUnionOp(geoms);
			Geometry g = op.union();
			geoms.clear();
			geoms.add(g);
		}
		Geometry g = geoms.get(0);
		return getArea(g);
	}
	
	private double getArea(Geometry g) {
		try {
			Coordinate c = g.getCentroid().getCoordinate();
			CoordinateReferenceSystem targetCRS = CRS.decode("AUTO2:42001,"+c.x+","+c.y); //$NON-NLS-1$ //$NON-NLS-2$
			Geometry t = JTS.transform(g, ReprojectUtils.findMathTransform(targetCRS));
			return t.getArea();
		}catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
