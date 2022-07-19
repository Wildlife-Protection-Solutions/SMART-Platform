/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.er.json;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.MissionIdGenerator;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.json.IJsonFeatureProcessor.Messages;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.TrackUtil;
import org.wcs.smart.util.UuidUtils;

/**
 * Processes mission JSON features, creating or updating missions.
 * 
 * @author Emily
 *
 */
public class MissionJsonFeatureProcessor extends IJsonFeatureProcessor {

	//Link data types for surveys
	public enum SurveyLinkDataType{
		MISSION("mission"), //$NON-NLS-1$
		INCIDENT("missionincident"), //$NON-NLS-1$
		SURVEY("survey"); //$NON-NLS-1$
			
		private String key;
		SurveyLinkDataType(String key){
			this.key = key;
		}
		
		public String getKey() {
			return this.key;
		}
	}
		
	private static final String SURVEY_KEY_ID = "id"; //$NON-NLS-1$

	private static final String MISSION_DATATYPE = "mission"; //$NON-NLS-1$
	private static final String SURVEY_DATATYPE = "survey"; //$NON-NLS-1$

	private static final String JSON_MISSIONUUID = "missionUuid"; //$NON-NLS-1$
	private static final String JSON_SURVEYUUID = "surveyUuid"; //$NON-NLS-1$
	
	//Survey/Mission related JSON feature types
	private enum SurveySmartFeatureType{
		SURVEY ("survey"), //$NON-NLS-1$
		SURVEY_NEW ("survey/new"), //$NON-NLS-1$
		MISSION("mission"), //$NON-NLS-1$
		MISSION_NEW("mission/new"), //$NON-NLS-1$
		MISSION_END("mission/end"), //$NON-NLS-1$
		TRACKPOINT("trackpoint/new"); //$NON-NLS-1$
		
		private String key;
			
		SurveySmartFeatureType(String key){
			this.key = key;
		}
		public String getKey() {
			return this.key;
		}
	}
		
	
	public enum Messages{
		INVALID_DATA_TYPE,
		INVALID_FEATURE_TYPE,
		MISSION_LINK_EXISTS,
		MISSION_LINK_MISSING,
		INVALID_MISSION_UUID,
		MISSIONDAY_MISSING,
		MISSING_PROPERTY,
		MISSION_EXISTS,
		INVALID_SURVEY_UUID,
		SURVEY_NOT_FOUND,
		EMPLOYEE_NOT_FOUND,
		NO_LEADER,
		NO_EMPLOYEES,
		CUSTOM_ATTRIBUTE_ERROR,
		SU_MISSING,
		DESIGN_MISSING,
		TRACKID,
		SURVEY_EXISTS,
		SURVEY_LINK_EXISTS,
		COMPLETE_MSG,
		OBSERVATION_EXISTS,
		WAYPOINT_NOT_FOUND,
		OBSERVATION_NOT_FOUND,
		CANNOT_UPDATE_DATE,
		CANNOT_UPDATE_SU;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(this, l);

		}
	}
	
	private Set<Mission> modifiedFeatures = new HashSet<>();

	
	private static final IWaypointSource MISSON_WP_SRC = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class)
			.getSource(SurveyWaypointSource.KEY);
	
	/**
	 * @return <code>true</code> if this processor can process the given feature
	 * type.  
	 */
	@Override
	public boolean canProcess(String featureType) {
		return featureType.equalsIgnoreCase(MISSION_DATATYPE) 
				|| featureType.equalsIgnoreCase(SURVEY_DATATYPE);
	}

	/**
	 * 
	 * @return set of features created by this processor
	 */
	public Set<Mission> getModifiedFeatures(){
		return this.modifiedFeatures;
	}
	
	@Override
	public void processFeature(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception {

		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);

		String dtype = props.get(JSON_SMARTDATATYPE).toString();
		if (dtype.equalsIgnoreCase(MISSION_DATATYPE)) {
			processMissionDataType(feature, ca, session, l);
		}else if (dtype.equalsIgnoreCase(SURVEY_DATATYPE)) {
			processSurveyDataType(feature, ca, session, l);
		}else {
			throw new Exception(MessageFormat.format(Messages.INVALID_DATA_TYPE.getMessage(l), dtype, MISSION_DATATYPE, SURVEY_DATATYPE));
		}
		
	}
	
	private void processSurveyDataType(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		
		if (!props.containsKey(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES)) {
			throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), IJsonFeatureProcessor.JSON_SMARTATTRIBUTES));
		}
		
		String ftype = props.get(JSON_SMARTFEATURETYPE).toString();
		
		if (ftype.equalsIgnoreCase(SurveySmartFeatureType.SURVEY_NEW.getKey())) {
			processNewSurvey(props, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SurveySmartFeatureType.SURVEY.getKey())) {
			processUpdateSurvey(props, ca, session, l);
		}else {
			throw new Exception(MessageFormat.format(Messages.INVALID_FEATURE_TYPE.getMessage(l), ftype, SurveySmartFeatureType.SURVEY_NEW.getKey() + ", " + SurveySmartFeatureType.SURVEY.getKey())); //$NON-NLS-1$
		}
		
		
	}

	private void processUpdateSurvey(JSONObject props, ConservationArea ca, Session session, Locale l) throws Exception {
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);

		String[] required = new String[] {JSON_SURVEYUUID};
		for (String r : required) {
			if (!attributes.containsKey(r)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), r));
		}
		
		UUID srcSurveyUuid = null;
		try {
			srcSurveyUuid = UuidUtils.stringToUuid((String)attributes.get(JSON_SURVEYUUID));
		}catch (Exception ex) {
			throw new Exception(MessageFormat.format(Messages.INVALID_SURVEY_UUID.getMessage(l), props.get(JSON_SURVEYUUID)));
		}

		Survey toUpdate = findSurveyLink(srcSurveyUuid, ca, session, l);
		if (toUpdate == null) throw new Exception(MessageFormat.format(Messages.SURVEY_NOT_FOUND.getMessage(l), srcSurveyUuid));
		
		if (attributes.containsKey(SURVEY_KEY_ID)) {
			String id = (String)attributes.get(SURVEY_KEY_ID);
			toUpdate.setId(id);
		}

		session.save(toUpdate);
		session.flush();		
	}
	
	private void processNewSurvey(JSONObject props, ConservationArea ca, Session session, Locale l) throws Exception {
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);

		String[] required = new String[] {JSON_SURVEYUUID,
				MissionAttributeMetadata.MissionMetadata.SURVEYDESIGN.getKey()};
		for (String r : required) {
			if (!attributes.containsKey(r)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), r));
		}
		
		UUID srcSurveyUuid = null;
		try {
			srcSurveyUuid = UuidUtils.stringToUuid((String)attributes.get(JSON_SURVEYUUID));
		}catch (Exception ex) {
			throw new Exception(MessageFormat.format(Messages.INVALID_SURVEY_UUID.getMessage(l), props.get(JSON_SURVEYUUID)));
		}

		Survey temp = findSurveyLink(srcSurveyUuid, ca, session, l);
		if (temp != null) {
			//a link to a mission already exists in the database for this uuid; 
			throw new Exception(MessageFormat.format(Messages.SURVEY_EXISTS.getMessage(l), srcSurveyUuid));
		}
		
		//check for survey design
		String sd = (String)attributes.get(MissionAttributeMetadata.MissionMetadata.SURVEYDESIGN.getKey());
		SurveyDesign design = findSurveyDesign(sd, ca, session, l);
		if (design == null) {
			throw new Exception(MessageFormat.format(Messages.DESIGN_MISSING.getMessage(l), sd));
		}
		
		String id = null;
		if (attributes.containsKey(SURVEY_KEY_ID)) {
			id = (String)attributes.get(SURVEY_KEY_ID);
		}

		//create new Survey
		Survey survey = new Survey();
		survey.setMissions(new ArrayList<>());
		survey.setSurveyDesign(design);
		if (id.trim().isBlank()) {
			id = MissionIdGenerator.INSTANCE.generateSurveyId(survey, session, l);
		}
		survey.setId(  id );
		session.save(survey);
		session.flush();
		DataLink dlink = new DataLink();
		dlink.setConservationArea(ca);
		dlink.setProviderId(srcSurveyUuid);
		dlink.setSmartId(survey.getUuid());
		dlink.setDataType(SurveyLinkDataType.SURVEY.getKey());
		session.save(dlink);
	}
	
	private void processMissionDataType(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		
		if (!props.containsKey(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES)) {
			throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), IJsonFeatureProcessor.JSON_SMARTATTRIBUTES));
		}
		
		String ftype = props.get(JSON_SMARTFEATURETYPE).toString();
		if (ftype.equalsIgnoreCase(SmartFeatureType.WAYPOINT_NEW.getKey())) {
			processWaypoint(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SmartFeatureType.WAYPOINT.getKey())) {
			processWaypointUpdate(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SmartFeatureType.OBSERVATION.getKey())) {
			processObservationUpdate(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SurveySmartFeatureType.MISSION_NEW.getKey())) {
			processStartMission(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SurveySmartFeatureType.MISSION_END.getKey())) {
			processEndMission(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SurveySmartFeatureType.TRACKPOINT.getKey())) {
			processTrackPoint(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SurveySmartFeatureType.MISSION.getKey())) {
			processMissionUpdate(feature, ca, session, l);
		}else {
			StringBuilder sb = new StringBuilder();
			sb.append(SmartFeatureType.WAYPOINT.getKey());
			sb.append(", "); //$NON-NLS-1$
			sb.append(SmartFeatureType.WAYPOINT_NEW.getKey());
			sb.append(", "); //$NON-NLS-1$
			sb.append(SmartFeatureType.OBSERVATION.getKey());
			sb.append(", "); //$NON-NLS-1$
			sb.append(SurveySmartFeatureType.MISSION_END.getKey());
			sb.append(", "); //$NON-NLS-1$
			sb.append(SurveySmartFeatureType.MISSION_NEW.getKey());
			sb.append(", "); //$NON-NLS-1$
			sb.append(SurveySmartFeatureType.MISSION.getKey());
			sb.append(", "); //$NON-NLS-1$
			sb.append(SurveySmartFeatureType.TRACKPOINT.getKey());
			throw new Exception(MessageFormat.format(Messages.INVALID_FEATURE_TYPE.getMessage(l), ftype, sb.toString()));
		}

	}
	
	private void processWaypoint(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		Waypoint wp = super.createWaypoint(feature, ca, session, l);
		Waypoint existingWp = findIncidentLink(wp.getUuid(), ca, session, l);

		LocalDateTime date = super.getDateTime(props);
		if (existingWp == null && date == null) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), JSON_DATETIME_KEY));
		if (date == null) date = existingWp.getDateTime();
		
		UUID srcMissionUuid = getSourceMissionUuid(attributes, l);
		Mission mission = findMissionLink(srcMissionUuid, ca, session, l);
		if (mission == null) throw new Exception(MessageFormat.format(Messages.MISSION_LINK_MISSING.getMessage(l), srcMissionUuid.toString()));
		
		modifiedFeatures.add(mission);
		
		if (date.toLocalDate().isBefore(mission.getStartDate())) {
			mission.setStartDate(date.toLocalDate());
		}
		
		if (date.toLocalDate().isAfter(mission.getEndDate())) {
			mission.setEndDate(date.toLocalDate());
		}
		createMissingDays(mission);
		
		
		MissionDay toUpdate = null;
		for (MissionDay day : mission.getMissionDays()) {
			if (day.getDate().equals(date.toLocalDate())) {
				toUpdate = day;
				break;
			}
		}
		
		if (toUpdate == null) throw new Exception(Messages.MISSIONDAY_MISSING.getMessage(l));
		if (toUpdate.getWaypoints() == null) toUpdate.setWaypoints(new ArrayList<>());
		if (toUpdate.getEndTime().equals(LocalTime.MAX) || toUpdate.getEndTime().isBefore(date.toLocalTime())) {
			toUpdate.setEndTime(date.toLocalTime());
		}

		SamplingUnit su = null;
		if (attributes.containsKey(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey())) {
			String suid = attributes.get(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey()).toString();
			
			su = findSamplingUnit(suid,ca,session,l);
			if (su == null) {
				warnings.add(MessageFormat.format(Messages.SU_MISSING.getMessage(l), suid));
			}
		}
		
		HashMap<WaypointObservationGroup, UUID> links = new HashMap<>();
		HashMap<WaypointObservation, UUID> obslinks = new HashMap<>();

		if (existingWp == null) {
			wp.setSourceId(SurveyWaypointSource.KEY);
			if (wp.getId() == null) wp.setId(String.valueOf(toUpdate.getWaypoints().size() + 1));
						
			UUID src = wp.getUuid();
			wp.setUuid(null);

			//create a new waypoint & associated links
			for (WaypointObservationGroup g : wp.getObservationGroups()) {
				if (g.getUuid() != null) {
					//clear any old link
					session.createQuery("DELETE From DataLink WHERE providerId = :uuid and dataType = :datatype") //$NON-NLS-1$
						.setParameter("uuid", g.getUuid()) //$NON-NLS-1$
						.setParameter("datatype", LinkDataType.OBSERVATION_GROUP.getKey()) //$NON-NLS-1$
						.executeUpdate();
					links.put(g,g.getUuid());
					g.setUuid(null);
					
				}
				
				for (WaypointObservation wo : g.getObservations()) {
					if (wo.getUuid() != null) {
						//clear any old link
						session.createQuery("DELETE From DataLink WHERE providerId = :uuid and dataType = :datatype") //$NON-NLS-1$
							.setParameter("uuid", wo.getUuid()) //$NON-NLS-1$
							.setParameter("datatype", LinkDataType.OBSERVATION.getKey()) //$NON-NLS-1$
							.executeUpdate();	
						obslinks.put(wo,  wo.getUuid());
						wo.setUuid(null);
					}
				}
			}
			
			computeAttachmentLocation(wp, toUpdate.getMission(), session);

			SurveyWaypoint pwp = new SurveyWaypoint();
			pwp.setWaypoint(wp);
			pwp.setMissionDay(toUpdate);
			pwp.setSamplingUnit(su);
			
			//TODO:  this is for reconnaissance missions where 
			//sampling units aren't assigned; instead we want to link the 
			//waypoint to a mission track
			pwp.setMissionTrack(null);
			
			toUpdate.getWaypoints().add(pwp);
			session.saveOrUpdate(wp);
			session.saveOrUpdate(pwp);
			
			session.flush();
			
			if (src != null) {
				DataLink dlink = new DataLink();
				dlink.setConservationArea(ca);
				dlink.setProviderId(src);
				dlink.setSmartId(wp.getUuid());
				dlink.setDataType(SurveyLinkDataType.INCIDENT.getKey());
				session.save(dlink);
			}

			//add track point
			addTrackPoint(toUpdate, getPosition(feature), su, l);
		}else {
			
			computeAttachmentLocation(wp, toUpdate.getMission(), session);
			
			//merge observation groups with existing wp
			for (WaypointObservation wo : wp.getAllObservations()) {
				if (wo.getUuid() != null) {
					WaypointObservation existing = findObservationLink(wo.getUuid(), ca, session);
					if (existing != null) {
						//throw an error - should not be updating observations this way
						throw new Exception(MessageFormat.format(Messages.OBSERVATION_EXISTS.getMessage(l), SmartFeatureType.OBSERVATION.getKey()));
					}	
					obslinks.put(wo,  wo.getUuid());
					wo.setUuid(null);
				}
			}
			
			List<WaypointObservationGroup> add = new ArrayList<>();
			for (WaypointObservationGroup group : wp.getObservationGroups()) {
				if (group.getUuid() == null) {
					add.add(group);
				}else {
					WaypointObservationGroup existing = findWaypointObservationGroup(group.getUuid(), ca, session);
					if (existing == null || !existing.getWaypoint().equals(existingWp)) {
						add.add( group );
					}else {
						//add observation from group to existing
						for (WaypointObservation o : group.getObservations()) {
							existing.getObservations().add(o);
							o.setObservationGroup(existing);
						}
					}
				}
			}
			for (WaypointObservationGroup g : add) {
				if (g.getUuid() != null) links.put(g, g.getUuid());
				g.setUuid(null);
				existingWp.getObservationGroups().add(g);
				g.setWaypoint(existingWp);
			}
			session.saveOrUpdate(existingWp);
			for (WaypointObservationGroup g : existingWp.getObservationGroups()) session.saveOrUpdate(g);
		}
		
		session.flush();
		for (Entry<WaypointObservationGroup, UUID> link : links.entrySet()) {
			DataLink dlink = new DataLink();
			dlink.setConservationArea(ca);
			dlink.setProviderId(link.getValue());
			dlink.setSmartId(link.getKey().getUuid());
			dlink.setDataType(LinkDataType.OBSERVATION_GROUP.getKey());
			session.save(dlink);
		}
		for (Entry<WaypointObservation, UUID> link : obslinks.entrySet()) {
			DataLink dlink = new DataLink();
			dlink.setConservationArea(ca);
			dlink.setProviderId(link.getValue());
			dlink.setSmartId(link.getKey().getUuid());
			dlink.setDataType(LinkDataType.OBSERVATION.getKey());
			session.save(dlink);
		}
		
		if (toUpdate.getStartTime().equals(LocalTime.MIN) || date.toLocalTime().isBefore(toUpdate.getStartTime())) {
			toUpdate.setStartTime(date.toLocalTime());
		}
	}
	
	private void processStartMission(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		
		Mission newMission = parseMission(feature, ca, session, l);
		UUID srcMissionUuid = newMission.getUuid();
		newMission.setUuid(null);
		
		modifiedFeatures.add(newMission);
		
		String[] required = new String[] {JSON_MISSIONUUID};
		for (String r : required) {
			if (!attributes.containsKey(r)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), r));
		}
		
		Mission temp = findMissionLink(srcMissionUuid, ca, session, l);
		if (temp != null) {
			//a link to a mission already exists in the database for this uuid; 
			throw new Exception(MessageFormat.format(Messages.MISSION_EXISTS.getMessage(l), srcMissionUuid));
		}
		
		if (newMission.getSurvey() == null) {
			//check for survey design
			String sd = (String)attributes.get(MissionAttributeMetadata.MissionMetadata.SURVEYDESIGN.getKey());
			SurveyDesign design = findSurveyDesign(sd, ca, session, l);
			if (design == null) {
				throw new Exception(MessageFormat.format(Messages.DESIGN_MISSING.getMessage(l), sd));
			}
			
			//create new Survey
			Survey survey = new Survey();
			survey.setMissions(new ArrayList<>());
			survey.setSurveyDesign(design);
			survey.setId(MissionIdGenerator.INSTANCE.generateSurveyId(survey, session, l) );
			session.save(survey);
			newMission.setSurvey(survey);
		}
		
		if (newMission.getMembers().isEmpty()) {
			throw new Exception(Messages.NO_EMPLOYEES.getMessage(l));
		}
		if (newMission.getLeader() == null) {
			throw new Exception(Messages.NO_LEADER.getMessage(l));
		}
		
		//add a track point
		Coordinate position = super.getPosition(feature);
		
		SamplingUnit su = null;
		if (attributes.containsKey(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey())) {
			String suid = attributes.get(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey()).toString();
			su = findSamplingUnit(suid,ca,session,l);
			if (su == null) {
				warnings.add(MessageFormat.format(Messages.SU_MISSING.getMessage(l), suid));
			}
		}
		
		addTrackPoint(newMission.getMissionDays().get(0), position, su, l);		
		
		//generate id
		if (newMission.getId() == null || newMission.getId().trim().isEmpty()) {
			newMission.setId(MissionIdGenerator.INSTANCE.generateMissionId(newMission, session));
		}
		session.save(newMission);
		session.flush();
		
		//create data links
		DataLink link = new DataLink();
		link.setConservationArea(ca);
		link.setProviderId(srcMissionUuid);
		link.setDataType(SurveyLinkDataType.MISSION.getKey());
		link.setSmartId(newMission.getUuid());
		session.save(link);
		
	}
	
	private UUID getSourceMissionUuid(JSONObject attributes, Locale l) throws Exception{
		if (!attributes.containsKey(JSON_MISSIONUUID)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), JSON_MISSIONUUID));
		try {
			return UuidUtils.stringToUuid((String)attributes.get(JSON_MISSIONUUID));
		}catch (Exception ex) {
			throw new Exception(MessageFormat.format(Messages.INVALID_MISSION_UUID.getMessage(l), attributes.get(JSON_MISSIONUUID)));
		}
	}

	
	private void processEndMission(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//configure date/time
		LocalDateTime date = super.getDateTime(props);
		Coordinate position = super.getPosition(feature);
		
		//find the mission
		UUID srcMissionUuid = getSourceMissionUuid(attributes, l);
		
		Mission mission = findMissionLink(srcMissionUuid, ca, session, l);
		if (mission == null) throw new Exception(MessageFormat.format(Messages.MISSION_LINK_MISSING.getMessage(l), srcMissionUuid.toString()));
		modifiedFeatures.add(mission);
		
		//update end date if required
		if (mission.getEndDate().isBefore(date.toLocalDate())) {
			mission.setEndDate(date.toLocalDate());
			createMissingDays(mission);
		}
		
		SamplingUnit su = null;
		if (attributes.containsKey(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey())) {
			String suid = attributes.get(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey()).toString();
			su = findSamplingUnit(suid,ca,session,l);
			if (su == null) {
				warnings.add(MessageFormat.format(Messages.SU_MISSING.getMessage(l), suid));
			}
		}
		
		for (MissionDay d : mission.getMissionDays()) {
			if (d.getDate().equals(date.toLocalDate())) {
				if (d.getEndTime().equals(LocalTime.MAX) ||  date.toLocalTime().isAfter(d.getEndTime())) {
					d.setEndTime(date.toLocalTime());
				}
				addTrackPoint(d, position, su, l);
				break;
			}
		}
		
		
	}
	
	
	private void processTrackPoint(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//configure date/time
		LocalDateTime date = super.getDateTime(props);
		
		UUID srcMissionUuid = getSourceMissionUuid(attributes, l);
		Mission mission = findMissionLink(srcMissionUuid, ca, session, l);
		if (mission == null) throw new Exception(MessageFormat.format(Messages.MISSION_LINK_MISSING.getMessage(l), srcMissionUuid.toString()));
		modifiedFeatures.add(mission);
		Coordinate position = super.getPosition(feature);

		//update leg dates
		if (date.toLocalDate().isBefore(mission.getStartDate())) {
			mission.setStartDate(date.toLocalDate());
		}
		
		if (date.toLocalDate().isAfter(mission.getEndDate())) {
			mission.setEndDate(date.toLocalDate());
		}
		createMissingDays(mission);
				
		MissionDay toUpdate = null;
		for (MissionDay day : mission.getMissionDays()) {
			if (day.getDate().equals(date.toLocalDate())) {
				toUpdate = day;
				break;
			}
		}
		if (toUpdate == null) throw new Exception(Messages.MISSIONDAY_MISSING.getMessage(l));

		if (toUpdate.getStartTime().equals(LocalTime.MIN) || date.toLocalTime().isBefore(toUpdate.getStartTime())) {
			toUpdate.setStartTime(date.toLocalTime());
		}
		if (toUpdate.getEndTime().equals(LocalTime.MAX) ||  toUpdate.getEndTime().isBefore(date.toLocalTime())) {
			toUpdate.setEndTime(date.toLocalTime());
		}
		SamplingUnit su = null;
		if (attributes.containsKey(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey())) {
			String suid = attributes.get(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey()).toString();
			
			su = findSamplingUnit(suid,ca,session,l);
			if (su == null) {
				warnings.add(MessageFormat.format(Messages.SU_MISSING.getMessage(l), suid));
			}
		}
		addTrackPoint(toUpdate, position, su, l);
	}
	
	private boolean isSame(SamplingUnit s1, SamplingUnit s2 ) {
		if (s1 == null && s2 == null) return true;
		if (s1 != null && s1.equals(s2)) return true;
		return false;
	}

	private void addTrackPoint(MissionDay toUpdate, Coordinate position, SamplingUnit su, Locale l) throws Exception {
		
		if (toUpdate.getTracks() == null) {
			toUpdate.setTracks(new ArrayList<>());
		}
		
		//find the track with same su that's contained
		//in the time range or else find the last track
		MissionTrack containedTrack = null;
		MissionTrack lastTrack = null;

		double lastpnt = Double.MIN_VALUE;
		for (MissionTrack t : toUpdate.getTracks()) {
			//find track closest to position.z
			if (t.getLineString() == null) continue;
			Coordinate last = t.getLineString().getCoordinateN(t.getLineString().getCoordinates().length -1 );
			Coordinate first = t.getLineString().getCoordinateN(0);
			if (last.z > lastpnt) {
				lastpnt = last.z;
				lastTrack = t;
			}
			if (position.z > first.z && position.z < last.z) {
				if (isSame(t.getSamplingUnit(), su)) containedTrack = t;
			}	
		}
		
		//pick the track to add to
		MissionTrack track = containedTrack != null ? containedTrack : lastTrack;
		if (track == null || !isSame(track.getSamplingUnit(),su)){
			track = new MissionTrack();
			track.setMissionDay(toUpdate);
			track.setSamplingUnit(su);
			track.setId(MessageFormat.format(Messages.TRACKID.getMessage(l), toUpdate.getTracks().size() + 1));
			toUpdate.getTracks().add(track);
		}

		boolean exists = false;
		List<Coordinate> items = new ArrayList<>();
		if (track.getLineString() != null) {
			for (Coordinate c : track.getLineString().getCoordinates()) {
				items.add(c);
				if (c.x == position.x && c.y == position.y && c.z == position.z) {
					exists = true;
				}
			}
		}
		if (!exists) items.add(position);		
		if (items.size() == 1) items.add(position);
		
		track.setLineString(TrackUtil.convertToLineString(items));
	}
	
	

	/**
	 * Creates a user friendly message describing the actions 
	 * applied to the database
	 */
	@Override
	public String getMessage(Locale l) {
		if (modifiedFeatures.isEmpty())
			return null;
 
		return MessageFormat.format(Messages.COMPLETE_MSG.getMessage(l), modifiedFeatures.size(),
				modifiedFeatures.stream().map(p->p.getId()).collect(Collectors.joining(", "))); //$NON-NLS-1$
	}
	
	private Survey findSurvey(UUID surveyUuid, ConservationArea ca, Session session, Locale l) {
		Survey s = session.get(Survey.class, surveyUuid);
		if (s == null) return null;
		if (!s.getSurveyDesign().getConservationArea().equals(ca)) return null;
		return s;
	}
	
	private SamplingUnit findSamplingUnit(String sustr, ConservationArea ca, Session session, Locale l) {
		UUID suUuid = null; 
		try {
			suUuid = UuidUtils.stringToUuid(sustr);
		}catch (Exception ex) {
			return null;
		}
		SamplingUnit su = session.get(SamplingUnit.class, suUuid);
		if (su == null) return null;
		if (!su.getSurveyDesign().getConservationArea().equals(ca)) return null;
		return su;
	}
	
	private SurveyDesign findSurveyDesign(String designKey, ConservationArea ca, Session session, Locale l) {
		SurveyDesign design = session.createQuery("FROM SurveyDesign WHERE keyId = :key and conservationArea = :ca ", SurveyDesign.class) //$NON-NLS-1$
				.setParameter("key", designKey) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.uniqueResult();
		if (design == null) return null;
		if (!design.getConservationArea().equals(ca)) return null;
		return design;
	}
	
	private Mission findMissionLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
		.setParameter("ca",ca) //$NON-NLS-1$
		.setParameter("puuid", providerUuid) //$NON-NLS-1$
		.setParameter("datatype", SurveyLinkDataType.MISSION.getKey()) //$NON-NLS-1$
		.uniqueResult();
		
		if (link == null) return null;
		
		Mission mission = session.get(Mission.class, link.getSmartId());
		if (mission == null) {
			//the object this links to does exist, so lets delete it and allow a new one
			session.delete(link);
			return null;
		}
		if (!mission.getSurvey().getSurveyDesign().getConservationArea().equals(ca)) {
			throw new Exception(Messages.MISSION_LINK_EXISTS.getMessage(l));
		}
			
		
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return mission;
	}
	
	private Survey findSurveyLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
		.setParameter("ca",ca) //$NON-NLS-1$
		.setParameter("puuid", providerUuid) //$NON-NLS-1$
		.setParameter("datatype", SurveyLinkDataType.SURVEY.getKey()) //$NON-NLS-1$
		.uniqueResult();
		
		if (link == null) return null;
		
		Survey survey = session.get(Survey.class, link.getSmartId());
		if (survey == null) {
			//the object this links to does exist, so lets delete it and allow a new one
			session.delete(link);
			return null;
		}
		if (!survey.getSurveyDesign().getConservationArea().equals(ca)) {
			throw new Exception(Messages.SURVEY_LINK_EXISTS.getMessage(l));
		}
			
		
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return survey;
	}
	
	private void createMissingDays(Mission mission) {
		
		LocalDate working = LocalDate.from(mission.getStartDate());
		
		Set<LocalDate> dates = new HashSet<>();
		for (MissionDay md : mission.getMissionDays()) {
			dates.add(md.getDate());
		}
		while (working.isBefore(mission.getEndDate()) 
				|| working.isEqual(mission.getEndDate())) {
			
			if (!dates.contains(working)){
				MissionDay md = new MissionDay();
				md.setDate(working);
				md.setStartTime(LocalTime.MIN);
				md.setEndTime(LocalTime.MAX);
				md.setRestMinutes(0);
				md.setTracks(new ArrayList<MissionTrack>());
				md.setWaypoints(new ArrayList<SurveyWaypoint>());
				md.setMission(mission);
				mission.getMissionDays().add(md);
			}
			working = ChronoUnit.DAYS.addTo(working, 1);
		}
	}
	
	private Waypoint findIncidentLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", SurveyLinkDataType.INCIDENT.getKey()) //$NON-NLS-1$
			.uniqueResult();
		
		if (link == null) return null;
		
		Waypoint waypoint = session.get(Waypoint.class, link.getSmartId());
		if (waypoint == null) {
			session.delete(link);
			return null;
		}
		if (!waypoint.getConservationArea().equals(ca)) {
			throw new Exception("Link Conservation Area doesn't match waypoint Conservation Area"); //$NON-NLS-1$
		}
		
		if (!waypoint.getSourceId().equals(SurveyWaypointSource.KEY)) {
			throw new Exception("Link is not a mission incident"); //$NON-NLS-1$
		}
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return waypoint;
	}	
	
	private void processWaypointUpdate(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//find the incident
		if (!attributes.containsKey(JSON_INCIDENTUUID_KEY)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), JSON_INCIDENTUUID_KEY));

		Waypoint wp = super.createWaypoint(feature, ca, session, l);
		Waypoint toUpdate = findIncidentLink(wp.getUuid(), ca, session, l);
		if (toUpdate == null) throw new Exception(MessageFormat.format(Messages.WAYPOINT_NOT_FOUND.getMessage(l), wp.getUuid().toString()));

		LocalDateTime currentdt = toUpdate.getDateTime();
		Double x = toUpdate.getRawX();
		Double y = toUpdate.getRawY();
		boolean updateTrackPoint = false;
		
		if (wp.getDateTime() != null && !wp.getDateTime().toLocalDate().equals(toUpdate.getDateTime().toLocalDate())){
			throw new Exception(Messages.CANNOT_UPDATE_DATE.getMessage(l));
		}
		
		//find mission with associated waypoint
		SurveyWaypoint sw = session.createQuery("FROM SurveyWaypoint WHERE id.waypoint = :wp ", SurveyWaypoint.class) //$NON-NLS-1$
				.setParameter("wp",toUpdate) //$NON-NLS-1$
				.uniqueResult();
		if (sw == null) throw new Exception(MessageFormat.format(Messages.WAYPOINT_NOT_FOUND.getMessage(l), wp.getUuid().toString()));
		
		//check the sampling unit - cannot update the sampling unit
		if (attributes.containsKey(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey())) {
			String suid = attributes.get(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.getKey()).toString();
			UUID newSuUuid = null;
			if (suid != null && !suid.trim().isEmpty()) newSuUuid = UuidUtils.stringToUuid(suid);
			if ((sw.getSamplingUnit() == null && newSuUuid != null) ||
				!sw.getSamplingUnit().getUuid().equals(newSuUuid)) 
				throw new Exception(Messages.CANNOT_UPDATE_SU.getMessage(l));
		}
		
		modifiedFeatures.add(sw.getMissionDay().getMission());
		
		if (wp.getDateTime() != null && !wp.getDateTime().equals(toUpdate.getDateTime())) {
			updateTrackPoint = true;
			toUpdate.setDateTime(wp.getDateTime());
		}
		if (wp.getRawX() != null && wp.getRawY() != null && 
				(wp.getRawX() != toUpdate.getRawX() || wp.getRawY() != toUpdate.getRawY())){
			updateTrackPoint = true;
			toUpdate.setRawX(wp.getRawX());
			toUpdate.setRawY(wp.getRawY());
		}
		
		if (wp.getId() != null) toUpdate.setId(wp.getId());
		if (wp.getComment() != null) toUpdate.setComment(wp.getComment());
		if (wp.getDirection() != null) toUpdate.setDirection(wp.getDirection());
		if (wp.getDistance() != null) toUpdate.setDistance(wp.getDistance());

		updateObserver(toUpdate, attributes, ca, session, l);
	
		//attachments
		if (toUpdate.getAttachments() == null) toUpdate.setAttachments(new ArrayList<>());
		for (WaypointAttachment attachment: wp.getAttachments()) {
			WaypointAttachment newattachment = new WaypointAttachment();
			newattachment.setCopyFromLocation(attachment.getCopyFromLocation());
			newattachment.setFilename(attachment.getFilename());
			newattachment.setWaypoint(toUpdate);
			toUpdate.getAttachments().add(newattachment);
		}
		
		if (updateTrackPoint) {
			//lets try to find a track point that matches the old values and update it to the new values
			Coordinate toFind = new Coordinate(x,y, SharedUtils.toLongTime(currentdt));
			Coordinate updateTo = new Coordinate(toUpdate.getRawX(), toUpdate.getRawY(), SharedUtils.toLongTime(toUpdate.getDateTime()));
			
			for (MissionTrack t : sw.getMissionDay().getTracks()) {
				boolean isModified = false;
				
				if ( (t.getSamplingUnit() == null && sw.getSamplingUnit() == null) ||
						(t.getSamplingUnit() != null && t.getSamplingUnit().equals(sw.getSamplingUnit()))) {
					LineString ls = t.getLineString();
					
					List<Coordinate> items = new ArrayList<>();
					for (Coordinate c : ls.getCoordinates()) {
						if (c.getX() == toFind.getX() && c.getY() == toFind.getY() && c.getZ() == toFind.getZ()) {
							isModified = true;
							items.add(updateTo);
						}else {
							items.add(c);
						}
					}
					if (isModified) {
						t.setLineString( TrackUtil.convertToLineString(items));
					}
				}
			}
		}
		
		session.save(toUpdate);
		session.flush();
		
	}
	
	private void processObservationUpdate(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//find the incident
		if (!attributes.containsKey(JSON_OBSERVATIONUUID_KEY)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), JSON_OBSERVATIONUUID_KEY));

		WaypointObservation observation = super.createWaypointObservation(attributes, ca, session, l);
		if (observation == null) throw new Exception(warnings.get(warnings.size() - 1));
				
		WaypointObservation toUpdate = findObservationLink(observation.getUuid(), ca, session);
		if (toUpdate == null) throw new Exception(MessageFormat.format(Messages.OBSERVATION_NOT_FOUND.getMessage(l), observation.getUuid().toString()));
		
		SurveyWaypoint sw = session.createQuery("FROM SurveyWaypoint WHERE id.waypoint = :wp ", SurveyWaypoint.class) //$NON-NLS-1$
				.setParameter("wp",toUpdate.getWaypoint()) //$NON-NLS-1$
				.uniqueResult();
		if (sw == null) throw new Exception(MessageFormat.format(Messages.WAYPOINT_NOT_FOUND.getMessage(l), toUpdate.getUuid().toString()));
		modifiedFeatures.add(sw.getMissionDay().getMission());

		if (attributes.containsKey(IJsonFeatureProcessor.JSON_OBSERVER_KEY)) {
			toUpdate.setObserver(observation.getObserver());
		}
	
		toUpdate.getAttributes().clear();
		session.flush();
			
		toUpdate.setCategory(observation.getCategory());
		for (WaypointObservationAttribute a : observation.getAttributes()) {
			a.setObservation(toUpdate);
			toUpdate.getAttributes().add(a);
		}
		
		//attachments
		if (toUpdate.getAttachments() == null) toUpdate.setAttachments(new ArrayList<>());
		for (ObservationAttachment attachment: observation.getAttachments()) {
			ObservationAttachment newattachment = new ObservationAttachment();
			newattachment.setCopyFromLocation(attachment.getCopyFromLocation());
			newattachment.setFilename(attachment.getFilename());
			newattachment.setObservation(toUpdate);
			toUpdate.getAttachments().add(newattachment);
		}
		session.save(toUpdate);
		session.flush();
	}
	
	private Mission parseMission(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//configure date/time
		LocalDateTime date = super.getDateTime(props);
		
		Mission newMission = new Mission();
		newMission.setStartDate(date.toLocalDate());
		newMission.setEndDate(date.toLocalDate());
		
		createMissingDays(newMission);
		
		newMission.getMissionDays().get(0).setStartTime(date.toLocalTime());
		newMission.getMissionDays().get(0).setEndTime(date.toLocalTime());

		if (attributes.containsKey(JSON_MISSIONUUID)) {
			try {
				UUID srcMissionUuid = UuidUtils.stringToUuid((String)attributes.get(JSON_MISSIONUUID));
				newMission.setUuid(srcMissionUuid);
			}catch (Exception ex) {
				throw new Exception(MessageFormat.format(Messages.INVALID_MISSION_UUID.getMessage(l), props.get(JSON_MISSIONUUID)));
			}	
		}
		
		
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.SURVEY.getKey())) {
			UUID surveyUuid = null;
			try {
				surveyUuid = UuidUtils.stringToUuid((String)attributes.get(MissionAttributeMetadata.MissionMetadata.SURVEY.getKey()));
			}catch (Exception ex) {
				throw new Exception(MessageFormat.format(Messages.INVALID_SURVEY_UUID.getMessage(l), props.get(JSON_MISSIONUUID)));
			}
			Survey survey;
			survey = findSurvey(surveyUuid, ca, session, l);
			if (survey == null) {
				survey = findSurveyLink(surveyUuid, ca, session, l);
			}
			if (survey == null) {
				throw new Exception(MessageFormat.format(Messages.SURVEY_NOT_FOUND.getMessage(l), surveyUuid));
			}
			newMission.setSurvey(survey);
		}
		
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.COMMENT.getKey())) {
			String comment = (String) attributes.get(MissionAttributeMetadata.MissionMetadata.COMMENT.getKey());
			newMission.setComment(comment);
		}
		
		
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.MISSIONID.getKey())) {
			String id = (String) attributes.get(MissionAttributeMetadata.MissionMetadata.MISSIONID.getKey());
			newMission.setId(id.trim());
		}
		
		//members leader & pilot
		newMission.setMembers(new ArrayList<>());
		
		String leader = null;
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.LEADER.getKey())) {
			leader = (String) attributes.get(MissionAttributeMetadata.MissionMetadata.LEADER.getKey());
		}
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.EMPLOYEES.getKey())) {
			JSONArray employees = (JSONArray) attributes.get(MissionAttributeMetadata.MissionMetadata.EMPLOYEES.getKey());
			
			for (int i = 0; i < employees.size(); i ++) {
				String euuid = (String) employees.get(i);
				
				UUID e = UuidUtils.stringToUuid(euuid);
				Employee employee = session.get(Employee.class, e);
				if (employee == null || !employee.getConservationArea().equals(ca)) {
					warnings.add(MessageFormat.format(Messages.EMPLOYEE_NOT_FOUND.getMessage(l), euuid));
				}else {
					MissionMember member = new MissionMember();
					member.setMission(newMission);
					member.setMember(employee);
					if (euuid.equalsIgnoreCase(leader)) {
						member.setIsLeader(true);
					}
					newMission.getMembers().add(member);
				}
			}
		}else {
			//no employees set leader and pilot
			if (leader != null) {
				UUID e = UuidUtils.stringToUuid(leader);
				Employee employee = session.get(Employee.class, e);
				if (employee == null || !employee.getConservationArea().equals(ca)) {
					warnings.add(MessageFormat.format(Messages.EMPLOYEE_NOT_FOUND.getMessage(l), leader));
				}else {
					MissionMember member = new MissionMember();
					member.setMission(newMission);
					member.setMember(employee);
					member.setIsLeader(true);
					newMission.getMembers().add(member);
				}
			}
		}
		
		//custom mission attributes
		List<MissionAttribute> customAttributes = session.createQuery("FROM MissionAttribute WHERE conservationArea = :ca", MissionAttribute.class) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.list();
		
		newMission.setMissionPropertyValues(new ArrayList<>());
		
		for (MissionAttribute custom : customAttributes) {
			String key = custom.getKeyId();
			if (!attributes.containsKey(key)) continue;
			
			
			MissionPropertyValue pvalue = new MissionPropertyValue();
			pvalue.setMission(newMission);
			pvalue.setMissionAttribute(custom);
			Object jsonValue = attributes.get(key);
			
			try {
				
				if (custom.getType() == AttributeType.BOOLEAN) {
					Boolean value = parseBoolean(jsonValue);
					pvalue.setNumberValue(value ? 1.0 : 0.0);
				}else if (custom.getType() == AttributeType.DATE) {
					LocalDate value = parseDate(jsonValue);
					pvalue.setDateValue(value);
				}else if (custom.getType() == AttributeType.TEXT) {
					String value = jsonValue.toString();
					pvalue.setStringValue(value);
				}else if (custom.getType() == AttributeType.LIST) {
					String itemkey = jsonValue.toString();
					MissionAttributeListItem value = null;
					for (MissionAttributeListItem item : custom.getAttributeList()) {
						if (item.getKeyId().equalsIgnoreCase(itemkey)) {
							value = item;
							break;
						}
					}
					if (value == null) {
						throw new Exception(MessageFormat.format("List item with key {0} not found for custom mission attribute {1}.", key, custom.getName())); //$NON-NLS-1$
					}
					pvalue.setAttributeListItem(value);
				}else if (custom.getType() == AttributeType.NUMERIC) {
					Double value = parseNumeric(jsonValue);
					pvalue.setNumberValue(value);
				}
					
			}catch (Exception ex) {
				warnings.add(MessageFormat.format(Messages.CUSTOM_ATTRIBUTE_ERROR.getMessage(l), custom.getName(), jsonValue));
				continue;
			}
			
			newMission.getMissionPropertyValues().add(pvalue);
		}
		
		return newMission;
	}
		
	private void processMissionUpdate(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		Mission newMission = parseMission(feature, ca, session, l);
		if (newMission.getUuid() == null)  throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), JSON_MISSIONUUID));
		
		Mission toUpdate = findMissionLink(newMission.getUuid(), ca, session, l);
		if (toUpdate == null)  
			throw new Exception(MessageFormat.format(Messages.MISSION_LINK_MISSING.getMessage(l), newMission.getUuid().toString()));
		
		modifiedFeatures.add(toUpdate);

		//if survey is updated make sure part of same design
		if (newMission.getSurvey() != null &&  
				!toUpdate.getSurvey().getSurveyDesign().equals(newMission.getSurvey().getSurveyDesign())) {
			throw new Exception("Cannot change the survey design associated with a mission."); //$NON-NLS-1$
		}
		if (newMission.getSurvey() != null) toUpdate.setSurvey(newMission.getSurvey());
		
		
		
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.COMMENT.getKey())) {
			toUpdate.setComment(newMission.getComment());
		}
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.MISSIONID.getKey())) {
			toUpdate.setId(newMission.getId());
		}
		
		UUID leaderUuid = null;
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.LEADER.getKey())) {
			leaderUuid = newMission.getLeader().getMember().getUuid();
		}else {
			leaderUuid = toUpdate.getLeader().getMember().getUuid();
		}
		
		if (attributes.containsKey(MissionAttributeMetadata.MissionMetadata.EMPLOYEES.getKey())) {
			Set<Employee> required = newMission.getMembers().stream().map(e->e.getMember()).collect(Collectors.toSet());
			
			List<MissionMember> toRemove = new ArrayList<>();
			for (MissionMember m : toUpdate.getMembers()) {
				if (!required.contains(m.getMember())) toRemove.add(m);
			}
			toUpdate.getMembers().removeAll(toRemove);
			Set<Employee> current = toUpdate.getMembers().stream().map(e->e.getMember()).collect(Collectors.toSet());
			
			for (Employee e : required) {
				if (!current.contains(e)) {
					MissionMember m = new MissionMember();
					m.setMember(e);
					m.setMission(toUpdate);
					toUpdate.getMembers().add(m);
				}
			}
		}
		
		if (toUpdate.getMembers().isEmpty()) {
			throw new Exception(Messages.NO_EMPLOYEES.getMessage(l));
		}
		
		boolean hasleader = false;
		for (MissionMember m : toUpdate.getMembers()) {
			if (m.getMember().getUuid().equals(leaderUuid)) {
				m.setIsLeader(true);
				hasleader = true;
			}else {
				m.setIsLeader(false);
			}
		}
		
		if (!hasleader) throw new Exception(Messages.NO_LEADER.getMessage(l));
		
		//custom mission attributes
		for (MissionPropertyValue value : newMission.getMissionPropertyValues()) {
		
			MissionPropertyValue propToUpdate = null;
			for (MissionPropertyValue v : toUpdate.getMissionPropertyValues()) {
				if (value.getMissionAttribute().equals(v.getMissionAttribute())) {
					propToUpdate= v;
					break;
				}
			}
			if (propToUpdate == null) {
				propToUpdate = new MissionPropertyValue();
				propToUpdate.setMission(toUpdate);
				toUpdate.getMissionPropertyValues().add(propToUpdate);
				propToUpdate.setMissionAttribute(value.getMissionAttribute());
			}
			propToUpdate.setValue(value.getValue());
		}

		if (newMission.getId() == null || newMission.getId().trim().isEmpty()) {
			newMission.setId(MissionIdGenerator.INSTANCE.generateMissionId(newMission, session));
		}
		session.save(toUpdate);
		session.flush();
	}
	
	
	private void computeAttachmentLocation(Waypoint wp, Mission mission, Session session) throws Exception {
		for(WaypointAttachment wa : wp.getAttachments()) {
			if (wa.getUuid() == null) {
				wa.computeFileLocation(Paths.get(wp.getConservationArea().getFileDataStoreLocation())
						.resolve(MISSON_WP_SRC.getDatastoreFileLocation(mission, session))
						.resolve(wa.getFilename()));
			}
		}
		for (WaypointObservation wo : wp.getAllObservations()) {
			for(ObservationAttachment oa : wo.getAttachments()) {
				if (oa.getUuid() == null) {
					oa.computeFileLocation(Paths.get(wp.getConservationArea().getFileDataStoreLocation())
							.resolve(MISSON_WP_SRC.getDatastoreFileLocation(mission, session))
							.resolve(oa.getFilename()));
				}
			}
		}
	}
}
