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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.BatchSize;
import org.wcs.smart.ca.UuidItem;

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
public class PatrolLegDay extends UuidItem {
	
	private Date date;

	private List<PatrolWaypoint> waypoints;
	private PatrolLeg patrolLeg;
	
	private Time startTime;
	private Time endTime;
	
	private Integer restMinutes;
	
	
	/* this list should only ever contain a single track
	 * It was implemented this way to get around hibernate bug
	 * with one to one relationships
	 */
	private List<Track> tracks; 
	
	@Column(name="patrol_day")
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	
	@Column(name="rest_minutes")
	public Integer getRestMinutes(){
		return this.restMinutes;
	}
	public void setRestMinutes(Integer restMinutes){
		this.restMinutes = restMinutes;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.patrolLegDay")
	@BatchSize(size=200)
	public List<PatrolWaypoint> getWaypoints(){
		return this.waypoints;
	}
	public void setWaypoints(List<PatrolWaypoint> waypoints){
		this.waypoints = waypoints;
	}

	@ManyToOne()
	@JoinColumn(name="patrol_leg_uuid")
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
	
	@Transient
	public Track getTrack(){
		if (getTracks() != null && getTracks().size() > 0){
			return getTracks().get(0);
		}
		return null;
	}
	public void setTrack(Track t){
		if (this.tracks == null){
			this.tracks = new ArrayList<Track>();
		}
		if (this.tracks.size() == 1){
			this.tracks.get(0).setPatrolLegDay(null);
			this.tracks.remove(0);
		}
		if (t != null){
			this.tracks.add(t);
		}
	}
	
	@Transient
	public double getLengthSeconds(){
		Calendar cal = Calendar.getInstance();
		cal.setTime(startTime);
		long start = cal.get(Calendar.HOUR_OF_DAY) * 60 *60 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);   
		cal.setTime(endTime);
		long end = cal.get(Calendar.HOUR_OF_DAY) * 60 *60 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
		
		return end - start;
	}
	
	@Transient
	public double getHoursWorked(){
		if (startTime == null || endTime == null){
			return 0;
		}
		
		double time = getLengthSeconds();
		if (restMinutes != null){
			time = time - restMinutes * 60;
		}
		
		time = time / (60*60);
		return time;
	}
	
	//workaround for https://hibernate.onjira.com/browse/HHH-5267
	@OneToMany(fetch = FetchType.LAZY, mappedBy="patrolLegDay", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<Track> getTracks(){
		return this.tracks;
	}
	public void setTracks(List<Track> tracks){
		this.tracks = tracks;
	}
	
}
