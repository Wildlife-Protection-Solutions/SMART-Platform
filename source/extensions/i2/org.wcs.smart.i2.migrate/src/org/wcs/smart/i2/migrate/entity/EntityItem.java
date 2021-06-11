package org.wcs.smart.i2.migrate.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntityItem {

	public enum Status{ACTIVE, INACTIVE};
	
	private UUID uuid;
	private UUID eType;
	private String id;
	private Status status;
	
	private UUID dmUuid;
	
	private List<EntityItemAttribute> attributes;
	
	private Double x;
	private Double y;
	
	public EntityItem() {
		attributes = new ArrayList<>();
	}
	
	public void addAttribute(EntityItemAttribute attribute) {
		this.attributes.add(attribute);
	}
	public List<EntityItemAttribute> getAttributes(){
		return this.attributes;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public UUID geteType() {
		return eType;
	}
	public void seteType(UUID eType) {
		this.eType = eType;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public UUID getDmUuid() {
		return dmUuid;
	}
	public void setDmUuid(UUID dmUuid) {
		this.dmUuid = dmUuid;
	}
	public Double getX() {
		return x;
	}
	public void setX(Double x) {
		this.x = x;
	}
	public Double getY() {
		return y;
	}
	public void setY(Double y) {
		this.y = y;
	}	
}
