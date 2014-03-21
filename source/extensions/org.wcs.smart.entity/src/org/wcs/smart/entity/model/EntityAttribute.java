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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute;

/**
 * Represents a attribute associated
 * with an etity type.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.entity_attribute")
public class EntityAttribute extends NamedKeyItem {

	private Attribute dmAttribute;
	private EntityType entityType;
	private boolean isRequired;
	private boolean isPrimary;
	
	private int order;
	

	public EntityAttribute(){
		
	}
	
	/**
	 * The link to the datamodel
	 * attribute that represents this
	 * entity attribute
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
	 * Link to the entity type
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="entity_type_uuid", referencedColumnName="uuid")
	public EntityType getEntityType() {
		return entityType;
	}


	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	/**
	 * If the attribute is required or not
	 * @return
	 */
	@Column(name="is_required")
	public boolean getIsRequired() {
		return isRequired;
	}


	public void setIsRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	/**
	 * The order of the attributes for the
	 * entity.  Allows users to sort
	 * the order the attributes are displayed
	 * in the UI.
	 * @return
	 */
	@Column(name="attribute_order")
	public int getOrder(){
		return this.order;
	}
	
	public void setOrder(int order){
		this.order = order;
	}
	
	/**
	 * If the attribute is to appear in the
	 * main entity is or if the attribute
	 * is of secondary importance.
	 * 
	 * @return
	 */
	@Column(name="is_primary")
	public boolean getIsPrimary(){
		return this.isPrimary;
	}
	public void setIsPrimary(boolean isPrimary){
		this.isPrimary = isPrimary;
	}
	
	/**
	 * 
	 * @return the names associated with the list element in the
	 * language the platform is running in.
	 */
	@Override
	@Type(type="org.wcs.smart.ca.LabelUserType")
	@Column(name="uuid", insertable=false, updatable=false)
	public String getName() {
		String n = super.getName();
		if (n == null || n.length() == 0){
			if (getDmAttribute() == null){
				return "";
			}
			return getDmAttribute().getName();
		}
		return n;
	}
}
