package org.wcs.smart.connect.dataqueue;

import java.util.Date;
import java.util.UUID;

import org.wcs.smart.connect.dataqueue.model.DataQueueItem;

public class ServerDataQueueItemProxy extends DataQueueItem{

	private String caName;
	private ServerDataQueueItem.Status status;
	
	private Date uploadedDate;
	private String uploadedBy;
	
	public ServerDataQueueItemProxy(UUID uuid, String name, String caName, DataQueueItem.Type type, ServerDataQueueItem.Status status, Date uploadDate, String uploadBy){
		setUuid(uuid);
		setName(name);
		setType(type);
		this.caName = caName;
		this.status = status;
		this.uploadedDate = uploadDate;
		this.uploadedBy = uploadBy;
	}
	
	public String getCaName(){
		return this.caName;
	}
	
	public ServerDataQueueItem.Status getStatus(){
		return this.status;
	}
	public Date getUploadedDate(){
		return this.uploadedDate;
	}
	public String getUploadedBy(){
		return this.uploadedBy;
	}
}
