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
package org.wcs.smart.er.ui.surveydesign.editor;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.surveydesign.ConfigurableModelComposite;
import org.wcs.smart.er.ui.surveydesign.DescriptionComposite;
import org.wcs.smart.er.ui.surveydesign.DistanceDirectionComposite;
import org.wcs.smart.er.ui.surveydesign.MissionPropertiesComposite;
import org.wcs.smart.er.ui.surveydesign.NameIdComposite;
import org.wcs.smart.er.ui.surveydesign.PropertiesComposite;
import org.wcs.smart.er.ui.surveydesign.StatusComposite;
import org.wcs.smart.er.ui.surveydesign.SurveyDesignComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

/**
 * SurveyDesignCompositeFactory
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class SurveyDesignCompositeFactory {

	private static SurveyDesignCompositeFactory instance;
	
	private SurveyDesignCompositeFactory() {}

	public static SurveyDesignCompositeFactory getInstance() {
		if (instance == null) {
			instance = new SurveyDesignCompositeFactory();
		}
		return instance;
	}

	public SurveyDesignComposite createComposite(PanelType type) {
		
		switch (type) {
		case NAME:
			try(Session s = HibernateManager.openSession()){
				return new NameIdComposite(getSurveyDesigns(s));
			}
		case MODEL:
			try(Session s = HibernateManager.openSession()){
				return new ConfigurableModelComposite(getConfigurableModels(s));
			}
		case STATUS:				return new StatusComposite();
		case MISSION_PROPERTIES:	return new MissionPropertiesComposite();
		case PROPERTIES:			return new PropertiesComposite();
		case DESCRIPTION:			return new DescriptionComposite();
		case DISTANCE:				return new DistanceDirectionComposite();
		default: throw new UnsupportedOperationException(type + " is not supported"); //$NON-NLS-1$
		}
	}

	private List<SurveyDesign> getSurveyDesigns(Session session) {
		return QueryFactory.buildQuery(session, SurveyDesign.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
		
	}

	private List<ConfigurableModel> getConfigurableModels(Session session) {
		return QueryFactory.buildQuery(session, ConfigurableModel.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
	}
	
	/**
	 * The supported panels.
	 */
	public enum PanelType {
		NAME,
		MODEL,
		STATUS,
		MISSION_PROPERTIES,
		PROPERTIES,
		DISTANCE,
		DESCRIPTION;
	}

	
}
