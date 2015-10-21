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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Link between a SMART Connect server, a SMART user and 
 * the associated connect user account.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.connect_account")
//TODO: we do not want to store connect server passwords in plain text!!!!
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
	
	@Transient
	public String decryptPassword() throws Exception{
		if (getConnectPassword() == null){
			return null;
		}
		if (getConnectPassword().isEmpty()){
			return getConnectPassword();
		}
		String key = SmartDB.getPlainTextPassword();
		return PasswordAesManager.getInstance().decryptPassword(getConnectPassword(), key);
	}
	@Transient
	public String encryptPassword(String password) throws Exception{
		String key = SmartDB.getPlainTextPassword();
		return PasswordAesManager.getInstance().encryptPassword(password, key);
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof ConnectUser){
			ConnectUser s = (ConnectUser)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return this == s;
			}else if (s.getUuid() != null && this.getUuid() != null){
				return s.getUuid().equals(this.uuid);
			}
		}
		return false;
	}
	
	
	public int hashCode(){
		if (getUuid() != null){
			return getUuid().hashCode();
		}
		return super.hashCode();
	}
}
