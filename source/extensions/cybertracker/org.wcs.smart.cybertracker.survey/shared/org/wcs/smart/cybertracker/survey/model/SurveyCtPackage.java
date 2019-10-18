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
package org.wcs.smart.cybertracker.survey.model;

import java.util.ArrayList;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICmProvider;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldUuidValue;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Survey cybertracker package configuration
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.ct_survey_package")
public class SurveyCtPackage extends AbstractCtPackage implements ICmProvider{

	private static final long serialVersionUID = 1L;
	
	public static final String TYPE_NAME = "SURVEY"; //$NON-NLS-1$

	private SurveyDesign sd;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="sd_uuid", referencedColumnName="uuid")
	public SurveyDesign getSurveyDesign() {
		return this.sd;
	}
	public void setSurveyDesign(SurveyDesign sd) {
		this.sd = sd;
	}
	
	@Transient
	public String getTypeIdentifier() {
		return TYPE_NAME;
	}

	@Transient
	public boolean isDataModel() {
		if (getSurveyDesign() == null || getSurveyDesign().getConfigurableModel() == null) return true;
		return false;
	}
	
	@Override
	@Transient
	public ConfigurableModel getConfigurableModel() {
		return getSurveyDesign().getConfigurableModel();
	}
	
	@Transient
	@Override
	public ICtPackage copy() {
		SurveyCtPackage copy = new SurveyCtPackage();
		copy.setConservationArea(getConservationArea());
		copy.setSurveyDesign(getSurveyDesign());
		copy.setIncidentModel(getIncidentModel());
		copy.setCtProfile(getCtProfile());
		copy.setName(getName());
		copy.setBasemapDef(getBasemapDef());
		copy.setHasIncident(getHasIncident());
		
		if (getMetadataValues() != null) {
			copy.metadataValues = new ArrayList<>();
			for (MetadataFieldValue v : getMetadataValues()) {
				MetadataFieldValue mcopy = new MetadataFieldValue();
				mcopy.setBooleanValue(v.getBooleanValue());
				mcopy.setConservationArea(v.getConservationArea());
				mcopy.setCtPackage(copy);
				mcopy.setMetadataKey(v.getMetadataKey());
				mcopy.setStringValue(v.getStringValue());
				mcopy.setUuidValue(v.getUuidValue());
				mcopy.setVisible(v.isVisible());
				
				if (v.getUuidList() != null) {
					mcopy.setUuidList(new ArrayList<>());
					for (MetadataFieldUuidValue uuidValue : v.getUuidList()) {
						MetadataFieldUuidValue uuidcopy = new MetadataFieldUuidValue();
						uuidcopy.setMetadata(mcopy);
						uuidcopy.setUuidValue(uuidValue.getUuidValue());
						mcopy.getUuidList().add(uuidcopy);
					}
				}
				copy.getMetadataValues().add(mcopy);
			}
		}
		return copy;
	}

}
