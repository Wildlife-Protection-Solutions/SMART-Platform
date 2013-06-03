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
package org.wcs.smart.cybertracker.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.ConservationArea;

/**
 * Class responsible for representing CyberTracker Properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.cybertracker_properties")
public class CyberTrackerProperties {

	private byte[] uuid;
	private ConservationArea conservationArea;
	
	//default properties
	private Boolean kioskMode = false;
	private Integer waypointTimer = 0; //Track Timer 
	private Integer gpsTimeZone = 0; //GMT/UTC time offset
	

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
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}
	
	@Column(name="kiosk_mode")
	public Boolean getKioskMode() {
		return kioskMode;
	}
	public void setKioskMode(Boolean kioskMode) {
		this.kioskMode = kioskMode;
	}
	
	@Column(name="waypoint_timer")
	public Integer getWaypointTimer() {
		return waypointTimer;
	}
	public void setWaypointTimer(Integer waypointTimer) {
		this.waypointTimer = waypointTimer;
	}
	
	@Column(name="gps_time_zone")
	public Integer getGpsTimeZone() {
		return gpsTimeZone;
	}
	public void setGpsTimeZone(Integer gpsTimeZone) {
		this.gpsTimeZone = gpsTimeZone;
	}

}
