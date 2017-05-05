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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption.ProfileOptionID;

/**
 * Class responsible for representing CyberTracker Properties
 * 
 * @author elitvin
 * @since 4.0.0
 */
@Entity
@Table(name = "smart.ct_properties_profile")
public class CyberTrackerPropertiesProfile extends NamedItem {

	public static final int EXIT_PIN_MIN_VALUE = 1;
	public static final int EXIT_PIN_MAX_VALUE = 99999999;
	
	public static final double SIGHTING_ACCURACY_MIN_VALUE = 0.01;
	public static final double SIGHTING_ACCURACY_MAX_VALUE = 49;
	
	public static final double TRACK_ACCURACY_MIN_VALUE = 0.01;
	public static final double TRACK_ACCURACY_MAX_VALUE = 49;

	public static final int SIGHTING_FIX_COUNT_MIN_VALUE = 1;
	public static final int SIGHTING_FIX_COUNT_MAX_VALUE = 60;

	public static final int TIME_TRACK_MIN_VALUE = 0;
	public static final int TIME_TRACK_MAX_VALUE = 1000;

	public static final int UTM_ZONE_MIN_VALUE = 0;
	public static final int UTM_ZONE_MAX_VALUE = 60;

	public static final int MAX_PHOTO_COUNT_MIN_VALUE = 1;
	public static final int MAX_PHOTO_COUNT_MAX_VALUE = 100;
	
	public static final Integer[] GMT_VALUES = {
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
	
	public static final int DILUTION_OF_PRECISION_MIN_VALUE = 0;
	public static final int DILUTION_OF_PRECISION_MAX_VALUE = 49;

	//default properties
	private static final boolean largeScrollBars = false;
	private static final boolean kioskMode = true;
	private static final boolean simpleCamera = true;
	private static final boolean canPause = true;
	private static final int exitPin = 1234;
	
	private static final double sightingAccuracy = 20.0;
	private static final int sightingFixCount = 10;
	private static final int waypointTimer = 300; //aka Track Timer 
	private static final int gpsTimeZone = 0; //GMT/UTC time offset
    private static final int skipButtonTimeout = 3;
    
	private static final boolean useTitleBar = false;
	private static final boolean useLargeTitles = true;
	private static final boolean useLargeTabs = false;
	
	private static final boolean disableEditing = false;
	private static final boolean useSdCard = false;
	private static final boolean testTime = false;
	private static final boolean resetOnSync = false;
	private static final boolean resetOnNext = true;
	
	private static final double trackAccuracy = 20.0;
	
	private static final boolean useGpsTime = true;
	private static final boolean manualGps = false;
	private static final boolean allowSkipManualGps = true;
	
	private static final String fieldMapFilename = ""; //$NON-NLS-1$
	private static final boolean lock100 = false;
	private static final boolean useMapOnSkip = true;
	
	private static final boolean autoNext = false;
	private static final boolean showEdit = true;
	private static final boolean showGPS = true;
	
	private static final int projection = 0;
    private static final int utmZone = 0;

    private static final int maxPhotoCount = 10;
    
    private static final int dilutionOfPrecision = DILUTION_OF_PRECISION_MAX_VALUE;
    
	private ConservationArea conservationArea;
	private boolean isDefault = false;
	private Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> options;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="profile", cascade = {CascadeType.ALL}, orphanRemoval = true)
	@MapKey(name = "optionId")
	public Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> getOptions() {
		if (options == null)
			options = new HashMap<ProfileOptionID, CyberTrackerPropertiesProfileOption>();
		return options;
	}
	public void setOptions(Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> options) {
		this.options = options;
	}
	
	@Column(name = "is_default")
	public boolean isDefault() {
		return isDefault;
	}
	public void setDefault(Boolean isDefault) {
		this.isDefault = Boolean.TRUE.equals(isDefault); //null <==> false
	}
	
	@Transient
	private CyberTrackerPropertiesProfileOption getOption(ProfileOptionID optionId) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		if (option == null) {
			option = new CyberTrackerPropertiesProfileOption();
			option.setProfile(this);
			option.setOptionId(optionId);
			map.put(optionId, option);
		}
		return option;
	}

	@Transient
	private boolean getBooleanValue(ProfileOptionID optionId, boolean defaultValue) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		return (option != null) ? option.getBooleanValue() : defaultValue;
	}

	
	@Transient
	private double getDoubleValue(ProfileOptionID optionId, double defaultValue) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		return (option != null) ? option.getDoubleValue() : defaultValue;
	}

	
	@Transient
	private int getIntValue(ProfileOptionID optionId, int defaultValue) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		return (option != null) ? option.getIntegerValue() : defaultValue;
	}
	
	
	@Transient
	private String getStringValue(ProfileOptionID optionId, String defaultValue) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		return (option != null) ? option.getStringValue() : defaultValue;
	}
	
	
	
	@Transient
	public boolean isKioskMode() {
		return getBooleanValue(ProfileOptionID.KIOSK_MODE, kioskMode);
	}
	public void setKioskMode(boolean kioskMode) {
		getOption(ProfileOptionID.KIOSK_MODE).setBooleanValue(kioskMode);
	}

	@Transient
	public boolean isSimpleCamera() {
		return getBooleanValue(ProfileOptionID.SIMPLE_CAMERA, simpleCamera);
	}
	public void setSimpleCamera(boolean simpleCamera) {
		getOption(ProfileOptionID.SIMPLE_CAMERA).setBooleanValue(simpleCamera);
	}
	
	@Transient
	public boolean isCanPause() {
		return getBooleanValue(ProfileOptionID.CAN_PAUSE, canPause);
	}
	public void setCanPause(boolean canPause) {
		getOption(ProfileOptionID.CAN_PAUSE).setBooleanValue(canPause);
	}
	
	@Transient
	public boolean isLargeScrollBars() {
		return getBooleanValue(ProfileOptionID.LARGE_SCROLL_BARS, largeScrollBars);
	}
	public void setLargeScrollBars(boolean largeScrollBars) {
		getOption(ProfileOptionID.LARGE_SCROLL_BARS).setBooleanValue(largeScrollBars);
	}

	
	@Transient
	public double getSightingAccuracy() {
		return getDoubleValue(ProfileOptionID.SIGHTING_ACCURACY, sightingAccuracy);
	}
	public void setSightingAccuracy(double sightingAccuracy) {
		getOption(ProfileOptionID.SIGHTING_ACCURACY).setDoubleValue(sightingAccuracy);
	}
	
	
	@Transient
	public int getSightingFixCount() {
		return getIntValue(ProfileOptionID.SIGHTING_FIX_COUNT, sightingFixCount);
	}
	public void setSightingFixCount(int sightingFixCount) {
		getOption(ProfileOptionID.SIGHTING_FIX_COUNT).setIntegerValue(sightingFixCount);
	}
	
	
	@Transient
	public int getWaypointTimer() {
		return getIntValue(ProfileOptionID.WAYPOINT_TIMER, waypointTimer);
	}
	public void setWaypointTimer(int waypointTimer) {
		getOption(ProfileOptionID.WAYPOINT_TIMER).setIntegerValue(waypointTimer);
	}
	
	
	@Transient
	public int getGpsTimeZone() {
		return getIntValue(ProfileOptionID.GPS_TIME_ZONE, gpsTimeZone);
	}
	public void setGpsTimeZone(int gpsTimeZone) {
		getOption(ProfileOptionID.GPS_TIME_ZONE).setIntegerValue(gpsTimeZone);
	}

	
	@Transient
	public int getSkipButtonTimeout() {
		return getIntValue(ProfileOptionID.SKIP_BUTTON_TIMEOUT, skipButtonTimeout);
	}
	public void setSkipButtonTimeout(int skipButtonTimeout) {
		getOption(ProfileOptionID.SKIP_BUTTON_TIMEOUT).setIntegerValue(skipButtonTimeout);
	}
	
	
	@Transient
	public boolean isAutoNext() {
		return getBooleanValue(ProfileOptionID.AUTO_NEXT, autoNext);
	}
	public void setAutoNext(boolean autoNext) {
		getOption(ProfileOptionID.AUTO_NEXT).setBooleanValue(autoNext);
	}

	
	@Transient
	public int getExitPin() {
		return getIntValue(ProfileOptionID.EXIT_PIN, exitPin);
	}
	public void setExitPin(int exitPin) {
		getOption(ProfileOptionID.EXIT_PIN).setIntegerValue(exitPin);
	}
	
	
	@Transient
	public boolean isUseTitleBar() {
		return getBooleanValue(ProfileOptionID.USE_TITLE_BAR, useTitleBar);
	}
	public void setUseTitleBar(boolean useTitleBar) {
		getOption(ProfileOptionID.USE_TITLE_BAR).setBooleanValue(useTitleBar);
	}
	
	
	@Transient
	public boolean isUseLargeTabs() {
		return getBooleanValue(ProfileOptionID.USE_LARGE_TABS, useLargeTabs);
	}
	public void setUseLargeTabs(boolean useLargeTabs) {
		getOption(ProfileOptionID.USE_LARGE_TABS).setBooleanValue(useLargeTabs);
	}
	
	
	@Transient
	public boolean isUseLargeTitles() {
		return getBooleanValue(ProfileOptionID.USE_LARGE_TITLES, useLargeTitles);
	}
	public void setUseLargeTitles(boolean useLargeTitles) {
		getOption(ProfileOptionID.USE_LARGE_TITLES).setBooleanValue(useLargeTitles);
	}
	
	
	@Transient
	public boolean isDisableEditing() {
		return getBooleanValue(ProfileOptionID.DISABLE_EDITING, disableEditing);
	}
	public void setDisableEditing(boolean disableEditing) {
		getOption(ProfileOptionID.DISABLE_EDITING).setBooleanValue(disableEditing);
	}
	
	
	@Transient
	public boolean isUseSdCard() {
		return getBooleanValue(ProfileOptionID.USE_SD_CARD, useSdCard);
	}
	public void setUseSdCard(boolean useSdCard) {
		getOption(ProfileOptionID.USE_SD_CARD).setBooleanValue(useSdCard);
	}
	
	
	@Transient
	public boolean isTestTime() {
		return getBooleanValue(ProfileOptionID.TEST_TIME, testTime);
	}
	public void setTestTime(boolean testTime) {
		getOption(ProfileOptionID.TEST_TIME).setBooleanValue(testTime);
	}
	
	
	@Transient
	public boolean isResetOnSync() {
		return getBooleanValue(ProfileOptionID.RESET_ON_SYNC, resetOnSync);
	}
	public void setResetOnSync(boolean resetOnSync) {
		getOption(ProfileOptionID.RESET_ON_SYNC).setBooleanValue(resetOnSync);
	}
	
	
	@Transient
	public boolean isResetOnNext() {
		return getBooleanValue(ProfileOptionID.RESET_ON_NEXT, resetOnNext);
	}
	public void setResetOnNext(boolean resetOnNext) {
		getOption(ProfileOptionID.RESET_ON_NEXT).setBooleanValue(resetOnNext);
	}
	
	
	@Transient
	public double getTrackAccuracy() {
		return getDoubleValue(ProfileOptionID.TRACK_ACCURACY, trackAccuracy);
	}
	public void setTrackAccuracy(double trackAccuracy) {
		getOption(ProfileOptionID.TRACK_ACCURACY).setDoubleValue(trackAccuracy);
	}
	
	
	@Transient
	public boolean isUseGpsTime() {
		return getBooleanValue(ProfileOptionID.USE_GPS_TIME, useGpsTime);
	}
	public void setUseGpsTime(boolean useGpsTime) {
		getOption(ProfileOptionID.USE_GPS_TIME).setBooleanValue(useGpsTime);
	}
	
	
	@Transient
	public boolean isManualGps() {
		return getBooleanValue(ProfileOptionID.MANUAL_GPS, manualGps);
	}
	public void setManualGps(boolean manualGps) {
		getOption(ProfileOptionID.MANUAL_GPS).setBooleanValue(manualGps);
	}
	
	
	@Transient
	public boolean isAllowSkipManualGps() {
		return getBooleanValue(ProfileOptionID.ALLOW_SKIP_MANUAL_GPS, allowSkipManualGps);
	}
	public void setAllowSkipManualGps(boolean allowSkipManualGps) {
		getOption(ProfileOptionID.ALLOW_SKIP_MANUAL_GPS).setBooleanValue(allowSkipManualGps);
	}
	
	
	@Transient
	public String getFieldMapFilename() {
		return getStringValue(ProfileOptionID.FIELD_MAP_FILENAME, fieldMapFilename);
	}
	public void setFieldMapFilename(String fieldMapFilename) {
		getOption(ProfileOptionID.FIELD_MAP_FILENAME).setStringValue(fieldMapFilename);
	}
	
	
	@Transient
	public boolean isLock100() {
		return getBooleanValue(ProfileOptionID.LOCK100, lock100);
	}
	public void setLock100(boolean lock100) {
		getOption(ProfileOptionID.LOCK100).setBooleanValue(lock100);
	}
	
	
	@Transient
	public boolean isUseMapOnSkip() {
		return getBooleanValue(ProfileOptionID.USE_MAP_ON_SKIP, useMapOnSkip);
	}
	public void setUseMapOnSkip(boolean useMapOnSkip) {
		getOption(ProfileOptionID.USE_MAP_ON_SKIP).setBooleanValue(useMapOnSkip);
	}
	
	@Transient
	public boolean isShowEdit() {
		return getBooleanValue(ProfileOptionID.SHOW_EDIT, showEdit);
	}
	public void setShowEdit(boolean showEdit) {
		getOption(ProfileOptionID.SHOW_EDIT).setBooleanValue(showEdit);
	}

	@Transient
	public boolean isShowGPS() {
		return getBooleanValue(ProfileOptionID.SHOW_GPS, showGPS);
	}
	public void setShowGPS(boolean showGPS) {
		getOption(ProfileOptionID.SHOW_GPS).setBooleanValue(showGPS);
	}

	@Transient
	public int getProjection() {
		return getIntValue(ProfileOptionID.PROJECTION, projection);
	}
	public void setProjection(int prj) {
		getOption(ProfileOptionID.PROJECTION).setIntegerValue(prj);
	}

	@Transient
	public int getUtmZone() {
		return getIntValue(ProfileOptionID.UTM_ZONE, utmZone);
	}
	public void setUtmZone(int zone) {
		getOption(ProfileOptionID.UTM_ZONE).setIntegerValue(zone);
	}

	@Transient
	public int getMaxPhotoCount() {
		return getIntValue(ProfileOptionID.MAX_PHOTO_COUNT, maxPhotoCount);
	}
	public void setMaxPhotoCount(int count) {
		getOption(ProfileOptionID.MAX_PHOTO_COUNT).setIntegerValue(count);
	}

	@Transient
	public int getDilutionOfPrecision() {
		return getIntValue(ProfileOptionID.DILUTION_OF_PRECISION, dilutionOfPrecision);
	}
	public void setDilutionOfPrecision(int dop) {
		getOption(ProfileOptionID.DILUTION_OF_PRECISION).setIntegerValue(dop);
	}
}
