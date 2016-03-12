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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Single ca survey hibernate functions.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class CaSurveyHibernateManager implements ISurveyHibernateManager{

	private ConservationArea ca;
	
	public CaSurveyHibernateManager(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * Gets all sampling units for a survey design;
	 * This includes all fixed sampling units.
	 *  
	 * <p>
	 * If in CCAA mode, will return all sampling units in all
	 * conservation ares whose survey keys match.
	 * </p>
	 *  
	 * @param survey the survey design 
	 * @param s current session
	 * @param state the state of sampling unit; null if you want all 
	 * @return all sampling units for the given conservation area
	 */
	public List<SamplingUnit> getSamplingUnits(SurveyDesign survey, Session s, SamplingUnit.State state){
		
		List<SamplingUnit> units = new ArrayList<SamplingUnit>();
		
		//get fixed sampling units
		StringBuilder sb = new StringBuilder();
		sb.append("FROM SamplingUnit a "); //$NON-NLS-1$
		sb.append(" WHERE a.surveyDesign = :survey "); //$NON-NLS-1$
		if (state != null){
			sb.append(" AND a.state = :state");	 //$NON-NLS-1$
		}
		
		Query q = s.createQuery(sb.toString());
		q.setParameter("survey", survey); //$NON-NLS-1$
		if (state != null){
			q.setParameter("state", state); //$NON-NLS-1$
		}
		List<SamplingUnit> unit = q.list();
		units.addAll(unit);
		
		return units;
	}
	
	
	/**
	 * Gets all sampling units for a survey design;
	 * This includes all fixed sampling units and reconnaissance
	 * sampling units (represented as tracks).
	 *  
	 * <p>
	 * If in CCAA mode, will return all sampling units in all
	 * conservation ares whose survey keys match.
	 * </p>
	 *  
	 * @return all sampling units for the given conservation area
	 */
	public List<MissionTrack> getAdHocMissionTracks(SurveyDesign survey, Session s){
		
		List<MissionTrack> units = new ArrayList<MissionTrack>();

		//get reconnaissance tracks
		StringBuilder sb = new StringBuilder();
		sb.append("FROM MissionTrack "); //$NON-NLS-1$
		sb.append(" WHERE missionDay.mission.survey.surveyDesign = :survey "); //$NON-NLS-1$
		sb.append(" AND type = :type  "); //$NON-NLS-1$
		
		Query q = s.createQuery(sb.toString());
		q.setParameter("survey", survey); //$NON-NLS-1$
		q.setParameter("type", MissionTrack.TrackType.TRACK); //$NON-NLS-1$
		
		List<MissionTrack> tracks = q.list();
		units.addAll(tracks);
		
		return units;
	}

	/**
	 * Gets an editorInput list all the survey designs that match the specific filter for
	 * the current conservation area.
	 * If the filter
	 * is null with return all survey designs.
	 */
	@Override
	public List<SurveyDesignProxy> getSurveyDesignEditorInputs(Session s, SurveyDesignFilter filter) {
		if (filter == null){
			//get all
			List<SurveyDesign> ds = s.createCriteria(SurveyDesign.class)
					.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
					.list(); 
			List<SurveyDesignProxy> all = new ArrayList<SurveyDesignProxy>();
			
			for (SurveyDesign d : ds){
				SurveyDesignProxy ii = new SurveyDesignProxy(d.getName(), d.getUuid(), d.getKeyId(), d.getState());
				all.add(ii);
			}
			return all;
				
		}else{
			Query q = filter.buildQuery(s);
			List<Object[]> data = q.list();
			List<SurveyDesignProxy> all = new ArrayList<SurveyDesignProxy>();
			for (Object[] x : data){
				SurveyDesignProxy ii = new SurveyDesignProxy((String)x[1], (UUID)x[0], (String)x[3], (SurveyDesign.State)x[2]);
				all.add(ii);
			}
			return all;
		}
	}

	/**
	 * Loads all active surveys for the current conservation area
	 */
	@Override
	public List<Survey> getActiveSurveys(Session s) {
		//get all
		List<Survey> ds = s.createCriteria(Survey.class, "s") //$NON-NLS-1$
				.createAlias("s.surveyDesign", "sd") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("sd.conservationArea", ca)) //$NON-NLS-1$
				.add(Restrictions.eq("sd.state", SurveyDesign.State.ACTIVE)) //$NON-NLS-1$
				.list();
		return ds;
	}
	
	/**
	 * Loads all surveys associated with a survey design
	 */
	@Override
	public List<Survey> getActiveSurveys(SurveyDesign sd, Session s){
		List<Survey> ds = s.createCriteria(Survey.class, "s") //$NON-NLS-1$
				.add(Restrictions.eq("s.surveyDesign", sd)) //$NON-NLS-1$
				.list();
		return ds;
	}
	
	
	/**
	 * Determines all sampling unit types associated with survey design
	 */
	@Override
	public Set<SamplingUnit.GeometryType> getSamplingUnitTypes(SurveyDesign sd, Session s){
		HashSet<SamplingUnit.GeometryType> types = new HashSet<SamplingUnit.GeometryType>();
		for (Object x : getSamplingUnits(sd, s, null)){
			if (x instanceof SamplingUnit){
				types.add(((SamplingUnit) x).getType());
			}
		}
		return types;
	}
	
	
	/**
	 * Find the survey design with the given key
	 * @param key
	 * @param session
	 * @return
	 */
	public SurveyDesign getSurveyDesign(String key, Session session){
		List<SurveyDesign> designs = session.createCriteria(SurveyDesign.class)
				.add(Restrictions.eq("keyId", key)) //$NON-NLS-1$
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.list();
		if (designs.size() > 0){
			return designs.get(0);
		}
		return null;
	}

	@Override
	public MissionAttribute getMissionAttributeByKey(String missionAttributeKeyId, Session session) {
		List<MissionAttribute> list = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("keyId", missionAttributeKeyId)) //$NON-NLS-1$
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.list();
		if (list.size() > 0){
			return list.get(0);
		}
		return null;
	}

	@Override
	public MissionAttributeListItem getMissionAttributeListItenByKey(String key, Session session) {
		List<MissionAttributeListItem> list = session.createCriteria(MissionAttributeListItem.class)
				.createAlias("attribute", "a") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("keyId", key)) //$NON-NLS-1$
				.add(Restrictions.eq("a.conservationArea", ca)) //$NON-NLS-1$
				.list();
		if (list.size() > 0){
			return list.get(0);
		}
		return null;
	}

	@Override
	public SamplingUnit getSamplingUnitById(String key, Session session) {
		List<SamplingUnit> list = session.createCriteria(SamplingUnit.class)
				.createAlias("surveyDesign","sd") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("id", key)) //$NON-NLS-1$
				.add(Restrictions.eq("sd.conservationArea", ca)) //$NON-NLS-1$
				.list();
		if (list.size() > 0){
			return list.get(0);
		}
		return null;	}

	
	
	@Override
	public Survey getSurveyById(Session session, String id) {
		List<Survey> surveys = session.createCriteria(Survey.class)
				.createAlias("surveyDesign", "sd") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("id", id)) //$NON-NLS-1$
				.add(Restrictions.eq("sd.conservationArea", ca)) //$NON-NLS-1$
				.list();
		if (surveys.size() > 0){
			return surveys.get(0);
		}
		return null;
	}
}
