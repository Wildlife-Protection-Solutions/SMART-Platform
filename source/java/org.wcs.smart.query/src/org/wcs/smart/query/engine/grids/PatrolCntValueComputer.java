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

import java.util.Arrays;
import java.util.HashSet;

import com.vividsolutions.jts.geom.LineString;

/**
 * A value computer that computes
 * the number of patrols
 * in a cell.
 * 
 * @author egouge
 *
 */
public class PatrolCntValueComputer<T> implements IValueComputer<T> {

	
	/**
	 * @see org.wcs.smart.query.engine.grids.IValueComputer#computeValue(java.lang.Object, org.wcs.smart.query.engine.grids.Tile, org.wcs.smart.query.engine.grids.Grid, com.vividsolutions.jts.geom.LineString)
	 * 
	 * @return a hashset that contains the hashcode patrol_uuid represented by the linestring
	 * being processed
	 */
	@SuppressWarnings("unchecked")
	public T computeValue(T existingValue, Tile t, 
			Grid gridDef, LineString ls) {
		if (existingValue != null){
			return existingValue;
		}
		HashSet<Object> values = new HashSet<Object>();
		if (ls.getUserData() instanceof byte[]){
			values.add(Arrays.hashCode( ((byte[])ls.getUserData())));	
		}else{
			//this should never happen
			return null;
		}
		return (T) values;
	}
}

