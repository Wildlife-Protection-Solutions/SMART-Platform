package org.wcs.smart.i2.migrate.entity;

import java.util.UUID;

public class EntityItemAttribute {

	private UUID attributeUuid;
	private Double doubleValue;
	private String stringValue;
	private UUID uuidValue;
	
	public UUID getAttributeUuid() {
		return attributeUuid;
	}
	public void setAttributeUuid(UUID attributeUuid) {
		this.attributeUuid = attributeUuid;
	}
	public Double getDoubleValue() {
		return doubleValue;
	}
	public void setDoubleValue(Double doubleValue) {
		this.doubleValue = doubleValue;
	}
	public String getStringValue() {
		return stringValue;
	}
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	public UUID getUuidValue() {
		return uuidValue;
	}
	public void setUuidValue(UUID uuidValue) {
		this.uuidValue = uuidValue;
	}

	
}
