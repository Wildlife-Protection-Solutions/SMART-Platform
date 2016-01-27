package org.wcs.smart.connect.dataqueue;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.wcs.smart.connect.dataqueue.model.DataQueueItem;

@Entity
@Table(name="connect.data_queue")
public class ServerDataQueueItem extends DataQueueItem{

	public enum Status{
		UPLOADING,
		QUEUED,
		PROCESSING,
		COMPLETE,
		ERROR
	}
	
	private Date uploadedDate;
	private String uploadedBy;
	
	private String file;
	private Status status;
	private String statusMessage;
	private UUID workItem;
	
	@Column(name="uploaded_date")
	public Date getUploadedDate(){
		return this.uploadedDate;
	}
	
	public void setUploadedDate(Date date){
		this.uploadedDate = date;
	}
	
	@Column(name="uploaded_by")
	public String getUploadedBy(){
		return this.uploadedBy;
	}
	public void setUploadedBy(String uploadedBy){
		this.uploadedBy = uploadedBy;
	}
	
	@Column(name="file")
	public String getFile(){
		return this.file;
	}
	public void setFile(String file){
		this.file = file;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus(){
		return this.status;
	}
	public void setStatus(Status status){
		this.status = status;
	}
	
	@Column(name="status_message")
	public String getStatusMessage(){
		return this.statusMessage;
	}
	public void setStatusMessage(String statusMessage){
		this.statusMessage = statusMessage;
	}
	
	@Column(name="work_item_uuid")
	public UUID getWorkItem(){
		return this.workItem;
	}
	public void setWorkItem(UUID workItemUuid){
		this.workItem = workItemUuid;
	}
}
