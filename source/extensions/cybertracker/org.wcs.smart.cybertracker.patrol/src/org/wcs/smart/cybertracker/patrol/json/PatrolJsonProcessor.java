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
package org.wcs.smart.cybertracker.patrol.json;

import java.nio.file.Paths;
import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.json.IJsonProcessor;
import org.wcs.smart.cybertracker.importer.json.JsonCtParser;
import org.wcs.smart.cybertracker.importer.json.JsonTrackUtils;
import org.wcs.smart.cybertracker.patrol.export.PatrolJsonUtils;
import org.wcs.smart.cybertracker.patrol.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolWpLink;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.meta.PatrolScreenOptionMeta;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Parser for parsing patrol data from CT JSON data. 
 * 
 * @author Emily
 *
 */
public class PatrolJsonProcessor implements IJsonProcessor {
	
	private static final IWaypointSource PATROL_WP_SRC = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class)
			.getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
	
	private List<String> warnings;
	
	private Set<Patrol> modifiedPatrols;
	private Set<Patrol> newPatrols = new HashSet<>();
	private HashMap<UUID, CtPatrolLink> newPatrolLinks;
	
	//resize value for apply to all option
	private Point allSize = null;
	
	private ConservationArea ca;
	
	public PatrolJsonProcessor() {
		this.ca = SmartDB.getCurrentConservationArea();
		warnings = new ArrayList<String>();
	}

	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception{
		modifiedPatrols = new HashSet<Patrol>();
		newPatrolLinks = new HashMap<UUID, CtPatrolLink>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();;
		
		int observationFeatureCount = 0;
		for (JSONObject feature : features){
			JsonCtParser parser = new JsonCtParser();
			
			if (!JsonCtParser.isTrackPoint(feature)) observationFeatureCount++;
			
			try{
				JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
				if (sighting == null) continue;
				
				String type = (String) sighting.get(ScreensUtil.RESULT_DATATYPE);
				String deviceId = (String) properties.get(JsonCtParser.DEVICE_ID);
				
				// Validate data type
				if (!PatrolScreenOptionMeta.PATROL_RESOURCE_ID.equalsIgnoreCase(type)){
					//not a valid patrol point; skip it
					continue;
				}
				
				Integer observationCounter = parser.parseObservationCounter(sighting);
				if (observationCounter == null) continue;
								
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
				
				//is the end of this leg and needs a new leg
				//is this the end of the patrol
				boolean changeLeg = false;
				boolean isPatrolEnd = sighting.containsKey(PatrolScreensUtil.END_PATROL_KEY) ;
				if (sighting.containsKey(JsonCtParser.OBSERVATION_TYPE_KEY)) {
					String value = (String) sighting.get(JsonCtParser.OBSERVATION_TYPE_KEY);
					if (value.trim().equalsIgnoreCase(JsonCtParser.OBSERVATION_TYPE_END_PATROL_KEY)) {
						isPatrolEnd = true;
					}
					
					if (value.trim().equalsIgnoreCase(JsonCtParser.OBSERVATION_TYPE_CHANGE_PATROL_KEY)) {
						changeLeg = true;
					}
				}
				
				if (changeLeg) {
					//end the current leg and start a new one
					Date dt = JsonUtils.parseJsonDateTime((String)properties.get(JsonCtParser.DATETIME_KEY));
					
					if (link != null) {					
						//update the leg end time
						PatrolLegDay pd = findLegDay(link.getPatrolLeg(), dt, true, new Time(SmartUtils.getMidnight().getTime()), session);
						//PatrolLegDay pd = findLegDay(link.getPatrolLeg(), dt, true, null, session);
						pd.setEndTime(new Time(dt.getTime()));
						
						//start a new leg
						String defaultValues = (String)sighting.get(PatrolScreensUtil.RESULT_DEFAULT_META_VALUES);
						CyberTrackerPatrol ct = PatrolJsonUtils.parsePatrolMetadata((JSONObject) (new JSONParser()).parse(defaultValues), sighting, ca, session);
						
						PatrolLeg newLeg = addLegFromSighting(link.getPatrolLeg().getPatrol(), ct, sighting, deviceId, ctPatrolUuid, observationCounter, dt, session);

						//update the link; we started a new leg									
						//create a new link
						if (!newPatrolLinks.values().contains(link)) {
							CtPatrolLink oldLink = new CtPatrolLink();
							oldLink.setCtUuid(UUID.randomUUID());
							oldLink.setPatrolLeg(link.getPatrolLeg());
							oldLink.setDeviceId(link.getDeviceId());
							oldLink.setLastObservationCnt(-1);
							oldLink.setGroupStartTime(null);
							oldLink.setWaypointLinks(new ArrayList<>());
							session.save(oldLink);
							
						}
						link.setPatrolLeg(newLeg);	
						
					}else {
						throw new Exception("Change patrol observation type cannot be processed before the Start Patrol observation type."); //$NON-NLS-1$
					}
				}
				
				if (isPatrolEnd){
					//we want to find the patrol and update the end date
					//add the position to the track, but do not create an observation 
					//for this patrol
					Date dt = JsonUtils.parseJsonDateTime((String)properties.get(JsonCtParser.DATETIME_KEY));
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
					link.getPatrolLeg().setEndDate(SharedUtils.getDatePart(dt, true));
					PatrolLegDay pd = findLegDay(link.getPatrolLeg(), SharedUtils.getDatePart(link.getPatrolLeg().getEndDate(), false), true, null, session);
					pd.setEndTime(new Time(dt.getTime()));
						
					//add point to track
					addPointToTrack(pd, parser.readXYFromProperties(feature),dt);
					
					//update last observation count
					link.setLastObservationCnt(observationCounter);
					processedFeatures.add(feature);
					if (link.getPatrolLeg().getPatrol().getUuid() != null) modifiedPatrols.add(link.getPatrolLeg().getPatrol());
					continue;
				}
				
				//Parse the waypoint information 				
				Waypoint wp = parser.createWaypoint(feature, ca, session);
				warnings.addAll(parser.getWarnings());
				wp.setId(observationCounter);
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				allSize = JsonCtParser.processImages(wp, allSize, session);
				
				
				boolean noObservation = false;
				//patrol paused; no observation; record only as track point
				//same is true for NewPatrol or ChangePatrol observation type
				boolean isPaused = false;
				boolean isResumed = false;
				if (sighting.containsKey(ScreensUtil.RESULT_PAUSED)) {
					isPaused = true;
				}else if (sighting.containsKey(JsonCtParser.OBSERVATION_TYPE_KEY)) {
					if (((String) sighting.get(JsonCtParser.OBSERVATION_TYPE_KEY)).trim().equalsIgnoreCase(JsonCtParser.OBSERVATION_TYPE_PAUSE_PATROL_KEY)) {
						isPaused = true;
					}
					if (((String) sighting.get(JsonCtParser.OBSERVATION_TYPE_KEY)).trim().equalsIgnoreCase(JsonCtParser.OBSERVATION_TYPE_RESUME_PATROL_KEY)) {
						isResumed = true;
					}
				}
				if (isResumed) {
					//compute rest times
					if (link != null) {
						Date dt = JsonUtils.parseJsonDateTime((String)properties.get(JsonCtParser.DATETIME_KEY));
						PatrolLegDay currentDay = findLegDay(link.getPatrolLeg(), dt, true, new Time(SmartUtils.getMidnight().getTime()), session);
						//the pause event is recorded as a track point; not a waypoint
						//so find the previous track point
						List<PatrolLegDay> sorts = new ArrayList<PatrolLegDay>(currentDay.getPatrolLeg().getPatrolLegDays());
						sorts.sort((a,b)->-1*a.getDate().compareTo(b.getDate()));
						
						Date pausepoint = null;
						List<Double> lastValues = new ArrayList<>();

						for (PatrolLegDay d : sorts) {
							if (d.getTrack() != null && d.getTrack().getLineStrings() != null) {
								for (LineString ll : d.getTrack().getLineStrings()) {
									lastValues.add(ll.getCoordinateN(ll.getNumPoints() - 1).getZ());
								}
							}
						}
						lastValues.sort((a,b)->-1*a.compareTo(b));
						pausepoint = new Date(lastValues.get(0).longValue());
						
						DateFormat dd = DateFormat.getDateTimeInstance();
						dd.setTimeZone(Track.ZTIMEZONE);
						pausepoint = DateFormat.getDateTimeInstance().parse(dd.format(pausepoint));
						
						LocalDateTime end = (new java.sql.Timestamp(dt.getTime())).toLocalDateTime();
						LocalDateTime start = (new java.sql.Timestamp(pausepoint.getTime())).toLocalDateTime();
						LocalDateTime c = start;
						LocalDate lde = LocalDate.from(end);
						while(c.isBefore(end)) {
							LocalDate ld = LocalDate.from(c);
							if (ld.isEqual(lde)) {
								PatrolLegDay cd = findLegDay(link.getPatrolLeg(), java.sql.Date.valueOf(ld), true, new Time(SmartUtils.getMidnight().getTime()), session);
								long resttime = Math.round( ChronoUnit.SECONDS.between(c,end) / 60.0 );
								resttime += cd.getRestMinutes() == null ? 0 : cd.getRestMinutes();
								cd.setRestMinutes((int)resttime);
								//time from ld to end 
								break;
							}else {
								PatrolLegDay cd = findLegDay(link.getPatrolLeg(), java.sql.Date.valueOf(ld), true, new Time(SmartUtils.getMidnight().getTime()), session);
								//time from start to end of day c to end of day
								LocalDateTime endofday = c.withHour(23).withMinute(59).withSecond(59).withNano(999999);
								long resttime = Math.round( ChronoUnit.SECONDS.between(c,endofday) / 60.0 );
								resttime += cd.getRestMinutes() == null ? 0 : cd.getRestMinutes();
								cd.setRestMinutes((int)resttime);
							}
							
							//set current time to mighnight plus one day
							c = c.withHour(0).withMinute(0).withSecond(0).withNano(0).plus(1, ChronoUnit.DAYS);
						}
					}
				}
				
				noObservation = changeLeg || isPaused || isResumed;
				if (!noObservation) {
					//check if this is the start of the patrol
					if (sighting.containsKey(JsonCtParser.OBSERVATION_TYPE_KEY)) {
						String value = (String) sighting.get(JsonCtParser.OBSERVATION_TYPE_KEY);
						if (value.trim().equalsIgnoreCase(JsonCtParser.OBSERVATION_TYPE_START_PATROL_KEY)) {
							noObservation = true;
						}
					}
				}
				
				if (noObservation){
					if (link == null){
						link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
					}
					
					if (wp.getRawX() != null && wp.getRawY() != null){
						addPointToTrack(link.getPatrolLeg(), new Coordinate(wp.getRawX(), wp.getRawY()), wp.getDateTime(), session);
					}
					//update last observation count
					link.setLastObservationCnt(observationCounter);
					processedFeatures.add(feature);
					modifiedPatrols.add(link.getPatrolLeg().getPatrol());
					continue;				
				}
				
				//there is no position; likely skip on device; lets set to 0
				if (wp.getRawX() == null) wp.setRawX(0);
				if (wp.getRawY() == null) wp.setRawY(0);
				
				//here we have two versions - the add to last option that
				//is the old ct and smart mobile way OR we have a rootId/sighting
				//group id (SMART7)
				UUID ctRootId = null;
				UUID ctObsGroup = null;
				if (properties.containsKey(JsonCtParser.ROOT_ID_KEY)) ctRootId = UuidUtils.stringToUuid((String)properties.get(JsonCtParser.ROOT_ID_KEY));
				if (sighting.containsKey(ScreensUtil.RESULT_SIGHTINGGROUPID)) ctObsGroup = UuidUtils.stringToUuid((String)sighting.get(ScreensUtil.RESULT_SIGHTINGGROUPID));
				
				if (ctRootId == null || ctObsGroup == null) {
					//this is the old way of processing 
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
							
							if(wp.getRawX() != null && wp.getRawY() != null){
								addPointToTrack(link.getPatrolLeg(), new Coordinate(wp.getRawX(), wp.getRawY()), wp.getDateTime(), session);
							}
							link.setLastObservationCnt(observationCounter);
							processedFeatures.add(feature);
						}
						continue;
					}
	
					
					//Determine if this is a "Add to Last Waypoint" option
					boolean addToLast = false;
					Object v = sighting.get(ScreensUtil.RESULT_NEW_WAYPOINT);
					Boolean isNew = JsonUtils.convertToBoolean(v);
					if (isNew == null) {
						addToLast = false;
					}else {
						addToLast = (isNew == false);
					}
					
					if (addToLast){
						if (link == null){
							//we have nothing to add this to; this is an error
							warnings.add(Messages.PatrolJsonProcessor_NoPatrolFound);
							continue;
						}
						
						if (addWaypointToLastObservation(link.getPatrolLeg(), wp, session) == null) continue;
						link.setLastObservationCnt(observationCounter);
						processedFeatures.add(feature);
						modifiedPatrols.add(link.getPatrolLeg().getPatrol());
						continue;
					}
				
					//We want to create a new waypoint and add it to the patrol
					if (link == null){
						link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
					}

					//add these observation to the selected patrol leg
					addToExistingLeg(link.getPatrolLeg(), wp, session);
				}else {
					//we want to find the waypoint and/or observation group to add to
					//first see if we can find the waypoint and observation group					
					Waypoint mwp = null;
					WaypointObservationGroup mwpg = null;
					if(link != null && link.getWaypointLinks() != null) {
						UUID tomerge = null;
						UUID obsmerge = null;
						for (CtPatrolWpLink l : link.getWaypointLinks()) {
							if (l.getCtRootId().equals(ctRootId)) {
								tomerge = l.getWaypointUuid();
								if (l.getCtGroupId().equals(ctObsGroup)) {
									obsmerge = l.getObservationGroupUuid();
								}
							}
						}
						
						if (tomerge != null) {
							mwp = session.get(Waypoint.class, tomerge);
							if (obsmerge != null) {
								for (WaypointObservationGroup g : mwp.getObservationGroups()) {
									if (g.getUuid().equals(obsmerge)) {
										mwpg = g;
										break;
									}
								}
							}
						}
					}
					
					if (mwpg != null) {
						if (mwpg.getObservations() == null) mwpg.setObservations(new ArrayList<>());
						//merge wp observations with this group
						for (WaypointObservation wo : wp.getAllObservations()) {
							wo.setObservationGroup(mwpg);
							mwpg.getObservations().add(wo);
						}
						//copy attachments
						if (wp.getAttachments() != null) {
							if (mwpg.getWaypoint().getAttachments() == null) mwpg.getWaypoint().setAttachments(new ArrayList<>());						
							for (WaypointAttachment wa : wp.getAttachments()) {
								wa.setWaypoint(mwpg.getWaypoint());
								mwpg.getWaypoint().getAttachments().add(wa);
								
								wa.computeFileLocation(Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
										.resolve(PATROL_WP_SRC.getDatastoreFileLocation(link.getPatrolLeg().getPatrol(), session))
										.resolve(wa.getFilename()));
							
							}
						}
					}else if (mwp != null) {
						//create a new group with these observations
						if (mwp.getObservationGroups() == null) mwp.setObservationGroups(new ArrayList<>());
						WaypointObservationGroup newGroup = new WaypointObservationGroup();
						newGroup.setObservations(new ArrayList<>());
						newGroup.setWaypoint(mwp);
						mwp.getObservationGroups().add(newGroup);
						for (WaypointObservation wo : wp.getAllObservations()) {
							wo.setObservationGroup(newGroup);
							newGroup.getObservations().add(wo);
						}
						
						//copy attachments
						if (wp.getAttachments() != null) {
							if (mwp.getAttachments() == null) mwp.setAttachments(new ArrayList<>());						
							for (WaypointAttachment wa : wp.getAttachments()) {
								wa.setWaypoint(mwp);
								mwp.getAttachments().add(wa);
							
								wa.computeFileLocation(Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
										.resolve(PATROL_WP_SRC.getDatastoreFileLocation(link.getPatrolLeg().getPatrol(), session))
										.resolve(wa.getFilename()));
							}
						}
						
						if (link.getPatrolLeg().getPatrol().getUuid() != null) session.save(newGroup);
						
						//update patrol links
						CtPatrolWpLink wplink = new CtPatrolWpLink();
						wplink.setLink(link);
						wplink.setCtGroupId(ctObsGroup);
						wplink.setCtRootId(ctRootId);
						wplink.setWaypointUuid(mwp.getUuid());
						wplink.setObservationGroupUuid(newGroup.getUuid());
						
					}else {
						//We want to create a new waypoint and add it to the patrol
						if (link == null){
							link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
						}
						//we want this newly created waypoint
						addToExistingLeg(link.getPatrolLeg(), wp, session);
						
						if (link.getPatrolLeg().getPatrol().getUuid() != null) session.saveOrUpdate(wp);
						
						//update patrol links
						CtPatrolWpLink wplink = new CtPatrolWpLink();
						wplink.setLink(link);
						wplink.setCtGroupId(ctObsGroup);
						wplink.setCtRootId(ctRootId);
						wplink.setWaypointUuid(wp.getUuid());
						wplink.setObservationGroupUuid(wp.getObservationGroups().get(0).getUuid());
						link.getWaypointLinks().add(wplink);
					}
				}
				
				

				
				if (link.getPatrolLeg().getPatrol().getUuid() != null) modifiedPatrols.add(link.getPatrolLeg().getPatrol());
				
				//add position to track log
				addPointToTrack(link.getPatrolLeg(), new Coordinate(wp.getRawX(), wp.getRawY()), wp.getDateTime(), session);
				
				//update last observation count
				link.setLastObservationCnt(observationCounter);
				processedFeatures.add(feature);
				
			}catch (Exception ex){
				//TODO: if there is a session.flush error we have a problem we need to stop and rollback
				CyberTrackerPlugIn.log(ex.getMessage() + ": " + feature.toJSONString(), ex); //$NON-NLS-1$
				warnings.add(Messages.PatrolJsonProcessor_ParseError + ex.getMessage());
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
					try{
						PatrolDialog pd = new PatrolDialog(Display.getDefault().getActiveShell(), newPatrolLinks, session);
						if (pd.open() == Window.CANCEL){
							cancel[0] = true;
						}else{
							modifiedPatrols.addAll(pd.getMergedPatrols());
							newPatrols = pd.getNewPatrols();
						}
					}catch (Exception ex){
						CyberTrackerPlugIn.displayError(Messages.PatrolJsonProcessor_ErrorDialog, Messages.PatrolJsonProcessor_ErrorMesg + ex.getMessage(), ex);
						cancel[0] = true;
					}
				}	
			});
		}
		if (cancel[0]){
			throw new UserCancelledException(Messages.PatrolJsonProcessor_UserCancelled);
		}
		
		if (observationFeatureCount > 0 && processedFeatures.size() == 0){
			//there is at least one observation feature; but nothing could be processed
			//then we don't want to process track features because it is likely
			//these features should not be processed.
			//see ticket: #1877
			return processedFeatures;
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
			wp.getObservationGroups().clear();
			addToExistingLeg(legToUpdate, wp, session);
			
			return 1;
		}else{
			if ("FALSE".equalsIgnoreCase((String)sighting.get(ScreensUtil.RESULT_END_WAYPOINT_GROUP))){ //$NON-NLS-1$
				if (wp.getRawX() == null || wp.getRawY() == null){
					//no location; add to previous 
					if (addWaypointToLastObservation(legToUpdate, wp, session) != null) return 1;
					return 0;
				}else{
					addToExistingLeg(legToUpdate, wp, session);
					return 2;
				}
			}else if ("TRUE".equalsIgnoreCase((String)sighting.get(ScreensUtil.RESULT_END_WAYPOINT_GROUP))){ //$NON-NLS-1$
				if (wp.getRawX() == null || wp.getRawY() == null){
					//no location; add to previous 
					PatrolWaypoint pw = addWaypointToLastObservation(legToUpdate, wp, session);
					if (pw != null){
						if (!pw.getWaypoint().getObservationGroups().isEmpty())
							addAttributesToObservation(pw.getWaypoint().getObservationGroups().get(0).getObservations(), applyAll);
						return 1;
					}
					return 0;
				}else{
					addToExistingLeg(legToUpdate, wp, session);
					if (groupStartTime == null) {
						//see bug: https://app.assembla.com/spaces/smart-cs/tickets/2868
						//this case occurs when we have a group with only a single observation
						addAttributesToObservation(wp.getObservationGroups().get(0).getObservations(), applyAll);
					}else {
						//update all waypoints since the start of the group to include the defaults
						//and the after attributes
						for (PatrolLegDay pld : legToUpdate.getPatrolLegDays()){
							for (PatrolWaypoint pw : pld.getWaypoints()){
								if (pw.getWaypoint().getDateTime().equals(groupStartTime) || 
										pw.getWaypoint().getDateTime().after(groupStartTime)){
									
									if (!pw.getWaypoint().getObservationGroups().isEmpty())
										addAttributesToObservation(pw.getWaypoint().getObservationGroups().get(0).getObservations(), applyAll);
								}
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
			//this observation needs to be lost; otherwise no other observations
			//can be processed
			return null;
		}
		
		if (lastWaypoint.getWaypoint().getObservationGroups().isEmpty()) {
			WaypointObservationGroup g = new WaypointObservationGroup();
			g.setWaypoint(lastWaypoint.getWaypoint());
			lastWaypoint.getWaypoint().getObservationGroups().add(g);
			g.setObservations(new ArrayList<>());
		}
		WaypointObservationGroup toadd = lastWaypoint.getWaypoint().getObservationGroups().get(0);
		//merge observations into a single waypoint
		for (WaypointObservationGroup grp : wp.getObservationGroups()){
			for (WaypointObservation wo : grp.getObservations()){
				wo.setObservationGroup(toadd);
				toadd.getObservations().add(wo);
			}
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
								Messages.PatrolJsonProcessor_WarningsLabel, 
								Messages.PatrolJsonProcessor_WarningsMsg,
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
					throw new UserCancelledException(Messages.PatrolJsonProcessor_UserCancelled2);
				}
		 }
	}
	
	private CtPatrolLink createPatrolFromSighing(JSONObject sighting, String deviceId, UUID ctUuid, int observationCounter, Session session) throws Exception{
		Patrol p = new Patrol();
		p.setConservationArea(ca);
		String defaultValues = (String)sighting.get(PatrolScreensUtil.RESULT_DEFAULT_META_VALUES);
		CyberTrackerPatrol ct = PatrolJsonUtils.parsePatrolMetadata((JSONObject) (new JSONParser()).parse(defaultValues), sighting, ca, session);
		
		
		String startDate = (String)sighting.get(ScreensUtil.RESULT_START_DATE);		
		Date dStartDate = PatrolJsonUtils.DATEFORMAT.parse(startDate);
		p.setEndDate(dStartDate);
		p.setStartDate(dStartDate);
		
		p.setArmed(ct.isArmed());
		p.setComment(ct.getComment());
		p.setObjective(ct.getObjective());
		p.setPatrolType(ct.getPatrolType());
		p.setStation(ct.getStation());
		p.setTeam(ct.getTeam());
		
		//add custom attributes
		p.setCustomAttributes(new ArrayList<>());
		for (PatrolAttributeValue v : ct.getCustomAttributes()) {
			v.setPatrol(p);
			p.getCustomAttributes().add(v);
		}
		
		String startTime = (String)sighting.get(ScreensUtil.RESULT_START_TIME);
		Date startDateTime = SmartUtils.combineDateTime(dStartDate, PatrolJsonUtils.TIMEFORMAT.parse(startTime));
		
		PatrolLeg pl = addLegFromSighting(p, ct, sighting, deviceId, ctUuid, observationCounter, startDateTime, session);
		
		// create new leg and add members and set transport type
		CtPatrolLink link = new CtPatrolLink();
		link.setDeviceId(deviceId);
		link.setCtUuid(ctUuid);
		link.setLastObservationCnt(observationCounter);
		link.setPatrolLeg(pl);
		link.setWaypointLinks(new ArrayList<>());
		newPatrolLinks.put(ctUuid, link);
		
		return link;
	}
		
	private PatrolLeg addLegFromSighting(Patrol patrol, CyberTrackerPatrol ct,			
			JSONObject sighting,
			String deviceId, UUID ctUuid, int observationCounter,
			Date startDateTime,
			Session session) throws Exception{

		PatrolLeg pl = patrol.addLeg();
		pl.setMandate(ct.getMandate());
		pl.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		pl.setStartDate(startDateTime);
		pl.setEndDate(SharedUtils.getDatePart(pl.getStartDate(), true));
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
		
		return pl;
	}
	
	private void addToExistingLeg(PatrolLeg addTo, Waypoint wp, Session session)
			throws Exception {
		
//		PatrolLegDay addToD = findLegDay(addTo, wp.getDateTime(), true, null, session);
		PatrolLegDay addToD = findLegDay(addTo, wp.getDateTime(), true, new Time(SmartUtils.getMidnight().getTime()), session);
		PatrolWaypoint pw = addWaypointToLegDay(addToD, wp);
		
		if (addTo.getUuid() != null){
			if (wp.getAttachments() != null){
				for (WaypointAttachment wa : wp.getAttachments()){
					//the associated patrol waypoint has not been saved yet
					//so we need to fix up all attachment
					wa.computeFileLocation(Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
							.resolve(PATROL_WP_SRC.getDatastoreFileLocation(addTo.getPatrol(), session))
							.resolve(wa.getFilename()));
				}
			}
			
			for(WaypointObservation wo : wp.getAllObservations()){
				if (wo.getAttachments() != null){
					for (ObservationAttachment a : wo.getAttachments()){
						a.computeFileLocation(Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
								.resolve(PATROL_WP_SRC.getDatastoreFileLocation(addTo.getPatrol(), session))
								.resolve(a.getFilename()));
					}
				}
			}
			
			session.saveOrUpdate(wp);
			session.saveOrUpdate(addToD);
			session.save(pw);
			session.saveOrUpdate(addToD.getPatrolLeg().getPatrol());
			session.flush();
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
				CyberTrackerPlugIn.displayError(Messages.PatrolJsonProcessor_ErrorTitle, ex.getMessage(), ex);
			}
		}
		for (Patrol p : newPatrols){
			try{
				PatrolEventManager.getInstance().patrolAdded(p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.PatrolJsonProcessor_ErrorTitle, ex.getMessage(), ex);
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
		
		PatrolLegDay start = null;
		for (PatrolLegDay pld : leg.getPatrolLegDays()) {
			if (start == null || pld.getDate().before(start.getDate())) {
				start = pld;
			}
		}
		//configure the start time here so legs get created correctly
		leg.setStartDate(SmartUtils.combineDateTime(leg.getStartDate(), start.getStartTime()));
		
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
		
		//create a copy of the tracks to modify
		List<LineString> lineStrings = new ArrayList<>(pld.getTrack().getLineStrings());
		if (!lineStrings.isEmpty()) {
			LineString ll = JsonTrackUtils.addPointToTrack(lineStrings.get(lineStrings.size()-1), pnt, time);
			lineStrings.set(lineStrings.size()-1, ll);
			pld.getTrack().setLineStrings(lineStrings);
		} else {
			LineString ll = JsonTrackUtils.addPointToTrack(null, pnt, time);
			pld.getTrack().setLineStrings(Arrays.asList(ll));
		}
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
			sb.append(MessageFormat.format(Messages.PatrolJsonProcessor_CreatedMsg, newPatrols.size()));
			sb.append("("); //$NON-NLS-1$
			for(Patrol p : newPatrols){
				sb.append(p.getId());
				sb.append(" "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")"); //$NON-NLS-1$
		}
		HashSet<Patrol> tmp = new HashSet<Patrol>(modifiedPatrols);
		tmp.removeAll(newPatrols);
		if (tmp.size() > 0){
			sb.append(MessageFormat.format(Messages.PatrolJsonProcessor_ModifiedMsg, tmp.size()));
			sb.append("("); //$NON-NLS-1$
			for(Patrol p : tmp){
				sb.append(p.getId());
				sb.append(" "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")"); //$NON-NLS-1$
		}
		return sb.toString();
	}
	
	
}
