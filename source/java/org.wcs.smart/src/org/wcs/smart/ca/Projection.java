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

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.location.GeometryFactoryProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;


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
	
	private CoordinateReferenceSystem crs;

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
		this.crs = null;
		this.definition = definition;
	}
	
	/**
	 * 
	 * @return if this is the default projection for ca
	 */
	@Column(name="is_default")
	public boolean getIsDefault(){
		return this.isDefault;
	}
	
	
	public void setIsDefault(boolean isDefault){
		this.isDefault = isDefault;
	}
	
	
	/**
	 * Parses the definition into a coordinate reference system
	 * object.
	 * 
	 * @return 
	 * @throws FactoryException
	 */
	@Transient
	public CoordinateReferenceSystem getCrs() throws FactoryException{
		if (this.crs != null){
			return crs;
		}
		String def = getDefinition();
		if (def == null){
			return null;
		}
		
		crs = CRS.parseWKT(def);
		return crs;
	}
	
	@Transient
	public void setCrs(CoordinateReferenceSystem crs){
		this.crs = crs;
		this.definition = crs.toWKT();
	}
	
	public static Point transform(double x, double y, Projection targetProjection) {
		Point point = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x, y));
		if (targetProjection == null)
			return point;
		try {
			CoordinateReferenceSystem targetCrs = targetProjection.getCrs();
			
			if (!CRS.equalsIgnoreMetadata(SmartDB.DATABASE_CRS, targetCrs)){
				MathTransform transform = CRS.findMathTransform(SmartDB.DATABASE_CRS, targetCrs);
				Point p = (Point) JTS.transform(point, transform);
				return p;
			}
		} catch (Exception e) {
			SmartPlugIn.log("Failed while converting to view projection's CRS", e); //$NON-NLS-1$
		}
		return point;
	}
	
}
