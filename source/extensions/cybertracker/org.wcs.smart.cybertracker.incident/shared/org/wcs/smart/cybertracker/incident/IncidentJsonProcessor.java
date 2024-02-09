/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.incident;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.incident.model.IIncidentCyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.incident.model.IncidentCtPackage;
import org.wcs.smart.cybertracker.json.CtJsonObservationParser;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.cybertracker.model.CtIncidentLink;
import org.wcs.smart.incident.IncidentIdGenerator;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.UuidUtils;

/**
 * Parser for parsing patrol data from CT JSON data. 
 * 
 * @author Emily
 *
 */
public abstract class IncidentJsonProcessor implements IJsonProcessor {

	protected List<JsonImportWarning> warnings;
	
	protected Set<Waypoint> newIncidents;
	protected Set<Waypoint> modifiedIncidents;
	
	protected List<CtIncidentLink> groupMappings;
	protected ConservationArea ca;
	private List<Path> tempFiles;
	
	public enum StatusMessage{
		ADDED, MODIFIED;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IIncidentCyberTrackerLabelProvider.class).getLabel(this, l);
		}
	}
	
	public IncidentJsonProcessor(ConservationArea ca) {
		warnings = new ArrayList<>();
		tempFiles = new ArrayList<>();
		this.ca = ca;
	}

	protected abstract void logException(String message, Exception ex);
	
	protected void processWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		
	};
	
	@Override
	public List<JsonImportWarning> getWarnings(){
		return this.warnings;
	}
	
	@Override
	public void cleanUp() {
		cleanUpFiles(tempFiles);
	}
	
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session, Locale l) throws Exception{
		newIncidents = new HashSet<>();
		modifiedIncidents = new HashSet<>();
		groupMappings = new ArrayList<>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();;
		
		for (JSONObject feature : features){
			if (CtJsonUtil.isTrackPoint(feature)) continue;
			
			CtJsonObservationParser parser = new CtJsonObservationParser(l);
			try{
				JSONObject properties = (JSONObject) feature.get(CtJsonObservationParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(CtJsonObservationParser.SIGHTINGS_KEY);
				if (sighting == null) continue;				
				
				String type = (String) sighting.get(CtJsonUtil.JsonKey.DATATYPE.key);

				// Validate data type
				if (!IncidentCtPackage.TYPE_NAME.equalsIgnoreCase(type)){
					//not an incident point; skip it
					continue;
				}
				
				CtIncidentLink currentLink = null;
				CtIncidentLink rootLink = null;

				//read observation counter
				Integer observationCounter = parser.parseObservationCounter(sighting);
				if (observationCounter == null) continue;
				
				//read group id 
				String strGroupId = ((String) sighting.get(CtJsonUtil.JsonKey.OBSERVATION_GROUP.key));
				if (strGroupId == null || strGroupId.trim().isEmpty()) throw new Exception("No group id provided for independent incident.  Incident cannot be loaded"); //$NON-NLS-1$
				strGroupId = strGroupId.trim();
				
				UUID groupUuid = parseUuid(strGroupId);

				String rootId = ((String)properties.get(CtJsonObservationParser.ROOT_ID_KEY));
				//lets see if there is a waypoint to add to (based on groupid)
				
				if (rootId == null) {
					//this is the old way
					currentLink = findWaypointMapping(groupUuid, session);
					if (currentLink == null) {
						currentLink = new CtIncidentLink();
						currentLink.setIncidentGroupId(groupUuid);
					}
					
				}else {
					UUID rootUuid = parseUuid(rootId);
					
					//lets see if there is a waypoint to add to (based on groupid)
					rootLink = findRootWaypointMapping(rootUuid, session);
					if (rootLink == null) {
						rootLink = new CtIncidentLink();
						rootLink.setRootId(rootUuid);
						//rootLink.setWaypoint(waypoint);
						groupMappings.add(rootLink);
					}
					
					currentLink = findWaypointMapping(groupUuid, rootUuid, session);
					if (currentLink == null) {
						currentLink = new CtIncidentLink();	
						currentLink.setRootId(rootUuid);
						currentLink.setIncidentGroupId(groupUuid);
						//currentLink.setObservationGroup();
						currentLink.setWaypoint(rootLink.getWaypoint());
						groupMappings.add(rootLink);
					}
				}
			
				//Parse the waypoint information 				
				Waypoint parsedWp = parser.createWaypoint(feature,ca, session);
				parser.processImages(parsedWp, session);
				
				if (currentLink.getWaypoint() == null) {
					currentLink.setWaypoint(parsedWp);
					if (rootLink != null) rootLink.setWaypoint(parsedWp);
					if (currentLink.getRootId() != null && currentLink.getIncidentGroupId() != null) {
						if(parsedWp.getObservationGroups().size() > 0) {
							currentLink.setObservationGroup(parsedWp.getObservationGroups().get(0));
						}
					}
					
					Employee observer = null;
					for (WaypointObservation so : currentLink.getWaypoint().getAllObservations()) {
						if (so.getObserver() != null) {
							observer = so.getObserver();
							break;
						}
					}
					
					currentLink.getWaypoint().setSourceId(IndepedentIncidentSource.KEY);
					currentLink.getWaypoint().setConservationArea(ca);
					
					String id = IncidentIdGenerator.INSTANCE.getNextIncidentId(session, ca, 
							Collections.singleton(currentLink.getWaypoint().getSourceId()), currentLink.getWaypoint().getDateTime(), observer);
					currentLink.getWaypoint().setId(id);
					
					//there is no position; likely skip on device; lets set to 0
					if (currentLink.getWaypoint().getX() == null) currentLink.getWaypoint().setRawX(0);
					if (currentLink.getWaypoint().getY() == null) currentLink.getWaypoint().setRawY(0);
					
					newIncidents.add(currentLink.getWaypoint());
					
				}else if (currentLink.getWaypoint() !=  null && currentLink.getObservationGroup() == null){
					//merge all observations into first group
					if (currentLink.getWaypoint().getAttachments() == null) currentLink.getWaypoint().setAttachments(new ArrayList<>());
					if (parsedWp.getAttachments() != null) {
						for (WaypointAttachment a : parsedWp.getAttachments()) {
							currentLink.getWaypoint().getAttachments().add(a);
							a.setWaypoint(currentLink.getWaypoint());
						}
					}
					WaypointObservationGroup mgroup = null;
					if (currentLink.getIncidentGroupId() != null && currentLink.getRootId() != null) {
						//copy all observations into new group
						mgroup = new WaypointObservationGroup();
						mgroup.setWaypoint(currentLink.getWaypoint());
						currentLink.getWaypoint().getObservationGroups().add(mgroup);
						mgroup.setObservations(new ArrayList<>());
					}else {
						//copy all observations into existing group
						mgroup = currentLink.getWaypoint().getObservationGroups().get(0);
					}
					currentLink.setObservationGroup(mgroup);
					
					for (WaypointObservationGroup g : parsedWp.getObservationGroups()) {
						for (WaypointObservation wo : g.getObservations()) {
							wo.setObservationGroup(mgroup);
							mgroup.getObservations().add(wo);
						}
					}
					
					modifiedIncidents.add(currentLink.getWaypoint());
				}else if (currentLink.getWaypoint() != null && currentLink.getObservationGroup() != null) {
					//merge all observations into observation group
					
					if (currentLink.getWaypoint().getAttachments() == null) currentLink.getWaypoint().setAttachments(new ArrayList<>());
					if (parsedWp.getAttachments() != null) {
						for (WaypointAttachment a : parsedWp.getAttachments()) {
							currentLink.getWaypoint().getAttachments().add(a);
							a.setWaypoint(currentLink.getWaypoint());
						}
					}
					//copy all observations into this group
					for (WaypointObservationGroup g : parsedWp.getObservationGroups()) {
						for (WaypointObservation wo : g.getObservations()) {
							wo.setObservationGroup(currentLink.getObservationGroup());
							currentLink.getObservationGroup().getObservations().add(wo);
						}
					}
					
					modifiedIncidents.add(currentLink.getWaypoint());
				}
				warnings.addAll(parser.getWarnings());
			
				if (currentLink.getWaypoint().getUuid() == null) session.persist(currentLink.getWaypoint());
				if (currentLink.getUuid() == null) session.persist(currentLink);
				if (rootLink != null && rootLink.getUuid() == null) session.persist(rootLink);
				
				processedFeatures.add(feature);
				
			}catch (Exception ex){
				//if there is a session.flush error we have a problem we need to stop and rollback
				logException(ex.getMessage() + ": " + feature.toJSONString(), ex); //$NON-NLS-1$
				warnings.add(new JsonImportWarning(JsonImportWarning.Type.JSON_FEATURE_PARSE_ERROR, ex.getMessage()));
			}finally {
				tempFiles.addAll(parser.getTemporaryFiles());
			}
		}
		
		//display warnings to user; this may throw a cancelled exception if the user doesn't want to proceed
		processWarnings(warnings);
				
		return processedFeatures;
	}
	
	private UUID parseUuid(String uuid) {
		uuid = uuid.replaceAll("-", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return UuidUtils.stringToUuid(uuid);
	}
	
	private CtIncidentLink findWaypointMapping(UUID incidentGroupId, Session session) {
		for (CtIncidentLink l : groupMappings) {
			if (l.getRootId() == null && l.getIncidentGroupId() != null && l.getIncidentGroupId().equals(incidentGroupId)) {
				return l;
			}
		}
		List<CtIncidentLink> links = session.createQuery("FROM CtIncidentLink WHERE incidentGroupId = :groupid and rootId is null", CtIncidentLink.class) //$NON-NLS-1$
				.setParameter("groupid",  incidentGroupId) //$NON-NLS-1$
				.list();
		if (links.isEmpty()) return null;
		return links.get(0);
	}
	
	private CtIncidentLink findWaypointMapping(UUID incidentGroupId, UUID rootId, Session session) {
		for (CtIncidentLink l : groupMappings) {
			if (l.getIncidentGroupId()!= null && l.getIncidentGroupId().equals(incidentGroupId) && l.getRootId() != null && l.getRootId().equals(rootId)) {
				return l;
			}
		}
		List<CtIncidentLink> links = session.createQuery("FROM CtIncidentLink WHERE incidentGroupId = :groupid and rootId = :rootid ", CtIncidentLink.class) //$NON-NLS-1$
				.setParameter("groupid",  incidentGroupId) //$NON-NLS-1$
				.setParameter("rootid",  rootId) //$NON-NLS-1$
				.list();
		if (links.isEmpty()) return null;
		return links.get(0);
	}
	
	private CtIncidentLink findRootWaypointMapping(UUID rootId, Session session) {
		for (CtIncidentLink l : groupMappings) {
			if (l.getIncidentGroupId() == null && l.getRootId() != null &&  l.getRootId().equals(rootId)) {
				return l;
			}
		}
		List<CtIncidentLink> links = session.createQuery("FROM CtIncidentLink WHERE  rootId = :rootid and incidentGroupId is null ", CtIncidentLink.class) //$NON-NLS-1$
				.setParameter("rootid",  rootId) //$NON-NLS-1$
				.list();
		if (links.isEmpty()) return null;
		return links.get(0);
	}
	
	@Override
	public String getStatusMessage(Locale l) {
		if (newIncidents.isEmpty() && modifiedIncidents.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		if (!newIncidents.isEmpty()){
			sb.append(MessageFormat.format(StatusMessage.ADDED.getMessage(l), newIncidents.size()));
			sb.append("("); //$NON-NLS-1$
			for(Waypoint p : newIncidents){
				sb.append(p.getId());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append(") "); //$NON-NLS-1$
		}
		HashSet<Waypoint> tmp = new HashSet<>(modifiedIncidents);
		for (Waypoint w : newIncidents) tmp.remove(w);
		if (tmp.size() > 0){
			sb.append(MessageFormat.format(StatusMessage.MODIFIED.getMessage(l), tmp.size()));
			sb.append("("); //$NON-NLS-1$
			for(Waypoint p : tmp){
				sb.append(p.getId());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append(") "); //$NON-NLS-1$
		}
		return sb.toString();
	}	
	
}