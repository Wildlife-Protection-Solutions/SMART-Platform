/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.ca.ConservationArea;

/**
 * representation of SMART6 entity type 
 * @author Emily
 *
 */
public class EntityTypeItem {

	public enum Type{FIXED,TRANSIENT};
	
	private UUID uuid;
	private String keyId;
	private LocalDate dateCreated;
	private UUID creator;
	private UUID dmUuid;
	private Type type;
	
	private HashMap<UUID, String> names;
	private List<EntityTypeAttributeItem> items;
	
	private ConservationArea ca;
	
	public EntityTypeItem(ConservationArea ca) {
		names = new HashMap<>();
		items = new ArrayList<>();
		this.ca = ca;
	}
	
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	public List<EntityTypeAttributeItem> getAttributes(){
		return items;
	}
	public void addAttribute(EntityTypeAttributeItem item) {
		items.add(item);
	}
	
	public void addName(UUID languageUuid, String value) {
		names.put(languageUuid, value);
	}
	
	public HashMap<UUID, String> getNames(){
		return this.names;
	}
	
	public UUID getUuid() {
		return uuid;
	}


	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getKeyId() {
		return keyId;
	}


	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}


	public LocalDate getDateCreated() {
		return dateCreated;
	}


	public void setDateCreated(LocalDate dateCreated) {
		this.dateCreated = dateCreated;
	}


	public UUID getCreator() {
		return creator;
	}


	public void setCreator(UUID creator) {
		this.creator = creator;
	}


	public UUID getDmUuid() {
		return dmUuid;
	}


	public void setDmUuid(UUID dmUuid) {
		this.dmUuid = dmUuid;
	}


	public Type getType() {
		return type;
	}


	public void setType(Type type) {
		this.type = type;
	}

}
