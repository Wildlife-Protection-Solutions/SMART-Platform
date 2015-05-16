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

import org.wcs.smart.conversion.model.CategoryMap;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedCategory;
import org.wcs.smart.conversion.model.SmartMapping;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class Ct2SmartLookup {

	private SmartMapping ct2Smart;
	private Map<String, MappedAttribute> i2attr = new HashMap<String, MappedAttribute>();

	public Ct2SmartLookup(SmartMapping ct2Smart) {
		this.ct2Smart = ct2Smart;
		for (MappedAttribute ct2a : ct2Smart.getMappedAttribute()) {
			i2attr.put(ct2a.getI(), ct2a);
		}
	}

	public MappedAttribute findAttribute(String i) {
		return i2attr.get(i);
	}
	
	public MappedCategory findCategory(List<Ct2AttributeValuePair> data) {
		//brood force
		for (MappedCategory c : ct2Smart.getMappedCategory()) {
			if (c.getCategoryMap().size() != data.size())
				continue;
			if (c.getCategoryMap().size() == 0)
				return c; //corner case for empty mapping
			boolean match = false;
			for (CategoryMap cmap : c.getCategoryMap()) {
				match = false;
				for (Ct2AttributeValuePair pair : data) {
					if (isSame(cmap.getAi(), pair.attribute.getI()) && isSame(cmap.getVi(), pair.value)) {
						match = true;
						break;
					}
				}
				if (!match) {
					break;
				}
			}
			if (match)
				return c;
		}
		return null;
	}

	private boolean isSame(Object o1, Object o2) {
		if (o1 == null) 
			return o2 == null;
		return o1.equals(o2);
	}
	
	
	public static class Ct2AttributeValuePair {
		public MappedAttribute attribute;
		public String value;
		
		@Override
		public String toString() {
			return "ai=\""+attribute.getI()+"\" vi=\""+value+"\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
}
