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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;

/**
 * Label provider for intelligence attributes
 * 
 * @author Emily
 *
 */
public class AttributeLabelProvider extends LabelProvider {
	
	
	public String getText(Object element){
		if (element instanceof IntelAttribute){
			return ((IntelAttribute) element).getName();
		}else if (element instanceof IntelEntityTypeAttribute){
			return ((IntelEntityTypeAttribute)element).getAttribute().getName();
		}else if (element instanceof IntelRelationshipTypeAttribute){
			return ((IntelRelationshipTypeAttribute)element).getAttribute().getName();
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element){
		if (element instanceof IntelAttribute){
			IntelAttribute a = (IntelAttribute)element;
			return getImage(a.getType());
		}else if (element instanceof IntelEntityTypeAttribute){
			return getImage(((IntelEntityTypeAttribute)element).getAttribute().getType());
		}else if (element instanceof IntelRelationshipTypeAttribute){
			return getImage(((IntelRelationshipTypeAttribute)element).getAttribute().getType());
		}else if (element instanceof IntelAttribute.AttributeType){
			return getImage(((IntelAttribute.AttributeType) element));
		}
		return super.getImage(element);
	}
	
	public static Image getImage(IntelAttribute.AttributeType type){
		String key = null;
		switch(type){
		case BOOLEAN:
			key = SmartPlugIn.ATTRIBUTE_BOOLEAN_ICON;
			break;
		case DATE:
			key = SmartPlugIn.ATTRIBUTE_DATE_ICON;
			break;
		case LIST:
			key = SmartPlugIn.ATTRIBUTE_LIST_ICON;
			break;
		case NUMERIC:
			key = SmartPlugIn.ATTRIBUTE_NUMBER_ICON;
			break;
		case TEXT:
			key = SmartPlugIn.ATTRIBUTE_TEXT_ICON;
			break;
		default:
			break;
		}
		if (key == null) return null;
		return SmartPlugIn.getDefault().getImageRegistry().get(key);
	}
}