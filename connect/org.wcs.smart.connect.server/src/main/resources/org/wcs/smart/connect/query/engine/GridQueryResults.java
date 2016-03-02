/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.wcs.smart.query.common.model.GridResultItem;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Grid query result object.
 * 
 * @author Emily
 *
 */
public class GridQueryResults implements IMemoryTableResultSet<GridResultItem> {

	private Collection<GridResultItem> items;
	
	public GridQueryResults(AbstractQueryEngine engine, Collection<GridResultItem> items){
		this.items = items;
	}

	@Override
	public Iterator<GridResultItem> getIterator() throws SQLException {
		return items.iterator();
	}

	public GridMetadata getTileBounds(){
		if (items == null || items.size() == 0){
			return new GridMetadata(0,0,0,0);
		}
		long xmax = Long.MIN_VALUE;
		long xmin = Long.MAX_VALUE;
		long ymax = Long.MIN_VALUE;
		long ymin = Long.MAX_VALUE;
		
		for (GridResultItem it : items){
			if (it.getTileX() < xmin){
				xmin = it.getTileX();
			}
			if (it.getTileX() > xmax){
				xmax = it.getTileX();
			}
			if (it.getTileY() < ymin){
				ymin = it.getTileY();
			}
			if (it.getTileY() > ymax){
				ymax = it.getTileY();
			}
		}
		return new GridMetadata(xmin, ymin, xmax, ymax);
	}

	@Override
	public String getValueAsString(GridResultItem item, QueryColumn column)
			throws SQLException {
		if (column instanceof GridQueryColumn){
			Object v = ((GridQueryColumn)column).getValue(item);
			if (v == null) return ""; //$NON-NLS-1$
			if (v instanceof Number){
				return ((Number)v).toString();
			}
			return v.toString();
		}
		throw new SQLException("Invalid column for gridded query." + column.getKey()); //$NON-NLS-1$
	}
	
	/*
	 * Class for tracking grid bounds
	 */
	public class GridMetadata{
		public Long xmin;
		public Long xmax;
		public Long ymin;
		public Long ymax;
		
		public GridMetadata(long xmin, long ymin, long xmax, long ymax){
			this.xmin = xmin;
			this.xmax = xmax;
			this.ymin = ymin;
			this.ymax = ymax;
		}
	}
}
