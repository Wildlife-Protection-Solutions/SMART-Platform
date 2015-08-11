package org.wcs.smart.connect.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "connect.alerts")
public class Alert extends UuidItem{
	private String userGeneratedId; 
	private Date date;
	private String description;
	private UUID type_uuid;
	private Integer level;
	private UUID ca_uuid;
	private String status;
	private Double x;
	private Double y;
	private UUID creator_uuid;
	
	@Column(name="date")
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	
	@Column(name="description")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Column(name="type_uuid")
	public UUID getType_uuid() {
		return type_uuid;
	}
	public void setType_uuid(UUID type_uuid) {
		this.type_uuid = type_uuid;
	}
	
	@Column(name="level")
	public Integer getLevel() {
		return level;
	}
	public void setLevel(Integer level) {
		this.level = level;
	}
	
	@Column(name="ca_uuid")
	public UUID getCa_uuid() {
		return ca_uuid;
	}
	public void setCa_uuid(UUID ca_uuid) {
		this.ca_uuid = ca_uuid;
	}
	
	@Column(name="status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Column(name="x")
	public Double getX() {
		return x;
	}
	public void setX(Double x) {
		this.x = x;
	}
	
	@Column(name="y")
	public Double getY() {
		return y;
	}
	public void setY(Double y) {
		this.y = y;
	}
	
	@Column(name="creator_uuid")
	public UUID getCreator_uuid() {
		return creator_uuid;
	}
	public void setCreator_uuid(UUID creator_uuid) {
		this.creator_uuid = creator_uuid;
	}
	
	@Column(name="user_generated_id")
	public String getUserGeneratedId() {
		return userGeneratedId;
	}
	public void setUserGeneratedId(String userGeneratedId) {
		this.userGeneratedId = userGeneratedId;
	}
	
	
	
	
}
