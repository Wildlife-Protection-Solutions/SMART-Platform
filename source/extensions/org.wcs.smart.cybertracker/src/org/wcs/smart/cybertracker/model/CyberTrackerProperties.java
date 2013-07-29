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

	public static final int STORAGE_TIME_MIN_VALUE = 0;
	public static final int STORAGE_TIME_MAX_VALUE = 365;
	public static final int STORAGE_TIME_DEFAULT_VALUE = 30;

	/**
	 * Maximum length of application name field
	 */
	public static final int APPLICATION_NAME_MAX_LENTH = 256;

	public static final int EXIT_PIN_MIN_VALUE = 1;
	public static final int EXIT_PIN_MAX_VALUE = 99999999;
	
	public static final double SIGHTING_ACCURACY_MIN_VALUE = 0.01;
	public static final double SIGHTING_ACCURACY_MAX_VALUE = 49;

	public static final int SIGHTING_FIX_COUNT_MIN_VALUE = 1;
	public static final int SIGHTING_FIX_COUNT_MAX_VALUE = 60;

	public static final int TIME_TRACK_MIN_VALUE = 0;
	public static final int TIME_TRACK_MAX_VALUE = 1000;

	public static final Integer[] GTM_VALUES = {
		-1200,
		-1100,
		-1000,
		-900,
		-800,
		-700,
		-600,
		-500,
		-450,
		-400,
		-350,
		-300,
		-200,
		-100,
		0,
		100,
		200,
		300,
		400,
		450,
		500,
		550,
		575,
		600,
		650,
		700,
		800,
		900,
		950,
		1000,
		1050,
		1100,
		1150,
		1200,
		1300,		
	};
	
	public static final int SKIP_BUTTON_TIMEOUT_MIN_VALUE = 0;
	public static final int SKIP_BUTTON_TIMEOUT_MAX_VALUE = Integer.MAX_VALUE;
	
	private byte[] uuid;
	private ConservationArea conservationArea;
	
	//default properties
	private String  applicationName = "SMART"; //$NON-NLS-1$
	
	private boolean largeScrollBars = false;
	private boolean kioskMode = false;
	private int exitPin = 1234;
	
	private Double sightingAccuracy = 49.0;
	private Integer sightingFixCount = 1;
	private Integer waypointTimer = 0; //Track Timer 
	private Integer gpsTimeZone = 0; //GMT/UTC time offset
    private Integer skipButtonTimeout = 3;

	//this value is not CT application parameter but it will be applied to all radio screens while generating application
	private boolean autoNext = true;
	
	private int storageTime = STORAGE_TIME_DEFAULT_VALUE; //indicates how many days ctx files will be stored in SMART storage

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
	public boolean isKioskMode() {
		return kioskMode;
	}
	public void setKioskMode(boolean kioskMode) {
		this.kioskMode = kioskMode;
	}
	
	@Column(name="large_scroll_bars")
	public boolean isLargeScrollBars() {
		return largeScrollBars;
	}
	public void setLargeScrollBars(boolean largeScrollBars) {
		this.largeScrollBars = largeScrollBars;
	}

	@Column(name="sighting_accuracy")
	public Double getSightingAccuracy() {
		return sightingAccuracy;
	}
	public void setSightingAccuracy(Double sightingAccuracy) {
		this.sightingAccuracy = sightingAccuracy;
	}
	
	@Column(name="sighting_fix_count")
	public Integer getSightingFixCount() {
		return sightingFixCount;
	}
	public void setSightingFixCount(Integer sightingFixCount) {
		this.sightingFixCount = sightingFixCount;
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

	@Column(name="skip_button_timeout")
	public Integer getSkipButtonTimeout() {
		return skipButtonTimeout;
	}
	public void setSkipButtonTimeout(Integer skipButtonTimeout) {
		this.skipButtonTimeout = skipButtonTimeout;
	}
	
	@Column(name="application_name")
	public String getApplicationName() {
		return applicationName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	
	@Column(name="auto_next")
	public boolean isAutoNext() {
		return autoNext;
	}
	
	public void setAutoNext(boolean autoNext) {
		this.autoNext = autoNext;
	}
	
	@Column(name="storage_time")
	public int getStorageTime() {
		return storageTime;
	}
	
	public void setStorageTime(int storageTime) {
		this.storageTime = storageTime;
	}

	@Column(name="exit_pin")
	public int getExitPin() {
		return exitPin;
	}
	public void setExitPin(int exitPin) {
		this.exitPin = exitPin;
	}

}
