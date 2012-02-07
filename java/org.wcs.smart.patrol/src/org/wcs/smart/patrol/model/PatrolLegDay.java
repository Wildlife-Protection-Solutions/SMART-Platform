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

import java.sql.Time;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

/**
 * Patrol leg day object.
 * <p>
 * A patrol leg day is one day within a patrol leg.  A Patrol leg will have
 * at least one leg day although it may span multiple days and have multiple
 * leg days.
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.patrol_leg_day")
public class PatrolLegDay {
	
	private byte[] uuid;
	private Date date;

	private List<Waypoint> waypoints;
	private PatrolLeg patrolLeg;
	
	private Time startTime;
	private Time endTime;
	
	private Track track;
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid() {
		return uuid;
	}
	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}
	
	@Column(name="patrol_day")
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	
	@OneToMany(fetch = FetchType.LAZY)
	public List<Waypoint> getWaypoints(){
		return this.waypoints;
	}
	public void setWaypoints(List<Waypoint> waypoints){
		this.waypoints = waypoints;
	}

	@ManyToOne
	@JoinColumn(name="patrol_leg_uuid", referencedColumnName="uuid")
	public PatrolLeg getPatrolLeg(){
		return this.patrolLeg;
	}
	public void setPatrolLeg(PatrolLeg patrolLeg){
		this.patrolLeg = patrolLeg;
	}
	
	@Column(name="start_time")
	public Time getStartTime(){
		return this.startTime;
	}
	public void setStartTime(Time startTime){
		this.startTime = startTime;
	}
	
	@Column(name="end_time")
	public Time getEndTime(){
		return this.endTime;
	}
	public void setEndTime(Time endTime){
		this.endTime = endTime;
	}
	
	@OneToOne
	@JoinColumn(name="uuid", referencedColumnName = "patrol_leg_day_uuid")
	public Track getTrack(){
		return this.track;
	}
	public void setTrack(Track track){
		this.track = track;
	}
}
