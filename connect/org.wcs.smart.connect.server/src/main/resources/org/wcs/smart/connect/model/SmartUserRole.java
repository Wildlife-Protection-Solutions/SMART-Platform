package org.wcs.smart.connect.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * Link between a smart user and a role
 * @author Emily
 *
 */
@Entity
@Table(name="connect.user_roles")
public class SmartUserRole {
	
	private SmartUserRolePk id = new SmartUserRolePk();
	
	public SmartUserRole(){
	}
	
	@EmbeddedId
	public SmartUserRolePk getId(){
		return this.id;
	}
	public void setId(SmartUserRolePk id){
		this.id = id;
	}
	
	@Transient
	public String getUsername(){
		return id.getUsername();
	}
	@Transient
	public void setUsername(String userName){
		id.setUsername(userName);
	}
	@Transient
	public SmartRole getRole(){
		return id.getRole();
	}
	@Transient
	public void setRole(SmartRole role){
		id.setRole(role);
	}
	
	@Embeddable
	private static class SmartUserRolePk implements Serializable{
		private static final long serialVersionUID = 1L;
		
		private String username;
		private SmartRole role;
		
		public SmartUserRolePk(){
		}
		
		@Column(name="username")
		public String getUsername(){
			return this.username;
		}
		public void setUsername(String username){
			this.username = username;
		}
		
		@ManyToOne
		@JoinColumn(name="role_id")
		public SmartRole getRole(){
			return this.role;
		}
		public void setRole(SmartRole role){
			this.role = role;
		}
	}
}
