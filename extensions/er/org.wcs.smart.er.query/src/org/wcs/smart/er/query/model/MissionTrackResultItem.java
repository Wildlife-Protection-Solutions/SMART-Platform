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

import java.util.Date;
import java.util.HashMap;

import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.query.model.IResultItem;

/**
 * A class to hold the results of a mission track 
 * query.   
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MissionTrackResultItem implements IResultItem{

	private String caId;
	private String caName;
	
	private String surveyDesign;
	private Date surveyDesignStart;
	private Date surveyDesignEnd;
	
	private String surveyId;
	private Date surveyStart;
	private Date surveyEnd;
	
	private String missionId;
	private Date missionStart;
	private Date missionEnd;
	private byte[] missionUuid;
	
	private byte[] samplingUnitUuid;
	private String samplingUnitId;
	private Double smaplingUnitBuffer;
	
	private byte[] trackUuid;
	private Date trackDate;
	private MissionTrack.TrackType trackType;
	
	private HashMap<String, Object> missionProperties = new HashMap<String, Object>();

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
	 * Sets the track uuid
	 * @param trackUuid
	 */
	public void setTrackUuid(byte[] trackUuid){
		this.trackUuid = trackUuid;
	}
	
	/**
	 * @return the track uuid
	 */
	public byte[] getTrackUuid(){
		return this.trackUuid;
	}
	
	/**
	 * Sets the track date attribute
	 * 
	 * @param leader
	 */
	public void setTrackDate(Date trackDate){
		this.trackDate = trackDate;
	}
	
	/**
	 * Get the track date
	 * 
	 * @return
	 */
	public Date getTrackDate(){
		return this.trackDate;
	}
	
	
	
	/**
	 * sets the mission uuid
	 * @param uuid
	 */
	public void setMissionUuid(byte[] uuid){
		this.missionUuid = uuid;
	}
	/**
	 * 
	 * @return the mission uuid
	 */
	public byte[] getMissionUuid(){
		return this.missionUuid;
	}
	
	/**
	 * sets the sampling unit uuid
	 * @param uuid
	 */
	public void setSamplingUnitUuid(byte[] uuid){
		this.samplingUnitUuid = uuid;
	}
	
	/**
	 * 
	 * @return the sampling unit uuid
	 */
	public byte[] getSamplingUnitUuid(){
		return this.samplingUnitUuid;
	}
	
	
	/**
	 * Sets the ca id
	 * @param caId
	 */
	public void setConservationAreaId(String caId){
		this.caId = caId;
	}
	
	/**
	 * Sets the ca name
	 * @param caName
	 */
	public void setConservationAreaName(String caName){
		this.caName = caName;
	}
	
	/**
	 * 
	 * @return this conservation area id
	 */
	public String getConservationAreaId(){
		return this.caId;
	}
	/**
	 * the conservation area name
	 * @return
	 */
	public String getConservationAreaName(){
		return this.caName;
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
	 * survey design start
	 * @return
	 */
	public Date getSurveyDesignStart() {
		return surveyDesignStart;
	}

	public void setSurveyDesignStart(Date surveyDesignStart) {
		this.surveyDesignStart = surveyDesignStart;
	}

	/**
	 * survey design end
	 * @return
	 */
	public Date getSurveyDesignEnd() {
		return surveyDesignEnd;
	}

	public void setSurveyDesignEnd(Date surveyDesignEnd) {
		this.surveyDesignEnd = surveyDesignEnd;
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
	 * survey start
	 * @return
	 */
	public Date getSurveyStart() {
		return surveyStart;
	}

	public void setSurveyStart(Date surveyStart) {
		this.surveyStart = surveyStart;
	}

	/**
	 * survey end
	 * @return
	 */
	public Date getSurveyEnd() {
		return surveyEnd;
	}

	public void setSurveyEnd(Date surveyEnd) {
		this.surveyEnd = surveyEnd;
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
	public Date getMissionStart() {
		return missionStart;
	}

	public void setMissionStart(Date missionStart) {
		this.missionStart = missionStart;
	}

	/**
	 * mission end
	 * @return
	 */
	public Date getMissionEnd() {
		return missionEnd;
	}

	public void setMissionEnd(Date missionEnd) {
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

	/**
	 * sampling unit buffer
	 * @return
	 */
	public Double getSmaplingUnitBuffer() {
		return smaplingUnitBuffer;
	}

	public void setSamplingUnitBuffer(Double smaplingUnitBuffer) {
		this.smaplingUnitBuffer = smaplingUnitBuffer;
	}
	
	public MissionTrack.TrackType getTrackType(){
		return this.trackType;
	}
	
	public void setTrackType(MissionTrack.TrackType trackType){
		this.trackType = trackType;
	}
}
