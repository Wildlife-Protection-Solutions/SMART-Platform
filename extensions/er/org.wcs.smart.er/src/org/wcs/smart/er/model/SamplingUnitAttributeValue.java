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

import java.io.Serializable;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * Link between a sampling unit attribute, sampling unit and
 * associated attribute value.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.sampling_unit_attribute_value")
@AssociationOverrides({
	@AssociationOverride(name = "id.samplingUnit", 
		joinColumns = @JoinColumn(name = "su_uuid")),
	@AssociationOverride(name = "id.samplingUnitAttribute", 
		joinColumns = @JoinColumn(name = "su_attribute_uuid")) })
public class SamplingUnitAttributeValue {

	private SamplingUnitAttributeValuePk id = new SamplingUnitAttributeValuePk();
	
	private String stringValue = null;
	
	private Double doubleValue = null;

	public SamplingUnitAttributeValue(){
		
	}
	
	@EmbeddedId
	public SamplingUnitAttributeValuePk getId() {
		return this.id;
	}

	public void setId(SamplingUnitAttributeValuePk id) {
		this.id = id;
	}
	
	/**
	 * 
	 * @return the associate sampling unit
	 */
	@Transient
	public SamplingUnit getSamplingUnit(){
		return id.getSamplingUnit();
	}
	
	public void setSamplingUnit(SamplingUnit unit){
		id.setSamplingUnit(unit);
	}
	
	/**
	 * 
	 * @return the associate sampling unit
	 * 
	 */
	@Transient
	public SamplingUnitAttribute getSamplingUnitAttribute(){
		return id.getSamplingUnitAttribute();
	}
	
	public void setSamplingUnitAttribute(SamplingUnitAttribute attribute){
		id.setSamplingUnitAttribute(attribute);
	}
	
	@Column(name = "string_value")
	public String getStringValue(){
		return this.stringValue;
	}
	
	public void setStringValue(String value){
		this.stringValue = value;
	}
	
	@Column(name = "number_value")
	public Double getNumberValue(){
		return this.doubleValue;
	}
	
	public void setNumberValue(Double value){
		this.doubleValue = value;
	}
	
	@Embeddable
	private static class SamplingUnitAttributeValuePk implements Serializable {
		private static final long serialVersionUID = 1L;

		private SamplingUnit samplingUnit;	
		
		private SamplingUnitAttribute attribute;

		public SamplingUnitAttributeValuePk() {
		}

		/**
		 * 
		 * @return the associate sampling unit
		 */
		@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
		@JoinColumn(name = "su_uuid", referencedColumnName = "uuid")
		public SamplingUnit getSamplingUnit(){
			return this.samplingUnit;
		}
		
		public void setSamplingUnit(SamplingUnit unit){
			this.samplingUnit = unit;
		}
		
		/**
		 * 
		 * @return the associate sampling unit
		 */
		@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
		@JoinColumn(name = "su_attribute_uuid", referencedColumnName = "uuid")
		public SamplingUnitAttribute getSamplingUnitAttribute(){
			return this.attribute;
		}
		
		public void setSamplingUnitAttribute(SamplingUnitAttribute attribute){
			this.attribute = attribute;
		}

		@Override
		public boolean equals(Object key) {
			if (!(key instanceof SamplingUnitAttributeValuePk)) {
				return false;
			}
			SamplingUnitAttributeValuePk p = (SamplingUnitAttributeValuePk) key;

			if (p.samplingUnit == null || this.samplingUnit == null
					|| p.attribute == null || this.attribute == null) {

				if (p.samplingUnit == null && this.samplingUnit == null
						&& p.attribute == null && this.attribute == null) {
					return true;
				}
				return false;
			}

			return p.samplingUnit.equals(this.samplingUnit)
					&& p.attribute.equals(this.attribute);
		}

		@Override
		public int hashCode() {
			int code = 0;
			if (samplingUnit != null) {
				code += samplingUnit.hashCode();
			}
			code *= 31;
			if (attribute != null) {
				code += attribute.hashCode();
			}
			return code;
		}
	}
}

