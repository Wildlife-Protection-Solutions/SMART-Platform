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
package org.wcs.smart.asset.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

/**
 * Model class of asset module settings.  Provides a key value pair storage
 * for other asset module settings.  These settings are specific to a Conservation Area. 
 * 
 * @author egouge
 */
@Table
@Entity(name="smart.asset_module_settings")
public class AssetModuleSettings extends UuidItem {

	/**
	 * Value to be stored in meters
	 * 
	 * The maximum distance between an station location and the station 
	 * position for the asset deployment to be associated with that station
	 */
	public static final String STATION_BUFFER_KEY = "station_buffer"; //$NON-NLS-1$
	
	/**
	 * Value to be stored in meters
	 * 
	 * The maximum distance between an asset deployment location and the location
	 * position for the asset deployment to be associated with that station location
	 */
	public static final String LOCATION_BUFFER_KEY = "location_buffer"; //$NON-NLS-1$
	
	/**
	 * Columns to be included in the overview map
	 */
	public static final String OVERVIEW_MAP_COLUMN_KEY = "overview_map_columns"; //$NON-NLS-1$
	
	public static final int STATION_BUFFER_DEFAULT_VALUE = 50;
	public static final int LOCATION_BUFFER_DEFAULT_VALUE = 5;

	
	private ConservationArea conservationArea;
	private String key;
	private String value;

	/**
	 * Constructor.
	 */
	public AssetModuleSettings() {
	}

	/**
	 * Get the conservation_area.
	 * 
	 * @return conservation_area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.conservationArea;
	}
	
	/**
	 * Set the conservation_area.
	 * 
	 * @param ca
	 *            conservation_area
	 */
	public void setConservationArea(ConservationArea ca) {
		this.conservationArea = ca;
	}

	/**
	 * Get the settiong key
	 * 
	 * @return key
	 */
	@Column(name="keyid")
	public String getKeyId() {
		return this.key;
	}
	
	/**
	 * Set the key.
	 * 
	 * @param key
	 *            key
	 */
	public void setKeyId(String key) {
		this.key = key;
	}



	/**
	 * Get the setting value.
	 * 
	 * @return value
	 */
	@Column(name="value")
	public String getValue() {
		return this.value;
	}
	/**
	 * Set the value.
	 * 
	 * @param value
	 *            value
	 */
	public void setValue(String value) {
		this.value = value;
	}

}
