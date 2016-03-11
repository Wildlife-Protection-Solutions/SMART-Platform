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

import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.GridQueryResultMetadata;
import org.wcs.smart.query.common.model.GridResultItem;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Grid query result object.
 * 
 * @author Emily
 *
 */
public class GridQueryResults extends GridQueryResult implements IMemoryTableResultSet<GridResultItem> {

	
	public GridQueryResults(Collection<GridResultItem> items){
		super(items);
	}

	@Override
	public Iterator<GridResultItem> getIterator() throws SQLException {
		return getData().iterator();
	}

	public GridQueryResultMetadata getTileBounds(){
		if (resultMetadata != null) return resultMetadata;
		
		resultMetadata = GridQueryResultMetadata.computeMetadata(getData());
		return resultMetadata;
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
	
}
