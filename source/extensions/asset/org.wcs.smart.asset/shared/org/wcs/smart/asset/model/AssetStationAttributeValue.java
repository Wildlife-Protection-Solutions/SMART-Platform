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
 * Model class of Asset Station attributes.  Maps attribute values to stations.
 * 
 * @author egouge
 */
@Entity
@Table(name="asset_station_attribute_value", schema="smart")
public class AssetStationAttributeValue extends AbstractAssetAttributeValue {

	private AssetStationAttributeValuePk id = new AssetStationAttributeValuePk();

	/**
	 * Constructor.
	 */
	public AssetStationAttributeValue() {
	}

	@EmbeddedId
	public AssetStationAttributeValuePk getId(){
		return this.id;
	}
	public void setId(AssetStationAttributeValuePk id){
		this.id = id;
	}
	
	@Transient
	public AssetStation getStation(){
		return id.getStation();
	}
	public void setStation(AssetStation station){
		id.setStation(station);
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
		if (o instanceof AssetStationAttributeValue){
			return this.id.equals(((AssetStationAttributeValue)o).id);
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
	private static class AssetStationAttributeValuePk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private AssetStation station;
		private AssetAttribute attribute;
		

		public AssetStationAttributeValuePk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="station_uuid")
		public AssetStation getStation() {
			return station;
		}

		public void setStation(AssetStation station) {
			this.station = station;
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
			
			AssetStationAttributeValuePk p = (AssetStationAttributeValuePk)key;
			return Objects.equals(station, p.station) && Objects.equals(attribute, p.attribute);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(station, attribute);
		  }
	}
}