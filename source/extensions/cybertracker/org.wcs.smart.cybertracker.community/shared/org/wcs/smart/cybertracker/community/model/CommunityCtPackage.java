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
package org.wcs.smart.cybertracker.community.model;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.ICmProvider;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.util.UuidUtils;

/**
 * Patrol cybertracker package configuration
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.ct_community_package")
public class CommunityCtPackage extends AbstractCtPackage implements ICmProvider {

	private static final long serialVersionUID = 1L;
	
	public static final String TYPE_NAME = "COMMUNITY"; //$NON-NLS-1$

	protected String name;
	protected ConservationArea ca;
	
	private ConfigurableModel cm;
	protected CyberTrackerPropertiesProfile ctprofile;

	protected String basemapdef;
	
	protected List<MetadataFieldValue> metadataValues;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
	public ConfigurableModel getConfigurableModel() {
		return this.cm;
	}
	public void setConfigurableModel(ConfigurableModel cm) {
		this.cm = cm;
	}
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ctprofile_uuid", referencedColumnName="uuid")
	public CyberTrackerPropertiesProfile getCtProfile() {
		return this.ctprofile;
	}
	public void setCtProfile(CyberTrackerPropertiesProfile ctprofile) {
		this.ctprofile = ctprofile;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	@Column(name = "name")
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
	 * Users files imported into the SMART system as the basemap
	 * @param files
	 */
	@SuppressWarnings("unchecked")
	@Transient
	public void setBasemapToFiles() {
		JSONObject obj = new JSONObject();
		obj.put(BaseMapKeys.FILE.jsonkey, ICyberTrackerConstants.STR_TRUE);
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
	
	@Transient
	public boolean isDataModel() {
		if (getConfigurableModel() == null) return true;
		return false;
	}
	
	@Transient
	public String getTypeIdentifier() {
		return TYPE_NAME;
	}

	@Transient
	@Override
	public ICtPackage copy() {
		CommunityCtPackage copy = new CommunityCtPackage();
		copy.ca = this.ca;
		copy.cm = this.cm;
		copy.ctprofile = this.ctprofile;
		copy.name = name;
		copy.basemapdef = this.basemapdef;
		
//		if (getMetadataValues() != null) {
//			copy.metadataValues = new ArrayList<>();
//			for (MetadataFieldValue v : getMetadataValues()) {
//				MetadataFieldValue mcopy = new MetadataFieldValue();
//				mcopy.setBooleanValue(v.getBooleanValue());
//				mcopy.setConservationArea(v.getConservationArea());
//				mcopy.setCtPackage(copy);
//				mcopy.setMetadataKey(v.getMetadataKey());
//				mcopy.setStringValue(v.getStringValue());
//				mcopy.setUuidValue(v.getUuidValue());
//				mcopy.setVisible(v.isVisible());
//				
//				if (v.getUuidList() != null) {
//					mcopy.setUuidList(new ArrayList<>());
//					for (MetadataFieldUuidValue uuidValue : mcopy.getUuidList()) {
//						MetadataFieldUuidValue uuidcopy = new MetadataFieldUuidValue();
//						uuidcopy.setMetadata(mcopy);
//						uuidcopy.setUuidValue(uuidValue.getUuidValue());
//						mcopy.getUuidList().add(uuidcopy);
//					}
//				}
//				copy.getMetadataValues().add(mcopy);
//			}
//		}
		return copy;
	}
}
