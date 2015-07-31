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
package org.wcs.smart.observation.model;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;


/**
 * Waypoint observation attribute.
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.wp_observation_attributes")
@AssociationOverrides({
	@AssociationOverride(name = "id.observation", 
		joinColumns = @JoinColumn(name = "observation_uuid")),
	@AssociationOverride(name = "id.attribute", 
		joinColumns = @JoinColumn(name = "attribute_uuid")) })
public class WaypointObservationAttribute {

	private WaypointObservationAttributePk id = new WaypointObservationAttributePk();

	private AttributeListItem listItem;
	private AttributeTreeNode nodeItem;
	private String sValue;
	private Double dValue;
	
	public WaypointObservationAttribute(){
		
	}
	
	@EmbeddedId
	public WaypointObservationAttributePk getId(){
		return this.id;
	}
	public void setId(WaypointObservationAttributePk id){
		this.id = id;
	}
	
	@Transient
	public WaypointObservation getObservation(){
		return id.getObservation();
	}
	public void setObservation(WaypointObservation observation){
		id.setObservation(observation);
	}
	
	@Transient
	public Attribute getAttribute(){
		return id.getAttribute();
	}
	
	public void setAttribute(Attribute attribute){
		id.setAttribute(attribute);
	}
	
	@ManyToOne(fetch =FetchType.LAZY)
	@JoinColumn(name="list_element_uuid", referencedColumnName="uuid")
	public AttributeListItem getAttributeListItem(){
		return this.listItem;
	}
	public void setAttributeListItem(AttributeListItem item){
		this.listItem = item;
	}
	@ManyToOne(fetch =FetchType.LAZY)
	@JoinColumn(name="tree_node_uuid", referencedColumnName="uuid")
	public AttributeTreeNode getAttributeTreeNode(){
		return this.nodeItem;
	}
	public void setAttributeTreeNode(AttributeTreeNode node){
		this.nodeItem = node;
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
		return this.dValue != null || this.listItem != null || this.nodeItem != null || this.sValue != null;
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
		
		if (other instanceof WaypointObservationAttribute){
			if (id == null){
				return super.equals(other);
			}
			return id.equals(( (WaypointObservationAttribute)other).id);
		}
		return false;
	}
	
	/**
	 * Clones the observation attribute.  Does
	 * not clone the observation field - that must be set
	 * by the function calling clone.
	 */
	public WaypointObservationAttribute clone(){
		WaypointObservationAttribute clone = new WaypointObservationAttribute();
		clone.id = new WaypointObservationAttributePk();
		clone.id.attribute = id.attribute;
		clone.id.attribute.getType(); /*ensure attribute has been loaded*/
		clone.listItem = listItem;
		if (dValue != null){
			clone.dValue = new Double(dValue);
		}
		clone.nodeItem = nodeItem;
		if (sValue != null){
			clone.sValue = new String(sValue);
		}
		return clone;
	}
	
	/**
	 * 
	 * @return the value of the observation based
	 * on the attribute type.
	 */
	@Transient
	public Object getAttributeValue(){
		AttributeType type = getAttribute().getType();
		if (type == AttributeType.BOOLEAN ||
				type == AttributeType.NUMERIC){
			return getNumberValue();
		}else if (type == AttributeType.TEXT){
			return getStringValue();
		}else if (type == AttributeType.LIST){
			return getAttributeListItem();
		}else if (type == AttributeType.TREE){
			return getAttributeTreeNode();
		}else if (type == AttributeType.DATE){
			return getDateValue();
		}
		throw new IllegalStateException("Invalid attribute type"); //$NON-NLS-1$
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
	public Date getDateValue(){
		if (getStringValue() == null){
			return null;
		}
		return java.sql.Date.valueOf(getStringValue());
	}
	
	/**
	 * This calls setStringValue formating the
	 * date as required for SMART
	 * @return
	 */
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
		switch (getAttribute().getType()){
		case TEXT:
			if (getStringValue() != null){
				text = getStringValue();
			}
			break;
		case DATE:
			if (getStringValue() != null){
				text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(getDateValue());
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
		case TREE:
			if (getAttributeTreeNode() != null){
				text = getAttributeTreeNode().getName();
			}
			break;
		}
		return text;
	}
	
	@Embeddable
	private static class WaypointObservationAttributePk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private WaypointObservation observation;
		private Attribute attribute;
		
		public WaypointObservationAttributePk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="observation_uuid")
		public WaypointObservation getObservation(){
			return this.observation;
		}
		public void setObservation(WaypointObservation observation){
			this.observation = observation;
		}
		
		@ManyToOne(fetch =FetchType.LAZY)
		@JoinColumn(name="attribute_uuid", referencedColumnName="uuid")
		public Attribute getAttribute(){
			return this.attribute;
		}
		public void setAttribute(Attribute attribute){
			this.attribute = attribute;
		}
		
		public int hashCode(){
			if (observation == null || attribute == null){
				return super.hashCode();
			}
			return  observation.hashCode() * 31 + attribute.hashCode();
			
		}
		public boolean equals(Object other){			
			if (other instanceof WaypointObservationAttributePk){
				if (observation == null || attribute == null){
					return super.equals(other);
				}
				return observation.equals( ((WaypointObservationAttributePk)other).observation) && attribute.equals( ((WaypointObservationAttributePk)other).attribute); 
			}
			return false;
		}
		
	}
}
