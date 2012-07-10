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
import java.sql.SQLException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * A collection of geometry functions for
 * supporting spatial queries in the derby 
 * database.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class GeometryUtils {
	private static GeometryFactory geomFactory = new GeometryFactory();
	
	private static WKBReader wkbreader(){
		return new WKBReader(geomFactory);
	}
	
	/**
	 * Determines if the given point is in the polygon.
	 * <p>
	 * Assumes the point and polygon are of
	 * the same projection.
	 * </p>
	 * @param x
	 * @param y
	 * @param wkb
	 * @return
	 */
	public static boolean pointInPolygon(Double x, Double y, Blob wkb){
		try {
			return pointInPolygon(x,y,wkb.getBytes(1, (int)wkb.length()));
		} catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * Determines if the given point is in the polygon.
	 * <p>
	 * Assumes the point in polygon are of the same
	 * projection.
	 * </p>
	 * @param x
	 * @param y
	 * @param wkb
	 * @return
	 */
	public static boolean pointInPolygon(Double x, Double y, byte[] wkb){
		if (wkb == null || x == null || y == null) return false;
		Geometry geom = gFromWKB(wkb);
		return geom.intersects(geomFactory.createPoint(new Coordinate(x,y)));
	}
	
	/**
	 * Determines if two geometries represented by the
	 * wkb blobs intersect.
	 * <p>Assumes the wkb geometries are in the same projection.</p>
	 * @param wkb1
	 * @param wkb2
	 * @return
	 */
	public static boolean intersects (Blob wkb1, Blob wkb2){
		if (wkb1 == null || wkb2 == null) return false;
		try{
			Geometry g1 = gFromWKB(wkb1.getBytes(1, (int)wkb1.length()));
			Geometry g2 = gFromWKB(wkb2.getBytes(1, (int)wkb2.length()));
			return g1.intersects(g2);
		}catch (SQLException e){
			throw new RuntimeException ( e );
		}
	}
	
	/**
	 * Converts a wkb array of tyes into a geometry.
	 * @param wkb
	 * @return
	 */
	public static Geometry gFromWKB( byte[] wkb ) {
        try {
            return wkbreader().read( wkb );
        }
        catch (ParseException e) {
            throw new RuntimeException( e );
        }
    }
}
