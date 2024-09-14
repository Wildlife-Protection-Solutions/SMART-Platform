/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.custom;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Station;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.i18n.labels.SmartLabelProvider;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.patrol.json.PatrolJsonFeatureProcessor.PatrolLinkDataType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * For custom api for querying patrols
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class CustomPatrolQueryEngine extends CustomQueryEngine {
	
	public List<PatrolLeg> getPatrolLegByUuid(Session session, String patrolLegUuid, Set<UUID> conservationAreas) {
		UUID puuid = UuidUtils.stringToUuid(patrolLegUuid);
		return getPatrolLegByPatrolUuid(session, puuid, conservationAreas);
	}

	public List<PatrolLeg> getPatrolLegByPatrolUuid(Session session, UUID patrolLegUuid, Set<UUID> conservationAreas) {
		PatrolLeg p = session.get(PatrolLeg.class, patrolLegUuid);
		if (p == null || !conservationAreas.contains(p.getPatrol().getConservationArea().getUuid()))
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No patrol leg with uuid {0}", patrolLegUuid)); //$NON-NLS-1$
		return Collections.singletonList(p);
	}
	
	public List<PatrolLeg> getPatrolLegByClientUuid(Session session, String clientPatrolLegUuid, Set<UUID> conservationAreas) {

		UUID clientuuid = UuidUtils.stringToUuid(clientPatrolLegUuid);
		DataLink link = session
				.createQuery("FROM DataLink WHERE providerId = :clientUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("clientUuid", clientuuid)  //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.LEG.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link == null)
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No patrol leg found with link to client id {0}", clientPatrolLegUuid)); //$NON-NLS-1$

		return getPatrolLegByPatrolUuid(session, link.getSmartId(), conservationAreas);
	}
	
	public List<Patrol> getPatrolsByPatrolUuid(Session session, String patrolUuid, Set<UUID> conservationAreas) {
		UUID puuid = UuidUtils.stringToUuid(patrolUuid);
		return getPatrolsByPatrolUuid(session, puuid, conservationAreas);
	}

	public List<Patrol> getPatrolsByPatrolUuid(Session session, UUID patrolUuid, Set<UUID> conservationAreas) {
		Patrol p = session.get(Patrol.class, patrolUuid);
		if (p == null || !conservationAreas.contains(p.getConservationArea().getUuid()))
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No patrol with uuid {0}", patrolUuid)); //$NON-NLS-1$
		return Collections.singletonList(p);
	}
	
	public List<Patrol> getPatrolsByClientUuid(Session session, String clientPatrolUuid, Set<UUID> conservationAreas) {

		UUID clientuuid = UuidUtils.stringToUuid(clientPatrolUuid);
		DataLink link = session
				.createQuery("FROM DataLink WHERE providerId = :clientUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("clientUuid", clientuuid)  //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.PATROL.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link == null)
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No patrol found with link to client id {0}", clientPatrolUuid)); //$NON-NLS-1$

		return getPatrolsByPatrolUuid(session, link.getSmartId(), conservationAreas);
	}
	
	public List<PatrolWaypoint> getWaypointsByPatrolUuid(Session session, String patrolUuid, Set<UUID> conservationAreas) {
		UUID puuid = UuidUtils.stringToUuid(patrolUuid);
		return getWaypointsByPatrolUuid(session, puuid, conservationAreas);
	}

	public List<PatrolWaypoint> getWaypointsByPatrolUuid(Session session, UUID patrolUuid, Set<UUID> conservationAreas) {
		Patrol p = session.get(Patrol.class, patrolUuid);
		if (p == null || !conservationAreas.contains(p.getConservationArea().getUuid()))
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No patrol with uuid {0}", patrolUuid)); //$NON-NLS-1$
		List<PatrolWaypoint> waypoints = new ArrayList<>();
		for (PatrolLeg leg : p.getLegs()) {
			for (PatrolLegDay pday : leg.getPatrolLegDays()) {
				for (PatrolWaypoint pw : pday.getWaypoints()) {
					waypoints.add(pw);
					if (waypoints.size() > MAX_RESULTS)
						throw TOO_MANY_ROWS;
				}
			}
		}
		return waypoints;
	}

	public List<PatrolWaypoint> getWaypointsByClientUuid(Session session, String clientPatrolUuid, Set<UUID> conservationAreas) {

		UUID clientuuid = UuidUtils.stringToUuid(clientPatrolUuid);
		DataLink link = session
				.createQuery("FROM DataLink WHERE providerId = :clientUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("clientUuid", clientuuid)  //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.PATROL.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link == null)
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No patrol found with link to client id {0}", clientPatrolUuid)); //$NON-NLS-1$

		return getWaypointsByPatrolUuid(session, link.getSmartId(), conservationAreas);
	}

	public List<PatrolWaypoint> getWaypointsByPatrolLegUuid(Session session, String leguuid, Set<UUID> conservationAreas) {
		UUID pleguuid = UuidUtils.stringToUuid(leguuid);
		return getWaypointsByPatrolLegUuid(session, pleguuid, conservationAreas);
	}

	public List<PatrolWaypoint> getWaypointsByPatrolLegUuid(Session session, UUID leguuid, Set<UUID> conservationAreas) {
		PatrolLeg p = session.get(PatrolLeg.class, leguuid);
		if (p == null || !conservationAreas.contains(p.getPatrol().getConservationArea().getUuid()) )
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No patrol leg with uuid {0}", leguuid)); //$NON-NLS-1$
		List<PatrolWaypoint> waypoints = new ArrayList<>();
		for (PatrolLegDay pday : p.getPatrolLegDays()) {
			for (PatrolWaypoint pw : pday.getWaypoints()) {
				waypoints.add(pw);
				if (waypoints.size() > MAX_RESULTS)
					throw TOO_MANY_ROWS;
			}
		}
		return waypoints;
	}

	public List<PatrolWaypoint> getWaypointsByClientLegUuid(Session session, String clientLegUuid, Set<UUID> conservationAreas) {

		UUID clientuuid = UuidUtils.stringToUuid(clientLegUuid);
		DataLink link = session
				.createQuery("FROM DataLink WHERE providerId = :clientUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("clientUuid", clientuuid) //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.LEG.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link == null)
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No patrol leg found with link to client id {0}", clientLegUuid)); //$NON-NLS-1$

		return getWaypointsByPatrolLegUuid(session, link.getSmartId(), conservationAreas);
	}

	
	public List<Patrol> getPatrolsById(Session session, String patrolId, Set<UUID> conservationAreas){
		return session.createQuery("FROM Patrol WHERE id = :id AND conservationArea.uuid IN (:uuids) ", Patrol.class) //$NON-NLS-1$
				.setParameterList("uuids", conservationAreas) //$NON-NLS-1$
				.setParameter("id", patrolId).list(); //$NON-NLS-1$
		
	}
	
	public List<PatrolWaypoint> getWaypointsByPatrolId(Session session, String patrolId, Set<UUID> conservationAreas) {

		List<Patrol> patrols = getPatrolsById(session, patrolId, conservationAreas); 

		List<PatrolWaypoint> waypoints = new ArrayList<>();
		for (Patrol p : patrols) {
			for (PatrolLeg leg : p.getLegs()) {
				for (PatrolLegDay pday : leg.getPatrolLegDays()) {
					for (PatrolWaypoint pw : pday.getWaypoints()) {
						waypoints.add(pw);
						if (waypoints.size() > MAX_RESULTS)
							throw TOO_MANY_ROWS;
					}
				}
			}
		}
		return waypoints;
	}

	public List<PatrolWaypoint> getWaypointsByPatrolWaypointUuid(Session session, String wpuuid, Set<UUID> conservationAreas){
		UUID uuid = UuidUtils.stringToUuid(wpuuid);
		
		StringBuilder sb = new StringBuilder();
		sb.append(" FROM PatrolWaypoint "); //$NON-NLS-1$
		sb.append(" WHERE id.waypoint.uuid = :uuid "); //$NON-NLS-1$
		sb.append(" AND id.waypoint.conservationArea.uuid IN (:uuids) "); //$NON-NLS-1$
		
		List<PatrolWaypoint> pws = session.createQuery(sb.toString(), PatrolWaypoint.class) 
				.setParameterList("uuids", conservationAreas) //$NON-NLS-1$
				.setParameter("uuid", uuid).list(); //$NON-NLS-1$
		
		if (pws.isEmpty()) throw new SmartConnectException(Status.NOT_FOUND,
				MessageFormat.format("No patrol waypoint found with id {0}", wpuuid)); //$NON-NLS-1$
		
		
		return pws;
	}
	
	public List<Track> getPatrolTracksByDate(Session session, LocalDate min, LocalDate max, Set<UUID> conservationAreas){
		Query<Track> query = null;
		if (max == null) {
			query = session.createQuery("FROM Track t WHERE t.patrolLegDay.date = :date ", Track.class) //$NON-NLS-1$
					.setParameter("date",  min); //$NON-NLS-1$
		}else {
			query = session.createQuery("FROM Track t WHERE t.patrolLegDay.date between :min and :max ", Track.class) //$NON-NLS-1$
					.setParameter("min",  min) //$NON-NLS-1$
					.setParameter("max",  max); //$NON-NLS-1$
		}
		List<Track> tracks = query.list();
		if (tracks.size() > MAX_RESULTS) throw TOO_MANY_ROWS;
		
		return tracks;		
	}
	
	
	
	public List<PatrolWaypoint> getWaypointsByPatrolWaypointDate(Session session, LocalDate min, LocalDate max, Set<UUID> conservationAreas){
		return getWaypointsByDateField(session, min, max, "dateTime", conservationAreas); //$NON-NLS-1$
	}
	
	public List<PatrolWaypoint> getWaypointsByPatrolWaypointDate(Session session, LocalDate date, Set<UUID> conservationAreas){
		return getWaypointsByPatrolWaypointDate(session, date, date, conservationAreas);
	}
	
	
	public List<PatrolWaypoint> getWaypointsByPatrolStartDate(Session session, LocalDate date, Set<UUID> conservationAreas) {
		return getWaypointsByPatrolStartDate(session, date, date, conservationAreas);
	}

	public List<PatrolWaypoint> getWaypointsByPatrolStartDate(Session session, LocalDate start, LocalDate end, Set<UUID> conservationAreas) {
		return getWaypointsByPatrolDateField(session, start, end, "startDate", conservationAreas); //$NON-NLS-1$
	}

	public List<PatrolWaypoint> getWaypointsByPatrolEndDate(Session session, LocalDate date, Set<UUID> conservationAreas) {
		return getWaypointsByPatrolEndDate(session, date, date, conservationAreas);
	}

	public List<PatrolWaypoint> getWaypointsByPatrolEndDate(Session session, LocalDate start, LocalDate end, Set<UUID> conservationAreas) {
		return getWaypointsByPatrolDateField(session, start, end, "endDate", conservationAreas); //$NON-NLS-1$
	}

	public List<PatrolWaypoint> getWaypointsByPatrolDateField(Session session, LocalDate start, LocalDate end,
			String dateField, Set<UUID> conservationAreas) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("FROM Patrol "); //$NON-NLS-1$
		sb.append("WHERE "); //$NON-NLS-1$
		sb.append(dateField + " >= :start AND "); //$NON-NLS-1$
		sb.append(dateField + " <= :end "); //$NON-NLS-1$
		sb.append(" AND conservationArea.uuid IN (:uuids)"); //$NON-NLS-1$
		
		List<Patrol> patrols = session
				.createQuery(sb.toString(),Patrol.class)
				.setParameter("start", start) //$NON-NLS-1$
				.setParameter("end", end) //$NON-NLS-1$
				.setParameter("uuids", conservationAreas) //$NON-NLS-1$
				.list();

		List<PatrolWaypoint> waypoints = new ArrayList<>();
		for (Patrol p : patrols) {
			for (PatrolLeg leg : p.getLegs()) {
				for (PatrolLegDay pday : leg.getPatrolLegDays()) {
					for (PatrolWaypoint pw : pday.getWaypoints()) {
						waypoints.add(pw);
						if (waypoints.size() > MAX_RESULTS)
							throw TOO_MANY_ROWS;
					}
				}
			}
		}
		return waypoints;
	}

	public List<PatrolWaypoint> getWaypointsByLastModified(Session session, LocalDate date, Set<UUID> conservationAreas) {
		return getWaypointsByLastModified(session, date, date, conservationAreas);
	}

	public List<PatrolWaypoint> getWaypointsByLastModified(Session session, LocalDate start, LocalDate end, Set<UUID> conservationAreas) {
		return getWaypointsByDateField(session, start, end, "lastModified", conservationAreas); //$NON-NLS-1$
	}

	private List<PatrolWaypoint> getWaypointsByDateField(Session session, LocalDate start, LocalDate end,
			String dateField, Set<UUID> conservationAreas) {

		StringBuilder sb = new StringBuilder();
		sb.append("FROM PatrolWaypoint "); //$NON-NLS-1$
		sb.append(" WHERE "); //$NON-NLS-1$
		sb.append(" cast(id.waypoint." + dateField + " as LocalDate) >= :start "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" AND cast(id.waypoint." + dateField + " as LocalDate) <= :end "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" AND id.waypoint.conservationArea.uuid IN (:uuids) "); //$NON-NLS-1$
		
		List<PatrolWaypoint> waypoints = session
				.createQuery(sb.toString(),PatrolWaypoint.class)
				.setParameter("start", start) //$NON-NLS-1$
				.setParameter("end", end) //$NON-NLS-1$
				.setParameter("uuids",  conservationAreas) //$NON-NLS-1$
				.setMaxResults(MAX_RESULTS + 1)
				.list();

		
		if (waypoints.size() > MAX_RESULTS) throw TOO_MANY_ROWS;
		return waypoints;
	}
	
	
	public JSONArray convertToJSON(List<PatrolWaypoint> pws, Session session, Locale l) {
		JSONArray items = new JSONArray(); 
		for(PatrolWaypoint pw : pws) {
			items.add(convertToJSON(pw, session, l));
		}
		return items;
		
	}

	
	public JSONObject convertToJSON(PatrolWaypoint pw, Session session, Locale l) {
		JSONObject feature = new JSONObject();
		feature.put("type", "Feature"); //$NON-NLS-1$ //$NON-NLS-2$
		
		JSONObject geom = new JSONObject();
		geom.put("type","Point"); //$NON-NLS-1$ //$NON-NLS-2$
		JSONArray coords = new JSONArray();
		coords.add(pw.getWaypoint().getX());
		coords.add(pw.getWaypoint().getY());
		geom.put("coordinates", coords); //$NON-NLS-1$
		feature.put("geometry", geom); //$NON-NLS-1$
		
		JSONObject props = new JSONObject();
		feature.put("properties", props); //$NON-NLS-1$
		
		props.put("fid", UuidUtils.uuidToString(pw.getWaypoint().getUuid())); //$NON-NLS-1$
		
		Patrol ptr = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
		
		JSONObject patrol = new JSONObject();
		patrol.put(UUID_FIELD, UuidUtils.uuidToString(ptr.getUuid()));
		patrol.put(ID_FIELD, ptr.getId());
		patrol.put("comment", ptr.getComment()); //$NON-NLS-1$
		patrol.put("armed", ptr.isArmed()); //$NON-NLS-1$
		patrol.put("start_date", ptr.getStartDate().toString()); //$NON-NLS-1$
		patrol.put("end_date", ptr.getEndDate().toString()); //$NON-NLS-1$
		patrol.put("objective", ptr.getObjective()); //$NON-NLS-1$
		
		//find a client uuid
		DataLink link = session
				.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("smartUuid", ptr.getUuid()) //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.PATROL.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link != null) {
			patrol.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
		}
			
		if (ptr.getStation() != null) {
			JSONObject station = new JSONObject();
			Station ps = ptr.getStation();
			station.put(UUID_FIELD, UuidUtils.uuidToString(ps.getUuid()));
			for (Label ll : ps.getNames()) {
				station.put(NAME_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
			}
			patrol.put("station", station); //$NON-NLS-1$
		}
		
		if (ptr.getTeam() != null) {
			JSONObject team = new JSONObject();
			Team pt = ptr.getTeam();
			team.put(UUID_FIELD, UuidUtils.uuidToString(pt.getUuid()));
			for (Label ll : pt.getNames()) {
				team.put(NAME_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
			}
			patrol.put("team", team); //$NON-NLS-1$
		}
		
		JSONObject ca = new JSONObject();
		ca.put(UUID_FIELD, UuidUtils.uuidToString(ptr.getConservationArea().getUuid()));
		ca.put(ID_FIELD, ptr.getConservationArea().getId());
		ca.put(NAME_FIELD, ptr.getConservationArea().getName());
		patrol.put("conservation_area", ca); //$NON-NLS-1$
		
		
		JSONArray customAttribute = new JSONArray();
		patrol.put("attributes", customAttribute); //$NON-NLS-1$
		for (PatrolAttributeValue custom : ptr.getCustomAttributes() ) {
			JSONObject value = new JSONObject();
			value.put("attribute_key", custom.getPatrolAttribute().getKeyId()); //$NON-NLS-1$
			for (Label ll : custom.getPatrolAttribute().getNames()) {
				value.put("attribute_name_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
			}
			switch(custom.getPatrolAttribute().getType()) {
			case BOOLEAN: value.put(VALUE_FIELD, custom.getNumberValue() >= 0.5);
				break;
			case DATE: value.put(VALUE_FIELD,custom.getDateValue().toString());
				break;
			case LIST:
				value.put(VALUE_FIELD, custom.getAttributeListItem().getKeyId());
				for (Label ll : custom.getAttributeListItem().getNames()) {
					value.put(LABEL_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
				}
				break;
			case MLIST:
				//not supported
				break;
			case NUMERIC: value.put(VALUE_FIELD, custom.getNumberValue());
				break;
			case TEXT:value.put(VALUE_FIELD, custom.getStringValue());
				break;
			case TREE:
			case TIME:
				//not supported
				break;
			default:
				break;
			
			}
		}
		props.put("patrol", patrol); //$NON-NLS-1$
		
		
		JSONObject patrolleg = new JSONObject();
		patrolleg.put(UUID_FIELD, UuidUtils.uuidToString(pw.getPatrolLegDay().getPatrolLeg().getUuid()));
		patrolleg.put(ID_FIELD, pw.getPatrolLegDay().getPatrolLeg().getId());
		patrolleg.put("start_date", pw.getPatrolLegDay().getPatrolLeg().getStartDate().toString()); //$NON-NLS-1$
		patrolleg.put("end_date", pw.getPatrolLegDay().getPatrolLeg().getEndDate().toString()); //$NON-NLS-1$
		
		//find a client uuid
		link = session
				.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("smartUuid", pw.getPatrolLegDay().getPatrolLeg().getUuid()) //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.LEG.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link != null) {
			patrolleg.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
		}
				
		if (pw.getPatrolLegDay().getPatrolLeg().getMandate() != null) {
			JSONObject mandate = new JSONObject();
			PatrolMandate pm = pw.getPatrolLegDay().getPatrolLeg().getMandate();
			mandate.put(UUID_FIELD, UuidUtils.uuidToString(pm.getUuid()));
			mandate.put(KEY_FIELD,pw.getPatrolLegDay().getPatrolLeg().getMandate().getKeyId());
			for (Label ll : pm.getNames()) {
				mandate.put(NAME_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
			}
			patrolleg.put("mandate", mandate); //$NON-NLS-1$
		}
		
		JSONObject type = new JSONObject();
		PatrolTransportType tt = pw.getPatrolLegDay().getPatrolLeg().getType();
		type.put(UUID_FIELD, UuidUtils.uuidToString(tt.getUuid()));
		type.put(KEY_FIELD, tt.getKeyId());
		for (Label ll : tt.getNames()) {
			type.put("name_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
		}
		patrolleg.put("type", type); //$NON-NLS-1$
		
		
		JSONArray members = new JSONArray();
		for (PatrolLegMember m : pw.getPatrolLegDay().getPatrolLeg().getMembers()) {
			JSONObject member = new JSONObject();
			member.put(UUID_FIELD, UuidUtils.uuidToString(m.getMember().getUuid()));
			member.put(ID_FIELD, m.getMember().getId());
			member.put(NAME_FIELD, SmartLabelProvider.getFullName(m.getMember(), l));
			member.put("is_leader", m.getIsLeader()); //$NON-NLS-1$
			member.put("is_pilot", m.getIsPilot()); //$NON-NLS-1$
			members.add(member);
		}
		patrolleg.put("members",members); //$NON-NLS-1$
				
		//patrol leg day start time
		JSONObject patrolLegDay = new JSONObject();
		patrolLegDay.put("date", pw.getPatrolLegDay().getDate().toString()); //$NON-NLS-1$
		patrolLegDay.put("start_time", pw.getPatrolLegDay().getStartTime().toString()); //$NON-NLS-1$
		patrolLegDay.put("end_time", pw.getPatrolLegDay().getEndTime().toString()); //$NON-NLS-1$
		
		patrolleg.put("patrol_leg_day",patrolLegDay); //$NON-NLS-1$
		
		props.put("patrol_leg", patrolleg); //$NON-NLS-1$
		
		JSONObject jwp = convertWaypoint(pw.getWaypoint(), session);	
		
		link = session
				.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("smartUuid", pw.getWaypoint().getUuid()) //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.INCIDENT.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link != null) {
			jwp.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
		}
		props.put(WAYPOINT_FIELD,jwp);
		
		return feature;
		
	}
	
	/**
	 * return GeoJSON feature collection of the tracks
	 * @param tracks
	 * @param session
	 * @param l
	 * @return
	 * @throws JSONException
	 * @throws ParseException
	 */
	public JSONObject convertTrackToJSON(List<Track> tracks, Session session, Locale l) throws ParseException {
		JSONArray items = new JSONArray(); 
		for(Track track : tracks) {
			items.add(convertTrackToJSON(track, session, l));
		}
		JSONObject collection = new JSONObject();
		collection.put("type",  "FeatureCollection"); //$NON-NLS-1$ //$NON-NLS-2$
		collection.put("features",  items); //$NON-NLS-1$
		return collection;
		
	}
	
	public JSONObject convertTrackToJSON(Track track, Session session, Locale l) throws ParseException {
		JSONObject feature = new JSONObject();
		feature.put("type", "Feature"); //$NON-NLS-1$ //$NON-NLS-2$
		
		JSONObject geom = new JSONObject();
		
		JSONArray times = new JSONArray();
		
		if (track.getLineStrings().isEmpty()) {
			//no geometries
			geom.put("type","LineString"); //$NON-NLS-1$ //$NON-NLS-2$
			geom.put("coordinates", new JSONArray()); //$NON-NLS-1$
		}else if (track.getLineStrings().size() == 1) {
			//single line
			geom.put("type","LineString"); //$NON-NLS-1$ //$NON-NLS-2$
			
			LineString ls = track.getLineStrings().get(0);
			JSONArray coords = new JSONArray();
			geom.put("coordinates", coords); //$NON-NLS-1$
				
			for (Coordinate c : ls.getCoordinates()) {
				JSONArray values = new JSONArray();
				values.add(c.getX());
				values.add(c.getY());
				coords.add(values);
				
				times.add(SharedUtils.toLocalDateTime(c).toString());
			}
		}else {
			//multiple lines
			geom.put("type","MultiLineString"); //$NON-NLS-1$ //$NON-NLS-2$
			JSONArray linestrings = new JSONArray();
			geom.put("coordinates", linestrings); //$NON-NLS-1$
			
			
			for (LineString ls : track.getLineStrings()) {
				JSONArray ltimes = new JSONArray();
				times.add(ltimes);
				JSONArray coords = new JSONArray();
				linestrings.add(coords);
				for (Coordinate c : ls.getCoordinates()) {
					JSONArray values = new JSONArray();
					values.add(c.getX());
					values.add(c.getY());
					ltimes.add(SharedUtils.toLocalDateTime(c).toString());
					coords.add(values);
				}
			}
		}
		
		feature.put("geometry", geom); //$NON-NLS-1$
		
		JSONObject props = new JSONObject();
		feature.put("properties", props); //$NON-NLS-1$
		
		props.put("fid", UuidUtils.uuidToString(track.getUuid())); //$NON-NLS-1$
		props.put("date", track.getPatrolLegDay().getDate().toString()); //$NON-NLS-1$
		props.put("distance_km", track.getDistance()); //$NON-NLS-1$
		props.put("timestamps", times); //$NON-NLS-1$
		
		Patrol p = track.getPatrolLegDay().getPatrolLeg().getPatrol();
		JSONObject patrol = new JSONObject();
		patrol.put(UUID_FIELD, UuidUtils.uuidToString(p.getUuid()));	
		patrol.put(ID_FIELD, p.getId());
		patrol.put("ca_uuid", UuidUtils.uuidToString(p.getConservationArea().getUuid()) ); //$NON-NLS-1$
		
		//find a client uuid
		DataLink link = session
				.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("smartUuid", p.getUuid()) //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.PATROL.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link != null) {
			patrol.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
		}
		
		props.put("patrol", patrol); //$NON-NLS-1$
		
		
		JSONObject patrolleg = new JSONObject();
		patrolleg.put(UUID_FIELD, UuidUtils.uuidToString(track.getPatrolLegDay().getPatrolLeg().getUuid()));
		patrolleg.put(ID_FIELD, track.getPatrolLegDay().getPatrolLeg().getId());
		
		//find a client uuid
		link = session
				.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("smartUuid", track.getPatrolLegDay().getPatrolLeg().getUuid()) //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.LEG.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link != null) {
			patrolleg.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
		}
		props.put("patrol_leg", patrolleg); //$NON-NLS-1$
		
		
		
		
		
		return feature;
		
	}
	
	public JSONArray convertPatrolsToJSON(List<Patrol> patrols, Session session, Locale l) {
		JSONArray ptrs = new JSONArray();
		for (Patrol p : patrols) {
			ptrs.add(convertToJSON(p, session, l));
		}
		return ptrs;
	}
	
	public JSONObject convertToJSON(Patrol ptr, Session session, Locale l) {
		
		JSONObject patrol = new JSONObject();
		patrol.put(UUID_FIELD, UuidUtils.uuidToString(ptr.getUuid()));
		patrol.put(ID_FIELD, ptr.getId());
		patrol.put("comment", ptr.getComment()); //$NON-NLS-1$
		patrol.put("armed", ptr.isArmed()); //$NON-NLS-1$
		patrol.put("start_date", ptr.getStartDate().toString()); //$NON-NLS-1$
		patrol.put("end_date", ptr.getEndDate().toString()); //$NON-NLS-1$
		patrol.put("objective", ptr.getObjective()); //$NON-NLS-1$
		
		//find a client uuid
		DataLink link = session
				.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("smartUuid", ptr.getUuid()) //$NON-NLS-1$
				.setParameter("dataType", PatrolLinkDataType.PATROL.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link != null) {
			patrol.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
		}
			
		if (ptr.getStation() != null) {
			JSONObject station = new JSONObject();
			Station ps = ptr.getStation();
			station.put(UUID_FIELD, UuidUtils.uuidToString(ps.getUuid()));
			for (Label ll : ps.getNames()) {
				station.put(NAME_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
			}
			patrol.put("station", station); //$NON-NLS-1$
		}
		
		if (ptr.getTeam() != null) {
			JSONObject team = new JSONObject();
			Team pt = ptr.getTeam();
			team.put(UUID_FIELD, UuidUtils.uuidToString(pt.getUuid()));
			for (Label ll : pt.getNames()) {
				team.put(NAME_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
			}
			patrol.put("team", team); //$NON-NLS-1$
		}
		
		JSONObject ca = new JSONObject();
		ca.put(UUID_FIELD, UuidUtils.uuidToString(ptr.getConservationArea().getUuid()));
		ca.put(ID_FIELD, ptr.getConservationArea().getId());
		ca.put(NAME_FIELD, ptr.getConservationArea().getName());
		patrol.put("conservation_area", ca); //$NON-NLS-1$
		
		
		JSONArray customAttribute = new JSONArray();
		patrol.put("attributes", customAttribute); //$NON-NLS-1$
		for (PatrolAttributeValue custom : ptr.getCustomAttributes() ) {
			JSONObject value = new JSONObject();
			value.put("attribute_key", custom.getPatrolAttribute().getKeyId()); //$NON-NLS-1$
			for (Label ll : custom.getPatrolAttribute().getNames()) {
				value.put("attribute_name_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
			}
			switch(custom.getPatrolAttribute().getType()) {
			case BOOLEAN: value.put(VALUE_FIELD, custom.getNumberValue() >= 0.5);
				break;
			case DATE: value.put(VALUE_FIELD,custom.getDateValue().toString());
				break;
			case LIST:
				value.put(VALUE_FIELD, custom.getAttributeListItem().getKeyId());
				for (Label ll : custom.getAttributeListItem().getNames()) {
					value.put(LABEL_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
				}
				break;
			case MLIST:
				//not supported
				break;
			case NUMERIC: value.put(VALUE_FIELD, custom.getNumberValue());
				break;
			case TEXT:value.put(VALUE_FIELD, custom.getStringValue());
				break;
			case TREE:
				value.put(VALUE_FIELD, custom.getAttributeTreeNode().getHkey());
				for (Label ll : custom.getAttributeTreeNode().getNames()) {
					value.put(LABEL_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
				}
				break;
			case TIME:
				//not supported
				break;
			default:
				break;
			
			}
			customAttribute.add(value);
		}
		
		JSONArray legs = new JSONArray();
		for (PatrolLeg leg : ptr.getLegs()) {
			JSONObject patrolleg = new JSONObject();
			patrolleg.put(UUID_FIELD, UuidUtils.uuidToString(leg.getUuid()));
			patrolleg.put(ID_FIELD, leg.getId());
			patrolleg.put("start_date", leg.getStartDate().toString()); //$NON-NLS-1$
			patrolleg.put("end_date", leg.getEndDate().toString()); //$NON-NLS-1$
			legs.add(patrolleg);
			
			//find a client uuid
			link = session
					.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
					.setParameter("smartUuid", leg.getUuid()) //$NON-NLS-1$
					.setParameter("dataType", PatrolLinkDataType.LEG.getKey()) //$NON-NLS-1$
					.uniqueResult();
			if (link != null) {
				patrolleg.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
			}
			
			
			
			if (leg.getMandate() != null) {
				JSONObject mandate = new JSONObject();
				PatrolMandate pm = leg.getMandate();
				mandate.put(UUID_FIELD, UuidUtils.uuidToString(pm.getUuid()));
				mandate.put(KEY_FIELD,leg.getMandate().getKeyId());
				for (Label ll : pm.getNames()) {
					mandate.put(NAME_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
				}
				patrolleg.put("mandate", mandate); //$NON-NLS-1$
			}
			
			JSONObject type = new JSONObject();
			PatrolTransportType tt = leg.getType();
			type.put(UUID_FIELD, UuidUtils.uuidToString(tt.getUuid()));
			type.put(KEY_FIELD, tt.getKeyId());
			for (Label ll : tt.getNames()) {
				type.put("name_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
			}
			patrolleg.put("type", type); //$NON-NLS-1$
			
			
			JSONArray members = new JSONArray();
			for (PatrolLegMember m : leg.getMembers()) {
				JSONObject member = new JSONObject();
				member.put(UUID_FIELD, UuidUtils.uuidToString(m.getMember().getUuid()));
				member.put(ID_FIELD, m.getMember().getId());
				member.put(NAME_FIELD, SmartLabelProvider.getFullName(m.getMember(), l));
				member.put("is_leader", m.getIsLeader()); //$NON-NLS-1$
				member.put("is_pilot", m.getIsPilot()); //$NON-NLS-1$
				members.add(member);
			}
			patrolleg.put("members",members); //$NON-NLS-1$
					
			JSONArray legdays = new JSONArray();
			
			for (PatrolLegDay day : leg.getPatrolLegDays()) {
				//patrol leg day start time
				JSONObject patrolLegDay = new JSONObject();
				patrolLegDay.put("date", day.getDate().toString()); //$NON-NLS-1$
				patrolLegDay.put("start_time", day.getStartTime().toString()); //$NON-NLS-1$
				patrolLegDay.put("end_time", day.getEndTime().toString()); //$NON-NLS-1$
				legdays.add(patrolLegDay);
			}
			patrolleg.put("patrol_leg_days",legdays); //$NON-NLS-1$
			
		}
		patrol.put("patrol_legs", legs); //$NON-NLS-1$
		
		return patrol;
		
	}
}
