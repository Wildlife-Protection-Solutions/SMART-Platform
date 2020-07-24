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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.query.engine.IntelRecordResultItem;

/**
 * Intelligence attribute query column
 * 
 * @author Emily
 *
 */
public class IntelRecordAttributeQueryColumn extends AbstractQueryColumn {

	private IntelRecordSourceAttribute attribute;
	
	public IntelRecordAttributeQueryColumn(IntelRecordSourceAttribute attribute) {
		super(IIntelligenceLabelProvider.getName(attribute) + " (" + attribute.getSource().getName() + ")", "recordattribute:" + attribute.getSource().getKeyId() + ":" + attribute.getKeyId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		this.attribute = attribute;
	}
		
	/**
	 * Identifies if the column can be sorted or not
	 * 
	 * @return
	 */
	public boolean canSort(){
		if (attribute.getIsMultiple() == null) return true;
		return !attribute.getIsMultiple();
	}
	
	public IntelRecordSourceAttribute getAttribute() {
		return this.attribute;
	}
	
	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof IntelRecordResultItem) {
			IntelRecordResultItem i = (IntelRecordResultItem)item;
			if (i.getRecordSourceUuid() == null) return null;
			if (!i.getRecordSourceUuid().equals(attribute.getSource().getUuid())) return null;
			return i.getAttributeValue(attribute.getKeyId());
			
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getValue(IResultItem item, Locale l) {
		Object toFormat = getValue(item);
		if (toFormat == null) return ""; //$NON-NLS-1$
		
		if (attribute.getEntityType() != null) {
			List<IntelEntity> items = (List<IntelEntity>)toFormat;
			StringBuilder sb = new StringBuilder();
			for (IntelEntity i : items) {
				sb.append(i.getIdAttributeAsText(l));
				sb.append(", "); //$NON-NLS-1$
			}
			if (sb.length() > 0) return sb.substring(0, sb.length() - 2);
			return sb.toString();
		}else {
			switch(attribute.getAttribute().getType()) {
				case BOOLEAN:
					if ((Double)toFormat > 0.5) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l);
					return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
				case DATE:
					return DateFormat.getDateInstance().format( (Date)toFormat );
				case LIST:
					List<IntelAttributeListItem> items = (List<IntelAttributeListItem>)toFormat;
					StringBuilder sb = new StringBuilder();
					for (IntelAttributeListItem i : items) {
						sb.append( i.getName() );
						sb.append(", "); //$NON-NLS-1$
					}
					if (sb.length() > 0) return sb.substring(0, sb.length() - 2);
					return sb.toString();
				case NUMERIC:
					return toFormat.toString();
				case TEXT:
					return toFormat.toString();
				case EMPLOYEE:
					List<Employee> items2 = (List<Employee>)toFormat;
					StringBuilder sb2 = new StringBuilder();
					for (Employee i : items2) {
						sb2.append( SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(i, l));
						sb2.append(", "); //$NON-NLS-1$
					}
					if (sb2.length() > 0) return sb2.substring(0, sb2.length() - 2);
					return sb2.toString();
				case POSITION:
					Object[] data = (Object[]) toFormat;
					return "POINT(" + (Double)data[0] + " " + (Double)data[1] + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return ""; //$NON-NLS-1$
		}
	}

	@Override
	public Type getDataType() {
		if (attribute.getEntityType() != null) return Type.STRING;
		
		switch(attribute.getAttribute().getType()) {
		case BOOLEAN:
			return Type.BOOLEAN;
		case DATE:
			return Type.DATE;
		case NUMERIC:
			return Type.NUMERIC;
		case EMPLOYEE:
		case LIST:
		case POSITION:
		case TEXT:
			return Type.STRING;
		}
		return null;
	}

}
