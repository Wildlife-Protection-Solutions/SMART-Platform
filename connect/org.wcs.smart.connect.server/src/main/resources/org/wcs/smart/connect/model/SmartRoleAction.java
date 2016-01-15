package org.wcs.smart.connect.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * SMART Connect action associated with a user role.
 * @author Emily
 *
 */
@Entity
@Table(name="connect.role_actions")
public class SmartRoleAction  extends ConnectUuidItem{

	private SmartRole role;
	private String actionKey;
	private UUID resource;
	
	@JoinColumn(name="role_id")
	@Type(type = "pg-uuid")
	@ManyToOne
	public SmartRole getRole() {
		return role;
	}
	public void setRole(SmartRole role) {
		this.role = role;
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
