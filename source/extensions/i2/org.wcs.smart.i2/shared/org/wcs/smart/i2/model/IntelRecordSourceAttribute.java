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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.NamedItem;

/**
 * Link between intelligence source and valid
 * attributes for that source;
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.i_recordsource_attribute")
public class IntelRecordSourceAttribute extends NamedItem{

	private IntelRecordSource source;
	private IntelAttribute attribute;
	private IntelEntityType type;
	private int seqOrder;
	private Boolean isMultiple;
	
	public IntelRecordSourceAttribute() {
	}
	
	@Column(name="seq_order")
	public int getOrder(){
		return this.seqOrder;
	}
	public void setOrder(int order){
		this.seqOrder = order;
	}
	
	@ManyToOne
	@JoinColumn(name="source_uuid")
	public IntelRecordSource getSource(){
		return this.source;
	}
	public void setSource(IntelRecordSource source){
		this.source = source;
	}
	
	@ManyToOne
	@JoinColumn(name="attribute_uuid")
	public IntelAttribute getAttribute(){
		return this.attribute;
	}
	public void setAttribute(IntelAttribute attribute){
		this.attribute = attribute;
	}
	
	@ManyToOne
	@JoinColumn(name="entity_type_uuid")
	public IntelEntityType getEntityType(){
		return this.type;
	}
	public void setEntityType(IntelEntityType type){
		this.type = type;
	}
	
	@Column(name="is_multi")
	public Boolean getIsMultiple(){
		return this.isMultiple;
	}
	public void setIsMultiple(Boolean isMultiple){
		this.isMultiple = isMultiple;
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(source, attribute, type);
	}
	
	public boolean equals(Object other){
		if (other == this) return true;
		if (other == null) return false;
		if (!other.getClass().equals(getClass())) return false;
		IntelRecordSourceAttribute o = (IntelRecordSourceAttribute)other;
		return Objects.equals(source, o.source) && Objects.equals(attribute, o.attribute) && Objects.equals(type, o.type);
	}
}
