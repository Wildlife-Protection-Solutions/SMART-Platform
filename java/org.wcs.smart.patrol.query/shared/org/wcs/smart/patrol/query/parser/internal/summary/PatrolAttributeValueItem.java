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
package org.wcs.smart.patrol.query.parser.internal.summary;

import org.wcs.smart.query.model.summary.AttributeValueItem;

/**
 * Creates a new value item that represents
 * a numeric attribute or a 
 * numeric attribute and it's associated category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolAttributeValueItem extends AttributeValueItem {
	
	/**
	 * Creates a new attribute value item of the form
	 * < ATTRIBUTE_VALUE_KEY : "attribute:n:" < AGG > ":" < ATTRIBUTE_KEY >
	 * 
	 * @param key
	 * @return
	 */
	public static PatrolAttributeValueItem createAttributeItem(String key){
		return new PatrolAttributeValueItem(key, false);
	}
	
	/**
	 * Creates a new attribute value item with category filter of the
	 * form:
	 * |    < SUM_CAT_ATT_VALUE_KEY : "category:" < DM_KEY > ":attribute:n:" < AGG > ":" < DM_KEY > >
	 * 
	 * @param key
	 * @return
	 */
	public static PatrolAttributeValueItem createCategoryAttributeItem(String key){
		return new PatrolAttributeValueItem(key, true);
	}

	
	/**
	 * Creates a new value item from the given key.
	 * @param key key
	 * @param includeCategory if the key includes a category
	 */
	public PatrolAttributeValueItem(String key, boolean includeCategory){
		super(key, includeCategory);
	}
	
}
