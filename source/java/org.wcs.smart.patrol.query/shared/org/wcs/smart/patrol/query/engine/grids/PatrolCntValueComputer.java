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
package org.wcs.smart.patrol.query.engine.grids;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.wcs.smart.query.common.engine.IValueComputer;
import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.Tile;

import com.vividsolutions.jts.geom.LineString;

/**
 * A value computer that computes
 * the number of patrols
 * in a cell.
 * 
 * @author egouge
 *
 */
public class PatrolCntValueComputer implements IValueComputer<HashSet<Object>> {

	
	/**
	 * @see org.wcs.smart.query.common.engine.IValueComputer#computeValue(java.lang.Object, org.org.wcs.smart.query.common.model.Tile, org.org.wcs.smart.query.common.model.Grid, com.vividsolutions.jts.geom.LineString)
	 * 
	 * @return a hashset that contains the hashcode patrol_uuid represented by the linestring
	 * being processed
	 */
	public HashSet<Object> computeValue(HashSet<Object> existingValue, Tile t, 
			Grid gridDef, LineString ls) {
		if (existingValue != null){
			return existingValue;
		}
		HashSet<Object> values = new HashSet<Object>();
		if (ls.getUserData() instanceof UUID){
			values.add( (UUID)ls.getUserData() );	
		}else if (ls.getUserData() instanceof byte[]){
			values.add( Arrays.hashCode((byte[])ls.getUserData()) );
		}else{
			throw new IllegalStateException("Invalid value for counting patrols."); //$NON-NLS-1$
		}
		return values;
	}
}
