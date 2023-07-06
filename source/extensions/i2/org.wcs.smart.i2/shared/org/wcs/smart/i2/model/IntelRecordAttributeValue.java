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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Provides attribute values for a given record
 * @author Emily
 *
 */
@Entity
@Table(name="i_record_attribute_value", schema="smart")
public class IntelRecordAttributeValue extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	private IntelRecord record;
	private IntelRecordSourceAttribute attribute;
	
	private String stringValue;
	private Double doubleValue;
	private Double doubleValue2;
	
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
	
	@Column(name="double_value2")
	public Double getNumberValue2(){
		return this.doubleValue2;
	}
	public void setNumberValue2(Double doubleValue){
		this.doubleValue2 = doubleValue;
	}
	
	@Transient
	public LocalDate getDateValue(){
		if (getStringValue() == null){
			return null;
		}
		return LocalDate.parse(getStringValue(), DateTimeFormatter.ISO_LOCAL_DATE);
	}
	@Transient
	public void setDateValue(LocalDate date){
		if (date == null){
			setStringValue(null);
			return;
		}
		setStringValue(DateTimeFormatter.ISO_LOCAL_DATE.format(date));
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
			switch(type){
			case BOOLEAN:
			case NUMERIC:
				return getNumberValue();
			case DATE:
				return getDateValue();
			case EMPLOYEE:
			case LIST:
				return getAttributeListItems();
			case POSITION:
				return GeometryFactoryProvider.getFactory().createPoint(new Coordinate(getNumberValue(), getNumberValue2()));
			case TEXT:
				return getStringValue();
			
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
	public String getAttributeValueAsString(Locale l, CoordinateReferenceSystem crs){
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
				LocalDate dvalue = getDateValue();
				if (dvalue == null) return ""; //$NON-NLS-1$
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(dvalue);
			case LIST:
			case EMPLOYEE:
				return String.valueOf(listItems.size());
			case NUMERIC:
				return String.valueOf(getNumberValue());
			case TEXT:
				return getStringValue();
			case POSITION:
				if (crs == null || crs == GeometryUtils.SMART_CRS || CRS.equalsIgnoreMetadata(crs, GeometryUtils.SMART_CRS)){
					return "POINT( " + getNumberValue() +" " + getNumberValue2() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}else{
					try{
						Coordinate c = ReprojectUtils.reproject(getNumberValue(), getNumberValue2(), GeometryUtils.SMART_CRS, crs);
						return "POINT( " + c.x + " " + c.y + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}catch (Exception ex){
						return "ERROR: " + ex.getMessage(); //$NON-NLS-1$
					}
				}
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