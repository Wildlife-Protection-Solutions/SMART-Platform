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
package org.wcs.smart.query.model.filter.date;

import java.sql.Date;
import java.util.Locale;

/**
 * Date filter interface
 * @author Emily
 *
 */
public interface IDateFilter {

	/**
	 * Valid date filters for the given query type
	 * @return
	 */
	public static IDateFilter[] DATE_FILTERS = 
		new IDateFilter[]{
				Last30DaysDateFilter.INSTANCE,
				Last60DaysDateFilter.INSTANCE,
				MonthToDateDateFilter.INSTANCE,
				LastMonthFilter.INSTANCE,
				CurrentQuarterDateFilter.INSTANCE,
				LastQuarterDateFilter.INSTANCE,
				YearToDateDateFilter.INSTANCE,
				LastYearDateFilter.INSTANCE,
				AllDatesFilter.INSTANCE,
				new CustomDateFilter()};
	
	/**
	 * The GUI name of the filter.
	 * @return
	 */
	public String getGuiName(Locale l);
	
	/**
	 * the unique key used in querys
	 * @return
	 */
	public String getQueryKey();
	
	/**
	 * 
	 * @return the label associated with the filter; generally
	 * this is the date range represented by the filter.  May
	 * be null.
	 */
	public String getLabel();
	
	/**
	 * 
	 * @return the dates associated with the filter
	 */
	public Date[] getDates();
	
	/**
	 * 
	 * @return error message if date filter not valid (start date after
	 * the end date) or null
	 */
	public String validate();
	
	/**
	 * If the last date in the filter should be inclusive
	 * of the query results.  If false the query should return everything up
	 * to the last date, but not including the last date.  The start date 
	 * is always inclusive.
	 * @return
	 */
	public boolean isEndDateInclusive();
	
}
