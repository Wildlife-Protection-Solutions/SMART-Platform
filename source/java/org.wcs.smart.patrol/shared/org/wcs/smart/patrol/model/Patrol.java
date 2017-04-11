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

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OrderBy;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Represents a patrol object
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.patrol")
public class Patrol extends UuidItem {

	/**
	 * Location of patrol data in the filestore
	 */
	public static final String PATROL_FILESTORE_LOC = "patrol"; //$NON-NLS-1$
	
	/**
	 * Absolute maximum length of patrol in days
	 */
	public static final int MAX_PATROL_LENGTH_DAYS = 60;
	/**
	 * User warning patrol length
	 */
	public static final int WARN_PATROL_LENGTH_DAYS = 30;
	/**
	 * Maximum patrol id length
	 */
	public static final Integer MAX_ID_LENGTH = 32;
	/**
	 * Maximum length of patrol objective
	 */
	public static final int MAX_OBJECTIVE_LENGTH = 8192;
	/**
	 * Maximum length of patrol comment
	 */
	public static final int MAX_COMMENT_LENGTH = 32700;
	
	private ConservationArea ca;
	private Station station;
	private Team team;
	private String id;
	private String objective;
	private PatrolMandate mandate;
	private PatrolType.Type patrolType;
	private boolean isArmed;
	private Date startDate;
	private Date endDate;
	private String comment;
	
	
	private List<PatrolLeg> legs;
	
	public Patrol(){
		
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

	@Column(name="comment")
	public String getComment() {
		return this.comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
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
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="patrol", cascade={CascadeType.ALL}, orphanRemoval = true)
	@OrderBy(clause="start_date")
	@BatchSize(size=50)
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
		pl.setId(String.valueOf((this.legs.size() + 1)));
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
		return patrolType != null && patrolType.requiresPilot();
	}
	
	/**
	 * Creates the required leg-day objects
	 * for the patrol.
	 */
	@Transient
	public Collection<PatrolLegDay> createLegDays(Session session){
		for (PatrolLeg leg : getLegs()){
			leg.createLegDays(session);
		}
		return null;
	}
	
	
	/**
	 * Creates leg objects to fill in any days where no leg exist. We can't have "blank" days in between the start and end of a patrol in the current model.
	 * for the patrol.
	 */
	@Transient
	public Collection<PatrolLegDay> createLegs(Session session){
		
		//determine start & end dates
		Calendar cal = SharedUtils.convertDate( SharedUtils.getDatePart(getStartDate(), false) );
		Calendar calEnd= SharedUtils.convertDate( SharedUtils.getDatePart(getEndDate(), false) );

		while (cal.before(calEnd) || cal.equals(calEnd) ){
			boolean legFound = false;
			for (PatrolLeg leg : getLegs()){
				Calendar legStart = SharedUtils.convertDate( SharedUtils.getDatePart(leg.getStartDate(), false));
				Calendar legEnd = SharedUtils.convertDate( SharedUtils.getDatePart(leg.getEndDate(), false));
				if(cal.equals(legStart) || cal.equals(legEnd) ||  (cal.after(legStart) && cal.before(legEnd)) ){
					legFound = true;
					break;
				}
			}

			if(legFound == false){
				PatrolLeg newLeg = new PatrolLeg();
				newLeg.setStartDate(cal.getTime());
				newLeg.setEndDate(cal.getTime());
				newLeg.setPatrol(this);
				newLeg.setType(getLegs().get(0).getType());//set it to the same type as the first leg, assuming they keep the same type for this empty/filler leg.
				newLeg.setId("Automatically Created Patrol Leg");
				this.getLegs().add(newLeg);
				newLeg.createLegDays(session);
			}
			
			
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		
		return null;
	}
	
	/**
	 * Calculates and updates the type of the patrol based on transport types in assigned legs.
	 */
	@Transient
	public void recalculateType() {
		if (getLegs() == null || getLegs().isEmpty())
			return;
		PatrolType.Type type = getLegs().get(0).getType().getPatrolType();
		for (PatrolLeg leg : getLegs()){
			if (!type.equals(leg.getType().getPatrolType())) {
				setPatrolType(PatrolType.Type.MIXED);
				return;
			}
		}
		setPatrolType(type);
	}
	
	/**
	 * 
	 * <p>
	 * To get full file names you must prepend this with the conservation area file store location.
	 * </p>
	 * <code>
	 * ConservationArea.getFileDataStoreLocation() + File.separator + Patrol.getPatrolDatastorePath();
	 * </code>
	 * @return the file store location for the patrol relative to the conservation area file store
	 */
	@Transient
	public String getPatrolDatastorePath(){
		return PATROL_FILESTORE_LOC + File.separator + UuidUtils.getDirectoryPath(getUuid());
	}

	/*
	 * Only Clones the simple attributes, not the legs, leg days or waypoints.
	 */
	public Patrol simpleClone() {
		Patrol clone = new Patrol();
		clone.setArmed(isArmed);
		clone.setComment(comment);
		clone.setConservationArea(ca);
		clone.setEndDate(endDate);
		clone.setId(id);
		clone.setMandate(mandate);
		clone.setObjective(objective);
		clone.setPatrolType(patrolType);
		clone.setStartDate(startDate);
		clone.setStation(station);
		clone.setTeam(team);
		return clone;
	}


	
}
