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
package org.wcs.smart.observation.query.model.columns;

import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Class represents one of the fixed table columns that
 * do not change from conservation area to conservation area.
 * 
 * <p>This includes items such as the patrol id, patrol type etc
 * but not items related to the datamodel.</p>
 * 
 * @author Jeffloun
 * @since 1.0.0
 */

public class GridQueryColumn extends QueryColumn{

	/**
	 * The defined fixed columns.
	 */
	public enum GridColumns{
		TILE_X(Messages.GridQueryColumn_TileXIdColumnName, ColumnType.LONG,"tile_x"), //$NON-NLS-1$
		TILE_Y(Messages.GridQueryColumn_TileYIdColumnName, ColumnType.LONG, "tile_y"), //$NON-NLS-1$
		VALUE(Messages.GridQueryColumn_ValueColumnName, ColumnType.NUMBER,"value"); //$NON-NLS-1$
		
		private String guiName;
		private ColumnType type;
		private String key;
		private GridColumns(String name, ColumnType type, String key){
			this.guiName = name;
			this.type = type;
			this.key = key;
			
		}
	}
	
	
	private GridColumns column;
	
	/**
	 * Creates a new fixed table column.
	 * 
	 * @param column the column definition
	 */
	public GridQueryColumn(GridColumns column) {
		super(column.guiName, column.key, column.type);
		this.column = column;
	}


	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#getValue(org.wcs.smart.query.model.QueryResultItem)
	 */
	public Object getValue(IResultItem item) {
		if (item instanceof GridResultItem) {
			switch (column) {
			case TILE_X:
				return ((GridResultItem) item).getTileX();
			case TILE_Y:
				return ((GridResultItem) item).getTileY();
			case VALUE:
				return ((GridResultItem) item).getValue();
			}
		}
		return ""; //$NON-NLS-1$
	}


	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		GridQueryColumn newColumn = new GridQueryColumn(this.column);
		return newColumn;
	}


}
