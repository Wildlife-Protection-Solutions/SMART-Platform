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
package org.wcs.smart.query.common.engine;

import java.util.HashMap;
import java.util.List;

import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.Tile;

import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * 
 * @author egouge
 *
 * Extends the robust line intersector
 * to perform additional processing 
 * when an intersection is found.
 * <p>In this case it computes the values
 * for all cells that touch the intersection
 * points.</p>
 *
 * @param <T> the type of data to computer
 */
public class GriddedLineIntersector<T> extends RobustLineIntersector {

	private HashMap<Tile, T> data = null;
	private IValueComputer<T> valueComputer;
	private Grid gridDef;
	private LineString ls;
	private int intersectionCnt = 0;
	private Exception ex;
	
	/**
	 * 
	 * @param valueComputer the cell value computer
	 * @param gridDef the grid def
	 * @param ls the linestring being processed
	 */
	public GriddedLineIntersector(IValueComputer<T> valueComputer, 
			Grid gridDef, LineString ls) {
		
		super();
		this.ex = null;
		this.valueComputer = valueComputer;
		this.gridDef = gridDef;
		this.data = new HashMap<Tile, T>();
		this.ls = ls;
		intersectionCnt = 0;
	}

	/**
	 * @return the data computed for all tiles
	 */
	public HashMap<Tile,T> getData(){
		return this.data;
	}
	
	/**
	 * @return the number of intersections
	 */
	public int getIntersectionCount(){
		return this.intersectionCnt;
	}
	
	/**
	 * Computes the intersection points then computes the
	 * data for the cells and adds the results to the 
	 * data row.
	 */
	@Override
	protected int computeIntersect(Coordinate p1, Coordinate p2, Coordinate q1,
			Coordinate q2) {

		int r = super.computeIntersect(p1, p2, q1, q2);

		int cnt = 0;
		if (r == POINT_INTERSECTION) {
			cnt = 1;
		} else if (r == COLLINEAR_INTERSECTION) {
			cnt = 2;
		}
		intersectionCnt  += cnt;
		for (int i = 0; i < cnt; i++) {
			Coordinate c = super.getIntersection(i);
			List<Tile> tiles = gridDef.findTiles(c.x, c.y);
			for (Tile t : tiles) {
				T value = this.data.get(t);
				try{
					T x = valueComputer.computeValue(value, t, gridDef, ls);
					this.data.put(t, x);
				}catch (Exception ex){
					this.ex = ex;
					return -1;
				}
				
			}
		}
		return r;
	}
	
	public Exception getException(){
		return this.ex;
	}
}
