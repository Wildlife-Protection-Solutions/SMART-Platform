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
package org.wcs.smart.ca;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Map setting store
 * @author egouge
 * @since 1.0.0
 */

@Entity
@Table(name = "smart.saved_maps")
public class BasemapDefinition extends NamedItem{
		
	private ConservationArea conservationArea;
	
	private String mapDef;
	private boolean isDefault;
	
	public BasemapDefinition(){
		super();
	}

	/**
	 * @return the conservationArea
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}

	/**
	 * @param conservationArea the conservationArea to set
	 */
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}

	/**
	 * @return the mapDef
	 */
	@Column(name="map_def")
	public String getMapDef() {
		return mapDef;
	}

	/**
	 * @param mapDef the mapDef to set
	 */
	public void setMapDef(String mapDef) {
		this.mapDef = mapDef;
	}

	/**
	 * @return if the basemap is default
	 */
	@Column(name = "is_default")
	public boolean getIsDefault(){
		return this.isDefault;
	}
	/**
	 * @param isDefault if the basemap is default
	 */
	public void setIsDefault(boolean isDefault){
		this.isDefault = isDefault;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getUuid().hashCode();
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasemapDefinition other = (BasemapDefinition) obj;
		if (getUuid() == null && other.getUuid() == null) return true;
		if (getUuid() != null && getUuid().equals(other.getUuid())) return true;
		return false;
	}
}
