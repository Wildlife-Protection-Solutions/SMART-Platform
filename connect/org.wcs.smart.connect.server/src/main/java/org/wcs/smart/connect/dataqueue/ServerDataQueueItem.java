package org.wcs.smart.connect.dataqueue;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.util.ZonedDateTimeDeserializer;
import org.wcs.smart.connect.util.ZonedDateTimeSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table(name="connect.data_queue")
public class ServerDataQueueItem extends DataQueueItem{

	private static final long serialVersionUID = 1L;
	
	public enum Status{
		UPLOADING("ServerDataQueueItem.Uploading"), //$NON-NLS-1$
		QUEUED("ServerDataQueueItem.Queued"), //$NON-NLS-1$
		PROCESSING("ServerDataQueueItem.Processing"), //$NON-NLS-1$
		COMPLETE("ServerDataQueueItem.Complete"), //$NON-NLS-1$
		ERROR("ServerDataQueueItem.Error"); //$NON-NLS-1$
		
		private String guiName;
		
		private Status(String guiName){
			this.guiName = guiName;
		}
		
		public String getGuiName(Locale l){
			return Messages.getString(guiName, l);
		}
	}
	
	private ZonedDateTime uploadedDate;
	private ZonedDateTime lastModifiedDate;
	private String uploadedBy;
	
	private String file;
	private Status status;
	private String statusMessage;
	private UUID workItem;
	
	@Column(name="uploaded_date")
	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)  
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	public ZonedDateTime getUploadedDate(){
		return this.uploadedDate;
	}
	
	public void setUploadedDate(ZonedDateTime date){
		this.uploadedDate = date;
	}
	
	@Column(name="lastmodified_date")
	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)  
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	public ZonedDateTime getLastModified(){
		return this.lastModifiedDate;
	}
	
	public void setLastModified(ZonedDateTime date){
		this.lastModifiedDate = date;
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
