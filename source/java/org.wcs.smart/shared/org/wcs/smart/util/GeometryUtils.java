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
import java.text.MessageFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.rowset.serial.SerialBlob;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.GeometryAttributeValue;
import org.wcs.smart.map.GeometryFactoryProvider;

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
	
	//earth raidus in meters
	public static final int EARTH_RADIUS = 6378100;//6371000;//6378.1;

	public static CoordinateReferenceSystem SMART_CRS;
	static{
		try {
			SMART_CRS = CRS.decode("EPSG:4326", true); //$NON-NLS-1$
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	} 
	
	private static WKBReader wkbreader(){
		return wkbreader(false);
	}
	
	private static WKBReader wkbreader(boolean withoutPm){
		if (withoutPm) {
			return new WKBReader(GeometryFactoryProvider.getFactoryWithoutPrecisionModel());
		}else {
			return new WKBReader(GeometryFactoryProvider.getFactory());
		}
	}
	
	
	private static WKBWriter wkbwriter(){
		return new WKBWriter();
	}
	
	
	/**
	 * Converts and x and y position into a string
	 * representing the position.

	 * @param x
	 * @param y
	 * 
	 * @return
	 */
	public static String toPoint(Double x, Double y){
		return "(" + x + "," + y + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	public static boolean pointInPolygon(Double x, Double y, Float distance, Float direction, Blob wkb){
		try {
			return pointInPolygon(x, y, distance, direction, wkb.getBytes(1, (int)wkb.length()));
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
	public static boolean pointInPolygon(Double x, Double y, Float distance, Float direction, byte[] wkb){
		if (wkb == null || x == null || y == null) return false;
		Coordinate c = null;
		if (distance != null && direction != null) {
			c = projectPoint(new Coordinate(x,y), distance, direction);
		}else {
			c = new Coordinate(x,y);
		}
		Geometry geom = gFromWKB(wkb);
		return geom.intersects(GeometryFactoryProvider.getFactory().createPoint(c));
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
	 * Determines if a linestring intersects and area.  This accounts
	 * for the case where the linestring is of zero length.
	 * 
	 * @param track
	 * @param area
	 * @return
	 */
	public static boolean trackIntersects (Blob track, Blob area){
		if (track == null || area == null) return false;
		try{
			Geometry g1 = gFromWKB(track.getBytes(1, (int)track.length()));
			if (g1.isEmpty()) return false;
			if (g1.getLength() == 0){
				return pointInPolygon(g1.getCoordinate().x, g1.getCoordinate().y, null, null, area);
			}
			Geometry g2 = gFromWKB(area.getBytes(1, (int)area.length()));
			return g1.intersects(g2);
		}catch (SQLException e){
			throw new RuntimeException ( e );
		}
	}
	
	/**
	 * Buffers the given geometry by the provided value and returns
	 * a geometry in the original smart projection 
	 * 
	 * @param wkb1
	 * @param wkb2
	 * @return
	 */
	public static Blob buffer(Blob wkb1, double bufferValue){
		if (wkb1 == null) return null;
		if (bufferValue <= 0) return wkb1;
		try{
			Geometry g1 = gFromWKB(wkb1.getBytes(1, (int)wkb1.length()));
			if (g1.isEmpty()) return null;
			
			Coordinate c = g1.getCentroid().getCoordinate();
			CoordinateReferenceSystem targetCRS = CRS.decode("AUTO2:42001,"+c.x+","+c.y); //$NON-NLS-1$ //$NON-NLS-2$
			
			Geometry g2 = JTS.transform(g1, ReprojectUtils.findMathTransform(targetCRS)).buffer(bufferValue);
			//tansform back to 
			g2 = JTS.transform(g2,CRS.findMathTransform(targetCRS, SMART_CRS));
			
			return new SerialBlob(wkbwriter().write(g2));
		}catch (Throwable e){
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
		
		Geometry g1 = null;
		Geometry g2 = null;
		try{
			g1 = gFromWKB(wkb1.getBytes(1, (int)wkb1.length()));
			g2 = gFromWKB(wkb2.getBytes(1, (int)wkb2.length()));
		}catch (Throwable e) {
			throw new RuntimeException(e);
		}
		
		try {
			Geometry g3 = g2.intersection(g1);
			return new SerialBlob(wkbwriter().write(g3));
		}catch(TopologyException te) {
			//lets try with full precision;			
			try {
				g1 = wkbreader(true).read(wkb1.getBytes(1, (int)wkb1.length()));
				g2 = wkbreader(true).read(wkb2.getBytes(1, (int)wkb2.length()));
			}catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			try {
				Geometry g3 = g2.intersection(g1);
				return new SerialBlob(wkbwriter().write(g3));
			}catch (TopologyException te2) {
				throw new RuntimeException ("A topology exception occured - try checking track geometries for cases where GPS units were not turned off at rest points and cleaning these tracks", te ); //$NON-NLS-1$
			}catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}catch (Throwable e){
 			throw new RuntimeException(e);
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
			
			Geometry mls = wkbreader().read(linestring.getBytes(1, (int)linestring.length()));
			if (mls instanceof LineString) {
				mls = GeometryFactoryProvider.getFactory().createMultiLineString(new LineString[] {(LineString)mls});
			}
			if (!(mls instanceof MultiLineString)) return 0;
			
			Geometry poly = wkbreader().read(geom.getBytes(1, (int)geom.length()));
			double sum = 0;
			for (int i = 0; i < ((MultiLineString)mls).getNumGeometries(); i ++) {
				LineString ls = (LineString)mls.getGeometryN(i);
				if (poly instanceof Polygon){
					sum += computeHours((Polygon)poly, (LineString)ls);
				}else if (poly instanceof MultiPolygon){
					sum += computeHours((MultiPolygon)poly, (LineString)ls);
				}else if (poly instanceof GeometryCollection){
					sum += computeHours((GeometryCollection)poly, (LineString)ls);
				}
			}
			return sum;
		}catch (Throwable e){
			throw new RuntimeException(e);
		}		
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
		if (ls.getLength() == 0){
			//0 length linestrings are not valid in JTS and intersection function does not always computer
			//correctly so we specifically check here the linestring point
			//see tickets 2243 & 1933
			if (p.intersects(GeometryFactoryProvider.getFactory().createPoint(ls.getCoordinate()))){
				value = ls.getCoordinateN(ls.getNumPoints() - 1).getZ() - ls.getCoordinateN(0).getZ();
			}
		}else{
			if (p.contains(ls)){
				value = ls.getCoordinateN(ls.getNumPoints() - 1).getZ() - ls.getCoordinateN(0).getZ();
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
					LineString temp = GeometryFactoryProvider.getFactory().createLineString(new Coordinate[]{c1, c2});
					if (pg.containsProperly(temp)){
						//entirely contained within	 
						value += (c2.getZ() - c1.getZ());
					}else if (pg.intersects(temp)){
						double time = c2.getZ() - c1.getZ();
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
		}
		if ((Double.valueOf(value).isNaN())) value = 0;  //this should never happen but if it doesn't lets make sure it doesn't cause an error
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
        	//return (new WKBReader(new GeometryFactory(new PrecisionModel(1_000_000_000.0)))).read(wkb);
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
	
	public static Polygon envelopeToGeometry(final Envelope env) {
		
		Coordinate c1 = new Coordinate(env.getMinX(), env.getMinY());
		Coordinate c2 = new Coordinate(env.getMinX(), env.getMaxY());
		Coordinate c3 = new Coordinate(env.getMaxX(), env.getMinY());
		Coordinate c4 = new Coordinate(env.getMinX(), env.getMaxY());
		
		return GeometryFactoryProvider.getFactory().createPolygon(new Coordinate[] {c1, c2, c3, c4, c1});
	}
	
	/**
	 * Computes the distance in meter of the given
	 * linestring.
	 * 
	 * 
	 * @param ls
	 * @param crs linestring projection
	 * 
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
	 * Computes the perimeter in  meters of the given polygon
	 * in 4326 projections.
	 * @param ls
	 * @return
	 * @throws TransformException
	 */
	public static double perimeterInMeters(Polygon p) {
		GeodeticCalculator cal = new GeodeticCalculator();
		double distance = 0;
		LinearRing outer = p.getExteriorRing();
		Coordinate[] c = outer.getCoordinates();
		for (int i = 1; i < c.length; i ++){
			cal.setStartingGeographicPoint(c[i-1].x, c[i-1].y);
			cal.setDestinationGeographicPoint(c[i].x, c[i].y);			
			distance +=cal.getOrthodromicDistance();
		}
		for (int k = 0; k < p.getNumInteriorRing(); k++) {
			LinearRing inner = p.getInteriorRingN(k);
			c = inner.getCoordinates();
			for (int i = 1; i < c.length; i ++){
				cal.setStartingGeographicPoint(c[i-1].x, c[i-1].y);
				cal.setDestinationGeographicPoint(c[i].x, c[i].y);			
				distance +=cal.getOrthodromicDistance();
			}
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
        Point closestPoint = GeometryFactoryProvider.getFactory().createPoint(c);
		return distanceInMeters(closestPoint, p);
	}

	/**
	 * Computes the distance in meters of the two points
	 * in 4326 projections.
	 * @param ls
	 * @return
	 * @throws TransformException
	 )*/
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
	
	/**
	 * Determines if the point represented by x,y,distance,direction is within
	 * the bounding box represented by x1,y1,x2,y2
	 * 
	 * @param x
	 * @param y
	 * @param distance
	 * @param direction
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static boolean waypointWithin(double x, double y, Float distance, Float direction, double x1, double y1, double x2, double y2) {
		Coordinate c = new Coordinate(x, y);
		if (distance != null && direction != null) {
			c = projectPoint(c, distance, direction);
		}
		if (x2 < x1) {
			double t = x2;
			x2 = x1;
			x1 = t;
		}
		if (y2 < y1) {
			double t = y2;
			y2 = y1;
			y1 = t;
		}
		return c.x >= x1 && c.x <= x2 && c.y >= y1 && c.y <= y2;
	}
	
	public static Coordinate projectPoint(Coordinate c, double distance, double direction) {
		double a = Math.toRadians(direction);
		double dR = distance/EARTH_RADIUS;		
		double ry = Math.toRadians(c.y);
		double rx = Math.toRadians(c.x);
		double prjy1 = Math.asin( Math.sin(ry)*Math.cos(dR) + Math.cos(ry)*Math.sin(dR)*Math.cos(a) );
		double prjx1 = rx + Math.atan2(Math.sin(a)*Math.sin(dR)*Math.cos(ry), Math.cos(dR)-Math.sin(ry)*Math.sin(prjy1));
		double prjx = Math.toDegrees(prjx1);
		double prjy = Math.toDegrees(prjy1);
		return new Coordinate(prjx, prjy);
	}
	
	/**
	 * Computes the distance and bearing between two points.
	 * 
	 * @param c1
	 * @param c2
	 * @return array where the first element is the distance in meters and the
	 * second is the bearing in degrees
	 */
	public static Float[] computeDistanceBearing(Coordinate c1, Coordinate c2) {
		//initial bearing
		c1 = new Coordinate(Math.toRadians(c1.x), Math.toRadians(c1.y));
		c2 = new Coordinate(Math.toRadians(c2.x), Math.toRadians(c2.y));
		
		double y = Math.sin(c2.x-c1.x) * Math.cos(c2.y);
		double x = Math.cos(c1.y)*Math.sin(c2.y) -
		        Math.sin(c1.y)*Math.cos(c2.y)*Math.cos(c2.x-c1.x);
		double brng = Math.toDegrees( Math.atan2(y, x) );
		brng = (brng + 360) % 360;
				
		//Distance haversine formula
		double lat1 = c1.y;//Math.toRadians(c1.y);
		double lat2 = c2.y;//Math.toRadians(c2.y);
		double latdiff = c2.y - c1.y;//= Math.toRadians(c2.y-c1.y);
		double longdiff = c2.x - c1.x; //Math.toRadians(c2.x-c1.x);
		double a = Math.sin(latdiff/2) * Math.sin(latdiff/2) +
		        Math.cos(lat1) * Math.cos(lat2) *
		        Math.sin(longdiff/2) * Math.sin(longdiff/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = EARTH_RADIUS * c;
				
		return new Float[] {(float)d, (float)brng};
	}

	
	public static String getAttributeGeometryLabel(GeometryAttributeValue value, Locale l) {
		if (value == null) return ""; //$NON-NLS-1$
		
		if (value.isPolygon()) {
			String msg = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.POLYGON_GEOM_ATTRIBUTE_LABEL, l);
			return MessageFormat.format(msg, value.getArea(), value.getPerimeter(), value.getSource().getLabel(l));
		}
		if (value.isLineString()) {
			String msg = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.LINESTRING_GEOM_ATTRIBUTE_LABEL, l);
			return MessageFormat.format(msg, value.getPerimeter(), value.getSource().getLabel(l));
		}
		return ""; //$NON-NLS-1$
	}	

	public static double getAreaInKm(MultiPolygon mp) {
		if (mp == null) return 0;
		try {
			Coordinate c = mp.getCentroid().getCoordinate();
			CoordinateReferenceSystem targetCRS = CRS.decode("AUTO2:42001," + c.x + "," + c.y); //$NON-NLS-1$ //$NON-NLS-2$
			Geometry t = JTS.transform(mp, ReprojectUtils.findMathTransform(targetCRS));
			return t.getArea() / (1_000_000.0);
		}catch (Exception ex) {
			Logger.getLogger(GeometryUtils.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
			return Double.NaN;
		}
	}

	public static double getGeometryPerimeterInKm(Geometry g) {
		if (g == null) return 0.0;
		if (g instanceof LineString) return distanceInMeters( (LineString)g) / 1000.0;
		if (g instanceof MultiLineString) {
			MultiLineString mp = (MultiLineString)g;
			double length = 0;
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				length += GeometryUtils.distanceInMeters((LineString)mp.getGeometryN(i));
			}
			return length / 1000.0;
		}
		if (g instanceof Polygon) g = GeometryFactoryProvider.getFactory().createMultiPolygon(new Polygon[] {(Polygon)g});

		if (g instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon)g;
			double outside = 0.0;
			for (int i = 0; i < mp.getNumGeometries(); i ++) {
				outside += GeometryUtils.perimeterInMeters((Polygon)mp.getGeometryN(i));
			}
			return outside / 1000.0;
		}
		return 0;
	}
}
