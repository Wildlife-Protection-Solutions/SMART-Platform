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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Single ca survey hibernate functions.
 * 
 * @author Emily
 *
 */
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
		
		Query<SamplingUnit> q = s.createQuery(sb.toString(),SamplingUnit.class);
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
		
		Query<MissionTrack> q = s.createQuery(sb.toString(), MissionTrack.class);
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
			List<SurveyDesign> ds = QueryFactory.buildQuery(s, SurveyDesign.class, "conservationArea", ca).getResultList(); //$NON-NLS-1$
			List<SurveyDesignProxy> all = new ArrayList<SurveyDesignProxy>();
			for (SurveyDesign d : ds){
				SurveyDesignProxy ii = new SurveyDesignProxy(d.getName(), d.getUuid(), d.getKeyId(), d.getState());
				all.add(ii);
			}
			return all;
				
		}else{
			Query<?> q = filter.buildQuery(s);
			List<?> data = q.list();
			List<SurveyDesignProxy> all = new ArrayList<SurveyDesignProxy>();
			for (Object d : data){
				Object[] x = (Object[])d;
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
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Survey> c = cb.createQuery(Survey.class);
		Root<Survey> from = c.from(Survey.class);
		c.select(from);
		c.where(cb.and(
				cb.equal(from.join("surveyDesign").get("conservationArea"), ca), //$NON-NLS-1$ //$NON-NLS-2$
				cb.equal(from.join("surveyDesign").get("state"), SurveyDesign.State.ACTIVE) //$NON-NLS-1$ //$NON-NLS-2$
				));
		return s.createQuery(c).getResultList();
		
	}
	
	/**
	 * Loads all surveys associated with a survey design
	 */
	@Override
	public List<Survey> getActiveSurveys(SurveyDesign sd, Session s){
		return QueryFactory.buildQuery(s, Survey.class, "surveyDesign", sd).getResultList(); //$NON-NLS-1$
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
		return QueryFactory.buildQuery(session, SurveyDesign.class,
				new Object[] {"keyId", key}, //$NON-NLS-1$
				new Object[] {"conservationArea", ca}).uniqueResult(); //$NON-NLS-1$
	}

	@Override
	public MissionAttribute getMissionAttributeByKey(String missionAttributeKeyId, Session session) {
		return QueryFactory.buildQuery(session, MissionAttribute.class,
				new Object[] {"keyId", missionAttributeKeyId}, //$NON-NLS-1$
				new Object[] {"conservationArea", ca}).uniqueResult(); //$NON-NLS-1$
	}

	@Override
	public MissionAttributeListItem getMissionAttributeListItenByKey(String missionAttributeKeyId, String key, Session session) {
		
		//get all
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<MissionAttributeListItem> c = cb.createQuery(MissionAttributeListItem.class);
		Root<MissionAttributeListItem> from = c.from(MissionAttributeListItem.class);
		c.select(from);
		c.where(cb.and(
				cb.equal(from.get("keyId"), key), //$NON-NLS-1$
				cb.equal(from.join("attribute").get("conservationArea"), ca), //$NON-NLS-1$ //$NON-NLS-2$
				cb.equal(from.join("attribute").get("keyId"), missionAttributeKeyId) //$NON-NLS-1$ //$NON-NLS-2$
				));
		return session.createQuery(c).uniqueResult();
	}

	@Override
	public SamplingUnit getSamplingUnitById(String key, Session session) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<SamplingUnit> c = cb.createQuery(SamplingUnit.class);
		Root<SamplingUnit> from = c.from(SamplingUnit.class);		
		c.select(from);
		c.where(cb.and(
				cb.equal(from.get("id"), key), //$NON-NLS-1$
				cb.equal(from.join("surveyDesign").get("conservationArea"), ca) //$NON-NLS-1$ //$NON-NLS-2$
				));
		return session.createQuery(c).uniqueResult();
	}

	
	
	@Override
	public Survey getSurveyById(Session session, String id) {
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Survey> c = cb.createQuery(Survey.class);
		Root<Survey> from = c.from(Survey.class);
		c.select(from);
		c.where(cb.and(
				cb.equal(from.get("id"), id), //$NON-NLS-1$
				cb.equal(from.join("surveyDesign").get("conservationArea"), ca) //$NON-NLS-1$ //$NON-NLS-2$
				));
		List<Survey> items = session.createQuery(c).list();
		if (items.size() == 0) return null;
		return items.get(0);
	}
}
