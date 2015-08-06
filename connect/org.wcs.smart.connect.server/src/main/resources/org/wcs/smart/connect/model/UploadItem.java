package org.wcs.smart.connect.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="connect.upload_item")
public class UploadItem extends ConnectUuidItem {

	public enum Type {
		CA;
	}
	
	public enum Status{
		UPLOADING,
		PROCESSING,
		COMPLETE,
		ERROR
	}
	private ConservationAreaInfo caInfo;
	
	private Date startTime;
	private long totalBytes;
	private String localFile;
	private Type type;
	private Status status;
	private String message;


	@ManyToOne
	@JoinColumn(name = "ca_uuid")
	public ConservationAreaInfo getConservationAreaInfo(){
		return this.caInfo;
	}
	
	public void setConservationAreaInfo(ConservationAreaInfo info){
		this.caInfo = info;
	}
	
	@Column(name="start_datetime")
	public Date getStartTime(){
		return this.startTime;
	}
	public void setStartTime(Date startTime){
		this.startTime = startTime;
	}
	
	@Column(name="total_bytes")
	public long getTotalBytes(){
		return this.totalBytes;
	}
	public void setTotalBytes(long totalBytes){
		this.totalBytes = totalBytes;
	}
	
	@Column(name="local_filename")
	public String getLocalFilename(){
		return this.localFile;
	}
	public void setLocalFilename(String filename){
		this.localFile = filename;
	}
	
	@Column(name="type")
	@Enumerated(EnumType.STRING)
	public Type getType(){
		return this.type;
	}
	public void setType(Type type){
		this.type = type;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus(){
		return this.status;
	}
	public void setStatus(Status status){
		this.status = status;
	}
	
	@Column(name="message")
	public String getMessage(){
		return this.message;
	}
	public void setMessage(String message){
		this.message = message;
	}
	
}

