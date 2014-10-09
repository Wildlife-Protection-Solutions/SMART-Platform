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

import java.util.HashSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
/**
 * Mission attribute list item model object.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.sampling_unit_attribute_list")
public class SamplingUnitAttributeListItem extends NamedKeyItem {

	private int listOrder;			//order of item in list
	private SamplingUnitAttribute attribute;	//attribute item is associated with
	
	/**
	 * Creates a new attribute list item.
	 */
	public SamplingUnitAttributeListItem(){
		super();
	}
	/**
	 * 
	 * @return set order of the item in the list
	 */
	@Column(name="list_order")
	public int getListOrder(){
		return this.listOrder;
	}
	/**
	 * 
	 * @param listOrder the order of the item in the list
	 */
	public void setListOrder(int listOrder){
		this.listOrder = listOrder;
	}
	

	/**
	 * 
	 * @param attribute the attribute this list item is associated with
	 */
	public void setAttribute(SamplingUnitAttribute attribute){
		this.attribute = attribute;
	}
	
	/**
	 * 
	 * @return the attribute this list item is associated with
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="sampling_unit_attribute_uuid", referencedColumnName="uuid")
	public SamplingUnitAttribute getAttribute(){
		return this.attribute;
	}
	
	/**
	 * Clones an attribute list item
	 * @param newAtt the attribute to associate the cloned list item with
	 * @param oldCa the conservation area of the object being cloned 
	 * @return a cloned attribute list item
	 */
	public SamplingUnitAttributeListItem clone(SamplingUnitAttribute newAttribute, 
			ConservationArea oldCa, 
			String defaultLang){
		
		SamplingUnitAttributeListItem clone = new SamplingUnitAttributeListItem();

		clone.setKeyId(getKeyId());
		clone.setName(getName());
		clone.setNames(new HashSet<Label>());
		clone.setAttribute(newAttribute);
		clone.setListOrder(this.getListOrder());
		
		if (getNames() != null){
			for (Label l : getNames()){
				if (l.getLanguage().getCode().equals(defaultLang)){
					this.updateName(newAttribute.getConservationArea().getDefaultLanguage(), l.getValue());
				}
				for (Language lang : newAttribute.getConservationArea().getLanguages()){
					if (l.getLanguage().isSame(lang) ){
						this.updateName(lang, l.getValue());
						break;
					}
				}
			}
		}

		return clone;
	}
}