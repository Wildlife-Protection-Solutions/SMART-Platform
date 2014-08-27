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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;

/**
 * A sampling unit attributes.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.sampling_unit_attribute")
public class SamplingUnitAttribute extends NamedKeyItem{

	/**
	 * Maximum length of string attribute value
	 */
	public static final int MAX_STRING_LENGTH = 1024;
	
	private AttributeType type;
	
	private ConservationArea ca;
	
	/**
	 * 
	 * @return the attribute type
	 */
	@Column(name = "att_type")
	public AttributeType getType(){
		return this.type;
	}
	
	public void setType(AttributeType type){
		this.type = type;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea(){
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea conservationArea){
		this.ca = conservationArea;
	}
}
