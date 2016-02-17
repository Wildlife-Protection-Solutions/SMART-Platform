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
package org.wcs.smart.connect.i18n.labels;

import java.util.HashMap;
import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
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
		labels.put(AllDatesFilter.class, "QueryDateLabelProvider.AllDatesFilterOp"); //$NON-NLS-1$
		labels.put(CurrentQuarterDateFilter.class, "QueryDateLabelProvider.CurrentQuarterFilterOp"); //$NON-NLS-1$
		labels.put(CustomDateFilter.class, "QueryDateLabelProvider.CustomDateFilterOp"); //$NON-NLS-1$
		labels.put(Last30DaysDateFilter.class, "QueryDateLabelProvider.Last30DatesFilterOp"); //$NON-NLS-1$
		labels.put(Last60DaysDateFilter.class, "QueryDateLabelProvider.Last60DatesFilterOp"); //$NON-NLS-1$
		labels.put(LastMonthFilter.class, "QueryDateLabelProvider.LasTMonthDatesFilterOp"); //$NON-NLS-1$
		labels.put(LastQuarterDateFilter.class, "QueryDateLabelProvider.LastQuarterDatesFilterOp"); //$NON-NLS-1$
		labels.put(LastYearDateFilter.class, "QueryDateLabelProvider.LastYEarDatesFilterOp"); //$NON-NLS-1$
		labels.put(MonthToDateDateFilter.class, "QueryDateLabelProvider.MonthToDateDatesFilterOp"); //$NON-NLS-1$
		labels.put(YearToDateDateFilter.class, "QueryDateLabelProvider.YeartoDateDatesFilterOp"); //$NON-NLS-1$
		labels.put(DayDateGroupBy.class, "QueryDateLabelProvider.DayDatesFilterOp"); //$NON-NLS-1$
		labels.put(MonthDateGroupBy.class, "QueryDateLabelProvider.MonthDatesFilterOp"); //$NON-NLS-1$
		labels.put(YearDateGroupBy.class, "QueryDateLabelProvider.YearDatesFilterOp"); //$NON-NLS-1$
		labels.put(WaypointDateField.class,"QueryDateLabelProvider.WpDateDatesFilterOp"); //$NON-NLS-1$
		
	}
	
	private String getStartEndDateErrorStr(Locale l){
		return Messages.getString("QueryDateLabelProvider.InvalidDateError", l); //$NON-NLS-1$
	}
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item.equals(START_BEFORE_END_ERR)){
			return getStartEndDateErrorStr(l);
		}
		
		return Messages.getString(labels.get(item.getClass()), l);
	}
}
