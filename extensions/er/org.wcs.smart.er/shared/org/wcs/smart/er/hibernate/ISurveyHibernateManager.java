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
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;

public interface ISurveyHibernateManager {

	/**
	 * Gets all sampling units for a conservation area and survey with
	 * the given state.  If the state is null then all sampling units
	 * will be returned.  It does not include reconnissance mission tracks.
	 *  
	 * <p>
	 * If in CCAA mode, will return all sampling units in all
	 * conservation ares whose survey keys match.
	 * </p>
	 *  
	 * @return all sampling units for the given conservation area
	 */
	public List<SamplingUnit> getSamplingUnits(SurveyDesign survey, Session s, SamplingUnit.State state);

	/**
	 * Returns all mission tracks that are not associated with a sampling
	 * unit.
	 * 
	 * @param mission
	 * @param s
	 * @return
	 */
	public List<MissionTrack> getAdHocMissionTracks(SurveyDesign survey, Session s);

	/**
	 * Returns all surveys design that match the given filter.  If the filter
	 * is not provided all surveys are returned.
	 * 
	 * @param s
	 * @param filter filter or null if not filter should be applied
	 * @return
	 */
	public List<SurveyDesignProxy> getSurveyDesignEditorInputs(Session s, 
			SurveyDesignFilter filter);
	
//	/**
//	 * Returns all surveys that match the given filter.  If the filter
//	 * is not provided all surveys are returned.
//	 * 
//	 * @param s
//	 * @param filter filter or null if not filter should be applied
//	 * @return
//	 */
//	public List<SurveyProxy> getSurveys(Session s, SurveyFilter filter);
	
	
	/**
	 * Returns all surveys associated with an active survey design
	 * for the current conservation area.
	 * 
	 * @param s
	 * @return
	 */
	public List<Survey> getActiveSurveys(Session s);
	
	/**
	 * Returns all surveys associated with an active survey design
	 * for the current conservation area and the given survey design
	 * 
	 * @param s
	 * @return
	 */
	public List<Survey> getActiveSurveys(SurveyDesign sd, Session s);
	
	/**
	 * Returns a set of sampling unit types applicable
	 * for the given survey design 
	 */
	public Set<SamplingUnit.GeometryType> getSamplingUnitTypes(SurveyDesign sd, Session s);
	
	/**
	 * Finds the survey design with the given key.
	 * @param key survey design key
	 * @param session
	 * @return the associated survey design or null
	 */
	public SurveyDesign getSurveyDesign(String key, Session session);


	/**
	 * Finds the mission attribute with the given key.
	 * @param key mission attribute key
	 * @param session
	 * @return the associated mission attribute or null
	 */
	public MissionAttribute getMissionAttributeByKey(String missionAttributeKeyId, Session session);


	/**
	 * Finds the mission attribute list item with the given key.
	 * @param key mission attribute list item key
	 * @param session
	 * @return the associated mission attribute list item or null
	 */
	public MissionAttributeListItem getMissionAttributeListItenByKey(String key, Session session);


	/**
	 * Finds the sampling unit with the given key.
	 * @param key sampling unit key
	 * @param session
	 * @return the associated sampling unit or null
	 */
	public SamplingUnit getSamplingUnitById(String missionTrackId,
			Session session);


	/**
	 * Finds the survey with the given key.
	 * @param key survey key
	 * @param session
	 * @return the associated survey or null
	 */
	public Survey getSurveyById(Session session, String id);
}
