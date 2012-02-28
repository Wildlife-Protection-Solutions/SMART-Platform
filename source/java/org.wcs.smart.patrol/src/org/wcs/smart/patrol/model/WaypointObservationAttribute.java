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
package org.wcs.smart.patrol.model;

import java.io.Serializable;

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
	
	
	@Transient
	public String validate(){
		if (getAttribute().getType() == AttributeType.BOOLEAN){
			if (getAttribute().getIsRequired() && getNumberValue() == null){
				return getAttribute().getName() + " must be provided.";
			}
		}else if (getAttribute().getType() == AttributeType.NUMERIC){
			if (getAttribute().getIsRequired() && getNumberValue() == null){
				return getAttribute().getName() + " must be provided.";
			}
			if (getNumberValue() != null ){
				if (getAttribute().getMinValue() != null){
					if (getNumberValue() < getAttribute().getMinValue()){
						return getAttribute().getName() + " must be greater than " + getAttribute().getMinValue();
					}
				}
				if (getAttribute().getMaxValue() != null){
					if (getNumberValue() > getAttribute().getMaxValue()){
						return getAttribute().getName() + " must be less than " + getAttribute().getMaxValue();
					}
				}
			}
		}else if (getAttribute().getType() == AttributeType.TEXT){
			if (getAttribute().getIsRequired() && getStringValue() == null){
				return  getAttribute().getName() + " must be provided.";
			}
			if (getStringValue() != null && getAttribute().getRegex() != null && getAttribute().getRegex().length() > 0){
				if (!getStringValue().matches(getAttribute().getRegex())){
					return "The value '" + getStringValue() + "' for attribute " + getAttribute().getName() + " does not match the required expression '" + getAttribute().getRegex() + "'";
				}
			}
		}else if (getAttribute().getType() == AttributeType.LIST){
			if (getAttribute().getIsRequired()&& getAttributeListItem() == null){
				return getAttribute().getName() + " must be provided.";
			}
		}else if (getAttribute().getType() == AttributeType.TREE){
			if (getAttribute().getIsRequired() && getAttributeTreeNode() == null){
				return getAttribute().getName() + " must be provided.";
			}
		}
		return null;
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
	
	@Embeddable
	private static class WaypointObservationAttributePk implements Serializable{
		private WaypointObservation observation;
		private Attribute attribute;
		
		public WaypointObservationAttributePk(){
			
		}
		public WaypointObservationAttributePk(WaypointObservation observation, Attribute attribute){
			this.observation = observation;
			this.attribute = attribute;
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
