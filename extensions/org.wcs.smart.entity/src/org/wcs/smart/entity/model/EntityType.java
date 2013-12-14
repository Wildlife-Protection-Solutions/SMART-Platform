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

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;

/**
 * Entity type object.  These objects
 * describe entity schemas.
 * 
 * @author Emily
 *
 */
@javax.persistence.Entity
@Table(name="smart.entity_type")
public class EntityType extends NamedItem{

	public enum Status{
		ACTIVE("Active"), INACTIVE("Inactive");
		
		private String guiName;
		
		private Status(String guiName){
			this.guiName = guiName;
		}
		
		public String getGuiName(){
			return this.guiName;
		}
	}
	
	public enum Type{
		FIXED("Fixed"),TRANSIENT("Transient");

		private String guiName;
		
		private Type(String guiName){
			this.guiName = guiName;
		}
		
		public String getGuiName(){
			return this.guiName;
		}
	}
	
	private ConservationArea ca;
	private String keyId;
	private String id;
	private Date dateCreated;
	private Employee creator;
	private Status status;
	private Attribute dmAttribute;
	private Type type;
	
	private List<Entity> entities;
	
	public EntityType(){
	}

	@OneToMany(fetch = FetchType.LAZY)
	public List<Entity> getEntities(){
		return this.entities;
	}
	public void setEntities(List<Entity> entities){
		this.entities = entities;
	}
	/**
	 * 
	 * @return the conservation area associated with the entity type
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}
	
	/**
	 * Sets the conservation area
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}

	/**
	 * The keyid is used for sharing entity models
	 * across conservation areas. 
	 * 
	 * @return the key id associated with the entity type
	 */
	@Column(name="keyid")
	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	/**
	 * type id
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
	 * Date created 
	 * @return
	 */
	@Column(name="date_created")
	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	/**
	 * Users who created the entity type
	 * @return
	 */
	@Column(name="creator_uuid")
	public Employee getCreator() {
		return creator;
	}

	public void setCreator(Employee creator) {
		this.creator = creator;
	}

	/**
	 * The entity status.
	 * @return
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
	 * The object in the data model
	 * that represents this entity.
	 * 
	 * @return
	 */
	@Column(name="dm_attribute_uuid")
	public Attribute getDmAttribute() {
		return dmAttribute;
	}

	public void setDmAttribute(Attribute dmAttribute) {
		this.dmAttribute = dmAttribute;
	}

	/**
	 * The type of the entity (fixed or transient)
	 * @return
	 */
	@Column(name="type")
	@Enumerated(EnumType.STRING)
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
}
