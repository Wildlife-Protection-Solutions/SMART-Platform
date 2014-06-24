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
package org.wcs.smart.ct2smart.ui.support;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.ui.DataModelLookup;
import org.wcs.smart.ct2smart.util.Ct2AttributeTypeUtil;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SmartAttributeLabelProvider extends ColumnLabelProvider {
	
	private String langCode = "en"; //TODO: make customisable
	private DataModelLookup lookup;
	

	public SmartAttributeLabelProvider(DataModelLookup lookup) {
		super();
		this.lookup = lookup;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Ct2Attribute) {
			Ct2Attribute ct2a = (Ct2Attribute) element;
			return Ct2AttributeTypeUtil.canMap(ct2a.getType()) ? getNameForKey(ct2a.getMapTo()) : "--none--"; 

		} else if (element instanceof String) {
			return getNameForKey((String)element);
		}
		return super.getText(element);
	}
	
	private String getNameForKey(String key) {
		AttributeType a = lookup.getAttribute(key);
		if (a == null)
			return "?"; //TODO: empty string?
		for (NameType nameType : a.getNames()) {
			if (langCode.equals(nameType.getLanguageCode()))
				return nameType.getValue();
		}
		if (!a.getNames().isEmpty())
			return a.getNames().get(0).getValue();
		return "? " + key; //should never happen
	}
}
