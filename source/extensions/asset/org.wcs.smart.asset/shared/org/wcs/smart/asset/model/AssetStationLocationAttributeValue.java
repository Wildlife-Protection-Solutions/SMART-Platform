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

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Model class of Asset Station Location attributes.  Maps attribute values to station locations.
 * 
 * @author egouge
 */
@Entity
@Table(name="asset_station_location_attribute_value", schema="smart")
public class AssetStationLocationAttributeValue extends AbstractAssetAttributeValue {

	private AssetStationLocationAttributeValuePk id = new AssetStationLocationAttributeValuePk();

	/**
	 * Constructor.
	 */
	public AssetStationLocationAttributeValue() {
	}

	@EmbeddedId
	public AssetStationLocationAttributeValuePk getId(){
		return this.id;
	}
	public void setId(AssetStationLocationAttributeValuePk id){
		this.id = id;
	}
	
	@Transient
	public AssetStationLocation getStationLocation(){
		return id.getStationLocation();
	}
	public void setStationLocation(AssetStationLocation location){
		id.setStationLocation(location);
	}
	
	@Override
	@Transient
	public AssetAttribute getAttribute(){
		return id.getAttribute();
	}
	
	public void setAttribute(AssetAttribute attribute){
		id.setAttribute(attribute);
	}
	
	/**
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o){
		if (o instanceof AssetStationLocationAttributeValue){
			return this.id.equals(((AssetStationLocationAttributeValue)o).id);
		}
		return false;
	}
	
	/**
	 * @return
	 */
	@Override
	public int hashCode(){
		return id.hashCode();
	}

	/**
	 * Primary key object for category attribute association 
	 * 
	 */
	@Embeddable
	private static class AssetStationLocationAttributeValuePk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private AssetStationLocation location;
		private AssetAttribute attribute;
		

		public AssetStationLocationAttributeValuePk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="station_location_uuid")
		public AssetStationLocation getStationLocation() {
			return location;
		}

		public void setStationLocation(AssetStationLocation location) {
			this.location = location;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="attribute_uuid")
		public AssetAttribute getAttribute() {
			return attribute;
		}

		public void setAttribute(AssetAttribute attribute) {
			this.attribute = attribute;
		}
		
		@Override
		public boolean equals(Object key) {
			if (this == key) return true;
			if (key == null) return false;
			if (!getClass().equals(key.getClass())) return false;
			
			AssetStationLocationAttributeValuePk p = (AssetStationLocationAttributeValuePk)key;
			return Objects.equals(location, p.location) && Objects.equals(attribute, p.attribute);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(location, attribute);
		  }
	}
}