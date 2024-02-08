/*
 * Copyright (C) 2024 Wildlife Conservation Society
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.wcs.smart.map.GeometryFactoryProvider;

/**
 * Utilties for converting geometry objects to geometry geojson
 * format
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class GeoJsonUtil {

	/**
	 * Parses the "geometry" part of JSONObject into geometry.
	 *  
	 *  Assumes 2d geometry
	 * @param geometry
	 * @return
	 */
	public static Geometry toJTSGeometry(JSONObject geometry) {
		String type = geometry.get("type").toString().toUpperCase(); //$NON-NLS-1$
		if (type.equals("POINT")){ //$NON-NLS-1$
			return parsePoint(geometry);
		}else if (type.equals("MULTIPOINT")){ //$NON-NLS-1$
			return parseMultiPoint(geometry);
		}else if (type.equals("LINESTRING")){ //$NON-NLS-1$
			return parseLineString(geometry);
		}else if (type.equals("MULTILINESTRING")){ //$NON-NLS-1$
			return parseMultiLineString(geometry);
		}else if (type.equals("POLYGON")){ //$NON-NLS-1$
			return parsePolygon(geometry);
		}else if (type.equals("MULTIPOLYGON")){ //$NON-NLS-1$
			return parseMultiPolygon(geometry);
		}
		return null;
	}
	
	private static Point parsePoint(JSONObject geometry) {
		Coordinate c = parseCoordinate((JSONArray) geometry.get("coordinates")); //$NON-NLS-1$
		return GeometryFactoryProvider.getFactory().createPoint(c);
	}
	
	private static MultiPoint parseMultiPoint(JSONObject geometry) {
		Coordinate[] c = parseCoordinateArray((JSONArray) geometry.get("coordinates")); //$NON-NLS-1$
		return GeometryFactoryProvider.getFactory().createMultiPointFromCoords(c);
	}
	
	private static LineString parseLineString(JSONObject geometry) {
		Coordinate[] c = parseCoordinateArray((JSONArray) geometry.get("coordinates")); //$NON-NLS-1$
		return GeometryFactoryProvider.getFactory().createLineString(c);
	}
	
	private static MultiLineString parseMultiLineString(JSONObject geometry) {
		JSONArray ls = (JSONArray)geometry.get("coordinates"); //$NON-NLS-1$
	
		LineString[] items = new LineString[ls.size()];
		for (int i = 0; i < ls.size(); i ++) {
			Coordinate[] c = parseCoordinateArray((JSONArray) ls.get(i));
			items[i] = GeometryFactoryProvider.getFactory().createLineString(c);
		}
		return GeometryFactoryProvider.getFactory().createMultiLineString(items);
	}
	
	private static  Polygon parsePolygon(JSONObject geometry) {
		JSONArray ls = (JSONArray)geometry.get("coordinates"); //$NON-NLS-1$
		
		LinearRing[] items = new LinearRing[ls.size()-1];
		LinearRing outer = GeometryFactoryProvider.getFactory().createLinearRing( parseCoordinateArray((JSONArray)ls.get(0)));
		for (int i = 1; i < ls.size(); i ++) {
			Coordinate[] c = parseCoordinateArray((JSONArray) ls.get(i));
			items[i-1] = GeometryFactoryProvider.getFactory().createLinearRing( c );
		}
		
		return GeometryFactoryProvider.getFactory().createPolygon(outer,  items);
	}
	
	private static MultiPolygon parseMultiPolygon(JSONObject geometry) {
		JSONArray polygons = (JSONArray)geometry.get("coordinates"); //$NON-NLS-1$
		
		Polygon[] polygon = new Polygon[polygons.size()];
		
		for (int k = 0; k < polygons.size(); k ++) {
			JSONArray ls = (JSONArray) polygons.get(k);
		
			LinearRing[] items = new LinearRing[ls.size()-1];
			LinearRing outer = GeometryFactoryProvider.getFactory().createLinearRing( parseCoordinateArray((JSONArray)ls.get(0)));
			for (int i = 1; i < ls.size(); i ++) {
				Coordinate[] c = parseCoordinateArray((JSONArray) ls.get(i));
				items[i-1] = GeometryFactoryProvider.getFactory().createLinearRing( c );
			}
		
			polygon[k] = GeometryFactoryProvider.getFactory().createPolygon(outer,  items);
		}
		return GeometryFactoryProvider.getFactory().createMultiPolygon(polygon);
	}
	
	private static Coordinate[] parseCoordinateArray(JSONArray array) {
		Coordinate[] coordinates = new Coordinate[array.size()];
		for (int i = 0; i < array.size(); i ++) {
			coordinates[i] = parseCoordinate((JSONArray) array.get(i));
		}
		return coordinates;
	}
	private static Coordinate parseCoordinate(JSONArray array) {
		double x = ((Number) array.get(0)).doubleValue();
		double y = ((Number) array.get(1)).doubleValue();
		return new Coordinate(x,y);
	}
	
	/**
	 * Converts a JTS geometry to the "geometry" JSON object 
	 * in geojson format. For example: { "type": "Point","coordinates": [125.6, 10.1]  }
	 * 
	 * Outputs 2d geometry
	 * 
	 * @param g
	 * @return
	 */
	public static JSONObject toGeoJSONGeometry(Geometry g) {
		if (g instanceof LineString ls) {
			return toGeometry(ls);
		}else if (g instanceof MultiLineString mls) {
			return toGeometry(mls);
		}else if (g instanceof Polygon p) {
			return toGeometry(p);
		}else if (g instanceof MultiPolygon mp) {
			return toGeometry(mp);
		}else if (g instanceof Point p) {
			return toGeometry(p);
		}else if (g instanceof MultiPoint mp) {
			return toGeometry(mp);
		}
		return null;
	}
	
	private static JSONObject toGeometry(LineString ls) {
		JSONObject geom = new JSONObject();
		geom.put("type", "LineString"); //$NON-NLS-1$ //$NON-NLS-2$
		geom.put("coordinates", toArray(ls.getCoordinates())); //$NON-NLS-1$
		return geom;		
	}
		
	private static JSONObject toGeometry(MultiLineString g) {
		JSONObject geom = new JSONObject();
		geom.put("type", "MultiLineString"); //$NON-NLS-1$ //$NON-NLS-2$
		
		JSONArray outer = new JSONArray();
		for (int i = 0; i < g.getNumGeometries(); i ++) {
			LineString ls = (LineString) g.getGeometryN(i);
			outer.add(toArray(ls.getCoordinates()));			
		}
		
		geom.put("coordinates", outer); //$NON-NLS-1$
		return geom;		
	}
	
	private static JSONObject toGeometry(Polygon g) {
		JSONObject geom = new JSONObject();
		geom.put("type", "Polygon"); //$NON-NLS-1$ //$NON-NLS-2$
		
		JSONArray outer = new JSONArray();
		outer.add(toArray(g.getExteriorRing().getCoordinates()));
		for (int i = 0; i < g.getNumInteriorRing(); i ++) {
			LinearRing ls = (LinearRing) g.getInteriorRingN(i);
			outer.add(toArray(ls.getCoordinates()));			
		}
		
		geom.put("coordinates", outer); //$NON-NLS-1$
		return geom;		
	}
	
	private static JSONObject toGeometry(MultiPolygon g) {
		JSONObject geom = new JSONObject();
		geom.put("type", "MultiPolygon"); //$NON-NLS-1$ //$NON-NLS-2$
		
		JSONArray outer = new JSONArray();
		for (int i = 0; i < g.getNumGeometries(); i ++) {
			Polygon p = (Polygon) g.getGeometryN(i);
			
			JSONArray outer2 = new JSONArray();
			outer2.add(toArray(p.getExteriorRing().getCoordinates()));
			for (int j = 0; j < p.getNumInteriorRing(); j ++) {
				LinearRing ls = (LinearRing) p.getInteriorRingN(j);
				outer2.add(toArray(ls.getCoordinates()));			
			}	
			outer.add(outer2);
		}
		
		geom.put("coordinates", outer); //$NON-NLS-1$
		return geom;		
	}
	
	private static JSONObject toGeometry(Point p) {
		JSONObject geom = new JSONObject();
		geom.put("type", "Point"); //$NON-NLS-1$ //$NON-NLS-2$
		geom.put("coordinates", toArray(p.getCoordinate())); //$NON-NLS-1$
		return geom;		
	}
	
	
	private static JSONObject toGeometry(MultiPoint p) {
		JSONObject geom = new JSONObject();

		JSONArray outer = new JSONArray();
		for (int i = 0; i < p.getNumGeometries(); i ++) {
			Point ls = (Point) p.getGeometryN(i);
			outer.add(toArray(ls.getCoordinate()));			
		}
		
		geom.put("type", "MultiPoint"); //$NON-NLS-1$ //$NON-NLS-2$
		geom.put("coordinates", outer); //$NON-NLS-1$
		return geom;		
	}
	
	private static JSONArray toArray(Coordinate[] coordinates) {
		JSONArray array = new JSONArray();
		for (Coordinate c : coordinates) {
			array.add(toArray(c));
		}
		return array;
		
	}
	
	private static JSONArray toArray(Coordinate c) {
		JSONArray inner = new JSONArray();
		inner.add(c.x);
		inner.add(c.y);
		return inner;
	}
	
//	public static void main (String[] args) throws Exception{
//		String point = "POINT (199 510)";
//		String mpoint = "MULTIPOINT (199 510, 259 444)";
//		String ls = "LINESTRING (366 539, 618 400)";
//		String mls = "MULTILINESTRING ((367 330, 486 537), (366 539, 618 400))";
//		String polygon = "POLYGON ((613 602, 787 636, 828 469, 721 453, 641 510, 613 602), (737 522, 771 523, 758 574, 737 522), (708 537, 715 578, 660 577, 708 537))";
//		String mpolygon = "MULTIPOLYGON (((613 602, 787 636, 828 469, 721 453, 641 510, 613 602), (737 522, 771 523, 758 574, 737 522), (708 537, 715 578, 660 577, 708 537)), ((764 351, 842 350, 821 239, 752 242, 764 351)))";
//		
//		
//		String[] all = new String[] {point, mpoint, ls, mls, polygon, mpolygon};
//		
//		for (String s : all) {
//			Geometry g = (new WKTReader()).read(s);
//			JSONObject obj = GeoJsonUtil.toGeoJSONGeometry(g);
//			System.out.println(obj.toJSONString());
//		}
//		System.out.println("==========");
//		
//		String jpoint = "{\"geometry\": {\"type\": \"Point\",\"coordinates\": [125.6, 10.1]  }}";
//		String jmpoint = "{\"geometry\": {\"type\": \"MultiPoint\",\"coordinates\": [[125.6, 10.1],[123.6, 10.2]]  }}";
//		String jline = "{\"geometry\": {\"type\": \"LineString\",\"coordinates\": [[366, 539], [618,400]]  }}";
//		String jmline = "{\"geometry\": {\"type\": \"MultiLineString\",\"coordinates\": [[[367, 330], [486,537]], [[366 539], [618 400]]] }}";
//		String jpolygon = "{\"geometry\": {\"type\": \"Polygon\",\"coordinates\": [[[613 602],[787,636],[828,469],[721,453],[641,510],[613,602]],[[737,522],[771,523],[758,574],[737,522]],[[708,537],[715,578],[660,577],[708,537]]] }}";
//		String jmpolygon = "{\"geometry\": {\"type\": \"MultiPolygon\",\"coordinates\": [[[[613 602],[787,636],[828,469],[721,453],[641,510],[613,602]],[[737,522],[771,523],[758,574],[737,522]],[[708,537],[715,578],[660,577],[708,537]]],[[[764,351],[842,350],[821,239],[752,242],[764,351]]]] }}";
//		
//		String[] jall = new String[] {jpoint, jmpoint, jline, jmline, jpolygon, jmpolygon};
//		for (String s : jall) {
//			JSONObject o = (JSONObject) ((JSONObject) (new JSONParser()).parse(s)).get("geometry");
//			Geometry g = GeoJsonUtil.toJTSGeometry(o);
//			System.out.println(g.toText());
//		}
//		System.out.println("==========");
//
//		
//		for (String s : all) {
//			Geometry g = (new WKTReader()).read(s);
//			JSONObject obj = GeoJsonUtil.toGeoJSONGeometry(g);
//			Geometry g2 = GeoJsonUtil.toJTSGeometry(obj);
//			System.out.println(g.equalsExact(g2));
//		}
//	}
}
