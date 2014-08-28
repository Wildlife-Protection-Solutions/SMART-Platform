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
package org.wcs.smart.er.hibernate;

import java.util.Date;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Filter for survey list.  Provides the ability to filter based on
 * survey state, name, start date and end date.
 * 
 * @author Emily
 *
 */
public class SurveyFilter {

	private SurveyDesign.State[] states = null;
	
	private DateFilter startDateFilter = null;
	private Date sstartDate;
	private Date sendDate;
	
	private DateFilter endDateFilter = null;
	private Date estartDate;
	private Date eendDate;
	
	private String surveyNameFilter = null;
	private StringComparison stringComparator = null;
	
	
	/**
	 * 
	 * @return the start date filter
	 */
	public DateFilter getStartDateFilter(){
		return this.startDateFilter;
	}
	
	/**
	 * 
	 * @return the start date filter
	 */
	public DateFilter getEndDateFilter(){
		return this.endDateFilter;
	}
	
	/**
	 * @return the current survey name string comparator
	 */
	public StringComparison getSurveyNameComparator(){
		return this.stringComparator;
	}
	/**
	 * 
	 * @return current survey name filter string
	 */
	public String getSurveyNameFilter(){
		return this.surveyNameFilter;
	}
	
	/**
	 * 
	 * @return start date for custom start date filter
	 */
	public Date getStartStartDate(){
		return this.sstartDate;
	}
	
	/**
	 * 
	 * @return end date for custom start date filter
	 */
	public Date getStartEndDate(){
		return this.sendDate;
	}
	

	/**
	 * 
	 * @return start date for custom end date filter
	 */
	public Date getEndStartDate(){
		return this.estartDate;
	}
	
	/**
	 * 
	 * @return end date for custom end date filter
	 */
	public Date getEndEndDate(){
		return this.eendDate;
	}
	
	/**
	 * 
	 * @return patrol type filters
	 */
	public SurveyDesign.State[] getSurveyStateFilters(){
		return this.states;
	}
	
	/**
	 * Resets all values to the default
	 */
	public void setDefaults(){
		this.startDateFilter = null;
		this.endDateFilter = null;
		this.sendDate = null;
		this.sstartDate = null;
		this.eendDate = null;
		this.estartDate = null;
		
		this.states = new SurveyDesign.State[]{SurveyDesign.State.ACTIVE};
		
		this.surveyNameFilter = null;
		this.stringComparator = null;
	}
	
	
	/**
	 * Sets the start date filter.  
	 * Set to null to include all dates;
	 * 
	 * @param dFilter date filter
	 * @param start the start date for custom filter; null if not custom date filter
	 * @param end the end date for custom filter; null if not custom date filter
	 */
	public void setStartDateFilter(DateFilter dFilter, Date start, Date end){
		this.startDateFilter = dFilter;
		this.sstartDate = start;
		this.sendDate = end;
	}
	
	/**
	 * Sets the end date filter.  
	 * Set to null to include all dates;
	 * 
	 * @param dFilter date filter
	 * @param start the start date for custom filter; null if not custom date filter
	 * @param end the end date for custom filter; null if not custom date filter
	 */
	public void setEndDateFilter(DateFilter dFilter, Date start, Date end){
		this.endDateFilter = dFilter;
		this.estartDate = start;
		this.eendDate = end;
	}
	
	/**
	 * Sets the survey states to fill.  Set to null to 
	 * include all states.
	 * 
	 * @param types list of patrol types
	 */
	public void setSurveyStates(SurveyDesign.State[] states){
		this.states = states;
	}
	
	/**
	 * Sets the survey name filter.  Set to null
	 * to include all surveys;
	 * 
	 * @param stringComparitor the types of string comparison or null
	 * @param text the text to compare or null
	 */
	public void setSurveyNameFilter(StringComparison stringComparitor, String text){
		this.stringComparator = stringComparitor;
		this.surveyNameFilter = text;
	}
	
	/**
	 * Builds a query that returns the following patrol fields:
	 * patrol uuid, patrol id, patrol type, start date, end date
	 * 
	 * @param s
	 * @return
	 */
//	public Query buildQuery(Session s){ 
//		StringBuilder str = new StringBuilder();
//		
//		str.append("SELECT s.uuid, s.state, s.startDate, s.endDate, lbl.value "); //$NON-NLS-1$
//		str.append("FROM SurveyDesign s, Label lbl "); //$NON-NLS-1$
//		str.append("WHERE s.conservationArea = :ca " ); //$NON-NLS-1$
//		str.append("AND  lbl.id.element.uuid = s.uuid AND lbl.id.language = :language "); //$NON-NLS-1$
//
//		
//		boolean and = true;
//		boolean or = false;
//		if (states != null && states.length > 0){
//			if (and ){
//				str.append(" AND ("); //$NON-NLS-1$
//				and = false;
//			}
//			or = true;
//			str.append(" s.state IN (:states) "); //$NON-NLS-1$
//		}
//		if (stringComparator != null && surveyNameFilter != null){
//			if (and){
//				str.append(" AND ("); //$NON-NLS-1$
//				and = false;
//			}
//			if (or){
//				str.append(" AND "); //$NON-NLS-1$
//			}
//			or = true;
//			str.append(" lower(lbl.value) like :name "); //$NON-NLS-1$
//			
//		}
//		if (startDateFilter != null){
//			if (and){
//				str.append(" AND ("); //$NON-NLS-1$
//				and = false;
//			}
//			if (or){
//				str.append(" AND "); //$NON-NLS-1$
//			}
//			or = true;
//			str.append(" ( s.startDate >= :date1 and s.startDate <= :date2 ) "); //$NON-NLS-1$
//		}
//		
//		if (endDateFilter != null){
//			if (and){
//				str.append(" AND ("); //$NON-NLS-1$
//				and = false;
//			}
//			if (or){
//				str.append(" AND "); //$NON-NLS-1$
//			}
//			or = true;
//			str.append(" ( s.endDate >= :date3 and s.endDate <= :date4 ) "); //$NON-NLS-1$
//		}
//		if (!and){
//			str.append(")"); //$NON-NLS-1$
//		}
//		
//		str.append("ORDER BY lbl.value desc"); //$NON-NLS-1$
//		
//		Query query = s.createQuery(str.toString())
//				.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
//				.setParameter("language", SmartDB.getCurrentLanguage()); //$NON-NLS-1$
//
//		
//		if (states != null && states.length > 0){
//			query.setParameterList("states", this.states); //$NON-NLS-1$
//		}
//		if (stringComparator != null && surveyNameFilter != null){
//			if (stringComparator == StringComparison.CONTAINS){
//				query.setParameter("name", "%" + this.surveyNameFilter.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//			}else{
//				query.setParameter("name", this.surveyNameFilter.toLowerCase()); //$NON-NLS-1$
//			}
//		}
//		if (startDateFilter != null) {
//			Date start = startDateFilter.getStartDate();
//			if (start == null){
//				start = sstartDate;
//			}
//			Date end = startDateFilter.getEndDate();
//			if (end == null){
//				end = sendDate;
//			}
//			query.setParameter("date1", start); //$NON-NLS-1$
//			query.setParameter("date2", end); //$NON-NLS-1$
//		}
//		
//		if (endDateFilter != null) {
//			Date start = endDateFilter.getStartDate();
//			if (start == null){
//				start = estartDate;
//			}
//			Date end = endDateFilter.getEndDate();
//			if (end == null){
//				end = eendDate;
//			}
//			query.setParameter("date3", start); //$NON-NLS-1$
//			query.setParameter("date4", end); //$NON-NLS-1$
//		}
//		return query;
//	}

	public Query buildQuery(Session s){ 
		Query q =  s.createQuery("FROM Survey s WHERE s.surveyDesign.conservationArea = :ca ");
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		return q;
		
	}
}