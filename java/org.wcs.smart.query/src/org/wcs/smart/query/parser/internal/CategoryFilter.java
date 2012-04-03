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
package org.wcs.smart.query.parser.internal;

import java.util.HashMap;

import org.wcs.smart.ca.datamodel.Category;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class CategoryFilter implements Filter {

	private String key;
	
	public CategoryFilter(String key){
		this.key = key;
	}
	
	public static CategoryFilter createFilter(String key){
		return new CategoryFilter(key);
	}
	
	
	public String asString(){
		return key;
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#asSql(java.util.HashMap)
	 */
	@Override
	public String asHql(HashMap<Class<?>, String> tableMapping, HashMap<String, Object> parameters) {
		String keyPart = key.split(":")[1];
		
		String prefix = tableMapping.get(Category.class);
		if (prefix == null){
			throw new IllegalStateException("Category prefix could not be determined.");
		}
		String param1 = keyPart;
		String param2 = keyPart + "%";
		String key1 = "p" + String.valueOf(parameters.size());
		String key2 = "p" + String.valueOf(parameters.size() + 1);
		
		parameters.put(key1, param1);
		parameters.put(key2, param2);
		
		return "( " + prefix + ".hkey = :" + key1 + " or " + prefix + ".hkey like :" + key2 + ") ";
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		String keyPart = key.split(":")[1];
		
		String prefix = tableMapping.get(Category.class);
		if (prefix == null){
			throw new IllegalStateException("Category prefix could not be determined.");
		}
		
		return "( " + prefix + ".hkey >= '" + keyPart + "' and " + prefix + ".hkey < '" + keyPart.substring(0,  keyPart.length() -1) + "/') ";
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeTreeItemFilter()
	 */
	@Override
	public boolean hasAttributeTreeItemFilter() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeListItemFilter()
	 */
	@Override
	public boolean hasAttributeListItemFilter() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}
}
