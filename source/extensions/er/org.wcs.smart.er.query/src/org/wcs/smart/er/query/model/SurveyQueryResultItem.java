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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.wcs.smart.query.model.IResultItem;

/**
 * A class to hold the results of a waypoint 
 * query.  Each class contains the results for
 * a single observation.  The observation contains
 * a single category and all attributes.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryResultItem implements IResultItem{

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
	
	private String samplingUnitId;
	private Double smaplingUnitBuffer;
	
	private Date wpDateTime;
	private Date waypointTime;
	
	private byte[] waypointUuid;
	private int waypointId;
	private double waypointX;
	private double waypointY;
	private Float waypointDistance;
	private Float waypointDirection;
	private String waypointComment;
	
	private String[] observationCategory;
	private HashMap<String, Object> properties = new HashMap<String, Object>();
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	private byte[] observationUuid;
	private List<byte[]> tracks = null;
	
	/**
	 * @param observationUuid the observation uuid
	 */
	public void setObservationUuid(byte[] observationUuid){
		this.observationUuid = observationUuid;
	}
	
	/**
	 * @return the observation uuid
	 */
	public byte[] getObservationUuid(){
		return this.observationUuid;
	}
	
	
	/**
	 * Each item is associated with a single category.  This
	 * returns an array of the names of the category and
	 * all the parent categories:
	 *   - {parent1, parent2, category}
	 * 
	 * @return an array of the category names of the category & parent categories
	 */
	public String[] getCategories(){
		return this.observationCategory;
	}
	
	/**
	 * @param cat sets the category
	 */
	public void setCategory(String[] categoryLabels){
		this.observationCategory = categoryLabels;
	}
	
	/**
	 * Finds the attribute value of the associated attribute
	 * key.
	 * 
	 * @param attributeKey the attribute key
	 * @return the value associated with the attribute given key
	 */
	public Object getAttributeValue(String attributeKey){
		return attributes.get(attributeKey);
	}
	
	/**
	 * Adds an attribute to the observation results 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addAttribute(String key, Object value){
		attributes.put(key, value);
	}
	
	public void setAttributes(HashMap<String, Object> attributes){
		this.attributes = attributes;
	}
		
	/**
	 * Finds the properties value of the associated attribute
	 * key.
	 * 
	 * @param attributeKey the attribute key
	 * @return the value associated with the attribute given key
	 */
	public Object getMissionPropertyValue(String attributeKey){
		return properties.get(attributeKey);
	}
	
	/**
	 * Adds an properties to the observation results 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addMissionPropertyValue(String key, Object value){
		properties.put(key, value);
	}
	
	/**
	 * sets the waypoint uuid
	 * @param uuid
	 */
	public void setWaypointUuid(byte[] uuid){
		this.waypointUuid = uuid;
	}
	/**
	 * 
	 * @return the waypoint uuid
	 */
	public byte[] getWaypointUuid(){
		return this.waypointUuid;
	}
	
	/**
	 * @return waypoint date 
	 */
	public Date getWpDateTime() {
		return wpDateTime;
	}
	/**
	 * @param wpDateTime waypoint date 
	 */
	public void setWpDateTime(Date wpDateTime) {
		this.wpDateTime = wpDateTime;
	}
	/**
	 * @return waypoint time
	 */
	public Date getWaypointTime() {
		return waypointTime;
	}
	/**
	 * @param wpTime waypoint time
	 */
	public void setWaypointTime(Date wpTime) {
		this.waypointTime = wpTime;
	}
	/**
	 * @return waypoint id
	 */
	public int getWaypointId() {
		return waypointId;
	}
	/**
	 * @param waypointId waypoint id
	 */
	public void setWaypointId(int waypointId) {
		this.waypointId = waypointId;
	}
	/**
	 * @return waypoint x (longitude) position
	 */
	public double getWaypointX() {
		return waypointX;
	}
	/**
	 * @param waypointX waypoint y (longitude)
	 */
	public void setWaypointX(double waypointX) {
		this.waypointX = waypointX;
	}
	
	
	/**
	 * @return the waypoint y (latitude)
	 */
	public double getWaypointY() {
		return waypointY;
	}
	/**
	 * @param waypointY the waypoint y (latitude)
	 */
	public void setWaypointY(double waypointY) {
		this.waypointY = waypointY;
	}
	
	/**
	 * @return waypoint distance observation
	 */
	public Float getWaypointDistance() {
		return waypointDistance;
	}
	/**
	 * @param waypointDistance
	 */
	public void setWaypointDistance(Float waypointDistance) {
		this.waypointDistance = waypointDistance;
	}
	
	/**
	 * @return the waypoint direction of observation
	 */
	public Float getWaypointDirection() {
		return waypointDirection;
	}
	/**
	 * @param waypointDirection direction of observation
	 */
	public void setWaypointDirection(Float waypointDirection) {
		this.waypointDirection = waypointDirection;
	}
	
	/**
	 * @return waypoint comment
	 */
	public String getWaypointComment() {
		return waypointComment;
	}
	/**
	 * @param wpComment wyapoint comment
	 */
	public void setWaypointComment(String wpComment) {
		this.waypointComment = wpComment;
	}
	
	
	public List<byte[]> getTrack(){
		return this.tracks;
	}
	public void addTrack(byte[] track){
		if (track == null || track.length == 0){
			return;
		}
		if (this.tracks == null){
			this.tracks = new ArrayList<byte[]>();
		}
		this.tracks.add(track);
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

	public void setSmaplingUnitBuffer(Double smaplingUnitBuffer) {
		this.smaplingUnitBuffer = smaplingUnitBuffer;
	}
	
}
