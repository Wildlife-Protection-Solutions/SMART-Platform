/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i81n.labels;

import java.util.HashMap;
import java.util.Locale;

import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.CurrentQuarterDateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.IQueryDateLabelProvider;
import org.wcs.smart.query.model.filter.date.Last30DaysDateFilter;
import org.wcs.smart.query.model.filter.date.Last60DaysDateFilter;
import org.wcs.smart.query.model.filter.date.LastMonthFilter;
import org.wcs.smart.query.model.filter.date.LastQuarterDateFilter;
import org.wcs.smart.query.model.filter.date.LastYearDateFilter;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.MonthToDateDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.filter.date.YearDateGroupBy;
import org.wcs.smart.query.model.filter.date.YearToDateDateFilter;

/**
 * Implementation for date label provider.
 * 
 * @author Emily
 *
 */
public class QueryDateLabelProvider implements IQueryDateLabelProvider {

	private HashMap<Class<?>, String> labels = new HashMap<Class<?>, String>();
	
	public QueryDateLabelProvider(){
		labels.put(AllDatesFilter.class, "All Dates");
		labels.put(CurrentQuarterDateFilter.class, "Current Quarter");
		labels.put(CustomDateFilter.class, "Custom...");
		labels.put(Last30DaysDateFilter.class, "Last 30 Days");
		labels.put(Last60DaysDateFilter.class, "Last 60 Days");
		labels.put(LastMonthFilter.class, "Last Month");
		labels.put(LastQuarterDateFilter.class, "Last Quarter");
		labels.put(LastYearDateFilter.class, "Last Year");
		labels.put(MonthToDateDateFilter.class, "Month To Date");
		labels.put(YearToDateDateFilter.class, "Year To Date");
		labels.put(DayDateGroupBy.class, "Day");
		labels.put(MonthDateGroupBy.class, "Month");
		labels.put(YearDateGroupBy.class, "Year");
		labels.put(WaypointDateField.class,"Waypoint Date");
		
	}
	
	private String getStartEndDateErrorStr(){
		return "End date must be after start date.";
	}
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item.equals(START_BEFORE_END_ERR)){
			return getStartEndDateErrorStr();
		}
		return labels.get(item.getClass());
	}
}
