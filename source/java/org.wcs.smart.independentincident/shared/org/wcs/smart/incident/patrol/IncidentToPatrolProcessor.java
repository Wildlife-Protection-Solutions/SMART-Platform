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
package org.wcs.smart.incident.patrol;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.incident.IncidentPropertyManager;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.model.IncidentType;
import org.wcs.smart.incident.model.IncidentWaypoint;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.TrackUtil;

/**
 * Processor which moves patrol based incidents into appropriate patrols.
 * 
 * It searches for patrol leg days with a track point times between the
 * the incident time and if that track is within a specified distance
 * from incident, the incident is added to the patrol. 
 * 
 * @author Emily
 * @since 7.5.7
 *
 */
public class IncidentToPatrolProcessor {

	
	private ConservationArea ca;
	private List<PatrolLegDay> updatedPatrols;
	private List<Waypoint> updatedWaypoints;
	
	private boolean doExpire = false;
	
	/**
	 * Currently to prevent sync conflicts doExpire should only be set to true
	 * if this processing is occurring on a Connect server.
	 * 
	 * @param ca Conservation Area to process
	 * @param doExpire true if old incidents should be converted to normal incidents or not
	 * 
	 */
	public IncidentToPatrolProcessor(ConservationArea ca, boolean doExpire ) {
		this.ca = ca;
		this.updatedPatrols = new ArrayList<>();
		this.updatedWaypoints = new ArrayList<>();
		this.doExpire = doExpire;
	}
	
	public List<PatrolLegDay> getUpdatedPatrols(){
		return this.updatedPatrols;
	}
	public List<Waypoint> getUpdatedWaypoints(){
		return this.updatedWaypoints;
	}
	
	public void doWork(Session session) throws Exception{
		
		int maxDistance = IncidentPropertyManager.INSTANCE.getSetting(session, this.ca, IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_DISTANCE);
		int expireDays = IncidentPropertyManager.INSTANCE.getSetting(session, this.ca, IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_EXPIRE);
		
		this.updatedPatrols.clear();
		this.updatedWaypoints.clear();
		
		List<IncidentType> types = session.createQuery("FROM IncidentType WHERE conservationArea = :ca and options in (:ops)", IncidentType.class) //$NON-NLS-1$
				.setParameter("ca",  ca) //$NON-NLS-1$
				.setParameterList("ops", new String[] {IncidentType.LINK_PATROL_OP, IncidentType.MOVE_PATROL_OP}) //$NON-NLS-1$
				.list();
		Map<UUID, IncidentType> typeMap = new HashMap<>();
		List<UUID> uuids = new ArrayList<>();
		for (IncidentType t : types) {
			uuids.add(t.getUuid());
			typeMap.put(t.getUuid(), t);
		}
		
		List<Waypoint> toProcess = 
				session.createQuery("FROM Waypoint WHERE sourceId = :source AND conservationArea = :ca and incidentTypeUuid in (:types)", Waypoint.class) //$NON-NLS-1$
				.setParameter("source", IndepedentIncidentSource.KEY) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameterList("types", uuids) //$NON-NLS-1$
				.list();
		
		for (Waypoint wp : toProcess) {
			//if waypoints are old convert them to normal waypoints	
			if (doExpire) {
				if (ChronoUnit.DAYS.between(wp.getDateTime().toLocalDate(), LocalDate.now()) > expireDays) {
					//convert to normal integrate waypoint and move on
					session.beginTransaction();
					try {
						IncidentType type = session.get(IncidentType.class, wp.getIncidentTypeUuid());
						if (type != null && type.getFallbackType() != null) {
							//set to fallback type
							wp.setIncidentTypeUuid(type.getFallbackType().getUuid());
						}						
						session.getTransaction().commit();
					}catch (Exception ex) {
						session.getTransaction().rollback();
						throw new Exception(ex);
					}
					updatedWaypoints.add(wp);
					continue;
				}
			}

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT pld "); //$NON-NLS-1$
			sb.append(" FROM PatrolLegDay pld join pld.patrolLeg pl join pl.patrol p "); //$NON-NLS-1$
			sb.append("WHERE p.conservationArea = :ca AND pld.date = :day"); //$NON-NLS-1$
			
			List<PatrolLegDay> days = session.createQuery(sb.toString(), PatrolLegDay.class)
					.setParameter("ca", this.ca) //$NON-NLS-1$
					.setParameter("day", wp.getDateTime().toLocalDate()) //$NON-NLS-1$
					.list();
			
			if (days.isEmpty()) continue;
			
			Track matchedTrack = null;
			LineString matchedLs = null;
			double distance = Double.MAX_VALUE;
			
			for (PatrolLegDay pld : days) {
				//find the track based on the waypoint time 
				//and find the closest track within the maximum distance
				
				//make the assumption that we don't add this before the first track 
				//point or after the last track point
				for (Track t : pld.getTracks()) {
					List<LineString> tracks = t.getLineStrings();
					for (LineString ls : tracks) {
						Coordinate[] c = ls.getCoordinates();
						for (int i = 1; i < c.length; i ++) {
							Coordinate prev = c[i-1];
							Coordinate next = c[i];
							
							LocalDateTime dtprev = SharedUtils.toLocalDateTime(prev);
							LocalDateTime dtnext = SharedUtils.toLocalDateTime(next);
							
							if (dtprev.isEqual(wp.getDateTime()) || 
								dtnext.isEqual(wp.getDateTime()) || 
								(wp.getDateTime().isAfter(dtprev) && wp.getDateTime().isBefore(dtnext))){
								//matches in times
								
								//make a line between prev and next and compute the distance to that line 							
								LineString temp = GeometryFactoryProvider.getFactory().createLineString(new Coordinate[] {prev, next});
								Point tempp =  GeometryFactoryProvider.getFactory().createPoint(new Coordinate(wp.getX(), wp.getY()));
								try {
									double d = GeometryUtils.distanceInMeters(temp, tempp);
									if (d < distance && d < maxDistance) {
										distance = d;
										matchedLs = ls;
										matchedTrack = t;
									}
								}catch (Throwable tr) {
									Logger.getLogger(IncidentToPatrolProcessor.class.getName()).log(Level.WARNING, tr.getMessage(), tr);
								}
							}			
						}
					}
				}
			}//pld
			
			if (matchedTrack == null) continue;
			
			session.beginTransaction();
			try {
				Map<Path,Path> toMove = new HashMap<>();
				
				if (typeMap.get(wp.getIncidentTypeUuid()).doMovePatrol()  ) {
				
					//add to patrol
					//recompute track
					PatrolWaypoint pw = new PatrolWaypoint();
					pw.setPatrolLegDay(matchedTrack.getPatrolLegDay());
					pw.setWaypoint(wp);
					session.persist(pw);	
					
					//deal with attachments as they have to move to a new location
					PatrolWaypointSource src = (PatrolWaypointSource) SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
							
					
					Path toLoc = Paths.get(wp.getConservationArea().getFileDataStoreLocation())
							.resolve(src.getDatastoreFileLocation(matchedTrack.getPatrolLegDay().getPatrolLeg().getPatrol()));
									
					
					for (WaypointAttachment wa : wp.getAttachments()) {
						wa.computeFileLocation(session);
						Path fromFile = wa.getAttachmentFile();
						Path toFile = toLoc.resolve(wa.getFilename());
						toMove.put(fromFile, toFile);
					}
					for (WaypointObservation wo : wp.getAllObservations()) {
						for (ObservationAttachment oa : wo.getAttachments()) {
							oa.computeFileLocation(session);
							Path fromFile = oa.getAttachmentFile();
							Path toFile = toLoc.resolve(oa.getFilename());
							toMove.put(fromFile, toFile);
						}
					}
					
					
					//update source
					wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
					wp.setIncidentTypeUuid(null);
					
					//add to track
					List<LineString> tracks = matchedTrack.getLineStrings();
					List<LineString> newtracks = new ArrayList<>();
					for (LineString ls : tracks) {
						if (ls == matchedLs) {
							List<Coordinate> coords = new ArrayList<>();
							for (Coordinate c : ls.getCoordinates()) coords.add(c);
							coords.add(new Coordinate(wp.getRawX(), wp.getRawY(), 
									SharedUtils.toLongTime(wp.getDateTime())));
							newtracks.add(TrackUtil.convertToLineString(coords));
						}else {
							newtracks.add(ls);
						}
					}
					matchedTrack.setLineStrings(newtracks);
					updatedPatrols.add(matchedTrack.getPatrolLegDay());
				}else if (typeMap.get(wp.getIncidentTypeUuid()).doLinkPatrol()  ) {

					//set to fallback type after linked
					wp.setIncidentTypeUuid(typeMap.get(wp.getIncidentTypeUuid()).getFallbackType().getUuid());
					
					IncidentWaypoint iw = new IncidentWaypoint();
					iw.setPatrol(matchedTrack.getPatrolLegDay().getPatrolLeg().getPatrol());
					iw.setWaypoint(wp);
					session.persist(iw);
				}
				//flush session to ensure all changes can be saved
				session.flush();

				//copy files
				//there is a small chance a file gets lost if something goes wrong here
				//if the first file copies but the second doesn't 
				for (Entry<Path,Path> files : toMove.entrySet()) {
					Path toPath = files.getValue();
					if (!Files.exists(toPath.getParent())) Files.createDirectories(toPath.getParent());
					Files.move(files.getKey(), files.getValue());
				}
			
				
				session.getTransaction().commit();
				
				updatedWaypoints.add(wp);
				
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw new Exception(ex);
			}

		}
		
		
	}
	
}
