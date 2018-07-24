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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.json.IJsonProcessor;
import org.wcs.smart.cybertracker.importer.json.JsonCtParser;
import org.wcs.smart.cybertracker.importer.json.UserCancelledException;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Parser for parsing patrol data from CT JSON data. 
 * 
 * @author Emily
 *
 */
public class IncidentJsonProcessor implements IJsonProcessor {

	private List<String> warnings;
	
	private Set<Waypoint> newIncidents = new HashSet<>();
	private Set<Waypoint> modifiedIncidents = new HashSet<>();
	//resize value for apply to all option
	private Point allSize = null;
	
	public IncidentJsonProcessor() {
		warnings = new ArrayList<String>();
	}

	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception{
		//for this build we don't process anything
		if (true) return Collections.emptyList();
		
		newIncidents = new HashSet<>();
		
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();;
		
//		int observationFeatureCount = 0;
		for (JSONObject feature : features){
			JsonCtParser parser = new JsonCtParser();
			
			if (JsonCtParser.isTrackPoint(feature)) continue; //observationFeatureCount++;
			
			try{
				JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
				if (sighting == null) continue;
				
				String type = (String) sighting.get(ScreensUtil.RESULT_DATATYPE);
				String deviceId = (String) properties.get(JsonCtParser.DEVICE_ID);
				
				// Validate data type
				if (!IncidentPackageContribution.INCIDENT_RESOURCE_ID.equalsIgnoreCase(type)){
					//not an incident point; skip it
					continue;
				}

				
//				//read cybertracker patrol id and convert to uuid
//				String ctPatrolId = (String) sighting.get(ScreensUtil.RESULT_ID);
//				UUID ctPatrolUuid = UuidUtils.stringToUuid(ctPatrolId);
//				
//				//check the database for link; if not found check local links
//				CtPatrolLink link = (CtPatrolLink) session.get(CtPatrolLink.class, ctPatrolUuid);
//				if (link == null){
//					link = newPatrolLinks.get(ctPatrolUuid);
//				}
				
				Long cnt = QueryFactory.buildCountQuery(session, Waypoint.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"sourceId",IndepedentIncidentSource.KEY}).longValue();

				//Parse the waypoint information 				
				Waypoint wp = parser.createWaypoint(feature, session);
				warnings.addAll(parser.getWarnings());
				wp.setId((int)(cnt + 1));
				wp.setSourceId(IndepedentIncidentSource.KEY);
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				allSize = JsonCtParser.processImages(wp, allSize, session);
				
				//there is no position; likely skip on device; lets set to 0
				if (wp.getX() == null) wp.setX(0);
				if (wp.getY() == null) wp.setY(0);
				
				session.saveOrUpdate(wp);
				newIncidents.add(wp);
				
				processedFeatures.add(feature);
				
			}catch (Exception ex){
				//TODO: if there is a session.flush error we have a problem we need to stop and rollback
				CyberTrackerPlugIn.log(ex.getMessage() + ": " + feature.toJSONString(), ex); //$NON-NLS-1$
				warnings.add("Error parsing independent incident feature." + ex.getMessage());
			}
		}
		
		//display warnings to user; this may throw a cancelled exception if the user doesn't want to proceed
		displayWarnings(warnings);
				
		return processedFeatures;
	}
	
//	/**
//	 * returns 0 if error
//	 * 1 if ok
//	 * 2 if ok, but needs to configure groupWpStartDateTime to waypoint
//	 * 3 if ok but need to clear groupwpstartdatetime
//	 * @param sighting
//	 * @param legToUpdate
//	 * @param wp
//	 * @param applyAll
//	 * @param session
//	 * @return
//	 * @throws Exception 
//	 */
//	private int processGroup(JSONObject sighting, PatrolLeg legToUpdate, Waypoint wp, List<WaypointObservationAttribute> applyAll, Date groupStartTime, Session session) throws Exception{
//		if (!sighting.containsKey(ScreensUtil.RESULT_END_WAYPOINT_GROUP)){
//			//clear observations associated with 
//			wp.getObservations().clear();
//			addToExistingLeg(legToUpdate, wp, session);
//			
//			return 1;
//		}else{
//			if ("FALSE".equalsIgnoreCase((String)sighting.get(ScreensUtil.RESULT_END_WAYPOINT_GROUP))){ //$NON-NLS-1$
//				if (wp.getX() == null || wp.getY() == null){
//					//no location; add to previous 
//					if (addWaypointToLastObservation(legToUpdate, wp, session) != null) return 1;
//					return 0;
//				}else{
//					addToExistingLeg(legToUpdate, wp, session);
//					return 2;
//				}
//			}else if ("TRUE".equalsIgnoreCase((String)sighting.get(ScreensUtil.RESULT_END_WAYPOINT_GROUP))){ //$NON-NLS-1$
//				if (wp.getX() == null || wp.getY() == null){
//					//no location; add to previous 
//					PatrolWaypoint pw = addWaypointToLastObservation(legToUpdate, wp, session);
//					if (pw != null){
//						addAttributesToObservation(pw.getWaypoint().getObservations(), applyAll);
//						return 1;
//					}
//					return 0;
//				}else{
//					addToExistingLeg(legToUpdate, wp, session);
//					//update all waypoints since the start of the group to include the defaults
//					//and the after attributes
//					for (PatrolLegDay pld : legToUpdate.getPatrolLegDays()){
//						for (PatrolWaypoint pw : pld.getWaypoints()){
//							if (pw.getWaypoint().getDateTime().equals(groupStartTime) || 
//									pw.getWaypoint().getDateTime().after(groupStartTime)){
//								addAttributesToObservation(pw.getWaypoint().getObservations(), applyAll);
//							}
//						}
//					}
//					//update groupwpstartdatetime to null
//					return 3;
//				}
//			}
//		}
//		return 0;
//	}
//	
//	private void addAttributesToObservation(List<WaypointObservation> obs, List<WaypointObservationAttribute> attributeValues ){
//		for (WaypointObservation wo : obs){
//			for (WaypointObservationAttribute value : attributeValues){
//				boolean attributeExists = false;
//				for (WaypointObservationAttribute existing : wo.getAttributes()){
//					if (existing.getAttribute().equals(value.getAttribute())){
//						attributeExists = true;
//						break;
//					}
//				}
//				if (!attributeExists){
//					WaypointObservationAttribute toAdd = value.clone();
//					toAdd.setObservation(wo);
//					if (wo.getAttributes() == null) wo.setAttributes(new ArrayList<>());
//					wo.getAttributes().add(toAdd);
//				}
//			}
//		}
//	}
//	
//	private Waypoint addWaypointToLastWaypoint(Waypoint lastWaypoint, Waypoint newWaypoint, Session session){
//		if (lastWaypoint == null){
//			//we have a problem ; there is no last waypoint to add to
//			//we cannot create a new one because we don't have position
//			
//			//lets just return this new waypoint
//			session.saveOrUpdate(newWaypoint);
//			newIncidents.add(newWaypoint);
//			return newWaypoint;
//		}
//		
//		//merge observations into a single waypoint
//		for (WaypointObservation wo : newWaypoint.getObservations()){
//			wo.setWaypoint(lastWaypoint);
//			lastWaypoint.getObservations().add(wo);
//		}
//		//merge attachments
//		if (newWaypoint.getAttachments() != null && !newWaypoint.getAttachments().isEmpty()){
//			if (lastWaypoint.getAttachments() == null){
//				lastWaypoint.setAttachments(new ArrayList<>());
//			}
//			for (WaypointAttachment attachment: newWaypoint.getAttachments()){
//				attachment.setWaypoint(lastWaypoint);
//				lastWaypoint.getAttachments().add(attachment);
//			}
//		}
//		session.saveOrUpdate(lastWaypoint);
//		modifiedIncidents.add(lastWaypoint);
//		return lastWaypoint;
//	}
	
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
								"Incident Warnings", 
								"The following warnings were generated while processing incidents from the file.  Do you want to continue?",
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
					throw new UserCancelledException("Import cancelled by user.");
				}
		 }
	}
	

	@Override
	public void afterSave(){
		for (Waypoint p : modifiedIncidents){
			try{
				WaypointEventManager.getInstance().waypointModified(p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Error notifying system of modified waypoints.", ex.getMessage(), ex);
			}
		}
		for (Waypoint p : newIncidents){
			try{
				WaypointEventManager.getInstance().waypointModified(p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Error notifing system of new waypoints.", ex.getMessage(), ex);
			}
		}
	}
	
	


	@Override
	public String getStatusMessage() {
		if (newIncidents.isEmpty() && modifiedIncidents.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		if (!newIncidents.isEmpty()){
			sb.append(MessageFormat.format("Created {0} Incidents", newIncidents.size()));
			sb.append("("); //$NON-NLS-1$
			for(Waypoint p : newIncidents){
				sb.append(p.getId());
				sb.append(" "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")"); //$NON-NLS-1$
		}
		HashSet<Waypoint> tmp = new HashSet<>(modifiedIncidents);
		tmp.removeAll(newIncidents);
		if (tmp.size() > 0){
			sb.append(MessageFormat.format("Modified {0} Incidents", tmp.size()));
			sb.append("("); //$NON-NLS-1$
			for(Waypoint p : tmp){
				sb.append(p.getId());
				sb.append(" "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")"); //$NON-NLS-1$
		}
		return sb.toString();
	}
	
	
}
