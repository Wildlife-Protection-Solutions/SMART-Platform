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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.incident.IIncidentLabelProvider;
import org.wcs.smart.incident.IncidentIdGenerator;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;

import com.ibm.icu.text.MessageFormat;

/**
 * Processes incident JSON features into SMART Independent Incidents
 * 
 * @author Emily
 *
 */
public class IncidentJsonFeatureProcessor extends IJsonFeatureProcessor {

	private static final String DATATYPE = "incident"; //$NON-NLS-1$
	
	public enum Messages{
		INVALID_DATA_TYPE,
		INVALID_FEATURE_TYPE,
		COMPLETE_MSG;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IIncidentLabelProvider.class).getLabel(this, l);
		}
	}
	
	private Set<Waypoint> createdFeatures = new HashSet<>();

	/**
	 * @return <code>true</code> if this processor can process the given feature
	 * type.  
	 */
	@Override
	public boolean canProcess(String featureType) {
		return featureType.equalsIgnoreCase(DATATYPE); 
	}

	/**
	 * 
	 * @return set of features created by this processor
	 */
	public Set<Waypoint> getCreatedFeatures(){
		return this.createdFeatures;
	}
	
	@Override
	public void processFeature(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception {

		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);

		String dtype = props.get(JSON_SMARTDATATYPE).toString(); 
		if (!dtype.equalsIgnoreCase(DATATYPE))
			throw new Exception(MessageFormat.format(Messages.INVALID_DATA_TYPE.getMessage(l), dtype, DATATYPE));

		String ftype = props.get(JSON_SMARTFEATURETYPE).toString();
		if (!ftype.equalsIgnoreCase(JSON_FT_OBSERVATION))
		throw new Exception(MessageFormat.format(Messages.INVALID_FEATURE_TYPE.getMessage(l), ftype, JSON_FT_OBSERVATION));

		Waypoint wp = super.createWaypoint(feature, ca, session, l);
		wp.setSourceId(IndepedentIncidentSource.KEY);
		if (wp.getId() == null) {
			Employee observer = null;
			for (WaypointObservation so : wp.getAllObservations()) {
				if (so.getObserver() != null) {
					observer = so.getObserver();
					break;
				}
			}
			wp.setId(IncidentIdGenerator.INSTANCE.getNextIncidentId(session, ca, Collections.singleton(wp.getSourceId()), observer));
		}
		
		Waypoint addTo = null;
		
		if (wp.getUuid() != null) {
			addTo = findIncidentLink(wp.getUuid(), ca, session, l);
		}
		
		HashMap<UUID, WaypointObservationGroup> links = new HashMap<>();

		if (addTo == null) {
			UUID srcUuid = wp.getUuid();
			wp.setUuid(null);
			wp.getObservationGroups().forEach(g->{
				if (g.getUuid() != null) {
					//clear any old link
					session.createQuery("DELETE From DataLink WHERE providerId = :uuid") //$NON-NLS-1$
						.setParameter("uuid", g.getUuid()) //$NON-NLS-1$
						.executeUpdate();
					links.put(g.getUuid(), g);
					g.setUuid(null);
				}
			});
	
			session.saveOrUpdate(wp);
			if (srcUuid != null) {
				session.flush();
				DataLink dl = new DataLink();
				dl.setConservationArea(ca);
				dl.setProviderId(srcUuid);
				dl.setSmartId(wp.getUuid());
				dl.setDataType(DATATYPE);
				session.save(dl);
			}else {
				links.clear();
			}
		}else {
			//add observations
			List<WaypointObservationGroup> add = new ArrayList<>();
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
			session.saveOrUpdate(addTo);
			for (WaypointObservationGroup g : addTo.getObservationGroups()) session.saveOrUpdate(g);

		}
		
		session.flush();
		for (Entry<UUID,WaypointObservationGroup> lnk : links.entrySet()) {
			DataLink dl = new DataLink();
			dl.setConservationArea(ca);
			dl.setProviderId(lnk.getKey());
			dl.setSmartId(lnk.getValue().getUuid());
			dl.setDataType(IJsonFeatureProcessor.OBSGROUP_DATATYPE);
			session.save(dl);
		}
		createdFeatures.add(wp);
	}

	/**
	 * Creates a user friendly message describing the actions 
	 * applied to the database
	 */
	@Override
	public String getMessage(Locale l) {
		if (createdFeatures.isEmpty())
			return null;
 
		return MessageFormat.format(Messages.COMPLETE_MSG.getMessage(l), createdFeatures.size(),
				createdFeatures.stream().map(wp->wp.getId()).collect(Collectors.joining(", "))); //$NON-NLS-1$
	}
	
	
	private Waypoint findIncidentLink(UUID providerUuid, ConservationArea ca, Session session, Locale l) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", DATATYPE) //$NON-NLS-1$
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
		
		if (!waypoint.getSourceId().equals(IndepedentIncidentSource.KEY)) {
			throw new Exception("Link is not independent incident"); //$NON-NLS-1$
		}
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return waypoint;
	}	
}
