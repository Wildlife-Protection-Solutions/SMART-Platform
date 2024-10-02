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
package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IIncidentProvider;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.model.IncidentType;

import jakarta.persistence.Tuple;

/**
 * Filter for the incident view.  Filters
 * have a date filter and 
 * a incident id filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class IncidentFilter {

	private DateFilter dateFilter = DateFilter.LAST_30_DAYS;
	private String incidentIdFilter = null;
	private StringComparison stringComparator = null;
	
	private Set<IIncidentProvider> sourceids = null;
	private List<IncidentType> types = null;
	
	private LocalDate startDate;
	private LocalDate endDate;
	
	public IncidentFilter() {
		sourceids = new HashSet<>();
		
		for (IIncidentProvider p : IncidentManager.getInstance().getIncidentProviders()) {
			sourceids.add(p);
		}
		try(Session session = HibernateManager.openSession()){
			types = IncidentManager.getInstance().getIncidentTypes(session, false);
		}
	}
	
	/**
	 * 
	 * @return the set of source ids to filter on
	 */
	public Set<IIncidentProvider> getSourceIds(){
		return this.sourceids;
	}
	
	/**
	 * 
	 * @return the set of source ids to filter on
	 */
	public List<IncidentType> getTypes(){
		return this.types;
	}
	
	/**
	 * 
	 * @return the current date filter
	 */
	public DateFilter getDateFilter(){
		return this.dateFilter;
	}
	/**
	 * @return the current incident id string comparator
	 */
	public StringComparison getIncidentIdComparator(){
		return this.stringComparator;
	}
	/**
	 * 
	 * @return current incident id string filter
	 */
	public String getIncidentIdFilter(){
		return this.incidentIdFilter;
	}
	
	/**
	 * 
	 * @return start date for custom date filter
	 */
	public LocalDate getStartDate(){
		return this.startDate;
	}
	
	/**
	 * 
	 * @return end date for custom date filter
	 */
	public LocalDate getEndDate(){
		return this.endDate;
	}
	
	
	/**
	 * Resets all values to the default
	 */
	public void setDefaults(){
		this.dateFilter = DateFilter.LAST_30_DAYS;
		this.incidentIdFilter = null;
		this.stringComparator = null;
	}
	
	/**
	 * Sets the date filter.  Set to null
	 * to include all dates;
	 * 
	 * @param dFilter date filter
	 * @param start the start date for custom filter; null if not custom date filter
	 * @param end the end date for custom filter; null if not cusom date filter
	 */
	public void setDateFilter(DateFilter dFilter, LocalDate start, LocalDate end){
		this.dateFilter = dFilter;
		this.startDate = start;
		this.endDate = end;
	}
	
	
	/**
	 * Sets the incident id filter.  If either are null then
	 * all incident ids will be included.
	 * 
	 * @param stringComparitor the types of string comparison or null
	 * @param text the text to compare or null
	 */
	public void setIncidentIdFilter(StringComparison stringComparitor, String text){
		this.stringComparator = stringComparitor;
		this.incidentIdFilter = text;
	}
	
	/**
	 * Builds a query that returns the following incident fields:
	 * incident uuid, incident id, incident datetime, source, typeuuid
	 * 
	 * @param s
	 * @return
	 */
	public Query<Tuple> buildQuery(Session s){ 
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT i.uuid, i.id, i.dateTime, i.sourceId, i.incidentTypeUuid "); //$NON-NLS-1$
		str.append("FROM Waypoint i "); //$NON-NLS-1$
		str.append("WHERE i.conservationArea = :ca " ); //$NON-NLS-1$
		str.append("AND i.sourceId IN ( :source ) " ); //$NON-NLS-1$
		str.append("AND (i.incidentTypeUuid is null or i.incidentTypeUuid IN ( :types )) " ); //$NON-NLS-1$
	
		boolean and = true;
		boolean or = false;
		
		if (stringComparator != null && incidentIdFilter != null){
			if (and){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			if (or){
				str.append(" AND "); //$NON-NLS-1$
			}
			or = true;
			if (stringComparator == StringComparison.EQUALS){
				str.append(" i.id = :pid "); //$NON-NLS-1$
			}else if (stringComparator == StringComparison.CONTAINS) {
				str.append(" i.id like :pid "); //$NON-NLS-1$
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
			str.append(" ( i.dateTime >= :date1 and i.dateTime <= :date2 ) "); //$NON-NLS-1$
		}
		if (!and){
			str.append(")"); //$NON-NLS-1$
		}
		
		str.append("ORDER BY i.dateTime desc, i.id"); //$NON-NLS-1$
		
		Set<String> sourcestrings = sourceids.stream().map(e->e.getWaypointSourceKey()).collect(Collectors.toSet());
		Set<UUID> typeuuids = types.stream().map(e->e.getUuid()).collect(Collectors.toSet());
		
		Query<Tuple> query = s.createQuery(str.toString(), Tuple.class).setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		query.setParameterList("source", sourcestrings); //$NON-NLS-1$
		query.setParameterList("types", typeuuids); //$NON-NLS-1$
		
		if (stringComparator != null && incidentIdFilter != null){
			if (stringComparator == StringComparison.EQUALS){
				query.setParameter("pid", this.incidentIdFilter); //$NON-NLS-1$				
			}else if (stringComparator == StringComparison.CONTAINS) {
				query.setParameter("pid", "%" + this.incidentIdFilter + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else {
				throw new IllegalStateException(MessageFormat.format("String operator {0} not supported.", stringComparator.getGuiName())); //$NON-NLS-1$
			}
		}
		if (dateFilter != null) {
			
			LocalDateTime start = dateFilter.getStartDate() == null ? startDate.atStartOfDay() : dateFilter.getStartDate().atStartOfDay();
			LocalDateTime end = dateFilter.getEndDate() == null ? endDate.atTime(LocalTime.MAX) : dateFilter.getEndDate().atTime(LocalTime.MAX);
			
			query.setParameter("date1", start); //$NON-NLS-1$
			query.setParameter("date2", end); //$NON-NLS-1$
		}
		return query;
	}
	
}
