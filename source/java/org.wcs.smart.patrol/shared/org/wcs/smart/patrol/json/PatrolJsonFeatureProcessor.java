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
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
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
import org.wcs.smart.util.TrackUtil;
import org.wcs.smart.util.UuidUtils;

/**
 * Processes patrol JSON features, creating or updating patrols.
 * 
 * @author Emily
 *
 */
public class PatrolJsonFeatureProcessor extends IJsonFeatureProcessor {

	private static final String JSON_PATROLLEGUUID = "patrolLegUuid"; //$NON-NLS-1$
	private static final String JSON_PATROLUUID = "patrolUuid"; //$NON-NLS-1$
	
	private static final String PATROL_DATATYPE = "patrol"; //$NON-NLS-1$
	private static final String INCIDENT_DATATYPE = "patrolincident"; //$NON-NLS-1$
	private static final String LEG_DATATYPE = "patrolleg"; //$NON-NLS-1$
	
	public static final String JSON_FT_START = "start"; //$NON-NLS-1$
	public static final String JSON_FT_END = "end"; //$NON-NLS-1$
	public static final String JSON_FT_NEWLEG = "newleg"; //$NON-NLS-1$
	public static final String JSON_FT_TRACKPOINT = "trackpoint"; //$NON-NLS-1$
	
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
		COMPLETE_MSG;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getLabel(this, l);
		}
	}
	
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
		if (ftype.equalsIgnoreCase(JSON_FT_OBSERVATION)) {
			processObservation(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(JSON_FT_START)) {
			processStartPatrol(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(JSON_FT_END)) {
			processEndPatrolLeg(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(JSON_FT_NEWLEG)) {
			processNewLeg(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(JSON_FT_TRACKPOINT)) {
			processTrackPoint(feature, ca, session, l);
		}else {
			throw new Exception(MessageFormat.format(Messages.INVALID_FEATURE_TYPE.getMessage(l), ftype, JSON_FT_OBSERVATION));
		}

	}
	
	private void processObservation(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		LocalDateTime date = super.getDateTime(props);
		
		UUID srcPatrolLegUuid = getSourcePatrolLegUuid(attributes, l);
		PatrolLeg leg = findPatrolLegLink(srcPatrolLegUuid, ca, session, l);
		if (leg == null) throw new Exception(MessageFormat.format(Messages.PATROLLEG_LINK_MISSING.getMessage(l), srcPatrolLegUuid.toString()));
		modifiedFeatures.add(leg.getPatrol());
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
		
		session.saveOrUpdate(leg.getPatrol());
		session.flush();
		
		Waypoint wp = super.createWaypoint(feature, ca, session, l);
		wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
		if (wp.getId() == null) {
			wp.setId(String.valueOf(toUpdate.getWaypoints().size() + 1));
		}
		
		Waypoint existingWp = findIncidentLink(wp.getUuid(), ca, session, l);
		HashMap<WaypointObservationGroup, UUID> links = new HashMap<>();

		if (existingWp == null) {
			UUID src = wp.getUuid();
			wp.setUuid(null);
			//create a new waypoint & associated links
			for (WaypointObservationGroup g : wp.getObservationGroups()) {
				if (g.getUuid() != null) {
					//clear any old link
					session.createQuery("DELETE From DataLink WHERE providerId = :uuid") //$NON-NLS-1$
						.setParameter("uuid", g.getUuid()) //$NON-NLS-1$
						.executeUpdate();
					links.put(g,g.getUuid());
					g.setUuid(null);
					
				}
			}

			PatrolWaypoint pwp = new PatrolWaypoint();
			pwp.setWaypoint(wp);
			pwp.setPatrolLegDay(toUpdate);
			toUpdate.getWaypoints().add(pwp);
			session.saveOrUpdate(wp);
			session.saveOrUpdate(pwp);
			
			session.flush();
			
			if (src != null) {
				DataLink dlink = new DataLink();
				dlink.setConservationArea(ca);
				dlink.setProviderId(src);
				dlink.setSmartId(wp.getUuid());
				dlink.setDataType(INCIDENT_DATATYPE);
				session.save(dlink);
			}
			
			
			
		}else {
			//merge observation groups with existing wp
			
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
			dlink.setDataType(OBSGROUP_DATATYPE);
			session.save(dlink);
		}
		

		if (toUpdate.getStartTime().equals(LocalTime.MIN) || date.toLocalTime().isBefore(toUpdate.getStartTime())) {
			toUpdate.setStartTime(date.toLocalTime());
		}
		
		addTrackPoint(toUpdate, getPosition(feature));
		
	}
	
	private void processStartPatrol(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//configure date/time
		LocalDateTime date = super.getDateTime(props);
		Patrol newPatrol = new Patrol();
		newPatrol.setStartDate(date.toLocalDate());
		newPatrol.setEndDate(date.toLocalDate());
		newPatrol.setConservationArea(ca);
		modifiedFeatures.add(newPatrol);
		PatrolLeg newLeg = newPatrol.addLeg();
		
		createLegDays(newLeg, session);
		newLeg.getPatrolLegDays().get(0).setStartTime(date.toLocalTime());
		newLeg.getPatrolLegDays().get(0).setEndTime(date.toLocalTime());

		String[] required = new String[] {
				JSON_PATROLUUID, JSON_PATROLLEGUUID, PatrolAttributeMetadata.FixedPatrolMetadata.TRANSPORT_TYPE.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.MANDATE.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.EMPLOYEES.getKey(),
				PatrolAttributeMetadata.FixedPatrolMetadata.LEADER.getKey()
		};
		for (String r : required) {
			if (!attributes.containsKey(r)) throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), r));
		}
		
		UUID srcPatrolUuid = null;
		try {
			srcPatrolUuid = UuidUtils.stringToUuid((String)attributes.get(JSON_PATROLUUID));
		}catch (Exception ex) {
			throw new Exception(MessageFormat.format(Messages.INVALID_PATROL_UUID.getMessage(l), props.get(JSON_PATROLUUID)));
		}

		Patrol temp = findPatrolLink(srcPatrolUuid, ca, session, l);
		if (temp != null) {
			//a link to a patrol already exists in the database for this uuid; 
			throw new Exception(MessageFormat.format(Messages.PATROL_EXISTS.getMessage(l), srcPatrolUuid));
		}
		
		UUID srcLegUuid = getSourcePatrolLegUuid(attributes, l);
		PatrolLeg temp2 = findPatrolLegLink(srcLegUuid, ca, session, l);
		if (temp2 != null) {
			//a link to a patrol leg already exists in the database for this uuid; 
			throw new Exception(MessageFormat.format(Messages.PATROLLEG_EXISTS.getMessage(l), srcLegUuid));
		}
		
		newPatrol.setArmed(false);
		
		String teamKey = null;
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.TEAM.getKey())) {
			teamKey = (String)attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.TEAM.getKey());
			
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
		
		String stationKey = null;
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.STATION.getKey())) {
			stationKey = (String)attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.STATION.getKey());
			
			try {
				UUID stationUuid = UuidUtils.stringToUuid(stationKey);
				Station station = session.get(Station.class, stationUuid);
				if (station == null) throw new Exception();
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
			String comment = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.OBJECTIVE.getKey());
			newPatrol.setObjective(comment);
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.PATROLID.getKey())) {
			String id = (String) attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.PATROLID.getKey());
			newPatrol.setId(id.trim());
		}
		
		if (attributes.containsKey(PatrolAttributeMetadata.FixedPatrolMetadata.ARMED.getKey())) {
			String armed = (String)attributes.get(PatrolAttributeMetadata.FixedPatrolMetadata.ARMED.getKey());
			if (armed.equalsIgnoreCase(Boolean.TRUE.toString())) {
				newPatrol.setArmed(true);
			}
		}
		
		newLeg.setMandate(findPatrolMandate(ca, session,attributes, l));
		
		newLeg.setType(findTransportType(ca, session, attributes, l));
		newPatrol.recalculateType();
		
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
					hasleader = true;
					member.setIsLeader(true);
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
			throw new Exception(MessageFormat.format(Messages.NO_PILOT.getMessage(l), newLeg.getType().getName()));
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
		
		//add a track point
		Coordinate position = super.getPosition(feature);
		PatrolLegDay pday = newLeg.getPatrolLegDays().get(0);
		
		addTrackPoint(pday, position);		
		
		
		if (newPatrol.getId() == null || newPatrol.getId().trim().isEmpty()) {
			newPatrol.setId(PatrolIdGenerator.INSTANCE.generatePatrolId(newPatrol, session));
		}
		session.save(newPatrol);
		session.flush();
		
		//create data links
		DataLink link = new DataLink();
		link.setConservationArea(ca);
		link.setProviderId(srcPatrolUuid);
		link.setDataType(PATROL_DATATYPE);
		link.setSmartId(newPatrol.getUuid());
		session.save(link);
		
		link = new DataLink();
		link.setConservationArea(ca);
		link.setProviderId(srcLegUuid);
		link.setDataType(LEG_DATATYPE);
		link.setSmartId(newLeg.getUuid());
		session.save(link);
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
		modifiedFeatures.add(patrol);
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
	}
	
	
	private void processTrackPoint(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception{
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);
		JSONObject attributes = (JSONObject) props.get(IJsonFeatureProcessor.JSON_SMARTATTRIBUTES);
		
		//configure date/time
		LocalDateTime date = super.getDateTime(props);
		
		UUID srcPatrolLegUuid = getSourcePatrolLegUuid(attributes, l);
		PatrolLeg leg = findPatrolLegLink(srcPatrolLegUuid, ca, session, l);
		if (leg == null) throw new Exception(MessageFormat.format(Messages.PATROLLEG_LINK_MISSING.getMessage(l), srcPatrolLegUuid.toString()));
		modifiedFeatures.add(leg.getPatrol());
		Coordinate position = super.getPosition(feature);

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
		if (!toUpdate.getTrack().getLineStrings().isEmpty()) {
			trackls.addAll(toUpdate.getTrack().getLineStrings());
			
			LineString ls = toUpdate.getTrack().getLineStrings().get(toUpdate.getTrack().getLineStrings().size() - 1);
			trackls.remove(ls);
			for (Coordinate c : ls.getCoordinates()) {
				items.add(c);
			}
		}
		items.add(position);		
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
		modifiedFeatures.add(patrolToUpdate);
		
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
		
		//add a track point
		Coordinate position = super.getPosition(feature);
		PatrolLegDay pday = newLeg.getPatrolLegDays().get(0);
		addTrackPoint(pday, position);		
		
		DataLink legLink = new DataLink();
		legLink.setConservationArea(ca);
		legLink.setDataType(LEG_DATATYPE);
		legLink.setProviderId(srcLegUuid);
		legLink.setSmartId(newLeg.getUuid());
		session.save(legLink);
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
	
	
	private Patrol findPatrolLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
		.setParameter("ca",ca) //$NON-NLS-1$
		.setParameter("puuid", providerUuid) //$NON-NLS-1$
		.setParameter("datatype", PATROL_DATATYPE) //$NON-NLS-1$
		.uniqueResult();
		
		if (link == null) return null;
		
		Patrol p = session.get(Patrol.class, link.getSmartId());
		if (p == null) {
			//the object this links to does exist, so lets delete it and allow a new one
			session.delete(link);
			return null;
		}
		if (!p.getConservationArea().equals(ca)) {
			throw new Exception(Messages.PATROL_LINK_EXISTS.getMessage(l));
		}
			
		
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return p;
	}
	

	private PatrolLeg findPatrolLegLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", LEG_DATATYPE) //$NON-NLS-1$
			.uniqueResult();
		
		if (link == null) return null;
		
		PatrolLeg p = session.get(PatrolLeg.class, link.getSmartId());
		if (p == null) {
			session.delete(link);
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
					session.delete(pw.getWaypoint());
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
	
	private Waypoint findIncidentLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", INCIDENT_DATATYPE) //$NON-NLS-1$
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
		
		if (!waypoint.getSourceId().equals(PatrolWaypointSource.PATROL_WP_SOURCE_ID)) {
			throw new Exception("Link is not a patrol incident"); //$NON-NLS-1$
		}
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return waypoint;
	}	
}
