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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;

/**
 * Data model query column (attribute or category)
 * 
 * @author Emily
 *
 */
public class DataModelColumn extends AbstractQueryColumn{

	private int level = -1;
	private String attributeKey;
	private Attribute.AttributeType type;
	
	public DataModelColumn(int level) {
		super(MessageFormat.format("Level {0}", level), "category:" + level);
		this.level = level;
	}

	public DataModelColumn(Attribute attribute){
		super(attribute.getName(), "attribute:" + attribute.getKeyId());
		this.attributeKey = attribute.getKeyId();
		this.type = attribute.getType();
	}
	
	public int getLevel(){
		return this.level;
	}
	
	public String getAttributeKey(){
		return this.attributeKey;
	}
	
	public Attribute.AttributeType getAttributeType(){
		return this.type;
	}
	
	@Override
	public String getValue(IResultItem item, Locale l) {
		if (!(item instanceof IntelObservationResultItem)) return null;
		
		if (level >= 0){
			return ((IntelObservationResultItem)item).getCategoryLabel(level);
		}
		if (attributeKey != null){
			Object value = ((IntelObservationResultItem)item).getAttributeValue(attributeKey);
			if (value == null) return "";
			switch(type){
			case BOOLEAN:
				if ((Double)value > 0.5) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l);
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
			case DATE:
				return DateFormat.getDateInstance().format((Date)value);
			case LIST:
				return value.toString();
			case NUMERIC:
				return ((Double)value).toString();
			case TEXT:
				return value.toString();
			case TREE:
				return value.toString();
			}
		}
		return "";
	}
	

}
