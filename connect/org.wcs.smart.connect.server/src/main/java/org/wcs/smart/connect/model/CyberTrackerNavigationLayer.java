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
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.connect.cybertracker.model.CyberTrackerNavigationProxy;
import org.wcs.smart.connect.util.ZonedDateTimeDeserializer;
import org.wcs.smart.connect.util.ZonedDateTimeSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Database cybertracker navigation layer
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "connect.ct_navigation_layer")
public class CyberTrackerNavigationLayer {

	public enum Status{
		UPLOADING,
		READY
	};
	private UUID uuid;
	
	private Status status;
	private ZonedDateTime uploadedDate;
	private ConservationAreaInfo ca;

	private String name;
	private UUID workItem;
	private String filename;

	/**
	 * 
	 * @return the uuid for the list element
	 */
	@Id
	@Column(name="uuid")
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	@Column(name="uploaded_date")
	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)  
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	public ZonedDateTime getUploadedDate() {
		return this.uploadedDate;
	}
	
	public void setUploadedDate(ZonedDateTime date) {
		this.uploadedDate = date;
	}
	
	@Column(name="filename")
	public String getFilename() {
		return this.filename;
	}
	public void setFilename(String filename) {
		this.filename= filename;
	}
	
	@Column(name="status")
	@Enumerated(value = EnumType.STRING)
	public Status getStatus() {
		return this.status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	
	@Column(name="name")
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
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
	
	@Transient
	public CyberTrackerNavigationProxy asProxy() {
		CyberTrackerNavigationProxy n = new CyberTrackerNavigationProxy();
		n.setCaLabel(getConservationArea().getLabel());
		n.setCaUuid(getConservationArea().getUuid());
		n.setName(getName());
		n.setUploadedDate(getUploadedDate());
		n.setUuid(getUuid());
		return n;
	}
	
}
