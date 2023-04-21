/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.incident.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.model.Patrol;


/**
 * IncidentWaypoint adds addition details to waypoints specific
 * to incidents. 
 * 
 * @author Emily
 * @since 7.5.7
 * 
 */
@Entity
@Table(name="smart.incident_waypoint")
public class IncidentWaypoint implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Waypoint waypoint;	
	private Patrol patrol;
	
    @Id
    @OneToOne
    @JoinColumn(name = "wp_uuid")
    public Waypoint getWaypoint() {
    	return this.waypoint;
    }
    
    public void setWaypoint(Waypoint waypoint) {
    	this.waypoint = waypoint;
    }
    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="patrol_uuid", referencedColumnName="uuid")
	public Patrol getPatrol() {
		return this.patrol;
	}
	
	public void setPatrol(Patrol patrol) {
		this.patrol = patrol;
	}
	
	@Override
	public boolean equals(Object other){
		if (other == null) return false;
		if (other.getClass() != getClass()) return false;
		return waypoint.equals(((IncidentWaypoint)other).getWaypoint());
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(waypoint);
	}
	
	
}
