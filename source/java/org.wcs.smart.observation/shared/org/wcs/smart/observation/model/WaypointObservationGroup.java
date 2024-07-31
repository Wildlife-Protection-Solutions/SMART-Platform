/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.observation.model;

import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Waypoint observation group
 * 
 * @author Emily
 * @since 7.0.0
 */
@Entity
@Table(name="wp_observation_group", schema="smart")
public class WaypointObservationGroup extends UuidItem {

	private static final long serialVersionUID = 1L;
	
	private Waypoint waypoint = null;

	private List<WaypointObservation> observations;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
	public Waypoint getWaypoint(){
		return this.waypoint;
	}
	public void setWaypoint(Waypoint waypoint){
		this.waypoint = waypoint;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="observationGroup", 
			orphanRemoval=true, cascade=CascadeType.ALL)
	@BatchSize(size=500)
	public List<WaypointObservation> getObservations(){
		return this.observations;
	}
	public void setObservations(List<WaypointObservation> observations){
		this.observations = observations;
	}
}
