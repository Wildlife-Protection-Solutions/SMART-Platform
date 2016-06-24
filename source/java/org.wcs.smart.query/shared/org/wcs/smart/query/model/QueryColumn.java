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
package org.wcs.smart.query.model;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.util.GeometryUtils;


/**
 * A column available for output 
 * by a observation query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class QueryColumn implements Cloneable{

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
	}
	
	private String key;
	private String name;
	private ColumnType type;
	private boolean isVisible = true;
	
	protected IProjectionProvider prjProvider;
	
	/**
	 * Creates a new query column 
	 * @param name the column name as displayed to the user
	 * @param key the unique identifier for the column
	 * @param type the column data type
	 */
	public QueryColumn(String name, String key, ColumnType type){
		this.name = name;
		this.key = key;
		this.type = type;
	}
	
	public void setProjectionProvider(IProjectionProvider prjProvider){
		this.prjProvider = prjProvider;
	}
	
	protected CoordinateReferenceSystem getProjection(){
		if (prjProvider == null) return null;
		if (prjProvider.getProjection() == null) return null;
		return prjProvider.getProjection().getParsedCoordinateReferenceSystem();
	}
	
	protected String getProjectionTooltip(){
		if (prjProvider != null) return prjProvider.getProjection().getName();
		return GeometryUtils.SMART_CRS.getName().toString();
	}
	
	/**
	 * 
	 * @return Query column tooltip.  Null if not applicable
	 */
	public String getTooltip(){
		return null;
	}
	
	/**
	 * A unique key for the column.
	 * <p>This can be used when persisting 
	 * column information.
	 * </p> 
	 * 
	 * @return unique key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * This is the name of the column.  It is displayed to the user
	 * in the output table.
	 * 
	 * @return the query columns
	 */
	public String getName() {
		return name;
	}

	/**
	 * Overwrites the default column name with the new name
	 * @param name
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * @return the column data type
	 */
	public ColumnType getType(){
		return this.type;
	}
	
	/**
	 * @param type the column type
	 */
	public void setType(ColumnType type){
		this.type = type;
	}
	
	/** 
	 * The hash code is based on the column key
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode(){
		return key.hashCode();
	}
	
	/** Two columns are the same if they have the same kye.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o){
		if (o == this){
			return true;
		}
		if (!(o instanceof QueryColumn)){
			return false;
		}
		
		if (key != null){
			return this.key.equals(((QueryColumn)o).key);
		}
		return false;
	}
	
	/**
	 * @return if the column is visible or hidden
	 */
	public boolean isVisible(){
		return this.isVisible;
	}
	/**
	 * @param isVisible if the column is visible or hidden
	 */
	public void setVisible(boolean isVisible){
		this.isVisible = isVisible;
	}
	
	/**
	 * @param item
	 * 
	 * @return for a given query result item returns
	 * the object associated with this column
	 */
	public abstract Object getValue(IResultItem item) ;

	/**
	 * @param item
	 * 
	 * @return for a given query result item returns
	 * the string representation of the object associated 
	 * with this column
	 */
	public String getValueAsString(Object value){
		if (value == null) return ""; //$NON-NLS-1$
		if (type == ColumnType.BOOLEAN) {
			if (value instanceof Double){
				if ((Double)value < 0.5){
					//false
					return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, Locale.getDefault());	
				}else{
					//true
					return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, Locale.getDefault());	
				}
			}
			if ((Boolean) value) {
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, Locale.getDefault());
			} else {
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, Locale.getDefault());
			}
		} else if (type == ColumnType.DATE) {
			return DateFormat.getDateInstance().format((Date) value);
		} else if (type == ColumnType.TIME) {
			return DateFormat.getTimeInstance().format((Date) value);
		} else if (type == ColumnType.STRING ||
				type == ColumnType.TIME_STR) {
			return (String) value;
		} else if (type == ColumnType.INTEGER) {
			return String.valueOf((Integer) value);
		} else if (type == ColumnType.LONG) {
			return String.valueOf((Long) value);
		} else if (type == ColumnType.NUMBER) {
			if (value instanceof Double) {
				return String.valueOf((Double) value);
			} else if (value instanceof Float) {
				return String.valueOf((Float) value);
			} else if (value instanceof Integer) {
				return String.valueOf((Integer) value);
			}
		}
		return ""; //$NON-NLS-1$

	}
	
	/** 
	 * Clones the object
	 * @see java.lang.Object#clone()
	 */
	public abstract QueryColumn clone();
		
	
}
