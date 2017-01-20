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

import java.util.Locale;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelAttribute;

/**
 * Label provider for attribute types
 * @author Emily
 *
 */
public class AttributeTypeLabelProvider extends LabelProvider {
	
	public String getText(Object element){
		if (element instanceof IntelAttribute.AttributeType){
			return ((IntelAttribute.AttributeType) element).getGuiName(Locale.getDefault());
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element){
		if (element instanceof IntelAttribute.AttributeType){
			IntelAttribute.AttributeType a = (IntelAttribute.AttributeType)element;
			return AttributeLabelProvider.getImage(a);
		}
		return super.getImage(element);
	}
}
