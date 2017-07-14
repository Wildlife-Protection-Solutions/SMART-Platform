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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;

/**
 * Class responsible for representing specific option
 * among CyberTracker Properties that applies to specific properties profile
 * 
 * @author elitvin
 * @since 4.0.0
 */
@Entity
@Table(name = "smart.ct_properties_profile_option")
public class CyberTrackerPropertiesProfileOption extends UuidItem {

	public enum ProfileOptionID {
		APP_NAME,
		LARGE_SCROLL_BARS,
		KIOSK_MODE,
		SIMPLE_CAMERA,
		EXIT_PIN,
		
		SIGHTING_ACCURACY,
		SIGHTING_FIX_COUNT,
		WAYPOINT_TIMER,
		GPS_TIME_ZONE,
		SKIP_BUTTON_TIMEOUT,

		PROJECTION,
		UTM_ZONE,
		
		USE_TITLE_BAR,
		USE_LARGE_TITLES,
		USE_LARGE_TABS,
		
		DISABLE_EDITING,
		USE_SD_CARD,
		TEST_TIME,
		RESET_ON_SYNC,
		RESET_ON_NEXT,
		
		TRACK_ACCURACY,
		
		USE_GPS_TIME,
		MANUAL_GPS,
		ALLOW_SKIP_MANUAL_GPS,
		
		FIELD_MAP_FILENAME,
		LOCK100,
		USE_MAP_ON_SKIP,
		
		AUTO_NEXT,
		CAN_PAUSE,
		SHOW_EDIT,
		SHOW_GPS,
		MAX_PHOTO_COUNT,
		DILUTION_OF_PRECISION;
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
