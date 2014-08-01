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
package org.wcs.smart.er.hibernate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyDesignListFilter;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Cross conservation area analysis hibernate manager.
 * 
 * @author Emily
 *
 */
public class CcaaFieldSurveyHibernateManager implements IFieldSurveyHibernateManager{

	
	/**
	 * Gets all sampling units for a survey across all
	 * conservation areas the survey is applicable for.
	 * Users the survey key to determine surveys to match.
	 * 
	 * This includes all fixed sampling units and reconnaissance
	 * sampling units (represented as tracks).
	 *  
	 *  
	 * @return all sampling units for the given conservation area
	 */
	public List<Object> getSamplingUnits(SurveyDesign survey, Session s){
		
		List<Object> units = new ArrayList<Object>();
		
		//get fixed sampling units
				
		Query q = null;
		StringBuilder sb = new StringBuilder();
		sb.append("FROM SamplingUnit a ");
		sb.append(" WHERE a.survey.conservationArea IN (:ca) ");
		sb.append(" AND a.survey.keyId = :surveyKey ");
		
		q = s.createQuery(sb.toString());
		q.setParameter("surveyKey", survey.getKeyId());
		q.setParameterList("ca", SmartDB.getConservationAreaConfiguration().getConservationAreas());
		
		List<SamplingUnit> unit = q.list();
		units.addAll(unit);

		//get fixed reconnaissance tracks
		// get reconnaissance tracks
		sb = new StringBuilder();
		sb.append("FROM FieldSurveyTrack ");
		sb.append(" WHERE fieldSurvey.survey.conservationArea IN (:ca) ");
		sb.append(" AND fieldSurvey.survey.keyId = :surveyKey ");
		sb.append(" AND type = :type ");
		
		q = s.createQuery(sb.toString());
		q.setParameterList("ca", SmartDB.getConservationAreaConfiguration().getConservationAreas());
		q.setParameter("surveyKey", survey.getKeyId());
		q.setParameter("type", MissionTrack.TrackType.RECON);

		List<MissionTrack> tracks = q.list();
		units.addAll(tracks);

		return units;
	}

	/**
	 * This should merge all surveys with the same key into a single survey.
	 * <p>filter is not applicable and wil be ignored.</p>
	 * 
	 */
	@Override
	public List<SurveyDesignEditorInput> getSurveys(Session s,
			SurveyDesignListFilter filter) {
		
		// TODO Auto-generated method stub
		
		//this should merge all surveys with the same key into a single survey
		//
		return null;
	}

	@Override
	public List<SurveyDesign> getActiveSurveys(Session s) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
