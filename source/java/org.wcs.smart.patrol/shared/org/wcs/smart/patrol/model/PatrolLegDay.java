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


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.locationtech.jts.geom.LineString;
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
@Table(name="patrol_leg_day", schema="smart")
public class PatrolLegDay extends UuidItem {
	
	private static final long serialVersionUID = 1L;
	
	private LocalDate date;

	private List<PatrolWaypoint> waypoints;
	private PatrolLeg patrolLeg;
	
	private LocalTime startTime;
	private LocalTime endTime;
	
	private Integer restMinutes;
	
	
	/* this list should only ever contain a single track
	 * It was implemented this way to get around hibernate bug
	 * with one to one relationships
	 */
	private List<Track> tracks; 
	
	@Column(name="patrol_day")
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
		for (Track old : this.tracks) {
			old.setPatrolLegDay(null);
		}
		this.tracks.clear();
		
		if (t != null){
			this.tracks.add(t);
		}
	}
	
	@Transient
	public double getLengthSeconds(){
		return ChronoUnit.SECONDS.between(startTime, endTime);
	}
	
	@Transient
	public double getPatrolHoursWorked(){
		if (startTime == null || endTime == null){
			return 0;
		}
		// As per comment on 1177
		// https://www.assembla.com/spaces/smart-cs/tickets/1177-provide-the-option-of-including-(not-subtracting)-rest-time-when-calculating-patrol-and-mission-e---?comment=814666483#comment:814666483
		// do not subtract rest time for patrol hours worked
		double time = getLengthSeconds() / (60*60);
		return time;
	}
		
	
	@Transient
	public double getFieldHoursWorked(){
		if (startTime == null || endTime == null){
			return 0;
		}
		// As per comment on 1177
		// https://www.assembla.com/spaces/smart-cs/tickets/1177-provide-the-option-of-including-(not-subtracting)-rest-time-when-calculating-patrol-and-mission-e---?comment=814666483#comment:814666483
		// subtract rest time for field hours worked
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

	/**
	 * Returns true when there is at least one waypoint or one track for the patrol leg day
	 * 
	 * @return
	 */
	@Transient
	public boolean hasData() {
		boolean hasTrack = getTrack() != null;
		boolean hasWaypoints = getWaypoints() != null && !getWaypoints().isEmpty();
		return (hasTrack || hasWaypoints);
	}
	
	/**
	 * Returns an array of times (min, max) of the earliest date/time and latest date/time
	 * of all the waypoints and trackpoints for this patrol leg day.
	 * 
	 * @return
	 */
	@Transient
	public LocalDateTime[] computeMinMaxDate() {
		List<PatrolWaypoint> wps = getWaypoints();
		
		LocalDateTime mindate = null;
		LocalDateTime maxdate = null;
		
		if (!wps.isEmpty()) {
			for (PatrolWaypoint wp : wps) {
				LocalDateTime thisdt = wp.getWaypoint().getDateTime();
				if (mindate == null || thisdt.isBefore(mindate)) mindate = thisdt;
				if (maxdate == null || thisdt.isAfter(maxdate)) maxdate = thisdt;
			}	
		}
		
		if (getTrack() != null) {
			try {
				for (LineString ls : getTrack().getLineStrings()) {
					
					long z1 = ((Double)ls.getCoordinateN(0).getZ()).longValue();
					long z2 = ((Double)ls.getCoordinateN(ls.getNumPoints() - 1).getZ()).longValue();
					
					for (long d : new long[] {z1, z2}) {
						LocalDateTime thisdt = Instant.ofEpochMilli(d).atOffset(ZoneOffset.UTC).toLocalDateTime();
						if (mindate == null || thisdt.isBefore(mindate)) mindate = thisdt;
						if (maxdate == null || thisdt.isAfter(maxdate)) maxdate = thisdt;
					}
				}
			}catch (Exception ex) {
				
			}
		}
		if (mindate == null || maxdate == null) return null;
		return new LocalDateTime[] {mindate, maxdate};
	}
	/**
	 * Clones the patrol leg day 
	 * NOTE: This function doesn't clone anything to do with waypoints
	 */
	public PatrolLegDay clone (){
		PatrolLegDay clone = new PatrolLegDay();
		clone.setDate(date);
		clone.setEndTime(endTime);
		clone.setRestMinutes(restMinutes);
		clone.setStartTime(startTime);
		if(tracks != null && tracks.size() > 0){
			Track t = new Track();
			t.setDistance(getTrack().getDistance());
			t.setGeom(getTrack().getGeom());
			t.setPatrolLegDay(clone);
			clone.setTracks(new ArrayList<Track>());
			clone.getTracks().add(t);
		}
		return clone;
	}
	
}
