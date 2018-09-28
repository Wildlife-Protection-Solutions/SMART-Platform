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

import com.vividsolutions.jts.geom.Envelope;

public class Layer {

	private String name;
	private List<ZoomLevel> zooms;
	private Envelope env;
	
	public Layer(String name, Envelope bounds, int minZoom, int maxZoom) {
		this.name = name;
		this.env = bounds;
		zooms = new ArrayList<>();
		for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
			zooms.add(new ZoomLevel(this, zoom));
		}
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
		return zooms.get(0).getZoom();
	}
	
	/**
	 * The maximum zoom level
	 * @return
	 */
	public int getMaxZoom() {
		return zooms.get(zooms.size() - 1).getZoom();
	}
	
	public List<ZoomLevel> getZooms(){
		return this.zooms;
	}
}
