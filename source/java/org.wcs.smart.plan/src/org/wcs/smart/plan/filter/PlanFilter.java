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
package org.wcs.smart.plan.filter;

import java.util.Date;

import org.hibernate.query.Query;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.StringFilterComposite;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;

/**
 * Filter for filtering
 * planning objects.
 *  
 * @author Emily
 *
 */
public class PlanFilter {
	
	public static DateFilter DEFAULT_DATE_FILTER = DateFilter.RANGE_30_DAYS; 

	private static StringFilterComposite.TextField PLAN_ID_FILTER = new StringFilterComposite.TextField(Messages.PlanFilter_PlanId, "id"); //$NON-NLS-1$
	private static StringFilterComposite.TextField PLAN_NAME_FILTER = new StringFilterComposite.TextField(Messages.PlanFilter_PlanName, "value"); //$NON-NLS-1$
	public static StringFilterComposite.TextField[] SEARCH_FIELDS = {PLAN_ID_FILTER, PLAN_NAME_FILTER};
	
	private Plan.PlanType[] types = null;
	private DateFilter dateFilter = DEFAULT_DATE_FILTER;
	
	private StringFilterComposite.TextField searchField = null;
	private String planIdFilter = null;
	private StringComparison stringComparator = null;
	
	private Date startDate;
	private Date endDate;
	
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
	public StringComparison getPlanIdComparator(){
		return this.stringComparator;
	}
	/**
	 * 
	 * @return current patrol id string filter
	 */
	public String getPlanIdFilter(){
		return this.planIdFilter;
	}
	
	/**
	 * 
	 * @return the id or name field to search
	 */
	public StringFilterComposite.TextField getSearchField(){
		return this.searchField;
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
	public Plan.PlanType[] getPlanTypeFilters(){
		return this.types;
	}
	
	/**
	 * Resets all values to the default
	 */
	public void setDefaults(){
		this.dateFilter = DEFAULT_DATE_FILTER;
		this.planIdFilter = null;
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
	public void setPlanTypes(Plan.PlanType[] types){
		this.types = types;
	}
	
	/**
	 * Sets the patrol id filter.  If either are null then
	 * all patrol ids will be included.
	 * 
	 * @param stringComparitor the types of string comparison or null
	 * @param text the text to compare or null
	 */
	public void setPatrolIdFilter(StringComparison stringComparitor, String text, StringFilterComposite.TextField field){
		this.stringComparator = stringComparitor;
		this.planIdFilter = text;
		this.searchField = field;
	}
	
	/**
	 * Builds a query that returns the following patrol fields:
	 * patrol uuid, patrol id, patrol type, start date, end date
	 * 
	 * @param s
	 * @return
	 */
	public Query<?> buildQuery(Session s){ 
		StringBuilder str = new StringBuilder();

		//NOTE: if select order is changed also change sorting in PlanHibernateManager.getRootPlans
		//as in assumes that id goes 2nd and name 3rd
		str.append("SELECT p.uuid, p.id, p.name, p.type, p.parent.uuid "); //$NON-NLS-1$
		str.append("FROM Plan p "); //$NON-NLS-1$
		if (stringComparator != null && planIdFilter != null && searchField == PLAN_NAME_FILTER) {
			str.append(", Label lbl "); //$NON-NLS-1$
		}
		str.append("WHERE p.conservationArea = :ca " ); //$NON-NLS-1$
	
		boolean and = true;
		boolean or = false;
		if (types != null && types.length > 0){
			if (and ){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			or = true;
			str.append(" p.type IN (:pt) "); //$NON-NLS-1$
		}
		if (stringComparator != null && planIdFilter != null && searchField != null){
			if (and){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			if (or){
				str.append(" AND "); //$NON-NLS-1$
			}
			or = true;
			if (searchField == PLAN_NAME_FILTER) {
			    str.append(" (lbl.id.element.uuid = p.uuid AND lower(lbl.value) like :pid AND lbl.id.language = :language) "); //$NON-NLS-1$
			} else {
				str.append(" lower( p." + searchField.getDbFieldName() + ") like lower(:pid) "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
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
			str.append(" ( p.endDate >= :date1 and p.startDate <= :date2 ) "); //$NON-NLS-1$
		}
		if (!and){
			str.append(")"); //$NON-NLS-1$
		}
		
		Query<?> query = s.createQuery(str.toString()).setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		if (types != null && types.length > 0){
			query.setParameterList("pt", this.types); //$NON-NLS-1$
		}
		if (stringComparator != null && planIdFilter != null){
			if (stringComparator == StringComparison.CONTAINS){
				query.setParameter("pid", "%" + this.planIdFilter + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query.setParameter("pid", this.planIdFilter); //$NON-NLS-1$
			}
			if (searchField == PLAN_NAME_FILTER) {
				query.setParameter("language", SmartDB.getCurrentLanguage()); //$NON-NLS-1$
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
