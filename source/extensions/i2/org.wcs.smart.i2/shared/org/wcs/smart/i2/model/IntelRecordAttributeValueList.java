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

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="i_record_attribute_value_list", schema="smart")
public class IntelRecordAttributeValueList {

	private IntelRecordAttributeValueListPk id = new IntelRecordAttributeValueListPk();
	
	public IntelRecordAttributeValueList(){
	}
	
	@EmbeddedId
	public IntelRecordAttributeValueListPk getId(){
		return this.id;
	}
	public void setId(IntelRecordAttributeValueListPk id){
		this.id = id;
	}
		
	@Override
	public int hashCode(){
		return id.hashCode();
	}
	
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;		
		IntelRecordAttributeValueList s = (IntelRecordAttributeValueList)other;
		return Objects.equals(getId(), s.getId());

	}
	
	
	@Embeddable
	public static class IntelRecordAttributeValueListPk implements Serializable{
		private static final long serialVersionUID = 1L;
		
		private IntelRecordAttributeValue valueItem;
		private UUID elementUuid;
		
		public IntelRecordAttributeValueListPk(){
			
		}
		
		@ManyToOne(cascade=CascadeType.ALL)
		@JoinColumn(name="value_uuid")
		public IntelRecordAttributeValue getValue(){
			return this.valueItem;
		}
		public void setValue(IntelRecordAttributeValue valueItem){
			this.valueItem = valueItem;
		}
		
		@Column(name="element_uuid")
		public UUID getElementUuid(){
			return this.elementUuid;
		}
		public void setElementUuid(UUID elementUuid){
			this.elementUuid = elementUuid;
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(elementUuid, valueItem);
		}
		
		@Override
		public boolean equals(Object other){
			if (this == other) return true;
			if (other == null) return false;
			if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;		
			IntelRecordAttributeValueListPk s = (IntelRecordAttributeValueListPk)other;
			return Objects.equals(getElementUuid(), s.getElementUuid()) && 
					Objects.equals(getValue(), s.getValue());
		}
	}
}
