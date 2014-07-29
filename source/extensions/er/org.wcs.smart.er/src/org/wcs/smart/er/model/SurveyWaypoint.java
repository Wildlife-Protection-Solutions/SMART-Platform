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

import java.io.Serializable;
import java.util.Arrays;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.observation.model.Waypoint;

/**
 * Survey waypoint object model.  Links a waypoint to
 * a mission and sampling unit.  The sampling unit may
 * be a mission track (reconnaissance tracks)
 * @author Emily
 *
 */
@Entity
@Table(name="smart.survey_waypoint")
@AssociationOverrides({
	@AssociationOverride(name = "id.waypoint", 
		joinColumns = @JoinColumn(name = "wp_uuid")) })

public class SurveyWaypoint {
	
	/*
	 * Associated mission
	 */
	private Mission mission;
	
	/*
	 * Associated sampling unit; NOTE: the sampling
	 * unit may be a track in which case the
	 * track variable is used.
	 */
	private SamplingUnit sunit;
	private MissionTrack track;
	
	
	private SurveyWaypointPk id = new SurveyWaypointPk();
	
	public SurveyWaypoint() {
	}
	
	
	@EmbeddedId
	public SurveyWaypointPk getId(){
		return this.id;
	}
	public void setId(SurveyWaypointPk id){
		this.id = id;
	}
	
	@Transient
	public Waypoint getWaypoint(){
		return id.getWaypoint();
	}
	public void setWaypoint(Waypoint wp){
		id.setWaypoint(wp);
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="sampling_unit_uuid", referencedColumnName="uuid")
	public SamplingUnit getSamplingUnit(){
		return this.sunit;
	}
	
	public void setSamplingUnit(SamplingUnit unit){
		this.sunit = unit;
	}
	
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="mission_track_uuid", referencedColumnName="uuid")
	public MissionTrack getMissionTrack(){
		return this.track;
	}
	
	public void setMissionTrack(MissionTrack track){
		this.track = track;
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="mission_uuid", referencedColumnName="uuid")
	public Mission getMission(){
		return this.mission;
	}
	
	public void setMission(Mission mission){
		this.mission = mission;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other != null && other instanceof SurveyWaypoint) {
			SurveyWaypoint s = (SurveyWaypoint) other;
			if (s.getWaypoint() == null && this.getWaypoint() == null) {
				return this == s;
			} else if (s.getWaypoint() != null && this.getWaypoint() != null){
				return s.getWaypoint().equals(this.getWaypoint());
			}
		}
		return false;
	}

	public int hashCode() {
		if (getWaypoint() != null) {
			return Arrays.hashCode(getWaypoint().getUuid());
		}
		return super.hashCode();
	}
	
	@Embeddable
	private static class SurveyWaypointPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Waypoint wp;
		
		public SurveyWaypointPk(){
		}
		
		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
		public Waypoint getWaypoint(){
			return this.wp;
		}
		public void setWaypoint(Waypoint wp){
			this.wp  = wp;
		}
	}
}
