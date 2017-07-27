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
package org.wcs.smart.intelligence.ui;

import java.util.Date;

import org.hibernate.query.Query;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * Filter for the intelligence view.  Filters
 * have a received date filter, from/to dates filter and name filter.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceViewFilter {	
	
	public enum SortBy{
		RECEIVED_DATE(Messages.IntelligenceViewFilter_ReceivedDateFilterName, "i.receivedDate DESC"), //$NON-NLS-1$
		NAME(Messages.IntelligenceViewFilter_NameFilterName, "lbl.value"); //$NON-NLS-1$
		
		private String guiName;
		private String sqlFilter;
		
		private SortBy(String guiName, String sqlFilter){
			this.guiName = guiName;
			this.sqlFilter = sqlFilter;
		}
		public String getGuiName(){
			return this.guiName;
		}
		public String getSqlFilterField(){
			return this.sqlFilter;
		}
	};
	
	private DateFilter receivedDateFilter;
	private Date receivedDateStart;
	private Date receivedDateEnd;

	private DateFilter relevantDateFilter;
	private Date relevantDateStart;
	private Date relevantDateEnd;

	private StringComparison nameComparison;
	private String name;
	
	private SortBy sortBy = SortBy.RECEIVED_DATE;

	public IntelligenceViewFilter() {
		resetDefaults();
	}
	
	public Query<?> buildQuery(Session s) { 
		boolean label = (nameComparison != null && name != null) || sortBy == SortBy.NAME;
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT i.uuid, i.name, i.receivedDate "); //$NON-NLS-1$
		str.append("FROM Intelligence i "); //$NON-NLS-1$
		if (label) {
			str.append(", Label lbl "); //$NON-NLS-1$
		}
		str.append("WHERE i.conservationArea = :ca " ); //$NON-NLS-1$

		//received date
		if (receivedDateFilter != null) {
			str.append("AND i.receivedDate >= :receivedStart AND i.receivedDate <= :receivedEnd "); //$NON-NLS-1$
		}

		//relevant date
		if (relevantDateFilter != null) {
			str.append("AND coalesce(i.toDate, i.fromDate) >= :relevantStart AND i.fromDate <= :relevantEnd "); //$NON-NLS-1$
		}
		
		//name
		if (label) {
		    str.append("AND  lbl.id.element.uuid = i.uuid AND lbl.id.language = :language "); //$NON-NLS-1$
		    if (nameComparison != null && name != null) {
		    	str.append("AND lower(lbl.value) like :name ");  //$NON-NLS-1$
		    }
		}
		
		if (sortBy != null && sortBy.getSqlFilterField() != null){
			str.append(" ORDER BY " + sortBy.getSqlFilterField()); //$NON-NLS-1$
		}
		
		Query<?> query = s.createQuery(str.toString());
		
		//---- parameters ----
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		//received date
		if (receivedDateFilter != null) {
			Date start = receivedDateFilter.getStartDate();
			if (start == null){
				start = receivedDateStart;
			}
			Date end = receivedDateFilter.getEndDate();
			if (end == null){
				end = receivedDateEnd;
			}
			query.setParameter("receivedStart", start); //$NON-NLS-1$
			query.setParameter("receivedEnd", end); //$NON-NLS-1$
		}

		//relevant date
		if (relevantDateFilter != null) {
			Date start = relevantDateFilter.getStartDate();
			if (start == null){
				start = relevantDateStart;
			}
			Date end = relevantDateFilter.getEndDate();
			if (end == null){
				end = relevantDateEnd;
			}
			query.setParameter("relevantStart", start); //$NON-NLS-1$
			query.setParameter("relevantEnd", end); //$NON-NLS-1$
		}
		
		//name
		if (nameComparison != null && name != null) {
			switch (nameComparison) {
			case CONTAINS:
				query.setParameter("name", "%" + name.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				break;
			case EQUALS:
				query.setParameter("name", name.toLowerCase()); //$NON-NLS-1$
				break;
			}
		}
		if (label){
			query.setParameter("language", SmartDB.getCurrentLanguage()); //$NON-NLS-1$
		}

		return query;
	}	

	public void resetDefaults() {
		this.receivedDateFilter = DateFilter.LAST_60_DAYS;
		this.receivedDateStart = null;
		this.receivedDateEnd = null;

		this.relevantDateFilter = null;
		this.relevantDateStart = null;
		this.relevantDateEnd = null;

		this.nameComparison = null;
		this.name = null;
	}
	
	/**
	 * Sets the received date filter.  Set to null to include all dates;
	 * 
	 * @param filter date filter
	 * @param start the start date for custom filter; null if not custom date filter
	 * @param end the end date for custom filter; null if not cusom date filter
	 */
	public void setReceivedDateFilter(DateFilter filter, Date start, Date end) {
		this.receivedDateFilter = filter;
		this.receivedDateStart = start;
		this.receivedDateEnd = end;
	}

	/**
	 * Sets the relevant date filter.  Set to null to include all dates;
	 * 
	 * @param filter date filter
	 * @param start the start date for custom filter; null if not custom date filter
	 * @param end the end date for custom filter; null if not cusom date filter
	 */
	public void setRelevantDateFilter(DateFilter filter, Date start, Date end) {
		this.relevantDateFilter = filter;
		this.relevantDateStart = start;
		this.relevantDateEnd = end;
	}

	/**
	 * Sets the name filter.  If either are null then
	 * all names will be included.
	 * 
	 * @param comparison the types of string comparison or null
	 * @param text the text to compare or null
	 */
	public void setNameFilter(StringComparison comparison, String text){
		this.nameComparison = comparison;
		this.name = text;
	}

	public DateFilter getReceivedDateFilter() {
		return receivedDateFilter;
	}

	public Date getReceivedDateStart() {
		return receivedDateStart;
	}

	public Date getReceivedDateEnd() {
		return receivedDateEnd;
	}

	public DateFilter getRelevantDateFilter() {
		return relevantDateFilter;
	}

	public Date getRelevantDateStart() {
		return relevantDateStart;
	}

	public Date getRelevantDateEnd() {
		return relevantDateEnd;
	}

	public StringComparison getNameComparison() {
		return nameComparison;
	}

	public String getName() {
		return name;
	}

	public void setSortByField(SortBy field){
		this.sortBy = field;
	}
	public SortBy getSortByField(){
		return this.sortBy;
	}

}
