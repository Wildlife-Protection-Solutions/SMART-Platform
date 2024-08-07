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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.er.MissionIdGenerator;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.er.ui.meta.MissionScreenOptionMeta;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

public class SurveyHibernateManager {

	private static ISurveyHibernateManager instance;
	
	
	public static ISurveyHibernateManager getInstance(){
		if (instance == null){
			if (SmartDB.isMultipleAnalysis()){
				instance = new CcaaSurveyHibernateManager();
			}else{
				instance = new CaSurveyHibernateManager(SmartDB.getCurrentConservationArea());
			}
		}
		return instance;
	}
	
	/**
	 * Determines if a mission id already exists in the database
	 * for the given conservation area.
	 * 
	 * @param newId mission id to validate
	 * @param ca conservation area
	 * @param session session
	 * @return <code>true</code> if id already exists; <code>false</code> otherwise
	 */
	public static boolean isDuplicateId(String newId, ConservationArea ca, Session session){
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Long> c = cb.createQuery(Long.class);
		Root<Mission> from = c.from(Mission.class);
		c.select(cb.count(from));
		c.where(cb.and(
				cb.equal(from.join("survey").join("surveyDesign").get("conservationArea"),ca), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				cb.equal(from.get("id"), newId) //$NON-NLS-1$
				));
		Long cnt = (Long)session.createQuery(c).uniqueResult();
		if (cnt > 0){
			return true;
		}
		return false;
	}

	
	
	public static Mission saveMission(Mission mission, Session session, boolean saveWaypoints) throws Exception{
		if (mission.getId() == null ){
			String id = MissionIdGenerator.INSTANCE.generateMissionId(mission, session);
			mission.setId(id);
		}
		
		//without this for code the saveorupdate causes some
		//waypoints to get deleted.  Not sure exactly why
		for (MissionDay md : mission.getMissionDays()){
			md.getWaypoints().forEach(wp->wp.getWaypoint().getId());
		}
		
		mission = HibernateManager.saveOrMerge(session, mission);
		
		IWaypointSource src = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(SurveyWaypointSource.KEY);
		if (saveWaypoints){
			session.flush();
		
			//save all the waypoints as well
			for (MissionDay md : mission.getMissionDays()){
				if (md.getWaypoints() == null) continue;
				
				for (SurveyWaypoint wp: md.getWaypoints()){
					if (wp.getWaypoint().getAttachments() != null){
						//update all the waypoint attachments directory
						for (WaypointAttachment wa : wp.getWaypoint().getAttachments()){
							wa.computeFileLocation(Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
									.resolve(src.getDatastoreFileLocation(mission, session))
									.resolve(wa.getFilename()));
						}
					}
					if (wp.getWaypoint().getObservationGroups() != null){
						for (WaypointObservationGroup grp : wp.getWaypoint().getObservationGroups()){
							for (WaypointObservation wo : grp.getObservations()){
								if (wo.getAttachments() != null){
									for (ObservationAttachment wa : wo.getAttachments()){
										wa.computeFileLocation(Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
												.resolve(src.getDatastoreFileLocation(mission, session))
												.resolve(wa.getFilename()));
									}
								}
							}
						}
					}
					HibernateManager.saveOrMerge(session, wp.getWaypoint());
					session.merge(wp);
				}
				
			}
		}
		return mission;
	}
	

	public static Map<MissionScreenOptionMeta, ScreenOption> getMissionScreenOptions(ConservationArea ca, Session session) {
		List<ScreenOption> results = QueryFactory.buildQuery(session, ScreenOption.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"resource", MissionScreenOptionMeta.MISSION_RESOURCE_ID}).getResultList(); //$NON-NLS-1$
		Map<MissionScreenOptionMeta, ScreenOption> options = new HashMap<MissionScreenOptionMeta, ScreenOption>();
		for (ScreenOption screenOption : results) {
			screenOption.getUuidList().size();
			try {
				options.put(MissionScreenOptionMeta.valueOf(screenOption.getType()), screenOption);
			} catch (IllegalArgumentException e) {
				//ignore unexpected screen type
				SmartPlugIn.log("Unexpected type for mission meta screen.", e); //$NON-NLS-1$
			}
		}
		return options;
	}
	
	
	/**
	 * Returns all surveys that match the given filter.  If the filter
	 * is not provided all surveys are returned.
	 * 
	 * @param s
	 * @param filter filter or null if not filter should be applied
	 * @return
	 */
	public static List<SurveyMissionProxy> getSurveys(Session s, SurveyFilter filter){
		if (filter == null){
			//get all
			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaQuery<Survey> c = cb.createQuery(Survey.class);
			Root<Survey> from = c.from(Survey.class);
			c.where(cb.equal(from.get("surveyDesign").get("conservationArea"), SmartDB.getCurrentConservationArea())); //$NON-NLS-1$ //$NON-NLS-2$
			List<Survey> ds = s.createQuery(c).getResultList();
			
			List<SurveyMissionProxy> all = new ArrayList<SurveyMissionProxy>();
			
			for (Survey d : ds){
				SurveyMissionProxy ii = new SurveyMissionProxy(d.getId(), d.getUuid(), d.getSurveyDesign().getName(), d.getSurveyDesign().getUuid());
				all.add(ii);
			}
			return all;
				
		}else{
			return filter.executeQuery(s);
		}
	}
}
