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

import org.hibernate.Session;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICmProvider;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.IIncidentCtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldUuidValue;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.SurveyDesign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Survey cybertracker package configuration
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="ct_survey_package", schema="smart")
public class SurveyCtPackage extends AbstractCtPackage implements ICmProvider, IIncidentCtPackage{

	private static final long serialVersionUID = 1L;
	
	public static final String TYPE_NAME = "SURVEY"; //$NON-NLS-1$

	private SurveyDesign sd;
	protected boolean hasIncident;
	protected ConfigurableModel incidentmodel;
	
	
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
	public void initConfigurableModel(Session session) {
		if (getSurveyDesign() == null) return;
		setSurveyDesign(session.get(SurveyDesign.class, getSurveyDesign().getUuid()));
		
	}

	@Transient
	public boolean isDataModel() {
		if (getSurveyDesign() == null) return true;
		return getSurveyDesign().getConfigurableModel() == null;
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

	@Transient
	@Override
	public String[] getFieldIdentifierKeys() {
		return new String[] {
			MissionMetadataField.LEADER.getJsonKey(),
			MetadataFieldValue.START_DATE_METADATA_KEY,
			MetadataFieldValue.START_TIME_METADATA_KEY
		};
	}
}
