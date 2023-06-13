/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.patrol.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Values for custom patrol attributes
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@Entity
@Table(name="patrol_attribute_value", schema="smart")
public class PatrolAttributeValue {

	private PatrolAttributeValuePk id = new PatrolAttributeValuePk();

	private PatrolAttributeListItem listItem;
	private String sValue;
	private Double dValue;
	
	public PatrolAttributeValue(){
		
	}
	
	@EmbeddedId
	public PatrolAttributeValuePk getId(){
		return this.id;
	}
	public void setId(PatrolAttributeValuePk id){
		this.id = id;
	}
	
	@Transient
	public Patrol getPatrol(){
		return id.getPatrol();
	}
	public void setPatrol(Patrol patrol){
		id.setPatrol(patrol);
	}
	
	@Transient
	public PatrolAttribute getPatrolAttribute(){
		return id.getPatrolAttribute();
	}
	
	public void setPatrolAttribute(PatrolAttribute attribute){
		id.setPatrolAttribute(attribute);
	}
	
	@ManyToOne(fetch =FetchType.LAZY)
	@JoinColumn(name="list_item_uuid", referencedColumnName="uuid")
	public PatrolAttributeListItem getAttributeListItem(){
		return this.listItem;
	}
	public void setAttributeListItem(PatrolAttributeListItem item){
		this.listItem = item;
	}

	@Column(name="string_value")
	public String getStringValue(){
		return this.sValue;
	}
	public void setStringValue(String value){
		this.sValue = value;
	}
	
	@Column(name="number_value")
	public Double getNumberValue(){
		return this.dValue;
	}
	public void setNumberValue(Double value){
		this.dValue = value;
	}
	
	public boolean hasValue(){
		return this.dValue != null || this.listItem != null || this.sValue != null;
	}
	
	@Override
	public int hashCode(){
		if (id == null){
			return super.hashCode();
		}
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if (other == null) return false;
		if (this == other) return true;
		if (getClass() != other.getClass()) return false;
		return Objects.equals(id, ((PatrolAttributeValue)other).id);
	}
	

	/**
	 * 
	 * @return the value of the observation based
	 * on the attribute type.
	 */
	@Transient
	public Object getAttributeValue(){
		AttributeType type = getPatrolAttribute().getType();
		if (type == AttributeType.BOOLEAN ||
				type == AttributeType.NUMERIC){
			return getNumberValue();
		}else if (type == AttributeType.TEXT){
			return getStringValue();
		}else if (type == AttributeType.LIST){
			return getAttributeListItem();
		}else if (type == AttributeType.DATE){
			return getDateValue();
		}
		throw new IllegalStateException("Invalid attribute type"); //$NON-NLS-1$
	}
	
	
	/**
	 * 
	 * @return sets the value of the given attribute based on the attribute type
	 * and type of object supplied
	 */
	@Transient
	public void setAttributeValue(Object newValue){
		AttributeType type = getPatrolAttribute().getType();
		switch(type){
		case BOOLEAN:
			if (newValue == null){
				setNumberValue(null);
			}else if (newValue instanceof Boolean){
				if ((Boolean)newValue){
					setNumberValue(1.0);
				}else{
					setNumberValue(0.0);
				}
			}else if (newValue instanceof Double){
				if (((Double)newValue) > 0.5){
					setNumberValue(1.0);
				}else{
					setNumberValue(0.0);
				}
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for boolean attribute"); //$NON-NLS-1$
			}
			break;
		case DATE:
			if (newValue == null){
				setDateValue(null);
			}else if (newValue instanceof LocalDate){
				setDateValue((LocalDate)newValue);
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for date attribute"); //$NON-NLS-1$
			}
			break;
		case LIST:
			if (newValue == null){
				setAttributeListItem(null);
			}else if (newValue instanceof PatrolAttributeListItem){
				setAttributeListItem((PatrolAttributeListItem)newValue);
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for list attribute"); //$NON-NLS-1$
			}
			break;
		case NUMERIC:
			if (newValue == null){
				setNumberValue(null);
			} else if (newValue instanceof Number){
				setNumberValue( ((Number)newValue).doubleValue());
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for numberic attribute"); //$NON-NLS-1$
			}
			break;
		case TEXT:
			if (newValue == null){
				setStringValue(null);
			}else if (newValue instanceof String){
				if (((String)newValue).length() == 0){
					setStringValue(null);	
				}else{
					setStringValue( (String)newValue );
				}
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for string attribute"); //$NON-NLS-1$
			}
			break;
		default:
			throw new IllegalStateException("Invalid attribute type"); //$NON-NLS-1$
		}
		
	}
	
	/**
	 * Date attribute types are stored
	 * as in the string field in the ISO8601 format
	 * of yyyy-mm-dd.  This is a transient
	 * function which converts the string value to 
	 * a date.
	 * 
	 * @return
	 */
	@Transient
	public LocalDate getDateValue(){
		if (getStringValue() == null){
			return null;
		}
		return LocalDate.parse(getStringValue(), DateTimeFormatter.ISO_LOCAL_DATE);	}
	
	/**
	 * This calls setStringValue formating the
	 * date as required for SMART
	 * @return
	 */
	@Transient
	public void setDateValue(LocalDate date){
		if (date == null){
			setStringValue(null);
			return;
		}
		setStringValue(DateTimeFormatter.ISO_LOCAL_DATE.format(date));

	}
	
	
	/**
	 * The string representation of the attribute value based
	 * on the attribute type as follows:
	 * * TEXT - return the text string
	 * * BOOLEAN - return Attribute.BOOLEAN_FALSE_LABEL or BOOLEAN_TRUE_LABEL
	 * * LIST - return the name of the list item or empty string
	 * * TREE - name of the tree node or empty string
	 * * NUMERIC - string representation of numeric value
	 * * DATE - the date string in format default locale medium format
	 * 
	 * @return the string representation of the attribute values.
	 */
	@Transient
	public String getAttributeValueAsString(Locale l){
		String text = ""; //$NON-NLS-1$
		switch (getPatrolAttribute().getType()){
		case TEXT:
			if (getStringValue() != null){
				text = getStringValue();
			}
			break;
		case DATE:
			if (getStringValue() != null){
				text = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(getDateValue());
			}
			break;
		case NUMERIC:
			if (getNumberValue() != null){
				text = String.valueOf(getNumberValue());	
			}
			break;
		case BOOLEAN:
			if (getNumberValue() != null){
				if (getNumberValue() < 0.5){
					text = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
				}else{
					text = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l);
				}
			}
			break;
		case LIST:
			if (getAttributeListItem() != null){
				text = getAttributeListItem().getName();
			}
			break;
		case MLIST:
			throw new IllegalStateException("multi list attributes not supported for patrol attributes"); //$NON-NLS-1$
		case TREE:
			throw new IllegalStateException("tree attributes not supported for patrol attributes"); //$NON-NLS-1$
		}
		return text;
	}
	
	@Embeddable
	public static class PatrolAttributeValuePk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Patrol patrol;
		private PatrolAttribute attribute;
		
		public PatrolAttributeValuePk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="patrol_uuid")
		public Patrol getPatrol(){
			return this.patrol;
		}
		public void setPatrol(Patrol patrol){
			this.patrol = patrol;
		}
		
		@ManyToOne(fetch =FetchType.LAZY)
		@JoinColumn(name="patrol_attribute_uuid", referencedColumnName="uuid")
		public PatrolAttribute getPatrolAttribute(){
			return this.attribute;
		}
		public void setPatrolAttribute(PatrolAttribute attribute){
			this.attribute = attribute;
		}
		
		public int hashCode(){
			if (patrol == null || attribute == null){
				return super.hashCode();
			}
			return Objects.hash(patrol, attribute);
			
		}
		public boolean equals(Object other){	
			if (other == this) return true;
			if (other == null) return false;
			if (other.getClass() != getClass()) return false;
			
			return Objects.equals(patrol, ((PatrolAttributeValuePk)other).patrol) &&
					Objects.equals(attribute, ((PatrolAttributeValuePk)other).attribute);
		}
		
	}
}
