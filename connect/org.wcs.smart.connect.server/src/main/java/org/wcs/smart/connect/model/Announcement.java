/*
 * Copyright (C) 2025 Wildlife Conservation Society
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

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name="announcement", schema="connect")
public class Announcement extends ConnectUuidItem{
	
	private ConservationAreaInfo caInfo;
	private String message;
	private ZonedDateTime createdOn;
	private ZonedDateTime expiresOn;
	
		
	public Announcement(){
			
	}
		
	@ManyToOne
	@JoinColumn(name = "ca_uuid")
	public ConservationAreaInfo getConservationArea(){
		return this.caInfo;
	}
		
	public void setConservationArea(ConservationAreaInfo info){
		this.caInfo = info;
	}
			
	@Column(name="created_on")
	public ZonedDateTime getCreatedOn(){
		return this.createdOn;
	}
	
	public void setCreatedOn(ZonedDateTime createdOn){
		this.createdOn = createdOn;
	}
	
	@Column(name="expires_on")
	public ZonedDateTime getExpiresOn(){
		return this.expiresOn;
	}
	
	public void setExpiresOn(ZonedDateTime expiresOn){
		this.expiresOn = expiresOn;
	}
	
	@Column(name="message")
	public String getMessage() {
		return this.message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Transient 
	public AnnouncementProxy toProxy() {
		AnnouncementProxy proxy = new AnnouncementProxy();
		proxy.setCaUuid(getConservationArea().getUuid());
		proxy.setCaName(getConservationArea().getLabel());
		proxy.setMessage(getMessage());
		proxy.setExpiresOn(getExpiresOn());
		proxy.setCreatedOn(getCreatedOn());
		proxy.setUuid(getUuid());
		return proxy;
	}
}
