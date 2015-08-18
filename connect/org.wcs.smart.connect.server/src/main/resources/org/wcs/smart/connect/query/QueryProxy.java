package org.wcs.smart.connect.query;

import java.util.UUID;

public class QueryProxy {

	private UUID uuid;
	private String name;
	private String type;
	private String conservationAreaName;
	private String id;
	
	public QueryProxy(UUID uuid, String name, String type, String caName, String id){
		this.uuid = uuid;
		this.name = name;
		this.type = type;
		this.conservationAreaName = caName;
		this.id = id;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getConservationArea() {
		return conservationAreaName;
	}
	public void setConservationArea(String conservationAreaName) {
		this.conservationAreaName = conservationAreaName;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	
}
