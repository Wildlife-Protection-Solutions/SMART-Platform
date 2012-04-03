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
public class BracketFilter implements Filter{

	private Filter filter;
	public BracketFilter(Filter filter){
		this.filter = filter;
	}
	public String asString(){
		return "(" + filter.asString() + ")";
	}
	
	public static BracketFilter createFilter(Filter f){
		return new BracketFilter(f);
	}
	
	public String asHql(HashMap<Class<?>, String> tableMapping, HashMap<String, Object> parameters){
		return "(" + filter.asHql(tableMapping, parameters) + ")";
	}
	
	
	public String asSql(HashMap<Class<?>, String> tableMapping){
		return "(" + filter.asSql(tableMapping) + ")";
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeTreeItemFilter()
	 */
	@Override
	public boolean hasAttributeTreeItemFilter() {
		return filter.hasAttributeTreeItemFilter();
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeListItemFilter()
	 */
	@Override
	public boolean hasAttributeListItemFilter() {
		return filter.hasAttributeListItemFilter();
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return filter.hasEmployeeFilter();
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		// TODO Auto-generated method stub
		return filter.hasCategoryFilter();
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		// TODO Auto-generated method stub
		return filter.hasAttributeFilter();
	}
	
}
