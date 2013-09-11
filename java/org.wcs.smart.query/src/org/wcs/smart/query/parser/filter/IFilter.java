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
package org.wcs.smart.query.parser.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A query filter.
 * @author Emily
 * @since 1.0.0
 */
public interface IFilter {

	public enum FilterType{
		OBSERVATION("observation",Messages.IFilter_ObservationFilterName),  //$NON-NLS-1$
		WAYPOINT("waypoint", Messages.IFilter_IncidentFilterName); //$NON-NLS-1$
	
		private String key;
		private String gui;
		
		FilterType(String key, String gui){
			this.key = key;
			this.gui = gui;
		}
		public String getKey(){
			return this.key;
		}
		public String getGuiName(){
			return this.gui;
		}
		
		
		public static FilterType parse(String type){
			if (type.equals(WAYPOINT.key)){
				return WAYPOINT;
			}
			return OBSERVATION;
		}
	};
	
	/**
	 * A class to represent and empty filter.
	 */
	public IFilter EMPTY_FILTER = new EmptyFilter();
	
	/**
	 * @return the string representation of the filter
	 */
	public String asString();
	
	/**
	 * Converts the filter to an sql where clause expression.
	 * 
	 * If the filter exists in the filterTables map then the filter
	 * can assume that a table exists with the given names that contains
	 * all waypoints that match the filter.  Otherwise the filter needs
	 * to use the tableMapping to filter the data 
	 * 
	 * @param tableMapping a mapping of tables to table prefixes
	 * @param filterTables 
	 * 
	 * @return sql where clause expression
	 */
	public String asSql(HashMap<Class<?>, String> tableMapping, HashMap<IFilter, String> filterTables);

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
