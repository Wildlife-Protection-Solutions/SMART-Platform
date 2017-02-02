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
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="smart.i_record_attribute_value_list")
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
		
	@Embeddable
	public static class IntelRecordAttributeValueListPk implements Serializable{
		private static final long serialVersionUID = 1L;
		private IntelRecordAttributeValue valueItem;
		private UUID uuid;
		
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
			return this.uuid;
		}
		public void setElementUuid(UUID uuid){
			this.uuid = uuid;
		}
	}
}
