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

package org.wcs.smart.er.ui.mission.export;

import java.util.Date;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.hibernate.SmartDB;


/*
 * Date filter for export Missions and Surveys
 * 
 */
public class MissionViewFilter {

	private DateFilter dateFilter = DateFilter.LAST_30_DAYS;
	private Date startDate;
	private Date endDate;
	
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
	 * Resets all values to the default
	 */
	public void setDefaults(){
		this.dateFilter = DateFilter.LAST_30_DAYS;
	}
	public void setDateFilter(DateFilter dFilter, Date start, Date end){
		this.dateFilter = dFilter;
		this.startDate = start;
		this.endDate = end;
	}
	
	public DateFilter getDateFilter(){
		return dateFilter;
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
		
		str.append("SELECT m.uuid, m.id, m.startDate, m.endDate, s.id, s.uuid "); //$NON-NLS-1$
		str.append("FROM Mission m JOIN m.survey s JOIN s.surveyDesign sd "); //$NON-NLS-1$
		str.append("WHERE sd.conservationArea = :ca " ); //$NON-NLS-1$
	
		if (dateFilter != null){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" ( m.endDate >= :date1 and m.startDate <= :date2 ) "); //$NON-NLS-1$
		}
		str.append("ORDER BY s.id, m.id desc"); //$NON-NLS-1$
	
		Query query = s.createQuery(str.toString()).setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
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
