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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;

/**
 * Data model query column (attribute or category)
 * 
 * @author Emily
 *
 */
public class DataModelColumn extends AbstractQueryColumn{

	public enum GeometryProperty{
		SOURCE, AREA, PERIMETER;
		
		public String getKey() {
			return name().toLowerCase();
		}
		
		public String generateKey(String attributeId) {
			return attributeId + "." + getKey(); //$NON-NLS-1$
		}
	}
	
	private int level = -1;
	private String attributeKey;
	private Attribute.AttributeType type;
	private DataModelColumn.GeometryProperty property;
	private String formatString = null;
	
	/**
	 * Creates a new category data model column
	 * @param level
	 */
	public DataModelColumn(int level, Locale l) {
		super(MessageFormat.format(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(IIntelligenceLabelProvider.QUERY_COLUMN_CATEGORY_LABEL, l), level), "category:" + level); //$NON-NLS-1$
		this.level = level;
	}

	/**
	 * Creates a new attribute data model column
	 * @param attribute
	 */
	public DataModelColumn(Attribute attribute){
		super(attribute.getName(), "attribute:" + attribute.getKeyId()); //$NON-NLS-1$
		this.attributeKey = attribute.getKeyId();
		this.type = attribute.getType();
		this.formatString = attribute.getRegex();
	}
	
	/**
	 * Creates a new attribute data model column
	 * @param attribute
	 */
	public DataModelColumn(Attribute attribute, DataModelColumn.GeometryProperty property, String name){
		super(name, "attribute:" + attribute.getKeyId() + ":" + property.name()); //$NON-NLS-1$ //$NON-NLS-2$
		this.attributeKey = attribute.getKeyId();
		this.type = attribute.getType();
		this.property = property;
		this.formatString = attribute.getRegex();
	}
	
	/**
	 * The format string for attribute columns. May be null.
	 * @return
	 */
	public String getFormatString() {
		return this.formatString;
	}
	
	/**
	 * The category level
	 * @return
	 */
	public int getLevel(){
		return this.level;
	}
	
	/**
	 * The attribute key
	 * @return
	 */
	public String getAttributeKey(){
		return this.attributeKey;
	}
	
	/**
	 * The attribute type
	 * @return
	 */
	public Attribute.AttributeType getAttributeType(){
		return this.type;
	}
	
	@Override
	public Object getValue(IResultItem item) {
		if (!(item instanceof IntelObservationResultItem)) return null;
		
		if (level >= 0){
			return ((IntelObservationResultItem)item).getCategoryLabel(level);
		}
		if (attributeKey != null){
			String key = attributeKey;
			if (this.property != null) {
				key = property.generateKey(attributeKey);
			}
			Object value = ((IntelObservationResultItem)item).getAttributeValue(key);
			if (value == null) return null;
			switch(type){
			case BOOLEAN:
				if ((Double)value > 0.5) return Boolean.TRUE;
				return Boolean.FALSE;
			case DATE:
				return (LocalDate)value;
			case NUMERIC:
				return ((Double)value);
			case LIST:
			case TEXT:
			case TREE:
			case MLIST:			
				return value.toString();
			case LINE:
			case POLYGON:
				if (this.property == null) {
					return value;
				}else if (this.property == GeometryProperty.SOURCE ) {
					return ((Attribute.GeometrySource)value).name();
				}
				return value; 
				
			}
		}
		return null;
	}

	@Override
	public String getValue(IResultItem item, Locale l){
		Object toFormat = getValue(item);
		if (toFormat == null) return ""; //$NON-NLS-1$
		if (getDataType() == Type.STRING) return (String)toFormat;
		if (getDataType() == Type.DATE) return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(l).format( (LocalDate)toFormat );
		if (getDataType() == Type.NUMERIC) return ((Number)toFormat).toString();
		if (getDataType() == Type.BOOLEAN){
			if ((Boolean)toFormat){
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, Locale.getDefault());
			}else{
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, Locale.getDefault());
			}
		}
		if (getDataType().isGeometry()) {
			if (this.property == null) {
				if (this.type.isGeometry()) return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(this.type, l);
				return getDataType().name();
			}
		}
		return toFormat.toString();
	}
	
	@Override
	public Type getDataType() {
		if (level >= 0 ) return Type.STRING;
		switch(type){
			case BOOLEAN:
				return Type.BOOLEAN;
			case DATE:
				return Type.DATE;
			case NUMERIC:
				return Type.NUMERIC;
			case TEXT:
			case TREE:
			case LIST:
			case MLIST:
				return Type.STRING;
			case LINE:
				if (this.property == GeometryProperty.SOURCE) return Type.STRING;
				if (this.property == null) return Type.MULTILINESTRING;
				return Type.NUMERIC;
			case POLYGON:
				if (this.property == GeometryProperty.SOURCE) return Type.STRING;
				if (this.property == null) return Type.MULTIPOLYGON;
				return Type.NUMERIC;
		}
		return Type.STRING;
	}
}
