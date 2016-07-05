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

import java.text.MessageFormat;
import java.util.Date;

import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
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
	
	/**
	 * Converts the data filter to a string representation
	 */
	public String asString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getDateFieldOption().getKey());
		sb.append(","); //$NON-NLS-1$
		sb.append(getDateFilterOption().getQueryKey());
		sb.append(","); //$NON-NLS-1$
		if (getDateFilterOption().getDates() != null){
			for (Date d : getDateFilterOption().getDates()){
				sb.append(d.getTime());
				sb.append(",");	 //$NON-NLS-1$
			}
		}
		return sb.toString();
	}
	
	/**
	 * Converts a string to a date filter.  If cannot parse
	 * the filter components an exception is thrown.
	 * @param filterString
	 * @return
	 */
	public static DateFilter fromString(String filterString, IQueryType queryType) throws Exception{
		IDateFieldFilter field = null;
		IDateFilter filter = null;
		IDateFieldFilter[] ops = queryType.getDateFilterOptions();
		
		
		String[] bits = filterString.split(","); //$NON-NLS-1$
		//field
		for (IDateFieldFilter op : ops){
			if (op.getKey().equalsIgnoreCase(bits[0])){
				field = op;
				break;
			}
		}
			
		for (IDateFilter f : IDateFilter.DATE_FILTERS){
			if (f.getQueryKey().equalsIgnoreCase(bits[1])){
				filter = f;
				break;
			}
		}
		if (filter instanceof CustomDateFilter){
			((CustomDateFilter)filter).setDates(new Date(Long.parseLong(bits[2])), new Date(Long.parseLong(bits[3])));
		}

		
		//default to all
		if (filter == null) throw new Exception(MessageFormat.format("Date filter part could not be parsed from date filter string: {0}", filterString)); 
		if (field == null)   throw new Exception(MessageFormat.format("Date field part could not be parsed from date filter string: {0}", filterString));
		return new DateFilter(field, filter);
	}
}
