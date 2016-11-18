package org.wcs.smart.i2.search;

import java.util.UUID;

import org.wcs.smart.i2.model.IntelEntity;

public class IntelEntitySearchResult {
	
	private String matchedString;
	private UUID entityUuid;
	private IntelEntity entity;
	private Double matchRate;

	public IntelEntitySearchResult(UUID entity, String matchedString, double rate) {
		setResult(entity, matchedString, rate);
	}
	
	public void setResult(UUID entity, String matchedString, double rate){
		this.matchedString = matchedString;
		this.entityUuid = entity;
		this.matchRate = rate;
		this.entity = null;
	}
	public UUID getEntityUuid(){
		return this.entityUuid;
	}
	
	public Double getRating(){
		return this.matchRate;
	}
	
	public String getMatchString(){
		return this.matchedString;
	}
	
	public IntelEntity getEntity(){
		return this.entity;
	}
	
	public void setEntity(IntelEntity entity){
		this.entity = entity;
	}
}