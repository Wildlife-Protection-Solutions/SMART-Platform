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
package org.wcs.smart.query.model.summary;

import org.wcs.smart.query.model.filter.QueryFilter;


/**
 * A representation of a grid query parsed into the various
 * components that make up the query.  The summary query is made up
 * of one or two values and a filter.
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class GridQueryDefinition {

	/**
	 * Creates a new grid query definition
	 * @param valueItem the values 
	 * @param queryFilter the filter
	 */
	public static final GridQueryDefinition createQuery(IValueItem valueItem, 
			Double gridSize, 
			QueryFilter queryFilter,
			QueryFilter rateFilter){
		return new GridQueryDefinition(valueItem, gridSize, queryFilter, rateFilter);
	}
	
	
	private IValueItem valueItem;
	private QueryFilter queryFilter;
	private QueryFilter rateFilter;
	private Double gridSize;
	
	/**
	 * Creates a new summary query definition
	 * @param valueItem
	 * @param gridSize
	 * @param queryFilter
	 */
	protected GridQueryDefinition (IValueItem valueItem, 
			Double gridSize, 
			QueryFilter queryFilter,
			QueryFilter rateFilter){
		this.valueItem = valueItem;
		this.queryFilter = queryFilter;
		this.rateFilter = rateFilter;
		this.gridSize= gridSize;
	}
	
	/**
	 * @return the value part
	 */
	public IValueItem getValuePart(){
		return this.valueItem;
	}
	/**
	 * @return the value query filter
	 */
	public QueryFilter getValueFilter(){
		return this.queryFilter;
	}
	
	/**
	 * @return the rate query filter
	 */
	public QueryFilter getRateFilter(){
		return this.rateFilter;
	}
	
	/**
	 * @return the query filter
	 */
	public Double getGridSize(){
		return this.gridSize;
	}
	
	/**
	 * @return the string representation of the query
	 */
	public String asQuery(){
		return valueItem.asString() + "|" + gridSize.toString() + "|" + (queryFilter == null ? "" : queryFilter.asString()) + "|" + (rateFilter == null ? "" : rateFilter.asString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

}
