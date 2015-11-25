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

import javax.sql.rowset.serial.SerialBlob;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/**
 * A collection of geometry functions for
 * supporting spatial queries in the derby 
 * database.
 * 
 * 
 * @author egouge
 * @author Mauricio Pazos
 * @since 1.0.0
 */
public class GeometryUtils {
	private static Double MILLISEC_PER_HOUR = 3600000.0;
	private static GeometryFactory geomFactory = new GeometryFactory();
	
	public static CoordinateReferenceSystem SMART_CRS;
	static{
		try {
			SMART_CRS = CRS.decode("EPSG:4326", true); //$NON-NLS-1$
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	} 
	
	private static WKBReader wkbreader(){
		return new WKBReader(geomFactory);
	}
	
	private static WKBWriter wkbwriter(){
		return new WKBWriter();
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
	 * Computes the intersection of two geometries.
	 * <p>Assumes the wkb geometries are in the same projection.</p>
	 * @param wkb1
	 * @param wkb2
	 * @return
	 */
	public static Blob intersection (Blob wkb1, Blob wkb2){
		if (wkb1 == null || wkb2 == null) return null;
		try{
			Geometry g1 = gFromWKB(wkb1.getBytes(1, (int)wkb1.length()));
			Geometry g2 = gFromWKB(wkb2.getBytes(1, (int)wkb2.length()));
			Geometry g3 = g2.intersection(g1);
			return new SerialBlob(wkbwriter().write(g3));
		}catch (Throwable e){
 			throw new RuntimeException ( e );
		}
	}
	
	
	/**
	 * Calculates the time in hours that the linestring spent
	 * inside the given geometry.  The geometry must either
	 * be a polygon, multipolygon or geometry collection.  The linestring
	 * must be a 3d linestring with the z value representing
	 * the time the point was recorded
	 * 
	 * @param polygon
	 * @param linestring
	 * @return
	 */
	public static double computeHours(Blob geom, Blob linestring){
		if (geom == null || linestring == null){
			return 0;
		}
		try{
			
			Geometry ls = wkbreader().read(linestring.getBytes(1, (int)linestring.length()));
			if (!(ls instanceof LineString)){
				return 0;
			}
			Geometry poly = wkbreader().read(geom.getBytes(1, (int)geom.length()));
			
			if (poly instanceof Polygon){
				double x = computeHours((Polygon)poly, (LineString)ls);
				return x;
			}else if (poly instanceof MultiPolygon){
				double x = computeHours((MultiPolygon)poly, (LineString)ls);
				return x;
			}else if (poly instanceof GeometryCollection){
				double x = computeHours((GeometryCollection)poly, (LineString)ls);
				return x;
			}
		}catch (Throwable e){
			throw new RuntimeException(e);
		}
		return 0;
		
	}
	/*
	 * support function from computeHours
	 */
	private static double computeHours(GeometryCollection geom, LineString ls){
		double total = 0;
		for (int i = 0; i < geom.getNumGeometries(); i ++){
			Geometry g = geom.getGeometryN(i);
			if (g instanceof Polygon){
				total += computeHours((Polygon)g, ls);
			}else if (g instanceof MultiPolygon){
				total += computeHours((MultiPolygon)g, ls);
			}else if (g instanceof GeometryCollection){
				total += computeHours((GeometryCollection)g, ls);
			}
		}
		return total;
	}
	/*
	 * support function from computeHours
	 */
	private static double computeHours(MultiPolygon poly, LineString ls){
		double total = 0;
		for (int i = 0; i < poly.getNumGeometries(); i ++){
			total += computeHours((Polygon)poly.getGeometryN(i), (LineString)ls);
		}
		return total;
	}
	/*
	 * support function from computeHours
	 */
	private static double computeHours(Polygon p, LineString ls){
		double value = 0;
		if (p.contains(ls)){
			value = ls.getCoordinateN(ls.getNumPoints() - 1).z - ls.getCoordinateN(0).z;
		}else if (!p.intersects(ls)){
			return 0;	//nothing
		}else{
			PreparedGeometry pg = PreparedGeometryFactory.prepare(p);	
			
			Coordinate[] c = ls.getCoordinates();
			for (int i = 0; i < c.length-1; i ++){
				Coordinate c1 = c[i];
				Coordinate c2 = c[i+1];
				
				if (!p.getEnvelopeInternal().intersects(new Envelope(c1, c2))){
					//outside envelop
					continue;
				}
				LineString temp = geomFactory.createLineString(new Coordinate[]{c1, c2});
				if (pg.containsProperly(temp)){
					//entirely contained within
					value += (c2.z - c1.z);
				}else if (pg.intersects(temp)){
					double time = c2.z - c1.z;
					double l1 = c2.distance(c1);
					if (l1 == 0){
						//the points are the same and intersect the polygon to include the entire length of time
						value += time;
					}else{
						//compute the intersection
						double l2 = pg.getGeometry().intersection(temp).getLength();
						value += time * (l2 / l1);
					}
				}
			}
		}
		return value / MILLISEC_PER_HOUR;
	}
	
	/**
	 * Computes the distance in meters for the given geometry.
	 * 
	 * @param wkb
	 * @return distance of all linestrings found in the geometry in meters
	 */
	public static double distanceInMeter(Blob wkb){
		if (wkb == null) return 0;
		try{
			Geometry g = gFromWKB(wkb.getBytes(1, (int)wkb.length()));
			if (g instanceof LineString){
				return distanceInMeters((LineString)g);
			}else if (g instanceof MultiLineString){
				double distance = 0;
				MultiLineString mls = (MultiLineString)g;
				for (int i = 0; i < mls.getNumGeometries(); i ++){
					distance += distanceInMeters((LineString)mls.getGeometryN(i));
				}
				return distance;
			}else if (g instanceof GeometryCollection){
				double distance = 0;
				GeometryCollection gc = (GeometryCollection)g;
				for (int i = 0; i < gc.getNumGeometries(); i ++){
					if (gc.getGeometryN(i) instanceof LineString){
						distance += distanceInMeters((LineString)gc.getGeometryN(i));
					}else if (gc.getGeometryN(i) instanceof MultiLineString){
						MultiLineString mls = (MultiLineString)gc.getGeometryN(i);
						for (int k = 0; k < mls.getNumGeometries(); k ++){
							distance += distanceInMeters((LineString)mls.getGeometryN(k));
						}		
					}
				}
				return distance;
			}
			return 0;
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
	
	/**
	 * In this application the Envelop is encode has WKT using the Multipoint syntax.
	 *  
	 * @return envelop as WKT MULTIPOINT ( ( x y), (x y) )
	 * @throws ParseException 
	 */
	public static String envelopToWKT(Envelope envelope) throws ParseException{

		StringBuilder sb = new StringBuilder(40);

		sb.append("MULTIPOINT ((") //$NON-NLS-1$
				.append(envelope.getMaxX()).append(" ").append(envelope.getMaxY()) //$NON-NLS-1$
				.append("),(") //$NON-NLS-1$
				.append(envelope.getMinX()).append(" ").append(envelope.getMinY()) //$NON-NLS-1$
				.append("))"); //$NON-NLS-1$

		return sb.toString();
	}
	
	
	/**
	 * Transforms the envelop, encoded as MULTIPOINT, to an Envelop object
	 * 
	 * @param wktEnvelope MULTIPOINT ( ( x y), (x y) )
	 * @return {@link Envelope}
	 * @throws ParseException 
	 */
	public static Envelope wktToEnvelop(final String wktEnvelope) throws ParseException{
		
		WKTReader wkt = new WKTReader();
		MultiPoint mp = (MultiPoint) wkt.read(wktEnvelope);

		assert mp.getCoordinates().length == 2;
		
		Coordinate p1 = mp.getCoordinates()[0];
		Coordinate p2 = mp.getCoordinates()[1];
		
		Envelope envelope = new Envelope(p1, p2);
		
		return envelope;
		
	}
	
	/**
	 * Computes the distance in meter of the given
	 * linestring.
	 * 
	 * Linestring must be in 4326 projection.
	 * 
	 * @param ls
	 * @return
	 * @throws TransformException 
	 */
	public static double distanceInMeters(LineString ls, CoordinateReferenceSystem crs) throws TransformException{
		GeodeticCalculator cal = new GeodeticCalculator(crs);
		double distance = 0;
		for (int i = 1; i < ls.getCoordinates().length; i ++){
			cal.setStartingPosition(  JTS.toDirectPosition( new Coordinate(ls.getCoordinateN(i-1).x, ls.getCoordinateN(i-1).y), crs ));
			cal.setDestinationPosition( JTS.toDirectPosition( new Coordinate(ls.getCoordinateN(i).x, ls.getCoordinateN(i).y), crs ));
			
			distance +=cal.getOrthodromicDistance();
		}
		return distance;
	
	}
	
	/**
	 * Computes the distance in  meters of the given linestring
	 * in 4326 projections.
	 * @param ls
	 * @return
	 * @throws TransformException
	 */
	public static double distanceInMeters(LineString ls) {
		GeodeticCalculator cal = new GeodeticCalculator();
		double distance = 0;
		for (int i = 1; i < ls.getCoordinates().length; i ++){
			cal.setStartingGeographicPoint(ls.getCoordinateN(i-1).x, ls.getCoordinateN(i-1).y);
			cal.setDestinationGeographicPoint(ls.getCoordinateN(i).x, ls.getCoordinateN(i).y);
			
			distance +=cal.getOrthodromicDistance();
		}
		return distance;
	}
	
	
	/**
	 * Computes the minimum distance in meters from between a point and a linestring
	 * in 4326 projections.
	 * @param ls
	 * @param p
	 * @return
	 * @throws TransformException
	 */
	public static double distanceInMeters(LineString ls, Point p) {
        LocationIndexedLine li = new LocationIndexedLine(ls);
        LinearLocation loc = li.project(p.getCoordinate());
        Coordinate c = li.extractPoint(loc);
       
        GeometryFactory fact = new GeometryFactory();
        Point closestPoint = fact.createPoint(c);

		return distanceInMeters(closestPoint, p);
	}

	/**
	 * Computes the distance in meters of the two points
	 * in 4326 projections.
	 * @param ls
	 * @return
	 * @throws TransformException
	 */
	private static double distanceInMeters(Point closestPoint, Point p) {
		GeodeticCalculator cal = new GeodeticCalculator();
		cal.setStartingGeographicPoint(closestPoint.getX(),closestPoint.getY());
		cal.setDestinationGeographicPoint(p.getX(), p.getY());
		return cal.getOrthodromicDistance();
		
	}
	
	/**
	 * Splits the specified lineString at the nearest point to
	 * the linestring specific by the coordinate.
	 *  
	 * @param lineString
	 * @param points
	 * 
	 */
	public static LineString[] splitSimple(LineString lineString, Coordinate point) {
		LocationIndexedLine ll = new LocationIndexedLine(lineString);
		LinearLocation loc = ll.indexOf(point);
		LineString l1 = (LineString) ll.extractLine(ll.getStartIndex(), loc);
		LineString l2 = (LineString) ll.extractLine(loc, ll.getEndIndex());
		return new LineString[]{l1, l2};
	}
}
