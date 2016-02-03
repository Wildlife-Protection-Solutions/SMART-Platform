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

import java.util.Date;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
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

	private static final String FIELD_SEP = ";"; //$NON-NLS-1$
	private static final String FIELD_SEP2 = ","; //$NON-NLS-1$
	private static final String PREFERENCE_KEY = ".patrolfilter.default"; //$NON-NLS-1$
	
	public enum SortByDir{
		ASC(Messages.PatrolViewFilter_AscOp, "asc"), //$NON-NLS-1$
		DESC(Messages.PatrolViewFilter_DescOp, "desc"); //$NON-NLS-1$
		public String guiName;
		public String sql;
		
		SortByDir(String name, String sql){ 
			this.guiName = name;
			this.sql = sql;
		}
	}
	public enum SortBy{
		ID(Messages.PatrolViewFilter_PatrolIdSort, "lower(p.id)"), //$NON-NLS-1$
		START_DATE(Messages.PatrolViewFilter_StartDateSort, "p.startDate"), //$NON-NLS-1$
		END_DATE(Messages.PatrolViewFilter_EndDateSort, "p.endDate"); //$NON-NLS-1$
		
		public String guiName;
		public String field;
		
		SortBy(String name, String field){
			this.guiName = name;
			this.field = field;
		}
	}

	/**
	 * Generates a new patrol filter, reading first from the preference store 
	 * 
	 * @return
	 */
	public static PatrolViewFilter newInstance(){
		PatrolViewFilter f = null;
	
		try{
			f = readFromPreference();
		}catch (Exception ex){
			//eat me
		}
		if (f == null){
			f = new PatrolViewFilter();
			f.setDefaults();
		}
		return f;
	}
	private PatrolViewFilter(){
	}
	
	private PatrolType.Type[] types = null;
	private DateFilter dateFilter = DateFilter.LAST_30_DAYS;
	private String patrolIdFilter = null;
	private StringComparison stringComparator = null;
	
	private SortByDir sortByDir = SortByDir.DESC;
	private SortBy sortBy = SortBy.ID;	
	
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
	 * @return the current sort field
	 */
	public SortBy getSortBy(){
		return this.sortBy;
	}
	
	/**
	 * 
	 * @return the current sort direction
	 */
	public SortByDir getSortByDir(){
		return this.sortByDir;
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
		this.sortBy = SortBy.ID;
		this.sortByDir = SortByDir.DESC;
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
	 * Sets the sort by fields
	 * @param sortBy
	 * @param dir
	 */
	public void setSortBy(SortBy sortBy, SortByDir dir){
		this.sortBy = sortBy;
		this.sortByDir = dir;
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
	
	/**
	 * Builds a query that returns the following patrol fields:
	 * patrol uuid, patrol id, patrol type, start date, end date
	 * 
	 * @param s
	 * @return
	 */
	public Query buildQuery(Session s){ 
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT p.uuid, p.id, p.patrolType, p.startDate, p.endDate "); //$NON-NLS-1$
		str.append("FROM Patrol p "); //$NON-NLS-1$
		str.append("WHERE p.conservationArea = :ca " ); //$NON-NLS-1$
	
		boolean and = true;
		boolean or = false;
		if (types != null && types.length > 0){
			if (and ){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			or = true;
			str.append(" p.patrolType IN (:pt) "); //$NON-NLS-1$
		}
		if (stringComparator != null && patrolIdFilter != null){
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
		
		
		str.append("ORDER BY " + sortBy.field + " " + sortByDir.sql ); //$NON-NLS-1$ //$NON-NLS-2$
		
		Query query = s.createQuery(str.toString()).setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		if (types != null && types.length > 0){
			query.setParameterList("pt", this.types); //$NON-NLS-1$
		}
		if (stringComparator != null && patrolIdFilter != null){
			if (stringComparator == StringComparison.CONTAINS){
				query.setParameter("pid", "%" + this.patrolIdFilter.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query.setParameter("pid", this.patrolIdFilter.toLowerCase()); //$NON-NLS-1$
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
	
	/**
	 * Converts the filter to a string.
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if (dateFilter != null){
			sb.append(dateFilter.name());
		}
		sb.append(FIELD_SEP);
		
		if (startDate != null){
			sb.append(startDate.getTime());
		}	
		sb.append(FIELD_SEP);
		
		if (endDate != null){
			sb.append(endDate.getTime());
		}	
		sb.append(FIELD_SEP);
		
		if (patrolIdFilter != null){
			sb.append(patrolIdFilter);
		}
		sb.append(FIELD_SEP);
		
		if (stringComparator != null){
			sb.append(stringComparator.name());
		}
		sb.append(FIELD_SEP);
		
		if (types != null){
			for (PatrolType.Type type : types){
				sb.append(type.name() + FIELD_SEP2);
			}
		}
		sb.append(FIELD_SEP);
		
		sb.append(sortBy.name());
		sb.append(FIELD_SEP);
		sb.append(sortByDir.name());
		return sb.toString();
		
	}
	
	/**
	 * Generates a filter from the string representation generated by the toString()
	 * method
	 * @param sfilter
	 * @return
	 */
	private static PatrolViewFilter fromString(String sfilter){
		String[] parts = sfilter.split(FIELD_SEP);
		
		PatrolViewFilter filter = new PatrolViewFilter();
		if (!parts[0].isEmpty()){
			filter.dateFilter = DateFilter.valueOf(parts[0]);
		}else{
			filter.dateFilter = null;
		}
		if (!parts[1].isEmpty()){
			filter.startDate = new Date(Long.valueOf(parts[1]));
		}else{
			filter.startDate = null;
		}
		if (!parts[2].isEmpty()){
			filter.endDate = new Date(Long.valueOf(parts[2]));
		}else{
			filter.endDate = null;
		}
		if (!parts[3].isEmpty()){
			filter.patrolIdFilter = parts[3];
		}else{
			filter.patrolIdFilter = null;
		}
		if (!parts[4].isEmpty()){
			filter.stringComparator = StringComparison.valueOf(parts[4]);
		}else{
			filter.stringComparator = null;
		}
		if (!parts[5].isEmpty()){
			String types[] = parts[5].split(FIELD_SEP2);
			filter.types = new PatrolType.Type[types.length];
			for (int i = 0; i < types.length; i ++){
				filter.types[i] = PatrolType.Type.valueOf(types[i]);
			}
		}else{
			filter.types = null;
		}
		filter.sortBy = SortBy.valueOf(parts[6]);
		filter.sortByDir = SortByDir.valueOf(parts[7]);
		return filter;
	}
	
	/**
	 * Saves the patrol filter to the preference store for use next time the application is 
	 * started or a new patrol filter is created.  This uses the conservation area uuid so users
	 * can have different filters for different conservation areas.
	 */
	public void saveAsPreference(){
		String key = SmartDB.getCurrentConservationArea().getUuid().toString() + PREFERENCE_KEY;
		SmartPatrolPlugIn.getDefault().getPreferenceStore().setValue(key, toString());
	}
	
	private static PatrolViewFilter readFromPreference(){
		String key = SmartDB.getCurrentConservationArea().getUuid().toString() + PREFERENCE_KEY;
		String stringValue = SmartPatrolPlugIn.getDefault().getPreferenceStore().getString(key);
		if (stringValue == null || stringValue.isEmpty()) return null;
		return fromString(stringValue);
	}
	
	public PatrolViewFilter clone(){
		PatrolViewFilter clone = new PatrolViewFilter();
		clone.dateFilter = dateFilter;
		clone.endDate = endDate;
		clone.patrolIdFilter = patrolIdFilter;
		clone.sortBy = sortBy;
		clone.sortByDir = sortByDir;
		clone.startDate= startDate;
		clone.stringComparator = stringComparator;
		clone.types = types;
		return clone;
	}
}
