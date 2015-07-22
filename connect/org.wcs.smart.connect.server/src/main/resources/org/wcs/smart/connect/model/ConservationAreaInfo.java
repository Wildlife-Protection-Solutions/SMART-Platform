package org.wcs.smart.connect.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "connect.ca_info")
public class ConservationAreaInfo {

	public enum Status{
		UPLOADING,
		DATA,
		NODATA
	}
	
	private UUID version;
	private String label;
	private UUID caUuid;
	private Status status;
	
	public ConservationAreaInfo(){
	}
	
	/**
	 * 
	 * @return the uuid for the list element
	 */
	@Id
	@Type(type = "pg-uuid")
	@Column(name="ca_uuid")
	public UUID getUuid() {
		return caUuid;
	}

	public void setUuid(UUID uuid) {
		this.caUuid = uuid;
	}
	
	@Column(name="version")
	@Type(type = "pg-uuid")
	public UUID getVersion(){
		return this.version;
	}
	public void setVersion(UUID version){
		this.version = version;
	}
	@Column(name="label")
	public String getLabel(){
		return this.label;
	}
	public void setLabel(String label){
		this.label = label;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus(){
		return this.status;
	}
	public void setStatus(Status status){
		this.status = status;
	}
}
