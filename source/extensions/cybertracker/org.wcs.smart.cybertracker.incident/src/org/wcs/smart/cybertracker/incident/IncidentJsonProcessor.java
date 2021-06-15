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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.json.IJsonProcessor;
import org.wcs.smart.cybertracker.importer.json.JsonCtParser;
import org.wcs.smart.cybertracker.importer.json.UserCancelledException;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.cybertracker.model.CtIncidentLink;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.event.IncidentEventManager;
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
public class IncidentJsonProcessor implements IJsonProcessor {

	private List<String> warnings;
	
	private Set<Waypoint> newIncidents;
	private Set<Waypoint> modifiedIncidents;
	//resize value for apply to all option
	private Point allSize = null;
	
	private List<CtIncidentLink> groupMappings;
	
	public IncidentJsonProcessor() {
		warnings = new ArrayList<String>();
	}

	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception{
		newIncidents = new HashSet<>();
		modifiedIncidents = new HashSet<>();
		groupMappings = new ArrayList<>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();;
		
		for (JSONObject feature : features){
			JsonCtParser parser = new JsonCtParser();
			if (JsonCtParser.isTrackPoint(feature)) continue;
			
			try{
				JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
				if (sighting == null) continue;				
				
				String type = (String) sighting.get(ScreensUtil.RESULT_DATATYPE);
				// Validate data type
				if (!IncidentPackageContribution.INCIDENT_RESOURCE_ID.equalsIgnoreCase(type)){
					//not an incident point; skip it
					continue;
				}
				
				CtIncidentLink currentLink = null;
				CtIncidentLink rootLink = null;

				//read observation counter
				Integer observationCounter = parser.parseObservationCounter(sighting);
				if (observationCounter == null) continue;
				
				//read group id 
				String strGroupId = ((String) sighting.get(ScreensUtil.RESULT_SIGHTINGGROUPID)).trim();
				if (strGroupId == null || strGroupId.isEmpty()) throw new Exception("No group id provided for independent incident.  Incident cannot be loaded"); //$NON-NLS-1$

				UUID groupUuid = parseUuid(strGroupId);

				String rootId = ((String)properties.get(JsonCtParser.ROOT_ID_KEY));
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
//			
//				if (currentLink.getLastObservationCounter() == null) {
//					if (observationCounter != 1)  continue; //not the first observation in the group; cannot process
//					currentLink.setLastObservationCounter(observationCounter);
//				}else if (currentLink.getLastObservationCounter() + 1 != observationCounter) {
//					//not the next observation in the group; cannot process
//					continue;
//				}else {
//					currentLink.setLastObservationCounter(observationCounter);
//				}
//			
			
				//Parse the waypoint information 				
				Waypoint parsedWp = parser.createWaypoint(feature, SmartDB.getCurrentConservationArea(), session);
				allSize = JsonCtParser.processImages(parsedWp, allSize, session);
				
				if (currentLink.getWaypoint() == null) {
					currentLink.setWaypoint(parsedWp);
					if (rootLink != null) rootLink.setWaypoint(parsedWp);
					if (currentLink.getRootId() != null && currentLink.getIncidentGroupId() != null) currentLink.setObservationGroup(parsedWp.getObservationGroups().get(0));
					
					Employee observer = null;
					for (WaypointObservation so : currentLink.getWaypoint().getAllObservations()) {
						if (so.getObserver() != null) {
							observer = so.getObserver();
							break;
						}
					}
					
					currentLink.getWaypoint().setId(IncidentManager.getInstance().getNextIncidentId(session, observer));
					currentLink.getWaypoint().setSourceId(IndepedentIncidentSource.KEY);
					currentLink.getWaypoint().setConservationArea(SmartDB.getCurrentConservationArea());

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
			
				session.saveOrUpdate(currentLink.getWaypoint());
				session.saveOrUpdate(currentLink);
				if (rootLink != null) session.saveOrUpdate(rootLink);
				
				processedFeatures.add(feature);
				
			}catch (Exception ex){
				//if there is a session.flush error we have a problem we need to stop and rollback
				CyberTrackerPlugIn.log(ex.getMessage() + ": " + feature.toJSONString(), ex); //$NON-NLS-1$
				warnings.add(Messages.IncidentJsonProcessor_ParseError + ex.getMessage());
			}
		}
		
		//display warnings to user; this may throw a cancelled exception if the user doesn't want to proceed
		displayWarnings(warnings);
				
		return processedFeatures;
	}
	
	private UUID parseUuid(String uuid) {
		uuid = uuid.replaceAll("-", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return UuidUtils.stringToUuid(uuid);
	}
	
	@SuppressWarnings("unchecked")
	private CtIncidentLink findWaypointMapping(UUID incidentGroupId, Session session) {
		for (CtIncidentLink l : groupMappings) {
			if (l.getRootId() == null && l.getIncidentGroupId() != null && l.getIncidentGroupId().equals(incidentGroupId)) {
				return l;
			}
		}
		List<CtIncidentLink> links = session.createQuery("FROM CtIncidentLink WHERE incidentGroupId = :groupid and rootId is null") //$NON-NLS-1$
				.setParameter("groupid",  incidentGroupId) //$NON-NLS-1$
				.list();
		if (links.isEmpty()) return null;
		return links.get(0);
	}
	@SuppressWarnings("unchecked")
	private CtIncidentLink findWaypointMapping(UUID incidentGroupId, UUID rootId, Session session) {
		for (CtIncidentLink l : groupMappings) {
			if (l.getIncidentGroupId()!= null && l.getIncidentGroupId().equals(incidentGroupId) && l.getRootId() != null && l.getRootId().equals(rootId)) {
				return l;
			}
		}
		List<CtIncidentLink> links = session.createQuery("FROM CtIncidentLink WHERE incidentGroupId = :groupid and rootId = :rootid ") //$NON-NLS-1$
				.setParameter("groupid",  incidentGroupId) //$NON-NLS-1$
				.setParameter("rootid",  rootId) //$NON-NLS-1$
				.list();
		if (links.isEmpty()) return null;
		return links.get(0);
	}
	@SuppressWarnings("unchecked")
	private CtIncidentLink findRootWaypointMapping(UUID rootId, Session session) {
		for (CtIncidentLink l : groupMappings) {
			if (l.getIncidentGroupId() == null && l.getRootId() != null &&  l.getRootId().equals(rootId)) {
				return l;
			}
		}
		List<CtIncidentLink> links = session.createQuery("FROM CtIncidentLink WHERE  rootId = :rootid and incidentGroupId is null ") //$NON-NLS-1$
				.setParameter("rootid",  rootId) //$NON-NLS-1$
				.list();
		if (links.isEmpty()) return null;
		return links.get(0);
	}
	/*
	 * displays warning dialog to user allowing them to cancel the processing
	 */
	private void displayWarnings(List<String> warnings) throws Exception{
		 if (!warnings.isEmpty()){
			 	final boolean[] cont = {false};
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), 
								Messages.IncidentJsonProcessor_WaringsTitle, 
								Messages.IncidentJsonProcessor_WarningsMessage,
								warnings,
								new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
						if (wd.open() == 0){
							cont[0] = true;
						}else{
							cont[0] = false;
						}
					}	
				});
				if (!cont[0]){
					throw new UserCancelledException(Messages.IncidentJsonProcessor_CanceledMsg);
				}
		 }
	}
	

	@Override
	public void afterSave(){
		for (Waypoint p : modifiedIncidents){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_MODIFIED, p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.IncidentJsonProcessor_NotificationError, ex.getMessage(), ex);
			}
		}
		for (Waypoint p : newIncidents){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_ADDED, p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.IncidentJsonProcessor_NotificationError2, ex.getMessage(), ex);
			}
		}
	}
	
	@Override
	public String getStatusMessage() {
		if (newIncidents.isEmpty() && modifiedIncidents.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		if (!newIncidents.isEmpty()){
			sb.append(MessageFormat.format(Messages.IncidentJsonProcessor_CreatedLabel, newIncidents.size()));
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
			sb.append(MessageFormat.format(Messages.IncidentJsonProcessor_ModifiedLabel, tmp.size()));
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