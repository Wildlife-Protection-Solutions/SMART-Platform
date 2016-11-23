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
package org.wcs.smart.i2.ui;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Label provider for attribute values.  Supports IntelEntityAttributeValue,
 * IntelEntityRelationshipAttributeValue, IntelObservationAttribute
 * 
 * @author Emily
 *
 */
public class AttributeValueLabelProvider extends LabelProvider {
	
	public String getText(Object element){
		Object value = null;
		IntelAttribute attribute = null;
		if (element instanceof IntelEntityAttributeValue){
			value = ((IntelEntityAttributeValue) element).getAttributeValue();
			attribute = ((IntelEntityAttributeValue) element).getAttribute();
		}else if (element instanceof IntelEntityRelationshipAttributeValue){
			value = ((IntelEntityRelationshipAttributeValue) element).getAttributeValue();
			attribute = ((IntelEntityRelationshipAttributeValue) element).getAttribute();
		}else if (element instanceof IntelEntityRelationshipAttributeValue){
			value = ((IntelEntityRelationshipAttributeValue) element).getAttributeValue();
			attribute = ((IntelEntityRelationshipAttributeValue) element).getAttribute();
		}else{
			return super.getText(element);
		}
		if (value == null){
			return "";
		}
		
		if (value instanceof String){
			return (String) value;
		}else if (value instanceof Number){
			if (attribute.getType() == IAttributeType.BOOLEAN){
				if (((Number)value).doubleValue() >= 0.5){
					return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				}else{
					return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
				}
			}
			return ((Number)value).toString();
		}else if (value instanceof Date){
			return DateFormat.getDateInstance().format((Date)value);
		}else if (value instanceof NamedItem){
			return ((NamedItem) value).getName();
		}else{
			return value.toString();
		}
	}
	
	
	public Image getImage(Object element){
		return super.getImage(element);
	}
}
