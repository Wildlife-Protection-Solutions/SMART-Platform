package org.wcs.smart.connect.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


@Entity
@Table(name = "connect.alert_filter_defaults")
public class AlertFilterDefault extends ConnectUuidItem{

	private int defaultPastHours; 
	private String defaultTypeUuids; 
	private boolean defaultActive;
	private boolean defaultDisabled; 
	private boolean defaultLevel1;
	private boolean defaultLevel2;
	private boolean defaultLevel3;
	private boolean defaultLevel4;
	private boolean defaultLevel5;
	private String defaultCaUuids;
	private String defaultText;
	private int secondsRefresh;
	private int startingZoomLevel;
	private float startingLat;
	private float startingLong;
	
	
	@Column(name="default_past_hours")
	public int getDefaultPastHours() {
		return defaultPastHours;
	}
	public void setDefaultPastHours(int defaultPastHours) {
		this.defaultPastHours = defaultPastHours;
	}
	
	@Column(name="default_type_uuids")
	public String getDefaultTypeUuids() {
		return defaultTypeUuids;
	}
	public void setDefaultTypeUuids(String defaultTypeUuids) {
		this.defaultTypeUuids = defaultTypeUuids;
	}
	
	@Column(name="default_active")
	public boolean isDefaultActive() {
		return defaultActive;
	}
	public void setDefaultActive(boolean defaultActive) {
		this.defaultActive = defaultActive;
	}
	
	@Column(name="default_disabled")
	public boolean isDefaultDisabled() {
		return defaultDisabled;
	}
	public void setDefaultDisabled(boolean defaultDisabled) {
		this.defaultDisabled = defaultDisabled;
	}
	
	@Column(name="default_level1")
	public boolean isDefaultLevel1() {
		return defaultLevel1;
	}
	public void setDefaultLevel1(boolean defaultLevel1) {
		this.defaultLevel1 = defaultLevel1;
	}
	
	@Column(name="default_level2")
	public boolean isDefaultLevel2() {
		return defaultLevel2;
	}
	public void setDefaultLevel2(boolean defaultLevel2) {
		this.defaultLevel2 = defaultLevel2;
	}
	
	@Column(name="default_level3")
	public boolean isDefaultLevel3() {
		return defaultLevel3;
	}
	public void setDefaultLevel3(boolean defaultLevel3) {
		this.defaultLevel3 = defaultLevel3;
	}
	
	@Column(name="default_level4")
	public boolean isDefaultLevel4() {
		return defaultLevel4;
	}
	public void setDefaultLevel4(boolean defaultLevel4) {
		this.defaultLevel4 = defaultLevel4;
	}
	
	@Column(name="default_level5")
	public boolean isDefaultLevel5() {
		return defaultLevel5;
	}
	public void setDefaultLevel5(boolean defaultLevel5) {
		this.defaultLevel5 = defaultLevel5;
	}
	
	@Column(name="default_ca_uuids")
	public String getDefaultCaUuids() {
		return defaultCaUuids;
	}
	public void setDefaultCaUuids(String defaultCaUuids) {
		this.defaultCaUuids = defaultCaUuids;
	}
	
	@Column(name="default_text")
	public String getDefaultText() {
		return defaultText;
	}
	public void setDefaultText(String defaultText) {
		this.defaultText = defaultText;
	}
	
	@Column(name="seconds_refresh")
	public int getSecondsRefresh() {
		return secondsRefresh;
	}
	public void setSecondsRefresh(int secondsRefresh) {
		this.secondsRefresh = secondsRefresh;
	}
	
	@Column(name="starting_zoom_level")
	public int getStartingZoomLevel() {
		return startingZoomLevel;
	}
	public void setStartingZoomLevel(int startingZoomLevel) {
		this.startingZoomLevel = startingZoomLevel;
	}
	
	@Column(name="starting_lat")
	public float getStartingLat() {
		return startingLat;
	}
	public void setStartingLat(float startingLat) {
		this.startingLat = startingLat;
	}
	
	@Column(name="starting_long")
	public float getStartingLong() {
		return startingLong;
	}
	public void setStartingLong(float startingLong) {
		this.startingLong = startingLong;
	}	
	
}
