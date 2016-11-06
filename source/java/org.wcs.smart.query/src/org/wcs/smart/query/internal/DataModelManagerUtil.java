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
package org.wcs.smart.query.internal;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;

/**
 * Util methods for datamodel manager.
 * 
 * @author elitvin
 * @since 4.1.0
 */
public class DataModelManagerUtil {

	/**
	 * Determines if attribute in the data model
	 * has an active category association
	 */
	public static boolean isActive(Attribute a, DataModel dm){
		for (Category c : dm.getActiveCategories()){
			if (isActive(c, a)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determines if the attribute has an active association
	 * with the given category or subcategory
	 */
	public static boolean isActive(Category c, Attribute a){
		for (CategoryAttribute ca : c.getAttributes()){
			if (ca.getAttribute().equals(a) && ca.getIsActive()){
				return true;
			}
		}
		for (Category kid : c.getActiveChildren()){
			if (isActive(kid,a)){
				return true;
			}
		}
		return false;
	}
		
}
