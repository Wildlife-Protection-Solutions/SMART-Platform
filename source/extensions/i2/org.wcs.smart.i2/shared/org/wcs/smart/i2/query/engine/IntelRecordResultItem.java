/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query.engine;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.query.IResultItem;

/**
 * Record query results item
 * 
 * @author Emily
 *
 */
public class IntelRecordResultItem implements IResultItem {

	private UUID recordUuid;
	private String recordTitle;
	
	private UUID profileUuid;
	private String profileKey;
	private String profileName;
	
	private UUID recordSource;
	private String recordSourceName;
	
	private String recordStatus;
	
	
	private Date primaryDate;
	
	
	
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	private String caId;
	private String caName;
	
	public IntelRecordResultItem(){
		
	}
	
	public void setProfile(String profileKey, UUID profileUuid, String name) {
		this.profileKey = profileKey;
		this.profileName = name;
		this.profileUuid = profileUuid;
	}
	
	public UUID getProfileUuid() {
		return this.profileUuid;
	}
	public String getProfileName() {
		return this.profileName;
	}

	public void setConservationAreaId(String caId) {
		this.caId = caId;
	}
	
	public void setConservationAreaName(String name) {
		this.caName = name;
	}
	
	public String getConservationAreaId() {
		return this.caId;
	}
	
	public String getConservationAreaName() {
		return this.caName;
	}
	
	public void setRecordSource(UUID uuid, String name) {
		this.recordSource = uuid;
		this.recordSourceName = name;
	}
	
	public String getRecordSourceName() {
		return this.recordSourceName;
	}

	public UUID getRecordSourceUuid() {
		return this.recordSource;
	}
	
	public String getRecordStatus(){
		return this.recordStatus;
	}
	public void setRecordStatus(String status){
		this.recordStatus = status;
	}
	public String getRecordTitle(){
		return this.recordTitle;
	}
	public void setRecordTitle(String title){
		this.recordTitle = title;
	}
	public Date getRecordDate(){
		return this.primaryDate;
	}
	public void setRecordDate(Date primaryDate){
		this.primaryDate = primaryDate;
	}
	
	public UUID getRecordUuid(){
		return this.recordUuid;
	}
	public void setRecordUuid(UUID uuid){
		this.recordUuid = uuid;
	}
	
	public String getProfileKey(){
		return this.profileKey;
	}
	
	public void addAttribute(String attributeKey, Object value){
		attributes.put(attributeKey, value);
	}
	
	public Object getAttributeValue(String attributeKey){
		return attributes.get(attributeKey);
	}
	
	
}
