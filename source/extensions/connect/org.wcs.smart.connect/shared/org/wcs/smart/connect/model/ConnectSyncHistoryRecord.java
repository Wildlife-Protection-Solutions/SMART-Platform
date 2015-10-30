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

import java.nio.file.FileSystems;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.util.UuidUtils;

/**
 * SMART Connect communication history record.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.connect_sync_history")
public class ConnectSyncHistoryRecord extends UuidItem{

	public static final String PACKAGE_FILESTORE_DIR = "filestore"; //$NON-NLS-1$
	
	public static final String CONNECT_FILESTORE_DIR = "smart_connect"; //$NON-NLS-1$

	public static final String METADATA_FILE_SUFFIX = ".changelog.metadata"; //$NON-NLS-1$
	public static final String CHANGELOG_FILE_SUFFIX = ".changelog"; //$NON-NLS-1$
	
	public enum Type{
		UPLOAD,
		DOWNLOAD
	};
	public enum Status{
		ACTIVE,
		ERROR,
		DONE,
		NODATA
	};
	
	private ConservationArea ca;
	private ConnectServer server;
	private Date datetime;
	private Type type;
	private Status status;
	private String statusUrl;
	private Long startRevision;
	private Long endRevision;
	
	@Transient
	private String errorString;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="connect_uuid", referencedColumnName="uuid")
	public ConnectServer getServer() {
		return server;
	}
	public void setServer(ConnectServer server) {
		this.server = server;
	}
	
	@Column(name="datetime")
	public Date getDatetime() {
		return datetime;
	}
	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}
	
	@Column(name="sync_type")
	@Enumerated(EnumType.STRING)
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	
	@Column(name="status_url")
	public String getStatusUrl() {
		return statusUrl;
	}
	public void setStatusUrl(String statusUrl) {
		this.statusUrl = statusUrl;
	}
	
	/**
	 * The last revision included in the package.
	 * End revision is inclusive (the packaged included
	 * everything up to and including the end revision).
	 */
	@Column(name="end_revision")
	public Long getEndRevision() {
		return endRevision;
	}
	public void setEndRevision(Long revision) {
		this.endRevision = revision;
	}
	
	/**
	 * start revision is exclusive.  Includes everything greater than start_revision
	 * @return
	 */
	@Column(name="start_revision")
	public Long getStartRevision() {
		return startRevision;
	}
	public void setStartRevision(Long revision) {
		this.startRevision = revision;
	}
	
	/**
	 * DO NOT CALL until after the object has been saved
	 * to the database and the uuid set.
	 * 
	 * @return
	 */
	@Transient
	private String getChangeLogFilePrefix(){
		return CONNECT_FILESTORE_DIR + FileSystems.getDefault().getSeparator() + UuidUtils.uuidToString(getUuid());
	}
	@Transient
	public String getFilestoreDirectory(){
		return getChangeLogFilePrefix() + FileSystems.getDefault().getSeparator() + PACKAGE_FILESTORE_DIR;
	}
	@Transient
	public String getChangeLogZipFile(){
		return getChangeLogFilePrefix() + ".changelog.zip"; //$NON-NLS-1$
	}
	@Transient
	public String getChangeLogFile(){
		return getChangeLogFilePrefix() + CHANGELOG_FILE_SUFFIX;
	}
	@Transient
	public String getChangeLogMetadataFile(){
		return getChangeLogFilePrefix() + METADATA_FILE_SUFFIX;
	}
	
	@Transient
	public String getErrorString(){
		return this.errorString;
	}
	@Transient
	public void setErrorString(String error){
		this.errorString = error;
	}
}
