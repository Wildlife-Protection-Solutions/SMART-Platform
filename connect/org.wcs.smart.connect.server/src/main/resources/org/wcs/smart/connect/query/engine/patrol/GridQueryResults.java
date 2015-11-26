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
package org.wcs.smart.connect.query.engine.patrol;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IMemoryTableResultSet;
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

	private AbstractQueryEngine engine;
	private Collection<GridResultItem> items;
	
	public GridQueryResults(AbstractQueryEngine engine, Collection<GridResultItem> items){
		this.engine = engine;
		this.items = items;
	}

	@Override
	public Iterator<GridResultItem> getIterator() throws SQLException {
		return items.iterator();
	}


	@Override
	public String getValueAsString(GridResultItem item, QueryColumn column)
			throws SQLException {
		Object v = getValue(item, column.getKey());
		if (v == null) return "";
		if (v instanceof Number){
			return ((Number)v).toString();
		}
		return "";
	}

	@Override
	public Object getValue(GridResultItem item, String columnKey)
			throws SQLException {
		if (columnKey.equals(GridQueryColumn.GridColumns.TILE_X)){
			return item.getTileX();
		}else if (columnKey.equals(GridQueryColumn.GridColumns.TILE_Y)){
			return item.getTileY();
		}else if (columnKey.equals(GridQueryColumn.GridColumns.VALUE)){
			return item.getValue();
		}
		return null;
	}
	

}
