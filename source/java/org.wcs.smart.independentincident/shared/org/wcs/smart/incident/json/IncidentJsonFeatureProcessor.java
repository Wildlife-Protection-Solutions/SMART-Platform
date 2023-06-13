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
package org.wcs.smart.incident.json;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.incident.IIncidentLabelProvider;
import org.wcs.smart.incident.IncidentIdGenerator;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.IntegrateIncidentSource;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;

/**
 * Processes incident JSON features into SMART Independent Incidents
 * 
 * @author Emily
 *
 */
public class IncidentJsonFeatureProcessor extends IJsonFeatureProcessor {

	//Incident related JSON data type
	private static final String INCIDENT_DATATYPE = "incident"; //$NON-NLS-1$
	private static final String INTEGRATE_DATATYPE = "integrateincident"; //$NON-NLS-1$
	
	public enum IncidentLinkDataType{
		INCIDENT("incident"); //$NON-NLS-1$
		
		private String key;
		IncidentLinkDataType(String key){
			this.key = key;
		}
		
		public String getKey() {
			return this.key;
		}
	}
	
	public enum Messages{
		INVALID_DATA_TYPE,
		INVALID_FEATURE_TYPE,
		OBSERVATION_EXISTS,
		MISSING_PROPERTY,
		OBSERVATION_NOT_FOUND,
		WAYPOINT_NOT_FOUND,
		COMPLETE_MSG;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IIncidentLabelProvider.class).getLabel(this, l);
		}
	}
	
	private Map<String, Set<Waypoint>> createdFeatures = new HashMap<>();

	/**
	 * @return <code>true</code> if this processor can process the given feature
	 * type.  
	 */
	@Override
	public boolean canProcess(String featureType) {
		return featureType.equalsIgnoreCase(INCIDENT_DATATYPE) ||
				featureType.equalsIgnoreCase(INTEGRATE_DATATYPE);
	}

	/**
	 * 
	 * @return set of features created by this processor
	 */
	public Set<Waypoint> getCreatedFeatures(IWaypointSource source){
		return this.createdFeatures.get(source.getKey());
	}
		
	@Override
	public void processFeature(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception {

		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);

		String dtype = props.get(JSON_SMARTDATATYPE).toString(); 
		if (!dtype.equalsIgnoreCase(INCIDENT_DATATYPE) && !dtype.equalsIgnoreCase(INTEGRATE_DATATYPE))
			throw new Exception(MessageFormat.format(Messages.INVALID_DATA_TYPE.getMessage(l), dtype, INCIDENT_DATATYPE));

		String ftype = props.get(JSON_SMARTFEATURETYPE).toString();
		
		if (ftype.equalsIgnoreCase(SmartFeatureType.WAYPOINT_NEW.getKey())) {
			processNewWaypoint(feature,dtype, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SmartFeatureType.WAYPOINT.getKey())) {
			processWaypointUpdate(feature, ca, session, l);
		}else if (ftype.equalsIgnoreCase(SmartFeatureType.OBSERVATION.getKey())) {
			processObservationUpdate(feature, ca, session, l);
		}else {
			throw new Exception(MessageFormat.format(Messages.INVALID_FEATURE_TYPE.getMessage(l), ftype, SmartFeatureType.WAYPOINT_NEW.getKey()));
	
		}
	}

	private void processNewWaypoint(JSONObject feature, String wpDataType, ConservationArea ca, Session session, Locale l) throws Exception {
		Waypoint wp = super.createWaypoint(feature, ca, session, l);
		
		wp.setSourceId(IndepedentIncidentSource.KEY);
		if (wpDataType.equalsIgnoreCase(INTEGRATE_DATATYPE)) {
			wp.setSourceId(IntegrateIncidentSource.KEY);	
		}
		
		if (wp.getId() == null) {
			Employee observer = null;
			for (WaypointObservation so : wp.getAllObservations()) {
				if (so.getObserver() != null) {
					observer = so.getObserver();
					break;
				}
			}
			wp.setId(IncidentIdGenerator.INSTANCE.getNextIncidentId(session, ca, Collections.singleton(wp.getSourceId()), wp.getDateTime(), observer));
		}
		
		Waypoint addTo = null;
		
		if (wp.getUuid() != null) {
			addTo = findIncidentLink(wp.getUuid(), ca, session, l);
		}
		
		HashMap<UUID, WaypointObservationGroup> links = new HashMap<>();
		HashMap<UUID, WaypointObservation> observationlinks = new HashMap<>();

		if (addTo == null) {
			
			if (wp.getDateTime() == null) {
				throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), IJsonFeatureProcessor.JSON_DATETIME_KEY));
			}
			
			if (wp.getRawX() == null || wp.getRawY() == null) {
				throw new Exception(MessageFormat.format(Messages.MISSING_PROPERTY.getMessage(l), "geometry")); //$NON-NLS-1$
			}
			
			UUID srcUuid = wp.getUuid();
			wp.setUuid(null);
			wp.getObservationGroups().forEach(g->{
				if (g.getUuid() != null) {
					//clear any old link
					session.createMutationQuery("DELETE From DataLink WHERE providerId = :uuid and dataType = :datatype") //$NON-NLS-1$
						.setParameter("uuid", g.getUuid()) //$NON-NLS-1$
						.setParameter("datatype", LinkDataType.OBSERVATION_GROUP.getKey()) //$NON-NLS-1$
						.executeUpdate();
					links.put(g.getUuid(), g);
					g.setUuid(null);
				}
			});
			
			for (WaypointObservation wo : wp.getAllObservations()) {
				if (wo.getUuid() != null) {
					//clear any old link
					session.createMutationQuery("DELETE From DataLink WHERE providerId = :uuid and dataType = :datatype") //$NON-NLS-1$
						.setParameter("uuid", wo.getUuid()) //$NON-NLS-1$
						.setParameter("datatype", LinkDataType.OBSERVATION.getKey()) //$NON-NLS-1$
						.executeUpdate();
					observationlinks.put(wo.getUuid(), wo);
					wo.setUuid(null);
				}
			}
	
			if (wp.getUuid() == null) session.persist(wp);
			
			if (srcUuid != null) {
				session.flush();
				DataLink dl = new DataLink();
				dl.setConservationArea(ca);
				dl.setProviderId(srcUuid);
				dl.setSmartId(wp.getUuid());
				dl.setDataType(INCIDENT_DATATYPE);
				session.persist(dl);
			}else {
				links.clear();
			}
		}else {
			//add observations
			List<WaypointObservationGroup> add = new ArrayList<>();
			
			for (WaypointObservation wo : wp.getAllObservations()) {
				if (wo.getUuid() != null) {
					WaypointObservation existing = findObservationLink(wo.getUuid(), ca, session);
					if (existing != null) {
						//throw an error - should not be updating observations this way
						throw new Exception(MessageFormat.format(Messages.OBSERVATION_EXISTS.getMessage(l), SmartFeatureType.OBSERVATION.getKey()));
					}	
					observationlinks.put(wo.getUuid(), wo);
					wo.setUuid(null);
				}
			}
			
			for (WaypointObservationGroup group : wp.getObservationGroups()) {
				
				if (group.getUuid() == null) {
					add.add(group);
				}else {
					WaypointObservationGroup existing = findWaypointObservationGroup(group.getUuid(), ca, session);
					if (existing == null || !existing.getWaypoint().equals(addTo)) {
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
				if (g.getUuid() != null) links.put(g.getUuid(), g);
				g.setUuid(null);
				addTo.getObservationGroups().add(g);
				g.setWaypoint(addTo);
			}
			if (addTo.getUuid() == null) session.persist(addTo);
			
			for (WaypointObservationGroup g : addTo.getObservationGroups()) {
				if (g.getUuid() == null) session.persist(g);
			}

		}
		
		session.flush();
		for (Entry<UUID,WaypointObservationGroup> lnk : links.entrySet()) {
			DataLink dl = new DataLink();
			dl.setConservationArea(ca);
			dl.setProviderId(lnk.getKey());
			dl.setSmartId(lnk.getValue().getUuid());
			dl.setDataType( LinkDataType.OBSERVATION_GROUP.getKey() );
			session.persist(dl);
		}
		for (Entry<UUID,WaypointObservation> lnk : observationlinks.entrySet()) {
			DataLink dl = new DataLink();
			dl.setConservationArea(ca);
			dl.setProviderId(lnk.getKey());
			dl.setSmartId(lnk.getValue().getUuid());
			dl.setDataType( LinkDataType.OBSERVATION.getKey() );
			session.persist(dl);
		}
		logFeature(wp);
	}
	
	private void logFeature(Waypoint wp) {
		if (!createdFeatures.containsKey(wp.getSourceId())) createdFeatures.put(wp.getSourceId(), new HashSet<>());
		createdFeatures.get(wp.getSourceId()).add(wp);
	}
	
	/**
	 * Creates a user friendly message describing the actions 
	 * applied to the database
	 */
	@Override
	public String getMessage(Locale l) {
		if (createdFeatures.isEmpty())
			return null;
 
		StringBuilder sb = new StringBuilder();
		for (Set<Waypoint> items : createdFeatures.values()) {
			for (Waypoint wp : items) {
				sb.append(wp.getId());
				sb.append(", "); //$NON-NLS-1$
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		return MessageFormat.format(Messages.COMPLETE_MSG.getMessage(l), createdFeatures.size(), sb.toString());
	}
	
	
	private Waypoint findIncidentLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", IncidentLinkDataType.INCIDENT.getKey()) //$NON-NLS-1$
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
		
		if (!(waypoint.getSourceId().equals(IndepedentIncidentSource.KEY) 
				|| waypoint.getSourceId().equals(IntegrateIncidentSource.KEY))) {
			throw new Exception("Link is not independent incident"); //$NON-NLS-1$
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

		if (wp.getId() != null) toUpdate.setId(wp.getId());
		if (wp.getComment() != null) toUpdate.setComment(wp.getComment());
		if (wp.getDirection() != null) toUpdate.setDirection(wp.getDirection());
		if (wp.getDistance() != null) toUpdate.setDistance(wp.getDistance());
		
		if (wp.getRawX() != null) toUpdate.setRawX(wp.getRawX());
		if (wp.getRawY() != null) toUpdate.setRawY(wp.getRawY());
		
		if (wp.getDateTime() != null) toUpdate.setDateTime(wp.getDateTime());
		
		//update observer
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
		
		session.persist(toUpdate);
		session.flush();

		logFeature(toUpdate);
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

		if (attributes.containsKey(IJsonFeatureProcessor.JSON_OBSERVER_KEY)) {
			toUpdate.setObserver(observation.getObserver());
		}

		toUpdate.getAttributes().clear();
		session.flush();
			
		if (attributes.containsKey("category")) { //$NON-NLS-1$
			toUpdate.setCategory(observation.getCategory());
			for (WaypointObservationAttribute a : observation.getAttributes()) {
				a.setObservation(toUpdate);
				toUpdate.getAttributes().add(a);
			}
		}
		session.persist(toUpdate);
		session.flush();
		
		//attachments
		if (toUpdate.getAttachments() == null) toUpdate.setAttachments(new ArrayList<>());
		for (ObservationAttachment attachment: observation.getAttachments()) {
			ObservationAttachment newattachment = new ObservationAttachment();
			newattachment.setCopyFromLocation(attachment.getCopyFromLocation());
			newattachment.setFilename(attachment.getFilename());
			newattachment.setObservation(toUpdate);
			toUpdate.getAttachments().add(newattachment);
		}
		logFeature(toUpdate.getWaypoint());
	}
}
