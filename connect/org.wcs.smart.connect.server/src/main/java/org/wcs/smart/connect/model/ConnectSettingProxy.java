package org.wcs.smart.connect.model;

public class ConnectSettingProxy {
	
	private String key;
	private String value;

	private String label;
	private String tooltip;
	
	public ConnectSettingProxy(String key, String value, String label, String tooltip) {
		this.key = key;
		this.value = value;
		this.label = label;
		this.tooltip = tooltip;
	}
	
	
	public String getKey() {
		return this.key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getTooltip() {
		return this.tooltip;
	}
	
	public void aetTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
}
