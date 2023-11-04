/*
 * Copyright (C) 2015 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect.dataqueue;

import java.time.ZonedDateTime;

import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.util.ZonedDateTimeDeserializer;
import org.wcs.smart.connect.util.ZonedDateTimeSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Proxy item for DataQueues
 * @author Emily
 *
 */
public class ServerDataQueueItemProxy extends DataQueueItem implements Comparable<ServerDataQueueItemProxy>	{

	private static final long serialVersionUID = 1L;
	
	private String caName;
	private ServerDataQueueItem.Status status;
	private ZonedDateTime lastModifiedDate;
	private ZonedDateTime uploadedDate;
	private String uploadedBy;
	
	private String statusMessage;
	
	public ServerDataQueueItemProxy(ServerDataQueueItem item, ConservationAreaInfo ca){

		setUuid(item.getUuid());
		setName(item.getName());
		setType(item.getType());
		setConservationArea(item.getConservationArea());
		
		this.caName = ca.getLabel();
		this.status = item.getStatus();
		this.uploadedDate = item.getUploadedDate();
		this.uploadedBy = item.getUploadedBy();
		this.lastModifiedDate = item.getLastModified();
		this.statusMessage = item.getStatusMessage();
	}
	
	public String getCaName(){
		return this.caName;
	}
	
	public ServerDataQueueItem.Status getStatus(){
		return this.status;
	}
	
	public String getStatusMessage() {
		return this.statusMessage;
	}
	
	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)  
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	public ZonedDateTime getUploadedDate(){
		return this.uploadedDate;
	}
	
	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)  
	@JsonSerialize(using = ZonedDateTimeSerializer.class)
	public ZonedDateTime getLastModifiedDate(){
		return this.lastModifiedDate;
	}
	public String getUploadedBy(){
		return this.uploadedBy;
	}

	//sort by Date 
	public int compareTo(ServerDataQueueItemProxy compare) {
		ZonedDateTime compareDate = ((ServerDataQueueItemProxy) compare).getUploadedDate(); 
		return -this.uploadedDate.compareTo(compareDate);
	}
}
