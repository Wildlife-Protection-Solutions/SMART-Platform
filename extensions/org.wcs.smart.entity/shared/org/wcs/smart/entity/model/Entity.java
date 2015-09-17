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
package org.wcs.smart.entity.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.AttributeListItem;

/**
 * An entity object.  This represents a particular
 * entities and it's associated attributes.
 * 
 * @author Emily
 *
 */
@javax.persistence.Entity
@Table(name="smart.entity")
public class Entity extends UuidItem {

	public static final int NAME_MAX_LENGTH = 1014;
	public static final int ID_MAX_LENGTH = 32;
	public static final int KEY_MAX_LENGTH = 128;

	private EntityType type;
	private String id;
	private Status status;
	private Double x;
	private Double y;
	private AttributeListItem attributeItem;
	private List<EntityAttributeValue> attributes;
	
	public Entity(){
		
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="entity_type_uuid", referencedColumnName="uuid")
	public EntityType getEntityType() {
		return type;
	}

	public void setEntityType(EntityType type) {
		this.type = type;
	}

	/**
	 * Unique entity identifier
	 * @return
	 */
	@Column(name="id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 
	 * @return the entity status
	 */
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * The entity x location if fixed
	 * entity type. This is the Longitude.
	 * @return
	 */
	public Double getX() {
		return x;
	}


	public void setX(Double x) {
		this.x = x;
	}

	/**
	 * The entity y location if fixed
	 * entity type.  This is the Latitude.
	 * @param x
	 */
	public Double getY() {
		return y;
	}

	public void setY(Double y) {
		this.y = y;
	}
	
	/**
	 * The attribute list item that represents this entity
	 * @return
	 */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="attribute_list_item_uuid", referencedColumnName="uuid")
	public AttributeListItem getAttributeListItem(){
		return this.attributeItem;
	}
	
	public void setAttributeListItem(AttributeListItem item){
		this.attributeItem = item;
	}
	
	/**
	 * The attribute values for the given entity
	 * @return
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "id.entity", 
			cascade={CascadeType.ALL}, orphanRemoval = true)
	public List<EntityAttributeValue> getAttributes(){
		return this.attributes;
	}
	public void setAttributes(List<EntityAttributeValue> attributes){
		this.attributes = attributes;
	}
	
	@Transient
	public EntityAttributeValue findAttribute(String eaKeyId){
		if (getAttributes() == null) return null;
		for (EntityAttributeValue v : getAttributes()){
			if (v.getEntityAttribute().getKeyId().equals(eaKeyId)){
				return v;
			}
		}
		return null;
	}
}
