package org.wcs.smart.query.ui;

import java.util.HashMap;
import java.util.Locale;

import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.CurrentQuarterDateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.DateGroupByViewer;
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
