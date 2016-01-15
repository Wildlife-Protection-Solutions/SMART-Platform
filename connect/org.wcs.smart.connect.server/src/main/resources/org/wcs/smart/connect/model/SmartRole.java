package org.wcs.smart.connect.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="connect.roles")
public class SmartRole {

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
