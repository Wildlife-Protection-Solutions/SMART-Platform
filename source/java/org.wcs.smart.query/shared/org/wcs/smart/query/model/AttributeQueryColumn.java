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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;


/**
 * An table column in the results table that represents an attribute.
 * <p>There should be one column for each attribute
 * defined in the data model</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeQueryColumn extends QueryColumn {
	
	public static final String KEY_PREFIX = "attribute:"; //$NON-NLS-1$

	public enum GeometryProperty{
		SOURCE, AREA, PERIMETER;
		
		public String getKey() {
			return name().toLowerCase();
		}
		
		public String generateKey(String attributeId) {
			return attributeId + "." + getKey(); //$NON-NLS-1$
		}
	}
	
	protected String attributeKey = null;
	protected AttributeType attributeType;
	
	protected String formatString;

	/**
	 * Creates a new attribute column.
	 * 
	 * @param name the column name as it appears to the user
	 * @param key the attribute id key
	 * @param type the type of the attribute column
	 */
	public AttributeQueryColumn(String name, String attributeId, AttributeType type, String formatString){
		super(name, KEY_PREFIX + attributeId, null);
		this.formatString = formatString;
		this.attributeKey = attributeId;
		this.attributeType = type;
		ColumnType ctype = ColumnType.STRING;
		if (type == AttributeType.NUMERIC ){
			ctype = ColumnType.NUMBER;
		}else if (type == AttributeType.BOOLEAN){
			ctype = ColumnType.BOOLEAN;
		}else if (type == AttributeType.DATE) {
			ctype = ColumnType.DATE;
		}else if (type.isGeometry()) {
			ctype = ColumnType.BLOB;
		}else {
			ctype = ColumnType.STRING;
		}
		super.setType(ctype);
	}
	
	public AttributeQueryColumn(String name, String attributeId, GeometryProperty prop, AttributeType type){
		super(name, KEY_PREFIX + attributeId + "." + prop.getKey(), null);
		if (prop == GeometryProperty.AREA || prop == GeometryProperty.PERIMETER) {
			this.formatString = "2";
		}
		this.attributeKey = attributeId;
		this.attributeType = type;
		ColumnType ctype = ColumnType.STRING;
		if (type == AttributeType.NUMERIC ){
			ctype = ColumnType.NUMBER;
		}else if (type == AttributeType.BOOLEAN){
			ctype = ColumnType.BOOLEAN;
		}else if (type == AttributeType.DATE) {
			ctype = ColumnType.DATE;
		}else if (type.isGeometry()) {
			ctype = ColumnType.BLOB;
		}else {
			ctype = ColumnType.STRING;
		}
		super.setType(ctype);
	}
	
	@Override
	public String getFormatString() {
		return this.formatString;
	}
	
	public AttributeType getAttributeType(){
		return this.attributeType;
	}
	
	public String getAttributeId(){
		return getKey().substring(KEY_PREFIX.length());
	}

	@Override
	public String getValueAsString(Object value, boolean formatted){

		if (value == null) return ""; //$NON-NLS-1$
		
		if (formatted && this.attributeType == Attribute.AttributeType.NUMERIC) {
			return Attribute.formatNumberAsString(value, formatString);
		}
		if (getAttributeType() == Attribute.AttributeType.POLYGON) {
			return "POLYGON";
		}
		if (getAttributeType() == Attribute.AttributeType.LINE) {
			return "LINE";
		}
		
		return super.getValueAsString(value, formatted);
	}
	
	@Override
	public Object getValue(IResultItem queryResultItem) {
		if (queryResultItem instanceof ObservationQueryResultItem) {
			ObservationQueryResultItem item = (ObservationQueryResultItem) queryResultItem;
			Object x = item.getAttributeValue(attributeKey);
			if (x != null) {
				if (getType() == QueryColumn.ColumnType.BOOLEAN){
					return Boolean.valueOf((Double)x >= 0.5);
				}else if(getType() == QueryColumn.ColumnType.DATE) {
					if (x instanceof String) {
						//convert strings to dates
						return LocalDate.parse((String)x, DateTimeFormatter.ISO_LOCAL_DATE);
					}
				}
			}
			return x;
		}
		return ""; //$NON-NLS-1$
	}


	/**
	 * @see org.wcs.smart.asset.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		QueryColumn newColumn = new AttributeQueryColumn(getName(), getAttributeId(), getAttributeType(), getFormatString());
		newColumn.setEdit(canEdit());
		return newColumn;
	}
	
}
