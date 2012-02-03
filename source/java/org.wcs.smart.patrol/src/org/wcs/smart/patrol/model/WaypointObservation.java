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

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.datamodel.Category;

/**
 * Waypoint observation
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.wp_observation")
public class WaypointObservation {
	
	private byte[] uuid;
	private Waypoint waypoint;
	private Category category;
	
	private List<WaypointObservationAttribute> attributes; 
	
	public WaypointObservation(){
		
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
	public Waypoint getWaypoint(){
		return this.waypoint;
	}
	public void setWaypoint(Waypoint waypoint){
		this.waypoint = waypoint;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	public Category getCategory(){
		return this.category;
	}
	public void setCategory(Category categort){
		this.category = category;
	}
	
	@OneToMany(fetch = FetchType.LAZY)
	public List<WaypointObservationAttribute> getAttributes(){
		return this.attributes;
	}
	public void setAttributes(List<WaypointObservationAttribute> attributes){
		this.attributes = attributes;
	}
}
