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

import java.util.HashSet;

import org.wcs.smart.query.common.engine.ICellMerger;

/**
 * Merges together hashsets of objects.  Returns a count of the 
 * number of unique items.
 * 
 * @author egouge
 *
 * @param <T> must be hashset
 */

public class UuidCellMerger implements ICellMerger<HashSet<Object>> {

	/**
	 * @see org.wcs.smart.query.common.engine.ICellMerger#mergeCell(java.lang.Object, java.lang.Object)
	 */
	@Override
	public HashSet<Object> mergeCell(HashSet<Object> v1, HashSet<Object> v2) {
		if (v1 == null && v2 == null){
			return null;
		}
		if (v1 != null && v2 == null){
			return v1;
		}
		if (v1 == null && v2 != null){
			return v2;
		}
		v1.addAll(v2);
		return v1;
	}

	/**
	 * @return the number of patrols in the hashset.
	 * 
	 * @see org.wcs.smart.query.common.engine.ICellMerger#getFinalValue(java.lang.Object)
	 */
	@Override
	public Double getFinalValue(HashSet<Object> value) {
		if (value == null) {
			return 0.0;
		} else {
			return (double)value.size();
		}
	}

}
