package org.wcs.smart.connect.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SmartActionsProxy {

	private String name;
	private String key;
	private List<ActionResource> resources;
	
	
	public SmartActionsProxy(String actionName, String actionKey){
		this.key = actionKey;
		this.name = actionName;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public List<ActionResource> getResources(){
		return this.resources;
	}
	
	public void addResource(String resourceName, UUID resourceKey){
		if (resources == null){
			resources = new ArrayList<SmartActionsProxy.ActionResource>();
		}
		resources.add(new ActionResource(resourceKey, resourceName));
	}
	
	public class ActionResource{
		private UUID key;
		private String name;
		
		public ActionResource(@JsonProperty("key")UUID key, @JsonProperty("name")String name){
			this.name = name;
			this.key = key;
		}
		public String getName(){
			return this.name;
		}
		public UUID getKey(){
			return this.key;
		}
		public void setName(String name){
			this.name = name;
		}
		public void setKey(UUID key){
			this.key =  key;
		}
	}
}
