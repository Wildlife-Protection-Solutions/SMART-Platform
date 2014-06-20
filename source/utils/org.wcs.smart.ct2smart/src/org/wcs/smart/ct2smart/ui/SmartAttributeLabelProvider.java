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
package org.wcs.smart.ct2smart.ui;

import java.io.StringWriter;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SmartAttributeLabelProvider extends ColumnLabelProvider {
	
	private String langCode = "en"; //TODO: make customisable
	private Map<String, AttributeType> key2Attribute;
	

	public SmartAttributeLabelProvider(Map<String, AttributeType> key2Attribute) {
		super();
		this.key2Attribute = key2Attribute;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Ct2Attribute) {
			Ct2Attribute ct2a = (Ct2Attribute) element;
			switch (ct2a.getType()) {
				case TEXT:
				case NUMERIC:
				case BOOL:
				case REF:
					break;
				default:
					return "--not aplicable--";
			}
			AttributeType a = key2Attribute.get(ct2a.getMapTo());
			if (a == null)
				return "?"; //TODO: empty string?
			for (NameType nameType : a.getNames()) {
				if (langCode.equals(nameType.getLanguageCode()))
					return nameType.getValue();
			}
			if (!a.getNames().isEmpty())
				return a.getNames().get(0).getValue();
		} else if (element instanceof String) {
			
		}
		return super.getText(element);
	}
	
}
