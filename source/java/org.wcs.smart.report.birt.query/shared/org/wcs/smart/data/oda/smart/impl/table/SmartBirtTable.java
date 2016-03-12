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
package org.wcs.smart.data.oda.smart.impl.table;

import java.util.List;
import java.util.Locale;

import org.wcs.smart.data.oda.smart.impl.SmartConnection;
//import org.eclipse.swt.graphics.Image;

/**
 * An extension point that allows tables to be created out
 * of SMART objects and be supplied to the 
 * BIRT reporting engine via oda dataset.
 *  
 * @author egouge
 * @since 1.0.0
 */
public abstract class SmartBirtTable {
	
	private String tableKey = null;

	/**
	 * Creates a new table with the given name.
	 * <p>The key must be unique.</p>
	 * @param tableKey unique key represent the table
	 * @Param image representing table
	 */
	public SmartBirtTable(String tableKey){
		this.tableKey = tableKey;
	}
	
	
	/**
	 * Column names should be unique and fixed.  They should
	 * not change.  
	 * @return list of column names in the output table
	 */
	public abstract String[] getColumnNames(SmartConnection connection);
	
	/**
	 * The suggested column label.
	 * 
	 * @return list of column labels in the output table
	 */
	public abstract String[] getColumnLabels(SmartConnection connection);
	
	/**
	 * This is used in the BIRT editor.
	 * 
	 * @return the full table name
	 */
	public abstract String getTableFullName(Locale l);
	
	/**
	 * This is used in the UI for selecting the table
	 * under the category.
	 * 
	 * @return the short form table name
	 */
	public abstract String getTableShortName(Locale l);
	
	/**
	 * 
	 * @return the unique fixed table key
	 */
	public String getTableKey(){
		return this.tableKey;
	}
	
	/**
	 * java.sql.Types
	 * @return the column types 
	 */
	public abstract int[] getColumnTypes(SmartConnection connection);
	
	
	/**
	 * @param ca the conservation area filter
	 * @param currentSession the open active session
	 * @return the values for the given conservation area
	 */
	public abstract List<Object> getValues(SmartConnection connection);

	/**
	 * Given an object returns the value for the given column
	 * 
	 * @param object the object
	 * @param index the column
	 * @return the cell value
	 */
	public abstract Object getValue(Object object, int index, SmartConnection connection);
	
	/**
	 * Starts the query
	 */
	public abstract void openQuery();
	
	/**
	 * Ends the query
	 */
	public abstract void closeQuery();

}
