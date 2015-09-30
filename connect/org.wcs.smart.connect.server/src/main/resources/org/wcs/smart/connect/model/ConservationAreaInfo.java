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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import jdk.nashorn.internal.ir.annotations.Ignore;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Simple Connect conservation area info object.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "connect.ca_info")
public class ConservationAreaInfo {

	public enum Status{
		UPLOADING,
		DATA,
		NODATA
	}
	
	private UUID version;
	private String label;
	private UUID caUuid;
	private Status status;
	private int lockKey;
	
	public ConservationAreaInfo(){
	}
	
	/**
	 * 
	 * @return the uuid for the list element
	 */
	@Id
	@Type(type = "pg-uuid")
	@Column(name="ca_uuid")
	public UUID getUuid() {
		return caUuid;
	}

	public void setUuid(UUID uuid) {
		this.caUuid = uuid;
	}
	
	@Column(name="version")
	@Type(type = "pg-uuid")
	public UUID getVersion(){
		return this.version;
	}
	public void setVersion(UUID version){
		this.version = version;
	}
	
	@Column(name="label")
	public String getLabel(){
		return this.label;
	}
	public void setLabel(String label){
		this.label = label;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus(){
		return this.status;
	}
	public void setStatus(Status status){
		this.status = status;
	}
	
	/**
	 * 
	 * @return the key used to lock the conservation area
	 */
	@Column(name="lock_key")
	@Generated(GenerationTime.INSERT)
	@JsonIgnore
	public int getLockKey(){
		return this.lockKey;
	}
	
	protected void setLockKey(int key){
		this.lockKey = key;
	}
}
