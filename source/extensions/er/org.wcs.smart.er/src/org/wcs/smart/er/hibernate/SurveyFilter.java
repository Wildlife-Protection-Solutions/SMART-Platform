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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
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

	private static final String FIELD_SEP = ";"; //$NON-NLS-1$
	private static final String FIELD_SEP2 = ","; //$NON-NLS-1$
	private static final String PREFERENCE_KEY = ".surveyfilter.default"; //$NON-NLS-1$

	private SurveyDesign.State state = null;
	
	private String surveyNameFilter = null;
	private StringComparison stringComparator = null;
	
	private String[] surveyDesignKeys = null;
	
	private DateFilter missionDateFilter = DateFilter.LAST_30_DAYS;
	private LocalDate missionStartDate;
	private LocalDate missionEndDate;
	
	
	/**
	 * Generates a new patrol filter, reading first from the preference store 
	 * 
	 * @return
	 */
	public static SurveyFilter newInstance(){
		SurveyFilter f = null;
	
		try{
			f = readFromPreference();
		}catch (Exception ex){
			//eat me
		}
		if (f == null){
			f = new SurveyFilter();
			f.setDefaults();
		}
		return f;
	}
	
	/**
	 * Creates a new filter with the default values.
	 */
	private SurveyFilter(){
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
	
	
	public synchronized void setMissionDateFilter(DateFilter dFilter, LocalDate start, LocalDate end){
		this.missionDateFilter = dFilter;
		this.missionStartDate = start;
		this.missionEndDate = end;
	}
	
	/**
	 * Resets all values to the default
	 */
	public synchronized void setDefaults(){
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
	public synchronized void setSurveyState(SurveyDesign.State state){
		this.state = state;
	}
	
	/**
	 * Sets the survey name filter.  Set to null
	 * to include all surveys;
	 * 
	 * @param stringComparitor the types of string comparison or null
	 * @param text the text to compare or null
	 */
	public synchronized void setSurveyNameFilter(StringComparison stringComparitor, String text){
		this.stringComparator = stringComparitor;
		this.surveyNameFilter = text;
	}
	
	/**
	 * Set to null to ignore filter
	 * @param keys
	 */
	public synchronized void setSurveyDesignKeyFilters(String[] keys){
		this.surveyDesignKeys = keys;
	}
	
	/**
	 * Builds a query that returns the following survey fields:
	 * {survey uuid, survey id, start date, survey design name}
	 * 
	 * @param s
	 * @return
	 */
	public synchronized List<SurveyMissionProxy> executeQuery(Session s){ 
		
		StringBuilder str = new StringBuilder();

		str.append("SELECT m.uuid, m.id, m.startDate, m.endDate, s.id, s.uuid, sd.name, sd.uuid "); //$NON-NLS-1$
		str.append("FROM Mission m JOIN m.survey s JOIN s.surveyDesign sd "); //$NON-NLS-1$
		str.append("WHERE sd.conservationArea = :ca " ); //$NON-NLS-1$
	
		if (missionDateFilter != null && missionDateFilter != DateFilter.ALL){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" ( m.endDate >= :date1 and m.startDate <= :date2 ) "); //$NON-NLS-1$
		}
		
		if (state != null ){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" sd.state = :states "); //$NON-NLS-1$
		}else if (state == null && surveyDesignKeys != null){
			str.append(" AND "); //$NON-NLS-1$
			if ( surveyDesignKeys.length == 0){
				str.append(" sd.keyId = ''  "); //$NON-NLS-1$
			}else{
				str.append( " sd.keyId in (:keys) "); //$NON-NLS-1$
			}
		}
		
		if (stringComparator != null && surveyNameFilter != null){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" lower(s.id) like lower(:name) "); //$NON-NLS-1$
			
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
		
		//include any survey without missions
		str = new StringBuilder();

		str.append("SELECT s.id, s.uuid, sd.name, sd.uuid "); //$NON-NLS-1$
		str.append("FROM Survey s JOIN s.surveyDesign sd "); //$NON-NLS-1$
		str.append("WHERE sd.conservationArea = :ca " ); //$NON-NLS-1$
		str.append("AND s.uuid NOT IN (SELECT survey.uuid FROM Mission) " ); //$NON-NLS-1$
	
		if (state != null ){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" sd.state = :states "); //$NON-NLS-1$
		}else if (state == null && surveyDesignKeys != null){
			str.append(" AND "); //$NON-NLS-1$
			if ( surveyDesignKeys.length == 0){
				str.append(" sd.keyId = ''  "); //$NON-NLS-1$
			}else{
				str.append( " sd.keyId in (:keys) "); //$NON-NLS-1$
			}
		}
		
		if (stringComparator != null && surveyNameFilter != null){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" lower(s.id) like lower(:name) "); //$NON-NLS-1$
		}
		str.append("ORDER BY  s.id asc "); //$NON-NLS-1$
		
		query = s.createQuery(str.toString()).setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
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
		
		for (Object result : query.list()) {
			Object[] data = (Object[])result;
			String surveyId = (String) data[0];
			UUID surveyUuid = (UUID) data[1];
			String designId = (String) data[2];
			UUID designUuid = (UUID) data[3];
			
			SurveyMissionProxy pp = results.get(surveyUuid);
			if (pp == null) {
				pp = new SurveyMissionProxy(surveyId, surveyUuid, designId, designUuid);
				results.put(surveyUuid, pp);
				toReturn.add(pp);
			}		
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
	
	/**
	 * Saves the patrol filter to the preference store for use next time the application is 
	 * started or a new patrol filter is created.  This uses the conservation area uuid so users
	 * can have different filters for different conservation areas.
	 */
	public void saveAsPreference(){
		String key = SmartDB.getCurrentConservationArea().getUuid().toString() + PREFERENCE_KEY;
		EcologicalRecordsPlugIn.getDefault().getPreferenceStore().setValue(key, toString());
	}
	
	private static SurveyFilter readFromPreference(){
		String key = SmartDB.getCurrentConservationArea().getUuid().toString() + PREFERENCE_KEY;
		String stringValue = EcologicalRecordsPlugIn.getDefault().getPreferenceStore().getString(key);
		if (stringValue == null || stringValue.isEmpty()) return null;
		return fromString(stringValue);
	}

	/**
	 * Converts the filter to a string.
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if (missionDateFilter != null){
			sb.append(missionDateFilter.name());
		}
		sb.append(FIELD_SEP);
		
		if (missionStartDate != null){
			sb.append(DateTimeFormatter.ISO_LOCAL_DATE.format(missionStartDate));
		}	
		sb.append(FIELD_SEP);
		
		if (missionEndDate != null){
			sb.append(DateTimeFormatter.ISO_LOCAL_DATE.format(missionEndDate));
		}	
		sb.append(FIELD_SEP);
		
		if (state != null){
			sb.append(state.name());
		}
		sb.append(FIELD_SEP);
		
		if (stringComparator != null){
			sb.append(stringComparator.name());
		}
		sb.append(FIELD_SEP);
		
		if (surveyNameFilter != null){
			sb.append(surveyNameFilter);
		}
		sb.append(FIELD_SEP);
		
		if (surveyDesignKeys != null){
			for (String key : surveyDesignKeys){
				sb.append(key + FIELD_SEP2);
			}
		}
		
		return sb.toString();
		
	}
	
	/**
	 * Generates a filter from the string representation generated by the toString()
	 * method
	 * @param sfilter
	 * @return
	 */
	private static SurveyFilter fromString(String sfilter){
		String[] parts = sfilter.split(FIELD_SEP);
		
		SurveyFilter filter = new SurveyFilter();
		if (!parts[0].isEmpty()){
			filter.missionDateFilter = DateFilter.valueOf(parts[0]);
		}else{
			filter.missionDateFilter = null;
		}
		if (!parts[1].isEmpty()){
			filter.missionStartDate = LocalDate.parse(parts[1], DateTimeFormatter.ISO_LOCAL_DATE);
		}else{
			filter.missionStartDate = null;
		}
		if (!parts[2].isEmpty()){
			filter.missionEndDate = LocalDate.parse(parts[2], DateTimeFormatter.ISO_LOCAL_DATE);
		}else{
			filter.missionEndDate = null;
		}
		if (!parts[3].isEmpty()){
			filter.state = SurveyDesign.State.valueOf(parts[3]);
		}else{
			filter.state = null;
		}
		if (!parts[4].isEmpty()){
			filter.stringComparator = StringComparison.valueOf(parts[4]);
		}else{
			filter.stringComparator = null;
		}
		if (!parts[5].isEmpty()){
			filter.surveyNameFilter = parts[5];
		}else{
			filter.surveyNameFilter = null;
		}
		if (parts.length > 6 && !parts[6].isEmpty()){
			String types[] = parts[6].split(FIELD_SEP2);
			filter.surveyDesignKeys = new String[types.length];
			for (int i = 0; i < types.length; i ++){
				filter.surveyDesignKeys[i] = types[i];
			}
		}else{
			filter.surveyDesignKeys = null;
		}
		return filter;
	}
	
	public SurveyFilter clone(){
		SurveyFilter clone = new SurveyFilter();
		
		clone.state = this.state;
		clone.surveyNameFilter = this.surveyNameFilter;
		clone.stringComparator = this.stringComparator;
		
		clone.missionDateFilter = this.missionDateFilter;
		if (this.missionStartDate != null)
			clone.missionStartDate = LocalDate.from(this.missionStartDate);
		if (this.missionEndDate != null)
			clone.missionEndDate = LocalDate.from(this.missionEndDate);
		
		if (this.surveyDesignKeys != null) {
			clone.surveyDesignKeys = Arrays.copyOf(this.surveyDesignKeys, this.surveyDesignKeys.length);
		}
	
		return clone;
	}
	
	
}