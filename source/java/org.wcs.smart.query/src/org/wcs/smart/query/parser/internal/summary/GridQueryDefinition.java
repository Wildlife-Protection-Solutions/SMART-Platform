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
package org.wcs.smart.query.parser.internal.summary;

import org.wcs.smart.query.parser.internal.filter.IFilter;

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
	public static final GridQueryDefinition createQuery(IValueItem valueItem, Double gridSize, IFilter queryFilter){
		return new GridQueryDefinition(valueItem, gridSize, queryFilter);
	}
	
	
	private IValueItem valueItem;
	private IFilter queryFilter;
	private Double gridSize;
	
	/**
	 * Creates a new summary query definition
	 * @param valueItem
	 * @param gridSize
	 * @param queryFilter
	 */
	protected GridQueryDefinition (IValueItem valueItem, Double gridSize, IFilter queryFilter){
		this.valueItem = valueItem;
		this.queryFilter = queryFilter;
		this.gridSize= gridSize;
	}
	
	/**
	 * @return the value part
	 */
	public IValueItem getValuePart(){
		return this.valueItem;
	}
	/**
	 * @return the query filter
	 */
	public IFilter getQueryFilter(){
		return this.queryFilter;
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
		return valueItem.asString() + "|" + gridSize.toString() + "|" + (queryFilter == null ? "" : queryFilter.asString());
	}

}
