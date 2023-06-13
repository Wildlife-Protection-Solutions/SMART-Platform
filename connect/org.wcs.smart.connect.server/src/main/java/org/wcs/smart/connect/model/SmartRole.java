package org.wcs.smart.connect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="roles", schema="connect")
public class SmartRole {

	public static final String SYSTEM_ROLE_NAME = "SYSTEM ROLE"; //$NON-NLS-1$
	
	public String roleId;
	public String rolename;
	public boolean isSystem;
	
	@Id
	@Column(name="role_id")
	public String getRoleId(){
		return this.roleId;
	}
	public void setRoleId(String roleId){
		this.roleId = roleId;
	}
	
	@Column(name="rolename")
	public String getRoleName(){
		return this.rolename;
	}
	
	public void setRoleName(String rolename){
		this.rolename = rolename;
	}
	
	@Column(name="is_system")
	public boolean getIsSystem(){
		return this.isSystem;
	}
	
	public void setIsSystem(boolean isSystem){
		this.isSystem = isSystem;
	}
	
}
