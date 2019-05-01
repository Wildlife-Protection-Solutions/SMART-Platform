/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.model;

import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.util.UuidUtils;

@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractCtPackage extends UuidItem implements ICtPackage {

	public enum BaseMapKeys{
		FILE("file"), //$NON-NLS-1$
		BM("smartbasemap"), //$NON-NLS-1$
		MINZOOM("minzoom"), //$NON-NLS-1$
		MAXZOOM("maxzoom"), //$NON-NLS-1$
		XMAX("xmax"), //$NON-NLS-1$
		XMIN("xmin"), //$NON-NLS-1$
		YMAX("ymax"), //$NON-NLS-1$
		YMIN("ymin"); //$NON-NLS-1$
		
		public String jsonkey;
		
		BaseMapKeys(String jsonkey){
			this.jsonkey = jsonkey;
		}
	}
	
	protected String name;
	protected ConservationArea ca;
	
	protected CyberTrackerPropertiesProfile ctprofile;

	protected boolean hasIncident;
	protected ConfigurableModel incidentmodel;
	
	protected String basemapdef;
	
	protected List<MetadataFieldValue> metadataValues;

	@OneToMany(fetch = FetchType.LAZY, cascade= {CascadeType.ALL}, orphanRemoval = true, mappedBy="ctPackage")
	public List<MetadataFieldValue> getMetadataValues(){
		return this.metadataValues;
	}
	
	public void setMetadataValues(List<MetadataFieldValue> values) {
		this.metadataValues = values;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ctprofile_uuid", referencedColumnName="uuid")
	public CyberTrackerPropertiesProfile getCtProfile() {
		return this.ctprofile;
	}
	public void setCtProfile(CyberTrackerPropertiesProfile ctprofile) {
		this.ctprofile = ctprofile;
	}
	
	@Column(name = "has_incident")
	public boolean getHasIncident() {
		return this.hasIncident;
	}
	public void setHasIncident(boolean hasIncident) {
		this.hasIncident = hasIncident;
	}
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="incident_uuid", referencedColumnName="uuid")
	public ConfigurableModel getIncidentModel() {
		return this.incidentmodel;
	}
	public void setIncidentModel(ConfigurableModel incidentmodel) {
		this.incidentmodel = incidentmodel;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Column(name = "basemapdef")
	public String  getBasemapDef() {
		return this.basemapdef;
	}
	public void setBasemapDef(String basemapdef) {
		this.basemapdef = basemapdef;
	}
	
	/**
	 * Sets the basemap to a set of files residing on the uesrs computers
	 * @param files
	 */
	@SuppressWarnings("unchecked")
	@Transient
	public void setBasemap(String files) {
		JSONObject obj = new JSONObject();
		obj.put(BaseMapKeys.FILE.jsonkey, files);
		setBasemapDef(obj.toJSONString());
	}
	
	/**
	 * Sets the basemap definition to smart basemap with various
	 * bounds
	 * @param basemapUuid
	 * @param env
	 * @param minzoom
	 * @param maxzoom
	 */
	@SuppressWarnings("unchecked")
	@Transient
	public void setBasemap(UUID basemapUuid, ReferencedEnvelope env, CoordinateReferenceSystem databaseCrs, int minzoom, int maxzoom ) throws Exception{
		//convert env to latlong
		Envelope e = env;
		if (!CRS.equalsIgnoreMetadata(env.getCoordinateReferenceSystem(), databaseCrs)) {
			try {
				e = JTS.transform(env, CRS.findMathTransform(env.getCoordinateReferenceSystem(), databaseCrs));
			} catch (Exception e1) {
				setBasemapDef(null);
				throw e1;
			}
		}
		JSONObject obj = new JSONObject();
		obj.put(BaseMapKeys.BM.jsonkey, UuidUtils.uuidToString(basemapUuid));
		obj.put(BaseMapKeys.MINZOOM.jsonkey, minzoom);
		obj.put(BaseMapKeys.MAXZOOM.jsonkey, maxzoom);
		obj.put(BaseMapKeys.XMIN.jsonkey, e.getMinX());
		obj.put(BaseMapKeys.XMAX.jsonkey, e.getMaxX());
		obj.put(BaseMapKeys.YMIN.jsonkey, e.getMinY());
		obj.put(BaseMapKeys.YMAX.jsonkey, e.getMaxY());
		
		setBasemapDef(obj.toJSONString());
	}
	
	/**
	 * Clears any exiting basemap settings, setting the basemap to none
	 */
	@Transient
	public void clearBasemap() {
		setBasemapDef(null);
	}
}
