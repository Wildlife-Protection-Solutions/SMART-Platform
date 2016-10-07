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
package org.wcs.smart.map.raster;

import java.util.Collection;
import java.util.Iterator;

/**
 * Gridded query result metadata.
 * 
 * @author Emily
 *
 */
public class GridMetadata {

	private double minResultValue = Double.NEGATIVE_INFINITY;
	
	private double maxResultValue = Double.POSITIVE_INFINITY;
	
	private long minXTile = 0;
	
	private long maxXTile = 0;
	
	private long minYTile = 0;
	
	private long maxYTile = 0;

	public GridMetadata(double minResultValue,
			double maxResultValue, long minXTile, long maxXTile, long minYTile,
			long maxYTile) {
		super();
		this.minResultValue = minResultValue;
		this.maxResultValue = maxResultValue;
		this.minXTile = minXTile;
		this.maxXTile = maxXTile;
		this.minYTile = minYTile;
		this.maxYTile = maxYTile;
	}

	public double getMinResultValue() {
		return minResultValue;
	}

	public double getMaxResultValue() {
		return maxResultValue;
	}

	public long getMinXTile() {
		return minXTile;
	}

	public long getMaxXTile() {
		return maxXTile;
	}

	public long getMinYTile() {
		return minYTile;
	}

	public long getMaxYTile() {
		return maxYTile;
	}
	
	public static final GridMetadata computeMetadata(Collection<? extends GridResultItem> items){
		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		
		long[] values = new long[]{Long.MAX_VALUE,Long.MIN_VALUE, Long.MAX_VALUE,Long.MIN_VALUE};
		
		for (Iterator<? extends GridResultItem> iterator = items.iterator(); iterator.hasNext();) {
			GridResultItem type = (GridResultItem) iterator.next();
			
			max = Math.max(max, type.getValue());
			min = Math.min(min, type.getValue());
			values[0] = Math.min(values[0], type.getTileX());
			values[1] = Math.max(values[1], type.getTileX());
			values[2] = Math.min(values[2], type.getTileY());
			values[3] = Math.max(values[3], type.getTileY());
			
		}
		
		GridMetadata metadata = new GridMetadata(
				min, max,values[0], values[1], values[2], values[3]);
		
		return metadata;
	}
}
