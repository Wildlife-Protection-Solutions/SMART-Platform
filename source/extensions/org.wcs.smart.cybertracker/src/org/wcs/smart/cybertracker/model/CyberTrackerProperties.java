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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

/**
 * Class responsible for representing CyberTracker Properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.cybertracker_properties")
public class CyberTrackerProperties extends UuidItem {

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
	
	public static final double TRACK_ACCURACY_MIN_VALUE = 1;
	public static final double TRACK_ACCURACY_MAX_VALUE = 49;

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
	
	private ConservationArea conservationArea;
	
	//default properties
	private String  applicationName = "SMART"; //$NON-NLS-1$
	
	private boolean largeScrollBars = false;
	private boolean kioskMode = false;
	private int exitPin = 1234;
	
	private Double sightingAccuracy = 49.0;
	private Integer sightingFixCount = 1;
	private Integer waypointTimer = 0; //aka Track Timer 
	private Integer gpsTimeZone = 0; //GMT/UTC time offset
    private Integer skipButtonTimeout = 3;
    
	private boolean useTitleBar = false;
	private boolean useLargeTitles = true;
	private boolean useLargeTabs = false;
	
	private boolean disableEditing = false;
	private boolean useSdCard = false;
	private boolean testTime = true;
	private boolean resetOnSync = false;
	private boolean resetOnNext = true;
	
	private int trackAccuracy = 49;
	
	private boolean useGpsTime = false;
	private boolean manualGps = false;
	private boolean allowSkipManualGps = true;
	
	private String fieldMapFilename = ""; //$NON-NLS-1$
	private boolean lock100 = false;
	private boolean useMapOnSkip = true;
	
   
	private boolean autoNext = true;
	
	private int storageTime = STORAGE_TIME_DEFAULT_VALUE; //indicates how many days ctx files will be stored in SMART storage

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
	
	@Column(name="use_title_bar")
	public boolean isUseTitleBar() {
		return useTitleBar;
	}
	public void setUseTitleBar(boolean useTitleBar) {
		this.useTitleBar = useTitleBar;
	}
	
	@Column(name="large_tabs")
	public boolean isUseLargeTabs() {
		return useLargeTabs;
	}
	public void setUseLargeTabs(boolean useLargeTabs) {
		this.useLargeTabs = useLargeTabs;
	}
	

	@Column(name="large_titles")
	public boolean isUseLargeTitles() {
		return useLargeTitles;
	}
	public void setUseLargeTitles(boolean useLargeTitles) {
		this.useLargeTitles = useLargeTitles;
	}
	
	@Column(name="disable_editing")
	public boolean isDisableEditing() {
		return disableEditing;
	}
	public void setDisableEditing(boolean disableEditing) {
		this.disableEditing = disableEditing;
	}
	
	@Column(name="sd_card")
	public boolean isUseSdCard() {
		return useSdCard;
	}
	public void setUseSdCard(boolean useSdCard) {
		this.useSdCard = useSdCard;
	}
	
	@Column(name="test_time")
	public boolean isTestTime() {
		return testTime;
	}
	public void setTestTime(boolean testTime) {
		this.testTime = testTime;
	}
	
	@Column(name="reset_on_sync")
	public boolean isResetOnSync() {
		return resetOnSync;
	}
	public void setResetOnSync(boolean resetOnSync) {
		this.resetOnSync = resetOnSync;
	}
	
	@Column(name="reset_on_next")
	public boolean isResetOnNext() {
		return resetOnNext;
	}
	public void setResetOnNext(boolean resetOnNext) {
		this.resetOnNext = resetOnNext;
	}
	
	@Column(name="track_accuracy")
	public int getTrackAccuracy() {
		return trackAccuracy;
	}
	public void setTrackAccuracy(int trackAccuracy) {
		this.trackAccuracy = trackAccuracy;
	}
	
	@Column(name="use_gps_time")
	public boolean isUseGpsTime() {
		return useGpsTime;
	}
	public void setUseGpsTime(boolean useGpsTime) {
		this.useGpsTime = useGpsTime;
	}
	
	@Column(name="manual_gps")
	public boolean isManualGps() {
		return manualGps;
	}
	public void setManualGps(boolean manualGps) {
		this.manualGps = manualGps;
	}
	
	@Column(name="allow_skip_manual")
	public boolean isAllowSkipManualGps() {
		return allowSkipManualGps;
	}
	public void setAllowSkipManualGps(boolean allowSkipManualGps) {
		this.allowSkipManualGps = allowSkipManualGps;
	}
	
	@Column(name="field_map_filename")
	public String getFieldMapFilename() {
		return fieldMapFilename;
	}
	public void setFieldMapFilename(String fieldMapFilename) {
		this.fieldMapFilename = fieldMapFilename;
	}
	
	@Column(name="lock_100")
	public boolean isLock100() {
		return lock100;
	}
	public void setLock100(boolean lock100) {
		this.lock100 = lock100;
	}
	
	@Column(name="use_map_on_skip")
	public boolean isUseMapOnSkip() {
		return useMapOnSkip;
	}
	public void setUseMapOnSkip(boolean useMapOnSkip) {
		this.useMapOnSkip = useMapOnSkip;
	}
	
}
