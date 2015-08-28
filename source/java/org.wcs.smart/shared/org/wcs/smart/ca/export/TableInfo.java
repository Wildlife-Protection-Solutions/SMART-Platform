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
package org.wcs.smart.ca.export;

/**
 * Class to track table and hibernate mapping information
 * for tables in the smart database.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class TableInfo {

	private String tableName;
	private String caPropertyName;
	private Class<?> clazz;
	
	/**
	 * @param clazz the hibernate class that represents the table
	 * @param tableName the schema qualified table name
	 */
	public TableInfo(Class<?> clazz, String tableName){
		this.tableName = tableName;
		this.clazz = clazz;
	}
	/**
	 * @param clazz the hibernate class that represents the table
	 * @param tableName the schema qualified table name
	 * @param caPropertyName the conservation area property column name (in the table)
	 */
	public TableInfo(Class<?> clazz, String tableName, String caPropertyName){
		this(clazz, tableName);
		this.caPropertyName = caPropertyName;
	}
	
	
	/**
	 * @return hibernate mapped class
	 */
	public Class<?> getClazz(){
		return this.clazz;
	}
	
	/**
	 * Sets the table name
	 * @param tableName
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	/**
	 * Sets the conservation area property column name (in the table)
	 * @param caPropertyName
	 */
	public void setCaPropertyName(String caPropertyName) {
		this.caPropertyName = caPropertyName;
	}

	/**
	 * @return the schema qualified table name
	 */
	public String getTableName(){
		return this.tableName;
	}
	
	/**
	 * @return the column name of the conservation area property
	 */
	public String getCaPropertyName(){
		return this.caPropertyName;
	}

}
