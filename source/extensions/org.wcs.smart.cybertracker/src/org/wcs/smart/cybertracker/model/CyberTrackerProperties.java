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

import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.OptionID;

/**
 * Class responsible for representing CyberTracker Properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerProperties {

	public static final int STORAGE_TIME_MIN_VALUE = 0;
	public static final int STORAGE_TIME_MAX_VALUE = 365;
	public static final int STORAGE_TIME_DEFAULT_VALUE = 30;

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
	

	public enum ProjectionFormat {
		DEGREE_MIN_SEC(Messages.CyberTrackerProperties_ProjectonFormat_DegreeMinSec, 0),
		DECEMAL_DEGREE(Messages.CyberTrackerProperties_ProjectonFormat_DecemalDegree, 1),
		UTM(Messages.CyberTrackerProperties_ProjectonFormat_UTM, 2);
		private String guiName;
		private int id;
		ProjectionFormat(String guiName, int id) {
			this.guiName = guiName;
			this.id = id;
		}
		public String getGuiName() {
			return this.guiName;
		}
		public int getId() {
			return id;
		}
		public static Integer[] getIds() {
			ProjectionFormat[] values = ProjectionFormat.values();
			Integer[] ids = new Integer[values.length];
			for (int i = 0; i < values.length; i++) {
				ids[i] = values[i].getId();
			}
			return ids;
		}
	}

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
    
	private Map<OptionID, CyberTrackerPropertiesOption> options;
	
	public Map<OptionID, CyberTrackerPropertiesOption> getOptions() {
		if (options == null)
			options = new HashMap<OptionID, CyberTrackerPropertiesOption>();
		return options;
	}
	public void setCmAttributeOptions(Map<OptionID, CyberTrackerPropertiesOption> options) {
		this.options = options;
	}
	
	
	private CyberTrackerPropertiesOption getOption(OptionID optionId) {
		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
		CyberTrackerPropertiesOption option = map.get(optionId);
		if (option == null) {
			option = new CyberTrackerPropertiesOption();
			option.setOptionId(optionId);
			map.put(optionId, option);
		}
		return option;
	}

	
	private boolean getBooleanValue(OptionID optionId, boolean defaultValue) {
		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
		CyberTrackerPropertiesOption option = map.get(optionId);
		return (option != null) ? option.getBooleanValue() : defaultValue;
	}

	
	private double getDoubleValue(OptionID optionId, double defaultValue) {
		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
		CyberTrackerPropertiesOption option = map.get(optionId);
		return (option != null) ? option.getDoubleValue() : defaultValue;
	}

	
	private int getIntValue(OptionID optionId, int defaultValue) {
		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
		CyberTrackerPropertiesOption option = map.get(optionId);
		return (option != null) ? option.getIntegerValue() : defaultValue;
	}
	
	
	private String getStringValue(OptionID optionId, String defaultValue) {
		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
		CyberTrackerPropertiesOption option = map.get(optionId);
		return (option != null) ? option.getStringValue() : defaultValue;
	}
	
	
	
	public boolean isKioskMode() {
		return getBooleanValue(OptionID.KIOSK_MODE, kioskMode);
	}
	public void setKioskMode(boolean kioskMode) {
		getOption(OptionID.KIOSK_MODE).setBooleanValue(kioskMode);
	}

	public boolean isSimpleCamera() {
		return getBooleanValue(OptionID.SIMPLE_CAMERA, simpleCamera);
	}
	public void setSimpleCamera(boolean simpleCamera) {
		getOption(OptionID.SIMPLE_CAMERA).setBooleanValue(simpleCamera);
	}
	
	public boolean isCanPause() {
		return getBooleanValue(OptionID.CAN_PAUSE, canPause);
	}
	public void setCanPause(boolean canPause) {
		getOption(OptionID.CAN_PAUSE).setBooleanValue(canPause);
	}
	
	public boolean isLargeScrollBars() {
		return getBooleanValue(OptionID.LARGE_SCROLL_BARS, largeScrollBars);
	}
	public void setLargeScrollBars(boolean largeScrollBars) {
		getOption(OptionID.LARGE_SCROLL_BARS).setBooleanValue(largeScrollBars);
	}

	
	public double getSightingAccuracy() {
		return getDoubleValue(OptionID.SIGHTING_ACCURACY, sightingAccuracy);
	}
	public void setSightingAccuracy(double sightingAccuracy) {
		getOption(OptionID.SIGHTING_ACCURACY).setDoubleValue(sightingAccuracy);
	}
	
	
	public int getSightingFixCount() {
		return getIntValue(OptionID.SIGHTING_FIX_COUNT, sightingFixCount);
	}
	public void setSightingFixCount(int sightingFixCount) {
		getOption(OptionID.SIGHTING_FIX_COUNT).setIntegerValue(sightingFixCount);
	}
	
	
	public int getWaypointTimer() {
		return getIntValue(OptionID.WAYPOINT_TIMER, waypointTimer);
	}
	public void setWaypointTimer(int waypointTimer) {
		getOption(OptionID.WAYPOINT_TIMER).setIntegerValue(waypointTimer);
	}
	
	
	public int getGpsTimeZone() {
		return getIntValue(OptionID.GPS_TIME_ZONE, gpsTimeZone);
	}
	public void setGpsTimeZone(int gpsTimeZone) {
		getOption(OptionID.GPS_TIME_ZONE).setIntegerValue(gpsTimeZone);
	}

	
	public int getSkipButtonTimeout() {
		return getIntValue(OptionID.SKIP_BUTTON_TIMEOUT, skipButtonTimeout);
	}
	public void setSkipButtonTimeout(int skipButtonTimeout) {
		getOption(OptionID.SKIP_BUTTON_TIMEOUT).setIntegerValue(skipButtonTimeout);
	}
	
	
	public boolean isAutoNext() {
		return getBooleanValue(OptionID.AUTO_NEXT, autoNext);
	}
	public void setAutoNext(boolean autoNext) {
		getOption(OptionID.AUTO_NEXT).setBooleanValue(autoNext);
	}
	
	
	public int getStorageTime() {
		return getIntValue(OptionID.STORAGE_TIME, STORAGE_TIME_DEFAULT_VALUE);
	}
	public void setStorageTime(int storageTime) {
		getOption(OptionID.STORAGE_TIME).setIntegerValue(storageTime);
	}

	
	public int getExitPin() {
		return getIntValue(OptionID.EXIT_PIN, exitPin);
	}
	public void setExitPin(int exitPin) {
		getOption(OptionID.EXIT_PIN).setIntegerValue(exitPin);
	}
	
	
	public boolean isUseTitleBar() {
		return getBooleanValue(OptionID.USE_TITLE_BAR, useTitleBar);
	}
	public void setUseTitleBar(boolean useTitleBar) {
		getOption(OptionID.USE_TITLE_BAR).setBooleanValue(useTitleBar);
	}
	
	
	public boolean isUseLargeTabs() {
		return getBooleanValue(OptionID.USE_LARGE_TABS, useLargeTabs);
	}
	public void setUseLargeTabs(boolean useLargeTabs) {
		getOption(OptionID.USE_LARGE_TABS).setBooleanValue(useLargeTabs);
	}
	
	
	public boolean isUseLargeTitles() {
		return getBooleanValue(OptionID.USE_LARGE_TITLES, useLargeTitles);
	}
	public void setUseLargeTitles(boolean useLargeTitles) {
		getOption(OptionID.USE_LARGE_TITLES).setBooleanValue(useLargeTitles);
	}
	
	
	public boolean isDisableEditing() {
		return getBooleanValue(OptionID.DISABLE_EDITING, disableEditing);
	}
	public void setDisableEditing(boolean disableEditing) {
		getOption(OptionID.DISABLE_EDITING).setBooleanValue(disableEditing);
	}
	
	
	public boolean isUseSdCard() {
		return getBooleanValue(OptionID.USE_SD_CARD, useSdCard);
	}
	public void setUseSdCard(boolean useSdCard) {
		getOption(OptionID.USE_SD_CARD).setBooleanValue(useSdCard);
	}
	
	
	public boolean isTestTime() {
		return getBooleanValue(OptionID.TEST_TIME, testTime);
	}
	public void setTestTime(boolean testTime) {
		getOption(OptionID.TEST_TIME).setBooleanValue(testTime);
	}
	
	
	public boolean isResetOnSync() {
		return getBooleanValue(OptionID.RESET_ON_SYNC, resetOnSync);
	}
	public void setResetOnSync(boolean resetOnSync) {
		getOption(OptionID.RESET_ON_SYNC).setBooleanValue(resetOnSync);
	}
	
	
	public boolean isResetOnNext() {
		return getBooleanValue(OptionID.RESET_ON_NEXT, resetOnNext);
	}
	public void setResetOnNext(boolean resetOnNext) {
		getOption(OptionID.RESET_ON_NEXT).setBooleanValue(resetOnNext);
	}
	
	
	public double getTrackAccuracy() {
		return getDoubleValue(OptionID.TRACK_ACCURACY, trackAccuracy);
	}
	public void setTrackAccuracy(double trackAccuracy) {
		getOption(OptionID.TRACK_ACCURACY).setDoubleValue(trackAccuracy);
	}
	
	
	public boolean isUseGpsTime() {
		return getBooleanValue(OptionID.USE_GPS_TIME, useGpsTime);
	}
	public void setUseGpsTime(boolean useGpsTime) {
		getOption(OptionID.USE_GPS_TIME).setBooleanValue(useGpsTime);
	}
	
	
	public boolean isManualGps() {
		return getBooleanValue(OptionID.MANUAL_GPS, manualGps);
	}
	public void setManualGps(boolean manualGps) {
		getOption(OptionID.MANUAL_GPS).setBooleanValue(manualGps);
	}
	
	
	public boolean isAllowSkipManualGps() {
		return getBooleanValue(OptionID.ALLOW_SKIP_MANUAL_GPS, allowSkipManualGps);
	}
	public void setAllowSkipManualGps(boolean allowSkipManualGps) {
		getOption(OptionID.ALLOW_SKIP_MANUAL_GPS).setBooleanValue(allowSkipManualGps);
	}
	
	
	public String getFieldMapFilename() {
		return getStringValue(OptionID.FIELD_MAP_FILENAME, fieldMapFilename);
	}
	public void setFieldMapFilename(String fieldMapFilename) {
		getOption(OptionID.FIELD_MAP_FILENAME).setStringValue(fieldMapFilename);
	}
	
	
	public boolean isLock100() {
		return getBooleanValue(OptionID.LOCK100, lock100);
	}
	public void setLock100(boolean lock100) {
		getOption(OptionID.LOCK100).setBooleanValue(lock100);
	}
	
	
	public boolean isUseMapOnSkip() {
		return getBooleanValue(OptionID.USE_MAP_ON_SKIP, useMapOnSkip);
	}
	public void setUseMapOnSkip(boolean useMapOnSkip) {
		getOption(OptionID.USE_MAP_ON_SKIP).setBooleanValue(useMapOnSkip);
	}
	
	public boolean isShowEdit() {
		return getBooleanValue(OptionID.SHOW_EDIT, showEdit);
	}
	public void setShowEdit(boolean showEdit) {
		getOption(OptionID.SHOW_EDIT).setBooleanValue(showEdit);
	}

	public boolean isShowGPS() {
		return getBooleanValue(OptionID.SHOW_GPS, showGPS);
	}
	public void setShowGPS(boolean showGPS) {
		getOption(OptionID.SHOW_GPS).setBooleanValue(showGPS);
	}

	public int getProjection() {
		return getIntValue(OptionID.PROJECTION, projection);
	}
	public void setProjection(int prj) {
		getOption(OptionID.PROJECTION).setIntegerValue(prj);
	}

	public int getUtmZone() {
		return getIntValue(OptionID.UTM_ZONE, utmZone);
	}
	public void setUtmZone(int zone) {
		getOption(OptionID.UTM_ZONE).setIntegerValue(zone);
	}

	public int getMaxPhotoCount() {
		return getIntValue(OptionID.MAX_PHOTO_COUNT, maxPhotoCount);
	}
	public void setMaxPhotoCount(int count) {
		getOption(OptionID.MAX_PHOTO_COUNT).setIntegerValue(count);
	}

	public int getDilutionOfPrecision() {
		return getIntValue(OptionID.DILUTION_OF_PRECISION, dilutionOfPrecision);
	}
	public void setDilutionOfPrecision(int dop) {
		getOption(OptionID.DILUTION_OF_PRECISION).setIntegerValue(dop);
	}
}
