/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.smartcollection.json;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.json.CtJsonObservationParser;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.incident.IncidentIdGenerator;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.smartcollect.model.ISmartCollectLabelProvider;
import org.wcs.smart.smartcollect.model.SmartCollectPackage;
import org.wcs.smart.smartcollect.model.SmartCollectUser;
import org.wcs.smart.smartcollect.model.SmartCollectUser.State;
import org.wcs.smart.smartcollect.model.SmartCollectWaypoint;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;
import org.wcs.smart.smartcollect.model.SmartCollectionMetadata;
import org.wcs.smart.smartcollection.json.SmartCollectJsonImportWarning.WarningType;

/**
 * JSON processor for SMARTCollect data.  Assumes all data for an individual
 * waypoint is included in the same file in order.  More than one waypoint can 
 * be in the file but observations must be in order.
 * 
 * @author Emily
 *
 */
public abstract class SmartCollectJsonProcessor implements IJsonProcessor {
	
	public static final Object FINISH_MESSAGE = new Object();
	
	protected List<JsonImportWarning> warnings;

	protected HashMap<String, WaypointObservationGroup> obsgroupmapping;
	protected Set<Waypoint> waypoints;
	
	public enum ProcessingOption{
		LOADDATA,
		ACCEPTANDLOAD,
		BLACKLISTANDDISCARD,
		DISCARD,
		VERIFYREQUEUE,
		CANCEL
	}
	
	protected ConservationArea ca;
	
	public SmartCollectJsonProcessor(ConservationArea ca) {
		this.ca = ca;
	}

	@Override
	public List<JsonImportWarning> getWarnings(){
		return this.warnings;
	}
	
	/**
	 * Logs an exception
	 * @param message
	 * @param ex
	 */
	protected abstract void logException(String message, Exception ex);

	/**
	 * Process the warnings however you want. Throw a cancelled exception if
	 * processing should stop
	 * 
	 * @param warnings
	 * @throws UserCancelledException
	 */
	protected void processWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		
	}

	/**
	 * Gets the user states
	 * @param users
	 * @return
	 */
	protected abstract Map<DeviceUser, SmartCollectUser> getUserState(Set<DeviceUser> users);
	
	/**
	 * 
	 * @param notok true - if all features should be discared, false - if features should
	 * be processed one by one and only discared based on user, null - process no features so
	 * file gets requeued
	 *  
	 * @return true if all users should be discared; null if data should be requeued and nothing loaded
	 */
	protected abstract Boolean processNotOkUsers(Set<SmartCollectUser> notok) throws Exception;
	
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session, Locale locale) throws Exception {
	
		warnings = new ArrayList<>();
		waypoints = new HashSet<>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();
		
		SmartCollectWaypoint currentWaypoint = null;
		
		//preprocess users
		Set<DeviceUser> users = new HashSet<>();
		for (JSONObject feature : features){
			JSONObject properties = (JSONObject) feature.get(CtJsonObservationParser.PROPERTIES_KEY);
			if (properties == null) continue;
			JSONObject sighting = (JSONObject)properties.get(CtJsonObservationParser.SIGHTINGS_KEY);
			if (sighting == null) continue;				
			
			String type = (String) sighting.get(CtJsonUtil.JsonKey.DATATYPE.key);
			// Validate data type
			if (!SmartCollectPackage.PACKAGE_TYPENAME.equalsIgnoreCase(type)) continue;
	
			
			String wpsource = ((String)sighting.get(SmartCollectionMetadata.USERNAMEMETADATA_KEY));
			if (wpsource == null) continue;
			
			String deviceId = ((String)properties.get(CtJsonObservationParser.DEVICE_ID));
			if (deviceId == null) continue;
			
			users.add(new DeviceUser(wpsource, deviceId));
		}
		
		if (users.isEmpty()) {
			return Collections.emptyList();
		}
		
		//check the state for the users
		Map<DeviceUser, SmartCollectUser> userStatus = getUserState(users);
		//if we have new users or validaton pending then we need more information
		Set<SmartCollectUser> notok = new HashSet<>();
		for (SmartCollectUser user : userStatus.values()) {
			if (user.getState() == State.NEW || user.getState() == State.VALIDATION_PENDING) {
				notok.add(user);
			}
		}
		
		Boolean discardall = Boolean.FALSE;
		
		if (!notok.isEmpty()) {
			discardall = processNotOkUsers(notok);
			//sending validation emails so requeue this file
			if (discardall == null) return Collections.emptyList();
		}
		
		Map<DeviceUser, Integer> blacklistCount = new HashMap<>();
		
		for (JSONObject feature : features){
			CtJsonObservationParser parser = new CtJsonObservationParser();
			if (CtJsonUtil.isTrackPoint(feature)) continue;
			
			try{
				JSONObject properties = (JSONObject) feature.get(CtJsonObservationParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(CtJsonObservationParser.SIGHTINGS_KEY);
				if (sighting == null) continue;				
				
				String type = (String) sighting.get(CtJsonUtil.JsonKey.DATATYPE.key);
				// Validate data type
				if (!SmartCollectPackage.PACKAGE_TYPENAME.equalsIgnoreCase(type)) continue;
				
				processedFeatures.add(feature);
				
				if (discardall) {
					//flag this feature as processed 
					continue;
				}
				
				//read group id
				String strGroupId = ((String) sighting.get(CtJsonUtil.JsonKey.OBSERVATION_GROUP.key));
				if (strGroupId == null || strGroupId.trim().isEmpty()) throw new Exception("No group id provided for independent incident.  Incident cannot be loaded"); //$NON-NLS-1$
				strGroupId = strGroupId.trim();
				
				String wpsource = ((String)sighting.get(SmartCollectionMetadata.USERNAMEMETADATA_KEY));
				if (wpsource == null) {
					warnings.add(new SmartCollectJsonImportWarning(WarningType.NO_USER, feature.toString()));
					continue;
				}
				String deviceId = ((String)properties.get(CtJsonObservationParser.DEVICE_ID));
				if (deviceId == null) {
					warnings.add(new SmartCollectJsonImportWarning(WarningType.MISSING_DEVICE_ID, feature.toString()));
					
					continue;
				}

				DeviceUser user = new DeviceUser(wpsource, deviceId);
				
				if (userStatus.get(user).getState() == State.BLACKLISTED) {
					Integer cnt = blacklistCount.get(user);
					if (cnt == null) {
						blacklistCount.put(user, 1);
					}else {
						blacklistCount.put(user, cnt+ 1);
					}
					
					continue;
				}
				
				//parse waypoint
				Waypoint parsedWp = parser.createWaypoint(feature, ca, session);
				parser.processImages(parsedWp, session);
				
				warnings.addAll(parser.getWarnings());

				
				if (currentWaypoint == null || (boolean)sighting.get(CtJsonUtil.JsonKey.NEW_WAYPOINT.key)) {
					currentWaypoint = new SmartCollectWaypoint();
					currentWaypoint.setWaypoint(parsedWp);
					currentWaypoint.setSource(wpsource);
					
					parsedWp.setConservationArea(ca);
					parsedWp.setSourceId(SmartCollectWaypointSource.KEY);
					
					String id = IncidentIdGenerator.INSTANCE.getNextIncidentId(session, ca, 
							Collections.singleton(parsedWp.getSourceId()), currentWaypoint.getWaypoint().getDateTime(), null);
					currentWaypoint.getWaypoint().setId(id);
					
					obsgroupmapping = new HashMap<>();
					obsgroupmapping.put(strGroupId, currentWaypoint.getWaypoint().getObservationGroups().get(0));
				}else {
					WaypointObservationGroup group = obsgroupmapping.get(strGroupId);
					if (group == null) {
						group = new WaypointObservationGroup();
						group.setWaypoint(currentWaypoint.getWaypoint());
						currentWaypoint.getWaypoint().getObservationGroups().add(group);
						group.setObservations(new ArrayList<>());
						obsgroupmapping.put(strGroupId, group);
					}
					
					for (WaypointObservation wo : parsedWp.getAllObservations()) {
						group.getObservations().add(wo);
						wo.setObservationGroup(group);
					}
				}
				if (currentWaypoint.getWaypoint().getUuid() == null) {
					session.persist(currentWaypoint.getWaypoint());
					session.persist(currentWaypoint);
				}
				
				waypoints.add(currentWaypoint.getWaypoint());
				
			}catch (Exception ex) {
				logException(ex.getMessage() + ": " +feature.toJSONString(), ex); //$NON-NLS-1$
				warnings.add(new JsonImportWarning(JsonImportWarning.Type.JSON_FEATURE_PARSE_ERROR, ex.getMessage()));
			}
		}
		
		if (discardall) {
			warnings.add(new SmartCollectJsonImportWarning(WarningType.FEATURE_DISCARDED, processedFeatures.size()));
		}
		for (Entry<DeviceUser, Integer> bcnt : blacklistCount.entrySet()) {
			warnings.add(new SmartCollectJsonImportWarning(WarningType.USER_BLACKLISTED_FEATURE_DISCARDED, bcnt.getKey().user + " (" + bcnt.getKey().deviceid + ")", bcnt.getValue())); //$NON-NLS-1$ //$NON-NLS-2$
			
		}
		//display warnings to user; this may throw a cancelled exception if the user doesn't want to proceed
		processWarnings(warnings);
				
		return processedFeatures;						
	}

	@Override
	public String getStatusMessage(Locale l) {
		if (waypoints.isEmpty() ) return null;
		
		StringBuilder sb = new StringBuilder();
		if (!waypoints.isEmpty()){
			sb.append(MessageFormat.format(SmartContext.INSTANCE.getClass(ISmartCollectLabelProvider.class).getLabel(FINISH_MESSAGE, l), waypoints.size()));
			sb.append("("); //$NON-NLS-1$
			for(Waypoint p : waypoints){
				sb.append(p.getId());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append(") "); //$NON-NLS-1$
		}
		
		return sb.toString();
	}

	protected class DeviceUser{
		private String user;
		private String deviceid;
		
		public DeviceUser(String user, String deviceId) {
			this.user = user;
			this.deviceid = deviceId;
		}
		
		public String getUser() {
			return this.user;
		}
		
		public String getDeviceId() {
			return this.deviceid;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(user, deviceid);
		}
		@Override
		public boolean equals(Object other) {
			if (other == null) return false;
			if (this == other) return true;
			if (other.getClass() != getClass()) return false;
			return Objects.equals(deviceid, ((DeviceUser)other).deviceid) &&
					Objects.equals(user, ((DeviceUser)other).user); 
		}
	}
}
