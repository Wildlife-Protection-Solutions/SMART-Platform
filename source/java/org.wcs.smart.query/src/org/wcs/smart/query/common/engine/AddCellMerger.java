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

/**
 * CellMerger that assumes the cell values
 * are double and merges them by adding them together.
 * 
 * @author egouge
 *
 */
public class AddCellMerger implements ICellMerger<Double> {

	/**
	 * <p>
	 * Assumes the cell values are double.  Adds the
	 * results together and returns a new double
	 * </p>
	 * @see org.wcs.smart.query.common.engine.ICellMerger#mergeCell(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Double mergeCell(Double v1, Double v2) {
		if(v1 == null ){
			return v2;
		}
		if (v2 == null){
			return v1;
		}
		
		return v1 + v2;
	}

	/**
	 * @see org.wcs.smart.query.common.engine.ICellMerger#getFinalValue(java.lang.Object)
	 */
	@Override
	public Double getFinalValue(Double value) {
		return value;
	}

}
