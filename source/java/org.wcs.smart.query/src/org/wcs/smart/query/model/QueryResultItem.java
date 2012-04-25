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
package org.wcs.smart.query.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.PatrolType;

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
public class QueryResultItem {

	private String patrolId;
	private Date patrolStartDate;
	private Date patrolEndDate;
	private String station;
	private String team;
	private String objective;
	private int objectiveRating;
	private String mandate;
	private PatrolType.Type patrolType;
	private byte[] patrolUuid;
	private boolean armed;
	private String patrolLegId;
	private String transportType;
	private Date wpDateTime;
	private Date waypointTime;
	private String leader;
	private String pilot;
	
	private int waypointId;
	private double waypointX;
	private double waypointY;
	private Float waypointDistance;
	private Float waypointDirection;
	private String waypointComment;
	
	private String[] observationCategory;
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	private byte[] observationUuid;
	
	
	/**
	 * @return the patrol id
	 */
	public String getPatrolId() {
		return patrolId;
	}
	
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
	 * @return the patrol-leg leader
	 */
	public String getLeader(){
		return this.leader;
	}
	/**
	 * @param leader the patrol leader
	 */
	public void setLeader(String leader){
		this.leader = leader;
	}
	/**
	 * @return the patrol-leg pilot
	 */
	public String getPilot(){
		return this.pilot;
	}
	/**
	 * @param leader the pilot leader
	 */
	public void setPilot(String pilot){
		this.pilot = pilot;
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
	public void setCategory(Category cat){
		if (cat == null){
			return;
		}
		ArrayList<String> values = new ArrayList<String>();
		values.add(cat.getName());
		Category parent = cat.getParent();
		while(parent != null){
			values.add(parent.getName());
			parent = parent.getParent();
		}
		Collections.reverse(values);
		
		observationCategory = values.toArray(new String[values.size()]);
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
		
	/**
	 * @param patrolId patrol id
	 */
	public void setPatrolId(String patrolId) {
		this.patrolId = patrolId;
	}
	/**
	 * @return patrol start date
	 */
	public Date getPatrolStartDate() {
		return patrolStartDate;
	}
	/**
	 * @param patrolStartDate  patrol start date 
	 */
	public void setPatrolStartDate(Date patrolStartDate) {
		this.patrolStartDate = patrolStartDate;
	}
	/**
	 * @return patrol end date
	 */
	public Date getPatrolEndDate() {
		return patrolEndDate;
	}
	/**
	 * @param patrolEndDate patrol end date
	 */
	public void setPatrolEndDate(Date patrolEndDate) {
		this.patrolEndDate = patrolEndDate;
	}
	
	
	/**
	 * @return patrol station name
	 */
	public String getStation() {
		return station;
	}
	
	/**
	 * @param station patrol station name
	 */
	public void setStation(String station) {
		this.station = station;
	}
	
	/**
	 * @return patrol team name 
	 */
	public String getTeam() {
		return team;
	}
	/**
	 * @param team patrol team name
	 */
	public void setTeam(String team) {
		this.team = team;
	}
	
	/**
	 * @return patrol objective 
	 */
	public String getObjective() {
		return objective;
	}
	/**
	 * @param objective patrol objective
	 */
	public void setObjective(String objective) {
		this.objective = objective;
	}
	
	/**
	 * @return patrol objective rating
	 */
	public int getObjectiveRating() {
		return objectiveRating;
	}
	/**
	 * @param objectiveRating patrol objective rating
	 */
	public void setObjectiveRating(int objectiveRating) {
		this.objectiveRating = objectiveRating;
	}
	
	/**
	 * @return patrol mandate
	 */
	public String getMandate() {
		return mandate;
	}
	/**
	 * @param mandate the patrol mandate
	 */
	public void setMandate(String mandate) {
		this.mandate = mandate;
	}
	
	/**
	 * @return the patrol type 
	 */
	public PatrolType.Type getPatrolType() {
		return patrolType;
	}
	/**
	 * @param patrolType the patrol type
	 */
	public void setPatrolType(PatrolType.Type patrolType) {
		this.patrolType = patrolType;
	}
	
	/**
	 * @return the patrol uuid
	 */
	public byte[] getPatrolUuid() {
		return patrolUuid;
	}
	/**
	 * @param patrolUuid the patrol uuid
	 */
	public void setPatrolUuid(byte[] patrolUuid) {
		this.patrolUuid = patrolUuid;
	}
	/**
	 * @return if the patrol is armed or not
	 */
	public boolean isArmed() {
		return armed;
	}
	/**
	 * @param armed if the patrol is armed or not
	 */
	public void setArmed(boolean armed) {
		this.armed = armed;
	}
	/**
	 * @return patrol leg id
	 */
	public String getPatrolLegId() {
		return patrolLegId;
	}
	/**
	 * @param patrolLegId patrol leg id
	 */
	public void setPatrolLegId(String patrolLegId) {
		this.patrolLegId = patrolLegId;
	}
	/**
	 * @return patrol transport type
	 */
	public String getTransportType() {
		return transportType;
	}
	/**
	 * @param transportType patrol transport type
	 */
	public void setTransportType(String transportType) {
		this.transportType = transportType;
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
	public void setWaypointDistance(float waypointDistance) {
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
	public void setWaypointDirection(float waypointDirection) {
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
}
