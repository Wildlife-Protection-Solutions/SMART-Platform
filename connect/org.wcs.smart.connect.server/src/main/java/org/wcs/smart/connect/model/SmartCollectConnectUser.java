/*
 * Copyright (C) 2020 Wildlife Conservation Society
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

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.smartcollect.model.SmartCollectUser;


@Entity
@Table(name="connect.smartcollect_user")
public class SmartCollectConnectUser extends UuidItem{
	
	private static final long serialVersionUID = 1L;
	
	private SmartCollectUser.State state;
	
	private String source;
	private String deviceId;
	
	private LocalDateTime validationSentDate;
	private String validationKey;

	@Enumerated(EnumType.STRING)
	@Column(name="state")
	public SmartCollectUser.State getState() {
		return this.state;
	}
	public void setState(SmartCollectUser.State state) {
		this.state = state;
	}
	
	/**
	 * The email address or phone number of the source
	 * @return
	 */
	@Column(name="source")
	public String getSource() {
		return this.source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	
	@Column(name="device_id")
	public String getDeviceId() {
		return this.deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	
	@Column(name="validation_sent_date")
	public LocalDateTime getValidateSentDate() {
		return this.validationSentDate;
	}
	public void setValidateSentDate(LocalDateTime date) {
		this.validationSentDate = date;
	}

	@Column(name="validation_key")
	public String getValidationKey() {
		return this.validationKey;
	}
	public void setValidationKey(String key) {
		this.validationKey = key;
	}
}
