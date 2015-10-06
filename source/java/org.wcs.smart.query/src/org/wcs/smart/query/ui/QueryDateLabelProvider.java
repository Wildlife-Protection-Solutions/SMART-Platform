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
package org.wcs.smart.query.ui;

import java.util.HashMap;
import java.util.Locale;

import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.CurrentQuarterDateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.EndHourGroupBy;
import org.wcs.smart.query.model.filter.date.IQueryDateLabelProvider;
import org.wcs.smart.query.model.filter.date.Last30DaysDateFilter;
import org.wcs.smart.query.model.filter.date.Last60DaysDateFilter;
import org.wcs.smart.query.model.filter.date.LastMonthFilter;
import org.wcs.smart.query.model.filter.date.LastQuarterDateFilter;
import org.wcs.smart.query.model.filter.date.LastYearDateFilter;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.MonthToDateDateFilter;
import org.wcs.smart.query.model.filter.date.StartHourGroupBy;
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
		labels.put(AllDatesFilter.class, Messages.PatrolQueryOptions_DateOpAll);
		labels.put(CurrentQuarterDateFilter.class, Messages.PatrolQueryOptions_DateOpCurrentQuarter);
		labels.put(CustomDateFilter.class, Messages.PatrolQueryOptions_DateOpCustom);
		labels.put(Last30DaysDateFilter.class, Messages.PatrolQueryOptions_DateOp_Last30);
		labels.put(Last60DaysDateFilter.class, Messages.PatrolQueryOptions_DateOpLast60);
		labels.put(LastMonthFilter.class, Messages.PatrolQueryOptions_DateOpLastMonth);
		labels.put(LastQuarterDateFilter.class, Messages.PatrolQueryOptions_DateOpLastQuarter);
		labels.put(LastYearDateFilter.class, Messages.PatrolQueryOptions_DateOpLastYear);
		labels.put(MonthToDateDateFilter.class, Messages.PatrolQueryOptions_DateOpCurrentMonth);
		labels.put(YearToDateDateFilter.class, Messages.PatrolQueryOptions_DateOpYearToDate);
		labels.put(DayDateGroupBy.class, Messages.DayDateGroupBy_GroupByDayName);
		labels.put(MonthDateGroupBy.class, Messages.MonthDateGroupBy_MonthGroupByName);
		labels.put(YearDateGroupBy.class, Messages.YearDateGroupBy_GroupByYearLabel);
		labels.put(WaypointDateField.class, Messages.WaypointDateField_WaypointDateFieldName);
		labels.put(StartHourGroupBy.class, Messages.QueryDateLabelProvider_StartHourLabel);
		labels.put(EndHourGroupBy.class, Messages.QueryDateLabelProvider_EndHourLabel);
	}
	
	private String getStartEndDateErrorStr(){
		return Messages.QueryDateLabelProvider_StartEndDateError;
	}
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item.equals(START_BEFORE_END_ERR)){
			return getStartEndDateErrorStr();
		}
		return labels.get(item.getClass());
	}
}
