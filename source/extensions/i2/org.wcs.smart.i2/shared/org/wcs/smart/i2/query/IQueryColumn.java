/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query;


/**
 * A query column.
 * 
 * @author Emily
 *
 */
public interface IQueryColumn {

	public enum Type {
		STRING("String"),
		DATE("Date"),
		TIME("String"),
		NUMERIC("Double"),
		BOOLEAN("Integer"),
		GEOMETRY("Geometry");
		
		private String geoToolsType;
		
		private Type(String geoType){
			this.geoToolsType = geoType;
		}
		
		public String getFeatureType(){
			return geoToolsType;
		}
	}
	
	
	/**
	 * The column name
	 * @return
	 */
	public String getColumnName();
	
	/**
	 * The unique identifier for the column
	 * @return
	 */
	public String getKey();
	
	/**
	 * Column visibility
	 * @return
	 */
	public boolean isVisible();
	
	/**
	 * Converts a query result record to a value for this column
	 * 
	 * @param item
	 * @param l
	 * @return
	 */
	public Object getValue(IResultItem item);
	
	
	/**
	 * The column data type
	 * @return
	 */
	public Type getDataType();
	
	/**
	 * The column tooltip (for longer column names)
	 * @return
	 */
	default public String getTooltip(){
		return null;
	}
	
	/**
	 * Identifies if the column can be sorted or not
	 * @return
	 */
	default public boolean canSort(){
		return true;
	}
}
