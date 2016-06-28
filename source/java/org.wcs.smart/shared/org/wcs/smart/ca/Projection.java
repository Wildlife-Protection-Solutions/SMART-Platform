/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004-2008, Refractions Research Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package org.wcs.smart.ca;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Represents a projection option.  A projection contains
 * a name, and wkt definition.
 * 
 * @author egouge
 *
 */
@Entity
@Table(name="smart.ca_projection")
public class Projection extends UuidItem {

	public static final int MAX_NAME_LENGTH = 1024;
	public static final int MAX_DEF_LENGTH = 32672;

	private ConservationArea ca;
	private String name;
	private String definition;
	private boolean isDefault;
	
	private CoordinateReferenceSystem parsedCrs;
	
	/**
	 * Creates a new empty projection
	 */
	public Projection(){
		
	}

	/**
	 * 
	 * @return the conservation area associated
	 * with the projection
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea(){
		return this.ca;
	}
	/**
	 * @param ca
	 */
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * 
	 * @return the projection name
	 */
	@Column(name="name")
	public String getName(){
		return this.name;
	}
	/**
	 * Sets the projection name
	 * @param name
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * 
	 * @return the projection definition
	 */
	public String getDefinition(){
		return this.definition;
		
	}
	/**
	 * 
	 * @param definition the projection definition
	 */
	public void setDefinition(String definition){
		this.definition = definition;
	}
	
	/**
	 * The viewing projection is the projection the system 
	 * displays all data in.
	 * @return if this is the default projection for ca
	 */
	@Column(name="is_default")
	public boolean getIsDefault(){
		return this.isDefault;
	}
	
	
	public void setIsDefault(boolean isDefault){
		this.isDefault = isDefault;
	}
	
	
	@Transient
	public void setParsedCoordinateReferenceSystem(CoordinateReferenceSystem crs){
		this.parsedCrs = crs;
	}
	
	/**
	 * This will return null unless the user has set it explicity using the
	 * set function.
	 * 
	 * @return
	 */
	@Transient
	public CoordinateReferenceSystem getParsedCoordinateReferenceSystem(){
		return this.parsedCrs;
	}
}
