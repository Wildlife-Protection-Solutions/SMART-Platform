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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.query.Query;
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
	
	private String surveyNameFilter = null;
	private StringComparison stringComparator = null;
	
	private String[] surveyDesignKeys = null;
	
	
	private DateFilter missionDateFilter = DateFilter.LAST_30_DAYS;
	private LocalDate missionStartDate;
	private LocalDate missionEndDate;
	
	/**
	 * Creates a new filter with the default values.
	 */
	public SurveyFilter(){
		setDefaults();
	}
	
	public void setMissionDateFilter(DateFilter dFilter, LocalDate start, LocalDate end){
		this.missionDateFilter = dFilter;
		this.missionStartDate = start;
		this.missionEndDate = end;
	}
	
	public DateFilter getMissionDateFilter(){
		return missionDateFilter;
	}
	
	/**
	 * 
	 * @return start date for custom date filter
	 */
	public LocalDate getMissionStartDate(){
		return this.missionStartDate;
	}
	
	/**
	 * 
	 * @return end date for custom date filter
	 */
	public LocalDate getMissionEndDate(){
		return this.missionEndDate;
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
	 * @return survey state filter or null if all or custom list selected
	 */
	public SurveyDesign.State getSurveyStateFilter(){
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
		this.state = SurveyDesign.State.ACTIVE;
		this.surveyNameFilter = null;
		this.stringComparator = null;
		this.setMissionDateFilter(DateFilter.LAST_60_DAYS, null, null);
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
	public List<SurveyMissionProxy> executeQuery(Session s){ 
		
		
		StringBuilder str = new StringBuilder();

		str.append("SELECT m.uuid, m.id, m.startDate, m.endDate, s.id, s.uuid, sd.name, sd.uuid "); //$NON-NLS-1$
		str.append("FROM Mission m JOIN m.survey s JOIN s.surveyDesign sd "); //$NON-NLS-1$
		str.append("WHERE sd.conservationArea = :ca " ); //$NON-NLS-1$
	
		if (missionDateFilter != null && missionDateFilter != DateFilter.ALL){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" ( m.endDate >= :date1 and m.startDate <= :date2 ) "); //$NON-NLS-1$
		}
		
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
			if ( surveyDesignKeys.length == 0){
				str.append(" sd.keyId = ''  "); //$NON-NLS-1$
			}else{
				str.append( " sd.keyId in (:keys) "); //$NON-NLS-1$
			}
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
			str.append(" lower(s.id) like lower(:name) "); //$NON-NLS-1$
			
		}
		if (!and){
			str.append(")"); //$NON-NLS-1$
		}
		
		str.append("ORDER BY  m.startDate desc, s.id asc "); //$NON-NLS-1$
		
		Query<?> query = s.createQuery(str.toString())
				.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
			
		if (state != null ){
			query.setParameter("states", this.state); //$NON-NLS-1$
		}else if (state == null && surveyDesignKeys != null && surveyDesignKeys.length > 0){
			query.setParameterList("keys", surveyDesignKeys); //$NON-NLS-1$
		}
		
		if (stringComparator != null && surveyNameFilter != null){
			if (stringComparator == StringComparison.CONTAINS){
				query.setParameter("name", "%" + this.surveyNameFilter + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query.setParameter("name", this.surveyNameFilter); //$NON-NLS-1$
			}
		}		
		if (missionDateFilter != null && missionDateFilter != DateFilter.ALL) {
			LocalDate start = missionDateFilter.getStartDate();
			if (start == null){
				start = missionStartDate;
			}
			LocalDate end = missionDateFilter.getEndDate();
			if (end == null){
				end = missionEndDate;
			}
			query.setParameter("date1", start); //$NON-NLS-1$
			query.setParameter("date2", end); //$NON-NLS-1$
		}
		
		HashMap<UUID, SurveyMissionProxy> results = new HashMap<>();
		List<SurveyMissionProxy> toReturn = new ArrayList<>();
		
		for (Object result : query.list()) {
			Object[] data = (Object[])result;
			UUID missionUuid = (UUID) data[0];
			String missionId = (String) data[1];
			LocalDate missionStart = (LocalDate) data[2];
			LocalDate missionEnd = (LocalDate) data[3];
			String surveyId = (String) data[4];
			UUID surveyUuid = (UUID) data[5];
			String designId = (String) data[6];
			UUID designUuid = (UUID) data[7];
			
			SurveyMissionProxy pp = results.get(surveyUuid);
			if (pp == null) {
				pp = new SurveyMissionProxy(surveyId, surveyUuid, designId, designUuid);
				results.put(surveyUuid, pp);
				toReturn.add(pp);
			}
			
			pp.addMission(missionId, missionUuid, missionStart, missionEnd);
			
		}
		return toReturn;
		
//StringBuilder str = new StringBuilder();
//		
//		str.append("SELECT m.uuid, m.id, m.startDate, m.endDate, s.id, s.uuid, sd.name "); //$NON-NLS-1$
//		str.append("FROM Mission m JOIN m.survey s JOIN s.surveyDesign sd "); //$NON-NLS-1$
//		str.append("WHERE sd.conservationArea = :ca " ); //$NON-NLS-1$
//	
//		if (dateFilter != null){
//			str.append(" AND "); //$NON-NLS-1$
//			str.append(" ( m.endDate >= :date1 and m.startDate <= :date2 ) "); //$NON-NLS-1$
//		}
//		str.append("ORDER BY m.startDate desc, m.id, s.id"); //$NON-NLS-1$
//	
//		Query<?> query = s.createQuery(str.toString()).setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
//		if (dateFilter != null) {
//			LocalDate start = dateFilter.getStartDate();
//			if (start == null){
//				start = startDate;
//			}
//			LocalDate end = dateFilter.getEndDate();
//			if (end == null){
//				end = endDate;
//			}
//			query.setParameter("date1", start); //$NON-NLS-1$
//			query.setParameter("date2", end); //$NON-NLS-1$
//		}
//		return query;
	}

	
}