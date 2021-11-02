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
package org.wcs.smart.patrol.query.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;

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
public class PatrolWaypointResultItem extends WaypointQueryResultItem implements IPatrolQueryResultItem{

	private String patrolId;
	private LocalDate patrolStartDate;
	private LocalDate patrolEndDate;
	private String station;
	private String team;
	private String objective;
	private String mandate;
	private PatrolType.Type patrolType;
	private UUID patrolUuid;
	private boolean armed;
	private String patrolLegId;
	private UUID patrolLegUuid;
	private String transportType;
	private LocalDate plStartDate;
	private LocalDate plEndDate;
	
	private String leader;
	private String pilot;
	
	private Map<String, Object> patrolAttributes = new HashMap<>();
	
	/**
	 * @return the patrol id
	 */
	public String getPatrolId() {
		return patrolId;
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
	 * @param patrolId patrol id
	 */
	public void setPatrolId(String patrolId) {
		this.patrolId = patrolId;
	}
	/**
	 * @return patrol start date
	 */
	public LocalDate getPatrolStartDate() {
		return patrolStartDate;
	}
	/**
	 * @param patrolStartDate  patrol start date 
	 */
	public void setPatrolStartDate(LocalDate patrolStartDate) {
		this.patrolStartDate = patrolStartDate;
	}
	/**
	 * @return patrol end date
	 */
	public LocalDate getPatrolEndDate() {
		return patrolEndDate;
	}
	/**
	 * @param patrolEndDate patrol end date
	 */
	public void setPatrolEndDate(LocalDate patrolEndDate) {
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
		if (station == null) {
			this.station = ""; //$NON-NLS-1$
			return;
		}
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
		if (team == null) {
			this.team = ""; //$NON-NLS-1$
			return;
		}
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
	 * @return patrol mandate
	 */
	public String getMandate() {
		return mandate;
	}
	/**
	 * @param mandate the patrol mandate
	 */
	public void setMandate(String mandate) {
		if (mandate == null) {
			this.mandate = ""; //$NON-NLS-1$
			return;
		}
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
	 * (optional)
	 * @return the patrol leg uuid 
	 */
	public UUID getPatrolLegUuid() {
		return patrolLegUuid;
	}
	/**
	 * sets the patrol leg uuid (optional)
	 * @param patrolUuid the patrol uuid
	 */
	public void setPatrolLegUuid(UUID legUuid) {
		this.patrolLegUuid = legUuid;
	}
	
	/**
	 * @return the patrol uuid
	 */
	public UUID getPatrolUuid() {
		return patrolUuid;
	}
	/**
	 * @param patrolUuid the patrol uuid
	 */
	public void setPatrolUuid(UUID patrolUuid) {
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
	
	public void setPatrolLegStartDate(LocalDate date){
		this.plStartDate = date;
	}
	public void setPatrolLegEndDate(LocalDate date){
		this.plEndDate = date;
	}
	
	public LocalDate getPatrolLegStartDate(){
		return this.plStartDate;
	}
	public LocalDate getPatrolLegEndDate(){
		return this.plEndDate;
	}
	
	public Object getPatrolAttribute(String keyId) {
		return patrolAttributes.get(keyId);
	}
	
	public void setPatrolAttribute(String keyId, Object value) {
		patrolAttributes.put(keyId, value);
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(patrolUuid, patrolLegUuid, getWaypointUuid());
		
	}
	
	@Override
	public boolean equals(Object other){
		if (other == this) return true;
		if (other == null) return false;
		if (!other.getClass().equals(getClass())) return false;
		PatrolWaypointResultItem o = (PatrolWaypointResultItem) other;
		
		return Objects.equals(patrolLegUuid, o.patrolLegUuid) &&
				Objects.equals(patrolUuid, o.patrolUuid) &&
				Objects.equals(getWaypointUuid(), o.getWaypointUuid());
	}
}
