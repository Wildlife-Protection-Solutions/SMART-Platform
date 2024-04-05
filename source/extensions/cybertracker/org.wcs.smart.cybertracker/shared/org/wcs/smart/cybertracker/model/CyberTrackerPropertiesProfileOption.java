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

import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Class responsible for representing specific option
 * among CyberTracker Properties that applies to specific properties profile
 * 
 * @author elitvin
 * @since 4.0.0
 */
@Entity
@Table(name = "ct_properties_profile_option", schema="smart")
public class CyberTrackerPropertiesProfileOption extends UuidItem {

	private static final long serialVersionUID = 1L;
	
	public enum Unit{
		METRIC,
		IMPERIAL;
		
	}
	public enum ProfileOptionID {
		APP_NAME,
		KIOSK_MODE,
		EXIT_PIN,		
		SIGHTING_FIX_COUNT,
		WAYPOINT_TIMER,
		WAYPOINT_TIMER_TYPE,
		SKIP_BUTTON_TIMEOUT,
		PROJECTION, //support removed SMART8
		CA_PROJECTION_UUID, //added in SMART8 - link to uuid of ca projection (may no exist if projection remoted)
		DISABLE_EDITING,
		TEST_TIME,
		USE_GPS_TIME,
		MANUAL_GPS,
		ALLOW_SKIP_MANUAL_GPS,
		USE_MAP_ON_SKIP,
		CAN_PAUSE,
		MAX_PHOTO_COUNT,
		TRACK_COLOR,
		THEME_COLOR_1, //primary
		THEME_COLOR_2, //accent
		THEME_COLOR_3, //foreground
		THEME_COLOR_4, //background
		RESIZE_IMAGE,
		IMAGE_WIDTH,
		IMAGE_HEIGHT,
		INCIDENT_GROUP_UI,
		UNITS;
		
		public String getMobleJsonKey() {
			if (this == ProfileOptionID.CA_PROJECTION_UUID) return "PROJECTION_WKT";
			return this.name();
		}
	}

	public enum TrackTimerOp{
		TIME,
		DISTANCE
	}
	
	private CyberTrackerPropertiesProfile profile;
	private ProfileOptionID optionId;
	private Double doubleValue;
	private Integer integerValue;
	private String stringValue;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="profile_uuid", referencedColumnName="uuid")
	public CyberTrackerPropertiesProfile getProfile() {
		return profile;
	}
	public void setProfile(CyberTrackerPropertiesProfile profile) {
		this.profile = profile;
	}

	@Column(name="option_id")
	@Enumerated(EnumType.STRING)
	public ProfileOptionID getOptionId() {
		return optionId;
	}
	public void setOptionId(ProfileOptionID optionId) {
		this.optionId = optionId;
	}
	
	
	@Column(name="double_value")
	public Double getDoubleValue() {
		return doubleValue;
	}
	public void setDoubleValue(Double doubleValue) {
		this.doubleValue = doubleValue;
	}

	@Column(name="integer_value")
	public Integer getIntegerValue() {
		return integerValue;
	}
	public void setIntegerValue(Integer integerValue) {
		this.integerValue = integerValue;
	}

	@Column(name="string_value")
	public String getStringValue() {
		return stringValue;
	}
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	
	@Transient
	public Boolean getBooleanValue() {
		if (integerValue == null)
			return null;
		return integerValue != 0;
	}
	@Transient
	public void setBooleanValue(boolean value) {
		integerValue = value ? 1 : 0;
	}
}
