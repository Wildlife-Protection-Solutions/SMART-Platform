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

import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;

/**
 * An extension point that allows tables to be created out
 * of SMART objects and be supplied to the 
 * BIRT reporting engine via oda dataset.
 *  
 * @author egouge
 * @since 1.0.0
 */
public abstract class SmartBirtTable {
	
	private String fullTableName = null;
	private String shortTableName = null;
	private String tableKey = null;
	protected Image tableImage = null;
	
	/**
	 * Creates a new table with the given name.
	 * <p>The key must be unique.</p>
	 * @param tableName full and short display name for table
	 * @param tableKey unique key represent the table
	 */
	public SmartBirtTable(String fullTableName, String tableKey){
		this(fullTableName, fullTableName, tableKey);
	}
	
	/**
	 * Creates a new table with the given name.
	 * <p>The key must be unique.</p>
	 * @param fullTableName full display name for table
	 * @param shortTableName short display name for table
	 * @param tableKey unique key represent the table
	 */
	public SmartBirtTable(String fullTableName, String shortTableName, String tableKey){
		this(fullTableName, shortTableName, tableKey, null);
	}
	
	/**
	 * Creates a new table with the given name.
	 * <p>The key must be unique.</p>
	 * @param tableName full table name display name for table
	 * @param tableKey unique key represent the table
	 * @Param image representing table
	 */
	public SmartBirtTable(String tableName, String tableKey, Image image){
		this(tableName, tableName, tableKey, image);
	}
	
	/**
	 * Creates a new table with the given name.
	 * <p>The key must be unique.</p>
	 * @param fullTableName full display name for table
	 * @param shortTableName short display name for table
	 * @param tableKey unique key represent the table
	 * @Param image representing table
	 */
	public SmartBirtTable(String fullTableName, String shortTableName,  String tableKey, Image image){
		this.fullTableName = fullTableName;
		this.shortTableName = shortTableName;
		this.tableKey = tableKey;
		this.tableImage = image;
	}
	
	/**
	 * Column names should be unique and fixed.  They should
	 * not change.  
	 * @return list of column names in the output table
	 */
	public abstract String[] getColumnNames();
	
	/**
	 * The suggested column label.
	 * 
	 * @return list of column labels in the output table
	 */
	public abstract String[] getColumnLabels();
	
	/**
	 * This is used in the BIRT editor.
	 * 
	 * @return the full table name
	 */
	public String getTableFullName(){
		return this.fullTableName;
	}
	
	/**
	 * This is used in the UI for selecting the table
	 * under the category.
	 * 
	 * @return the short form table name
	 */
	public String getTableShortName(){
		return this.shortTableName;
	}
	
	/**
	 * 
	 * @return an image for the table
	 */
	public Image getImage(){
		return tableImage;
	}
	
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
	public abstract int[] getColumnTypes();
	
	
	/**
	 * @param ca the conservation area filter
	 * @param currentSession the open active session
	 * @return the values for the given conservation area
	 */
	public abstract List<Object> getValues(Collection<ConservationArea> cas, Session currentSession);

	/**
	 * Given an object returns the value for the given column
	 * 
	 * @param object the object
	 * @param index the column
	 * @return the cell value
	 */
	public abstract Object getValue(Object object, int index);
	
	/**
	 * Starts the query
	 */
	public abstract void openQuery();
	
	/**
	 * Ends the query
	 */
	public abstract void closeQuery();

}
