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

import java.util.Map;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.util.Ct2AttributeTypeUtil;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SmartCategoryLabelProvider extends ColumnLabelProvider {
	
	private String langCode = "en"; //TODO: make customisable
	private Map<String, CategoryType> key2Category;
	
	public SmartCategoryLabelProvider(Map<String, CategoryType> key2Category) {
		super();
		this.key2Category = key2Category;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Ct2Attribute) {
			Ct2Attribute ct2a = (Ct2Attribute) element;
			return Ct2AttributeTypeUtil.canMap(ct2a.getType()) ? getNameForKey(ct2a.getCategoryKey()) : "--none--"; 

		} else if (element instanceof String) {
			return getNameForKey((String)element);
		}
		return super.getText(element);
	}
	
	private String getNameForKey(String key) {
		CategoryType c = key2Category.get(key);
		if (c == null)
			return "--default--";
		for (NameType nameType : c.getNames()) {
			if (langCode.equals(nameType.getLanguageCode()))
				return nameType.getValue();
		}
		if (!c.getNames().isEmpty())
			return c.getNames().get(0).getValue();
		return "? " + key; //should never happen
	}
	
}
