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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.query.engine.EntityRecordQueryResultItem;

/**
 * Intelligence attribute query column
 * 
 * @author Emily
 *
 */
public class IntelAttributeQueryColumn extends AbstractQueryColumn {

	private IntelAttribute attribute;
	
	public IntelAttributeQueryColumn(IntelAttribute attribute) {
		super(attribute.getName(), "entityattribute:" + attribute.getKeyId()); //$NON-NLS-1$
		this.attribute = attribute;
	}
	
	public IntelAttribute getAttribute() {
		return this.attribute;
	}
	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof EntityRecordQueryResultItem) {
			Object value = ((EntityRecordQueryResultItem) item).getAttributeValue(attribute.getKeyId());
			return value;
			
		}
		return null;
	}

	@Override
	public String getValue(IResultItem item, Locale l) {
		Object toFormat = getValue(item);
		if (toFormat == null) return ""; //$NON-NLS-1$
		
		switch(attribute.getType()) {
			case BOOLEAN:
				if ((Double)toFormat > 0.5) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l);
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
			case DATE:
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format( (LocalDate)toFormat );
			case LIST:
				return ((IntelAttributeListItem)toFormat).getName();
			case NUMERIC:
				return toFormat.toString();
			case TEXT:
				return toFormat.toString();
			case EMPLOYEE:
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel((Employee) toFormat, l);
			case POSITION:
				return toFormat.toString();
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public Type getDataType() {
		switch(attribute.getType()) {
		case BOOLEAN:
			return Type.BOOLEAN;
		case DATE:
			return Type.DATE;
		case NUMERIC:
			return Type.NUMERIC;
		case EMPLOYEE:
		case LIST:
		case TEXT:
			return Type.STRING;
		case POSITION:
			return Type.POINT;
		}
		return null;
	}

}
