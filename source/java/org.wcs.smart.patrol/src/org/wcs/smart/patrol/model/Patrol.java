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
package org.wcs.smart.patrol.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Station;

/**
 * Represents a patrol objet
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.patrol")
public class Patrol {

	private byte[] uuid;
	private ConservationArea ca;
	private Station station;
	private Team team;
	private String id;
	private Integer objectiveRating;
	private String objective;
	private PatrolMandate mandate;
	private PatrolType.Type patrolType;
	private boolean isArmed;
	private Date startDate;
	private Date endDate;
	
	
	private List<PatrolLeg> legs;
	
	public Patrol(){
		
	}

	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid() {
		return uuid;
	}

	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}

	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name = "id")
	public String getId(){
		return this.id;
	}
	public void setId(String id){
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="station_uuid", referencedColumnName="uuid")
	public Station getStation() {
		return station;
	}

	public void setStation(Station station) {
		this.station = station;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="team_uuid", referencedColumnName="uuid")
	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	@Column(name="objective_rating")
	public Integer getObjectiveRating() {
		return objectiveRating;
	}

	public void setObjectiveRating(Integer objectiveRating) {
		this.objectiveRating = objectiveRating;
	}

	@Column(name="objective")
	public String getObjective() {
		return objective;
	}

	public void setObjective(String objective) {
		this.objective = objective;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="mandate_uuid", referencedColumnName="uuid")
	public PatrolMandate getMandate() {
		return mandate;
	}

	public void setMandate(PatrolMandate mandate) {
		this.mandate = mandate;
	}

	@Column(name="patrol_type")
	@Enumerated(EnumType.STRING)
	public PatrolType.Type getPatrolType() {
		return patrolType;
	}

	public void setPatrolType(PatrolType.Type patrolType) {
		this.patrolType = patrolType;
	}

	@Column(name="is_armed")
	public boolean isArmed() {
		return isArmed;
	}

	public void setArmed(boolean isArmed) {
		this.isArmed = isArmed;
	}

	@Column(name="start_date")
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@Column(name="end_date")
	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name="patrol_uuid", referencedColumnName="uuid")
	public List<PatrolLeg> getLegs(){
		return this.legs;
	}
	public void setLegs(List<PatrolLeg> legs){
		this.legs = legs;
	}
	
	/**
	 * Gets the first leg associated with the patrol.  If
	 * not legs are yet associated with the patrol a new leg is 
	 * created and added to the patrol
	 * 
	 * @return the first patrol leg
	 */
	@Transient
	public PatrolLeg getFirstLeg(){
		if (this.legs == null || this.legs.size() == 0){
			return addLeg();
		}
		return this.legs.get(0);
	}
	
	
	/**
	 * Creates a new patrol leg, assigns an
	 * id and returns the newly created leg.
	 * 
	 * @return  newly created patrol leg
	 */
	@Transient
	public PatrolLeg addLeg(){
		if (this.legs == null){
			this.legs = new ArrayList<PatrolLeg>();
		}
		
		PatrolLeg pl = new PatrolLeg();
		pl.setId(this.legs.size());
		pl.setPatrol(this);
		pl.setStartDate(getStartDate());
		pl.setEndDate(getEndDate());
		
		this.legs.add(pl);
		return pl;
	}
	
	/**
	 * 
	 * @return <code>true</code> if the patrol has a pilot, <code>false</code> otherwise
	 */
	@Transient
	public boolean hasPilot(){
		if (patrolType != null && (patrolType == PatrolType.Type.AIR || patrolType == PatrolType.Type.MARINE)){
			return true;
		}
		return false;
	}
	
	/**
	 * Creates the required leg-day objects
	 * for the patrol.
	 */
	@Transient
	public void createLegDays(){
		for (PatrolLeg leg : getLegs()){
			leg.createLegDays();
		}
	}
}
