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

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Mbtile zoomlevel
 * 
 * @author Emily
 *
 */
public class ZoomLevel {

	private Layer layer;
	private int zoom;
	private List<Tile> tiles = null;

	public ZoomLevel(Layer layer, int zoom) {
		this.layer = layer;
		this.zoom = zoom;
		
		tiles = new ArrayList<>();

		int startx = layer.getMinZoomMinTiles()[0] * (int)Math.pow(2,  (zoom - layer.getMinZoom()) );
		int starty = layer.getMinZoomMaxTiles()[1] * (int)Math.pow(2,  (zoom - layer.getMinZoom()) );
		
		int endx = (layer.getMinZoomMaxTiles()[0] + 1) * (int)Math.pow(2,  (zoom - layer.getMinZoom()) ) - 1;
		int endy = (layer.getMinZoomMinTiles()[1] + 1)* (int)Math.pow(2,  (zoom - layer.getMinZoom()) ) - 1;
		
		for (int x = startx; x <= Math.min(endx, Math.pow(2, zoom)); x++) {
			for (int y = starty; y <= Math.min(endy, Math.pow(2, zoom)); y++) {
				Tile tile = new Tile(x, y, this);
				tiles.add(tile);
			}
		}
	}

	/**
	 * 
	 * @return the layer associated with the zoomlevel
	 */
	public Layer getLayer() {
		return this.layer;
	}
	
	/**
	 * The minimum x tile for the zoomlevel and bounds
	 * @return
	 */
	public int getMinTileX() {
		return tiles.get(0).getTileX();
	}

	/**
	 * The maximum x tile for the zoomlevel and bounds
	 * @return
	 */
	public int getMaxTileX() {
		return tiles.get(tiles.size() - 1).getTileX();
	}
	
	/**
	 * The minimum y tile for the zoomlevel and bounds
	 * @return
	 */
	public int getMinTileY() {
		return tiles.get(0).getTileY();
	}

	/**
	 * The maximum y tile for the zoomlevel and bounds
	 * @return
	 */
	public int getMaxTileY() {
		return tiles.get(tiles.size() - 1).getTileY();
	}
	
	/**
	 * Finds the tile with the given tilex and tiley values.  Returns
	 * null if tile not found
	 * 
	 * @param tilex
	 * @param tiley
	 * @return
	 */
	public Tile getTile(int tilex, int tiley) {
		for (Tile t : getTiles()) {
			if (t.getTileX() == tilex && t.getTileY() == tiley) return t;
		}
		return null;
	}

	/**
	 * 
	 * @return the zoom level
	 */
	public int getZoom() {
		return this.zoom;
	}

	/**
	 * 
	 * @return the bounds of all the tiles in the zoom level
	 */
	public Envelope getZoomBounds() {
		Envelope env = null;
		for (Tile t : getTiles()) {
			if (env == null) {
				env = t.getBoundsLatLong();
			} else {
				env.expandToInclude(t.getBoundsLatLong());
			}
		}
		return env;
	}

	/**
	 * Gets all the tiles in the zoom level
	 * @return
	 */
	public List<Tile> getTiles() {
		return this.tiles;
	}

	/*
	 * converts lon/lat to a tile x and tile y
	 */
	public static int[] toTile(double x, double y, int zoom) {
		double lon = x;
		double lat = y;

		int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
		int ytile = (int) Math
				.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2
						* (1 << zoom));
		if (xtile < 0)
			xtile = 0;
		if (xtile >= (1 << zoom))
			xtile = ((1 << zoom) - 1);
		if (ytile < 0)
			ytile = 0;
		if (ytile >= (1 << zoom))
			ytile = ((1 << zoom) - 1);
		return new int[] { xtile, ytile };
	}

}
