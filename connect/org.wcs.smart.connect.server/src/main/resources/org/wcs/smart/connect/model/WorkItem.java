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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;

import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.Uploader;

/**
 * Smart connect upload item entity.
 * @author Emily
 *
 */
@Entity
@Table(name="connect.work_item")
public class WorkItem extends ConnectUuidItem {

	public enum Type {
		UP_CA,
		UP_SYNC,
		DOWN_CA;
	}
	
	public enum Status{
		UPLOADING,
		PROCESSING,
		COMPLETE,
		ERROR
	}
	private ConservationAreaInfo caInfo;
	
	private Date startTime;
	private long totalBytes;
	private String localFile;
	private Type type;
	private Status status;
	private String message;


	@ManyToOne
	@JoinColumn(name = "ca_uuid")
	public ConservationAreaInfo getConservationAreaInfo(){
		return this.caInfo;
	}
	
	public void setConservationAreaInfo(ConservationAreaInfo info){
		this.caInfo = info;
	}
	
	@Column(name="start_datetime")
	public Date getStartTime(){
		return this.startTime;
	}
	public void setStartTime(Date startTime){
		this.startTime = startTime;
	}
	
	@Column(name="total_bytes")
	public long getTotalBytes(){
		return this.totalBytes;
	}
	public void setTotalBytes(long totalBytes){
		this.totalBytes = totalBytes;
	}
	
	@Column(name="local_filename")
	public String getLocalFilename(){
		return this.localFile;
	}
	public void setLocalFilename(String filename){
		this.localFile = filename;
	}
	
	@Column(name="type")
	@Enumerated(EnumType.STRING)
	public Type getType(){
		return this.type;
	}
	public void setType(Type type){
		this.type = type;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus(){
		return this.status;
	}
	public void setStatus(Status status){
		this.status = status;
	}
	
	@Column(name="message")
	public String getMessage(){
		return this.message;
	}
	public void setMessage(String message){
		this.message = message;
	}
	
	@Transient
	public String getStatusURL(HttpServletRequest request) throws UnsupportedEncodingException{
		return request.getScheme() + "://" + request.getServerName()  //$NON-NLS-1$
				+ ":" + request.getServerPort()  //$NON-NLS-1$
				+ request.getContextPath() 
				+ ConnectRESTApplication.PATH_SEPERATOR + ConnectRESTApplication.APP_PATH + ConnectRESTApplication.PATH_SEPERATOR
				+ Uploader.PATH + "/" //$NON-NLS-1$
				+ URLEncoder.encode(getUuid().toString(), ConnectRESTApplication.UTF8);
	}
}

