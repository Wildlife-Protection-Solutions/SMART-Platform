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
package org.wcs.smart.patrol.json;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
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
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.PatrolIdGenerator;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.TrackUtil;
import org.wcs.smart.util.UuidUtils;

/**
 * Processes patrol JSON features, creating or updating patrols.
 * 
 * @author Emily
 *
 */
public class PatrolJsonFeatureProcessor extends IJsonFeatureProcessor {

	//Link data types for patrols
	public enum PatrolLinkDataType{
		PATROL("patrol"), //$NON-NLS-1$
		INCIDENT("patrolincident"), //$NON-NLS-1$
		LEG("patrolleg"); //$NON-NLS-1$
		
		private String key;
		PatrolLinkDataType(String key){
			this.key = key;
		}
		
		public String getKey() {
			return this.key;
		}
	}
	
	//json field keys
	private static final String JSON_PATROLLEGUUID = "patrolLegUuid"; //$NON-NLS-1$
	private static final String JSON_PATROLUUID = "patrolUuid"; //$NON-NLS-1$
	
	//Patrol related JSON data type
	private static final String PATROL_DATATYPE = "patrol"; //$NON-NLS-1$
	
	//leg id; not added to the metadata as this is optional and generally not supplied by the user
	private static final String JSON_LEGID_KEY = "legId"; //$NON-NLS-1$
	
	//Patrol related JSON feature types
	private enum PatrolSmartFeatureType{
		PATROL_NEW ("patrol/new"), //$NON-NLS-1$
		PATROL("patrol"), //$NON-NLS-1$
		LEG_NEW("leg/new"), //$NON-NLS-1$
		LEG_END("leg/end"), //$NON-NLS-1$
		LEG("leg"), //$NON-NLS-1$
		TRACKPOINT("trackpoint/new"); //$NON-NLS-1$
		
		private String key;
		
		PatrolSmartFeatureType(String key){
			this.key = key;
		}
		public String getKey() {
			return this.key;
		}
	}
	
	public enum Messages{
		INVALID_DATA_TYPE,
		INVALID_FEATURE_TYPE,
		MISSING_PROPERTY,
		PATROLLEG_LINK_MISSING,
		PATROL_LINK_MISSING,
		PATROLLEG_MISSING,
		TRANSPORTTYPE_MISSING,
		MANDATE_MISSING,
		MANDATE_EXISTING,
		NO_EMPLOYEES,
		NO_LEADER,
		NO_PILOT,
		EMPLOYEE_NOT_FOUND,
		PATROL_LINK_EXISTS,
		PATROLLEG_LINK_EXISTS,
		INVALID_PATROL_UUID,
		INVALID_PATROLLEG_UUID,
		PATROL_EXISTS,
		PATROLLEG_EXISTS,
		TEAM_MISSING,
		STATION_MISSING,
		CUSTOM_ATTRIBUTE_ERROR,
		COMPLETE_MSG,
		OBSERVATION_EXISTS,
		WAYPOINT_NOT_FOUND,
		OBSERVATION_NOT_FOUND,
		CANNOT_UPDATE_DATE;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getLabel(this, l);
		}
	}
	
	private static final IWaypointSource PATROL_WP_SRC = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class)
			.getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
	
	private Set<Patrol> modifiedFeatures = new HashSet<>();

	/**
	 * @return <code>true</code> if this processor can process the given feature
	 * type.  
	 */
	@Override
	public boolean canProcess(String featureType) {
		return featureType.equalsIgnoreCase(PATROL_DATATYPE); 
	}

	/**
	 * 
	 * @return set of features created by this processor
	 */
	public Set<Patrol> getModifiedFeatures(){
		return this.modifiedFeatures;
	}
	
	
	@Override
	public void processFeature(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception {

		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);

		String dtype = props.get(JSON_SMARTDATATYPE).toString(); 
		if (!dtype.equalsIgnoreCase(PATROL_DATATYPE))
			throw new Exception(MessageFormat.format(Messages.INVALID_DATA_TYPE.getMessage(l), dtype, PATROL_DATATYPE));

		if (!props.containsKey(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES)) {
			throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), IJsonFeatureProcessor.JSON_SMARTATTRIBUTES));
		}
		
		String ftype = props.get(JSON_SMARTFEATURETYPE).toString();
		
		
		if (ftype.equalsIgnoreCase(PatrolSmartFeatureType.PATROL_NEW.getKey())) {
			processStartPatrol(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(PatrolSmartFeatureType.LEG_END.getKey())) {
			processEndPatrolLeg(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(PatrolSmartFeatureType.LEG_NEW.getKey())) {
			processNewLeg(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(PatrolSmartFeatureType.TRACKPOINT.getKey())) {
			processTrackPoint(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(PatrolSmartFeatureType.PATROL.getKey())) {
			processPatrolUpdate(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(PatrolSmartFeatureType.LEG.getKey())) {
			processPatrolLegUpdate(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SmartFeatureType.WAYPOINT_NEW.getKey())) {
			processingWaypoint(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SmartFeatureType.OBSERVATION.getKey())) {
			processObservationUpdate(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SmartFeatureType.WAYPOINT.getKey())) {
			processWaypointUpdate(feature, ca, session, l);
		}else {
			StringJoiner j = new StringJoiner(","); //$NON-NLS-1$
			for (PatrolSmartFeatureType t : PatrolSmartFeatureType.values()) {
				j.add(t.getKey());
			}
			for (SmartFeatureType t : SmartFeatureType.values()) {
				j.add(t.getKey());
			}
			throw new Exception(MessageFormat.format(Messages.INVALID_FEATURE_TYPE.getMessage(l), ftype, j.toString()));
		}

	}
	
	private void processingWaypoint(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		LocalDateTime date = super.getDateTime(props);

		Waypoint wp = super.createWaypoint(feature, ca, session, l);
		Waypoint existingWp = findIncidentLink(wp.getUuid(), ca, session, l);
		if (existingWp == null && date == null) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), JSON_DATETIME_KEY));
		if (date == null) date = existingWp.getDateTime();
		
		UUID srcPatrolLegUuid = getSourcePatrolLegUuid(attributes, l);
		PatrolLeg leg = findPatrolLegLink(srcPatrolLegUuid, ca, session, l);
		if (leg == null) throw new Exception(MessageFormat.format(Messages.PATROLLEG_LINK_MISSING.getMessage(l), srcPatrolLegUuid.toString()));
		
		//update leg dates
		if (date.toLocalDate().isBefore(leg.getStartDate())) {
			leg.setStartDate(date.toLocalDate());
			createLegDays(leg, session);
		}
		
		if (date.toLocalDate().isAfter(leg.getEndDate())) {
			leg.setEndDate(date.toLocalDate());
			createLegDays(leg, session);
		}

		//update patrol dates
		if (date.toLocalDate().isBefore(leg.getPatrol().getStartDate())) {
			leg.getPatrol().setStartDate(date.toLocalDate());
		}
		if (date.toLocalDate().isAfter(leg.getPatrol().getEndDate())) {
			leg.getPatrol().setEndDate(date.toLocalDate());
		}
		
		PatrolLegDay toUpdate = null;
		for (PatrolLegDay day : leg.getPatrolLegDays()) {
			if (day.getDate().equals(date.toLocalDate())) {
				toUpdate = day;
				break;
			}
		}
		
		if (toUpdate == null) throw new Exception(Messages.PATROLLEG_MISSING.getMessage(l));
		if (toUpdate.getWaypoints() == null) toUpdate.setWaypoints(new ArrayList<>());
		
		//new day or time before new time
		if (toUpdate.getUuid() == null || toUpdate.getEndTime().isBefore(date.toLocalTime())) {
			toUpdate.setEndTime(date.toLocalTime());
		}
		
		session.flush();
		
		modifiedFeatures.add(leg.getPatrol());
		
		HashMap<WaypointObservationGroup, UUID> groupLinks = new HashMap<>();
		HashMap<WaypointObservation, UUID> observationLinks = new HashMap<>();		

		if (existingWp == null) {
			wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
			if (wp.getId() == null)  wp.setId(String.valueOf(toUpdate.getWaypoints().size() + 1));
			
			UUID src = wp.getUuid();
			wp.setUuid(null);
			//create a new waypoint & associated links
			for (WaypointObservationGroup g : wp.getObservationGroups()) {
				if (g.getUuid() != null) {
					//clear any old link
					session.createMutationQuery("DELETE From DataLink WHERE providerId = :uuid and dataType = :datatype") //$NON-NLS-1$
						.setParameter("uuid", g.getUuid()) //$NON-NLS-1$
						.setParameter("datatype", LinkDataType.OBSERVATION_GROUP.getKey()) //$NON-NLS-1$
						.executeUpdate();
					groupLinks.put(g,g.getUuid());
					g.setUuid(null);
					
				}
				for (WaypointObservation wo : g.getObservations()) {
					if (wo.getUuid() != null) {
						//clear any old link
						session.createMutationQuery("DELETE From DataLink WHERE providerId = :uuid and dataType = :datatype") //$NON-NLS-1$
							.setParameter("uuid", wo.getUuid()) //$NON-NLS-1$
							.setParameter("datatype", LinkDataType.OBSERVATION.getKey()) //$NON-NLS-1$
							.executeUpdate();	
						observationLinks.put(wo,  wo.getUuid());
						wo.setUuid(null);
					}
				}
			}

			computeAttachmentLocation(wp, toUpdate.getPatrolLeg().getPatrol(), session);
			
			PatrolWaypoint pwp = new PatrolWaypoint();
			pwp.setWaypoint(wp);
			pwp.setPatrolLegDay(toUpdate);
			toUpdate.getWaypoints().add(pwp);
			
			if (wp.getUuid() == null) session.persist(wp);
			session.persist(pwp);
			
			
			session.flush();
			
			if (src != null) {
				DataLink dlink = new DataLink();
				dlink.setConservationArea(ca);
				dlink.setProviderId(src);
				dlink.setSmartId(wp.getUuid());
				dlink.setDataType(PatrolLinkDataType.INCIDENT.getKey());
				session.persist(dlink);
			}
			
			addTrackPoint(toUpdate, getPosition(feature));
			
		}else {
			//merge observation groups with existing wp
			computeAttachmentLocation(wp, toUpdate.getPatrolLeg().getPatrol(), session);
			
			for (WaypointObservation wo : wp.getAllObservations()) {
				if (wo.getUuid() != null) {
					WaypointObservation existing = findObservationLink(wo.getUuid(), ca, session);
					if (existing != null) {
						//throw an error - should not be updating observations this way
						throw new Exception(MessageFormat.format(Messages.OBSERVATION_EXISTS.getMessage(l), SmartFeatureType.OBSERVATION.getKey()));
					}	
					observationLinks.put(wo,  wo.getUuid());
					wo.setUuid(null);
				}
			}
			
			//add observations
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
				if (g.getUuid() != null) groupLinks.put(g, g.getUuid());
				g.setUuid(null);
				existingWp.getObservationGroups().add(g);
				g.setWaypoint(existingWp);
			}
			if (existingWp.getUuid() == null) session.persist(existingWp);
			
			
			for (WaypointObservationGroup g : existingWp.getObservationGroups()) {
				if (g.getUuid() == null) session.persist(g);
			}

		}
		
		session.flush();
		for (Entry<WaypointObservationGroup, UUID> link : groupLinks.entrySet()) {
			DataLink dlink = new DataLink();
			dlink.setConservationArea(ca);
			dlink.setProviderId(link.getValue());
			dlink.setSmartId(link.getKey().getUuid());
			dlink.setDataType(LinkDataType.OBSERVATION_GROUP.getKey());
			session.persist(dlink);
		}
		for (Entry<WaypointObservation, UUID> link : observationLinks.entrySet()) {
			DataLink dlink = new DataLink();
			dlink.setConservationArea(ca);
			dlink.setProviderId(link.getValue());
			dlink.setSmartId(link.getKey().getUuid());
			dlink.setDataType(LinkDataType.OBSERVATION.getKey());
			session.persist(dlink);
		}

		if (toUpdate.getStartTime().equals(LocalTime.MIN) || date.toLocalTime().isBefore(toUpdate.getStartTime())) {
			toUpdate.setStartTime(date.toLocalTime());
		}
		
		
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
		
		//find patrol with associated waypoint
		PatrolWaypoint pw = session.createQuery("FROM PatrolWaypoint WHERE id.waypoint = :wp ", PatrolWaypoint.class) //$NON-NLS-1$
				.setParameter("wp",toUpdate) //$NON-NLS-1$
				.uniqueResult();
		if (pw == null) throw new Exception(MessageFormat.format(Messages.WAYPOINT_NOT_FOUND.getMessage(l), wp.getUuid().toString()));
		
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
		
		//attachments
		if (toUpdate.getAttachments() == null) toUpdate.setAttachments(new ArrayList<>());
		for (WaypointAttachment attachment: wp.getAttachments()) {
			WaypointAttachment newattachment = new WaypointAttachment();
			newattachment.setCopyFromLocation(attachment.getCopyFromLocation());
			newattachment.setFilename(attachment.getFilename());
			newattachment.setWaypoint(toUpdate);
			toUpdate.getAttachments().add(newattachment);
		}
		
		updateObserver(toUpdate, attributes, ca, session, l);
		
		if (updateTrackPoint) {
			//lets try to find a track point that matches the old values and update it to the new values
			Coordinate toFind = new Coordinate(x,y, SharedUtils.toLongTime(currentdt));
			Coordinate updateTo = new Coordinate(toUpdate.getRawX(), toUpdate.getRawY(), SharedUtils.toLongTime(toUpdate.getDateTime()));
			
			for (Track t : pw.getPatrolLegDay().getTracks()) {
				boolean isModified = false;
				List<LineString> newLineStrings = new ArrayList<>();
				for (LineString ls : t.getLineStrings()) {
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
						newLineStrings.add(TrackUtil.convertToLineString(items));
					}else {
						newLineStrings.add(ls);
					}
				}
				if (isModified) {
					t.setLineStrings(newLineStrings);
				}
			}
		}
		
		session.persist(toUpdate);
		session.flush();
		
		modifiedFeatures.add(pw.getPatrolLegDay().getPatrolLeg().getPatrol());
		
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
		
		PatrolWaypoint pw = session.createQuery("FROM PatrolWaypoint WHERE id.waypoint = :wp ", PatrolWaypoint.class) //$NON-NLS-1$
				.setParameter("wp", toUpdate.getWaypoint()) //$NON-NLS-1$
				.uniqueResult();
		if (pw == null) throw new Exception(MessageFormat.format(Messages.WAYPOINT_NOT_FOUND.getMessage(l), toUpdate.getWaypoint().getUuid().toString()));

		if (attributes.containsKey(IJsonFeatureProcessor.JSON_OBSERVER_KEY)) {
			toUpdate.setObserver(observation.getObserver());
		}
		toUpdate.getAttributes().clear();
		session.flush();

		modifiedFeatures.add(pw.getPatrolLegDay().getPatrolLeg().getPatrol());

		if (attributes.containsKey("category")) { //$NON-NLS-1$
			toUpdate.setCategory(observation.getCategory());
			for (WaypointObservationAttribute a : observation.getAttributes()) {
				a.setObservation(toUpdate);
				toUpdate.getAttributes().add(a);
			}
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
		session.persist(toUpdate);
		session.flush();
	}

	private Patrol parsePatrol(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		Patrol newPatrol = new Patrol();
		
		newPatrol.setUuid(getSourcePatrolUuid(attributes, l));
		
		//configure date/time
		if (props.containsKey(JSON_DATETIME_KEY)) {
			LocalDateTime date = super.getDateTime(props);
			newPatrol.setStartDate(date.toLocalDate());
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.TEAM.getKey())) {
			String teamKey = (String)attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.TEAM.getKey());
			Team pTeam = session.createQuery("FROM Team WHERE conservationArea = :ca and keyId = :key", Team.class) //$NON-NLS-1$
					.setParameter("ca",ca ) //$NON-NLS-1$
					.setParameter("key", teamKey) //$NON-NLS-1$
					.uniqueResult();
			if (pTeam == null) {
				warnings.add(MessageFormat.format(Messages.TEAM_MISSING.getMessage(l), teamKey));
			}else {
				newPatrol.setTeam(pTeam);
			}
			
		}
		
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.STATION.getKey())) {
			String stationKey = (String)attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.STATION.getKey());
			try {
				UUID stationUuid = UuidUtils.stringToUuid(stationKey);
				Station station = session.get(Station.class, stationUuid);
				if (station == null) throw new Exception(MessageFormat.format(Messages.STATION_MISSING.getMessage(l), stationKey));
				if (!station.getConservationArea().equals(ca)) throw new Exception(MessageFormat.format(Messages.STATION_MISSING.getMessage(l), stationKey));
				newPatrol.setStation(station);
			}catch (Exception ex) {
				warnings.add(MessageFormat.format(Messages.STATION_MISSING.getMessage(l), stationKey));
			}
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.COMMENT.getKey())) {
			String comment = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.COMMENT.getKey());
			newPatrol.setComment(comment);
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.OBJECTIVE.getKey())) {
			String objective = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.OBJECTIVE.getKey());
			newPatrol.setObjective(objective);
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.PATROLID.getKey())) {
			String id = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.PATROLID.getKey());
			newPatrol.setId(id.trim());
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.ARMED.getKey())) {
			Object x = attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.ARMED.getKey());
			if (x instanceof Boolean) {
				newPatrol.setArmed(((Boolean)x));
			}else {
				if (x.toString().equalsIgnoreCase(Boolean.TRUE.toString())) {
					newPatrol.setArmed(true);
				}else {
					newPatrol.setArmed(false);
				}
			}
		}
		
		//custom patrol attributes
		List<PatrolAttribute> customAttributes = session.createQuery("FROM PatrolAttribute WHERE conservationArea = :ca", PatrolAttribute.class) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.list();
		
		newPatrol.setCustomAttributes(new ArrayList<>());
		
		for (PatrolAttribute custom : customAttributes) {
			String key = custom.getKeyId();
			if (!attributes.containsKey(key)) continue;
			
			PatrolAttributeValue pvalue = new PatrolAttributeValue();
			pvalue.setPatrol(newPatrol);
			pvalue.setPatrolAttribute(custom);
			Object jsonValue = attributes.get(key);
			
			try {
				Object value = null;
				if (custom.getType() == AttributeType.BOOLEAN) {
					value = parseBoolean(jsonValue);
				}else if (custom.getType() == AttributeType.DATE) {
					value = parseDate(jsonValue);
				}else if (custom.getType() == AttributeType.TEXT) {
					value = jsonValue.toString();
				}else if (custom.getType() == AttributeType.LIST) {
					String itemkey = jsonValue.toString();
					
					for (PatrolAttributeListItem item : custom.getAttributeList()) {
						if (item.getKeyId().equalsIgnoreCase(itemkey)) {
							value = item;
							break;
						}
					}
					if (value == null) {
						throw new Exception(MessageFormat.format("List item with key {0} not found for custom patrol attribute {1}.", key, custom.getName())); //$NON-NLS-1$
					}
					
				}else if (custom.getType() == AttributeType.NUMERIC) {
					value = parseNumeric(jsonValue);
				}
				pvalue.setAttributeValue(value);	
			}catch (Exception ex) {
				warnings.add(MessageFormat.format(Messages.CUSTOM_ATTRIBUTE_ERROR.getMessage(l), custom.getName(), jsonValue));
				continue;
			}
			
			newPatrol.getCustomAttributes().add(pvalue);
		}
		
		return newPatrol;
	}
	
	private PatrolLeg parsePatrolLeg(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		PatrolLeg newPatrolLeg = new PatrolLeg();
		newPatrolLeg.setUuid(getSourcePatrolLegUuid(attributes, l));

		//configure date/time
		if (props.containsKey(JSON_DATETIME_KEY)) {
			LocalDateTime date = super.getDateTime(props);
			newPatrolLeg.setStartDate(date.toLocalDate());
		}
		if (attributes.containsKey(JSON_LEGID_KEY)) {
			newPatrolLeg.setId(attributes.get(JSON_LEGID_KEY).toString());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.MANDATE.getKey())) {
			newPatrolLeg.setMandate(findPatrolMandate(ca, session,attributes, l));
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.TRANSPORT_TYPE.getKey())) {
			newPatrolLeg.setType(findTransportType(ca, session, attributes, l));
		}
		
		//members leader & pilot
		newPatrolLeg.setMembers(new ArrayList<>());
		
		
		String leader = null;
		String pilot = null;
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.LEADER.getKey())) {
			leader = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.LEADER.getKey());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.PILOT.getKey())) {
			pilot = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.PILOT.getKey());
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.EMPLOYEES.getKey())) {
			JSONArray employees = (JSONArray) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.EMPLOYEES.getKey());
			
			for (int i = 0; i < employees.size(); i ++) {
				String euuid = (String) employees.get(i);
				
				UUID e = UuidUtils.stringToUuid(euuid);
				Employee employee = session.get(Employee.class, e);
				if (employee == null || !employee.getConservationArea().equals(ca)) {
					warnings.add(MessageFormat.format(Messages.EMPLOYEE_NOT_FOUND.getMessage(l), euuid));
				}else {
					PatrolLegMember member = new PatrolLegMember();
					member.setPatrolLeg(newPatrolLeg);
					member.setMember(employee);
					if (euuid.equalsIgnoreCase(leader)) {
						member.setIsLeader(true);
					}
					if (euuid.equalsIgnoreCase(pilot)) {
						member.setIsPilot(true);
					}
					newPatrolLeg.getMembers().add(member);
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
					PatrolLegMember member = new PatrolLegMember();
					member.setPatrolLeg(newPatrolLeg);
					member.setMember(employee);
					member.setIsLeader(true);
					if (leader.equals(pilot)) member.setIsPilot(true);
					newPatrolLeg.getMembers().add(member);
				}
			}
			if (pilot != null && !pilot.equals(leader)) {
				UUID e = UuidUtils.stringToUuid(pilot);
				Employee employee = session.get(Employee.class, e);
				if (employee == null || !employee.getConservationArea().equals(ca)) {
					warnings.add(MessageFormat.format(Messages.EMPLOYEE_NOT_FOUND.getMessage(l), pilot));
				}else {
					PatrolLegMember member = new PatrolLegMember();
					member.setPatrolLeg(newPatrolLeg);
					member.setMember(employee);
					 member.setIsPilot(true);
					newPatrolLeg.getMembers().add(member);
				}
			}
		}
		return newPatrolLeg;
	}
	
	private void processPatrolUpdate(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{

		Patrol updatedPatrol = parsePatrol(feature, ca, session, l);
		
		//find the patrol
		if (updatedPatrol.getUuid() == null) {
			//not found
			throw new Exception(MessageFormat.format(Messages.PATROL_LINK_MISSING.getMessage(l), "null")); //$NON-NLS-1$
		}
		
		Patrol toUpdate = findPatrolLink(updatedPatrol.getUuid(), ca, session, l);
		if (toUpdate == null) {
			throw new Exception(MessageFormat.format(Messages.PATROL_LINK_MISSING.getMessage(l), updatedPatrol.getUuid().toString()));
		}
		
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		

		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.ARMED.getKey())) {
			toUpdate.setArmed(updatedPatrol.isArmed());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.COMMENT.getKey())) {
			toUpdate.setComment(updatedPatrol.getComment());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.PATROLID.getKey())) {
			toUpdate.setId(updatedPatrol.getId());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.OBJECTIVE.getKey())) {
			toUpdate.setObjective(updatedPatrol.getObjective());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.STATION.getKey())) {
			toUpdate.setStation(updatedPatrol.getStation());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.TEAM.getKey())) {
			toUpdate.setTeam(updatedPatrol.getTeam());
		}
	
		if (toUpdate.getCustomAttributes() == null) toUpdate.setCustomAttributes(new ArrayList<>());
		if (updatedPatrol.getCustomAttributes() != null) {
			for (PatrolAttributeValue value : updatedPatrol.getCustomAttributes()) {
				PatrolAttributeValue current = null;
				
				for (PatrolAttributeValue cv : toUpdate.getCustomAttributes()) {
					if (cv.getPatrolAttribute().equals(value.getPatrolAttribute())) {
						current = cv;
						break;
					}
				}
				if (current == null) {
					current = new PatrolAttributeValue();
					current.setPatrol(toUpdate);
					toUpdate.getCustomAttributes().add(current);
				}
				current.setAttributeValue(value.getAttributeValue());
			}
		}
		session.persist(toUpdate);
		session.flush();
		
		modifiedFeatures.add(toUpdate);
	}
	
	private void processPatrolLegUpdate(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{

		PatrolLeg updatedPatrolLeg = parsePatrolLeg(feature, ca, session, l);
		
		//find the patrol
		if (updatedPatrolLeg.getUuid() == null) {
			//not found
			throw new Exception(MessageFormat.format(Messages.PATROLLEG_LINK_MISSING.getMessage(l), "null")); //$NON-NLS-1$
		}
		
		PatrolLeg toUpdate = findPatrolLegLink(updatedPatrolLeg.getUuid(), ca, session, l);
		if (toUpdate == null) {
			throw new Exception(MessageFormat.format(Messages.PATROLLEG_LINK_MISSING.getMessage(l), updatedPatrolLeg.getUuid().toString()));
		}
				
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		if (attributes.containsKey(JSON_LEGID_KEY)) {
			toUpdate.setId(updatedPatrolLeg.getId());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.MANDATE.getKey())) {
			toUpdate.setMandate(updatedPatrolLeg.getMandate());
		}
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.TRANSPORT_TYPE.getKey())) {
			toUpdate.setType(updatedPatrolLeg.getType());
		}
		
		toUpdate.getPatrol().recalculateType();
		
		//
		//if employees is provided by not leader/pilot then use existing leader/pilot
		//if leader/pilot is provided by not employees then try to set based on current employees
		
		UUID leaderUuid = null;
		UUID pilotUuid = null;
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.LEADER.getKey())) {
			leaderUuid = updatedPatrolLeg.getLeader().getMember().getUuid();
		}else {
			leaderUuid = toUpdate.getLeader().getMember().getUuid();
		}

		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.PILOT.getKey())) {
			pilotUuid = updatedPatrolLeg.getPilot().getMember().getUuid();
		}else {
			if (toUpdate.getPilot() != null) pilotUuid = toUpdate.getPilot().getMember().getUuid();
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.EMPLOYEES.getKey())) {
			Set<Employee> required = updatedPatrolLeg.getMembers().stream().map(e->e.getMember()).collect(Collectors.toSet());
			
			List<PatrolLegMember> toRemove = new ArrayList<>();
			for (PatrolLegMember m : toUpdate.getMembers()) {
				if (!required.contains(m.getMember())) toRemove.add(m);
			}
			toUpdate.getMembers().removeAll(toRemove);
			Set<Employee> current = toUpdate.getMembers().stream().map(e->e.getMember()).collect(Collectors.toSet());
			
			for (Employee e : required) {
				if (!current.contains(e)) {
					PatrolLegMember m = new PatrolLegMember();
					m.setMember(e);
					m.setPatrolLeg(toUpdate);
					toUpdate.getMembers().add(m);
				}
			}
		}
		
		if (toUpdate.getMembers().isEmpty()) {
			throw new Exception(Messages.NO_EMPLOYEES.getMessage(l));
		}
		
		boolean hasleader = false;
		boolean haspilot = false;
		for (PatrolLegMember m : toUpdate.getMembers()) {
			if (m.getMember().getUuid().equals(leaderUuid)) {
				m.setIsLeader(true);
				hasleader = true;
			}else {
				m.setIsLeader(false);
			}
			if (toUpdate.getType().getPatrolType().requiresPilot() && m.getMember().getUuid().equals(pilotUuid)) {
				m.setIsPilot(true);
				haspilot = true;
			}else {
				m.setIsPilot(false);
			}
		}
		
		if (!hasleader) {
			throw new Exception(Messages.NO_LEADER.getMessage(l));
		}
		if (toUpdate.getType().getPatrolType().requiresPilot() && !haspilot) {
			throw new Exception(MessageFormat.format(Messages.NO_PILOT.getMessage(l), toUpdate.getType().getName()));
		}
	
		session.persist(toUpdate);
		session.flush();
		
		modifiedFeatures.add(toUpdate.getPatrol());

	}
	
	private void processStartPatrol(JSONObject feature, ConservationArea ca,  Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		
		String[] required = new String[] {
				JSON_PATROLUUID, JSON_PATROLLEGUUID, PatrolAttributeMetadata.FixedPatrolMetadata.TRANSPORT_TYPE.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.MANDATE.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.EMPLOYEES.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.LEADER.getKey()
		};
		for (String r : required) {
			if (!attributes.containsKey(r)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), r));
		}
		
		//configure date/time

		Patrol newPatrol = parsePatrol(feature, ca, session, l);
		PatrolLeg newPatrolLeg = parsePatrolLeg(feature, ca, session, l);
		
		LocalDateTime date = super.getDateTime(props);
		newPatrol.setEndDate(date.toLocalDate());
		newPatrol.setConservationArea(ca);
		
		newPatrolLeg.setStartDate(newPatrol.getStartDate());
		newPatrolLeg.setEndDate(newPatrol.getEndDate());
		
		newPatrol.setLegs(new ArrayList<>());
		newPatrol.getLegs().add(newPatrolLeg);
		newPatrolLeg.setPatrol(newPatrol);
		if (newPatrolLeg.getId() == null) newPatrolLeg.setId("1"); //$NON-NLS-1$
		
		createLegDays(newPatrolLeg, session);
		newPatrolLeg.getPatrolLegDays().get(0).setStartTime(date.toLocalTime());
		newPatrolLeg.getPatrolLegDays().get(0).setEndTime(date.toLocalTime());
		
		UUID srcPatrolUuid = newPatrol.getUuid();
		newPatrol.setUuid(null);

		newPatrol.recalculateType();
		
		Patrol temp = findPatrolLink(srcPatrolUuid, ca, session, l);
		if (temp != null) {
			//a link to a patrol already exists in the database for this uuid; 
			throw new Exception(MessageFormat.format(Messages.PATROL_EXISTS.getMessage(l), srcPatrolUuid));
		}
		
		UUID srcLegUuid = newPatrolLeg.getUuid();
		newPatrolLeg.setUuid(null);
		PatrolLeg temp2 = findPatrolLegLink(srcLegUuid, ca, session, l);
		if (temp2 != null) {
			//a link to a patrol leg already exists in the database for this uuid; 
			throw new Exception(MessageFormat.format(Messages.PATROLLEG_EXISTS.getMessage(l), srcLegUuid));
		}
		
		boolean hasleader = false;
		boolean haspilot = false;
		for (PatrolLegMember member : newPatrolLeg.getMembers()) {
			if (member.getIsLeader()) hasleader = true;
			if (member.getIsPilot()) haspilot = true;
		}

		if (newPatrolLeg.getMembers().isEmpty()) {
			throw new Exception(Messages.NO_EMPLOYEES.getMessage(l));
		}
		if (!hasleader) {
			throw new Exception(Messages.NO_LEADER.getMessage(l));
		}
		if (newPatrolLeg.getType().getPatrolType().requiresPilot() && !haspilot) {
			throw new Exception(MessageFormat.format(Messages.NO_PILOT.getMessage(l), newPatrolLeg.getType().getName()));
		}

		//add a track point
		Coordinate position = super.getPosition(feature);
		PatrolLegDay pday = newPatrolLeg.getPatrolLegDays().get(0);
		
		addTrackPoint(pday, position);		
		
		if (newPatrol.getId() == null || newPatrol.getId().trim().isEmpty()) {
			newPatrol.setId(PatrolIdGenerator.INSTANCE.generatePatrolId(newPatrol, session));
		}
		session.persist(newPatrol);
		session.flush();
		modifiedFeatures.add(newPatrol);
		
		//create data links
		DataLink link = new DataLink();
		link.setConservationArea(ca);
		link.setProviderId(srcPatrolUuid);
		link.setDataType(PatrolLinkDataType.PATROL.getKey());
		link.setSmartId(newPatrol.getUuid());
		session.persist(link);
		
		link = new DataLink();
		link.setConservationArea(ca);
		link.setProviderId(srcLegUuid);
		link.setDataType(PatrolLinkDataType.LEG.getKey());
		link.setSmartId(newPatrolLeg.getUuid());
		session.persist(link);
	}
	
	private UUID getSourcePatrolUuid(JSONObject attributes, Locale l) throws Exception{
		if (!attributes.containsKey(JSON_PATROLUUID)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), JSON_PATROLUUID));
		try {
			return UuidUtils.stringToUuid((String)attributes.get(JSON_PATROLUUID));
		}catch (Exception ex) {
			throw new Exception(MessageFormat.format(Messages.INVALID_PATROL_UUID.getMessage(l), attributes.get(JSON_PATROLUUID)));
		}
	}
	private UUID getSourcePatrolLegUuid(JSONObject attributes, Locale l) throws Exception{
		if (!attributes.containsKey(JSON_PATROLLEGUUID)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), JSON_PATROLLEGUUID));
		try {
			return UuidUtils.stringToUuid((String)attributes.get(JSON_PATROLLEGUUID));
		}catch (Exception ex) {
			throw new Exception(MessageFormat.format(Messages.INVALID_PATROLLEG_UUID.getMessage(l), attributes.get(JSON_PATROLLEGUUID)));
		}
	}
	
	private void processEndPatrolLeg(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//configure date/time
		LocalDateTime date = super.getDateTime(props);
		Coordinate position = super.getPosition(feature);
		
		//find the patrol leg day with this date
		UUID srcPatrolLegUuid = getSourcePatrolLegUuid(attributes, l);
		PatrolLeg leg = findPatrolLegLink(srcPatrolLegUuid, ca, session, l);
		if (leg == null) throw new Exception(MessageFormat.format(Messages.PATROLLEG_LINK_MISSING.getMessage(l), srcPatrolLegUuid.toString()));

		Patrol patrol = leg.getPatrol();
		
		if (!leg.getEndDate().equals(date.toLocalDate())) {
			leg.setEndDate(date.toLocalDate());
			createLegDays(leg, session);
		}
		
		for (PatrolLegDay d : leg.getPatrolLegDays()) {
			if (d.getDate().equals(date.toLocalDate())) {
				if (d.getEndTime().equals(LocalTime.MAX) || date.toLocalTime().isAfter(d.getEndTime())) {
					d.setEndTime(date.toLocalTime());
				}
				addTrackPoint(d, position);
				break;
			}
		}
		
		//update patrol end date if required
		if (patrol.getEndDate().isBefore(date.toLocalDate())) {
			patrol.setEndDate(date.toLocalDate());
		}
		modifiedFeatures.add(patrol);
	}
	
	
	private void processTrackPoint(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//configure date/time
		LocalDateTime date = super.getDateTime(props);
		if (date == null) {
			throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), IJsonFeatureProcessor.JSON_DATETIME_KEY));
		}
		
		Coordinate position = super.getPosition(feature);
		if (position == null) {
			throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), "geometry")); //$NON-NLS-1$
		}
		
		UUID srcPatrolLegUuid = getSourcePatrolLegUuid(attributes, l);
		PatrolLeg leg = findPatrolLegLink(srcPatrolLegUuid, ca, session, l);
		if (leg == null) throw new Exception(MessageFormat.format(Messages.PATROLLEG_LINK_MISSING.getMessage(l), srcPatrolLegUuid.toString()));
		
		//update leg dates
		if (date.toLocalDate().isBefore(leg.getStartDate())) {
			leg.setStartDate(date.toLocalDate());
			createLegDays(leg, session);
		}
		
		if (date.toLocalDate().isAfter(leg.getEndDate())) {
			leg.setEndDate(date.toLocalDate());
			createLegDays(leg, session);
		}
		
		//update patrol dates
		if (date.toLocalDate().isBefore(leg.getPatrol().getStartDate())) {
			leg.getPatrol().setStartDate(date.toLocalDate());
		}
		if (date.toLocalDate().isAfter(leg.getPatrol().getEndDate())) {
			leg.getPatrol().setEndDate(date.toLocalDate());
		}
				
		PatrolLegDay toUpdate = null;
		for (PatrolLegDay day : leg.getPatrolLegDays()) {
			if (day.getDate().equals(date.toLocalDate())) {
				toUpdate = day;
				break;
			}
		}
		if (toUpdate == null) throw new Exception(Messages.PATROLLEG_MISSING.getMessage(l));

		if (toUpdate.getStartTime().equals(LocalTime.MIN) || date.toLocalTime().isBefore(toUpdate.getStartTime())) {
			toUpdate.setStartTime(date.toLocalTime());
		}
		if (toUpdate.getEndTime().equals(LocalTime.MAX) || toUpdate.getEndTime().isBefore(date.toLocalTime())) {
			toUpdate.setEndTime(date.toLocalTime());
		}
		modifiedFeatures.add(leg.getPatrol());
		addTrackPoint(toUpdate, position);
	}
	
	
	private void addTrackPoint(PatrolLegDay toUpdate, Coordinate position) throws Exception {
		
		if (toUpdate.getTrack() == null) {
			Track track = new Track();
			track.setPatrolLegDay(toUpdate);
			track.setLineStrings(new ArrayList<>());
			toUpdate.setTrack(track);
		}
		

		List<Coordinate> items = new ArrayList<>();
		List<LineString> trackls = new ArrayList<>();
		
		boolean exists = false;
		if (!toUpdate.getTrack().getLineStrings().isEmpty()) {
			trackls.addAll(toUpdate.getTrack().getLineStrings());
			
			LineString ls = toUpdate.getTrack().getLineStrings().get(toUpdate.getTrack().getLineStrings().size() - 1);
			trackls.remove(ls);
			
			for (Coordinate c : ls.getCoordinates()) {
				items.add(c);
				if (c.x == position.x && c.y == position.y && c.z == position.z) {
					exists = true;
				}
			}
		}
		//don't allow duplicates
		if (!exists) items.add(position);		
		if (items.size() == 1) items.add(position);
		
		trackls.add(TrackUtil.convertToLineString(items));
		toUpdate.getTrack().setLineStrings(trackls);
	}
	
	
	private void processNewLeg(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//configure date/time
		LocalDateTime date = super.getDateTime(props);
		
		String[] required = new String[] {
				JSON_PATROLUUID, JSON_PATROLLEGUUID, 
				PatrolAttributeMetadata.FixedPatrolMetadata.TRANSPORT_TYPE.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.MANDATE.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.EMPLOYEES.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.LEADER.getKey()
		};
		for (String r : required) {
			if (!attributes.containsKey(r)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), r));
		}
		
		UUID srcPatrolUuid = getSourcePatrolUuid(attributes, l);
		UUID srcLegUuid = getSourcePatrolLegUuid(attributes, l);
		
		Patrol patrolToUpdate = findPatrolLink(srcPatrolUuid, ca, session, l);
		if (patrolToUpdate == null) throw new Exception(MessageFormat.format(Messages.PATROL_LINK_MISSING.getMessage(l), srcPatrolUuid.toString()));
		
		
		PatrolLeg existing = findPatrolLegLink(srcLegUuid, ca, session, l);
		if (existing != null) throw new Exception(MessageFormat.format(Messages.PATROLLEG_EXISTS.getMessage(l), srcLegUuid));

		
		if (patrolToUpdate.getEndDate().isBefore(date.toLocalDate())) {
			patrolToUpdate.setEndDate(date.toLocalDate());
		}
		
		PatrolLeg newLeg = new PatrolLeg();
		newLeg.setStartDate(date.toLocalDate());
		newLeg.setEndDate(date.toLocalDate());
		newLeg.setId(String.valueOf((patrolToUpdate.getLegs().size() + 1)));
		
		createLegDays(newLeg, session);
		
		newLeg.getPatrolLegDays().get(0).setStartTime(date.toLocalTime());
		newLeg.getPatrolLegDays().get(0).setEndTime(date.toLocalTime());
		
		PatrolMandate pMandate = null;
		try {
			pMandate = findPatrolMandate(ca, session, attributes, l);
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.MANDATE_EXISTING.getMessage(l), ex.getMessage()));
			for (PatrolLeg leg : patrolToUpdate.getLegs()) {
				if (leg.getMandate() != null) {
					pMandate = leg.getMandate();
				}
			}
		}
		newLeg.setMandate(pMandate);
		
		newLeg.setType(findTransportType(ca, session, attributes, l));
		
		//members leader & pilot
		newLeg.setMembers(new ArrayList<>());
		JSONArray employees = (JSONArray) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.EMPLOYEES.getKey());
		
		String leader = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.LEADER.getKey());
		String pilot = null;
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.PILOT.getKey())) {
			pilot = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.PILOT.getKey());
		}
		boolean hasleader = false;
		boolean haspilot = false;
		for (int i = 0; i < employees.size(); i ++) {
			String euuid = (String) employees.get(i);
			
			UUID e = UuidUtils.stringToUuid(euuid);
			Employee employee = session.get(Employee.class, e);
			if (employee == null || !employee.getConservationArea().equals(ca)) {
				warnings.add(MessageFormat.format(Messages.EMPLOYEE_NOT_FOUND.getMessage(l), euuid));
			}else {
				PatrolLegMember member = new PatrolLegMember();
				member.setPatrolLeg(newLeg);
				member.setMember(employee);
				if (euuid.equalsIgnoreCase(leader)) {
					member.setIsLeader(true);
					hasleader = true;
				}
				if (euuid.equalsIgnoreCase(pilot)) {
					haspilot = true;
					member.setIsPilot(true);
				}
				newLeg.getMembers().add(member);
			}
		}
		if (newLeg.getMembers().isEmpty()) {
			throw new Exception(Messages.NO_EMPLOYEES.getMessage(l));
		}
		if (!hasleader) {
			throw new Exception(Messages.NO_LEADER.getMessage(l));
		}
		if (newLeg.getType().getPatrolType().requiresPilot() && !haspilot) {
			throw new Exception(MessageFormat.format(Messages.NO_LEADER.getMessage(l), newLeg.getType().getName()));
		}
		
		newLeg.setPatrol(patrolToUpdate);
		if (patrolToUpdate.getLegs() == null) patrolToUpdate.setLegs(new ArrayList<>());
		patrolToUpdate.getLegs().add(newLeg);
		newLeg.getPatrol().recalculateType();

		//expand to ensure at least one leg per day
		PatrolUtils.createLegDaysForMissingDays(patrolToUpdate);
		
		session.flush();
		modifiedFeatures.add(patrolToUpdate);
		
		//add a track point
		Coordinate position = super.getPosition(feature);
		PatrolLegDay pday = newLeg.getPatrolLegDays().get(0);
		addTrackPoint(pday, position);		
		
		DataLink legLink = new DataLink();
		legLink.setConservationArea(ca);
		legLink.setDataType(PatrolLinkDataType.LEG.getKey());
		legLink.setProviderId(srcLegUuid);
		legLink.setSmartId(newLeg.getUuid());
		session.persist(legLink);
	}

	/**
	 * Finds the patrol transport type or throws a new exception if type
	 * cannot be found.
	 * 
	 * @param ca
	 * @param session
	 * @param attributes
	 * @return
	 * @throws Exception
	 */
	private PatrolTransportType findTransportType(ConservationArea ca, Session session, JSONObject attributes, Locale l)
			throws Exception {
		String typeKey = (String)attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.TRANSPORT_TYPE.getKey());
		PatrolTransportType tType = session.createQuery("FROM PatrolTransportType WHERE conservationArea = :ca and keyId = :key", PatrolTransportType.class) //$NON-NLS-1$
				.setParameter("ca",ca ) //$NON-NLS-1$
				.setParameter("key", typeKey) //$NON-NLS-1$
				.uniqueResult();
		if (tType == null) {
			//required
			throw new Exception(MessageFormat.format(Messages.TRANSPORTTYPE_MISSING.getMessage(l), typeKey));
		}
		return tType;
	}
	
	/**
	 * finds the patrol mandate or throws an exception if not found
	 * @param ca
	 * @param session
	 * @param attributes
	 * @return
	 * @throws Exception
	 */
	private PatrolMandate findPatrolMandate(ConservationArea ca, Session session, JSONObject attributes, Locale l) throws Exception{
		String mandateKey = (String)attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.MANDATE.getKey());
		PatrolMandate pMandate = session.createQuery("FROM PatrolMandate WHERE conservationArea = :ca and keyId = :key", PatrolMandate.class) //$NON-NLS-1$
				.setParameter("ca",ca ) //$NON-NLS-1$
				.setParameter("key", mandateKey) //$NON-NLS-1$
				.uniqueResult();
		if (pMandate == null) {
			//required
			throw new Exception(MessageFormat.format(Messages.MANDATE_MISSING.getMessage(l), mandateKey));
		}
		return pMandate;
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
	
	/**
	 * Find the patrol linked with to the provided uuid.  Will return null if no patrol found.
	 * @param providerUuid
	 * @param ca
	 * @param session
	 * @param l
	 * @return
	 * @throws Exception
	 */
	private Patrol findPatrolLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
		.setParameter("ca",ca) //$NON-NLS-1$
		.setParameter("puuid", providerUuid) //$NON-NLS-1$
		.setParameter("datatype", PatrolLinkDataType.PATROL.getKey()) //$NON-NLS-1$
		.uniqueResult();
		
		if (link == null) return null;
		
		Patrol p = session.get(Patrol.class, link.getSmartId());
		if (p == null) {
			//the object this links to does exist, so lets delete it and allow a new one
			session.remove(link);
			return null;
		}
		if (!p.getConservationArea().equals(ca)) {
			throw new Exception(Messages.PATROL_LINK_EXISTS.getMessage(l));
		}
			
		
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return p;
	}
	

	/**
	 * Find the patrol leg linked to the uuid provided.  Will return null if link or leg not found.
	 * @param providerUuid
	 * @param ca
	 * @param session
	 * @param l
	 * @return
	 * @throws Exception
	 */
	private PatrolLeg findPatrolLegLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", PatrolLinkDataType.LEG.getKey()) //$NON-NLS-1$
			.uniqueResult();
		
		if (link == null) return null;
		
		PatrolLeg p = session.get(PatrolLeg.class, link.getSmartId());
		if (p == null) {
			session.remove(link);
			return null;
		}
		if (!p.getPatrol().getConservationArea().equals(ca)) {
			throw new Exception(Messages.PATROLLEG_LINK_EXISTS.getMessage(l));
		}
		
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return p;
	}	

	
	public void createLegDays(PatrolLeg leg, Session session){
		
		if (leg.getPatrolLegDays() == null) leg.setPatrolLegDays(new ArrayList<>());
		
		List<PatrolLegDay> days = leg.getPatrolLegDays();
		
		//lets make a hash set of existing leg days; 
		//we try to re-use these so associated data is not lost
		HashMap<LocalDate, PatrolLegDay> current = new HashMap<LocalDate, PatrolLegDay>();
		for (PatrolLegDay day : days){
			current.put(day.getDate(), day);
		}

		// -- the remaining days
		LocalDate working = leg.getStartDate();
		while (working.isBefore(leg.getEndDate()) || working.isEqual(leg.getEndDate()) ){
			
			PatrolLegDay existing = current.remove(working);
			if (existing != null){
				if (existing.getStartTime() == null) existing.setStartTime(LocalTime.MIN);
				if (existing.getEndTime() == null) existing.setEndTime(LocalTime.MAX);
			}else{
				PatrolLegDay previousDay = new PatrolLegDay();
				previousDay.setDate( working );
				previousDay.setStartTime( LocalTime.MIN );
				previousDay.setEndTime( LocalTime.MAX );
				previousDay.setPatrolLeg(leg);
				days.add(previousDay);
				
			}
			working = ChronoUnit.DAYS.addTo(working, 1);
		}
	
		//remove old legs that weren't used
		for (PatrolLegDay day : current.values()){
			//we need to make sure we delete all waypoints here
			if (day.getWaypoints() != null){
				for (PatrolWaypoint pw : day.getWaypoints()){
					session.remove(pw.getWaypoint());
				}
			}
			days.remove(day);
		}
		
		//sort 
		Collections.sort(days, new Comparator<PatrolLegDay>() {
			@Override
			public int compare(PatrolLegDay o1, PatrolLegDay o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});
	}
	
	/**
	 * Finds the waypoint that is linked to the uuid provided.  Will return null if not found. Will thrown an exception
	 * if linked to a different conservation area then currently being processed.
	 * @param providerUuid
	 * @param ca
	 * @param session
	 * @param l
	 * @return
	 * @throws Exception
	 */
	private Waypoint findIncidentLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", PatrolLinkDataType.INCIDENT.getKey()) //$NON-NLS-1$
			.uniqueResult();
		
		if (link == null) return null;
		
		Waypoint waypoint = session.get(Waypoint.class, link.getSmartId());
		if (waypoint == null) {
			session.remove(link);
			return null;
		}
		if (!waypoint.getConservationArea().equals(ca)) {
			throw new Exception("Link Conservation Area doesn't match waypoint Conservation Area"); //$NON-NLS-1$
		}
		
		if (!waypoint.getSourceId().equals(PatrolWaypointSource.PATROL_WP_SOURCE_ID)) {
			throw new Exception("Link is not a patrol incident"); //$NON-NLS-1$
		}
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return waypoint;
	}	
	
	private void computeAttachmentLocation(Waypoint wp, Patrol p, Session session) throws Exception {
		for(WaypointAttachment wa : wp.getAttachments()) {
			if (wa.getUuid() == null) {
				wa.computeFileLocation(Paths.get(p.getConservationArea().getFileDataStoreLocation())
						.resolve(PATROL_WP_SRC.getDatastoreFileLocation(p, session))
						.resolve(wa.getFilename()));
			}
		}
		for (WaypointObservation wo : wp.getAllObservations()) {
			for(ObservationAttachment oa : wo.getAttachments()) {
				if (oa.getUuid() == null) {
					oa.computeFileLocation(Paths.get(p.getConservationArea().getFileDataStoreLocation())
							.resolve(PATROL_WP_SRC.getDatastoreFileLocation(p, session))
							.resolve(oa.getFilename()));
				}
			}
		}
	}
	
	
}
