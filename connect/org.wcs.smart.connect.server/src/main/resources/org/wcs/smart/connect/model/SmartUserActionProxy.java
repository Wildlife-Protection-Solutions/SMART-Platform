package org.wcs.smart.connect.model;

import java.util.UUID;

public class SmartUserActionProxy {

	private String actionName;
	private String resourceName;
	private String actionKey;
	private UUID resource;
	
	public String getActionKey(){
		return this.actionKey;
	}
	
	public void setActionKey(String key){
		this.actionKey = key;
	}
	
	public String getActionName(){
		return this.actionName;
	}
	public void setActionName(String name){
		this.actionName = name;
	}
	
	public String getResourceName(){
		return this.resourceName;
	}
	
	public void setResourceName(String name){
		this.resourceName = name;
	}
	
	public UUID getResource(){
		return this.resource;
	}
	public void setResource(UUID resource){
		this.resource = resource;
	}
}
