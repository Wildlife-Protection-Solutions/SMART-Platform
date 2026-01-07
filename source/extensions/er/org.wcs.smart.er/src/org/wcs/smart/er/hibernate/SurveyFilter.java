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
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
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
	private StringComparison surveyStringComparator = null;
	
	
	private String missionNameFilter = null;
	private StringComparison missionStringComparator = null;
	
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
		return this.surveyStringComparator;
	}
	/**
	 * 
	 * @return current survey name filter string
	 */
	public String getSurveyNameFilter(){
		return this.surveyNameFilter;
	}
	
	/**
	 * @return the current survey name string comparator
	 */
	public StringComparison getMissionNameComparator(){
		return this.missionStringComparator;
	}
	/**
	 * 
	 * @return current survey name filter string
	 */
	public String getMissionNameFilter(){
		return this.missionNameFilter;
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
		this.surveyStringComparator = null;
		this.missionNameFilter = null;
		this.missionStringComparator = null;
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
		this.surveyStringComparator = stringComparitor;
		this.surveyNameFilter = text;
	}
	
	/**
	 * Sets the mission name filter.  Set to null
	 * to include all missions;
	 * 
	 * @param stringComparitor the types of string comparison or null
	 * @param text the text to compare or null
	 */
	public synchronized void setMissionNameFilter(StringComparison stringComparitor, String text){
		this.missionStringComparator = stringComparitor;
		this.missionNameFilter = text;
	}
	
	/**
	 * Set to null to ignore filter
	 * @param keys
	 */
	public synchronized void setSurveyDesignKeyFilters(String[] keys){
		this.surveyDesignKeys = keys;
	}
	
	private boolean hasDateFilter() {
		return this.missionDateFilter != null && this.missionDateFilter != DateFilter.ALL;
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

		str.append("SELECT m "); //$NON-NLS-1$
		str.append("FROM Mission m JOIN m.survey s JOIN s.surveyDesign sd "); //$NON-NLS-1$
		str.append("WHERE sd.conservationArea = :ca " ); //$NON-NLS-1$
	
		if (hasDateFilter()){
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
		
		if (surveyStringComparator != null && surveyNameFilter != null){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" lower(s.id) like lower(:name) "); //$NON-NLS-1$			
		}
		
		if (missionStringComparator != null && missionNameFilter != null){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" lower(m.id) like lower(:mname) "); //$NON-NLS-1$	
		}
		str.append("ORDER BY  m.startDate desc, s.id asc "); //$NON-NLS-1$
		
		Query<Mission> query = s.createQuery(str.toString(), Mission.class)
				.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
			
		if (state != null ){
			query.setParameter("states", this.state); //$NON-NLS-1$
		}else if (state == null && surveyDesignKeys != null && surveyDesignKeys.length > 0){
			query.setParameterList("keys", surveyDesignKeys); //$NON-NLS-1$
		}
		
		if (surveyStringComparator != null && surveyNameFilter != null){
			if (surveyStringComparator == StringComparison.CONTAINS){
				query.setParameter("name", "%" + this.surveyNameFilter + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query.setParameter("name", this.surveyNameFilter); //$NON-NLS-1$
			}
		}		
		if (missionStringComparator != null && missionNameFilter != null){
			if (missionStringComparator == StringComparison.CONTAINS){
				query.setParameter("mname", "%" + this.missionNameFilter + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query.setParameter("mname", this.surveyNameFilter); //$NON-NLS-1$
			}
		}	
		if (hasDateFilter()) {
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
		
		for (Mission result : query.list()) {
			SurveyMissionProxy pp = results.get(result.getSurvey().getUuid());
			if (pp == null) {
				pp = new SurveyMissionProxy(result.getSurvey().getId(), result.getSurvey().getUuid(), result.getSurvey().getSurveyDesign().getName(), result.getSurvey().getSurveyDesign().getUuid());
				results.put(result.getSurvey().getUuid(), pp);
				toReturn.add(pp);
			}
			
			pp.addMission(result.getId(),result.getUuid(),result.getStartDate(),result.getEndDate());
			
		}
		
		//include any survey without missions
		str = new StringBuilder();

		str.append("SELECT s "); //$NON-NLS-1$
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
		
		if (surveyStringComparator != null && surveyNameFilter != null){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" lower(s.id) like lower(:name) "); //$NON-NLS-1$
		}
		str.append("ORDER BY  s.id asc "); //$NON-NLS-1$
		
		Query<Survey> query2 = s.createQuery(str.toString(), Survey.class)
				.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		if (state != null ){
			query2.setParameter("states", this.state); //$NON-NLS-1$
		}else if (state == null && surveyDesignKeys != null && surveyDesignKeys.length > 0){
			query2.setParameterList("keys", surveyDesignKeys); //$NON-NLS-1$
		}
		if (surveyStringComparator != null && surveyNameFilter != null){
			if (surveyStringComparator == StringComparison.CONTAINS){
				query2.setParameter("name", "%" + this.surveyNameFilter + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query2.setParameter("name", this.surveyNameFilter); //$NON-NLS-1$
			}
		}		
		
		for (Survey result : query2.list()) {
			SurveyMissionProxy pp = results.get(result.getUuid());
			if (pp == null) {
				pp = new SurveyMissionProxy(result.getId(), result.getUuid(), result.getSurveyDesign().getName(), result.getSurveyDesign().getUuid());
				results.put(result.getUuid(), pp);
				toReturn.add(pp);
			}		
		}
		
		return toReturn;
	
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
		
		if (surveyStringComparator != null){
			sb.append(surveyStringComparator.name());
		}
		sb.append(FIELD_SEP);
		
		if (surveyNameFilter != null){
			sb.append(surveyNameFilter);
		}
		sb.append(FIELD_SEP);
		
		if (missionStringComparator != null){
			sb.append(missionStringComparator.name());
		}
		sb.append(FIELD_SEP);
		
		if (missionNameFilter != null){
			sb.append(missionNameFilter);
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
			filter.surveyStringComparator = StringComparison.valueOf(parts[4]);
		}else{
			filter.surveyStringComparator = null;
		}
		if (!parts[5].isEmpty()){
			filter.surveyNameFilter = parts[5];
		}else{
			filter.surveyNameFilter = null;
		}
		
		if (!parts[6].isEmpty()){
			filter.missionStringComparator = StringComparison.valueOf(parts[6]);
		}else{
			filter.missionStringComparator = null;
		}
		if (!parts[7].isEmpty()){
			filter.missionNameFilter = parts[7];
		}else{
			filter.missionNameFilter = null;
		}
		
		if (parts.length > 8 && !parts[8].isEmpty()){
			String types[] = parts[8].split(FIELD_SEP2);
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
		clone.surveyStringComparator = this.surveyStringComparator;
		
		clone.missionNameFilter = this.missionNameFilter;
		clone.missionStringComparator = this.missionStringComparator;
		
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