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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;

/**
 * Model class for asset station location. An asset station location with a station
 * where an asset is deployed to.

 * 
 * @author egouge
 */
@Entity
@Table(name="smart.asset_station_location")
public class AssetStationLocation extends UuidItem {

	private AssetStation station;
	private String id;
	private Double x;
	private Double y;

	private List<AssetStationLocationAttributeValue> attributes;

	/**
	 * Constructor.
	 */
	public AssetStationLocation() {
		
	}

	
	/**
	 * Get the conservation_area.
	 * 
	 * @return conservation_area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="station_uuid", referencedColumnName="uuid")
	public AssetStation getStation() {
		return this.station;
	}
	
	/**
	 * Set the conservation_area.
	 * 
	 * @param ca
	 *            conservation_area
	 */
	public void setStation(AssetStation station) {
		this.station = station;
	}


	/**
	 * Get the id.  This id should be unique across the Conservation Area.
	 * 
	 * @return id
	 */
	@Column(name="id")
	public String getId() {
		return this.id;
	}
	
	/**
	 * Set the id.  This id should be unique across the Conservation Area
	 * 
	 * @param id
	 */
	
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the longitude position value
	 * 
	 * @return x
	 */
	@Column(name="x")
	public Double getX() {
		return this.x;
	}

	/**
	 * Set the longitude position value.
	 * 
	 * @param x
	 */
	public void setX(Double x) {
		this.x = x;
	}

	/**
	 * Get the latitude position value
	 * 
	 * @return y
	 */
	@Column(name="y")
	public Double getY() {
		return this.y;
	}
	
	/**
	 * Set the latitude position value
	 * 
	 * @param y
	 */
	public void setY(Double y) {
		this.y = y;
	}

	/**
	 * Get the set of the asset_station_attribute_value.
	 * 
	 * @return The set of asset_station_attribute_value
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.stationLocation", orphanRemoval=true, cascade= {CascadeType.ALL})
	public List<AssetStationLocationAttributeValue> getAttributeValues() {
		return this.attributes;
	}


	/**
	 * Set the set of the asset_station_attribute_value.
	 * 
	 * @param attributes
	 */
	public void setAttributeValues(List<AssetStationLocationAttributeValue> attributes) {
		this.attributes = attributes;
	}

}

