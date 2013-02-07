package org.wcs.smart.plan.filter;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.Plan.PlanType;

public class PlanFilter {
	
	private Plan.PlanType[] types = null;
	private DateFilter dateFilter = DateFilter.LAST_30_DAYS;
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
		this.dateFilter = DateFilter.LAST_30_DAYS;
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
	public void setPatrolIdFilter(StringComparison stringComparitor, String text){
		this.stringComparator = stringComparitor;
		this.planIdFilter = text;
	}
	
	/**
	 * Builds a query that returns the following patrol fields:
	 * patrol uuid, patrol id, patrol type, start date, end date
	 * 
	 * @param s
	 * @return
	 */
	public Query buildQuery(Session s){ 
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT p.uuid, p.id, p.name, p.type, p.parent.uuid "); //$NON-NLS-1$
		str.append("FROM Plan p "); //$NON-NLS-1$
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
		if (stringComparator != null && planIdFilter != null){
			if (and){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			if (or){
				str.append(" AND "); //$NON-NLS-1$
			}
			or = true;
			str.append(" lower(p.id) like :pid "); //$NON-NLS-1$
			
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
		
		str.append("ORDER BY p.name, p.id desc"); //$NON-NLS-1$
		
		Query query = s.createQuery(str.toString()).setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		if (types != null && types.length > 0){
			query.setParameterList("pt", this.types); //$NON-NLS-1$
		}
		if (stringComparator != null && planIdFilter != null){
			if (stringComparator == StringComparison.CONTAINS){
				query.setParameter("pid", "%" + this.planIdFilter.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query.setParameter("pid", this.planIdFilter.toLowerCase()); //$NON-NLS-1$
			}
		}
		if (dateFilter != null) {

			if (dateFilter == DateFilter.LAST_30_DAYS) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, -30);
				query.setParameter("date1", cal.getTime()); //$NON-NLS-1$
				query.setParameter("date2", getCurrentDate()); //$NON-NLS-1$
			} else if (dateFilter == DateFilter.LAST_60_DAYS) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, -60);
				query.setParameter("date1", cal.getTime()); //$NON-NLS-1$
				query.setParameter("date2", getCurrentDate()); //$NON-NLS-1$
			} else if (dateFilter == DateFilter.YEAR_TO_DATE) {
				Calendar cal = Calendar.getInstance();
				cal.set(cal.get(Calendar.YEAR), 0, 01, 0, 0, 0);
				query.setParameter("date1", cal.getTime()); //$NON-NLS-1$
				query.setParameter("date2", getCurrentDate()); //$NON-NLS-1$
			} else if (dateFilter == DateFilter.MONTH_TO_DATE) {
				Calendar cal = Calendar.getInstance();
				cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 01, 0, 0, 0);
				query.setParameter("date1", cal.getTime()); //$NON-NLS-1$
				query.setParameter("date2", getCurrentDate()); //$NON-NLS-1$
			} else if (dateFilter == DateFilter.CUSTOM) {
				query.setParameter("date1", startDate); //$NON-NLS-1$
				query.setParameter("date2", endDate); //$NON-NLS-1$
			}

		}
		return query;
	}
	
	private Date getCurrentDate() {
		Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}
}
