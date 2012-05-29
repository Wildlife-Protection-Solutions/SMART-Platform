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
package org.wcs.smart.query.parser.internal.filter;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A query date filter.
 * 
 * <p>Date filters take the form</p>
 *
 * DateField DateFilter<br>
 * or<br>
 * DateField DateFilter Date1 Date2<br>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DateFilter implements IFilter {

	/**
	 * Possible date fields for date filters.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public static enum DATE_FIELD_OP{
		WAYPOINT("Waypoint Date", "patrol:waypointdate", "patrol_day"),	
		PATROL_START("Patrol Start", "patrol:startdate", "start_date"),
		PATROL_END("Patrol End", "patrol:enddate", "end_date");
			
		public String guiName;
		public String key;
		public String columnName;
			
		private DATE_FIELD_OP(String name, String key, String columnName){
			this.guiName = name;
			this.key = key;
			this.columnName = columnName;
		}
	}
	
	/**
	 * Possible date filters 
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public static enum DATE_FILTER_OP{
		LAST_30_DAYS("Last 30 Days", "last30days"),
		LAST_60_DAYS("Last 60 Days", "last60days"),
		MONTH_TO_DATE("Month to Date", "monthtodate"),
		LAST_MONTH("Last Month", "lastmonth"),
		YEAR_TO_DATE("Year to Date", "yeartodate"),
		ALL("All Dates", "alldates"),
		CUSTOM("Custom ...", "custom");
		
		public String guiName;
		public String key;
		
		private DATE_FILTER_OP(String name, String key){
			this.guiName = name;
			this.key = key;
		}
	}
	
	private DATE_FIELD_OP dateField;
	private DATE_FILTER_OP dateFilter;
	
	private java.sql.Date startDate;
	private java.sql.Date endDate;
	
	public DateFilter(DATE_FIELD_OP dateField, DATE_FILTER_OP dateFilter){
		this.dateField  = dateField;
		this.dateFilter = dateFilter;
	}
	
	public DateFilter(DATE_FIELD_OP dateField, DATE_FILTER_OP dateFilter, java.sql.Date startDate, java.sql.Date endDate){
		this.dateField  = dateField;
		this.startDate = startDate;
		this.endDate = endDate;
		this.dateFilter = dateFilter;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		String str = dateField.key + " " + dateFilter.key;
		if (startDate != null){
			str += " " + startDate.toString();
		}else if (endDate != null){
			str += " " + endDate.toString();
		}
		return str;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		String tablePrefix = "";
		if (this.dateField == DATE_FIELD_OP.PATROL_END || this.dateField == DATE_FIELD_OP.PATROL_START){
			tablePrefix = tableMapping.get(Patrol.class);
		}else if (this.dateField == DATE_FIELD_OP.WAYPOINT){
			tablePrefix = tableMapping.get(PatrolLegDay.class);
		}else{
			assert false;
			//error
		}
		String field = tablePrefix + "." + dateField.columnName;
		return asSql(field);
	}
	
	/**
	 * @return the date filter option represented by this date filter
	 */
	public DATE_FILTER_OP getDateFilterOption(){
		return this.dateFilter;
	}
	
	public static java.sql.Date[] findDates(DATE_FILTER_OP filter){
		GregorianCalendar cal = new GregorianCalendar();

		if (filter == DATE_FILTER_OP.LAST_30_DAYS) {
			cal.setTimeInMillis((long) (cal.getTime().getTime() - (30 * 24 * 60 * 60 * 1000.0)));
			java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
			return new java.sql.Date[] { d };
		} else if (filter == DATE_FILTER_OP.LAST_60_DAYS) {
			cal.setTimeInMillis((long) (cal.getTime().getTime() - (60 * 24 * 60 * 60 * 1000.0)));
			java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
			return new java.sql.Date[] { d };

		} else if (filter == DATE_FILTER_OP.MONTH_TO_DATE) {
			cal.set(Calendar.DAY_OF_MONTH, 1);
			java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
			return new java.sql.Date[] { d };

		} else if (filter == DATE_FILTER_OP.YEAR_TO_DATE) {
			cal.set(Calendar.MONTH, 0);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
			return new java.sql.Date[] { d };
		} else if (filter == DATE_FILTER_OP.LAST_MONTH){
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			
			int lastyear = year;
			int lastmonth = month;
			lastmonth --;
			if (lastmonth < 0){
				lastmonth = 11;
				lastyear --;
			}
			
			cal.set(Calendar.DAY_OF_MONTH, 1);
			java.sql.Date d1 = new java.sql.Date(cal.getTimeInMillis());
			
			cal.set(Calendar.MONTH, lastmonth);
			cal.set(Calendar.YEAR, lastyear);
			java.sql.Date d2 = new java.sql.Date(cal.getTimeInMillis());
			return new java.sql.Date[]{d2, d1};
		}
		return null;
	}
	
	/**
	 * Determine start and end date of date filter  
	 * 
	 * @param op the date filter
	 * @return the dates associated with the date filter
	 */
	public java.sql.Date[] getDates() {
		java.sql.Date[] dates = findDates(dateFilter);
		if (dates == null){
			if (dateFilter == DATE_FILTER_OP.CUSTOM){
				return new java.sql.Date[]{startDate, endDate};
			}
		}
		return dates;

	}
	
	private String asSql(String field){
		java.sql.Date[] bits = getDates(); 
		String f = "";
		if (dateFilter == DATE_FILTER_OP.LAST_30_DAYS ||
				dateFilter == DATE_FILTER_OP.LAST_60_DAYS ||
						dateFilter == DATE_FILTER_OP.MONTH_TO_DATE ||
								dateFilter == DATE_FILTER_OP.YEAR_TO_DATE
				){
			f =  " ( " + field + " >= '" + bits[0].toString() + "' ) ";
		}else if (dateFilter == DATE_FILTER_OP.LAST_MONTH){
			f = " ( " + field + " >= '" + bits[0].toString() + "' and " + field  + " < '" + bits[1].toString() + "' ) ";
		}else if (dateFilter == DATE_FILTER_OP.CUSTOM){
			f = " ( " + field + " >= '" + bits[0].toString() + "' and " + field  + " <= '" + bits[1].toString() + "' ) ";
			
		}
		return f;
	}


	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}
	
	/**
	 * There are no drop items for dates
	 * @return null
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		return null;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}

}
