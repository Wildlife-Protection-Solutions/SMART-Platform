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
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Proxy for announcement to support ui
 */
public class AnnouncementProxy extends ConnectUuidItem{
	
	private UUID cauuid;
	private String caname;
	private String message;
	private ZonedDateTime createdOn;
    private ZonedDateTime expiresOn;
	
		
	public AnnouncementProxy(){
			
	}
		
	public UUID getCaUuid(){
		return this.cauuid;
	}
		
	public void setCaUuid(UUID cauuid){
		this.cauuid = cauuid;
	}
	
	public String getCaName() {
		return this.caname;
	}
	
	public void setCaName(String name) {
		this.caname = name;
	}
			
    @JsonSerialize(using = org.wcs.smart.connect.util.ZonedDateTimeSerializer.class)
	public ZonedDateTime getCreatedOn(){
		return this.createdOn;
	}
	
	public void setCreatedOn(ZonedDateTime createdOn){
		this.createdOn = createdOn;
	}
	
    @JsonSerialize(using = org.wcs.smart.connect.util.ZonedDateTimeSerializer.class)
	public ZonedDateTime getExpiresOn(){
		return this.expiresOn;
	}
	
	
	public void setExpiresOn(ZonedDateTime expiresOn){
		this.expiresOn = expiresOn;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
}
