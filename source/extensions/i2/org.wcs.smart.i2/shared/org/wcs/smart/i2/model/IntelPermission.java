package org.wcs.smart.i2.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.Employee;


@Entity
@Table(name="smart.i_permission")
public class IntelPermission {

	public static final int ADMIN = 1 << 0;
	public static final int READ_ONLY = 1 << 1;
	
	public static final int ENTITY_CREATE = 1 << 2;
	public static final int ENTITY_VIEW = 1 << 3;
	public static final int ENTITY_EDIT = 1 << 4;
	public static final int ENTITY_DELETE = 1 << 5;
	
	public static final int RECORD_CREATE = 1 << 6;
	public static final int RECORD_VIEW = 1 << 7;
	public static final int RECORD_EDIT_ALL = 1 << 8;
	public static final int RECORD_EDIT_NOTSTATUS = 1 << 9;
	public static final int RECORD_DELETE = 1 << 10;
	
	
	
	private int permission;
	private IntelPermissionPk id;
	
	public int getPermission() {
		return this.permission;
	}
	
	public void setPermission(int permission) {
		this.permission = permission;
	}
	
	@EmbeddedId
	public IntelPermissionPk getId(){
		return id;
	}
	/**
	 * 
	 * @param id primary key for association
	 */
	public void setId(IntelPermissionPk id){
		this.id = id;
	}
	
	@Transient
	public Employee getEmployee() {
		return id.getEmployee();
	}
	@Transient
	public void setEntityType(Employee employee) {
		id.setEmployee(employee);
	}
	@Transient
	public IntelProfile getProfile() {
		return id.getProfile();
	}
	@Transient
	public void setProfile(IntelProfile config) {
		id.setProfile(config);
	}
	
	@Embeddable
	private static class IntelPermissionPk implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private Employee employee;
		private IntelProfile config;

		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="employee_uuid")
		public Employee getEmployee() {
			return employee;
		}

		public void setEmployee(Employee employee) {
			this.employee = employee;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="profile_uuid")
		public IntelProfile getProfile() {
			return config;
		}

		public void setProfile(IntelProfile config) {
			this.config = config;
		}
		
		@Override
		public boolean equals(Object key) {
			if (key == null) return false;
			if (key == this) return true;
			if (key.getClass() == getClass()) return true;
			return Objects.equals(config, ((IntelPermissionPk)key).config) &&
					Objects.equals(employee, ((IntelPermissionPk)key).employee);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(config, employee);
		}
	}
	
}
