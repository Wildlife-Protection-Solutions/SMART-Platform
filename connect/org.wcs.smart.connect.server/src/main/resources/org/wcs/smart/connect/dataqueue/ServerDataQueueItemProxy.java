package org.wcs.smart.connect.dataqueue;

import java.util.Date;
import java.util.UUID;

import org.wcs.smart.connect.dataqueue.model.DataQueueItem;

public class ServerDataQueueItemProxy {

	public UUID uuid;
	private String name;
	private String caName;
	private DataQueueItem.Type type;
	private ServerDataQueueItem.Status status;
	
	private Date uploadedDate;
	private String uploadedBy;
	
	public ServerDataQueueItemProxy(UUID uuid, String name, String caName, DataQueueItem.Type type, ServerDataQueueItem.Status status, Date uploadDate, String uploadBy){
		this.uuid = uuid;
		this.name = name;
		this.caName = caName;
		this.type = type;
		this.status = status;
		this.uploadedDate = uploadDate;
		this.uploadedBy = uploadBy;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getCaName(){
		return this.caName;
	}
	public DataQueueItem.Type getType(){
		return this.type;
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
