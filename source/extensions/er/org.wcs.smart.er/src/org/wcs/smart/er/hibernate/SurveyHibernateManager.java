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

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.er.ui.meta.MissionScreenOptionMeta;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;

public class SurveyHibernateManager {

	private static ISurveyHibernateManager instance;
	private static NumberFormat MISSION_ID_FORMATTER = new DecimalFormat("000000"); //$NON-NLS-1$
	
	
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

	
	
	public static void saveMission(Mission mission, Session session, boolean saveWaypoints) throws Exception{
		if (mission.getId() == null ){
			String id = SurveyHibernateManager.generateMissionId(session);
			mission.setId(id);
		}
		
		session.saveOrUpdate(mission);
		
		IWaypointSource src = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(SurveyWaypointSource.KEY);
		if (saveWaypoints){
			session.flush();
		
			//save all the waypoints as well
			for (MissionDay md : mission.getMissionDays()){
				if (md.getWaypoints() != null) {
					for (SurveyWaypoint wp: md.getWaypoints()){
						if (wp.getWaypoint().getAttachments() != null){
							//update all the waypoint attachments directory
							for (WaypointAttachment wa : wp.getWaypoint().getAttachments()){
								wa.computeFileLocation(new File(new File(
										SmartDB.getCurrentConservationArea().getFileDataStoreLocation(),
										src.getDatastoreFileLocation(mission, session)), wa.getFilename()));
							}
						}
						if (wp.getWaypoint().getObservations() != null){
							for (WaypointObservation wo : wp.getWaypoint().getObservations()){
								if (wo.getAttachments() != null){
									for (ObservationAttachment wa : wo.getAttachments()){
										wa.computeFileLocation(new File(new File(
												SmartDB.getCurrentConservationArea().getFileDataStoreLocation(),
												src.getDatastoreFileLocation(mission, session)), wa.getFilename()));
									}
								}
							}
						}
						session.saveOrUpdate(wp.getWaypoint());
						session.saveOrUpdate(wp);
					}
				}
			}
		}
	}
	
	public static String generateMissionId( Session s) {
		StringBuilder sb = new StringBuilder();

		sb.append(SmartDB.getCurrentConservationArea().getId());

		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Mission> c = cb.createQuery(Mission.class);
		Root<Mission> from = c.from(Mission.class);
		c.where(cb.like(from.get("id"), sb.toString() + "%")); //$NON-NLS-1$ //$NON-NLS-2$
		c.orderBy(cb.desc(from.get("id"))); //$NON-NLS-1$
		List<Mission> results = s.createQuery(c).getResultList();
		
		long idNumber = 0;
		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
			Mission m =  (Mission) iterator.next();
			String localId = m.getId(); 
			try {
				int offset = localId.lastIndexOf('_') + 1;
				idNumber = Integer.parseInt(localId.substring(offset));
				break;
			} catch (Exception ex) {
				// not of the form CAID_# skip this one
			}
		}
		sb.append("_"); //$NON-NLS-1$
		//sb.append("M"); //$NON-NLS-1$  //not so good for non-english language installs. Just going to leave it without a prefix I guess.
		idNumber = (idNumber + 1) % 1000000;
		if (idNumber <= 0) {
			idNumber = 1;
		}
		sb.append(MISSION_ID_FORMATTER.format(idNumber));

		return sb.toString();
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
	public static List<SurveyProxy> getSurveys(Session s, SurveyFilter filter){
		if (filter == null){
			//get all
			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaQuery<Survey> c = cb.createQuery(Survey.class);
			Root<Survey> from = c.from(Survey.class);
			c.where(cb.equal(from.get("surveyDesign").get("conservationArea"), SmartDB.getCurrentConservationArea())); //$NON-NLS-1$ //$NON-NLS-2$
			List<Survey> ds = s.createQuery(c).getResultList();
			
			List<SurveyProxy> all = new ArrayList<SurveyProxy>();
			
			for (Survey d : ds){
				SurveyProxy ii = new SurveyProxy(d.getId(), d.getUuid(), d.getStartDate(), d.getSurveyDesign().getName());
				all.add(ii);
			}
			return all;
				
		}else{
			Query<?> q = filter.buildQuery(s);
			List<?> data = q.list();
			List<SurveyProxy> all = new ArrayList<SurveyProxy>();
			for (Object d : data){
				Object[] x = (Object[])d;
				SurveyProxy ii = new SurveyProxy((String)x[1], (UUID)x[0], (Date)x[2], (String)x[3]);
				all.add(ii);
			}
			return all;
		}
	}
}
