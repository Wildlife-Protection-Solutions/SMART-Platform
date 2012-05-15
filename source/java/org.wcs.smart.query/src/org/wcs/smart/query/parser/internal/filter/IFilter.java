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
package org.wcs.smart.query.parser.internal.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A query filter.
 * @author Emily
 * @since 1.0.0
 */
public interface IFilter {

	/**
	 * A class to represent and empty filter.
	 */
	public IFilter EMPTY_FILTER = new IFilter(){

		@Override
		public String asString() {
			return "";
		}

		@Override
		public String asSql(HashMap<Class<?>, String> tableMapping) {
			return "";
		}


		@Override
		public boolean hasCategoryFilter() {
			return false;
		}

		@Override
		public boolean hasAttributeFilter() {
			return false;
		}

		@Override
		public boolean hasEmployeeFilter() {
			return false;
		}

		@Override
		public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
		}
		
		@Override
		public DropItem[] getDropItems(Session session){
			return new DropItem[]{};
		}

		@Override
		public List<IFilter> getChildren() {
			return null;
		}
	};
	
	/**
	 * @return the string representation of the filter
	 */
	public String asString();
	

	/**
	 * Converts the filter to an sql where clause expression
	 * 
	 * @param tableMapping a mapping of tables to table prefixes
	 * @return sql where clause expression
	 */
	public String asSql(HashMap<Class<?>, String> tableMapping);

	
	/**
	 * @return <code>true</code> if the filter includes a data 
	 * model category filter, <code>false</code> otherwise
	 */
	public boolean hasCategoryFilter();

	/**
	 * @return <code>true</code> if the filter includes a data 
	 * model attribute filter, <code>false</code> otherwise
	 */
	public boolean hasAttributeFilter();
	
	/**
	 * @return <code>true</code> if the filter includes a 
	 * patrol member filter, <code>false</code> otherwise
	 */
	public boolean hasEmployeeFilter();
	
	/**
	 * Add the attributes used in the filter to to set of 
	 * attributes. 
	 * 
	 * @param attributes set of attribute info used in the attribute
	 * filter
	 */
	public void getAttributeFilters(HashSet<AttributeInfo> attributes);
	
	/**
	 * Converts the filter to a set of drop items
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public DropItem[] getDropItems(Session session) throws Exception;
	
	/**
	 * @return the children elements that make up this filter or
	 * null if no children elements
	 */
	public List<IFilter> getChildren();
}
