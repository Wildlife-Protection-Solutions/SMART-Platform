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
package org.wcs.smart.cybertracker.survey.json;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.cybertracker.json.CtJsonObservationParser;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.json.SmartMobileProcessingError;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.survey.model.CtMissionLink;
import org.wcs.smart.cybertracker.survey.model.CtMissionWpLink;
import org.wcs.smart.cybertracker.survey.model.ISurveyCyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.survey.model.SurveyMetadata;
import org.wcs.smart.er.MissionIdGenerator;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Parser for parsing patrol data from CT JSON data. 
 * 
 * @author Emily
 *
 */
public abstract class MissionJsonProcessor implements IJsonProcessor {
	
	//if CA of mission link doesn't match the CA being processed
	public static final Object CA_ERROR = new Object();
	
	private DateTimeFormatter DATEFORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd"); //$NON-NLS-1$
	private DateTimeFormatter TIMEFORMAT = DateTimeFormatter.ofPattern("HH:mm:ss"); //$NON-NLS-1$
	
	private static final IWaypointSource SURVEY_WP_SRC = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class)
			.getSource(SurveyWaypointSource.KEY);
	
	private List<JsonImportWarning> warnings;
	private List<Path> tempFiles;
	
	protected Set<Mission> modifiedMissions;
	protected Set<Mission> newMissions = new HashSet<>();
	protected HashMap<UUID, CtMissionLink> newMissionLinks;
	
	protected ConservationArea ca;
	protected Locale locale;
	
	public enum StatusMessage{
		ADDED, MODIFIED;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(ISurveyCyberTrackerLabelProvider.class).getLabel(this, l);
		}
	}
	
	public MissionJsonProcessor(ConservationArea ca) {
		warnings = new ArrayList<>();
		tempFiles = new ArrayList<>();
		
		this.ca = ca;
	}

	protected abstract void logException(String message, Exception ex);
	
	/**
	 * Should throw exception if processing should stop 
	 * 
	 * @throws UserCancelledException
	 */
	protected void processMissionWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		
	};
	
	/**
	 * Should throw exception if processing should stop.  
	 * @throws UserCancelledException
	 */
	protected void processTrackWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		
	};
	
	@Override
	public List<JsonImportWarning> getWarnings(){
		return this.warnings;
	}
	
	@Override
	public void cleanUp() {
		cleanUpFiles(tempFiles);
	}
	
	/**
	 * Should throw exception if processing should stop 
	 * 
	 * @throws UserCancelledException
	 */
	protected abstract void assignMissions(Session session) throws UserCancelledException;
	
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session, Locale locale, IProgressMonitor monitor) throws Exception{
		this.locale = locale;
		modifiedMissions = new HashSet<Mission>();
		newMissionLinks = new HashMap<UUID, CtMissionLink>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();;
		
		int observationFeatureCount = 0;
		SubMonitor smonitor = SubMonitor.convert(monitor, features.size()*2);

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
				if (!SurveyMetadata.DATATYPE_SURVEY.equalsIgnoreCase(type)){
					//not a valid patrol point; skip it
					continue;
				}
				
				Integer observationCounter = parser.parseObservationCounter(sighting);
				if (observationCounter == null) continue;
				
				//read cybertracker patrol id and convert to uuid
				String ctMissionId = (String) sighting.get(CtJsonUtil.JsonKey.ID.key);
				UUID ctMissionUuid = UuidUtils.stringToUuid(ctMissionId);
				
				//check the database for link; if not found check local links
				CtMissionLink link = (CtMissionLink) session.get(CtMissionLink.class, ctMissionUuid);
				if (link == null){
					link = newMissionLinks.get(ctMissionUuid);
				}else if (!link.getMission().getSurvey().getSurveyDesign().getConservationArea().equals(this.ca)) {
					//error data in file is being loaded into a patrol in a different ca
					//this is an error
					String message = SmartContext.INSTANCE.getClass(ISurveyCyberTrackerLabelProvider.class).getLabel(CA_ERROR, locale);
					throw new SmartMobileProcessingError(MessageFormat.format(message, this.ca.getNameLabel(), 
							link.getMission().getSurvey().getSurveyDesign().getConservationArea().getNameLabel()));
				}
				
				//ensure valid observation id
				if (link == null){
					//no patrol created yet, observation counter must be 1
					if (observationCounter != 1) continue;
				}else{
					//must be the next observation
					if (link.getLastObservationCnt()  + 1 != observationCounter) continue;					
				}
				
				if (sighting.containsKey(SurveyMetadata.JsonKey.MISSION_SAMPLING_UNIT.key) &&
						link != null){
					//sampling unit changed
					//we have a new sampling unit
					SamplingUnit su = null;
					String suKey = (String) sighting.get(SurveyMetadata.JsonKey.MISSION_SAMPLING_UNIT.key);
					if (suKey != null && suKey.startsWith(SurveyMetadata.JsonSurveyKey.SAMPLING_UNIT.key + CtJsonUtil.KEY_SEP)){
						suKey = suKey.substring(SurveyMetadata.JsonSurveyKey.SAMPLING_UNIT.key.length() + 1);
						if (!suKey.equals(CtJsonUtil.NULL_KEY)){
							su = (SamplingUnit) session.get(SamplingUnit.class, UuidUtils.stringToUuid(suKey));
							if (su == null){
								warnings.add(new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.SU_NOT_FOUND, suKey));
							}
						}
					}
					if ((su != null && !su.equals(link.getSamplingUnit())) ||
							(su == null && link.getSamplingUnit() != null)) {
						//sampling unit changed
						link.setSamplingUnit(su);
						
						//add this point to track; this is not an observation
						LocalDateTime dt = CtJsonUtil.parseJsonDateTime((String)properties.get(CtJsonObservationParser.DATETIME_KEY));
						addPointToTrack(link.getMission(), su, parser.readXYFromProperties(feature), dt, session);
						
						//update last observation count
						link.setLastObservationCnt(observationCounter);
						processedFeatures.add(feature);
											
						continue;
					}
				}
				
				//is this the end of the mission
				boolean isMissionEnd = sighting.containsKey(SurveyMetadata.JsonKey.END_MISSION_KEY.key) ;
				//new CT check
				
				if (sighting.containsKey(CtJsonUtil.JsonKey.OBSERVATION.key)) {
					String value = (String) sighting.get(CtJsonUtil.JsonKey.OBSERVATION.key);
					if (value.trim().equalsIgnoreCase(CtJsonObservationParser.OBSERVATION_TYPE_END_PATROL_KEY)) {
						isMissionEnd = true;
					}
				}
				
				if (isMissionEnd){
					//we want to find the patrol and update the end date
					//add the position to the track, but do not create an observation 
					//for this patrol
					LocalDateTime dt = CtJsonUtil.parseJsonDateTime((String)properties.get(CtJsonObservationParser.DATETIME_KEY));
					
					if (link == null){
						//create a new patrol object
						link = createMissionFromSighting(sighting, deviceId, ctMissionUuid, observationCounter, session);
						
						//update patrol end date/time
						Mission p = link.getMission();
						link.getMission().setEndDate(p.getEndDate());
					}
					
					//update patrol end date and end time for last patrol leg day
					link.getMission().setEndDate(dt.toLocalDate());
					MissionDay md = findDay(link.getMission(), link.getMission().getEndDate(), true, null, session);
					md.setEndTime(dt.toLocalTime());
						
					//add point to track
					addPointToTrack(md, link.getSamplingUnit(), parser.readXYFromProperties(feature),dt);
					
					//update last observation count
					link.setLastObservationCnt(observationCounter);
					processedFeatures.add(feature);
					continue;
				}
				
				session.flush();
				
				//Parse the waypoint information 				
				Waypoint wp = parser.createWaypoint(feature, ca, session);
				warnings.addAll(parser.getWarnings());
				wp.setId(String.valueOf(observationCounter));
				wp.setSourceId(SurveyWaypointSource.KEY);
				wp.setConservationArea(ca);
				parser.processImages(wp,  session);
				
				
				boolean noObservation = false;
				//patrol paused; no observation; record only as track point
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
						
						MissionDay currentDay = findDay(link.getMission(), dt.toLocalDate(), true, LocalTime.MIN, session);
						//the pause event is recorded as a track point; not a waypoint
						//so find the previous track point
						List<MissionDay> sorts = new ArrayList<MissionDay>(currentDay.getMission().getMissionDays());
						sorts.sort((a,b)->-1*a.getDate().compareTo(b.getDate()));
						LocalDateTime pausepoint = null;
						for (MissionDay d : sorts) {
							if (d.getTracks() == null || d.getTracks().isEmpty()) continue;
							
							List<Double> lastValues = new ArrayList<>();
							for (MissionTrack t : d.getTracks()) {
								lastValues.add(t.getLineString().getCoordinateN(t.getLineString().getNumPoints() - 1).getZ());
							}
							if (lastValues.isEmpty()) continue;
							lastValues.sort((a,b)->-1*a.compareTo(b));
							pausepoint = SharedUtils.toLocalDateTime(lastValues.get(0).longValue());
							break;
						}
						if (pausepoint == null) {
							warnings.add(new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.REST_TIME_ERROR));
							pausepoint = dt;
						}
						
						LocalDateTime end = dt;
						LocalDateTime start = pausepoint;
						LocalDateTime c = start;
						LocalDate lde = LocalDate.from(end);
						while(c.isBefore(end)) {
							LocalDate ld = LocalDate.from(c);
							if (ld.isEqual(lde)) {
								MissionDay cd = findDay(link.getMission(), ld, true, null, session);
								long resttime = Math.round( ChronoUnit.SECONDS.between(c,end) / 60.0 );
								resttime += cd.getRestMinutes() == null ? 0 : cd.getRestMinutes();
								cd.setRestMinutes((int)resttime);
								//time from ld to end 
								break;
							}else {
								MissionDay cd = findDay(link.getMission(), ld, true,null, session);
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
				
				noObservation = isPaused | isResumed;
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
					//patrol paused; no observation; record only as track point
					if (link == null){
						link = createMissionFromSighting(sighting, deviceId, ctMissionUuid, observationCounter, session);
					}
					
					if (wp.getRawX() != null && wp.getRawY() != null){
						addPointToTrack(link.getMission(), link.getSamplingUnit(), new Coordinate(wp.getRawX(), wp.getRawY()), wp.getDateTime(), session);
					}
					//update last observation count
					link.setLastObservationCnt(observationCounter);
					processedFeatures.add(feature);
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
					//the old version
					
					if (!sighting.containsKey(CtJsonUtil.JsonKey.NEW_WAYPOINT.key)){
						//assume this is a group attribute
						
						if (link == null){
							//create a new patrol
							link = createMissionFromSighting(sighting, deviceId, ctMissionUuid, observationCounter, session);
						}
	
						LocalDateTime groupStartTime = link.getGroupStartTime();
						int groupResult = processGroup(sighting, link.getMission(), wp, link.getSamplingUnit(), parser.getApplyToAdd(), groupStartTime, session);
						if (groupResult > 0){
							
							if (groupResult == 2){
								if (link.getGroupStartTime() == null){
									link.setGroupStartTime(wp.getDateTime());
								}
							}else if (groupResult == 3){
								link.setGroupStartTime(null);
							}
							
							if(wp.getRawX() != null && wp.getRawY() != null){
								addPointToTrack(link.getMission(), link.getSamplingUnit(), new Coordinate(wp.getRawX(), wp.getRawY()), wp.getDateTime(), session);
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
							warnings.add(new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.MISSION_NOT_FOUND));
							continue;
						}
						
						if (addWaypointToLastObservation(link.getMission(), wp, session) == null) continue;
						link.setLastObservationCnt(observationCounter);
						processedFeatures.add(feature);
						continue;
					}
					
				
					//We want to create a new waypoint and add it to the patrol
					if (link == null){
						link = createMissionFromSighting(sighting, deviceId, ctMissionUuid, observationCounter, session);
					}
					
					//add these observation to the selected patrol leg
					addToExistingMission(link.getMission(), wp, link.getSamplingUnit(), session);
				}else {
					//new way of processings waypoints/observation groups
					//we want to find the waypoint and/or observation group to add to
					//first see if we can find the waypoint and observation group					
					Waypoint mwp = null;
					WaypointObservationGroup mwpg = null;
					if(link != null && link.getWaypointLinks() != null) {
						for (CtMissionWpLink l : link.getWaypointLinks()) {
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
										.resolve(SURVEY_WP_SRC.getDatastoreFileLocation(link.getMission(), session))
										.resolve(wa.getFilename()));
							
							}
							for(WaypointObservation wo : wp.getAllObservations()){
								if (wo.getAttachments() != null){
									for (ObservationAttachment a : wo.getAttachments()){
										a.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
												.resolve(SURVEY_WP_SRC.getDatastoreFileLocation(link.getMission(), session))
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
										.resolve(SURVEY_WP_SRC.getDatastoreFileLocation(link.getMission(), session))
										.resolve(wa.getFilename()));
							}
							for(WaypointObservation wo : wp.getAllObservations()){
								if (wo.getAttachments() != null){
									for (ObservationAttachment a : wo.getAttachments()){
										a.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
												.resolve(SURVEY_WP_SRC.getDatastoreFileLocation(link.getMission(), session))
												.resolve(a.getFilename()));
									}
								}
							}
						}

						if (link.getMission().getUuid() != null) session.persist(newGroup);

						//update patrol links
						CtMissionWpLink wplink = new CtMissionWpLink();
						wplink.setLink(link);
						wplink.setCtGroupId(ctObsGroup);
						wplink.setCtRootId(ctRootId);
						wplink.setWaypoint(mwp);
						wplink.setObservationGroup(newGroup);
						link.getWaypointLinks().add(wplink);

						
					}else {
						if (link == null){
							link = createMissionFromSighting(sighting, deviceId, ctMissionUuid, observationCounter, session);
						}
						//add these observation to the selected patrol leg
						addToExistingMission(link.getMission(), wp, link.getSamplingUnit(), session);
						
						if (link.getMission().getUuid() != null) {
							if (wp.getUuid() == null) session.persist(wp);
						}
						
						//update patrol links
						CtMissionWpLink wplink = new CtMissionWpLink();
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
				
				
				if (link.getMission().getUuid() != null) modifiedMissions.add(link.getMission());
				
				//add position to track log
				addPointToTrack(link.getMission(), link.getSamplingUnit(), new Coordinate(wp.getRawX(), wp.getRawY()), wp.getDateTime(), session);
				
				//update last observation count
				link.setLastObservationCnt(observationCounter);
				
				processedFeatures.add(feature);
				
			}catch (Exception ex){
				//TODO: if there is a session.flush error we have a problem we need to stop and rollback
				logException(ex.getMessage() + ": " + feature.toJSONString(), ex); //$NON-NLS-1$
				warnings.add(new JsonImportWarning(JsonImportWarning.Type.JSON_FEATURE_PARSE_ERROR, ex.getMessage()));
			}finally {
				tempFiles.addAll(parser.getTemporaryFiles());
			}
		}
		

		//display warnings to user; this may throw a cancelled exception if the user doesn't want to proceed
		processMissionWarnings(warnings);
		
		if (observationFeatureCount > 0 && processedFeatures.size() == 0){
			//there is at least one observation feature; but nothing could be processed
			//then we don't want to process track features because it is likely
			//these features should not be processed.
			//see ticket: #1877
			return processedFeatures;
		}
		
		if (!newMissionLinks.isEmpty()){
			//assign missions to surveys
			assignMissions(session);			
		}
		
		//try processing track features
		MissionJsonTrackProcessor trackProcessor = new MissionJsonTrackProcessor(this.ca);
		processedFeatures.addAll(trackProcessor.processJson(features, session, locale, smonitor.split(features.size())));
		modifiedMissions.addAll(trackProcessor.getModifiedMissions());
		processTrackWarnings(trackProcessor.getWarnings());
		
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
	private int processGroup(JSONObject sighting, Mission missionToUpdate, Waypoint wp, SamplingUnit su, List<WaypointObservationAttribute> applyAll, LocalDateTime groupStartTime, Session session) throws Exception{
		if (!sighting.containsKey(CtJsonUtil.JsonKey.END_WAYPOINT_GROUP.key)){
			//clear observations associated with 
			wp.getObservationGroups().clear();
			addToExistingMission(missionToUpdate, wp, su, session);
			
			return 1;
		}else{
			if ("FALSE".equalsIgnoreCase((String)sighting.get(CtJsonUtil.JsonKey.END_WAYPOINT_GROUP.key))){ //$NON-NLS-1$
				if (wp.getRawX() == null || wp.getRawY() == null){
					//no location; add to previous 
					if (addWaypointToLastObservation(missionToUpdate, wp, session) != null) return 1;
					return 0;
				}else{
					addToExistingMission(missionToUpdate, wp, su, session);
					return 2;
				}
			}else if ("TRUE".equalsIgnoreCase((String)sighting.get(CtJsonUtil.JsonKey.END_WAYPOINT_GROUP.key))){ //$NON-NLS-1$
				if (wp.getRawX() == null || wp.getRawY() == null){
					//no location; add to previous 
					SurveyWaypoint pw = addWaypointToLastObservation(missionToUpdate, wp, session);
					if (pw != null){
						if (!pw.getWaypoint().getObservationGroups().isEmpty()) {
							addAttributesToObservation(pw.getWaypoint().getObservationGroups().get(0).getObservations(), applyAll);
						}
						return 1;
					}
					return 0;
				}else{
					addToExistingMission(missionToUpdate, wp, su, session);
					if (groupStartTime == null) {
						//see bug: https://app.assembla.com/spaces/smart-cs/tickets/2868
						//this case occurs when we have a group with only a single observation
						addAttributesToObservation(wp.getObservationGroups().get(0).getObservations(), applyAll);
					}else {
						//update all waypoints since the start of the group to include the defaults
						//and the after attributes
						for (MissionDay pld : missionToUpdate.getMissionDays()){
							for (SurveyWaypoint pw : pld.getWaypoints()){
								if (pw.getWaypoint().getDateTime().equals(groupStartTime) || 
										pw.getWaypoint().getDateTime().isAfter(groupStartTime)){
									
									if (!pw.getWaypoint().getObservationGroups().isEmpty()) {
										addAttributesToObservation(pw.getWaypoint().getObservationGroups().get(0).getObservations(), applyAll);
									}
	
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
	
	private SurveyWaypoint addWaypointToLastObservation(Mission missionToUpdate, Waypoint wp, Session session){
		//find previous waypoint
		//find the last waypoint by date/time in the legToUpdate
		SurveyWaypoint lastWaypoint = null;
		if (missionToUpdate != null){
			for (MissionDay md : missionToUpdate.getMissionDays()){
				for (SurveyWaypoint pw: md.getWaypoints()){
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
		
		//merge observations into a single waypoint
		Waypoint addTo = lastWaypoint.getWaypoint();
		if (addTo.getObservationGroups().isEmpty()) {
			WaypointObservationGroup first = new WaypointObservationGroup();
			first.setObservations(new ArrayList<>());
			first.setWaypoint(addTo);
			addTo.getObservationGroups().add(first);
		}
		WaypointObservationGroup addToGroup = addTo.getObservationGroups().get(0);
		
		for(WaypointObservationGroup g : wp.getObservationGroups()) {
			for (WaypointObservation wo : g.getObservations()){
				wo.setObservationGroup(addToGroup);
				addToGroup.getObservations().add(wo);
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
		if (missionToUpdate.getUuid() != null){
			modifiedMissions.add(missionToUpdate);
			if (session.get(lastWaypoint.getClass(), lastWaypoint.getId()) == null) session.persist(lastWaypoint);
			session.flush();
		}
		
		return lastWaypoint;
	}
	
	
	private CtMissionLink createMissionFromSighting(JSONObject sighting, String deviceId, UUID ctUuid, int observationCounter, Session session) throws Exception{
		Mission mission = new Mission();
		
		String defaultValues = (String)sighting.get(CtJsonUtil.JsonKey.DEFAULT_METADATA_VALUES.key);
		JsonMission ct = SurveyMetadata.parseMissionMetadata(
				(JSONObject) (new JSONParser()).parse(defaultValues), sighting, ca, session);
		
		if (ct.getSurveyDesign() == null) {
			String msg = (new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.SURVEY_DESIGN_NOTFOUND, ct.getSurveyDesignKey())).getMessage(locale);
			throw new Exception(msg);
		}
		
		String startDate = (String)sighting.get(MetadataFieldValue.START_DATE_METADATA_KEY);
		String startTime = (String)sighting.get(MetadataFieldValue.START_TIME_METADATA_KEY);
		
		LocalDate dStartDate = LocalDate.parse(startDate,DATEFORMAT);
		LocalTime dStartTime = LocalTime.parse(startTime,TIMEFORMAT);
		
		mission.setEndDate(dStartDate);
		mission.setStartDate(dStartDate);
		mission.setComment(ct.getComment());
		
		String missionId = MissionIdGenerator.INSTANCE.generateMissionId(mission, session);
		mission.setId(missionId);
		
		for (Employee member: ct.getMembers()){
			MissionMember item = new MissionMember();
			item.setMember(member);
			item.setMission(mission);
			mission.getMembers().add(item);
			if (member.equals(ct.getLeader())) item.setIsLeader(true);
		}
		
		//parse mission attributes
		mission.setMissionPropertyValues(new ArrayList<MissionPropertyValue>());
		for (Object x : sighting.keySet()){
			String key = (String)x;
			if (key.startsWith(SurveyMetadata.JsonKey.MISSION_PROPERTY_PREFIX.key)){
				String missionPropKey = key.substring(SurveyMetadata.JsonKey.MISSION_PROPERTY_PREFIX.key.length());
				
				List<MissionAttribute> mas = QueryFactory.buildQuery(session, MissionAttribute.class,
						new Object[] {"keyId", missionPropKey}, //$NON-NLS-1$
						new Object[] {"conservationArea", ca}) //$NON-NLS-1$
						.list();
	
				if (mas.size() == 0){
					warnings.add(new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.MISSION_ATTRIBUTE_NOT_FOUND, missionPropKey));
				}else if (mas.size() > 1){
					warnings.add(new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.MULTIPLE_ATTRIBUTES_FOUND, missionPropKey));
				}else{
					MissionAttribute ma = mas.get(0);
					
					Object value = null;
					if (ma.getType() == AttributeType.TEXT){
						value = (String)sighting.get(x);
					}else if (ma.getType() == AttributeType.NUMERIC){
						value = ((Number)sighting.get(x)).doubleValue();
					}else if (ma.getType() == AttributeType.LIST){
						String listItem = (String)sighting.get(x);
						if (listItem.startsWith(SurveyMetadata.JsonSurveyKey.MISSION_ATT_LIST.key + CtJsonUtil.KEY_SEP)){
							String listItemUuid = listItem.substring(SurveyMetadata.JsonSurveyKey.MISSION_ATT_LIST.key.length() + 1);
							value = session.get(MissionAttributeListItem.class, UuidUtils.stringToUuid(listItemUuid));
							if (value == null){
								warnings.add(new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.LIST_ITEM_NOT_FOUND, listItemUuid));
							}
						}
					}
					if (value != null){
						MissionPropertyValue mv = new MissionPropertyValue();
						mv.setMission(mission);
						mv.setMissionAttribute(ma);
						mv.setValue(value);
						mission.getMissionPropertyValues().add(mv);
					}
				}
			}
		}
		
		
		//make a single mission day for the start date and time
		MissionDay md = new MissionDay();
		md.setDate(dStartDate );
		md.setStartTime( dStartTime );
		md.setRestMinutes(0);
		md.setEndTime( SharedUtils.END_OF_DAY );
		md.setMission(mission);
		mission.setMissionDays(new ArrayList<MissionDay>());
		mission.getMissionDays().add(md);
		
		//find the initial sampling unit
		SamplingUnit su = null;
		String suKey = null;
		if (sighting.containsKey(SurveyMetadata.JsonKey.MISSION_START_SAMPLING_UNIT.key)){
			suKey = (String) sighting.get(SurveyMetadata.JsonKey.MISSION_START_SAMPLING_UNIT.key);
		}else if (sighting.containsKey(SurveyMetadata.JsonKey.MISSION_SAMPLING_UNIT.key)) {
			suKey = (String) sighting.get(SurveyMetadata.JsonKey.MISSION_SAMPLING_UNIT.key);
		}
		if (suKey != null && suKey.startsWith(SurveyMetadata.JsonSurveyKey.SAMPLING_UNIT.key + CtJsonUtil.KEY_SEP)){
			suKey = suKey.substring(SurveyMetadata.JsonSurveyKey.SAMPLING_UNIT.key.length() + 1);
			if (!suKey.equals(CtJsonUtil.NULL_KEY)){
				su = (SamplingUnit) session.get(SamplingUnit.class, UuidUtils.stringToUuid(suKey));
				if (su == null){
					warnings.add(new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.SU_NOT_FOUND, suKey));
				}
			}
		}
			
		CtMissionLink link = new CtMissionLink();
		link.setDeviceId(deviceId);
		link.setCtUuid(ctUuid);
		link.setLastObservationCnt(observationCounter);
		link.setMission(mission);
		link.setNewSurveyDesign(ct.getSurveyDesign());
		link.setSamplingUnit(su);
		link.setWaypointLinks(new ArrayList<>());
		
		newMissionLinks.put(ctUuid, link);
		
		return link;
	}
		
	private void addToExistingMission(Mission addTo, Waypoint wp, SamplingUnit su, Session session)
			throws Exception {
		
		MissionDay addToD = findDay(addTo, wp.getDateTime().toLocalDate(), true, null, session);
		SurveyWaypoint pw = addWaypointToMissionDay(addToD, wp, su);
		
		if (addTo.getUuid() != null){
			if (wp.getAttachments() != null){
				for (WaypointAttachment wa : wp.getAttachments()){
					//the associated patrol waypoint has not been saved yet
					//so we need to fix up all attachment
					wa.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
							.resolve(SURVEY_WP_SRC.getDatastoreFileLocation(addTo, session))
							.resolve(wa.getFilename()));
					
				}
			}
			for(WaypointObservation wo : wp.getAllObservations()){
				if (wo.getAttachments() != null){
					for (ObservationAttachment a : wo.getAttachments()){
						a.computeFileLocation(Paths.get(ca.getFileDataStoreLocation())
								.resolve(SURVEY_WP_SRC.getDatastoreFileLocation(addTo, session))
								.resolve(a.getFilename()));
					}
				}
			}
			if (wp.getUuid() == null) session.persist(wp);
			if (addToD.getMission().getUuid() == null) session.persist(addToD.getMission());
			session.persist(pw);
			session.flush();
		}
	}

	private SurveyWaypoint addWaypointToMissionDay(MissionDay addToD, Waypoint wp, SamplingUnit su){
		SurveyWaypoint pw = new SurveyWaypoint();
		pw.setMissionDay(addToD);
		pw.setWaypoint(wp);
		pw.setSamplingUnit(su);
		if (addToD.getWaypoints() == null) addToD.setWaypoints(new ArrayList<SurveyWaypoint>());
		addToD.getWaypoints().add(pw);
		
		return pw;
	}
	
		
	private static final MissionDay findDay(Mission mission, LocalDate day, boolean create, LocalTime startTime, Session session){
		for (MissionDay md : mission.getMissionDays()){
			if (md.getDate().isEqual(day)){
				return md;
			}
		}
		if (!create) return null;
		
		MissionDay md = new MissionDay();
		md.setDate(day);
		md.setRestMinutes(0);
		md.setWaypoints(new ArrayList<SurveyWaypoint>());
		mission.getMissionDays().add(md);
		md.setMission(mission);
		
		
		//update leg and patrol dates as necessary
		if (mission.getStartDate().isAfter(md.getDate())){
			mission.setStartDate(md.getDate());
		}
		if (mission.getEndDate().isBefore(md.getDate())){
			mission.setEndDate(md.getDate());
		}
		
		//make sure there is at least one legday for each day between the start and end date
		
		LocalDate working = mission.getStartDate();
		//create a day for each mission day
		while(working.isBefore(mission.getEndDate()) || working.isEqual(mission.getEndDate())){
			boolean found = false;
			for (MissionDay temp : mission.getMissionDays()){
				if (temp.getDate().isEqual(working)){
					found = true;
				}
			}
			if (!found){
				MissionDay newd = new MissionDay();
				newd.setDate(working);
				newd.setEndTime(SharedUtils.END_OF_DAY);
				newd.setMission(mission);
				newd.setRestMinutes(0);
				newd.setStartTime(LocalTime.MIN);
				mission.getMissionDays().add(newd);
			}
			working = ChronoUnit.DAYS.addTo(working, 1);
		}
		
		if (startTime == null){
			md.setStartTime(LocalTime.MIN);
		}else{
			md.setStartTime(startTime);
		}
		md.setEndTime(SharedUtils.END_OF_DAY);
	
		return md;
	}
	
	public final void addPointToTrack(MissionDay missionDay, SamplingUnit su, Coordinate pnt, LocalDateTime time) throws Exception{
		if (pnt == null) return;
		if (missionDay == null) return;
		if (missionDay.getTracks() == null) missionDay.setTracks(new ArrayList<MissionTrack>());
		MissionJsonTrackProcessor.addSuPointToMisisonTracks(missionDay, su, pnt, time, locale);
	}
	
	public final void addPointToTrack(Mission mission, SamplingUnit su, Coordinate pnt, LocalDateTime time, Session session) throws Exception{
		if (pnt == null) return;
		MissionDay pld = findDay(mission, time.toLocalDate(), true, null, session);
		addPointToTrack(pld, su, pnt, time);
	}

	@Override
	public String getStatusMessage(Locale l) {
		if (newMissions.isEmpty() && modifiedMissions.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		if (!newMissions.isEmpty()){
			sb.append(MessageFormat.format(StatusMessage.ADDED.getMessage(l), newMissions.size()));
			sb.append("("); //$NON-NLS-1$
			for(Mission p : newMissions){
				sb.append(p.getId());
				sb.append(" "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")"); //$NON-NLS-1$
		}
		HashSet<Mission> tmp = new HashSet<Mission>(modifiedMissions);
		tmp.removeAll(newMissions);
		if (tmp.size() > 0){
			sb.append(MessageFormat.format(StatusMessage.MODIFIED.getMessage(l), tmp.size()));
			sb.append("("); //$NON-NLS-1$
			for(Mission p : tmp){
				sb.append(p.getId());
				sb.append(" "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")"); //$NON-NLS-1$
		}
		return sb.toString();
	}
}
