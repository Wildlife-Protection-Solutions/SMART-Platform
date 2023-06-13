package org.wcs.smart.connect.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


/**
 * Link between a smart user and a role
 * @author Emily
 *
 */
@Entity
@Table(name="user_roles", schema="connect")
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
		
		@Override
		public boolean equals(Object other){
			if (other == this) return true;
			if (other == null) return false;
			if (getClass() != other.getClass()) return false;
			SmartUserRolePk o = (SmartUserRolePk) other;
			return Objects.equals(role, o.role) && Objects.equals(username, o.username);
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(role, username);
		}
	}
}
