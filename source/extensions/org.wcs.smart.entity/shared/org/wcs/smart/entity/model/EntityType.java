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
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.OrderBy;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.entity.IEntityLabelProvider;

/**
 * Entity type object.  These objects
 * describe entity schemas.
 * 
 * @author Emily
 *
 */
@javax.persistence.Entity
@Table(name="smart.entity_type")
public class EntityType extends NamedKeyItem{
	
	public enum Type{
		FIXED,TRANSIENT;

		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IEntityLabelProvider.class).getLabel(this, l);
		}
	}
	
	private ConservationArea ca;
	private Date dateCreated;
	private Employee creator;
	private Status status;
	private Attribute dmAttribute;
	private Type type;
	
	private List<Entity> entities;
	private List<EntityAttribute> attributes;
	
	public EntityType(){
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy="entityType", cascade={CascadeType.ALL}, orphanRemoval = true)
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
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="creator_uuid", referencedColumnName="uuid")
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
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="dm_attribute_uuid", referencedColumnName="uuid")
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
	@Column(name="entity_type")
	@Enumerated(EnumType.STRING)
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	/**
	 * 
	 * @return list of attributes associated with entity
	 * type
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="entityType", cascade={CascadeType.ALL}, orphanRemoval = true)
	@OrderBy(clause="attribute_order")
	public List<EntityAttribute> getAttributes(){
		return this.attributes;
	}
	
	public void setAttributes(List<EntityAttribute> attributes){
		this.attributes = attributes;
	}
	
	/**
	 * Combine the name with the id to generate a 
	 * label for display on the GUI
	 * @return
	 */
	@Transient
	public String getLabel(){
		if (getName() != null){
			return getName(); 
		}
		return getKeyId();
	}
}
