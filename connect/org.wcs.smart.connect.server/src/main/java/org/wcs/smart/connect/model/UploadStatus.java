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
package org.wcs.smart.connect.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Upload status object.
 * 
 * @author Emily
 *
 */
public class UploadStatus extends ConnectUuidItem{

	private WorkItem.Status status;
	private long currentSize;
	private long expectedSize;
	private String message;
	
	public UploadStatus(WorkItem item){
		setUuid(item.getUuid());
		setExpectedSize(item.getTotalBytes());
		setStatus(item.getStatus());
		setMessage(item.getMessage());
	}
	
	@JsonProperty("status")
	public WorkItem.Status getStatus(){
		return this.status;
	}
	public void setStatus(WorkItem.Status  status){
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
	
	@JsonProperty("message")
	public String getMessage(){
		return this.message;
	}
	public void setMessage(String message){
		this.message = message;
	}
}
