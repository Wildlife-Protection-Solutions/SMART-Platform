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
 * survey design state, survey id, survey start date, and survey design.
 * 
 * @author Emily
 *
 */
public class SurveyFilter {

	private SurveyDesign.State state = null;
	
	private DateFilter dateFilter = null;
	private Date startDate;
	private Date endDate;
	
	private String surveyNameFilter = null;
	private StringComparison stringComparator = null;
	
	private String[] surveyDesignKeys = null;
	
	/**
	 * Creates a new filter with the default values.
	 */
	public SurveyFilter(){
		setDefaults();
	}
	
	/**
	 * 
	 * @return the start date filter
	 */
	public DateFilter getDateFilter(){
		return this.dateFilter;
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
	public Date getStartDate(){
		return this.startDate;
	}
	
	/**
	 * 
	 * @return end date for custom start date filter
	 */
	public Date getEndDate(){
		return this.endDate;
	}
	

	/**
	 * 
	 * @return patrol type filters
	 */
	public SurveyDesign.State getSurveyStateFilters(){
		return this.state;
	}
	
	/**
	 * 
	 * @return survey design keys to filter by
	 */
	public String[] getDesignKeys(){
		return this.surveyDesignKeys;
	}
	
	/**
	 * Resets all values to the default
	 */
	public void setDefaults(){
		this.dateFilter = null;
		this.endDate = null;
		this.startDate = null;
		
		this.state = SurveyDesign.State.ACTIVE;
		
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
	public void setDateFilter(DateFilter dFilter, Date start, Date end){
		this.dateFilter = dFilter;
		this.startDate = start;
		this.endDate = end;
	}
	
	
	/**
	 * Sets the survey states in include.  Set to null
	 * if you want to include all or only selected 
	 * survey design ids
	 * 
	 * @param types list of patrol types
	 */
	public void setSurveyState(SurveyDesign.State state){
		this.state = state;
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
	 * Set to null to ignore filter
	 * @param keys
	 */
	public void setSurveyDesignKeyFilters(String[] keys){
		this.surveyDesignKeys = keys;
	}
	
	/**
	 * Builds a query that returns the following survey fields:
	 * {survey uuid, survey id, start date, survey design name}
	 * 
	 * @param s
	 * @return
	 */
	public Query buildQuery(Session s){ 
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT s.uuid, s.id, s.startDate, sd.name "); //$NON-NLS-1$
		str.append("FROM Survey s JOIN s.surveyDesign sd "); //$NON-NLS-1$
		str.append("WHERE sd.conservationArea = :ca " ); //$NON-NLS-1$

		boolean and = true;
		boolean or = false;
		if (state != null ){
			if (and ){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			or = true;
			str.append(" sd.state = :states "); //$NON-NLS-1$
		}else if (state == null && surveyDesignKeys != null){
			if (and){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			or = true;
			str.append( " sd.keyId in (:keys) "); //$NON-NLS-1$
		}
		
		if (stringComparator != null && surveyNameFilter != null){
			if (and){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			if (or){
				str.append(" AND "); //$NON-NLS-1$
			}
			or = true;
			str.append(" lower(s.id) like :name "); //$NON-NLS-1$
			
		}
		if (dateFilter != null){
			if (and){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			if (or){
				str.append(" AND "); //$NON-NLS-1$
			}
			or = true;
			str.append(" ( s.startDate >= :date1 and s.endDate <= :date2 ) "); //$NON-NLS-1$
		}
		if (!and){
			str.append(")"); //$NON-NLS-1$
		}
		
		str.append("ORDER BY s.startDate desc, s.id asc "); //$NON-NLS-1$
		
		Query query = s.createQuery(str.toString())
				.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
			
		if (state != null ){
			query.setParameter("states", this.state); //$NON-NLS-1$
		}else if (state == null && surveyDesignKeys != null){
			query.setParameterList("keys", surveyDesignKeys); //$NON-NLS-1$
		}
		
		if (stringComparator != null && surveyNameFilter != null){
			if (stringComparator == StringComparison.CONTAINS){
				query.setParameter("name", "%" + this.surveyNameFilter.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query.setParameter("name", this.surveyNameFilter.toLowerCase()); //$NON-NLS-1$
			}
		}
		if (dateFilter != null) {
			Date start = dateFilter.getStartDate();
			if (start == null){
				start = startDate;
			}
			Date end = dateFilter.getEndDate();
			if (end == null){
				end = endDate;
			}
			query.setParameter("date1", start); //$NON-NLS-1$
			query.setParameter("date2", end); //$NON-NLS-1$
		}
		
		return query;
	}

	
}