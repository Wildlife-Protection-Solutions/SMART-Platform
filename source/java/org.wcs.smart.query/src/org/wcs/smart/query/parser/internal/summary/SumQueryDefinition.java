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

import org.wcs.smart.query.parser.filter.IFilter;

/**
 * A representation of a summary query parsed into the various
 * components that make up the query.  The summary query is made up
 * of a collection of values, a collection or row group bys,
 *  a collection of column group bys, and a filter.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SumQueryDefinition {

	/**
	 * Creates a new summary query definition
	 * @param valuePart the values 
	 * @param rowGroupBy the row group by
	 * @param colGroupBy the column group by
	 * @param queryFilter the filter
	 */
	public static final SumQueryDefinition createQuery(ValuePart valuePart, 
			GroupByPart rowGroupBy, GroupByPart colGroupBy, 
			IFilter valueFilter, IFilter rateFilter){
		return new SumQueryDefinition(valuePart, rowGroupBy, colGroupBy, valueFilter, rateFilter);
	}
	
	
	private ValuePart valuePart;
	private GroupByPart rowGroupBy;
	private GroupByPart colGroupBy;
	private IFilter valueFilter;
	private IFilter rateFilter;
	
	/**
	 * Creates a new summary query definition
	 * @param valuePart
	 * @param rowGroupBy
	 * @param colGroupBy
	 * @param queryFilter
	 */
	protected SumQueryDefinition (ValuePart valuePart, GroupByPart rowGroupBy, 
			GroupByPart colGroupBy, 
			IFilter valueFilter,
			IFilter rateFilter){
		this.valuePart = valuePart;
		this.rowGroupBy = rowGroupBy;
		this.colGroupBy = colGroupBy;
		this.valueFilter = valueFilter;
		this.rateFilter = rateFilter;
	}
	
	/**
	 * @return the value part
	 */
	public ValuePart getValuePart(){
		return this.valuePart;
	}
	/**
	 * @return the row group by part
	 */
	public GroupByPart getRowGroupByPart(){
		return this.rowGroupBy;
	}
	/**
	 * @return the column group by part
	 */
	public GroupByPart getColumnGroupByPart(){
		return this.colGroupBy;
	}
	/**
	 * @return the query value filter (numerator filter)
	 */
	public IFilter getValueFilter(){
		return this.valueFilter;
	}
	
	/**
	 * @return the rate filter (denominator filter)
	 */
	public IFilter getRateFilter(){
		return this.rateFilter;
	}
	
	/**
	 * @return the string representation of the query
	 */
	public String asQuery(){
		return valuePart.asString() + "|" + rowGroupBy.asString() + "|" + colGroupBy.asString() + "|" + (valueFilter == null ? "" : valueFilter.asString() + "|" + (rateFilter == null ? "" : rateFilter.asString())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
