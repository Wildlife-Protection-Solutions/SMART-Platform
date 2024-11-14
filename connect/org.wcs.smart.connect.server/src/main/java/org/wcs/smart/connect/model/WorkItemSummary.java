/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Smart connect upload item entity.
 * @author Emily
 *
 */
@Entity
@Table(name="work_item_summary", schema="connect")
public class WorkItemSummary extends ConnectUuidItem {

	private ConservationAreaInfo caInfo;
	
	private String username;
	private String ip;
	private LocalDateTime lastSyncUp;
	private LocalDateTime lastSyncDown;
	private LocalDateTime lastCaUp;
	private LocalDateTime lastCaDown;
	
	private IpAlias alias;
	
	public WorkItemSummary(){
		
	}
	
	@ManyToOne
	@JoinColumn(name = "ca_uuid")
	public ConservationAreaInfo getConservationAreaInfo(){
		return this.caInfo;
	}
	
	public void setConservationAreaInfo(ConservationAreaInfo info){
		this.caInfo = info;
	}
	
	@ManyToOne
	@JoinColumn(name = "ip", insertable=false, updatable=false)
	public IpAlias getAlias(){
		return this.alias;
	}
	
	public void setAlias(IpAlias alias){
		this.alias = alias;
	}
	
	@Column(name="username")
	public String getUsername(){
		return this.username;
	}
	public void setUsername(String username){
		this.username= username;
	}
	
	@Column(name="ip")
	public String getIp(){
		return this.ip;
	}
	public void setIp(String ip){
		this.ip= ip;
	}
	
	@JsonIgnore
	@Column(name="last_ca_up")
	public LocalDateTime getLastCaUpUtc(){
		return this.lastCaUp;
	}
	public void setLastCaUpUtc(LocalDateTime lastCaUp){
		this.lastCaUp= lastCaUp;
	}

	@JsonIgnore
	@Column(name="last_ca_down")
	public LocalDateTime getLastCaDownUtc(){
		return this.lastCaDown;
	}
	public void setLastCaDownUtc(LocalDateTime lastCaDown){
		this.lastCaDown= lastCaDown;
	}
	
	@JsonIgnore
	@Column(name="last_sync_up")
	public LocalDateTime getLastSyncUpUtc(){
		return this.lastSyncUp;
	}
	public void setLastSyncUpUtc(LocalDateTime lastSyncUp){
		this.lastSyncUp= lastSyncUp;
	}
	
	@JsonIgnore
	@Column(name="last_sync_down")
	public LocalDateTime getLastSyncDownUtc(){
		return this.lastSyncDown;
	}
	public void setLastSyncDownUtc(LocalDateTime lastSyncDown){
		this.lastSyncDown= lastSyncDown;
	}
	
	@Transient
	public String getLastSyncUp() {
		if (getLastSyncUpUtc() == null) return ""; //$NON-NLS-1$
		return getLastSyncUpUtc().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
	
	@Transient
	public String getLastSyncDown() {
		if (getLastSyncDownUtc() == null) return ""; //$NON-NLS-1$
		return getLastSyncDownUtc().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
	

	@Transient
	public String getLastCaUp() {
		if (getLastCaUpUtc() == null) return ""; //$NON-NLS-1$
		return getLastCaUpUtc().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
	
	@Transient
	public String getLastCaDown() {
		if (getLastCaDownUtc() == null) return ""; //$NON-NLS-1$
		return getLastCaDownUtc().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
	

}
