package org.wcs.smart.connect.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name="connect.user_actions")
public class SmartUserAction extends UuidItem{

	private String username;
	private String actionKey;
	private UUID resource;
	
	@Column(name="username")
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Column(name="action")
	public String getAction(){
		return this.actionKey;
	}
	
	public void setAction(String actionKey){
		this.actionKey = actionKey;
	}
	
	@Column(name="resource")
	@Type(type = "pg-uuid")
	public UUID getResource(){
		return this.resource;
	}
	public void setResource(UUID resource){
		this.resource = resource;
	}
}
