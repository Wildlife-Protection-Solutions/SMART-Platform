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
package org.wcs.smart.asset.ui.views.map;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Interface for asest overview map table column
 * 
 * @author Emily
 *
 */
public interface IOverviewTableColumn {

	public static enum GroupByOption{
		STATION,LOCATION
	};
	
	/**
	 * Possible column types
	 */
	public enum ColumnType{
		INTEGER("Integer", java.sql.Types.INTEGER), //$NON-NLS-1$
		LONG("Integer", java.sql.Types.INTEGER), //$NON-NLS-1$
		NUMBER("Double", java.sql.Types.DOUBLE), //$NON-NLS-1$
		STRING("String", java.sql.Types.VARCHAR), //$NON-NLS-1$
		BOOLEAN("Integer", java.sql.Types.BOOLEAN), //$NON-NLS-1$
		DATE("Date", java.sql.Types.DATE), //$NON-NLS-1$
		TIME("Date", java.sql.Types.TIME), //$NON-NLS-1$
		TIME_STR("String", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
		public String geotoolsType;
		public int sqlType;
		
		ColumnType(String geotoolsType, int sqlType){
			this.geotoolsType = geotoolsType;
			this.sqlType = sqlType;
		}
		
		public int getSqlType(){
			return this.sqlType;
		}
		
		public String asString(Object value) {
			if (value == null) return "";
			if (value instanceof Exception) return "ERROR: " + ((Exception)value).getMessage();
			switch(this) {
			case BOOLEAN:
				if ((Boolean)value) {
					return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				}else {
					return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
				}
			case DATE:
				return DateFormat.getDateInstance().format((Date)value);
			case INTEGER:
				if (value instanceof Long) {
					return ((Long)value).toString();
				}else if (value instanceof Integer) {
					return ((Integer)value).toString();
				}
				return value.toString();
			case LONG:
				return ((Long)value).toString();
			case NUMBER:
				return ((Number)value).toString();
			case TIME_STR:
			case STRING:
				return ((String)value);
			case TIME:
				return DateFormat.getTimeInstance().format((Date)value);
			}
			return value.toString();
		}
	}
	
	/**
	 * 
	 * @return the name of the column
	 */
	public String getName();
	
	
	/**
	 * A unique key for identifying the column.  This is used for
	 * the udig feature fields so that the ui can have different names for
	 * various languages but the udig/geotools styles will
	 * still work. 
	 * 
	 * Key should only contain a-z and 0-9 characters
	 * 
	 * @return
	 */
	public String getKey();
	
	/**
	 * Sets the key for the columns.  By default this does nothing
	 * so columns that need to support this should override. 
	 */
	public default void setKey(String newKey) {} 
	
	/**
	 * Get the value for the column given the data row
	 * @param data
	 * @return
	 */
	public Object getValue(StationData data);
	
	/**
	 * Get the type of column
	 * @return
	 */
	public ColumnType getType();

	/**
	 * convert the column to a JSON object that defines the column
	 * 
	 * @return
	 */
	public JSONObject serialize();
}
