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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.IntegrateIncidentSource;
import org.wcs.smart.incident.json.IncidentJsonFeatureProcessor.IncidentLinkDataType;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;
import org.wcs.smart.util.UuidUtils;

/**
 * For custom api for querying independent incidents
 * 
 * @author Emily
 *
 */
public class CustomIncidentQueryEngine extends CustomQueryEngine {

	public List<Waypoint> getWaypointsById(Session session, String wpId, Set<UUID> conservationAreas) {

		List<Waypoint> waypoints = session.createQuery("FROM Waypoint WHERE id = :id and sourceId = :source and conservationArea.uuid IN (:uuids) ", Waypoint.class) //$NON-NLS-1$
				.setParameter("id", wpId) //$NON-NLS-1$
				.setParameter("source", IndepedentIncidentSource.KEY) //$NON-NLS-1$
				.setParameterList("uuids", conservationAreas) //$NON-NLS-1$
				.setMaxResults(MAX_RESULTS + 1)
				.list();
		if (waypoints.size() > MAX_RESULTS) throw TOO_MANY_ROWS;
		return waypoints;
	}

	public List<Waypoint> getWaypointsByUUID(Session session, String smartuuid, Set<UUID> conservationAreas) {

		UUID wpuuid = UuidUtils.stringToUuid(smartuuid);
		return getWaypointsByUUID(session, wpuuid, conservationAreas);
	}

	public List<Waypoint> getWaypointsByClient(Session session, String clientuuid, Set<UUID> conservationAreas) {

		UUID cuuid = UuidUtils.stringToUuid(clientuuid);
		
		DataLink link = session
				.createQuery("FROM DataLink WHERE providerId = :clientUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("clientUuid", cuuid) //$NON-NLS-1$
				.setParameter("dataType", IncidentLinkDataType.INCIDENT.getKey()) //$NON-NLS-1$
				.uniqueResult();
		if (link == null)
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No incident found with link to client id {0}", clientuuid)); //$NON-NLS-1$

		return getWaypointsByUUID(session, link.getSmartId(), conservationAreas);
	}
	
	public List<Waypoint> getWaypointsByUUID(Session session, UUID wpuuid, Set<UUID> conservationAreas) {
		Waypoint wp = session.get(Waypoint.class, wpuuid);
		if (wp == null || !conservationAreas.contains(wp.getConservationArea().getUuid())) {
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No incident found with uuid {0}", wpuuid)); //$NON-NLS-1$
		}
		//TODO: add additional incident sources here if they are added to desktop
		if (!wp.getSourceId().equals(IndepedentIncidentSource.KEY) && 
				wp.getSourceId().equals(IntegrateIncidentSource.KEY) &&
				wp.getSourceId().equals(SmartCollectWaypointSource.KEY) ) {
			throw new SmartConnectException(Status.NOT_FOUND,
					MessageFormat.format("No independent incident found with uuid {0}", wpuuid)); //$NON-NLS-1$
		}
		return Collections.singletonList(wp);
	}
	



	public List<Waypoint> getWaypointsByDate(Session session, LocalDate date, Set<UUID> conservationAreas) {
		return getWaypointsByDate(session, date, date, conservationAreas);
	}

	public List<Waypoint> getWaypointsByDate(Session session, LocalDate start, LocalDate end, Set<UUID> conservationAreas) {
		return getWaypointsByDateField(session, start, end, "dateTime", conservationAreas); //$NON-NLS-1$
	}

	public List<Waypoint> getWaypointsByLastModified(Session session, LocalDate date, Set<UUID> conservationAreas) {
		return getWaypointsByLastModified(session, date, date, conservationAreas);
	}

	public List<Waypoint> getWaypointsByLastModified(Session session, LocalDate start, LocalDate end, Set<UUID> conservationAreas) {
		return getWaypointsByDateField(session, start, end, "lastModified", conservationAreas); //$NON-NLS-1$
	}

	private List<Waypoint> getWaypointsByDateField(Session session, LocalDate start, LocalDate end,
			String dateField, Set<UUID> conservationAreas) {

		StringBuilder sb = new StringBuilder();
		sb.append("FROM Waypoint "); //$NON-NLS-1$
		sb.append("WHERE cast(" + dateField +  " as LocalDate) >= :start "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("AND cast(" + dateField +  " as LocalDate) <= :end "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" AND sourceId = :source "); //$NON-NLS-1$
		sb.append(" AND conservationArea.uuid IN (:uuids) "); //$NON-NLS-1$
		
		List<Waypoint> waypoints = session
				.createQuery(sb.toString(), Waypoint.class)
				.setParameter("start", start) //$NON-NLS-1$
				.setParameter("end", end) //$NON-NLS-1$
				.setParameter("source", IndepedentIncidentSource.KEY) //$NON-NLS-1$
				.setParameterList("uuids", conservationAreas) //$NON-NLS-1$
				.setMaxResults(MAX_RESULTS + 1)
				.list();
		if (waypoints.size() > MAX_RESULTS) throw TOO_MANY_ROWS;
		return waypoints;

	}
	
	
	public JSONArray convertToJSON(List<Waypoint> pws, Session session) {
		JSONArray items = new JSONArray(); 
		for(Waypoint pw : pws) {
			items.add(convertToJSON(pw, session));
		}
		return items;
		
	}
	@SuppressWarnings("unchecked")
	public JSONObject convertToJSON(Waypoint pw, Session session) {
		JSONObject feature = new JSONObject();
		feature.put("type", "Feature"); //$NON-NLS-1$ //$NON-NLS-2$
		
		JSONObject geom = new JSONObject();
		geom.put("type","Point"); //$NON-NLS-1$ //$NON-NLS-2$
		JSONArray coords = new JSONArray();
		coords.add(pw.getX());
		coords.add(pw.getY());
		geom.put("coordinates", coords); //$NON-NLS-1$
		feature.put("geometry", geom); //$NON-NLS-1$
		
		JSONObject props = new JSONObject();
		feature.put("properties", props); //$NON-NLS-1$
		
		props.put("fid", UuidUtils.uuidToString(pw.getUuid())); //$NON-NLS-1$
		
		JSONObject ca = new JSONObject();
		ca.put(UUID_FIELD, UuidUtils.uuidToString(pw.getConservationArea().getUuid()));
		ca.put(ID_FIELD, pw.getConservationArea().getId());
		ca.put(NAME_FIELD, pw.getConservationArea().getName());
			
				
		JSONObject jwp = convertWaypoint(pw, session);
		jwp.put("conservation_area", ca); //$NON-NLS-1$
		
		//find a client uuid
		DataLink link = session
			.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
			.setParameter("smartUuid", pw.getUuid()) //$NON-NLS-1$
			.setParameter("dataType", IncidentLinkDataType.INCIDENT.getKey()) //$NON-NLS-1$
			.uniqueResult();
		if (link != null) {
			jwp.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
		}
				
		props.put(WAYPOINT_FIELD, jwp);
		
		return feature;
		
	}
	
	
}
