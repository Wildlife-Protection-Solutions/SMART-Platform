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

/**
 * Mbtile layer representation.
 * 
 */
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Envelope;

public class Layer {

	private String name;
	private List<ZoomLevel> zooms;
	private Envelope env;
	
	private int[] minZoomMinTiles;
	private int[] minZoomMaxTiles;
	
	private int minZoom;
	private int maxZoom;
	
	public Layer(String name, Envelope bounds, int minZoom, int maxZoom) {
		this.name = name;
		this.env = bounds;
		
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		
		zooms = new ArrayList<>();
		
		int[] bndsminTile = ZoomLevel.toTile(bounds.getMinX(), bounds.getMinY(), minZoom);
		int[] bndsmaxTile = ZoomLevel.toTile(bounds.getMaxX(), bounds.getMaxY(), minZoom);
		
		int xMinTile = bndsminTile[0];
		int yMinTile = bndsminTile[1];
		int xMaxTile = bndsmaxTile[0];
		int yMaxTile = bndsmaxTile[1];

		minZoomMinTiles = new int[] {xMinTile, yMinTile};
		minZoomMaxTiles = new int[] {xMaxTile, yMaxTile};
		
		for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
			zooms.add(new ZoomLevel(this, zoom));
		}
	}
	
	/**
	 * The x,y tiles representing the minimum tiles for the minimum zoom level 
	 * @return
	 */
	public int[] getMinZoomMinTiles() {
		return minZoomMinTiles;
	}
	/**
	 * The x,y tiles representing the maximum tiles for the minimum zoom level 
	 * @return
	 */
	public int[] getMinZoomMaxTiles() {
		return minZoomMaxTiles;
	}
	
	/**
	 * Layer bounds
	 * @return
	 */
	public Envelope getBounds() {
		return this.env;
	}
	
	/**
	 * Layer name
	 * @return
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * The minimum zoom level
	 * @return
	 */
	public int getMinZoom() {
		return minZoom;
	}
	
	/**
	 * The maximum zoom level
	 * @return
	 */
	public int getMaxZoom() {
		return maxZoom;
	}
	
	public List<ZoomLevel> getZooms(){
		return this.zooms;
	}
}
