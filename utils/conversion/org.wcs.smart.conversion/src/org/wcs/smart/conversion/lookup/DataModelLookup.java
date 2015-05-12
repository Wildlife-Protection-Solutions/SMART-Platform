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
package org.wcs.smart.conversion.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class DataModelLookup {
	
	private DataModel dataModel;

	private Map<String, AttributeType> key2Attribute;
	private Map<String, CategoryType> key2Category;
	private Map<CategoryType, String> category2Key;
	private Map<CategoryType, CategoryType> category2Parent;

	public DataModelLookup(DataModel dataModel) {
		this.dataModel = dataModel;
		key2Attribute = createKey2AttributeMap(dataModel);
		
		key2Category = new HashMap<String, CategoryType>();
		category2Key = new HashMap<CategoryType, String>();
		category2Parent = new HashMap<CategoryType, CategoryType>();
		addChildCategories(null, dataModel.getCategories().getCategories(), ""); //$NON-NLS-1$
	}

	public DataModel getDataModel() {
		return dataModel;
	}
	
	public AttributeType getAttribute(String key) {
		return key2Attribute.get(key);
	}
	
	public CategoryType getCategory(String fullKey) {
		if (fullKey == null)
			return null;
		return key2Category.get(fullKey);
	}
	
	public CategoryType getParent(CategoryType categoty) {
		return category2Parent.get(categoty);
	}

	public String getFullKey(CategoryType categoty) {
		if (categoty == null)
			return null;
		return category2Key.get(categoty);
	}
	
	private Map<String, AttributeType> createKey2AttributeMap(DataModel dm) {
		Map<String, AttributeType> map = new HashMap<String, AttributeType>();
		for (AttributeType a : dm.getAttributes().getAttributes()) {
			map.put(a.getKey(), a);
		}
		return map;
	}

	private void addChildCategories(CategoryType parent, List<CategoryType> children, String parentKey) {
		for (CategoryType c : children) {
			String key = parentKey + c.getKey() + "."; //$NON-NLS-1$
			key2Category.put(key, c);
			category2Key.put(c, key);
			category2Parent.put(c, parent);
			addChildCategories(c, c.getCategories(), key);
		}
	}
	
}
