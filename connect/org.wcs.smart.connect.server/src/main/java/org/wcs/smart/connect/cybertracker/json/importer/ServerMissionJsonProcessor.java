/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.json.importer;

import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.query.Order;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.cybertracker.survey.json.MissionJsonProcessor;
import org.wcs.smart.cybertracker.survey.model.CtMissionLink;
import org.wcs.smart.cybertracker.survey.model.CtMissionWpLink;
import org.wcs.smart.er.MissionIdGenerator;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;

/**
 * Connect processor for SMART Mobile mission data.
 * 
 * @author Emily
 *
 */
public class ServerMissionJsonProcessor extends MissionJsonProcessor {

	private final Logger logger = Logger.getLogger(ServerMissionJsonProcessor.class.getName());

	public ServerMissionJsonProcessor(ConservationArea ca) {
		super(ca);
	}

	@Override
	protected void logException(String message, Exception ex) {
		logger.log(Level.SEVERE, message, ex.getMessage());		
	}

	@Override
	protected void assignMissions(Session session) throws UserCancelledException {
		for (CtMissionLink ctmission : this.newMissionLinks.values()) {
			saveMission(ctmission, session);
		}
	}
		
	private void saveMission(CtMissionLink ctmission, Session session) {
		
		Mission newMission = ctmission.getMission();
		
		//assign to survey based on survey created date
		SurveyDesign sd = ctmission.getNewSurveyDesign();
		List<Survey> options = session.createQuery("FROM Survey WHERE surveyDesign = :sd", Survey.class) //$NON-NLS-1$
				.setParameter("sd", sd) //$NON-NLS-1$
				.setOrder(Order.asc(Survey.class, "id")) //$NON-NLS-1$
				.setOrder(Order.desc(Survey.class, "createdDate")) //$NON-NLS-1$
				.list();
		if (options.isEmpty()) {
			Survey s = new Survey();
			s.initCreatedDate();
			s.setSurveyDesign(sd);
			s.setMissions(new ArrayList<>());
			s.setId(newMission.getStartDate().format(DateTimeFormatter.ofPattern("YYYYMMDD"))); //$NON-NLS-1$
			
			session.persist(s);
			newMission.setSurvey(s);
			s.getMissions().add(newMission);
		}else {
			//pick the newest one
			Survey s = options.get(0);
			newMission.setSurvey(s);
		}
		
		newMission.setId(MissionIdGenerator.INSTANCE.generateMissionId(ca, session));
		
		session.persist(newMission);
		
		IWaypointSource src = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(SurveyWaypointSource.KEY);
		
		String missionLocation = null;
		try{
			missionLocation = src.getDatastoreFileLocation(newMission, session);
		}catch (Exception ex) {
			//this should never happen
			throw new RuntimeException(ex);
		}
		
		ConservationArea ca = newMission.getSurvey().getSurveyDesign().getConservationArea();
		
		for (MissionDay md : newMission.getMissionDays()) {
			if (md.getWaypoints() == null) md.setWaypoints(new ArrayList<>());
			
			for(SurveyWaypoint sw : md.getWaypoints()) {
					
				if (sw.getWaypoint().getAttachments() != null){
					//update all the waypoint attachments directory
					for (WaypointAttachment wa : sw.getWaypoint().getAttachments()){
						wa.computeFileLocation(Paths.get(
								ca.getFileDataStoreLocation())
								.resolve(missionLocation)
								.resolve(wa.getFilename()));
					}
				}
				for (WaypointObservation wo : sw.getWaypoint().getAllObservations()){
					if (wo.getAttachments() != null){
						for (ObservationAttachment wa : wo.getAttachments()){
							wa.computeFileLocation(
									Paths.get(ca.getFileDataStoreLocation())
									.resolve(missionLocation)
									.resolve(wa.getFilename()));
						}
					}
				}
					
				session.persist(sw.getWaypoint());
				session.persist(sw);
			}
		}
					
		CtMissionLink link = new CtMissionLink();
		link.setCtUuid(ctmission.getCtUuid());
		link.setDeviceId(ctmission.getDeviceId());
		link.setLastObservationCnt(ctmission.getLastObservationCnt());
		link.setGroupStartTime(ctmission.getGroupStartTime());
		link.setSamplingUnit(ctmission.getSamplingUnit());
		link.setNewSurveyDesign(ctmission.getNewSurveyDesign());
		link.setMission(newMission);
		
		link.setWaypointLinks(new ArrayList<>());
		
		for (CtMissionWpLink l : ctmission.getWaypointLinks()) {
			l.setLink(link);
			link.getWaypointLinks().add(l);
		}
		session.persist(link);
		
		ctmission.getWaypointLinks().clear();

		this.newMissions.add(newMission);		
	}
}
