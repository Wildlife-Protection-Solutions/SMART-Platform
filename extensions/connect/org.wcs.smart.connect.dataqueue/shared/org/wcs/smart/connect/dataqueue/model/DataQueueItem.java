package org.wcs.smart.connect.dataqueue.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.wcs.smart.ca.UuidItem;

@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class DataQueueItem extends UuidItem{

	public enum Type{
		PATROL_XML,
		INCIDENT_XML,
		MISSION_XML,
		INTELL_XML
	}
	
	private Type type;
	private UUID caUuid;
	private String name;

	
	@Column(name="type")
	@Enumerated(EnumType.STRING)
	public Type getType(){
		return this.type;
	}
	
	public void setType(Type type){
		this.type = type;
	}
	
	@Column(name="ca_uuid")
	public UUID getConservationArea(){
		return this.caUuid;
	}
	
	public void setConservationArea(UUID caUuid){
		this.caUuid = caUuid;
	}
	
	@Column(name="name")
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}

	
}
