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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption.ProfileOptionID;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Class responsible for representing CyberTracker Properties
 * 
 * @author elitvin
 * @since 4.0.0
 */
@Entity
@Table(name = "ct_properties_profile", schema="smart")
public class CyberTrackerPropertiesProfile extends NamedItem {

	private static final long serialVersionUID = 1L;
	
	public static final int EXIT_PIN_MIN_VALUE = 1;
	public static final int EXIT_PIN_MAX_VALUE = 99999999;

	public static final int SIGHTING_FIX_COUNT_MIN_VALUE = 1;
	public static final int SIGHTING_FIX_COUNT_MAX_VALUE = 60;

	public static final int TIME_TRACK_MIN_VALUE = 0;
	public static final int TIME_TRACK_MAX_VALUE = 10000;

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
		350,
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
	private static final boolean kioskMode = true;
	private static final boolean canPause = true;
	private static final int exitPin = 1234;
	private static final int sightingFixCount = 10;
	private static final int waypointTimer = 300; //aka Track Timer 
	private static final int skipButtonTimeout = 3;
    private static final boolean disableEditing = false;
	private static final boolean testTime = false;
	private static final boolean useGpsTime = true;
	private static final boolean manualGps = false;
	private static final boolean allowSkipManualGps = true;
	private static final boolean useMapOnSkip = true;
	private static final int projection = 0;
    private static final int maxPhotoCount = 10;
    
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
	private boolean getBooleanValue(ProfileOptionID optionId) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		return (option != null) ? option.getBooleanValue() : (boolean)getDefaultValue(optionId);
	}

	
	@Transient
	private double getDoubleValue(ProfileOptionID optionId) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		return (option != null) ? option.getDoubleValue() : (double)getDefaultValue(optionId);
	}

	
	@Transient
	private int getIntValue(ProfileOptionID optionId) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		return (option != null && option.getIntegerValue() != null) ? option.getIntegerValue() : (int)getDefaultValue(optionId);
	}
	
	
	@Transient
	private String getStringValue(ProfileOptionID optionId) {
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> map = getOptions();
		CyberTrackerPropertiesProfileOption option = map.get(optionId);
		return (option != null) ? option.getStringValue() : (String)getDefaultValue(optionId);
	}
		
	@Transient
	public boolean isKioskMode() {
		return getBooleanValue(ProfileOptionID.KIOSK_MODE);
	}
	public void setKioskMode(boolean kioskMode) {
		getOption(ProfileOptionID.KIOSK_MODE).setBooleanValue(kioskMode);
	}

	@Transient
	public CyberTrackerPropertiesProfileOption.Unit getUnits(){
		return CyberTrackerPropertiesProfileOption.Unit.valueOf(getStringValue(ProfileOptionID.UNITS));
	}
	
	public void setUnits(CyberTrackerPropertiesProfileOption.Unit unit){
		getOption(ProfileOptionID.UNITS).setStringValue(unit.name());
	}
	
	@Transient
	public CyberTrackerPropertiesProfileOption.Mode getHistoryMode() {
		return CyberTrackerPropertiesProfileOption.Mode.valueOf(getStringValue(ProfileOptionID.HISTORY_MODE).toUpperCase());
	}
	
	public void setHistoryMode(CyberTrackerPropertiesProfileOption.Mode mode) {
		getOption(ProfileOptionID.HISTORY_MODE).setStringValue(mode.name());
	}
	
	@Transient
	public CyberTrackerPropertiesProfileOption.Mode getArchiveMode() {
		return CyberTrackerPropertiesProfileOption.Mode.valueOf(getStringValue(ProfileOptionID.ARCHIVE_MODE).toUpperCase());
	}
	
	public void setArchiveMode(CyberTrackerPropertiesProfileOption.Mode mode) {
		getOption(ProfileOptionID.ARCHIVE_MODE).setStringValue(mode.name());
	}
	
	@Transient
	public boolean getUseIncidentGroupUi() {
		return getBooleanValue(ProfileOptionID.INCIDENT_GROUP_UI);
	}
	public void setUserIncidentGroupUi(boolean useGroupUi) {
		getOption(ProfileOptionID.INCIDENT_GROUP_UI).setBooleanValue(useGroupUi);
	}
	

	@Transient
	public boolean isCanPause() {
		return getBooleanValue(ProfileOptionID.CAN_PAUSE);
	}
	public void setCanPause(boolean canPause) {
		getOption(ProfileOptionID.CAN_PAUSE).setBooleanValue(canPause);
	}
	
	@Transient
	public int getSightingFixCount() {
		return getIntValue(ProfileOptionID.SIGHTING_FIX_COUNT);
	}
	public void setSightingFixCount(int sightingFixCount) {
		getOption(ProfileOptionID.SIGHTING_FIX_COUNT).setIntegerValue(sightingFixCount);
	}
	
	
	@Transient
	public int getWaypointTimerValue() {
		return getIntValue(ProfileOptionID.WAYPOINT_TIMER);
	}
	public void setWaypointTimerValue(int waypointTimer) {
		getOption(ProfileOptionID.WAYPOINT_TIMER).setIntegerValue(waypointTimer);
	}
	
	@Transient
	public CyberTrackerPropertiesProfileOption.TrackTimerOp getWaypointTimerType() {
		return CyberTrackerPropertiesProfileOption.TrackTimerOp.valueOf( getStringValue(ProfileOptionID.WAYPOINT_TIMER_TYPE));
	}
	public void setWaypointTimerType(CyberTrackerPropertiesProfileOption.TrackTimerOp tt) {
		getOption(ProfileOptionID.WAYPOINT_TIMER_TYPE).setStringValue(tt.name());
	}
	
	@Transient
	public int getSkipButtonTimeout() {
		return getIntValue(ProfileOptionID.SKIP_BUTTON_TIMEOUT);
	}
	public void setSkipButtonTimeout(int skipButtonTimeout) {
		getOption(ProfileOptionID.SKIP_BUTTON_TIMEOUT).setIntegerValue(skipButtonTimeout);
	}
	
	
	@Transient
	public int getExitPin() {
		return getIntValue(ProfileOptionID.EXIT_PIN);
	}
	public void setExitPin(int exitPin) {
		getOption(ProfileOptionID.EXIT_PIN).setIntegerValue(exitPin);
	}
	
	
	
	@Transient
	public boolean isDisableEditing() {
		return getBooleanValue(ProfileOptionID.DISABLE_EDITING);
	}
	public void setDisableEditing(boolean disableEditing) {
		getOption(ProfileOptionID.DISABLE_EDITING).setBooleanValue(disableEditing);
	}

	
	@Transient
	public boolean isTestTime() {
		return getBooleanValue(ProfileOptionID.TEST_TIME);
	}
	public void setTestTime(boolean testTime) {
		getOption(ProfileOptionID.TEST_TIME).setBooleanValue(testTime);
	}

	
	@Transient
	public boolean isUseGpsTime() {
		return getBooleanValue(ProfileOptionID.USE_GPS_TIME);
	}
	public void setUseGpsTime(boolean useGpsTime) {
		getOption(ProfileOptionID.USE_GPS_TIME).setBooleanValue(useGpsTime);
	}
	
	
	@Transient
	public boolean isManualGps() {
		return getBooleanValue(ProfileOptionID.MANUAL_GPS);
	}
	public void setManualGps(boolean manualGps) {
		getOption(ProfileOptionID.MANUAL_GPS).setBooleanValue(manualGps);
	}
	
	
	@Transient
	public boolean isAllowSkipManualGps() {
		return getBooleanValue(ProfileOptionID.ALLOW_SKIP_MANUAL_GPS);
	}
	public void setAllowSkipManualGps(boolean allowSkipManualGps) {
		getOption(ProfileOptionID.ALLOW_SKIP_MANUAL_GPS).setBooleanValue(allowSkipManualGps);
	}
		
	@Transient
	public boolean isUseMapOnSkip() {
		return getBooleanValue(ProfileOptionID.USE_MAP_ON_SKIP);
	}
	public void setUseMapOnSkip(boolean useMapOnSkip) {
		getOption(ProfileOptionID.USE_MAP_ON_SKIP).setBooleanValue(useMapOnSkip);
	}
	
//	@Transient
//	public int getProjection() {
//		return getIntValue(ProfileOptionID.PROJECTION);
//	}
//	public void setProjection(int prj) {
//		getOption(ProfileOptionID.PROJECTION).setIntegerValue(prj);
//	}

	@Transient
	public Projection getCaProjection(Session session, boolean getDefaultIfNotFound) {
		String suuid = getStringValue(ProfileOptionID.CA_PROJECTION_UUID);
		if (suuid != null) {
			UUID uuid = null;
			try {
				uuid = UuidUtils.stringToUuid(suuid);
			}catch (Exception ex) {
			}
			if (uuid != null) {
				Projection prj = session.get(Projection.class, uuid);
				if (prj != null) return prj;			
				//projection no longer exists
			}
		}
		if (!getDefaultIfNotFound) return null;
		
		//can't find projection; lets try to fins the default
		Projection prj = session.createQuery("FROM Projection WHERE conservationArea = :ca and isDefault = true", Projection.class) //$NON-NLS-1$
				.setParameter("ca",  getConservationArea()) //$NON-NLS-1$
				.uniqueResult();

		if (prj != null) return prj;
		
		//no default projection; return lat/lon
		prj = new Projection();
		prj.setParsedCoordinateReferenceSystem(GeometryUtils.SMART_CRS);
		prj.setDefinition(GeometryUtils.SMART_CRS.toWKT());
		
		return prj;
	}
	public void setCaProjection(Projection projection) {
		String suuid = null;
		if (projection != null) {
			suuid =  UuidUtils.uuidToString(projection.getUuid());
		}
		getOption(ProfileOptionID.CA_PROJECTION_UUID).setStringValue(suuid);
	}
	
	@Transient
	public int getMaxPhotoCount() {
		return getIntValue(ProfileOptionID.MAX_PHOTO_COUNT);
	}
	public void setMaxPhotoCount(int count) {
		getOption(ProfileOptionID.MAX_PHOTO_COUNT).setIntegerValue(count);
	}

	@Transient
	public boolean getResizePhoto() {
		return getBooleanValue(ProfileOptionID.RESIZE_IMAGE);
	}
	@Transient
	public Integer getImageWidth() {
		return getIntValue(ProfileOptionID.IMAGE_WIDTH);
	}
	@Transient
	public Integer getImageHeight() {
		return getIntValue(ProfileOptionID.IMAGE_HEIGHT);
	}

	
	@Transient
	public void setResizePhoto(boolean resize, int w, int h) {
		getOption(ProfileOptionID.RESIZE_IMAGE).setBooleanValue(resize);
		if (resize) {
			getOption(ProfileOptionID.IMAGE_WIDTH).setIntegerValue(w);
			getOption(ProfileOptionID.IMAGE_HEIGHT).setIntegerValue(h);
		}else {
			getOption(ProfileOptionID.IMAGE_WIDTH).setIntegerValue(null);
			getOption(ProfileOptionID.IMAGE_HEIGHT).setIntegerValue(null);
		}
	}
	
	public Object getDefaultValue(ProfileOptionID option) {
		switch(option) {
		case INCIDENT_GROUP_UI: return false;
		case UNITS: return CyberTrackerPropertiesProfileOption.Unit.METRIC.name();
		case ALLOW_SKIP_MANUAL_GPS: return allowSkipManualGps;
		case APP_NAME: return "SMART Mobile Application";  //$NON-NLS-1$
		case CAN_PAUSE: return canPause;
		case DISABLE_EDITING: return disableEditing;
		case EXIT_PIN: return exitPin;
		case KIOSK_MODE: return kioskMode;
		case MANUAL_GPS: return manualGps;
		case MAX_PHOTO_COUNT: return maxPhotoCount;
		case PROJECTION: return projection; //removed smart8
		case CA_PROJECTION_UUID: return null;
		case SIGHTING_FIX_COUNT: return sightingFixCount;
		case SKIP_BUTTON_TIMEOUT: return skipButtonTimeout;
		case TEST_TIME: return testTime;
		case USE_GPS_TIME: return useGpsTime;
		case USE_MAP_ON_SKIP: return useMapOnSkip;
		case WAYPOINT_TIMER: return waypointTimer;
		case WAYPOINT_TIMER_TYPE: return CyberTrackerPropertiesProfileOption.TrackTimerOp.TIME.name();
		case THEME_COLOR_1: return -1;
		case THEME_COLOR_2: return -1;
		case THEME_COLOR_3: return -1;
		case THEME_COLOR_4: return -1;
		case TRACK_COLOR: return -1;
		case IMAGE_HEIGHT: return 1200;
		case IMAGE_WIDTH: return 1600;
		case RESIZE_IMAGE: return false;
		case ARCHIVE_MODE: return CyberTrackerPropertiesProfileOption.Mode.DISABLED.name();
		case HISTORY_MODE: return CyberTrackerPropertiesProfileOption.Mode.ENABLED.name();
		}
		return null;
	}
	
	/**
	 * Gets the default track color 
	 * @return
	 */
	@Transient
	public Color getTrackColor() {
		Integer color = getIntValue(ProfileOptionID.TRACK_COLOR);
		if (color == -1) return null;
		return new Color(color);
	}
	
	/**
	 * Sets the default track color
	 * @param color
	 */
	public void setTrackColor(Color color) {
		if (color == null) {
			getOption(ProfileOptionID.TRACK_COLOR).setIntegerValue(null);
		}else {
			getOption(ProfileOptionID.TRACK_COLOR).setIntegerValue(color.getRGB());
		}
	}
	
	/**
	 * 
	 * @param index one based index: values 1-4 for color theme index
	 * @return null if not set; otherwise color
	 */
	@Transient
	public Color getThemeColor(ProfileOptionID option) {
		Integer color = getIntValue(option);
		if (color == -1) return null;
		return new Color(color);
	}
	
	@Transient
	/**
	 * Sets the theme color 
	 * @param index color index one-based (1-4)
	 * @param color new color or null to clear setting
	 */
	public void setThemeColor(ProfileOptionID option, Color color) {
		Integer value = null;
		if (color != null) {
			value = color.getRGB();
		}
		getOption(option).setIntegerValue(value);
	}
	
}
