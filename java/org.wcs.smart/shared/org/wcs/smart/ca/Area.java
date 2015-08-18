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

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.util.GeometryUtils;

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
public class Area extends NamedItem{
	
	public static final int KEY_MAX_LENGTH = 256;
	public static final int NAME_MAX_LENGTH = 256;
	
	public static CoordinateReferenceSystem AREA_CRS = GeometryUtils.SMART_CRS;
		
	/**
	 * Type of the area.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum AreaType{
		CA,
		BA,
		ADMIN,
		MNGT,
		PATRL;
	}
	
	private String key;
	private byte[] geom;
	private ConservationArea ca;
	private AreaType type;
	private Geometry value = null;
	
	
	public Area(){
		
	}

	@Column(name="keyid")
	public String getKeyId() {
		return key;
	}

	public void setKeyId(String key) {
		this.key = key;
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
				//TODO: figure out logging here
				//SmartPlugIn.log(Messages.Area_Error_LoadingGeom, e);
			}
		}
		return value;
	}
	
	
	/**
	 * Generates a key for a area based on the name provided.
	 * 
	 * @param value the name 
	 * @param otherValues list keys this key must be different from
	 * 
	 * @return valid key
	 */
	public static String generateKey (String value, String defaultValue, Collection<String> otherValues){
		String raw = value.toLowerCase().replaceAll("[^a-z0-9_]", ""); //$NON-NLS-1$ //$NON-NLS-2$	
		if (raw.isEmpty()){
			//raw = Messages.Area_EmptyKey;
			raw = defaultValue;
		}
	
		int count = 0;
		String key = raw;
		if (key.substring(0, 1).matches("[0-9_]")){ //$NON-NLS-1$
			//cannot start with a digit
			key = "a" + key; //$NON-NLS-1$
		}
		if (key.length() > Area.KEY_MAX_LENGTH){
			key = key.substring(0, Area.KEY_MAX_LENGTH);
		}
		
		String postfix = key; 
		while(otherValues.contains(key)){
			count ++;
			String cnt = String.valueOf(count);
			if (postfix.length() + cnt.length() > Area.KEY_MAX_LENGTH){
				key = postfix.substring(0, Area.KEY_MAX_LENGTH- cnt.length() ) + cnt;
			}else{
				key = postfix + String.valueOf(count);
			}
			
		}
		return key;
	}
}
