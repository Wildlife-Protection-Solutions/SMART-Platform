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
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.ui.SurveyDesignInput;
import org.wcs.smart.er.ui.SurveyDesignListFilter;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Single ca survey hibernate functions.
 * 
 * @author Emily
 *
 */
public class CaFieldSurveyHibernateManager implements IFieldSurveyHibernateManager{

	
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
		sb.append("FROM SamplingUnit a ");
		sb.append(" WHERE a.surveyDesign = :survey ");
		
		Query q = s.createQuery(sb.toString());
		q.setParameter("survey", survey);
		
		List<SamplingUnit> unit = q.list();
		units.addAll(unit);

		//get reconnaissance tracks
		sb = new StringBuilder();
		sb.append("FROM MissionTrack ");
		sb.append(" WHERE mission.survey.surveyDesign = :survey ");
		sb.append(" AND type = :type  ");
		
		q = s.createQuery(sb.toString());
		q.setParameter("survey", survey);
		q.setParameter("type", MissionTrack.TrackType.RECON);
		
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
		sb.append("FROM SamplingUnit a ");
		sb.append(" WHERE a.surveyDesign = :survey ");
		
		Query q = s.createQuery(sb.toString());
		q.setParameter("survey", mission.getSurvey().getSurveyDesign());
		
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
	 * Gets all the survey designs that match the specific filter for
	 * the current conservation area.
	 * If the filter
	 * is null with return all survey designs.
	 */
	@Override
	public List<SurveyDesignInput> getSurveys(Session s, SurveyDesignListFilter filter) {
		if (filter == null){
			//get all
			List<SurveyDesign> ds = s.createCriteria(SurveyDesign.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
			List<SurveyDesignInput> all = new ArrayList<SurveyDesignInput>();
			
			for (SurveyDesign d : ds){
				SurveyDesignInput ii = new SurveyDesignInput(d.getName(), d.getUuid(), d.getState());
				all.add(ii);
			}
			return all;
				
		}else{
//			str.append("SELECT s.uuid, s.state, s.startDate, s.endDate, lbl.value "); //$NON-NLS-1$
			Query q = filter.buildQuery(s);
			List<Object[]> data = q.list();
			List<SurveyDesignInput> all = new ArrayList<SurveyDesignInput>();
			for (Object[] x : data){
				SurveyDesignInput ii = new SurveyDesignInput((String)x[4], (byte[])x[0], (SurveyDesign.State)x[1]);
				all.add(ii);
			}
			return all;
		}
	}
	
}
