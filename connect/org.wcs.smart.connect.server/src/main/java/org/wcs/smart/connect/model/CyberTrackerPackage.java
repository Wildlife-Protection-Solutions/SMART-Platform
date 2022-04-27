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
package org.wcs.smart.connect.model;

import java.beans.Transient;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.api.noa.CyberTrackerNoa;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;
import org.wcs.smart.connect.util.ZonedDateTimeDeserializer;
import org.wcs.smart.connect.util.ZonedDateTimeSerializer;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.smartcollect.model.SmartCollectPackage;
import org.wcs.smart.util.UuidUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Database cybertracker package table
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "connect.ct_package")
public class CyberTrackerPackage extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	public enum Status{
		UPLOADING,
		READY
	};
	
	private Status status;
	private ZonedDateTime uploadedDate;
	private ConservationAreaInfo ca;
	private String filename;
	private String version;
	private String name;
	private UUID workItem;
	private UUID ctpackage;
	private String type;
	private boolean isPrivate;

	@Column(name="uploaded_date")
	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)  
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	public ZonedDateTime getUploadedDate() {
		return this.uploadedDate;
	}
	
	public void setUploadedDate(ZonedDateTime date) {
		this.uploadedDate = date;
	}
	
	@Column(name="package_type")
	public String getType() {
		return this.type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	@Column(name="status")
	@Enumerated(value = EnumType.STRING)
	public Status getStatus() {
		return this.status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	
	@Column(name="version")
	public String getVersion() {
		return this.version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	@Column(name="name")
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name="filename")
	public String getFilename() {
		return this.filename;
	}
	public void setFilename(String filename) {
		this.filename= filename;
	}
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ca_uuid", referencedColumnName="ca_uuid")
	public ConservationAreaInfo getConservationArea() {
		return this.ca;
	}
	public void setConservationArea(ConservationAreaInfo ca) {
		this.ca = ca;
	}
	
	@Column(name="work_item_uuid")
	public UUID getWorkItem(){
		return this.workItem;
	}
	public void setWorkItem(UUID workItemUuid){
		this.workItem = workItemUuid;
	}

	@Column(name="package_uuid")
	public UUID getCtPackageUuid() {
		return this.ctpackage;
	}
	
	public void setCtPackageUuid(UUID ctpackage) {
		this.ctpackage = ctpackage;
	}
	
	@Column(name="is_private")
	public boolean getIsPrivate() {
		return this.isPrivate;
	}
	
	public void setIsPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}
	
	@Transient
	public CyberTrackerPackageProxy asProxy(URL rootUrl) throws MalformedURLException {	
		
		boolean requirespassword = getIsPrivate();
		if (getType().equals(SmartCollectPackage.PACKAGE_TYPENAME)) {
			requirespassword = false;
		}
		
		String path = rootUrl.getPath() + "/noa/" + CyberTrackerNoa.PATH + "/packages/" + UuidUtils.uuidToString(getCtPackageUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		URL url = new URL(rootUrl.getProtocol(), rootUrl.getHost(), rootUrl.getPort(), path);
		
		CyberTrackerPackageProxy proxy = new CyberTrackerPackageProxy();
		proxy.setAppLink(ICtPackage.generateSmartMobileAppLink(url, requirespassword));
		proxy.setCaLabel(getConservationArea().getLabel());
		proxy.setCaUuid(getConservationArea().getUuid());
		proxy.setUuid( getCtPackageUuid() );
		proxy.setVersion(getVersion());
		proxy.setUploadedDate(getUploadedDate());
		proxy.setName(getName());
		proxy.setType(getType());
		proxy.setIsPrivate(getIsPrivate());
		return proxy;
		
	}
}
