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
package org.wcs.smart.cybertracker.export.mbtile;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * Mbtile representation
 * 
 * @author Emily
 *
 */
public class Tile {

	private int tilex;
	private int tiley;
	private ZoomLevel zoom;

	public Tile(int tilex, int tiley, ZoomLevel zoom) {
		this.zoom = zoom;
		this.tilex = tilex;
		this.tiley = tiley;
	}

	/**
	 * the zoom level
	 * @return
	 */
	public ZoomLevel getZoom() {
		return this.zoom;
	}
	
	/**
	 * the x tile value
	 * @return
	 */
	public int getTileX() {
		return this.tilex;
	}
	
	/**
	 * The y tile value
	 * @return
	 */
	public int getTileY() {
		return this.tiley;
	}

	/**
	 * 
	 * @return the tile bounds in lat long
	 */
	public Envelope getBoundsLatLong() {
		return Tile.getBoundsLatLong(tilex, tiley, zoom.getZoom());
	}

	/**
	 * 
	 * @return the tile in spherical mercator
	 */
	public Envelope getBoundsMercator() {
		Envelope ee = getBoundsLatLong();
		Envelope e = new Envelope(toMercator(ee.getMinX(), ee.getMinY()), toMercator(ee.getMaxX(), ee.getMaxY()));
		return e;
	}
	
	private Coordinate toMercator(double x, double y) {
		x = 6378137.0 * Math.toRadians(x);
		if (y <= -90 ) { 
			y = Double.NEGATIVE_INFINITY;
		}else if (y >= 90) {
			y = Double.POSITIVE_INFINITY;
		}else {
			y = 6378137.0 * Math.log(
			Math.tan((Math.PI * 0.25) + (0.5 * Math.toRadians(y))));
		}
		return new Coordinate(x,y);
	}
	
	/**
	 * 	
	 * @return the wkt polygon representation of the tile bounds in lat/long
	 */
	public String asWktLatLong() {
		Envelope e = getBoundsMercator();
		return "POLYGON((" + e.getMinX() + " " + e.getMinY() + "," + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			e.getMinX() + " " + e.getMaxY() + "," + //$NON-NLS-1$ //$NON-NLS-2$
			e.getMaxX() + " " + e.getMaxY() + "," + //$NON-NLS-1$ //$NON-NLS-2$
			e.getMaxX() + " " + e.getMinY() + "," + //$NON-NLS-1$ //$NON-NLS-2$
			e.getMinX() + " " + e.getMinY() + "))"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * 	
	 * @return the wkt polygon representation of the tile bounds in mercator
	 */
	public String asWktMercator() {
		Envelope e = getBoundsLatLong();
		return "POLYGON((" + e.getMinX() + " " + e.getMinY() + "," + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			e.getMinX() + " " + e.getMaxY() + "," + //$NON-NLS-1$ //$NON-NLS-2$
			e.getMaxX() + " " + e.getMaxY() + "," + //$NON-NLS-1$ //$NON-NLS-2$
			e.getMaxX() + " " + e.getMinY() + "," + //$NON-NLS-1$ //$NON-NLS-2$
			e.getMinX() + " " + e.getMinY() + "))"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static Envelope getBoundsLatLong(int tilex, int tiley, int zoom) {
		double north = Math.toDegrees(Math.atan(Math.sinh( (Math.PI - (2.0 * Math.PI * tiley) / Math.pow(2.0, zoom)) )));
		double south = Math.toDegrees(Math.atan(Math.sinh( (Math.PI - (2.0 * Math.PI * (tiley + 1)) / Math.pow(2.0, zoom)) )));
		double west = tilex / Math.pow(2.0, zoom) * 360.0 - 180;
		double east = (tilex + 1) / Math.pow(2.0, zoom) * 360.0 - 180;
		
		return new Envelope(west, east, south, north);
	}
}
