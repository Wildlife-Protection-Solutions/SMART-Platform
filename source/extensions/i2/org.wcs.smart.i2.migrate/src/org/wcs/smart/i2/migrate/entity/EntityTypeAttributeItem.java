package org.wcs.smart.i2.migrate.entity;

import java.util.HashMap;
import java.util.UUID;

public class EntityTypeAttributeItem {

	private UUID uuid;
	
	private UUID dmAttribute;
	private int order;
	private boolean isPrimary;
	private String keyId;
	
	private HashMap<UUID, String> names;

	
	public EntityTypeAttributeItem() {
		names = new HashMap<>();

	}
	
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public UUID getUuid() {
		return this.uuid;
	}
	public void addName(UUID languageUuid, String value) {
		names.put(languageUuid, value);
	}
	
	public HashMap<UUID, String> getNames(){
		return this.names;
	}
	
	
	public UUID getDmAttribute() {
		return dmAttribute;
	}
	public void setDmAttribute(UUID dmAttribute) {
		this.dmAttribute = dmAttribute;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	public boolean isPrimary() {
		return isPrimary;
	}
	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
	public String getKeyId() {
		return keyId;
	}
	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}
	
}
