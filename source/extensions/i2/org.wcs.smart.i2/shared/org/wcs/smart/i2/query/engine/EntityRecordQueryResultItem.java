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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;

/**
 * Intelligence query results item
 * @author Emily
 *
 */
public class EntityRecordQueryResultItem implements IResultItem {

	private UUID entityUuid;
	private LocalDateTime entityLastModified;
	private String entityType;
	private String entityId;
	
	private UUID profileUuid;
	private String profileName;
	
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	private HashMap<IQueryFilter, Boolean> filterValues = new HashMap<>(); 
	
	private String caId;
	private String caName;
	private UUID caUuid;
	
	public EntityRecordQueryResultItem(){
		
	}
	public void setProflie(UUID profileUuid, String name) {
		this.profileUuid = profileUuid;
		this.profileName = name;
	}
	
	public String getProfileName() {
		return this.profileName;
	}
	public UUID getProfileUuid() {
		return this.profileUuid;
	}
	
	public void setConservationAreaUuid(UUID uuid) {
		this.caUuid = uuid;
	}
	public UUID getConservationAreaUuid() {
		return this.caUuid;
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
	
	public void setEntityUuid(UUID entityUuid) {
		this.entityUuid = entityUuid;
	}
	
	public UUID getEntityUuid() {
		return this.entityUuid;
	}
	
	public void setEntityLastModified(LocalDateTime lastModified) {
		this.entityLastModified = lastModified;
	}
	
	public LocalDateTime getEntityLastModified() {
		return this.entityLastModified;
	}
	
	public void setEntityId(String s ) {
		this.entityId = s;
	}
	
	public String getEnityId() {
		return this.entityId;
	}
	
	public void setEntityTypeName(String s ) {
		this.entityType = s;
	}
	
	public String getEnityTypeName() {
		return this.entityType;
	}
	
	
	public void addAttribute(String attributeKey, Object value){
		attributes.put(attributeKey, value);
	}
	
	public Object getAttributeValue(String attributeKey){
		return attributes.get(attributeKey);
	}
	
	public void addFilterValue(IQueryFilter filter, Boolean value){
		filterValues.put(filter, value);
	}
	public HashMap<IQueryFilter, Boolean> getFilterValues(){
		return filterValues;
	}
	
}
