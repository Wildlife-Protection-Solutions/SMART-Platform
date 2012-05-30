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
package org.wcs.smart.patrol.internal.ui.views;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.PatrolType;

/**
 * Filter for the patrol view.  Filters
 * have a date filter, patrol type filter and 
 * a patrol id filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolViewFilter {

	private PatrolType.Type[] types = null;
	private DateFilter dateFilter = DateFilter.LAST_30_DAYS;
	private String patrolIdFilter = null;
	private StringComparison stringComparator = null;
	
	private Date startDate;
	private Date endDate;
	
	
	/**
	 * Types of comparitors for patrol id filters.
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum StringComparison{
		EQUALS("Equals"),
		CONTAINS("Contains");
		
		private String guiName;
		private StringComparison(String guiName){
			this.guiName = guiName;
		}
		public String getGuiName(){
			return this.guiName;
		}
	}
	
	/**
	 * Date filters
	 */
	public enum DateFilter{
		LAST_30_DAYS("Last 30 Days"),
		LAST_60_DAYS("Last 60 Days"),
		YEAR_TO_DATE("Year To Date"),
		MONTH_TO_DATE("Month To Date"),
		CUSTOM("Custom...");
		
		private String guiName;
		
		private DateFilter(String name){
			this.guiName = name;
		}
		public String getGuiName(){
			return this.guiName;
		}
	}
	
	/**
	 * 
	 * @return the current date filter
	 */
	public DateFilter getDateFilter(){
		return this.dateFilter;
	}
	/**
	 * @return the current patrol id string comparator
	 */
	public StringComparison getPatrolIdComparator(){
		return this.stringComparator;
	}
	/**
	 * 
	 * @return current patrol id string filter
	 */
	public String getPatrolIdFilter(){
		return this.patrolIdFilter;
	}
	
	/**
	 * 
	 * @return start date for custom date filter
	 */
	public Date getStartDate(){
		return this.startDate;
	}
	
	/**
	 * 
	 * @return end date for custom date filter
	 */
	public Date getEndDate(){
		return this.endDate;
	}
	
	/**
	 * 
	 * @return patrol type filters
	 */
	public PatrolType.Type[] getPatrolTypeFilters(){
		return this.types;
	}
	
	/**
	 * Resets all values to the default
	 */
	public void setDefaults(){
		this.dateFilter = DateFilter.LAST_30_DAYS;
		this.patrolIdFilter = null;
		this.stringComparator = null;
		this.types = null;
	}
	/**
	 * Sets the date filter.  Set to null
	 * to include all dates;
	 * 
	 * @param dFilter date filter
	 * @param start the start date for custom filter; null if not custom date filter
	 * @param end the end date for custom filter; null if not cusom date filter
	 */
	public void setDateFilter(DateFilter dFilter, Date start, Date end){
		this.dateFilter = dFilter;
		this.startDate = start;
		this.endDate = end;
	}
	
	/**
	 * Sets the patrol types to filter by.  If
	 * null all patrol types will be selected.
	 * 
	 * @param types list of patrol types
	 */
	public void setPatrolTypes(PatrolType.Type[] types){
		this.types = types;
	}
	
	/**
	 * Sets the patrol id filter.  If either are null then
	 * all patrol ids will be included.
	 * 
	 * @param stringComparitor the types of string comparison or null
	 * @param text the text to compare or null
	 */
	public void setPatrolIdFilter(StringComparison stringComparitor, String text){
		this.stringComparator = stringComparitor;
		this.patrolIdFilter = text;
	}
	
	public Query buildQuery(Session s){ 
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT p.uuid, p.id, p.patrolType, p.startDate, p.endDate ");
		str.append("FROM Patrol p ");
		str.append("WHERE p.conservationArea = :ca " );
	
		boolean and = true;
		boolean or = false;
		if (types != null && types.length > 0){
			if (and ){
				str.append(" AND (");
				and = false;
			}
			or = true;
			str.append(" p.patrolType IN (:pt) ");
		}
		if (stringComparator != null && patrolIdFilter != null){
			if (and){
				str.append(" AND (");
				and = false;
			}
			if (or){
				str.append(" AND ");
			}
			or = true;
			str.append(" lower(p.id) like :pid ");
			
		}
		if (dateFilter != null){
			if (and){
				str.append(" AND (");
				and = false;
			}
			if (or){
				str.append(" AND ");
			}
			or = true;
			if (dateFilter != DateFilter.CUSTOM){
				str.append(" p.endDate >= :date1 ");
			}else{
				str.append(" ( p.endDate > :date1 and p.startDate < :date2 ) ");
			}
		}
		if (!and){
			str.append(")");
		}
		
		str.append("ORDER BY p.id desc");
		
		Query query = s.createQuery(str.toString()).setParameter("ca", SmartDB.getCurrentConservationArea());
		if (types != null && types.length > 0){
			query.setParameterList("pt", this.types);
		}
		if (stringComparator != null && patrolIdFilter != null){
			if (stringComparator == StringComparison.CONTAINS){
				query.setParameter("pid", "%" + this.patrolIdFilter.toLowerCase() + "%");
			}else{
				query.setParameter("pid", this.patrolIdFilter.toLowerCase());
			}
		}
		if (dateFilter != null) {

			if (dateFilter == DateFilter.LAST_30_DAYS) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, -30);
				query.setParameter("date1", cal.getTime());
			} else if (dateFilter == DateFilter.LAST_60_DAYS) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, -60);
				query.setParameter("date1", cal.getTime());
			} else if (dateFilter == DateFilter.YEAR_TO_DATE) {
				Calendar cal = Calendar.getInstance();
				cal.set(cal.get(Calendar.YEAR), 0, 01, 0, 0, 0);
				query.setParameter("date1", cal.getTime());
			} else if (dateFilter == DateFilter.MONTH_TO_DATE) {
				Calendar cal = Calendar.getInstance();
				cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 01, 0, 0, 0);
				query.setParameter("date1", cal.getTime());
			} else if (dateFilter == DateFilter.CUSTOM) {
				query.setParameter("date1", startDate);
				query.setParameter("date2", endDate);
			}

		}
		return query;
	}
}
