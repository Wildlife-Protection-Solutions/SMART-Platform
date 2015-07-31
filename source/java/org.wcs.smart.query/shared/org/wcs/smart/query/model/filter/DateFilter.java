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

import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;

/**
 * A query date filter.
 * 
 * <p>Date filters take the form:<br>
 * DateField DateFilter</p>
 * 
 *<p> Date filters are not converted to drop items</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DateFilter implements IFilter {

	private IDateFieldFilter dateField;
	private IDateFilter dateFilter;
	
	public DateFilter(IDateFieldFilter dateField, IDateFilter dateFilter){
		this.dateField  = dateField;
		this.dateFilter = dateFilter;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		String str = dateField.getKey() + " " + dateFilter.getQueryKey(); //$NON-NLS-1$
		return str;
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}
	
	/**
	 * @return the date filter option represented by this date filter
	 */
	public IDateFilter getDateFilterOption(){
		return this.dateFilter;
	}
	
	/**
	 * @return the date field option represented by this date filter
	 */
	public IDateFieldFilter getDateFieldOption(){
		return this.dateField;
	}
}
