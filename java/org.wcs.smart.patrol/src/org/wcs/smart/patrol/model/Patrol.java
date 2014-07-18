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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
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

import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OrderBy;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Station;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Represents a patrol object
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.patrol")
public class Patrol {

	/**
	 * Location of patrol data in the filestore
	 */
	public static final String PATROL_FILESTORE_LOC = "patrol"; //$NON-NLS-1$
	
	/**
	 * Text to identify patrol id as auto-generated
	 */
	public static final String AUTO_GENERATE_TEXT = Messages.Patrol_SystemGenerateId_Name;

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
	
	private byte[] uuid;
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
	public Collection<PatrolLegDay> createLegDays(Session session){
		for (PatrolLeg leg : getLegs()){
			leg.createLegDays(session);
		}
		return null;
	}
	
	
	@Override
	public int hashCode(){
		if (uuid != null){
			return Arrays.hashCode(uuid);
		}else{
			return super.hashCode();
		}
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof Patrol){
			Patrol s = (Patrol)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return super.equals(s);
			}else if (s.getUuid() != null && this.getUuid() != null){
				return Arrays.equals(s.getUuid(), this.getUuid());
			}
		}
		return false;
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
		return PATROL_FILESTORE_LOC + File.separator + SmartUtils.getDirectoryPath(uuid);
	}


	
}
