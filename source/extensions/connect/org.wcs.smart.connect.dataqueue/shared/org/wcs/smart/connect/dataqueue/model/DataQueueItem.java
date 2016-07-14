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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.wcs.smart.ca.UuidItem;

/**
 * Data queue item containing fields shared between the server
 * and the local application.
 * 
 * @author Emily
 *
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class DataQueueItem extends UuidItem{

	public enum Type{
		PATROL_XML,
		INCIDENT_XML,
		MISSION_XML,
		JSON_CT,
		JSON_ZLIB_CT,
	}
	
	private Type type;
	private UUID caUuid;
	private String name;

	
	@Column(name="type")
	@Enumerated(EnumType.STRING)
	public Type getType(){
		return this.type;
	}
	
	public void setType(Type type){
		this.type = type;
	}
	
	@Column(name="ca_uuid")
	public UUID getConservationArea(){
		return this.caUuid;
	}
	
	public void setConservationArea(UUID caUuid){
		this.caUuid = caUuid;
	}
	
	@Column(name="name")
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}

	
}
