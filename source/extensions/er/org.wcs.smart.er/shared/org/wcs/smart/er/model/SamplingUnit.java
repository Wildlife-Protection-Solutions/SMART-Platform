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
package org.wcs.smart.er.model;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * Sampling unit model object.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.sampling_unit")
public class SamplingUnit extends UuidItem {

	/**
	 * Maximum id length
	 */
	public static final int ID_MAX_LENGTH = 128;
	
	/**
	 * Supported sampling unit types
	 * @author Emily
	 *
	 */
	public enum GeometryType{
		TRANSECT ,
		PLOT;
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(this, l);
		}
	}
	
	public enum State{
		ACTIVE,
		INACTIVE;
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(this, l);
		}
	}

	private byte[] geom;
	private Geometry geometry;
	private String id;
	private SurveyDesign surveyDesign;
	private GeometryType type;
	private List<SamplingUnitAttributeValue> attributes;
	private State state;
	
	
	public SamplingUnit(){
		
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="survey_design_uuid", referencedColumnName="uuid")
	public SurveyDesign getSurveyDesign(){
		return this.surveyDesign;
	}
	
	public void setSurveyDesign(SurveyDesign survey){
		this.surveyDesign = survey;
	}
	
	@Column(name="geometry")
	@Lob
	@Basic(fetch = FetchType.LAZY)
	public byte[] getGeom() {
		return geom;
	}

	public void setGeom(byte[] geom) {
		this.geom = geom;
		this.geometry = null;
	}

	@Transient
	public Geometry getGeometry() throws Exception{
		if (geometry == null && geom != null){
			WKBReader reader = new WKBReader();
			geometry = (Geometry)reader.read(geom);
		}
		return geometry;
	}

	/**
	 * Sets the geometry.  The geometry should be a linestring or
	 * point in the 4326 projection.
	 * 
	 * @param geometry
	 */
	public void setGeometry(Geometry geometry) {
		if (geometry == null){
			this.geometry = null;
			this.geom = null;
			return;
		}
		
		if (geometry instanceof Point || geometry instanceof LineString){
			WKBWriter writer = new WKBWriter(3);
			this.geom = writer.write(geometry);
			this.geometry = geometry;
		}else{
			throw new RuntimeException(
					MessageFormat.format("{0} geometries are not supported for sampling units.",  //$NON-NLS-1$
							new Object[]{geometry.getClass().getName()}));
		}
	}

	/**
	 * @return the geometry length only if geometry represents a linestring; otherwise
	 * null is returned
	 */
	@Transient
	public Double getGeometryLengthKm() throws Exception{
		Geometry g = getGeometry();
		if (g != null){
			if (g instanceof LineString){
				return (GeometryUtils.distanceInMeters((LineString)g) / 1000.0);
			}
		}
		return null;
	}
	
	@Column(name="id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * associated survey design
	 * @return
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.samplingUnit", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<SamplingUnitAttributeValue> getAttributes() {
		return attributes;
	}
	public void setAttributes(List<SamplingUnitAttributeValue> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * 
	 * @return the attribute type
	 */
	@Column(name="unit_type")
	@Enumerated(EnumType.STRING)
	public GeometryType getType() {
		return type;
	}
	/**
	 * 
	 * @param type the attribute type
	 */
	public void setType(GeometryType type) {
		this.type = type;
	}
	
	/**
	 * The sampling unit state
	 * @return
	 */
	@Column(name="state")
	@Enumerated(EnumType.STRING)
	public State getState(){
		return state;
	}
	
	public void setState(State state){
		this.state = state;
	}
	

}
