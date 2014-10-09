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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyEditorInput;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Single ca survey hibernate functions.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class CaSurveyHibernateManager implements ISurveyHibernateManager{

	
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
	public List<Object> getSamplingUnits(SurveyDesign survey, Session s){
		
		List<Object> units = new ArrayList<Object>();
		
		//get fixed sampling units
		StringBuilder sb = new StringBuilder();
		sb.append("FROM SamplingUnit a "); //$NON-NLS-1$
		sb.append(" WHERE a.surveyDesign = :survey "); //$NON-NLS-1$
		
		Query q = s.createQuery(sb.toString());
		q.setParameter("survey", survey); //$NON-NLS-1$
		
		List<SamplingUnit> unit = q.list();
		units.addAll(unit);

		//get reconnaissance tracks
		sb = new StringBuilder();
		sb.append("FROM MissionTrack "); //$NON-NLS-1$
		sb.append(" WHERE mission.survey.surveyDesign = :survey "); //$NON-NLS-1$
		sb.append(" AND type = :type  "); //$NON-NLS-1$
		
		q = s.createQuery(sb.toString());
		q.setParameter("survey", survey); //$NON-NLS-1$
		q.setParameter("type", MissionTrack.TrackType.RECON); //$NON-NLS-1$
		
		List<MissionTrack> tracks = q.list();
		units.addAll(tracks);
		
		return units;
	}
	
	/**
	 * Gets all sampling units for a particular
	 * mission.  This includes all suvrey design sampling units
	 * and reconnaissance sampling units for the specific mission.
	 *  
	 * <p>
	 * If in CCAA mode, will return all sampling units in all
	 * conservation ares whose survey keys match.
	 * </p>
	 *  
	 * @return all sampling units for the given conservation area
	 */
	public List<Object> getSamplingUnits(Mission mission, Session s){
		
		List<Object> units = new ArrayList<Object>();
		
		//get fixed sampling units
		StringBuilder sb = new StringBuilder();
		sb.append("FROM SamplingUnit a "); //$NON-NLS-1$
		sb.append(" WHERE a.surveyDesign = :survey "); //$NON-NLS-1$
		
		Query q = s.createQuery(sb.toString());
		q.setParameter("survey", mission.getSurvey().getSurveyDesign()); //$NON-NLS-1$
		
		List<SamplingUnit> unit = q.list();
		units.addAll(unit);

		//get reconnaissance tracks
		for (MissionTrack mt : mission.getTracks()){
			if (mt.getType() == TrackType.RECON){
				units.add(mt);
			}
		}

		return units;
	}

	/**
	 * Gets an editorInput list all the survey designs that match the specific filter for
	 * the current conservation area.
	 * If the filter
	 * is null with return all survey designs.
	 */
	@Override
	public List<SurveyDesignEditorInput> getSurveyDesignEditorInputs(Session s, SurveyDesignFilter filter) {
		if (filter == null){
			//get all
			List<SurveyDesign> ds = s.createCriteria(SurveyDesign.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.list(); 
			List<SurveyDesignEditorInput> all = new ArrayList<SurveyDesignEditorInput>();
			
			for (SurveyDesign d : ds){
				SurveyDesignEditorInput ii = new SurveyDesignEditorInput(d.getName(), d.getUuid(), d.getKeyId(), d.getState());
				all.add(ii);
			}
			return all;
				
		}else{
			Query q = filter.buildQuery(s);
			List<Object[]> data = q.list();
			List<SurveyDesignEditorInput> all = new ArrayList<SurveyDesignEditorInput>();
			for (Object[] x : data){
				SurveyDesignEditorInput ii = new SurveyDesignEditorInput((String)x[1], (byte[])x[0], (String)x[3], (SurveyDesign.State)x[2]);
				all.add(ii);
			}
			return all;
		}
	}
	

	
	/**
	 * Returns all surveys that match the given filter.  If the filter
	 * is not provided all surveys are returned.
	 * 
	 * @param s
	 * @param filter filter or null if not filter should be applied
	 * @return
	 */
	public List<SurveyEditorInput> getSurveys(Session s, SurveyFilter filter){
		if (filter == null){
			//get all
			List<Survey> ds = s.createCriteria(Survey.class, "s") //$NON-NLS-1$
					.createAlias("surveyDesign", "sd") //$NON-NLS-1$ //$NON-NLS-2$
					.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
			List<SurveyEditorInput> all = new ArrayList<SurveyEditorInput>();
			
			for (Survey d : ds){
				SurveyEditorInput ii = new SurveyEditorInput(d.getId(), d.getUuid(), d.getStartDate(), d.getSurveyDesign().getName());
				all.add(ii);
			}
			return all;
				
		}else{
			Query q = filter.buildQuery(s);
			List<Object[]> data = q.list();
			List<SurveyEditorInput> all = new ArrayList<SurveyEditorInput>();
			for (Object[] x : data){
				SurveyEditorInput ii = new SurveyEditorInput((String)x[1], (byte[])x[0], (Date)x[2], (String)x[3]);
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
				.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
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
	public Set<SamplingUnit.SamplingUnitType> getSamplingUnitTypes(SurveyDesign sd, Session s){
		
		HashSet<SamplingUnit.SamplingUnitType> types = new HashSet<SamplingUnit.SamplingUnitType>();
		
		for (Object x : getSamplingUnits(sd, s)){
			if (x instanceof SamplingUnit){
				types.add(((SamplingUnit) x).getType());
			}
		}
		
		Long cnt = (Long)s.createCriteria(MissionTrack.class, "mt") //$NON-NLS-1$
			.createAlias("mt.mission", "m") //$NON-NLS-1$ //$NON-NLS-2$
			.createAlias("m.survey", "s") //$NON-NLS-1$ //$NON-NLS-2$
			.add(Restrictions.eq("s.surveyDesign", sd)) //$NON-NLS-1$
			.add(Restrictions.eq("type", TrackType.RECON)) //$NON-NLS-1$
			.setProjection(Projections.rowCount()).list().get(0);
		if (cnt > 0){
			types.add(SamplingUnitType.RECON);
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
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
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
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		if (list.size() > 0){
			return list.get(0);
		}
		return null;
	}

	@Override
	public MissionAttributeListItem getMissionAttributeListItenByKey(String key, Session session) {
		List<MissionAttributeListItem> list = session.createCriteria(MissionAttributeListItem.class)
				.createAlias("attribute", "a")
				.add(Restrictions.eq("keyId", key)) //$NON-NLS-1$
				.add(Restrictions.eq("a.conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		if (list.size() > 0){
			return list.get(0);
		}
		return null;
	}

	@Override
	public SamplingUnit getSamplingUnitById(String key, Session session) {
		List<SamplingUnit> list = session.createCriteria(SamplingUnit.class)
				.createAlias("surveyDesign","sd")
				.add(Restrictions.eq("id", key)) //$NON-NLS-1$
				.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		if (list.size() > 0){
			return list.get(0);
		}
		return null;	}

	
	
	@Override
	public Survey getSurveyById(Session session, String id) {
		List<Survey> surveys = session.createCriteria(Survey.class)
				.createAlias("surveyDesign", "sd")
				.add(Restrictions.eq("id", id)) //$NON-NLS-1$
				.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		if (surveys.size() > 0){
			return surveys.get(0);
		}
		return null;
	}
}
