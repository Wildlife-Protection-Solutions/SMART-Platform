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
package org.wcs.smart.er.query.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.query.common.engine.test.ObservationQueryResultItem;

/**
 * A class to hold the results of a survey waypoint 
 * query.  Each class contains the results for
 * a single observation.  The observation contains
 * a single category and all attributes. Also
 * included are all mission attributes in the query.
 * 
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyObservationResultItem extends ObservationQueryResultItem implements ISamplingUnitResultItem, ISurveyQueryResultItem{
	
	private String surveyDesign;
	private String surveyId;
	
	private String missionId;
	private LocalDateTime missionStart;
	private LocalDateTime missionEnd;
	private UUID missionUuid;
	private String missionLeader;
	
	private UUID samplingUnitUuid;
	private String samplingUnitId;
	
	private HashMap<String, Object> missionProperties = new HashMap<String, Object>();
	private HashMap<String, Object> suAttributes = new HashMap<String, Object>();

	/**
	 * Sets the mission leader attribute
	 * 
	 * @param leader
	 */
	public void setMissionLeader(String leader){
		this.missionLeader = leader;
	}
	
	/**
	 * Get the mission leader
	 * 
	 * @return
	 */
	public String getMissionLeader(){
		return this.missionLeader;
	}
	
		
	/**
	 * Finds the mission property with the associated
	 * attribute key.
	 * 
	 * @param attributeKey the attribute key
	 * @return the value associated with the attribute given key
	 */
	public Object getMissionPropertyValue(String attributeKey){
		return missionProperties.get(attributeKey);
	}
	
	/**
	 * Adds a mission property to the observation results 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addMissionPropertyValue(String key, Object value){
		missionProperties.put(key, value);
	}
	
	/**
	 * Finds the properties value of the associated sampling
	 * unit attribute key.
	 * 
	 * @param attributeKey sampling unit attribute key
	 * @return the value associated with the given key
	 */
	public Object getSamplingUnitAttributeValue(String attributeKey){
		return suAttributes.get(attributeKey);
	}
	
	/**
	 * Adds an sampling unit property value to the result 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addSamplingUnitAttributeValue(String key, Object value){
		suAttributes.put(key, value);
	}
	
	/**
	 * sets the mission uuid
	 * @param uuid
	 */
	public void setMissionUuid(UUID uuid){
		this.missionUuid = uuid;
	}
	/**
	 * 
	 * @return the mission uuid
	 */
	public UUID getMissionUuid(){
		return this.missionUuid;
	}
	
	/**
	 * sets the sampling unit uuid
	 * @param uuid
	 */
	public void setSamplingUnitUuid(UUID uuid){
		this.samplingUnitUuid = uuid;
	}
	
	/**
	 * 
	 * @return the sampling unit uuid
	 */
	public UUID getSamplingUnitUuid(){
		return this.samplingUnitUuid;
	}
	
	

	/**
	 * 
	 * @return the survey design name
	 */
	public String getSurveyDesign() {
		return surveyDesign;
	}

	/**
	 * 
	 * @param surveyDesign the survey design name
	 */
	public void setSurveyDesign(String surveyDesign) {
		this.surveyDesign = surveyDesign;
	}

	/**
	 * survey id
	 * @return
	 */
	public String getSurveyId() {
		return surveyId;
	}

	public void setSurveyId(String surveyId) {
		this.surveyId = surveyId;
	}

	/**
	 * mission id
	 * @return
	 */
	public String getMissionId() {
		return missionId;
	}

	public void setMissionId(String missionId) {
		this.missionId = missionId;
	}

	/**
	 * mission start
	 * @return
	 */
	public LocalDateTime getMissionStart() {
		return missionStart;
	}

	public void setMissionStart(LocalDateTime missionStart) {
		this.missionStart = missionStart;
	}

	/**
	 * mission end
	 * @return
	 */
	public LocalDateTime getMissionEnd() {
		return missionEnd;
	}

	public void setMissionEnd(LocalDateTime missionEnd) {
		this.missionEnd = missionEnd;
	}

	/**
	 * sampling unit id
	 * @return
	 */
	public String getSamplingUnitId() {
		return samplingUnitId;
	}

	public void setSamplingUnitId(String samplingUnitId) {
		this.samplingUnitId = samplingUnitId;
	}
	
}
