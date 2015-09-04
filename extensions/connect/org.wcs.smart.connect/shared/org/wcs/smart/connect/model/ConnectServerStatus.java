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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;

/**
 * Model object representing the status of
 * data upload to connect server.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.connect_status")
public class ConnectServerStatus {

	public enum Status{
		BACKUP,
		UPLOAD,
		DONE,
		ERROR
	}
	
	private UUID cauuid;

	private ConnectServer server;
	
	private UUID versionId;
	
	private Long server_revision;
	
	private Status status;
	
	private String uploadUrl;
	
	private String localFile;
	
	@Id
	@Column(name="ca_uuid")
	public UUID getUuid() {
		return cauuid;
	}

	protected void setUuid(UUID uuid) {
		this.cauuid = uuid;
	}

	@Transient
	public void setConservationArea(ConservationArea ca) {
		setUuid(ca.getUuid());
	}
	
	@OneToOne
	@JoinColumn(name="connect_uuid")
	public ConnectServer getServer(){
		return this.server;
	}
	public void setServer(ConnectServer server){
		this.server = server;
	}
	
	@Column(name="version")
	public UUID getVersion(){
		return this.versionId;
	}
	public void setVersion(UUID versionId){
		this.versionId = versionId;
	}
	
	@Column(name="server_revision")
	public Long getServerRevision(){
		return this.server_revision;
	}
	
	public void setServerRevision(Long revision){
		this.server_revision = revision;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus(){
		return this.status;
	}
	
	public void setStatus(Status status){
		this.status = status;
	}
	
	@Column(name="uploadurl")
	public String getUploadUrl(){
		return this.uploadUrl;
	}
	
	public void setUploadUrl(String url){
		this.uploadUrl = url;
	}
	
	@Column(name="localfile")
	public String getLocalFile(){
		return this.localFile;
	}
	public void setLocalFile(String file){
		this.localFile = file;
	}
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof ConnectServerStatus){
			ConnectServerStatus s = (ConnectServerStatus)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return this == s;
			}else if (s.getUuid() != null && this.getUuid() != null){
				return s.getUuid().equals(this.cauuid);
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
