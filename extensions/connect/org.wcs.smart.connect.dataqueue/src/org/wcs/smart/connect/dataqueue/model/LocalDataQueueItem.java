package org.wcs.smart.connect.dataqueue.model;

import java.util.Date;
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
		QUEUED,
		DOWNLOADING,
		PROCESSING,
		COMPLETE,
		ERROR
	}
	
	private Status localStatus;
	private String file;
	private UUID serverUuid;
	private Integer order;
	private String errorMessage;
	private Date dateProcessed;
	
	@Column(name="date_processed")
	public Date getDateProcessed(){
		return this.dateProcessed;
	}
	public void setDateProcessed(Date dateProcessed) {
		this.dateProcessed = dateProcessed;
	}
	
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
	
	
	@Column(name="server_item_uuid")
	public UUID getServerItemUuid(){
		return this.serverUuid;
	}
	
	public void setServerItemUuid(UUID serverUuid){
		this.serverUuid = serverUuid;
	}
	
	@Column(name="queue_order")
	public Integer getOrder(){
		return this.order;
	}
	
	public void setOrder(Integer order){
		this.order = order;
	}
	
	@Column(name="error_message")
	public String getErrorMessage(){
		return this.errorMessage;
	}
	
	public void setErrorMessage(String message){
		this.errorMessage = message;
	}
}
