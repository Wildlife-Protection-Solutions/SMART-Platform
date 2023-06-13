/*
 * Copyright (C) 2019 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.i2.model;

import java.io.Serializable;
import java.util.Objects;

import org.wcs.smart.ca.Employee;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


/**
 * Permission model for profiles
 * @author Emily
 *
 */
@Entity
@Table(name="i_permission", schema="smart")
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
	
	public static final int QUERY = 1 << 11;
	
	
	
	private int permission;
	private IntelPermissionPk id = new IntelPermissionPk();
	
	@Column(name="permissions")
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
	public void setEmployee(Employee employee) {
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
