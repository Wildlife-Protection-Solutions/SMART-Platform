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
package org.wcs.smart.er.model;

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

import org.wcs.smart.ca.UuidItem;


@Entity
@Table(name = "smart.mission_day")
public class MissionDay extends UuidItem{

	private Date date;
	
	private Mission mission;
	private Time startTime;
	private Time endTime;
	private Integer restMinutes;
	
	private List<SurveyWaypoint> waypoints;
	private List<MissionTrack> tracks;
	
	@Column(name="mission_day")
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
	
	@ManyToOne()
	@JoinColumn(name="mission_uuid")
	public Mission getMission(){
		return this.mission;
	}
	
	public void setMission(Mission mission){
		this.mission = mission;
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

	@OneToMany(fetch = FetchType.LAZY, mappedBy="missionDay")
	public List<SurveyWaypoint> getWaypoints() {
		if (waypoints == null) {
			waypoints = new ArrayList<SurveyWaypoint>();
		}
		return this.waypoints;
	}
	
	public void setWaypoints(List<SurveyWaypoint> waypoints){
		this.waypoints = waypoints;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="missionDay", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<MissionTrack> getTracks(){
		if (tracks == null) {
			tracks = new ArrayList<MissionTrack>();
		}
		return this.tracks;
	}
	
	public void setTracks(List<MissionTrack> tracks){
		this.tracks = tracks;
	}
}
