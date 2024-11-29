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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.json.CtJsonObservationParser;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.json.JsonTrackUtils;
import org.wcs.smart.cybertracker.json.SmartMobileProcessingError;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.patrol.CleanPatrolEngine;
import org.wcs.smart.cybertracker.patrol.CleanPatrolSettings;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolWpLink;
import org.wcs.smart.cybertracker.patrol.model.IPatrolCyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.patrol.model.JsonPatrol;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
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
import org.wcs.smart.util.UuidUtils;

/**
 * Parser for parsing patrol data from CT JSON data. 
 * 
 * @author Emily
 *
 */
public abstract class PatrolJsonProcessor implements IJsonProcessor {
	
	public static final Object CA_ERROR = new Object();
	
	private static final IWaypointSource PATROL_WP_SRC = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class)
			.getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
	
	protected List<JsonImportWarning> warnings;
	private List<Path> tempFiles ;
	
	protected Set<Patrol> modifiedPatrols;
	protected Set<Patrol> newPatrols = new HashSet<>();
	protected HashMap<UUID, CtPatrolLink> newPatrolLinks;
	
	protected ConservationArea ca;
	protected Locale locale;
	
	
	protected boolean duplicateWarningGenerated = false;
	
	public enum StatusMessage{
		ADDED, MODIFIED;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IPatrolCyberTrackerLabelProvider.class)
					.getLabel(this, l);
		}
	}
	
	public PatrolJsonProcessor(ConservationArea ca) {
		this.ca = ca;
		warnings = new ArrayList<>();
		tempFiles = new ArrayList<>();
	}

	@Override
	public void cleanUp() {
		cleanUpFiles(tempFiles);
	}
	
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session, Locale locale,
			IProgressMonitor monitor) throws Exception{
		
		SubMonitor smonitor = SubMonitor.convert(monitor, features.size()*2);
				
		modifiedPatrols = new HashSet<Patrol>();
		newPatrolLinks = new HashMap<UUID, CtPatrolLink>();
		this.locale = locale;
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();
		
		int observationFeatureCount = 0;
		for (JSONObject feature : features){
			smonitor.worked(1);
			
			if (!CtJsonUtil.isTrackPoint(feature)) observationFeatureCount++;
			
			CtJsonObservationParser parser = new CtJsonObservationParser(locale);
			try{
					JSONObject properties = (JSONObject) feature.get(CtJsonObservationParser.PROPERTIES_KEY);
					if (properties == null) continue;
					JSONObject sighting = (JSONObject)properties.get(CtJsonObservationParser.SIGHTINGS_KEY);
					if (sighting == null) continue;
					
					String type = (String) sighting.get(CtJsonUtil.JsonKey.DATATYPE.key);
					String deviceId = (String) properties.get(CtJsonObservationParser.DEVICE_ID);
					
					// Validate data type
					if (!PatrolScreenOptionMeta.PATROL_RESOURCE_ID.equalsIgnoreCase(type)){
						//not a valid patrol point; skip it
						continue;
					}
					
					Integer observationCounter = parser.parseObservationCounter(sighting);
					if (observationCounter == null) continue;
									
					//read cybertracker patrol id and convert to uuid
					String ctPatrolId = (String) sighting.get(CtJsonUtil.JsonKey.ID.key);
					UUID ctPatrolUuid = UuidUtils.stringToUuid(ctPatrolId);
					
					//check the database for link; if not found check local links
					CtPatrolLink link = (CtPatrolLink) session.get(CtPatrolLink.class, ctPatrolUuid);
					if (link == null){
						link = newPatrolLinks.get(ctPatrolUuid);
					}else if (!link.getPatrolLeg().getPatrol().getConservationArea().equals(this.ca)) {
						//error data in file is being loaded into a patrol in a different ca
						//this is an error
						String message = SmartContext.INSTANCE.getClass(IPatrolCyberTrackerLabelProvider.class).getLabel(CA_ERROR, locale);
						throw new SmartMobileProcessingError(MessageFormat.format(message, this.ca.getNameLabel(), link.getPatrolLeg().getPatrol().getConservationArea().getNameLabel()));
					}
					
					
					//if this is a startpatrol observation
					//then we want to cleanup and end any patrols
					//associated with the deviceID 
				
					String value2 = (String) sighting.get(CtJsonUtil.JsonKey.OBSERVATION.key);
					boolean isPatrolStart = false;
					if (value2.trim().equalsIgnoreCase(CtJsonObservationParser.OBSERVATION_TYPE_START_PATROL_KEY)) {
						isPatrolStart = true;
					}
					
					if (isPatrolStart) {
						
						if (link != null) {
							//this just shouldn't happen - two start patrols with the same patroID
						}else {
							//find any open patrols associated with this deviceId and close them.				
							
							List<CtPatrolLink> toCleanUp = session.createQuery("SELECT lk FROM CtPatrolLink lk join lk.patrolLeg g join g.patrol p WHERE lk.deviceId = :deviceId and lk.lastObservationCnt != -1 and p.conservationArea = :ca", CtPatrolLink.class) //$NON-NLS-1$
							.setParameter("deviceId", deviceId) //$NON-NLS-1$
							.setParameter("ca", ca) //$NON-NLS-1$
							.list();
							
							if (!toCleanUp.isEmpty()) {
								CleanPatrolSettings settings = CleanPatrolEngine.getOrCreateSettings(session, ca);
								CleanPatrolEngine engine = new CleanPatrolEngine(settings);
								for (CtPatrolLink ptoclean : toCleanUp) {
									boolean isModified = engine.cleanUpAndEndPatrol(ptoclean.getPatrolLeg().getPatrol(), session);
									if (isModified) modifiedPatrols.add(ptoclean.getPatrolLeg().getPatrol());
								}
							}
						}
						//create a new link for this patrol
						link = null;
					}				
					
					//ensure valid observation id
					if (link == null){
						//no patrol created yet, observation counter must be 1
						if (observationCounter != 1) continue;
					}else{
						
								
						//must be the next observation
						if (link.getLastObservationCnt()  + 1 != observationCounter) {
		
							//in an effort to improve error reporting lets make an assumption here 
							if (link.getLastObservationCnt() + 1 > observationCounter) {
								if (!duplicateWarningGenerated) {
									//only report once per file
									warnings.add(new PatrolJsonImportWarning(PatrolJsonImportWarning.WarningType.DUPLICATE,
											link.getPatrolLeg().getPatrol().getId(), link.getLastObservationCnt()+1, observationCounter));
											
									duplicateWarningGenerated = true;
								}
							}else if (link.getLastObservationCnt() +1 < observationCounter) {
								//likely we haven't yet processed previous files should probably be requeued
							}
							
							continue;					
						}
						
					}
					
					//is the end of this leg and needs a new leg
					//is this the end of the patrol
					boolean changeLeg = false;
					boolean isPatrolEnd = sighting.containsKey(PatrolJsonUtils.END_PATROL_KEY) ;
					if (sighting.containsKey(CtJsonUtil.JsonKey.OBSERVATION.key)) {
						String value = (String) sighting.get(CtJsonUtil.JsonKey.OBSERVATION.key);
						if (value.trim().equalsIgnoreCase(CtJsonObservationParser.OBSERVATION_TYPE_END_PATROL_KEY)) {
							isPatrolEnd = true;
						}
						
						if (value.trim().equalsIgnoreCase(CtJsonObservationParser.OBSERVATION_TYPE_CHANGE_PATROL_KEY)) {
							changeLeg = true;
						}
					}
					
					if (changeLeg) {
						//end the current leg and start a new one
						LocalDateTime dt = CtJsonUtil.parseJsonDateTime((String)properties.get(CtJsonObservationParser.DATETIME_KEY));
						
						if (link != null) {					
							//update the leg end time
							PatrolLegDay pd = findLegDay(link.getPatrolLeg(), dt.toLocalDate(), true, LocalTime.MIN, session);
							//PatrolLegDay pd = findLegDay(link.getPatrolLeg(), dt, true, null, session);
							pd.setEndTime(dt.toLocalTime());
							
							//start a new leg
							String defaultValues = (String)sighting.get(CtJsonUtil.JsonKey.DEFAULT_METADATA_VALUES.key);
							JsonPatrol ct = PatrolJsonUtils.parsePatrolMetadata((JSONObject) (new JSONParser()).parse(defaultValues), sighting, ca, session, this.locale);
							warnings.addAll(ct.getWarnings());
							
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
								session.persist(oldLink);
								
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
						LocalDateTime dt = CtJsonUtil.parseJsonDateTime((String)properties.get(CtJsonObservationParser.DATETIME_KEY));
						if (link == null){
							//create a new patrol object
							link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
							
							//update patrol end date/time
							Patrol p = link.getPatrolLeg().getPatrol();
							p.setEndDate(dt.toLocalDate());
							link.getPatrolLeg().setEndDate(p.getEndDate());
							p.createLegDays(session);
						}
						
						//update patrol end date and end time for last patrol leg day
						link.getPatrolLeg().getPatrol().setEndDate(dt.toLocalDate());
						link.getPatrolLeg().setEndDate(dt.toLocalDate());
						PatrolLegDay pd = findLegDay(link.getPatrolLeg(), link.getPatrolLeg().getEndDate(), true, LocalTime.MIN, session);
						pd.setEndTime(dt.toLocalTime());
						
						//add point to track
						addPointToTrack(pd, parser.readXYFromProperties(feature),dt);
						
						//update last observation count
						//link.setLastObservationCnt(observationCounter);
						//using -1 to flag end of patrol
						//this will prevent the cleanup software from trying to end this patrol again
						link.setLastObservationCnt(-1);
						processedFeatures.add(feature);
						if (link.getPatrolLeg().getPatrol().getUuid() != null) modifiedPatrols.add(link.getPatrolLeg().getPatrol());
						continue;
					}
					
					//Parse the waypoint information 				
					Waypoint wp = parser.createWaypoint(feature, ca, session);
					warnings.addAll(parser.getWarnings());
					wp.setId(String.valueOf(observationCounter));
					wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
					wp.setConservationArea(this.ca);
	
					parser.processImages(wp, session);
					
					boolean noObservation = false;
					//patrol paused; no observation; record only as track point
					//same is true for NewPatrol or ChangePatrol observation type
					boolean isPaused = false;
					boolean isResumed = false;
					if (sighting.containsKey(CtJsonUtil.JsonKey.PAUSED.key)) {
						isPaused = true;
					}else if (sighting.containsKey(CtJsonUtil.JsonKey.OBSERVATION.key)) {
						if (((String) sighting.get(CtJsonUtil.JsonKey.OBSERVATION.key)).trim().equalsIgnoreCase(CtJsonObservationParser.OBSERVATION_TYPE_PAUSE_PATROL_KEY)) {
							isPaused = true;
						}
						if (((String) sighting.get(CtJsonUtil.JsonKey.OBSERVATION.key)).trim().equalsIgnoreCase(CtJsonObservationParser.OBSERVATION_TYPE_RESUME_PATROL_KEY)) {
							isResumed = true;
						}
					}
					if (isResumed) {
						//compute rest times
						if (link != null) {
							LocalDateTime dt = CtJsonUtil.parseJsonDateTime((String)properties.get(CtJsonObservationParser.DATETIME_KEY));
							PatrolLegDay currentDay = findLegDay(link.getPatrolLeg(), dt.toLocalDate(), true, LocalTime.MIN, session);
							//the pause event is recorded as a track point; not a waypoint
							//so find the previous track point
							List<PatrolLegDay> sorts = new ArrayList<PatrolLegDay>(currentDay.getPatrolLeg().getPatrolLegDays());
							sorts.sort((a,b)->-1*a.getDate().compareTo(b.getDate()));
							
							LocalDateTime pausepoint = null;
							List<Double> lastValues = new ArrayList<>();
	
							for (PatrolLegDay d : sorts) {
								if (d.getTrack() != null && d.getTrack().getLineStrings() != null) {
									for (LineString ll : d.getTrack().getLineStrings()) {
										lastValues.add(ll.getCoordinateN(ll.getNumPoints() - 1).getZ());
									}
								}
							}
							lastValues.sort((a,b)->-1*a.compareTo(b));
							pausepoint = SharedUtils.toLocalDateTime( lastValues.get(0).longValue());
							
							
							LocalDateTime end = dt;
							LocalDateTime start = pausepoint;
							LocalDateTime c = start;
							LocalDate lde = LocalDate.from(end);
							while(c.isBefore(end)) {
								LocalDate ld = LocalDate.from(c);
								if (ld.isEqual(lde)) {
									PatrolLegDay cd = findLegDay(link.getPatrolLeg(), ld, true, null, session);
									long resttime = Math.round( ChronoUnit.SECONDS.between(c,end) / 60.0 );
									resttime += cd.getRestMinutes() == null ? 0 : cd.getRestMinutes();
									cd.setRestMinutes((int)resttime);
									//time from ld to end 
									break;
								}else {
									PatrolLegDay cd = findLegDay(link.getPatrolLeg(), ld, true, null, session);
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
						if (sighting.containsKey(CtJsonUtil.JsonKey.OBSERVATION.key)) {
							String value = (String) sighting.get(CtJsonUtil.JsonKey.OBSERVATION.key);
							if (value.trim().equalsIgnoreCase(CtJsonObservationParser.OBSERVATION_TYPE_START_PATROL_KEY)) {
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
						if (link.getPatrolLeg().getPatrol().getUuid() != null) modifiedPatrols.add(link.getPatrolLeg().getPatrol());
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
					if (properties.containsKey(CtJsonObservationParser.ROOT_ID_KEY)) ctRootId = UuidUtils.stringToUuid((String)properties.get(CtJsonObservationParser.ROOT_ID_KEY));
					if (sighting.containsKey(CtJsonUtil.JsonKey.OBSERVATION_GROUP.key)) ctObsGroup = UuidUtils.stringToUuid((String)sighting.get(CtJsonUtil.JsonKey.OBSERVATION_GROUP.key));
					
					if (ctRootId == null || ctObsGroup == null) {
						//this is the old way of processing 
						if (!sighting.containsKey(CtJsonUtil.JsonKey.NEW_WAYPOINT.key)){
							//assume this is a group attribute
							
							if (link == null){
								//create a new patrol
								link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
							}
		
							LocalDateTime groupStartTime = link.getGroupStartTime();
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
						Object v = sighting.get(CtJsonUtil.JsonKey.NEW_WAYPOINT.key);
						Boolean isNew = CtJsonUtil.convertToBoolean(v);
						if (isNew == null) {
							addToLast = false;
						}else {
							addToLast = (isNew == false);
						}
						
						if (addToLast){
							if (link == null){
								//we have nothing to add this to; this is an error
								warnings.add(new PatrolJsonImportWarning(PatrolJsonImportWarning.WarningType.PATROL_NOT_FOUND)); 
								continue;
							}
							
							if (addWaypointToLastObservation(link.getPatrolLeg(), wp, session) == null) continue;
							link.setLastObservationCnt(observationCounter);
							processedFeatures.add(feature);
							if (link.getPatrolLeg().getPatrol().getUuid() != null)  modifiedPatrols.add(link.getPatrolLeg().getPatrol());
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
							for (CtPatrolWpLink l : link.getWaypointLinks()) {
								if (l.getCtRootId().equals(ctRootId)) {
									mwp = l.getWaypoint();
									if (l.getCtGroupId().equals(ctObsGroup)) {
										mwpg = l.getObservationGroup();
									}
								}
							}
	
							if (mwp != null && mwp.getUuid() != null) {
								mwp = session.get(Waypoint.class, mwp.getUuid());
								if (mwpg != null) {
									for (WaypointObservationGroup g : mwp.getObservationGroups()) {
										if (mwpg == g || g.equals(mwpg)) {
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
									
									wa.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
											.resolve(PATROL_WP_SRC.getDatastoreFileLocation(link.getPatrolLeg().getPatrol(), session))
											.resolve(wa.getFilename()));
								
								}
								for(WaypointObservation wo : wp.getAllObservations()){
									if (wo.getAttachments() != null){
										for (ObservationAttachment a : wo.getAttachments()){
											a.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
													.resolve(PATROL_WP_SRC.getDatastoreFileLocation(link.getPatrolLeg().getPatrol(), session))
													.resolve(a.getFilename()));
										}
									}
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
								
									wa.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
											.resolve(PATROL_WP_SRC.getDatastoreFileLocation(link.getPatrolLeg().getPatrol(), session))
											.resolve(wa.getFilename()));
								}
								for(WaypointObservation wo : wp.getAllObservations()){
									if (wo.getAttachments() != null){
										for (ObservationAttachment a : wo.getAttachments()){
											a.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
													.resolve(PATROL_WP_SRC.getDatastoreFileLocation(link.getPatrolLeg().getPatrol(), session))
													.resolve(a.getFilename()));
										}
									}
								}
							}
							
							if (link.getPatrolLeg().getPatrol().getUuid() != null) session.persist(newGroup);
							
							//update patrol links
							CtPatrolWpLink wplink = new CtPatrolWpLink();
							wplink.setLink(link);
							wplink.setCtGroupId(ctObsGroup);
							wplink.setCtRootId(ctRootId);
							wplink.setWaypoint(mwp);
							wplink.setObservationGroup(newGroup);
							link.getWaypointLinks().add(wplink);
							
						}else {
							//We want to create a new waypoint and add it to the patrol
							if (link == null){
								link = createPatrolFromSighing(sighting, deviceId, ctPatrolUuid, observationCounter, session);
							}
							//we want this newly created waypoint
							addToExistingLeg(link.getPatrolLeg(), wp, session);
							
							if (link.getPatrolLeg().getPatrol().getUuid() != null) {
								if (wp.getUuid() == null) session.persist(wp);
							}
							
							//update patrol links
							CtPatrolWpLink wplink = new CtPatrolWpLink();
							wplink.setLink(link);
							wplink.setCtGroupId(ctObsGroup);
							wplink.setCtRootId(ctRootId);
							wplink.setWaypoint(wp);
							if (wp.getObservationGroups() != null && !wp.getObservationGroups().isEmpty()) {
								wplink.setObservationGroup(wp.getObservationGroups().get(0));
							}else {
								wplink.setObservationGroup(null);
							}
							link.getWaypointLinks().add(wplink);
						}
					}
	
					if (link.getPatrolLeg().getPatrol().getUuid() != null) modifiedPatrols.add(link.getPatrolLeg().getPatrol());
					
					//add position to track log
					addPointToTrack(link.getPatrolLeg(), new Coordinate(wp.getRawX(), wp.getRawY()), wp.getDateTime(), session);
					
					//update last observation count
					link.setLastObservationCnt(observationCounter);
					processedFeatures.add(feature);
					
					session.flush();
				}catch (SmartMobileProcessingError ex) {
					throw ex;
				}catch (Exception ex){
					//if there is a session.flush error we have a problem we need to stop and rollback
					logException(ex.getMessage() + ": " + feature.toJSONString(), ex); //$NON-NLS-1$
					warnings.add(new JsonImportWarning(JsonImportWarning.Type.JSON_FEATURE_PARSE_ERROR, ex.getMessage()) );
					//add this as a processed feature so we don't continue to re-try
					//if there is an error something needs to be done to resolve it before
					//continue processing
			}finally {
				tempFiles.addAll(parser.getTemporaryFiles());	
			}
		}

		//display warnings to user; this may throw a cancelled exception if the user doesn't want to proceed
		processPatrolWarnings(warnings);
		
		if (observationFeatureCount > 0 && processedFeatures.size() == 0){
			//there is at least one observation feature; but nothing could be processed
			//then we don't want to process track features because it is likely
			//these features should not be processed.
			//see ticket: #1877
			return processedFeatures;
		}
				
		if (!newPatrolLinks.isEmpty()){
			//create new patrols or modify existing patrols
			assignPatrols(session);			
		}
		
		
		//try processing track features
		PatrolJsonTrackProcessor trackProcessor = new PatrolJsonTrackProcessor(this.ca);
		processedFeatures.addAll(trackProcessor.processJson(features, session, locale, smonitor.split(features.size())));
		modifiedPatrols.addAll(trackProcessor.getModifiedPatrols());
		processTrackWarnings(trackProcessor.getWarnings());
		
		return processedFeatures;
	}
	

	@Override
	public List<JsonImportWarning> getWarnings(){
		return this.warnings;
	}
	
	protected abstract void logException(String message, Exception ex);
	
	/**
	 * Should throw exception if processing should stop 
	 * 
	 * @throws UserCancelledException
	 */
	protected void processPatrolWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		
	}
	
	/**
	 * Should throw exception if processing should stop.  
	 * @throws UserCancelledException
	 */
	protected void processTrackWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		
	}
	
	
	/**
	 * Should throw exception if processing should stop 
	 * 
	 * @throws UserCancelledException
	 */
	protected abstract void assignPatrols(Session session) throws UserCancelledException;

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
	private int processGroup(JSONObject sighting, PatrolLeg legToUpdate, Waypoint wp, 
			List<WaypointObservationAttribute> applyAll, LocalDateTime groupStartTime, Session session) throws Exception{
		if (!sighting.containsKey(CtJsonUtil.JsonKey.END_WAYPOINT_GROUP.key)){
			//clear observations associated with 
			wp.getObservationGroups().clear();
			addToExistingLeg(legToUpdate, wp, session);
			
			return 1;
		}else{
			if ("FALSE".equalsIgnoreCase((String)sighting.get(CtJsonUtil.JsonKey.END_WAYPOINT_GROUP.key))){ //$NON-NLS-1$
				if (wp.getRawX() == null || wp.getRawY() == null){
					//no location; add to previous 
					if (addWaypointToLastObservation(legToUpdate, wp, session) != null) return 1;
					return 0;
				}else{
					addToExistingLeg(legToUpdate, wp, session);
					return 2;
				}
			}else if ("TRUE".equalsIgnoreCase((String)sighting.get(CtJsonUtil.JsonKey.END_WAYPOINT_GROUP.key))){ //$NON-NLS-1$
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
										pw.getWaypoint().getDateTime().isAfter(groupStartTime)){
									
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
				if (pld.getWaypoints() == null) pld.setWaypoints(new ArrayList<>());
				for (PatrolWaypoint pw: pld.getWaypoints()){
					if (lastWaypoint == null ||
							pw.getWaypoint().getDateTime().isAfter(lastWaypoint.getWaypoint().getDateTime())){
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
			//session.saveOrUpdate(lastWaypoint);
			session.flush();
		}
		
		if (legToUpdate.getPatrol().getUuid() != null) modifiedPatrols.add(legToUpdate.getPatrol());
		
		return lastWaypoint;
	}

	
	private CtPatrolLink createPatrolFromSighing(JSONObject sighting, String deviceId, UUID ctUuid, int observationCounter, Session session) throws Exception{
		Patrol p = new Patrol();
		p.setConservationArea(ca);
		String defaultValues = (String)sighting.get(CtJsonUtil.JsonKey.DEFAULT_METADATA_VALUES.key);
		JsonPatrol ct = PatrolJsonUtils.parsePatrolMetadata((JSONObject) (new JSONParser()).parse(defaultValues), sighting, ca, session, this.locale);
		warnings.addAll(ct.getWarnings());
		
		String startDate = (String)sighting.get(MetadataFieldValue.START_DATE_METADATA_KEY);		
		LocalDate dStartDate = LocalDate.parse(startDate, PatrolJsonUtils.DATEFORMAT);
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
		
		String startTime = (String)sighting.get(MetadataFieldValue.START_TIME_METADATA_KEY);
		LocalDateTime startDateTime = dStartDate.atTime(LocalTime.parse(startTime, PatrolJsonUtils.TIMEFORMAT));
		
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
		
	private PatrolLeg addLegFromSighting(Patrol patrol, JsonPatrol ct,			
			JSONObject sighting,
			String deviceId, UUID ctUuid, int observationCounter,
			LocalDateTime startDateTime,
			Session session) throws Exception{

		PatrolLeg pl = patrol.addLeg();
		pl.setMandate(ct.getMandate());
		pl.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		pl.setStartDate(startDateTime.toLocalDate());
		pl.setEndDate(pl.getStartDate());
		for (Employee member: ct.getMembers()){
			PatrolLegMember item = pl.addPatrolLegMember(member);
			if (ct.getPatrolTransportType() != null && ct.getPatrolTransportType().getRequiresPilot()){
				if (member.equals(ct.getPilot())) item.setIsPilot(true);
			}
			if (member.equals(ct.getLeader())) item.setIsLeader(true);
		}
	
		pl.setType(ct.getPatrolTransportType());
		
		if (patrol.getPatrolType() == null) {
			patrol.setPatrolType(pl.getType().getPatrolType());
		}else {
			if (!pl.getType().getPatrolType().equals(patrol.getPatrolType())) {
				throw new Exception("Cannot merge patrols of different track types."); //$NON-NLS-1$
			}
		}
		
		//make a single patrol leg day for the start date and time
		PatrolLegDay one = new PatrolLegDay();
		one.setStartTime(startDateTime.toLocalTime());
		one.setEndTime(SharedUtils.END_OF_DAY);
		pl.getPatrolLegDays().add(one);
		
		pl.createLegDays(session);	
		
		return pl;
	}
	
	private void addToExistingLeg(PatrolLeg addTo, Waypoint wp, Session session)
			throws Exception {
		
//		PatrolLegDay addToD = findLegDay(addTo, wp.getDateTime(), true, null, session);
		PatrolLegDay addToD = findLegDay(addTo, wp.getDateTime().toLocalDate(), true, LocalTime.MIN, session);
		PatrolWaypoint pw = addWaypointToLegDay(addToD, wp);
		
		if (addTo.getUuid() != null){
			if (wp.getAttachments() != null){
				for (WaypointAttachment wa : wp.getAttachments()){
					//the associated patrol waypoint has not been saved yet
					//so we need to fix up all attachment
					wa.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
							.resolve(PATROL_WP_SRC.getDatastoreFileLocation(addTo.getPatrol(), session))
							.resolve(wa.getFilename()));
				}
			}
			
			for(WaypointObservation wo : wp.getAllObservations()){
				if (wo.getAttachments() != null){
					for (ObservationAttachment a : wo.getAttachments()){
						a.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
								.resolve(PATROL_WP_SRC.getDatastoreFileLocation(addTo.getPatrol(), session))
								.resolve(a.getFilename()));
					}
				}
			}
			
			if (wp.getUuid() == null) session.persist(wp);
			if (addToD.getPatrolLeg().getPatrol().getUuid() == null) session.persist(addToD.getPatrolLeg().getPatrol());
			if (addToD.getUuid() == null) session.persist(addToD);
			session.persist(pw);
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

	
	private PatrolLegDay findLegDay(PatrolLeg leg, LocalDate day, boolean create, LocalTime startTime, Session session){
		for (PatrolLegDay pld : leg.getPatrolLegDays()){
			if (pld.getDate().isEqual(day)){
				return pld;
			}
		}
		if (!create) return null;
		
		PatrolLegDay start = null;
		for (PatrolLegDay pld : leg.getPatrolLegDays()) {
			if (start == null || pld.getDate().isBefore(start.getDate())) {
				start = pld;
			}
		}
		//configure the start time here so legs get created correctly
//		leg.setStartDate(SmartUtils.combineDateTime(leg.getStartDate(), start.getStartTime()));
		
		PatrolLegDay pld = new PatrolLegDay();
		pld.setDate(day);
		pld.setRestMinutes(0);
		pld.setWaypoints(new ArrayList<PatrolWaypoint>());
		leg.getPatrolLegDays().add(pld);
		pld.setPatrolLeg(leg);
		
		
		//update leg and patrol dates as necessary
		if (leg.getStartDate().isAfter(pld.getDate())){
			leg.setStartDate(pld.getDate());
		}
		if (leg.getEndDate().isBefore(pld.getDate())){
			leg.setEndDate(pld.getDate());
		}
		if (leg.getPatrol().getStartDate().isAfter(pld.getDate())){
			leg.getPatrol().setStartDate(pld.getDate());
		}
		if (leg.getPatrol().getEndDate().isBefore(pld.getDate())){
			leg.getPatrol().setEndDate(pld.getDate());
		}
		
		//make sure there is at least one legday for each day between the start and end date		
		leg.createLegDays(session);
		
		if (startTime == null){
			pld.setStartTime(LocalTime.now());
		}else{
			pld.setStartTime(startTime);
		}
		pld.setEndTime(SharedUtils.END_OF_DAY);
		
		return pld;
	}
	
	private void addPointToTrack(PatrolLegDay pld, Coordinate pnt, LocalDateTime time) throws Exception{
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
	
	private void addPointToTrack(PatrolLeg leg, Coordinate pnt, LocalDateTime time, Session session) throws Exception{
		if (pnt == null) return;
		PatrolLegDay pld = findLegDay(leg, time.toLocalDate(), true, LocalTime.MIN, session);
		addPointToTrack(pld, pnt, time);
	}

	@Override
	public String getStatusMessage(Locale l) {
		if (newPatrols.isEmpty() && modifiedPatrols.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		if (!newPatrols.isEmpty()){
			sb.append(MessageFormat.format(StatusMessage.ADDED.getMessage(l), newPatrols.size()));
			sb.append(" ("); //$NON-NLS-1$
			for(Patrol p : newPatrols){
				sb.append(p.getId());
				sb.append(" "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")"); //$NON-NLS-1$
		}
		HashSet<Patrol> tmp = new HashSet<Patrol>();
		for (Patrol p : modifiedPatrols){
			if (p.getUuid() == null || newPatrols.contains(p)) continue;
			tmp.add(p);
		}
		
		if (tmp.size() > 0){
			sb.append(MessageFormat.format(StatusMessage.MODIFIED.getMessage(l), tmp.size()));
			sb.append(" ("); //$NON-NLS-1$
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
