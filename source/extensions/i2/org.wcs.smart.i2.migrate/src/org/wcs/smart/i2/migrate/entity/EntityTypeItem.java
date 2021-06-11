package org.wcs.smart.i2.migrate.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.ca.ConservationArea;

public class EntityTypeItem {

	public enum Type{FIXED,TRANSIENT};
	
	private UUID uuid;
	private String keyId;
	private LocalDate dateCreated;
	private UUID creator;
	private UUID dmUuid;
	private Type type;
	
	private HashMap<UUID, String> names;
	private List<EntityTypeAttributeItem> items;
	
	private ConservationArea ca;
	
	public EntityTypeItem(ConservationArea ca) {
		names = new HashMap<>();
		items = new ArrayList<>();
		this.ca = ca;
	}
	
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	public List<EntityTypeAttributeItem> getAttributes(){
		return items;
	}
	public void addAttribute(EntityTypeAttributeItem item) {
		items.add(item);
	}
	
	public void addName(UUID languageUuid, String value) {
		names.put(languageUuid, value);
	}
	
	public HashMap<UUID, String> getNames(){
		return this.names;
	}
	
	public UUID getUuid() {
		return uuid;
	}


	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getKeyId() {
		return keyId;
	}


	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}


	public LocalDate getDateCreated() {
		return dateCreated;
	}


	public void setDateCreated(LocalDate dateCreated) {
		this.dateCreated = dateCreated;
	}


	public UUID getCreator() {
		return creator;
	}


	public void setCreator(UUID creator) {
		this.creator = creator;
	}


	public UUID getDmUuid() {
		return dmUuid;
	}


	public void setDmUuid(UUID dmUuid) {
		this.dmUuid = dmUuid;
	}


	public Type getType() {
		return type;
	}


	public void setType(Type type) {
		this.type = type;
	}

}
