package org.wcs.smart.connect.dataqueue.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name="smart.connect_data_queue")
public class LocalDataQueueItem extends DataQueueItem{

	public enum Status{
		DOWNLOADING,
		PROCESSING,
		COMPLETE,
		ERROR
	}
	
	private Status localStatus;
	private String file;
	
	
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus(){
		return this.localStatus;
	}
	
	public void setStatus(Status localStatus){
		this.localStatus = localStatus;
	}
		
	@Column(name="local_file")
	public String getFile(){
		return this.file;
	}
	
	public void setFile(String file){
		this.file = file;
	}
	
	
}
