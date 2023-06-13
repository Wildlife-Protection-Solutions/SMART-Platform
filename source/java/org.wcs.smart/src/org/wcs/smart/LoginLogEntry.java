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
package org.wcs.smart;

import java.sql.Timestamp;

import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


/**
 * Login Log Entry - each time someone logs in we record a row with the user, userlevel and timestamp.
 * 
 * @author Jeff
 *
 */
@Entity
@Table(name = "login_log", schema="smart")
public class LoginLogEntry extends UuidItem{
	
	private static final long serialVersionUID = 1L;
	
	private String smartUserId;
	private String userLevels;
	private Timestamp loginTimestamp;
	private String caId;
	private String caName;
	
	public LoginLogEntry(){
		this.loginTimestamp = new Timestamp(System.currentTimeMillis());
	}
	

	@Column(name="login_timestamp")
	public Timestamp getLoginTimestamp() {
		return loginTimestamp;
	}
	public void setLoginTimestamp(Timestamp loginTimestamp) {
		this.loginTimestamp = loginTimestamp;
	}

	@Column(name="ca_id")
	public String getCaId() {
		return caId;
	}
	public void setCaId(String caId) {
		this.caId = caId;
	}

	@Column(name="ca_name")
	public String getCaName() {
		return caName;
	}
	public void setCaName(String caName) {
		this.caName = caName;
	}

	@Column(name="smart_userid")
	public String getSmartUserId() {
		return smartUserId;
	}
	public void setSmartUserId(String smartUserId) {
		this.smartUserId = smartUserId;
	}

	@Column(name="smart_userlevels")
	public String getUserLevels() {
		return userLevels;
	}
	public void setUserLevels(String userLevels) {
		this.userLevels = userLevels;
	}



	
}