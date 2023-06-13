/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.dataqueue.model;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.internal.Messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Local data queue item.  Extends the shared data queue item adding
 * additional fields specific to processing the local datastore.
 * @author Emily
 *
 */
@Entity
@Table(name="connect_data_queue", schema="smart")
public class LocalDataQueueItem extends DataQueueItem{

	private static final long serialVersionUID = 1L;
	
	public enum Status{
		QUEUED (Messages.LocalDataQueueItem_QueuedStatusLabel),
		REQUEUED (Messages.LocalDataQueueItem_RequeuedStatusLabel),
		DOWNLOADING (Messages.LocalDataQueueItem_DownloadingStatusLabel),
		PROCESSING (Messages.LocalDataQueueItem_ProcessingStatusLabel),
		COMPLETE (Messages.LocalDataQueueItem_CompleteStatusLabel),
		COMPLETE_WARN (Messages.LocalDataQueueItem_WarningStatusLabel),
		ERROR (Messages.LocalDataQueueItem_ErrorStatusLabel);
		
		private String guiName;
		
		Status(String guiName){
			this.guiName = guiName;
		}
		
		public String getGuiName(){
			return this.guiName;
		}
		
		public Image getImage(){
			if (this == ERROR){
				return ConnectDataQueuePlugin.getDefault().getImageRegistry().get(ConnectDataQueuePlugin.ERROR_ICON);
			}else if (this == DOWNLOADING || this == PROCESSING){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON);
			}else if (this == QUEUED || this == REQUEUED){
				return ConnectDataQueuePlugin.getDefault().getImageRegistry().get(ConnectDataQueuePlugin.QUEUED_ICON);
			}else if (this == COMPLETE){
				return ConnectDataQueuePlugin.getDefault().getImageRegistry().get(ConnectDataQueuePlugin.COMPLETE_ICON);
			}else if (this == COMPLETE_WARN){
				return ConnectDataQueuePlugin.getDefault().getImageRegistry().get(ConnectDataQueuePlugin.COMPLETE_WARN_ICON);
			}
			return null;
		}
	}
	
	private Status localStatus;
	private String file;
	private UUID serverUuid;
	private Integer order;
	private String errorMessage;
	private LocalDateTime dateProcessed;
	private Status checkOutStatus;
	
	@Column(name="date_processed")
	public LocalDateTime getDateProcessed(){
		return this.dateProcessed;
	}
	public void setDateProcessed(LocalDateTime dateProcessed) {
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
		
	@Transient
	public Path getFullFilePath(){
		if (getFile() == null) return null;
		return FileSystems.getDefault()
				.getPath(SmartContext.INSTANCE.getFilestoreLocation())
				.resolve(getFile());
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
	
	@Transient
	public Status getCheckOutStatus(){
		return this.checkOutStatus;
	}
	
	public void setCheckOutStatus(Status checkOutStatus){
		this.checkOutStatus = checkOutStatus;
	}
}
