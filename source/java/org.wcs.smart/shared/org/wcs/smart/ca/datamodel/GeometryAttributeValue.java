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
package org.wcs.smart.ca.datamodel;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;

/**
 * 
 * Used to represent and manage a geometry attribute value that includes
 * source, area, and perimeter.
 * 
 * @author Emily
 * @since 8.0.0
 *
 */
public class GeometryAttributeValue {

	private Attribute.GeometrySource source;
	private Double area;
	private Double perimeter;
	private Geometry geometry;
	
	public GeometryAttributeValue(Geometry geometry, Double area, Double perimeter, Attribute.GeometrySource source) {
		this.geometry = geometry;
		this.source = source;
		this.area = area;
		this.perimeter = perimeter;
		
		compute(false);
	}
	
	public GeometryAttributeValue(Geometry geometry, Attribute.GeometrySource source) {
		this.geometry = geometry;
		this.source = source;
		
		compute(true);
	}
	
	private void compute(boolean reset) {

		if (geometry instanceof LineString) {
			geometry = GeometryFactoryProvider.getFactory().createMultiLineString(new LineString[] {(LineString) geometry});
		}
		if (geometry instanceof Polygon) {
			geometry = GeometryFactoryProvider.getFactory().createMultiPolygon(new Polygon[] {(Polygon) geometry});
		}
		
		if (reset) {
			if (geometry instanceof MultiLineString) {
				this.perimeter = GeometryUtils.getGeometryPerimeterInKm(this.geometry);
				this.area = null;
			}
			
			if (geometry instanceof MultiPolygon) {
				this.area = GeometryUtils.getAreaInKm((MultiPolygon)geometry);
				this.perimeter = GeometryUtils.getGeometryPerimeterInKm(this.geometry);
			}
		}
	}
	
	public Attribute.GeometrySource getSource() {
		return source;
	}
	
	public void setSource(Attribute.GeometrySource source) {
		this.source = source;
	}
	
	public Double getArea() {
		return area;
	}
	
	public Double getPerimeter() {
		return perimeter;
	}

	public Geometry getGeometry() {
		return geometry;
	}
	
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
		compute(true);
		
	}
	
	public boolean isPolygon() {
		return this.geometry instanceof MultiPolygon;
	}
	
	public boolean isLineString() {
		return this.geometry instanceof MultiLineString;
	}
}
