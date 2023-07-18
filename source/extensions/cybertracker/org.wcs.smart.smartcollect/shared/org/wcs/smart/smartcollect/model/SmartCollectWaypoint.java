/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.smartcollect.model;

import java.io.Serializable;
import java.util.Objects;

import org.wcs.smart.incident.model.IncidentWaypoint;
import org.wcs.smart.observation.model.Waypoint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;


/**
 * SMARTCollect waypoint adds source information to a waypoint
 * which identifies which community user provided the data
 * 
 * @author Emily
 * 
 */
@Entity
@Table(name="smartcollect_waypoint", schema="smart")
public class SmartCollectWaypoint implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Waypoint waypoint;
	
	private String source;
	
    @Id
    @OneToOne
    @JoinColumn(name = "wp_uuid")
    public Waypoint getWaypoint() {
    	return this.waypoint;
    }
    
    public void setWaypoint(Waypoint waypoint) {
    	this.waypoint = waypoint;
    }
    
	@Column(name="source")
	public String getSource() {
		return this.source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;		
		SmartCollectWaypoint s = (SmartCollectWaypoint)other;
		return (Objects.equals(getWaypoint(), s.getWaypoint()));
		
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(waypoint);
	}
	
	
}
