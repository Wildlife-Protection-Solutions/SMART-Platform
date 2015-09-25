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
	public boolean isDefault_active() {
		return defaultActive;
	}
	public void setDefault_active(boolean default_active) {
		this.defaultActive = default_active;
	}
	
	@Column(name="default_disabled")
	public boolean isDefault_disabled() {
		return defaultDisabled;
	}
	public void setDefault_disabled(boolean default_disabled) {
		this.defaultDisabled = default_disabled;
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
	
}
