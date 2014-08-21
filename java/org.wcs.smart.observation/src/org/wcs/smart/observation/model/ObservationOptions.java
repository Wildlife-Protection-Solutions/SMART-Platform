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
package org.wcs.smart.observation.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.Projection;

/**
 * Class to track patrol
 * options for a given conservation
 * area.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.observation_options")
public class ObservationOptions {

	private boolean trackDistanceDirection;
	private boolean trackObserver;
	private Integer editTime;
	private Projection viewProjection;
	private byte[] ca_uuid;
	
	public ObservationOptions(){}
	
	/**
	 * 
	 * @return the uuid of the conservation area associated with the
	 * patrol options
	 */
	@Id
	@Column(name="ca_uuid")
	public byte[] getUuid(){
		return this.ca_uuid;
	}
	/**
	 * 
	 * @param uuid the conservation area uuid
	 */
	public void setUuid(byte[] uuid){
		this.ca_uuid = uuid;
	}
	
	/**
	 * 
	 * @return distance direction property
	 */
	@Column(name="distance_direction")
	public boolean getTrackDistanceDirection(){
		return this.trackDistanceDirection;
	}
	/**
	 * 
	 * @param track the distance direction property
	 */
	public void setTrackDistanceDirection(boolean track){
		this.trackDistanceDirection = track;
	}
	/**
	 * The number of days since the start time fo the patrol that
	 * it can be edited by data entry users.
	 * @return the edit time property
	 */
	@Column(name="edit_time")
	public Integer getEditTime(){
		return this.editTime;
	}
	/**
	 * 
	 * @param editTime the edit time property
	 */
	public void setEditTime(Integer editTime){
		this.editTime = editTime;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="view_projection_uuid", referencedColumnName="uuid")
	public Projection getViewProjection() {
		return viewProjection;
	}
	public void setViewProjection(Projection viewProjection) {
		this.viewProjection = viewProjection;
	}

	@Column(name="observer")
	public boolean getTrackObserver(){
		return this.trackObserver;
	}
	
	/**
	 * 
	 * @param track the observer property
	 */
	public void setTrackObserver(boolean track){
		this.trackObserver = track;
	}
}
