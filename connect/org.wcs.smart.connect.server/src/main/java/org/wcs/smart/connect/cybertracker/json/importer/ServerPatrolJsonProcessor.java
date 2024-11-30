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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonProcessor;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolWpLink;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.PatrolIdGenerator;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;

/**
 * Connect processor for SMART Mobile patrol data
 * 
 * @author Emily
 *
 */
public class ServerPatrolJsonProcessor extends PatrolJsonProcessor {

	private final Logger logger = Logger.getLogger(ServerPatrolJsonProcessor.class.getName());

	public ServerPatrolJsonProcessor(ConservationArea ca) {
		super(ca);
	}

	@Override
	protected void assignPatrols(Session session) throws UserCancelledException {
		for (CtPatrolLink ctpatrol : this.newPatrolLinks.values()) {
			saveAsNewPatrol(ctpatrol, session);
		}
	}
		
		
		
	private void saveAsNewPatrol(CtPatrolLink patrol, Session session) {
		
		Patrol newPatrol = patrol.getPatrolLeg().getPatrol();
		newPatrol.setConservationArea(this.ca);
			
		//this shouldn't be necessary with the CT Mobile, but 
		//may be required for old CT support
		LocalDate start = newPatrol.getFirstLeg().getStartDate();
		LocalDate end = newPatrol.getFirstLeg().getEndDate();
		for (PatrolLeg pl : newPatrol.getLegs()) {
			if (pl.getStartDate().isBefore(start)) start = pl.getStartDate();
			if (pl.getEndDate().isAfter(end)) end = pl.getEndDate();
		}
		newPatrol.setStartDate(start);
		newPatrol.setEndDate(end);
			
		newPatrol.setId(PatrolIdGenerator.INSTANCE.generatePatrolId(newPatrol, session));
		
		newPatrol.recalculateType();
		
		session.persist(newPatrol);
		
		IWaypointSource src = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
		
		String patrolLocation = null;
		try{
			patrolLocation = src.getDatastoreFileLocation(newPatrol, session);
		}catch (Exception ex) {
			//this should never happen
			throw new RuntimeException(ex);
		}
		
		for (PatrolLeg leg : newPatrol.getLegs()) {
			if(leg.getPatrolLegDays().isEmpty()) leg.createLegDays(session);
			for (PatrolLegDay pld : leg.getPatrolLegDays()) {
				if (pld.getWaypoints() == null) pld.setWaypoints(new ArrayList<>());
				for(PatrolWaypoint pw : pld.getWaypoints()) {
					
					if (pw.getWaypoint().getAttachments() != null){
						//update all the waypoint attachments directory
						for (WaypointAttachment wa : pw.getWaypoint().getAttachments()){
							wa.computeFileLocation(Paths.get(
									newPatrol.getConservationArea().getFileDataStoreLocation())
									.resolve(patrolLocation)
									.resolve(wa.getFilename()));
						}
					}
					for (WaypointObservation wo : pw.getWaypoint().getAllObservations()){
						if (wo.getAttachments() != null){
							for (ObservationAttachment wa : wo.getAttachments()){
								wa.computeFileLocation(
										Paths.get(newPatrol.getConservationArea().getFileDataStoreLocation())
										.resolve(patrolLocation)
										.resolve(wa.getFilename()));
							}
						}
					}
					
					session.persist(pw.getWaypoint());
					session.persist(pw);
					
				}
			}
		}
		
		
		//create links for all new legs
		for (PatrolLeg pl : newPatrol.getLegs()) {
			if (pl == patrol.getPatrolLeg()) continue;
			CtPatrolLink link = new CtPatrolLink();
			link.setCtUuid(UUID.randomUUID());
			link.setPatrolLeg(pl);
			link.setDeviceId(patrol.getDeviceId());
			link.setLastObservationCnt(-1);
			link.setGroupStartTime(null);
			link.setWaypointLinks(new ArrayList<>());
			session.persist(link);
		}
			
		CtPatrolLink link = new CtPatrolLink();
		link.setCtUuid(patrol.getCtUuid());
		link.setPatrolLeg(patrol.getPatrolLeg());
		link.setDeviceId(patrol.getDeviceId());
		link.setLastObservationCnt(patrol.getLastObservationCnt());
		link.setGroupStartTime(patrol.getGroupStartTime());
		link.setWaypointLinks(new ArrayList<>());
		
		for (CtPatrolWpLink l : patrol.getWaypointLinks()) {
			l.setLink(link);
			link.getWaypointLinks().add(l);
		}
		session.persist(link);
		
		patrol.getWaypointLinks().clear();
		
		this.newPatrols.add(newPatrol);
		
	}
	
	@Override
	protected void logException(String message, Exception ex) {
		logger.log(Level.SEVERE, message, ex);
	}
}
