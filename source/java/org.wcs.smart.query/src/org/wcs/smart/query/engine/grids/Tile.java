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
package org.wcs.smart.query.engine.grids;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Represents a tile in a grid.
 * Tiles are ordered from the lower_left starting 
 * at (0,0)
 * 
 * n,0
 * .
 * .
 * 0,0 ........... n,0
 * 
 * @author egouge
 *
 */
public class Tile {

	private int xid = -1;
	private int yid = -1;

	/**
	 * Creates a new tile
	 * @param x x tile id
	 * @param y y tile id
	 */
	public Tile(int x, int y) {
		this.xid = x;
		this.yid = y;
	}

	/**
	 * @return tile x id
	 */
	public int getXId() {
		return xid;
	}

	/**
	 * @return tile y id
	 */
	public int getYId() {
		return yid;
	}

	/**
	 * <p>Two tiles are equals if they have the same
	 * tile x id and tile y id
	 * </p>
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (getClass() == other.getClass()) {
			return ((Tile) other).xid == this.xid
					&& ((Tile) other).yid == this.yid;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + xid;
		result = prime * result + yid;
		return result;
	}


	/**
	 * Returns the bounds of the tile
	 * computed using the provided grid.
	 * <p>Bounds are returned in the
	 * crs of the grid.
	 * </p>
	 * @param g grid definition
	 * @return envelope representing the bounds of the tiles
	 */
	public Envelope getBounds(Grid g) {
		double xmin = g.getOriginX() + g.getCellSize() * xid;
		double xmax = g.getOriginX() + g.getCellSize() * (xid + 1);
		double ymin = g.getOriginY() + g.getCellSize() * yid;
		double ymax = g.getOriginY() + g.getCellSize() * (yid + 1);
		return new Envelope(xmin, xmax, ymin, ymax);
	}

	/**
	 * Prints the grid cell as a wkt POLYGON
	 * string for debugging purposes
	 * @param g
	 */
	public void printTile(Grid g) {
		double xmin = g.getOriginX() + g.getCellSize() * xid;
		double xmax = g.getOriginX() + g.getCellSize() * (xid + 1);
		double ymin = g.getOriginY() + g.getCellSize() * yid;
		double ymax = g.getOriginY() + g.getCellSize() * (yid + 1);

		StringBuilder sb = new StringBuilder();
		sb.append("POLYGON((");
		sb.append(xmin + " " + ymin + ",");
		sb.append(xmax + " " + ymin + ",");
		sb.append(xmax + " " + ymax + ",");
		sb.append(xmin + " " + ymax + ",");
		sb.append(xmin + " " + ymin + "))");

		System.out.println(sb.toString());
	}
}
