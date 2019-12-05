package org.wcs.smart.i2.ui.views.entity.search;

import java.util.HashMap;
import java.util.UUID;

public class EntityTableRowItem {

	private UUID entityUuid;
	private String id;
	private String type;
	
	private String profileName;
	private String profileKey;
	private UUID profileUuid;
	
	private HashMap<String, Object> attributes = new HashMap<>();
	
	public EntityTableRowItem(UUID entityUuid, String id, String profileName, String profileKey, UUID profileUuid) {
		this.entityUuid = entityUuid;
		this.id = id;
		this.profileName = profileName;
		this.profileKey = profileKey;
		this.profileUuid = profileUuid;
	}
	
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setAttribute(String attributeKey, Object value) {
		attributes.put(attributeKey, value);
	}
	public String getProfileName() { return this.profileName; }
	public String getProfileKey() { return this.profileKey; }
	public UUID getEntityUuid() { return this.entityUuid; }
	public UUID getProfileUuid() { return this.profileUuid; }
	public String getType() { return this.type; }
	public String getId() { return id; }
	public Object getAttributeValue(String attributeKey) { return attributes.get(attributeKey); }
	
}
