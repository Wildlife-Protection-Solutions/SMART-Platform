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
package org.wcs.smart.connect.cybertracker.model;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.wcs.smart.connect.util.ZonedDateTimeDeserializer;
import org.wcs.smart.connect.util.ZonedDateTimeSerializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * JSON Proxy for cybertracker package proxy
 * @author Emily
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CyberTrackerPackageProxy {

	private UUID uuid;
	
	private String caLabel;
	private UUID caUuid;
	private String type;
	
	private String revision;
	private String name;

	private String appLink;
	
	private boolean isPrivate;
	
	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)  
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	private ZonedDateTime uploadedDate;
	
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getCaLabel() {
		return caLabel;
	}

	public void setCaLabel(String caLabel) {
		this.caLabel = caLabel;
	}

	public UUID getCaUuid() {
		return caUuid;
	}

	public void setCaUuid(UUID caUuid) {
		this.caUuid = caUuid;
	}

	public ZonedDateTime getUploadedDate() {
		return uploadedDate;
	}

	public void setUploadedDate(ZonedDateTime uploadedDate) {
		this.uploadedDate = uploadedDate;
	}
	
	public String getVersion() {
		return this.revision;
	}
	public void setVersion(String revision) {
		this.revision = revision;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getType() {
		return this.type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public boolean getIsPrivate() {
		return this.isPrivate;
	}
	
	/**
	 * For SMART Collect packages by default packages are not private (false).
	 * For SMART Connect packages by default packages are private
	 * 
	 * @param isPrivate
	 */
	public void setIsPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}
	
	
	public void setAppLink(String link) {
		this.appLink = link;
	}
	
	public String getAppLink() {
		return this.appLink;
	}
}
