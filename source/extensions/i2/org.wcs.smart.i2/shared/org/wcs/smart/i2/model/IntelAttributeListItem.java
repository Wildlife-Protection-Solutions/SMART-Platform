/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.NamedKeyItem;

/**
 * Model class of attribute list item for intelligence attributes.
 * 
 */
@Entity
@Table(name="smart.i_attribute_list_item")
public class IntelAttributeListItem extends NamedKeyItem {

	private IntelAttribute attribute;

	/**
	 * Constructor.
	 */
	public IntelAttributeListItem() {
	}

	/**
	 * 
	 * @param attribute the attribute this list item is associated with
	 */
	public void setAttribute(IntelAttribute attribute){
		this.attribute = attribute;
	}
	
	/**
	 * 
	 * @return the attribute this list item is associated with
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	public IntelAttribute getAttribute(){
		return this.attribute;
	}

}
