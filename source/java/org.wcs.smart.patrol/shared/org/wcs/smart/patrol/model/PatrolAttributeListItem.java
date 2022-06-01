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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.NamedKeyIconItem;

/**
 * Attribute list item for patrol attributes of type list.
 * 
 * @author Emily
 * @since 7.0.0
 */
@Entity
@Table(name = "smart.patrol_attribute_list")
public class PatrolAttributeListItem extends NamedKeyIconItem{
	
	private static final long serialVersionUID = 1L;

	private int listOrder;			//order of item in list
	private boolean isActive;		//if item is active or not
	private PatrolAttribute attribute;	//attribute item is associated with
	
	/**
	 * Creates a new attribute list item.
	 */
	public PatrolAttributeListItem(){
		super();
	}
	/**
	 * 0 based
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
	 * @return <code>true</code> if data can be record for this item, <code>false</code> otherwise
	 */
	@Column(name = "is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	/**
	 * @param isActive <code>true</code> if data can be record for this item, <code>false</code> otherwise
	 */
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	
	
	/**
	 * 
	 * @return the attribute this list item is associated with
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="patrol_attribute_uuid")
	public PatrolAttribute getAttribute(){
		return this.attribute;
	}
	/**
	 * 
	 * @param attribute the attribute this list item is associated with
	 */
	public void setAttribute(PatrolAttribute attribute){
		this.attribute = attribute;
	}

}
