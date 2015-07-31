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

import java.io.Serializable;

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
 * @author Emily
 * Link between a waypoint and the patrol.
 * 
 */
@Entity
@Table(name="smart.patrol_waypoint")
@AssociationOverrides({
	@AssociationOverride(name = "id.waypoint", 
		joinColumns = @JoinColumn(name = "wp_uuid")),
	@AssociationOverride(name = "id.patrolLegDay", 
		joinColumns = @JoinColumn(name = "leg_day_uuid")) })
public class PatrolWaypoint {
	
	private PatrolWaypointPk id = new PatrolWaypointPk();

	
	@EmbeddedId
	public PatrolWaypointPk getId(){
		return this.id;
	}
	public void setId(PatrolWaypointPk id){
		this.id = id;
	}
	
	@Transient
	public Waypoint getWaypoint(){
		return this.getId().getWaypoint();
	}
	
	public void setWaypoint(Waypoint wp){
		this.getId().setWaypoint(wp);
	}
	
	@Transient
	public PatrolLegDay getPatrolLegDay(){
		return this.getId().getPatrolLegDay();
	}
	public void setPatrolLegDay(PatrolLegDay pld){
		this.getId().setPatrolLegDay(pld);
	}
		
	@Embeddable
	private static class PatrolWaypointPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private PatrolLegDay pld;
		private Waypoint wp;
		
		public PatrolWaypointPk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="leg_day_uuid", referencedColumnName="uuid")
		public PatrolLegDay getPatrolLegDay(){
			return this.pld;
		}
		public void setPatrolLegDay(PatrolLegDay pld){
			this.pld = pld;
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
