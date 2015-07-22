package org.wcs.smart.connect.security;

import java.util.UUID;

public class ResourceOption {

	private String name;
	private UUID uuid;
	
	public ResourceOption(String name, UUID uuid){
		this.name = name;
		this.uuid = uuid;
	}
	
	public String getName(){
		return this.name;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}
	
}
