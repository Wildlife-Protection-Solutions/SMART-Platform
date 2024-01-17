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
package org.wcs.smart.ca;

/**
 * For geometry columns in query and other datasets
 * 
 */
public interface IGeometryColumn {
	
	public enum Type{
		
		POINT("Point", 8200), //$NON-NLS-1$
		LINESTRING("LineString", 8201), //$NON-NLS-1$
		MULTIPOINT("MultiLineString", 8202), //$NON-NLS-1$
		MULTILINESTRING("MultiLineString", 8203), //$NON-NLS-1$
		POLYGON("Polygon", 8204), //$NON-NLS-1$
		MULTIPOLYGON("MultiPolygon", 8205); //$NON-NLS-1$
		
		public String geoToolsType;
		public int birtDataType;
		
		Type(String geoToolsType, int birtDataType) {
			this.geoToolsType = geoToolsType;
			this.birtDataType = birtDataType;
		}		
	}
	
	/**
	 * Used for creating geotools feature types
	 * 
	 * @return Point, Polygon, MultiPolygon, LineString etc.
	 */
	public Type getGeometryType();
	
	public default int getSRID() {
		return 4326;
	}
}
