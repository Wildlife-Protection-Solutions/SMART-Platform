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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;

/**
 * Waypoint observation
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.wp_observation")
public class WaypointObservation {
	
	private byte[] uuid = null;
	private Waypoint waypoint = null;
	private Category category = null;
	
	private List<WaypointObservationAttribute> attributes = null; 
	
	 
	
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
	@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
	public Waypoint getWaypoint(){
		return this.waypoint;
	}
	public void setWaypoint(Waypoint waypoint){
		this.waypoint = waypoint;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="category_uuid", referencedColumnName="uuid")
	public Category getCategory(){
		return this.category;
	}
	public void setCategory(Category category){
		this.category = category;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.observation", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<WaypointObservationAttribute> getAttributes(){
		return this.attributes;
	}
	public void setAttributes(List<WaypointObservationAttribute> attributes){
		this.attributes = attributes;
//		if (this.attributes == null){
//			this.attributes = attributes;
//		}else{
//			this.attributes.clear();
//			if (attributes != null){
//				this.attributes.addAll(attributes);
//			}
//		}
	}
	
	/**
	 * Finds the observation attribute for a given attribute.
	 * 
	 * @param attribute
	 * @return observation attribute or null if not found
	 */
	@Transient
	public WaypointObservationAttribute findAttribute(Attribute attribute){
		if (getAttributes() ==  null){
			return null;
		}
		for (WaypointObservationAttribute att: getAttributes()){
			if (att.getAttribute().equals(attribute)){
				return att;
			}
		}
		return null;
	}
	
	@Override
	public int hashCode(){
		if (uuid != null){
			return Arrays.hashCode(uuid);
		}else{
			return super.hashCode();
		}
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof WaypointObservation){
			WaypointObservation s = (WaypointObservation)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return super.equals(other);
			}else if (s.getUuid() != null && this.getUuid() != null){
				return Arrays.equals(s.getUuid(), this.getUuid());
			}
		}
		return false;
	}
	
	/**
	 * Clones the waypoint observation object.  Does not
	 * clone the uuid or the waypoint.
	 */
	public WaypointObservation clone(){
		WaypointObservation clone = new WaypointObservation();
		
		clone.category = getCategory();
		if (attributes != null) {
			clone.attributes = new ArrayList<WaypointObservationAttribute>();
			for (Iterator<WaypointObservationAttribute> iterator = attributes.iterator(); iterator.hasNext();) {
				WaypointObservationAttribute type = (WaypointObservationAttribute) iterator.next();
				WaypointObservationAttribute ctype = type.clone();
				ctype.setObservation(clone);
				clone.attributes.add(ctype);
			}
		}
		return clone;
		
	}
}
