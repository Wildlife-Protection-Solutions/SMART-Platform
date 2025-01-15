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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


@Entity
@Table(name = "mission_day", schema="smart")
public class MissionDay extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	private LocalDate date;
	
	private Mission mission;
	private LocalTime startTime;
	private LocalTime endTime;
	private Integer restMinutes;
	
	private List<SurveyWaypoint> waypoints;
	private List<MissionTrack> tracks;
	
	@Column(name="mission_day")
	public LocalDate getDate() {
		return date;
	}
	public void setDate(LocalDate date) {
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
	public LocalTime getStartTime(){
		return this.startTime;
	}
	public void setStartTime(LocalTime startTime){
		this.startTime = startTime;
	}
	
	@Column(name="end_time")
	public LocalTime getEndTime(){
		return this.endTime;
	}
	public void setEndTime(LocalTime endTime){
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
		return ChronoUnit.SECONDS.between(date.atTime(startTime), date.atTime(endTime));
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

	@OneToMany(fetch = FetchType.LAZY, mappedBy="missionDay", cascade={CascadeType.ALL})
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
