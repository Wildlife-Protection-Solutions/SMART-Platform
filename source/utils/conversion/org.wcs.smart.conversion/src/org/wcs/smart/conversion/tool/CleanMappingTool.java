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
package org.wcs.smart.conversion.tool;

import org.wcs.smart.conversion.model.CategoryMap;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeValue;
import org.wcs.smart.conversion.model.MappedCategory;
import org.wcs.smart.conversion.model.SmartMapping;

/**
 * Tool that removes all "n" related records from mapping.
 * Useful when "i" and "n" always duplicate each other, in such cases having "i" will be enough.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class CleanMappingTool {
	
	public void clean(SmartMapping mapping) {
		for (MappedAttribute a : mapping.getMappedAttribute()) {
			a.setN(null);
			for (MappedAttributeValue v : a.getMappedAttributeValue()) {
				v.setN(null);
			}
		}
		
		for (MappedCategory c : mapping.getMappedCategory()) {
			for (CategoryMap m : c.getCategoryMap()) {
				m.setAn(null);
				m.setVn(null);
			}
		}
	}

}
