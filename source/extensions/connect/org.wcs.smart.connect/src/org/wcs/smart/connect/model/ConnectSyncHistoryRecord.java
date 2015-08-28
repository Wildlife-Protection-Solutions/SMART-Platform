package org.wcs.smart.connect.model;

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

import org.eclipse.core.runtime.Path;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.util.UuidUtils;

@Entity
@Table(name="smart.connect_sync_history")
public class ConnectSyncHistoryRecord extends UuidItem{

	public enum Type{
		UPLOAD,
		DOWNLOAD
	};
	public enum Status{
		ACTIVE,
		ERROR,
		DONE
	};
	
	private ConservationArea ca;
	private ConnectServer server;
	private Date datetime;
	private Type type;
	private Status status;
	private String statusUrl;
	private Long startRevision;
	private Long endRevision;
	
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
	
	/*
	 * end revision is inclusive.  Includes everything including the end revision
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
		return ConnectPlugIn.CONNECT_FILESTORE_DIR + Path.SEPARATOR + UuidUtils.uuidToString(getUuid());
	}
	@Transient
	public String getChangeLogZipFile(){
		return getChangeLogFilePrefix() + ".changelog.zip";
	}
	@Transient
	public String getChangeLogFile(){
		return getChangeLogFilePrefix() + ".changelog";
	}
	@Transient
	public String getChangeLogMetadataFile(){
		return getChangeLogFilePrefix() + ".changelog.metadata";
	}
}
