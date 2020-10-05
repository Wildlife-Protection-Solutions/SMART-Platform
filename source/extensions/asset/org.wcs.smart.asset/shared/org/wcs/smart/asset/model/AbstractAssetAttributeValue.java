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
package org.wcs.smart.asset.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Class that contains "value" fields for classes that record attribute
 * values.  The underlying table widget must have the following fields: string_value, double_value and list_item_uuid 
 * @author Emily
 *
 */
@MappedSuperclass
public abstract class AbstractAssetAttributeValue {
	
	protected String stringValue;
	protected Double doubleValue;
	protected Double doubleValue2;
	protected AssetAttributeListItem listItem;
	
	@ManyToOne(fetch =FetchType.LAZY)
	@JoinColumn(name="list_item_uuid", referencedColumnName="uuid")
	public AssetAttributeListItem getAttributeListItem(){
		return this.listItem;
	}
	public void setAttributeListItem(AssetAttributeListItem listItem){
		this.listItem = listItem;
	}
		
	@Column(name="string_value")
	public String getStringValue(){
		return this.stringValue;
	}
	public void setStringValue(String stringValue){
		this.stringValue = stringValue;
	}
	
	@Column(name="double_value1")
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
		try{
			return LocalDate.parse(getStringValue(), DateTimeFormatter.ofPattern("uuuu-MM-dd")); //$NON-NLS-1$
		}catch (Exception ex){
			return null;
		}
	}
	@Transient
	public void setDateValue(LocalDate date){
		if (date == null){
			setStringValue(null);
			return;
		}
		
		setStringValue(DateTimeFormatter.ofPattern("uuuu-MM-dd").format(date)); //$NON-NLS-1$
		
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
				if (getNumberValue() == null || getNumberValue2() == null) return null;
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
	public abstract AssetAttribute getAttribute();
	
	
	@Transient
	public String getAttributeValueAsString(Locale l, CoordinateReferenceSystem crs){
		if(getAttribute() != null){
			AttributeType type = getAttribute().getType();
			switch(type){
			case BOOLEAN:
				if (getNumberValue() > 0.5){
					return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l);
				}else{
					return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
				}
			case DATE:
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format( getDateValue() );
			case LIST:
				return getAttributeListItem().getName();
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
		}
		return null;
	}
}
