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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * class to represent a SMART6 entity item.
 * 
 * @author Emily
 *
 */
public class EntityItem {

	public enum Status{ACTIVE, INACTIVE};
	
	private UUID uuid;
	private UUID eType;
	private String id;
	private Status status;
	
	private UUID dmUuid;
	
	private List<EntityItemAttribute> attributes;
	
	private Double x;
	private Double y;
	
	public EntityItem() {
		attributes = new ArrayList<>();
	}
	
	public void addAttribute(EntityItemAttribute attribute) {
		this.attributes.add(attribute);
	}
	public List<EntityItemAttribute> getAttributes(){
		return this.attributes;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public UUID geteType() {
		return eType;
	}
	public void seteType(UUID eType) {
		this.eType = eType;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public UUID getDmUuid() {
		return dmUuid;
	}
	public void setDmUuid(UUID dmUuid) {
		this.dmUuid = dmUuid;
	}
	public Double getX() {
		return x;
	}
	public void setX(Double x) {
		this.x = x;
	}
	public Double getY() {
		return y;
	}
	public void setY(Double y) {
		this.y = y;
	}	
}
