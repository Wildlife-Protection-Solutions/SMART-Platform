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
package org.wcs.smart.connect.dataqueue.cybertracker.patrol;

import java.io.File;
import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.dataqueue.cybertracker.IJsonProcessor;
import org.wcs.smart.connect.dataqueue.cybertracker.JsonCtParser;
import org.wcs.smart.connect.dataqueue.cybertracker.JsonTrackUtils;
import org.wcs.smart.connect.dataqueue.cybertracker.UserCancelledException;
import org.wcs.smart.connect.dataqueue.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.patrol.export.PatrolJsonUtils;
import org.wcs.smart.cybertracker.patrol.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Parser for parsing patrol data from CT JSON data. 
 * 
 * @author Emily
 *
 */
public class PatrolJsonProcessor implements IJsonProcessor {
	
	private static final DateFormat DATEFORMAT = new SimpleDateFormat("yyyy/MM/dd");
	private static final DateFormat TIMEFORMAT = new SimpleDateFormat("HH:mm:ss");
	
	private static final IWaypointSource PATROL_WP_SRC = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class)
			.getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
	
	private List<String> warnings;
	
	private Set<Patrol> modifiedPatrols;
	private Set<Patrol> newPatrols = new HashSet<>();
	private HashMap<UUID, CtPatrolLink> newPatrolLinks;
	
	public PatrolJsonProcessor() {
		warnings = new ArrayList<String>();
	}

	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception{
		modifiedPatrols = new HashSet<Patrol>();
		newPatrolLinks = new HashMap<UUID, CtPatrolLink>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();;
		
		
		for (JSONObject feature : features){
			JsonCtParser parser = new JsonCtParser();
			
			try{
				JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
				if (sighting == null) continue;
				
				String type = (String) sighting.get(ScreensUtil.RESULT_DATATYPE);
				String deviceId = (String) properties.get(JsonCtParser.DEVICE_ID);
				
				// Validate data type
				if (!PatrolScreensUtil.DATATYPE_PATROL.equalsIgnoreCase(type)){
					//not a valid patrol point; skip it
					continue;
				}
				
				//Validate counter
				if (!sighting.containsKey(ScreensUtil.RESULT_OBSERVATION_COUNTER)){
					//no observation counter; we cannot process this
					continue;
				}
				Integer observationCounter = ((Double)sighting.get(ScreensUtil.RESULT_OBSERVATION_COUNTER)).intValue();
				
				
				//read cybertracker patrol id and convert to uuid
				String ctPatrolId = (String) sighting.get(ScreensUtil.RESULT_ID);
				UUID ctPatrolUuid = UuidUtils.stringToUuid(ctPatrolId);
				
				//check the database for link; if not found check local links
				CtPatrolLink link = (CtPatrolLink) session.get(CtPatrolLink.class, ctPatrolUuid);
				if (link == null){
					link = newPatrolLinks.get(ctPatrolUuid);
				}
				
				//ensure valid observation id
				if (link == null){
					//no patrol created yet, observation counter must be 1
					if (observationCounter != 1) continue;
				}else{
					//must be the next observation
					if (link.getLastObservationCnt()  + 1 != observationCounter) continue;					
				}
				
				//is this the end of the patrol
				boolean isPatrolEnd = sighting.containsKey(PatrolScreensUtil.END_PATROL_KEY) ;
				if (isPatrolEnd){
					//we want to find the patrol and update the end date
					//add the position to the track, but do not create an observation 
					//for this patrol
					Date dt = JsonUtils.JSON_DATE_FORMAT.parse((String)properties.get(JsonCtParser.DATETIME_KEY));
					
					if (link == null){
						//create a new patrol object
						link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
						
						//update patrol end date/time
						Patrol p = link.getPatrolLeg().getPatrol();
						p.setEndDate(dt);
						link.getPatrolLeg().setEndDate(SharedUtils.getDatePart(p.getEndDate(), true));
						p.createLegDays(session);
					}
					
					//update patrol end date and end time for last patrol leg day
					link.getPatrolLeg().getPatrol().setEndDate(SharedUtils.getDatePart(dt, false));
					PatrolLegDay pd = findLegDay(link.getPatrolLeg(), link.getPatrolLeg().getEndDate(), true, null, session);
					pd.setEndTime(new Time(dt.getTime()));
						
					//add point to track
					addPointToTrack(pd, parser.readXYFromProperties(feature),dt);
					
					//update last observation count
					link.setLastObservationCnt(observationCounter);
					processedFeatures.add(feature);
					continue;
				}
				
				//Parse the waypoint information 				
				Waypoint wp = parser.createWaypoint(feature, session);
				warnings.addAll(parser.getWarnings());
				wp.setId(observationCounter);
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				
				if (sighting.containsKey(ScreensUtil.RESULT_PAUSED)){
					//patrol paused; no observation; record only as track point
					if (link == null){
						link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
					}
					
					if (wp.getX() != null && wp.getY() != null){
						addPointToTrack(link.getPatrolLeg(), new Coordinate(wp.getX(), wp.getY()), wp.getDateTime(), session);
					}
					//update last observation count
					link.setLastObservationCnt(observationCounter);
					processedFeatures.add(feature);
					continue;
					
				}
				
				if (!sighting.containsKey(ScreensUtil.RESULT_NEW_WAYPOINT)){
					//assume this is a group attribute
					
					if (link == null){
						//create a new patrol
						link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
					}

					Date groupStartTime = link.getGroupStartTime();
					int groupResult = processGroup(sighting, link.getPatrolLeg(), wp, parser.getApplyToAdd(), groupStartTime, session);
					if (groupResult > 0){
						
						if (groupResult == 2){
							if (link.getGroupStartTime() == null){
								link.setGroupStartTime(wp.getDateTime());
							}
						}else if (groupResult == 3){
							link.setGroupStartTime(null);
						}
						
						if(wp.getX() != null && wp.getY() != null){
							addPointToTrack(link.getPatrolLeg(), new Coordinate(wp.getX(), wp.getY()), wp.getDateTime(), session);
						}
						link.setLastObservationCnt(observationCounter);
						processedFeatures.add(feature);
					}
					continue;
				}
				
				//Determine if this is a "Add to Last Waypoint" option
				boolean addToLast = ((String)sighting.get(ScreensUtil.RESULT_NEW_WAYPOINT)).equalsIgnoreCase("false");
				if (addToLast){
					if (link == null){
						//we have nothing to add this to; this is an error
						warnings.add("No patrol found for 'add to previous waypoint' observation. ");
						continue;
					}
					
					if (addWaypointToLastObservation(link.getPatrolLeg(), wp, session) == null) continue;
					link.setLastObservationCnt(observationCounter);
					processedFeatures.add(feature);
					continue;
				}
				
				//there is no position; likely skip on device; lets set to 0
				if (wp.getX() == null) wp.setX(0);
				if (wp.getY() == null) wp.setY(0);
				
				//We want to create a new waypoint and add it to the patrol
				if (link == null){
					link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
				}
				
				//add these observation to the selected patrol leg
				//TODO: potentially we could validate metadata
				addToExistingLeg(link.getPatrolLeg(), wp, session);
				if (link.getPatrolLeg().getPatrol().getUuid() != null) modifiedPatrols.add(link.getPatrolLeg().getPatrol());
				
				//add position to track log
				addPointToTrack(link.getPatrolLeg(), new Coordinate(wp.getX(), wp.getY()), wp.getDateTime(), session);
				
				//update last observation count
				link.setLastObservationCnt(observationCounter);
				processedFeatures.add(feature);
				
			}catch (Exception ex){
				//TODO: if there is a session.flush error we have a problem we need to stop and rollback
				CyberTrackerPlugIn.log(ex.getMessage() + ": " + feature.toJSONString(), ex);
				warnings.add("Error parsing feature information (feature will not be processed): " + ex.getMessage());
			}
		}
		
		//display warnings to user; this may throw a cancelled exception if the user doesn't want to proceed
		displayWarnings(warnings);
		
		final boolean[] cancel = new boolean[]{false};
		if (!newPatrolLinks.isEmpty()){
			//we need to ask the user if they want to create a new patrol or add to an existing patrol
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					PatrolDialog pd = new PatrolDialog(Display.getDefault().getActiveShell(), newPatrolLinks, session);
					if (pd.open() == Window.CANCEL){
						cancel[0] = true;
					}else{
						modifiedPatrols.addAll(pd.getMergedPatrols());
						newPatrols = pd.getNewPatrols();
					}
				}	
			});
		}
		if (cancel[0]){
			throw new UserCancelledException("User cancelled operation while assigning observations to patrols.");
		}
		
		//try processing track features
		PatrolJsonTrackProcessor trackProcessor = new PatrolJsonTrackProcessor();
		processedFeatures.addAll(trackProcessor.processJson(features, session));
		modifiedPatrols.addAll(trackProcessor.getModifiedPatrols());
		displayWarnings(trackProcessor.getWarnings());
		
		return processedFeatures;
	}
	
	/**
	 * returns 0 if error
	 * 1 if ok
	 * 2 if ok, but needs to configure groupWpStartDateTime to waypoint
	 * 3 if ok but need to clear groupwpstartdatetime
	 * @param sighting
	 * @param legToUpdate
	 * @param wp
	 * @param applyAll
	 * @param session
	 * @return
	 * @throws Exception 
	 */
	private int processGroup(JSONObject sighting, PatrolLeg legToUpdate, Waypoint wp, List<WaypointObservationAttribute> applyAll, Date groupStartTime, Session session) throws Exception{
		if (!sighting.containsKey(ScreensUtil.RESULT_END_WAYPOINT_GROUP)){
			//clear observations associated with 
			wp.getObservations().clear();
			addToExistingLeg(legToUpdate, wp, session);
			
			return 1;
		}else{
			if ("FALSE".equalsIgnoreCase((String)sighting.get(ScreensUtil.RESULT_END_WAYPOINT_GROUP))){
				if (wp.getX() == null || wp.getY() == null){
					//no location; add to previous 
					if (addWaypointToLastObservation(legToUpdate, wp, session) != null) return 1;
					return 0;
				}else{
					addToExistingLeg(legToUpdate, wp, session);
					return 2;
				}
			}else if ("TRUE".equalsIgnoreCase((String)sighting.get(ScreensUtil.RESULT_END_WAYPOINT_GROUP))){
				if (wp.getX() == null || wp.getY() == null){
					//no location; add to previous 
					PatrolWaypoint pw = addWaypointToLastObservation(legToUpdate, wp, session);
					if (pw != null){
						addAttributesToObservation(pw.getWaypoint().getObservations(), applyAll);
						return 1;
					}
					return 0;
				}else{
					addToExistingLeg(legToUpdate, wp, session);
					//update all waypoints since the start of the group to include the defaults
					//and the after attributes
					for (PatrolLegDay pld : legToUpdate.getPatrolLegDays()){
						for (PatrolWaypoint pw : pld.getWaypoints()){
							if (pw.getWaypoint().getDateTime().equals(groupStartTime) || 
									pw.getWaypoint().getDateTime().after(groupStartTime)){
								addAttributesToObservation(pw.getWaypoint().getObservations(), applyAll);
							}
						}
					}
					//update groupwpstartdatetime to null
					return 3;
				}
			}
		}
		return 0;
	}
	
	private void addAttributesToObservation(List<WaypointObservation> obs, List<WaypointObservationAttribute> attributeValues ){
		for (WaypointObservation wo : obs){
			for (WaypointObservationAttribute value : attributeValues){
				boolean attributeExists = false;
				for (WaypointObservationAttribute existing : wo.getAttributes()){
					if (existing.getAttribute().equals(value.getAttribute())){
						attributeExists = true;
						break;
					}
				}
				if (!attributeExists){
					WaypointObservationAttribute toAdd = value.clone();
					toAdd.setObservation(wo);
					if (wo.getAttributes() == null) wo.setAttributes(new ArrayList<>());
					wo.getAttributes().add(toAdd);
				}
			}
		}
	}
	
	private PatrolWaypoint addWaypointToLastObservation(PatrolLeg legToUpdate, Waypoint wp, Session session){
		//find previous waypoint
		//find the last waypoint by date/time in the legToUpdate
		PatrolWaypoint lastWaypoint = null;
		if (legToUpdate != null){
			for (PatrolLegDay pld : legToUpdate.getPatrolLegDays()){
				for (PatrolWaypoint pw: pld.getWaypoints()){
					if (lastWaypoint == null ||
							pw.getWaypoint().getDateTime().after(lastWaypoint.getWaypoint().getDateTime())){
						lastWaypoint = pw;
					}
				}
			}
		}
		
		if (lastWaypoint == null){
			//we have a problem ; there is no last waypoint to add to
			//we cannot create a new one because we don't have position
			
			//TODO: we probably need to update the last observation count
			//this observation needs to be lost; otherwise no other observations
			//can be processed
			return null;
		}
		
		//merge observations into a single waypoint
		for (WaypointObservation wo : wp.getObservations()){
			wo.setWaypoint(lastWaypoint.getWaypoint());
			lastWaypoint.getWaypoint().getObservations().add(wo);
		}
		//merge attachments
		if (wp.getAttachments() != null && !wp.getAttachments().isEmpty()){
			if (lastWaypoint.getWaypoint().getAttachments() == null){
				lastWaypoint.getWaypoint().setAttachments(new ArrayList<>());
			}
			for (WaypointAttachment attachment: wp.getAttachments()){
				attachment.setWaypoint(lastWaypoint.getWaypoint());
				lastWaypoint.getWaypoint().getAttachments().add(attachment);
			}
		}
		if (legToUpdate.getUuid() != null){
			session.saveOrUpdate(lastWaypoint);
			session.flush();
		}
		
		if (legToUpdate.getPatrol().getUuid() != null) modifiedPatrols.add(legToUpdate.getPatrol());
		
		return lastWaypoint;
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
								"Warnings", 
								"The following warnings were generated while parsing patrol data.  Do you want to continue?",
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
					throw new UserCancelledException("User cancelled operation due to warnings.");
				}
		 }
	}
	
	private CtPatrolLink createPatrolFromSighing(JSONObject sighting, String deviceId, UUID ctUuid, int observationCounter, Session session) throws Exception{
		Patrol p = new Patrol();
		p.setConservationArea(SmartDB.getCurrentConservationArea());
		String defaultValues = (String)sighting.get(PatrolScreensUtil.RESULT_DEFAULT_META_VALUES);
		CyberTrackerPatrol ct = PatrolJsonUtils.parsePatrolMetadata((JSONObject) (new JSONParser()).parse(defaultValues), sighting, session);
		
		
		String startDate = (String)sighting.get(ScreensUtil.RESULT_START_DATE);
		String startTime = (String)sighting.get(ScreensUtil.RESULT_START_TIME);
		
		
		Date dStartDate = DATEFORMAT.parse(startDate);
		p.setEndDate(dStartDate);
		p.setStartDate(dStartDate);
		
		p.setArmed(ct.isArmed());
		p.setComment(ct.getComment());
		p.setMandate(ct.getMandate());
		p.setObjective(ct.getObjective());
		p.setPatrolType(ct.getPatrolType());
		p.setStation(ct.getStation());
		p.setTeam(ct.getTeam());
		
		// create new leg and add members and set transport type
		PatrolLeg pl = p.addLeg();
		pl.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		pl.setStartDate(SmartUtils.combineDateTime(dStartDate, TIMEFORMAT.parse(startTime)));
		pl.setEndDate(SharedUtils.getDatePart(dStartDate, true));
		for (Employee member: ct.getMembers()){
			PatrolLegMember item = pl.addPatrolLegMember(member);
			if (ct.getPatrolType().requiresPilot()){
				if (member.equals(ct.getPilot())) item.setIsPilot(true);
			}
			if (member.equals(ct.getLeader())) item.setIsLeader(true);
		}
	
		pl.setType(ct.getPatrolTransportType());
		
		
		//make a single patrol leg day for the start date and time
		pl.createLegDays(session);		
		
		CtPatrolLink link = new CtPatrolLink();
		link.setDeviceId(deviceId);
		link.setCtUuid(ctUuid);
		link.setLastObservationCnt(observationCounter);
		link.setPatrolLeg(p.getFirstLeg());
		newPatrolLinks.put(ctUuid, link);
		return link;
	}
		
	private void addToExistingLeg(PatrolLeg addTo, Waypoint wp, Session session)
			throws Exception {
		
		PatrolLegDay addToD = findLegDay(addTo, wp.getDateTime(), true, null, session);
		PatrolWaypoint pw = addWaypointToLegDay(addToD, wp);
		
		if (addTo.getUuid() != null){
			if (wp.getAttachments() != null){
				for (WaypointAttachment wa : wp.getAttachments()){
					//the associated patrol waypoint has not been saved yet
					//so we need to fix up all attachment
					wa.computeFileLocation(new File(new File(
							SmartDB.getCurrentConservationArea().getFileDataStoreLocation(),
							PATROL_WP_SRC.getDatastoreFileLocation(addTo.getPatrol(), session)), wa.getFilename()));
					
				}
			}
			if (wp.getObservations() != null){
				for(WaypointObservation wo : wp.getObservations()){
					if (wo.getAttachments() != null){
						for (ObservationAttachment a : wo.getAttachments()){
							a.computeFileLocation(new File(new File(
									SmartDB.getCurrentConservationArea().getFileDataStoreLocation(),
									PATROL_WP_SRC.getDatastoreFileLocation(addTo.getPatrol(), session)), a.getFilename()));
						}
					}
				}
			}
			session.saveOrUpdate(wp);
			session.save(pw);
			session.saveOrUpdate(addToD.getPatrolLeg().getPatrol());
		}
	}

	private PatrolWaypoint addWaypointToLegDay(PatrolLegDay addToD, Waypoint wp){
		PatrolWaypoint pw = new PatrolWaypoint();
		pw.setPatrolLegDay(addToD);
		pw.setWaypoint(wp);
		if (addToD.getWaypoints() == null) addToD.setWaypoints(new ArrayList<PatrolWaypoint>());
		addToD.getWaypoints().add(pw);
		
		return pw;
	}
	

	@Override
	public void afterSave(){
		for (Patrol p : modifiedPatrols){
			try{
				PatrolEventManager.getInstance().patrolSaved(p, true);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Importing JSON Data", ex.getMessage(), ex);
			}
		}
		for (Patrol p : newPatrols){
			try{
				PatrolEventManager.getInstance().patrolAdded(p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Importing JSON Data", ex.getMessage(), ex);
			}
		}
	}
	

	

	
	private static final PatrolLegDay findLegDay(PatrolLeg leg, Date day, boolean create, Time startTime, Session session){
		for (PatrolLegDay pld : leg.getPatrolLegDays()){
			if (SharedUtils.isSameDate(pld.getDate(), day)){
				return pld;
			}
		}
		if (!create) return null;
		
		PatrolLegDay pld = new PatrolLegDay();
		pld.setDate(SharedUtils.getDatePart(day, false));
		
		Calendar tmp =GregorianCalendar.getInstance();
		tmp.setTime(day);
		Calendar time = GregorianCalendar.getInstance();
		time.setTimeInMillis(0);
		time.set(Calendar.HOUR_OF_DAY, tmp.get(Calendar.HOUR_OF_DAY));
		time.set(Calendar.MINUTE, tmp.get(Calendar.MINUTE));
		time.set(Calendar.SECOND, tmp.get(Calendar.SECOND));
		time.set(Calendar.MILLISECOND, 0);

		pld.setRestMinutes(0);
		
		pld.setWaypoints(new ArrayList<PatrolWaypoint>());
		leg.getPatrolLegDays().add(pld);
		pld.setPatrolLeg(leg);
		
		
		//update leg and patrol dates as necessary
		if (leg.getStartDate().after(pld.getDate())){
			leg.setStartDate(pld.getDate());
		}
		if (leg.getEndDate().before(pld.getDate())){
			leg.setEndDate(SharedUtils.getDatePart(pld.getDate(), true));
		}
		if (leg.getPatrol().getStartDate().after(pld.getDate())){
			leg.getPatrol().setStartDate(pld.getDate());
		}
		if (leg.getPatrol().getEndDate().before(pld.getDate())){
			leg.getPatrol().setEndDate(pld.getDate());
		}
		
		//make sure there is at least one legday for each day between the start and end date
		leg.createLegDays(session);
		
		if (startTime == null){
			pld.setStartTime(new Time(time.getTime().getTime()));
		}else{
			pld.setStartTime(startTime);
		}
		pld.setEndTime(new Time(SmartUtils.getMidnight().getTime()- 1));
		
		if (SharedUtils.isSameDate(pld.getDate(), leg.getStartDate())){
			leg.setStartDate( SmartUtils.combineDateTime(leg.getStartDate(), pld.getStartTime()) );
		}
		if (SharedUtils.isSameDate(pld.getDate(), leg.getEndDate())){
			leg.setEndDate( SmartUtils.combineDateTime(leg.getEndDate(), pld.getEndTime()) );
		}
		return pld;
	}
	
	public static final void addPointToTrack(PatrolLegDay pld, Coordinate pnt, Date time) throws Exception{
		if (pnt == null) return;
		if (pld == null) return;
		if (pld.getTrack() == null){
			Track patrolTrack = new Track();
			patrolTrack.setPatrolLegDay(pld);
			pld.setTrack(patrolTrack);
		}
		
		LineString ll = JsonTrackUtils.addPointToTrack(pld.getTrack().getLineString(), pnt, time);
		pld.getTrack().setLineString(ll);
	}
	
	public static final void addPointToTrack(PatrolLeg leg, Coordinate pnt, Date time, Session session) throws Exception{
		if (pnt == null) return;
		PatrolLegDay pld = findLegDay(leg, time, true, null, session);
		addPointToTrack(pld, pnt, time);
	}

	@Override
	public String getStatusMessage() {
		if (newPatrols.isEmpty() && modifiedPatrols.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		if (!newPatrols.isEmpty()){
			sb.append(MessageFormat.format("Created {0} Patrols ", newPatrols.size()));
			sb.append("(");
			for(Patrol p : newPatrols){
				sb.append(p.getId());
				sb.append(" ");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")");
		}
		HashSet<Patrol> tmp = new HashSet<Patrol>(modifiedPatrols);
		tmp.removeAll(newPatrols);
		if (tmp.size() > 0){
			sb.append(MessageFormat.format("Modified {0} Patrols ", tmp.size()));
			sb.append("(");
			for(Patrol p : tmp){
				sb.append(p.getId());
				sb.append(" ");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")");
		}
		return sb.toString();
	}
}
