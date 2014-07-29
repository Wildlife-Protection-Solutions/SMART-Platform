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
package org.wcs.smart.er.model;

import java.util.ArrayList;
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
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;

/**
 * Mission attribute model object.  These are owned by a conservation
 * area and can be used in multiple surveys.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.mission_attribute")
public class MissionAttribute extends NamedKeyItem{
	
	private ConservationArea ca;
	
	private Attribute.AttributeType type;
	
	private List<MissionAttributeListItem> attributeList;
	
	public MissionAttribute(){
	}

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid")
	public ConservationArea getConservationArea(){
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
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
	
	/**
	 * Only valid for list attributes.
	 * 
	 * @return set of valid list elements
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute", cascade={CascadeType.ALL}, orphanRemoval=true)
	@OrderBy(clause = "list_order")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<MissionAttributeListItem> getAttributeList(){
		return this.attributeList;
	}
	/**
	 * Only valid for list attributes.
	 * 
	 * @param attributeList the set of valid list elements
	 */
	public void setAttributeList(List<MissionAttributeListItem> attributeList){
		this.attributeList = attributeList;
	}
	
	
	/**
	 * Clones an attribute.
	 * @param newCa the new conservation area to associated with the attribute
	 * 
	 * @return a cloned attribute 
	 */
	public MissionAttribute clone(ConservationArea newCa, String defaultLang){
		MissionAttribute clone = new MissionAttribute();

		clone.setKeyId(getKeyId());
		clone.setName(getName());
		clone.setConservationArea(newCa);
		clone.setType(this.getType());
		
		//copy names
		if (getNames() != null){
			for (Label l : getNames()){
				if (l.getLanguage().getCode().equals(defaultLang)){
					this.updateName(newCa.getDefaultLanguage(), l.getValue());
				}
				for (Language lang : newCa.getLanguages()){
					if (l.getLanguage().isSame(lang) ){
						this.updateName(lang, l.getValue());
						break;
					}
				}
			}
		}
		
		//copy list items
		if (this.getAttributeList() != null){
			clone.attributeList = new ArrayList<MissionAttributeListItem>();
			for(MissionAttributeListItem it : this.getAttributeList()){
				clone.attributeList.add(it.clone(clone, this.ca,defaultLang));
			}
		}
		
		
		return clone;
	}

}
