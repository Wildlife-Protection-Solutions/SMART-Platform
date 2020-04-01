/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.model;

import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.wcs.smart.map.GeometryFactoryProvider;

/**
 * Navigation layer targets.  These can either be points or linestrings.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class NavigationTarget {

	private static final String STROKE_STYLE_KEY = "stroke-style"; //$NON-NLS-1$
	private static final String STROKE_SIZE_KEY = "stroke-size"; //$NON-NLS-1$
	private static final String STROKE_COLOR_KEY = "stroke-color"; //$NON-NLS-1$
	private static final String MARKER_STYLE_KEY = "marker-style"; //$NON-NLS-1$
	private static final String MARKER_SIZE_KEY = "marker-size"; //$NON-NLS-1$
	private static final String MARKER_COLOR_KEY = "marker-color"; //$NON-NLS-1$
	private static final String GEOJSON_STYLE_KEY = "style"; //$NON-NLS-1$
	private static final String GEOJSON_TYPE_FEATURE_KEY = "Feature"; //$NON-NLS-1$
	private static final String POINT_GEOMTYPE = "Point"; //$NON-NLS-1$
	private static final String LINESTRING_GEOMTYPE = "LineString"; //$NON-NLS-1$
	private static final String GEOJSON_COORDS_KEY = "coordinates"; //$NON-NLS-1$
	private static final String GEOJSON_GEOMETRY_KEY = "geometry"; //$NON-NLS-1$
	private static final String GEOJSON_ID_KEY = "id"; //$NON-NLS-1$
	private static final String GEOJSON_PROPERTIES_KEY = "properties"; //$NON-NLS-1$
	private static final String GEOJSON_TYPE_KEY = "type"; //$NON-NLS-1$
	
	private String id;
	private String uuid;
	private Geometry geometry;
	
	
	private String color= null;
	private int size = -1;
	private String styletype;
	
	public NavigationTarget() {
		uuid = UUID.randomUUID().toString();
	}
	
	public NavigationTarget(String id, Point geometry) {
		this(id, geometry, null, -1, null);
	}
	
	public NavigationTarget(String id, Point geometry, String color, int size, String style) {
		this();
		this.id = id;
		this.geometry = geometry;
		this.color = color;
		this.size = size;
		this.styletype = style;
	}
	
	public NavigationTarget(String id, LineString geometry) {
		this(id, geometry, null, -1, null);
	}
	
	public NavigationTarget(String id, LineString geometry, String color, int size, String style) {
		this();
		this.id = id;
		this.geometry = geometry;
		this.color = color;
		this.size = size;
		this.styletype = style;
	}

	public void setStyle(String color, int size, String style) {
		this.color = color;
		this.size = size;
		this.styletype = style;
	}
	/**
	 * system generated transient unqiue identifier
	 * @return
	 */
	public String getUuid() {
		return this.uuid;
	}
	
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getColor() {
		return this.color;
	}
	public int getSize() {
		return this.size;
	}
	public String getStyle() {
		return this.styletype;
	}
	
	public Geometry getGeometry() {
		return this.geometry;
	}
	
	/**
	 * 
	 * @param geometry must be point or linestring
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	
	/**
	 * 
	 * @return true if point target
	 */
	public boolean isPoint() {
		return geometry instanceof Point;
	}
	/**
	 * 
	 * @return true if linear target
	 */
	public boolean isLine() {
		return geometry instanceof LineString;
	}
	
	/**
	 * Parses a json feature into a navigation target.  JSON
	 * feature must be a line or point.  Returns null
	 * if cannot parse feature
	 * 
	 * { "type": "Feature",
	 *   "geometry": {"type": "Point", "coordinates": [102.0, 0.5] },
	 *   "properties": {"id": "value0" } }
	 *     
	 * @param json
	 * @return
	 */
	public static NavigationTarget parse(JSONObject json) {
		if (!json.containsKey(GEOJSON_TYPE_KEY)) return null;
		if (!json.get(GEOJSON_TYPE_KEY).toString().equalsIgnoreCase(GEOJSON_TYPE_FEATURE_KEY)) return null;
		
		JSONObject properties = (JSONObject) json.get(GEOJSON_PROPERTIES_KEY);
		if (properties == null) return null;
		
		String id = (String) properties.get(GEOJSON_ID_KEY);
		if (id == null) return null;
		
		JSONObject geom = (JSONObject)json.get(GEOJSON_GEOMETRY_KEY);
		if (geom == null) return null;
		
		String type = (String) geom.get(GEOJSON_TYPE_KEY);
		if (type == null) return null;
		if (!type.equalsIgnoreCase(POINT_GEOMTYPE) && !type.equalsIgnoreCase(LINESTRING_GEOMTYPE)) return null;
		
		
		JSONObject style = (JSONObject) json.get(GEOJSON_STYLE_KEY);
		String color = "FF0000"; //$NON-NLS-1$
		String itemstyle = null;
		int size = 6;
		
				
		JSONArray coords = (JSONArray) geom.get(GEOJSON_COORDS_KEY);
		
		if (coords == null) return null;
		if (type.equalsIgnoreCase(POINT_GEOMTYPE)) {
			double x = (double) coords.get(0);
			double y = (double) coords.get(1);
			
			Point p = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x,y));
			
			if (style != null) {
				color = (String) style.get(MARKER_COLOR_KEY);
				size = ((Long) style.get(MARKER_SIZE_KEY)).intValue();
				itemstyle = (String)style.get(MARKER_STYLE_KEY);
			}else {
				itemstyle = "circle"; //$NON-NLS-1$
			}
			return new NavigationTarget(id, p, color, size, itemstyle);
		}
		if (type.equalsIgnoreCase(LINESTRING_GEOMTYPE)) {
			Coordinate[] items = new Coordinate[coords.size()];
			for (int i = 0; i < coords.size(); i ++) {
				JSONArray c = (JSONArray) coords.get(i);
				double x = (double) c.get(0);
				double y = (double) c.get(1);
				items[i] = new Coordinate(x,y);
			}
			LineString ls = GeometryFactoryProvider.getFactory().createLineString(items);
			if (style != null) {
				color = (String) style.get(STROKE_COLOR_KEY);
				size = ((Long) style.get(STROKE_SIZE_KEY)).intValue();
				itemstyle = (String)style.get(STROKE_STYLE_KEY);
			}else {
				itemstyle = "soild"; //$NON-NLS-1$
			}
			return new NavigationTarget(id, ls, color, size, itemstyle);
		}
		return null;
		
	}
	/**
	 * Converts a navigation target to a json feature.  
	 * 
	 * { "type": "Feature",
	 *   "geometry": {"type": "Point", "coordinates": [102.0, 0.5] },
	 *   "properties": {"id": "value0" } }
	 *     
	 * @param json
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		object.put(GEOJSON_TYPE_KEY, GEOJSON_TYPE_FEATURE_KEY);
		
		JSONObject style = new JSONObject();
		
		JSONObject geom = new JSONObject();
		if (geometry instanceof Point) {
			geom.put(GEOJSON_TYPE_KEY, POINT_GEOMTYPE);
			JSONArray c = new JSONArray();
			c.add(((Point)getGeometry()).getX());
			c.add(((Point)getGeometry()).getY());
			geom.put(GEOJSON_COORDS_KEY, c);
			
			style.put(MARKER_SIZE_KEY, size);
			style.put(MARKER_COLOR_KEY, color);
			style.put(MARKER_STYLE_KEY, styletype);
		}else if (geometry instanceof LineString) {
			geom.put(GEOJSON_TYPE_KEY, LINESTRING_GEOMTYPE);
			JSONArray cs = new JSONArray();
			for (int i = 0; i < ((LineString)geometry).getNumPoints(); i ++) {
				Coordinate pnt = ((LineString)geometry).getCoordinateN(i);
				JSONArray c = new JSONArray();
				c.add(pnt.getX());
				c.add(pnt.getY());
				cs.add(c);
			}
			geom.put(GEOJSON_COORDS_KEY, cs);
			
			style.put(STROKE_SIZE_KEY, size);
			style.put(STROKE_COLOR_KEY, color);
			style.put(STROKE_STYLE_KEY, styletype);
		}
		object.put(GEOJSON_GEOMETRY_KEY, geom);
		
		JSONObject properties = new JSONObject();
		properties.put(GEOJSON_ID_KEY,  getId());
		object.put(GEOJSON_PROPERTIES_KEY, properties);
		object.put(GEOJSON_STYLE_KEY,  style);
		return object;
	}
}
