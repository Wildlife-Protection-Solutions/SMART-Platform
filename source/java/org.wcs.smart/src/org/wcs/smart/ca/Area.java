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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.geotools.referencing.CRS;
import org.hibernate.annotations.GenericGenerator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Represents an area geometry from one of the conservation
 * area area types (boundary, management area, patrol sector etc.)
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name ="smart.area_geometries")
public class Area {
	
	public static CoordinateReferenceSystem AREA_CRS;
	static{
		try {
			AREA_CRS = CRS.decode("EPSG:4326");
		} catch (NoSuchAuthorityCodeException e) {
			SmartPlugIn.log("Error determining default crs", e);
		} catch (FactoryException e) {
			SmartPlugIn.log("Error determining default crs", e);
		}
	}
	
	/**
	 * Type of the area.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum AreaType{
		CA("Conservation Area Boundary"),
		BA("Buffered Management Area"),
		ADMIN("Administrative Areas"),
		MNGT("Management Sectors"),
		PATRL("Patrol Sectors");
		
		private String name;
		
		private AreaType(String name){
			this.name = name;
		}
		
		public String getGuiName(){
			return this.name;
		}
		
	}
	
	
	private byte[] uuid;
	
	private String id;
	private byte[] geom;
	private ConservationArea ca;
	private AreaType type;
	private Geometry value = null;
	
	public Area(){
		
	}

	/**
	 * 
	 * @return the uuid for the list element
	 */
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid() {
		return uuid;
	}

	
	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}
	
	@Column(name="id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name="geom")
	public byte[] getGeom() {
		return geom;
	}

	public void setGeom(byte[] geom) {
		this.geom = geom;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}

	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}

	@Column(name="area_type")
	@Enumerated(EnumType.STRING)
	public AreaType getType() {
		return type;
	}

	public void setType(AreaType type) {
		this.type = type;
	}

	@Transient
	public Geometry getGeometry() {		
		if (value == null){
			try {
				WKBReader reader = new WKBReader();
				value = reader.read(geom);
				if (value instanceof Polygon){
					value = new MultiPolygon(new Polygon[]{(Polygon)value}, value.getFactory());
				}
			} catch (ParseException e) {
				SmartPlugIn.log("Could not read geometry from database.", e);
			}
		}
		return value;
	}
}
