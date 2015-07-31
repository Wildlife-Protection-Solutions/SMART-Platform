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
package org.wcs.smart.query.model.filter;


/**
 * Query filter that wraps the filter with the filter type.
 * 
 * @author Emily
 *
 */
public class QueryFilter {

	/**
	 * Creates a new query filter of type observation
	 * @param filter
	 */
	public static QueryFilter createFilter(IFilter filter){
		return new QueryFilter(filter);
	}
	
	/**
	 * Creates a new query filter of the given type
	 * @param filter
	 * @param filterType
	 */
	public static QueryFilter createFilter(IFilter filter, IFilter.FilterType filterType){
		return new QueryFilter(filter, filterType);
	}
	
	private IFilter filter;
	private IFilter.FilterType filterType;
	
	/**
	 * Creates a new query filter of type observation
	 * @param filter
	 */
	public QueryFilter(IFilter filter){
		this.filter = filter;
		this.filterType = IFilter.FilterType.OBSERVATION;
	}
	
	/**
	 * Creates a new query filter of the given type
	 * @param filter
	 * @param filterType
	 */
	public QueryFilter(IFilter filter, IFilter.FilterType filterType){
		this.filter = filter;
		this.filterType = filterType;
	}
	
	/**
	 * 
	 * @return the string representation of the query
	 */
	public String asString(){
		return filterType.getKey() + "|" + filter.asString(); //$NON-NLS-1$
	}
	
	/**
	 * 
	 * @return the filter
	 */
	public IFilter getFilter(){
		return this.filter;
	}
	
	/**
	 * 
	 * @return the filter type
	 */
	public IFilter.FilterType getFilterType(){
		return this.filterType;
	}
}
