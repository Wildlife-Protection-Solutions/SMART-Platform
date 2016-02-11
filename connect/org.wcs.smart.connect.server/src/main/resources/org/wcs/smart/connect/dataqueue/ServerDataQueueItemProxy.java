package org.wcs.smart.connect.dataqueue;

import java.util.Date;
import java.util.UUID;

import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;

public class ServerDataQueueItemProxy extends DataQueueItem implements Comparable<ServerDataQueueItemProxy>	{

	private String caName;
	private ServerDataQueueItem.Status status;
	private Date lastModifiedDate;
	private Date uploadedDate;
	private String uploadedBy;
	
	public ServerDataQueueItemProxy(UUID uuid, String name, UUID caUuid, String caName, 
			DataQueueItem.Type type, ServerDataQueueItem.Status status, Date lastModifiedDate, Date uploadDate, String uploadBy){
		setUuid(uuid);
		setName(name);
		setType(type);
		setConservationArea(caUuid);
		this.caName = caName;
		this.status = status;
		this.uploadedDate = uploadDate;
		this.uploadedBy = uploadBy;
		this.lastModifiedDate = lastModifiedDate;
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
	public Date getLastModifiedDate(){
		return this.lastModifiedDate;
	}
	public String getUploadedBy(){
		return this.uploadedBy;
	}

	//sort by Date 
	public int compareTo(ServerDataQueueItemProxy compare) {
		Date compareDate = ((ServerDataQueueItemProxy) compare).getUploadedDate(); 
		return this.uploadedDate.compareTo(compareDate);
	}
}
