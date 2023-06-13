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

import org.wcs.smart.ca.datamodel.AttributeListItem;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


/**
 * Waypoint observation attribute.
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="wp_observation_attributes_list", schema="smart")
public class WaypointObservationAttributeList {

	private WaypointObservationAttributeListPk id = new WaypointObservationAttributeListPk();
	
	public WaypointObservationAttributeList(){
		
	}
	
	@EmbeddedId
	public WaypointObservationAttributeListPk getId(){
		return this.id;
	}
	public void setId(WaypointObservationAttributeListPk id){
		this.id = id;
	}
	
	@Transient
	public WaypointObservationAttribute getObservationAttribute(){
		return id.getObservationAttribute();
	}
	public void setObservationAttribute(WaypointObservationAttribute observation){
		id.setObservationAttribute(observation);
	}
	
	@Transient
	public AttributeListItem getAttributeListItem(){
		return id.getAttributeListItem();
	}
	
	public void setAttributeLisItem(AttributeListItem attribute){
		id.setAttributeListItem(attribute);
	}
	
	
	@Embeddable
	private static class WaypointObservationAttributeListPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private WaypointObservationAttribute observationAttribute;
		private AttributeListItem listItem;
		
		public WaypointObservationAttributeListPk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="observation_attribute_uuid")
		public WaypointObservationAttribute getObservationAttribute(){
			return this.observationAttribute;
		}
		public void setObservationAttribute(WaypointObservationAttribute observationAttribute){
			this.observationAttribute = observationAttribute;
		}
		
		@ManyToOne(fetch =FetchType.LAZY)
		@JoinColumn(name="list_element_uuid", referencedColumnName="uuid")
		public AttributeListItem getAttributeListItem(){
			return this.listItem;
		}
		public void setAttributeListItem(AttributeListItem item){
			this.listItem = item;
		}
		
		public int hashCode(){
			if (observationAttribute == null || listItem == null){
				return super.hashCode();
			}
			return  observationAttribute.hashCode() * 31 + listItem.hashCode();
			
		}
		public boolean equals(Object other){			
			if (other instanceof WaypointObservationAttributeListPk){
				if (observationAttribute == null || listItem == null){
					return super.equals(other);
				}
				return observationAttribute.equals( ((WaypointObservationAttributeListPk)other).observationAttribute) && listItem.equals( ((WaypointObservationAttributeListPk)other).listItem); 
			}
			return false;
		}
		
	}
	
	
}
