/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.model;

import java.util.Objects;
import java.util.UUID;

import org.wcs.smart.ca.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Link between a SMART Connect server, a SMART user and 
 * the associated connect user account.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="connect_account", schema="smart")
public class ConnectUser {
	
	private UUID uuid;
	
	private ConnectServer server;
	
	private Employee smartUser;
	
	private String username;
	
	private String password;

	@Id
	@Column(name="employee_uuid")
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
    @OneToOne
    @PrimaryKeyJoinColumn(name="employee_uuid")
	public Employee getSmartUser() {
		return smartUser;
	}

	public void setSmartUser(Employee smartUser) {
		setUuid(smartUser.getUuid());
		this.smartUser = smartUser;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="connect_uuid", referencedColumnName="uuid")
	public ConnectServer getServer() {
		return server;
	}

	public void setServer(ConnectServer server) {
		this.server = server;
	}

	@Column(name="connect_user")
	public String getConnectUsername() {
		return username;
	}

	public void setConnectUsername(String username) {
		this.username = username;
	}

	@Column(name="connect_pass")
	public String getConnectPassword() {
		return password;
	}

	public void setConnectPassword(String password) {
		this.password = password;
	}
	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getUuid() == null) return false;
		if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;	
		ConnectUser s = (ConnectUser)other;		
		return Objects.equals(getUuid(), s.getUuid());		
	}
	
	
	public int hashCode(){
		if (getUuid() != null){
			return getUuid().hashCode();
		}
		return super.hashCode();
	}
}
