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

import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.Tile;

import com.vividsolutions.jts.geom.LineString;


/**
 * Computes the cell value when rasterizing. 
 * 
 * @author egouge
 *
 */
public interface IValueComputer<T> {

	/**
	 * Computes the cell value when rasterizing a linestring.
	 * 
	 * @param existingValue if a linestring has already visited this
	 * tile then this is the value computed earlier; otherwise the value 
	 * is null 
	 * @param t tile being visited
	 * @param gridDef grid definition grid definition
	 * @param ls linestring being rasterized
	 * @return value for cell computed for the cell
	 * @throws expection if error occurs computing value
	 */
	public T computeValue(T existingValue, Tile t, Grid gridDef, LineString ls) throws Exception;
}
