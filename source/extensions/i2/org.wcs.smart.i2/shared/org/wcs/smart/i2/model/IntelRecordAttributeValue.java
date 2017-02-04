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

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;

/**
 * Provides attribute values for a given record
 * @author Emily
 *
 */
@Entity
@Table(name="smart.i_record_attribute_value")
public class IntelRecordAttributeValue extends UuidItem{

	
	private IntelRecord record;
	private IntelRecordSourceAttribute attribute;
	
	private String stringValue;
	private Double doubleValue;

	private List<IntelRecordAttributeValueList> listItems;
	
	
	/**
	 * Constructor.
	 */
	public IntelRecordAttributeValue() {
	}

		
	@Column(name="string_value")
	public String getStringValue(){
		return this.stringValue;
	}
	public void setStringValue(String stringValue){
		this.stringValue = stringValue;
	}
	
	@Column(name="double_value")
	public Double getNumberValue(){
		return this.doubleValue;
	}
	public void setNumberValue(Double doubleValue){
		this.doubleValue = doubleValue;
	}
	
	@Transient
	public Date getDateValue(){
		if (getStringValue() == null){
			return null;
		}
		try{
			return java.sql.Date.valueOf(getStringValue());
		}catch (Exception ex){
			return null;
		}
	}
	@Transient
	public void setDateValue(Date date){
		if (date == null){
			setStringValue(null);
			return;
		}
		java.sql.Date tmp = new java.sql.Date(date.getTime());
		setStringValue(tmp.toString());
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.value", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<IntelRecordAttributeValueList> getAttributeListItems(){
		return this.listItems;
	}
	public void setAttributeListItems(List<IntelRecordAttributeValueList> listItems){
		this.listItems = listItems;
	}
	
	/**
	 * 
	 * @return the value of the observation based
	 * on the attribute type.
	 */
	@Transient
	public Object getAttributeValue(){
		if(attribute.getAttribute() != null){
			AttributeType type = attribute.getAttribute().getType();
			if (type == AttributeType.BOOLEAN ||
				type == AttributeType.NUMERIC){
				return getNumberValue();
			}else if (type == AttributeType.TEXT){
				return getStringValue();
			}else if (type == AttributeType.LIST){
				return getAttributeListItems();
			}else if (type == AttributeType.DATE){
				return getDateValue();
			}
			throw new IllegalStateException("Invalid attribute type"); //$NON-NLS-1$
		}else if (attribute.getEntityType() != null){
			return getAttributeListItems();
		}
		return null;
	}
	
	/**
	 * 
	 * @return the value of the observation based
	 * on the attribute type.
	 */
	@Transient
	public String getAttributeValueAsString(Locale l){
		if(attribute.getAttribute() != null){
			AttributeType type = attribute.getAttribute().getType();
			switch(type){
			case BOOLEAN:
				if (getNumberValue() > 0.5){
					return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l);
				}else{
					return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
				}
			case DATE:
				return DateFormat.getDateInstance().format(getDateValue());
			case LIST:
				return String.valueOf(listItems.size());
			case NUMERIC:
				return String.valueOf(getNumberValue());
			case TEXT:
				return getStringValue();
			}
		}else if (attribute.getEntityType() != null){
			return String.valueOf(listItems.size());
		}
		return null;
	}
	
	@ManyToOne
	@JoinColumn(name="record_uuid")
	public IntelRecord getRecord() {
		return record;
	}

	public void setRecord(IntelRecord record) {
		this.record = record;
	}
	
	@ManyToOne
	@JoinColumn(name="attribute_uuid")
	public IntelRecordSourceAttribute getAttribute() {
		return attribute;
	}

	public void setAttribute(IntelRecordSourceAttribute attribute) {
		this.attribute = attribute;
	}
	
	
}