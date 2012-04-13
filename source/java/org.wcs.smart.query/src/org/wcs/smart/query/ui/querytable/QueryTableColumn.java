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
package org.wcs.smart.query.ui.querytable;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.query.model.QueryResultItem;

/**
 * A table column in the output table.  Each table
 * column has a key which must be unique across all
 * columns, a name, a label provider and a type;
 * 
 * @author Emily
 * @since 1.0.0
 */
public interface QueryTableColumn {

	
	/**
	 * Possible column types
	 */
	public enum ColumnType{
		INTEGER("Integer"),
		NUMBER("Double"),
		STRING("String"),
		BOOLEAN("Integer"),
		DATE("Date");
		
		public String geotoolsType;
		ColumnType(String geotoolsType){
			this.geotoolsType = geotoolsType;
		}
	}
	
	/**
	 * This is the name of the column.  It is displayed to the user
	 * in the output table.
	 * 
	 * @return the name of the column
	 */
	public String getName();
	
	/**
	 * @return the column label provider
	 */
	public ColumnLabelProvider getLabelProvider();
	
	/**
	 * @return the column type
	 */
	public ColumnType getType();
	
	/**
	 * Give a query result item, this function
	 * returns the value associated with the given 
	 * column.
	 * 
	 * @param item the item
	 * @return the value of the item associated with this table column
	 */
	public Object getValue(QueryResultItem item);
	
	/**
	 * A unique key for the column.
	 * <p>This can be used when persisting 
	 * column information.
	 * </p> 
	 * 
	 * @return unique key
	 */
	public String getKey();
}
