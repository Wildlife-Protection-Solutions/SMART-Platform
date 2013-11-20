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

import java.sql.Date;
import java.util.HashSet;

import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.LineString;

public class PatrolDayCntValueComputer implements IValueComputer<HashSet<Object>> {

	/** 
	 * <p>This counts the number of patrol days in a given cell.
	 * Since we guarentee each patrol day is represented by a single 
	 * line string this always returns one.
	 * </p>
	 * 
	 * @return 1
	 * @see org.wcs.smart.patrol.query.engine.grids.IValueComputer#computeValue(java.lang.Object, org.wcs.smart.patrol.query.engine.grids.Tile, org.wcs.smart.patrol.query.engine.grids.Grid, com.vividsolutions.jts.geom.LineString)
	 */
	public HashSet<Object> computeValue(HashSet<Object> existingValue, Tile t, Grid gridDef,
			LineString ls) {
		if (existingValue != null){
			return existingValue;
		}
		HashSet<Object> d = new HashSet<Object>();
		
		Object[] data = (Object[]) ls.getUserData();
		byte[] pid = (byte[]) data[0];
		Date day = (Date)data[1];
		
		String key = SmartUtils.encodeHex(pid) + "_" + day.getTime(); //$NON-NLS-1$
		d.add(key);
		
		return d;
	}
}
