/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.patrol.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OrderBy;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;


/**
 * Custom attributes for patrols
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@Entity
@Table(name = "smart.patrol_attribute")
public class PatrolAttribute extends NamedKeyItem{

	private ConservationArea conservationArea;
	private Attribute.AttributeType type;
	private boolean isActive;
	private List<PatrolAttributeListItem> attributeList = null;
	/**
	 * 
	 * @return the conservation area associated with the attribute
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	/**
	 * 
	 * @param ca the conservation area to be associated with the attribute
	 */
	public void setConservationArea(ConservationArea ca) {
		this.conservationArea = ca;
	}
	
	/**
	 * 
	 * @return the attribute type
	 */
	@Column(name="att_type")
	@Enumerated(EnumType.STRING)
	public AttributeType getType() {
		return type;
	}
	/**
	 * 
	 * @param type the attribute type
	 */
	public void setType(AttributeType type) {
		this.type = type;
	}
	
	@Column(name="is_active")
	public boolean getIsActive() {
		return this.isActive;
	}
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * @return set to list items
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute", cascade={CascadeType.ALL}, orphanRemoval=true)
	@OrderBy(clause = "list_order")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<PatrolAttributeListItem> getAttributeList(){
		return this.attributeList;
	}
	/**
	 * 
	 * @param attributeList the set of valid list elements
	 */
	public void setAttributeList(List<PatrolAttributeListItem> attributeList){
		this.attributeList = attributeList;
	}
}
