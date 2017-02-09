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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.map.GeometryFactoryProvider;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Class that contains "value" fields for classes that record attribute
 * values.  The underlying table widget must have the following fields: string_value, double_value and list_item_uuid 
 * @author Emily
 *
 */
@MappedSuperclass
public abstract class IntelValueItem {
	
	protected String stringValue;
	protected Double doubleValue;
	protected Double doubleValue2;
	protected IntelAttributeListItem listItem;
	
	@ManyToOne(fetch =FetchType.LAZY)
	@JoinColumn(name="list_item_uuid", referencedColumnName="uuid")
	public IntelAttributeListItem getAttributeListItem(){
		return this.listItem;
	}
	public void setAttributeListItem(IntelAttributeListItem listItem){
		this.listItem = listItem;
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
	
	@Column(name="double_value2")
	public Double getNumberValue2(){
		return this.doubleValue2;
	}
	public void setNumberValue2(Double doubleValue){
		this.doubleValue2 = doubleValue;
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
	/**
	 * 
	 * @return the value of the observation based
	 * on the attribute type.
	 */
	@Transient
	public Object getAttributeValue(){
		AttributeType type = getAttribute().getType();
		switch(type){
			case BOOLEAN:
			case NUMERIC:
				return getNumberValue();
			case DATE:
				return getDateValue();
			case LIST:
				return getAttributeListItem();
			case POSITION:
				return GeometryFactoryProvider.getFactory().createPoint(new Coordinate(getNumberValue(), getNumberValue2()));
			case TEXT:
				return getStringValue();
		}
		throw new IllegalStateException("Invalid attribute type"); //$NON-NLS-1$
	}
	
	/**
	 * 
	 * @return the attribute whose value is being represented 
	 */
	@Transient
	public abstract IntelAttribute getAttribute();
}
