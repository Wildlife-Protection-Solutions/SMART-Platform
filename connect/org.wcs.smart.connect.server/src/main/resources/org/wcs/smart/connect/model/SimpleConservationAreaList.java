package org.wcs.smart.connect.model;

import java.util.UUID;

public class SimpleConservationAreaList {
	private String name;
	private UUID uuid;
	
	public SimpleConservationAreaList(String name, UUID uuid){
		this.name = name;
		this.uuid = uuid;
	}
	
	public UUID getUuid(){
		return uuid;
	}
	public String getName(){
		return name;
	}

}
