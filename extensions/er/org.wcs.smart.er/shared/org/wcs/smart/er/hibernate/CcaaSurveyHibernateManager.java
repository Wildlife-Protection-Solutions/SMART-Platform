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

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * To be implemented when ccaa analysis implemented for ccaa survey;
 * 
 * @author Emily
 *
 */
public class CcaaSurveyHibernateManager implements ISurveyHibernateManager{

	@Override
	public List<SamplingUnit> getSamplingUnits(SurveyDesign survey, Session s, SamplingUnit.State state) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<MissionTrack> getAdHocMissionTracks(SurveyDesign survey, Session s){
		return null;		
	}
	
	@Override
	public List<SurveyDesignProxy> getSurveyDesignEditorInputs(Session s,
			SurveyDesignFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Survey> getActiveSurveys(Session s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Survey> getActiveSurveys(SurveyDesign sd, Session s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<GeometryType> getSamplingUnitTypes(SurveyDesign sd, Session s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SurveyDesign getSurveyDesign(String key, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MissionAttribute getMissionAttributeByKey(
			String missionAttributeKeyId, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MissionAttributeListItem getMissionAttributeListItenByKey(
			String key, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SamplingUnit getSamplingUnitById(String missionTrackId,
			Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Survey getSurveyById(Session session, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
