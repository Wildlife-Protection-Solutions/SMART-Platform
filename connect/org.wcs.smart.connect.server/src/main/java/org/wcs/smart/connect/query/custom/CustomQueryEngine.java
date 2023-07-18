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

import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.observation.json.IJsonFeatureProcessor.LinkDataType;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.UuidUtils;

/**
 * For custom api for querying data
 * 
 * @author Emily
 *
 */

/*
 * https://localhost:8443/server/api/query/custom/patrol?patrol_uuid=82936209-fc04-4e92-b3d1-1d17704e3311
https://localhost:8443/server/api/query/custom/patrol?patrolleg_uuid=1eeb3ba7-88c2-4dd5-90d6-27ddca9eac7a
https://localhost:8443/server/api/query/custom/patrol?patrol_id=SMART_000002
https://localhost:8443/server/api/query/custom/patrol?waypoint_uuid=e11269a91f334267ab5de9931a620998
https://localhost:8443/server/api/query/custom/patrol?waypoint_date=2012-03-18
https://localhost:8443/server/api/query/custom/patrol?waypoint_date=2012-03-18:2012-03-19
https://localhost:8443/server/api/query/custom/patrol?waypoint_lastmodified=2021-12-17
https://localhost:8443/server/api/query/custom/patrol?waypoint_lastmodified=2021-12-15:2021-12-17
https://localhost:8443/server/api/query/custom/patrol?patrol_startdate=2012-03-14
https://localhost:8443/server/api/query/custom/patrol?patrol_startdate=2012-03-14:2012-03-19
https://localhost:8443/server/api/query/custom/patrol?patrol_enddate=2012-03-18
https://localhost:8443/server/api/query/custom/patrol?patrol_enddate=2012-03-18:2012-03-19


https://localhost:8443/server/api/query/custom/patrol?client_patrol_uuid
https://localhost:8443/server/api/query/custom/patrol?client_patrolleg_uuid



https://localhost:8443/server/api/query/custom/incident?incident_uuid=f6d1968b-ef69-4d3a-95b5-15dc4301356f
https://localhost:8443/server/api/query/custom/incident?incident_id=1
https://localhost:8443/server/api/query/custom/incident?waypoint_date=2020-04-02
https://localhost:8443/server/api/query/custom/incident?waypoint_date=2020-04-02:2020-04-20
https://localhost:8443/server/api/query/custom/incident?waypoint_lastmodified=2022-01-11
https://localhost:8443/server/api/query/custom/incident?waypoint_lastmodified=2022-01-11:2022-01-12

https://localhost:8443/server/api/query/custom/incident?incident_uuid=4b5cf0e8-e9fb-42d3-9fcd-b7ba3b9bbe6f
https://localhost:8443/server/api/query/custom/incident?client_incident_uuid=9da25003-dada-47a0-b42e-65fe01a2fd2d 
 */
public class CustomQueryEngine {

	//TODO Permissions for these custom queries
	
	public static int MAX_RESULTS = 1000;

	private final Logger logger = Logger.getLogger(CustomQueryEngine.class.getName());

	
	protected static final String WAYPOINT_FIELD = "waypoint"; //$NON-NLS-1$
	protected static final String CLIENT_UUID_FIELD = "client_uuid"; //$NON-NLS-1$
	protected static final String NAME_FIELD = "name"; //$NON-NLS-1$
	protected static final String ID_FIELD = "id"; //$NON-NLS-1$
	protected static final String UUID_FIELD = "uuid"; //$NON-NLS-1$
	
	protected static final String VALUE_FIELD = "value"; //$NON-NLS-1$
	protected static final String KEY_FIELD = "key"; //$NON-NLS-1$
	protected static final String LABEL_FIELD = "label"; //$NON-NLS-1$
	
	public static SmartConnectException TOO_MANY_ROWS = new SmartConnectException(Status.FORBIDDEN,
			"Query returns too many rows. Add additional filters to reduce number of records returned."); //$NON-NLS-1$
	
	/**
	 * Converts a wp to a json object 
	 * @param pw
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject convertWaypoint(Waypoint pw, Session session) {
		JSONObject jwp = new JSONObject();
		jwp.put(UUID_FIELD, UuidUtils.uuidToString(pw.getUuid()));
		jwp.put(ID_FIELD, pw.getId());
		jwp.put("date", pw.getDateTime().toLocalDate()); //$NON-NLS-1$
		jwp.put("time", pw.getDateTime().toLocalTime()); //$NON-NLS-1$
		jwp.put("x", pw.getX()); //$NON-NLS-1$
		jwp.put("y", pw.getY()); //$NON-NLS-1$
		jwp.put("raw_x", pw.getRawX()); //$NON-NLS-1$
		jwp.put("raw_y", pw.getRawY()); //$NON-NLS-1$
		jwp.put("distance", pw.getDistance()); //$NON-NLS-1$
		jwp.put("bearing", pw.getDirection()); //$NON-NLS-1$
		jwp.put("comment", pw.getComment()); //$NON-NLS-1$
		jwp.put("last_modified", pw.getLastModified()); //$NON-NLS-1$
		jwp.put("source",pw.getSourceId()); //$NON-NLS-1$
		jwp.put("conservation_area_uuid", UuidUtils.uuidToString(pw.getConservationArea().getUuid())); //$NON-NLS-1$
		
		JSONArray attachments = new JSONArray();
		jwp.put("attachments",attachments); //$NON-NLS-1$
		for (WaypointAttachment a : pw.getAttachments()) {
			JSONObject attachment = new JSONObject();
			attachment.put("filename", a.getFilename()); //$NON-NLS-1$
			attachment.put("signatureType", a.getSignatureType() == null ? null : a.getSignatureType().getKeyId()); //$NON-NLS-1$
			try {
				a.computeFileLocation(session);
				attachment.put("size", Files.size(a.getAttachmentFile())); //$NON-NLS-1$
			}catch (Exception ex) {
				logger.log(Level.WARNING,ex.getMessage(),ex);
			}
		}
		
		JSONArray jgroups = new JSONArray();
		jwp.put("observation_groups", jgroups); //$NON-NLS-1$
		
		for(WaypointObservationGroup g : pw.getObservationGroups()) {
			if (g.getObservations().isEmpty()) continue;
			
			JSONObject obsgroup = new JSONObject();
			obsgroup.put(UUID_FIELD, UuidUtils.uuidToString(g.getUuid()));
			
			//find a client uuid
			DataLink link = session
				.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
				.setParameter("smartUuid", g.getUuid()) //$NON-NLS-1$
				.setParameter("dataType", LinkDataType.OBSERVATION_GROUP.getKey()) //$NON-NLS-1$
				.uniqueResult();
			if (link != null) {
				obsgroup.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
			}
			
			JSONArray jobs = new JSONArray();
			
			for (WaypointObservation wo : g.getObservations()) {
				JSONObject job = new JSONObject();
				job.put(UUID_FIELD, UuidUtils.uuidToString(wo.getUuid()));
				
				//find a client uuid
				link = session
					.createQuery("FROM DataLink WHERE smartId = :smartUuid and dataType = :dataType", DataLink.class) //$NON-NLS-1$
					.setParameter("smartUuid", wo.getUuid()) //$NON-NLS-1$
					.setParameter("dataType", LinkDataType.OBSERVATION.getKey()) //$NON-NLS-1$
					.uniqueResult();
				if (link != null) {
					obsgroup.put(CLIENT_UUID_FIELD, UuidUtils.uuidToString(link.getProviderId()));
				}
				
				JSONObject category = new JSONObject();
				category.put("hkey",wo.getCategory().getHkey()); //$NON-NLS-1$
				
				Category c = wo.getCategory();
				while(c != null) {
					String prefix = LABEL_FIELD + "_" + c.getCategoryOrder() + "_"; //$NON-NLS-1$ //$NON-NLS-2$
					for (Label ll : c.getNames()) {
						category.put(prefix + ll.getLanguage().getCode(), ll.getValue());
					}
					c = c.getParent();
				}
				job.put("category", category); //$NON-NLS-1$
				
				JSONArray jattributes = new JSONArray();
				job.put("attributes",jattributes); //$NON-NLS-1$

				for (WaypointObservationAttribute at: wo.getAttributes()) {
					JSONObject joa = new JSONObject();
					jattributes.add(joa);
					joa.put("attribute_key", at.getAttribute().getKeyId()); //$NON-NLS-1$
					for (Label ll : at.getAttribute().getNames()) {
						joa.put("attribute_name_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
					}
					switch(at.getAttribute().getType()) {
					case BOOLEAN: 
						joa.put(VALUE_FIELD, at.getNumberValue() >= 0.5);
						break;
					case DATE:
						joa.put(VALUE_FIELD, at.getDateValue());
						break;
					case LIST:
						joa.put(VALUE_FIELD, at.getAttributeListItem().getKeyId());
						for (Label ll : at.getAttributeListItem().getNames()) {
							joa.put(LABEL_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
						}
						break;
					case MLIST:
						JSONArray array = new JSONArray();
						for (WaypointObservationAttributeList i : at.getAttributeListItems()) {
							JSONObject temp = new JSONObject();
							temp.put(VALUE_FIELD,i.getAttributeListItem().getKeyId());
							for (Label ll : i.getAttributeListItem().getNames()) {
								temp.put(LABEL_FIELD + "_" + ll.getLanguage().getCode(), ll.getValue()); //$NON-NLS-1$
							}
							array.add(temp);
						}
						joa.put(VALUE_FIELD, array);
						break;
					case NUMERIC:
						joa.put(VALUE_FIELD, at.getNumberValue());
						break;
					case TEXT:
						joa.put(VALUE_FIELD, at.getStringValue());
						break;
					case TREE:
						joa.put(VALUE_FIELD, at.getAttributeTreeNode().getHkey());
						AttributeTreeNode n  = at.getAttributeTreeNode();
						while(n != null) {
							int index = Category.hkeyLength(n.getHkey());
							String prefix = LABEL_FIELD + "_" + index + "_"; //$NON-NLS-1$ //$NON-NLS-2$
							for (Label ll : n.getNames()) {
								joa.put(prefix + ll.getLanguage().getCode(), ll.getValue());
							}
							n = n.getParent();
						}
						break;					
					}
				}
				
				JSONArray attachments2 = new JSONArray();
				job.put("attachments",attachments2); //$NON-NLS-1$
				for (ObservationAttachment a : wo.getAttachments()) {
					JSONObject attachment = new JSONObject();
					attachment.put("filename", a.getFilename()); //$NON-NLS-1$
//					attachment.put("signatureType", a.getSignatureType() == null ? null : a.getSignatureType().getKeyId());
					try {
						a.computeFileLocation(session);
						attachment.put("size", Files.size(a.getAttachmentFile())); //$NON-NLS-1$
					}catch (Exception ex) {
						logger.log(Level.WARNING,ex.getMessage(),ex);
					}
				}
				
				jobs.add(job);
			}
			
			obsgroup.put("observations", jobs); //$NON-NLS-1$
			jgroups.add(obsgroup);
		}
		return jwp;
	}
}
