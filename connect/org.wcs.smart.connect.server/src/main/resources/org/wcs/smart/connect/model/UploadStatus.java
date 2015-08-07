package org.wcs.smart.connect.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadStatus extends ConnectUuidItem{

	private UploadItem.Status status;
	private long currentSize;
	private long expectedSize;
	
	public UploadStatus(UploadItem item){
		setUuid(item.getUuid());
		setExpectedSize(item.getTotalBytes());
		setStatus(item.getStatus());
	}
	
	@JsonProperty("status")
	public UploadItem.Status getStatus(){
		return this.status;
	}
	public void setStatus(UploadItem.Status  status){
		this.status = status;
	}
	
	@JsonProperty("current_size")
	public long getCurrentSize(){
		return this.currentSize;
	}
	public void setCurrentSize(long size){
		this.currentSize = size;
	}

	@JsonProperty("expected_size")
	public long getExpectedSize(){
		return this.expectedSize;
	}
	public void setExpectedSize(long size){
		this.expectedSize = size;
	}
}
