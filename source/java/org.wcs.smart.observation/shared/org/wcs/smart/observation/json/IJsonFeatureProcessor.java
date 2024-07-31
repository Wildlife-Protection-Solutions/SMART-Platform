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
package org.wcs.smart.observation.json;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.AttachmentTag;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.GeometryAttributeValue;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.IObservationLabelProvider;
import org.wcs.smart.observation.model.AttachmentTagLink;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.GeoJsonUtil;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

import jakarta.xml.bind.DatatypeConverter;


/**
 * Processor for loading data into the standard JSON format into 
 * the smart database.
 *  
 * @author Emily
 *
 */
public abstract class IJsonFeatureProcessor {

	public enum LinkDataType{
		OBSERVATION_GROUP("obsgroup"), //$NON-NLS-1$ 
		OBSERVATION("observation"); //$NON-NLS-1$ 
		
		private String key;
		
		LinkDataType(String key){
			this.key = key;
		}
		
		public String getKey() {
			return this.key;
		}
	}

	//JSON Keys
	public static final String JSON_OBSGROUPUUID_KEY = "groupUuid"; //$NON-NLS-1$
	public static final String JSON_INCIDENTUUID_KEY = "incidentUuid"; //$NON-NLS-1$
	public static final String JSON_OBSERVATIONUUID_KEY = "observationUuid"; //$NON-NLS-1$
	public static final String JSON_OBSERVER_KEY = "observer"; //$NON-NLS-1$
	public static final String JSON_SIGNATURETYPE_KEY = "signatureType"; //$NON-NLS-1$
	public static final String JSON_TAGS_KEY = "tags"; //$NON-NLS-1$
	public static final String JSON_SMARTATTRIBUTES = "smartAttributes";  //$NON-NLS-1$
	public static final String JSON_PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String JSON_SMARTDATATYPE = "smartDataType"; //$NON-NLS-1$
	public static final String JSON_SMARTFEATURETYPE = "smartFeatureType"; //$NON-NLS-1$
	public static final String JSON_DATETIME_KEY = "dateTime"; //$NON-NLS-1$
	
	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'H:m:s"; //$NON-NLS-1$
	
	//Waypoint Feature Types
	public enum SmartFeatureType{
		WAYPOINT_NEW("waypoint/new"), //$NON-NLS-1$
		WAYPOINT("waypoint"), //$NON-NLS-1$
		OBSERVATION("waypoint/observation"); //$NON-NLS-1$
		private String key;
		SmartFeatureType(String key){
			this.key = key;
		}
		public String getKey() {
			return this.key;
		}
	}
	
	
	public enum WaypointMetadata{
		DISTANCE("distance"), //$NON-NLS-1$
		BEARING("bearing"), //$NON-NLS-1$
		COMMENT("comment"), //$NON-NLS-1$
		CMUUID ("cmUuid"), //$NON-NLS-1$
		OBSERVER(JSON_OBSERVER_KEY); 
		
		String key;
		
		WaypointMetadata(String key) {
			this.key = key;
		}
		public String getKey() {
			return this.key;
		}
	}
	
	public enum Messages{
		EMPLOYEE_NOT_FOUND,
		CATEGORY_NOT_FOUND,
		ATTRIBUTE_NOT_FOUND,
		INVALID_BOOLEAN_ATTRIBUTE,
		INVALID_DATE_ATTRIBUTE,
		INVALID_TIME_ATTRIBUTE,
		INVALID_LIST_ATTRIBUTE,
		INVALID_MLIST_ATTRIBUTE,
		INVALID_MLIST2_ATTRIBUTE,
		INVALID_TREE_ATTRIBUTE,
		INVALID_GEOMETRY_ATTRIBUTE,
		INVALID_POLYGON_ATTRIBUTE,
		INVALID_LINE_ATTRIBUTE,
		INVALID_GEOMETRY_SRC_ATTRIBUTE,
		INVALID_NUMBER_ATTRIBUTE,
		SIGNATURE_TYPE_NOT_FOUND,
		ATTACHMENT_TAG_NOT_FOUND,
		CM_MISSING,
		INVALID_CM_UUID;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IObservationLabelProvider.class).getLabel(this, l);
		}
	}
	protected Logger logger = Logger.getLogger(IJsonFeatureProcessor.class.getName());

	/**
	 * Set of warnings generated when processing data
	 */
	protected List<String> warnings = new ArrayList<>();
	
	/*
	 * Set of temporary files for attachments; these are dropped
	 * when dispose is called
	 */
	protected Set<Path> tempFiles = new HashSet<>();
	
	/**
	 * 
	 * @param featureType
	 * @return true if processor can process feature type, false otherwise
	 */
	public abstract boolean canProcess(String featureType);
	
	/**
	 * Processes the given GeoJSON feature as new database features.  
	 * Returns a string that describes which features have been processed. 
	 * 
	 * @param feature
	 * @param ca
	 * @param session
	 * @throws Exception
	 */
	public abstract void processFeature(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception;

	
	/**
	 * disposes of any temporary resources created while loading data
	 */
	public void dispose() {
		for (Path p : tempFiles) {
			try {
				Files.delete(p);
			}catch (IOException ex) {
				logger.log(Level.WARNING, ex.getMessage(), ex);
			}
		}
	}
	
	/**
	 * 
	 * @return a description of updates made to the database
	 */
	public String getMessage(Locale locale){
		return null;
	}

	/**
	 * A set of warnings generated while processing data
	 * @return
	 */
	public List<String> getWarnings() {
		return this.warnings;
	}

	/**
	 * Converts the "dateTime" property into a local date time
	 * @param properties
	 * @return
	 */
	protected LocalDateTime getDateTime(JSONObject properties) {
		if (!properties.containsKey(JSON_DATETIME_KEY)) return null;
		
		String strDateTime = properties.get(JSON_DATETIME_KEY).toString();

		DateTimeFormatter pattern = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
		LocalDateTime wpdatetime = LocalDateTime.parse(strDateTime, pattern);
		return wpdatetime;
	}
	
	
	/**
	 * reads geometry from feature and
	 *  converts it to a coordinate
	 * 
	 * @param properties
	 * @return
	 */
	protected Coordinate getPosition(JSONObject feature) {
		if (feature.containsKey("geometry")) { //$NON-NLS-1$
			JSONArray cs = (JSONArray) ((JSONObject) feature.get("geometry")).get("coordinates"); //$NON-NLS-1$ //$NON-NLS-2$
			double x = ((Number) cs.get(0)).doubleValue();
			double y = ((Number) cs.get(1)).doubleValue();
			
			LocalDateTime dt = getDateTime((JSONObject)feature.get(JSON_PROPERTIES));
			Long time = SharedUtils.toLongTime(dt);
			
			return new Coordinate(x, y, time);
		}
		return null;
	}
	
	/**
	 * Converts a JSON object that represents an observation into an observation
	 * feature. Sets uuids if provided; they should be set to null before
	 * saving. Will return null if category cannot be parsed.
	 * 
	 * @param properties
	 * @param ca
	 * @param session
	 * @param locale
	 * @return
	 * @throws IOException
	 */
	protected WaypointObservation createWaypointObservation(JSONObject properties, ConservationArea ca, Session session, Locale locale) throws IOException{
		
		WaypointObservation wo = new WaypointObservation();
		wo.setAttachments(new ArrayList<>());
		wo.setAttributes(new ArrayList<>());
		
		if (properties.containsKey(JSON_OBSERVATIONUUID_KEY)) {
			//observation to update
			UUID observationUuid = UuidUtils.stringToUuid(properties.get(JSON_OBSERVATIONUUID_KEY).toString()); 
			wo.setUuid(observationUuid);
		}
		
		
		if (properties.containsKey(JSON_OBSERVER_KEY)) { 
			String euuid = properties.get(JSON_OBSERVER_KEY).toString(); 
			Employee e = findEmployee(euuid, ca, session);
			if (e == null) {
				warnings.add(MessageFormat
						.format(Messages.EMPLOYEE_NOT_FOUND.getMessage(locale), euuid));
			} else {
				wo.setObserver(e);
			}
		}

		Category c = null;
		String categoryKey = null;
		if (properties.containsKey("category")) { //$NON-NLS-1$
			categoryKey = properties.get("category").toString(); //$NON-NLS-1$
			c = findCategory(categoryKey, ca, session);
		}
		
		if (c == null) {
			warnings.add(MessageFormat.format(
					Messages.CATEGORY_NOT_FOUND.getMessage(locale),
					categoryKey));
			return null;
		}
		if (properties.containsKey("attachments")) { //$NON-NLS-1$
			List<WaypointAttachment> attachments = parseAttachments(properties, ca, session, locale);
			attachments.forEach(a->{
				ObservationAttachment oa = new ObservationAttachment();
				oa.setFilename(a.getFilename());
				oa.setCopyFromLocation(a.getCopyFromLocation());
				oa.setObservation(wo);
				oa.setSignatureType(a.getSignatureType());
				oa.setAttachmentTags(new ArrayList<>());
				for (AttachmentTagLink link : a.getAttachmentTags()) {
					AttachmentTagLink newLink = new AttachmentTagLink();
					newLink.setObservationAttachment(oa);
					newLink.setTag(link.getTag());
					oa.getAttachmentTags().add(newLink);
				}
				wo.getAttachments().add(oa);
			});
		}
		
		wo.setCategory(c);
		if (properties.containsKey("attributes")) { //$NON-NLS-1$
			JSONObject obatts = (JSONObject) properties.get("attributes"); //$NON-NLS-1$
			for (Object s : obatts.keySet()) {
				String attributeKey = s.toString();
				
				Object attributeValue = obatts.get(s);
				if (attributeValue == null) continue;
				
				Attribute a = findAttribute(attributeKey, c, session);
				if (a == null) {
					warnings.add(MessageFormat.format(
							Messages.ATTRIBUTE_NOT_FOUND.getMessage(locale),
							attributeKey, c.getName()));
				} else {
					WaypointObservationAttribute woa = new WaypointObservationAttribute();
					woa.setAttribute(a);
					try {
						updateObservationAttribute(woa, attributeValue, locale);
						wo.getAttributes().add(woa);
						woa.setObservation(wo);
					} catch (Exception ex) {
						warnings.add(ex.getMessage());
					}
				}
			}
		}
		
		return wo;
	}

		
	/**
	 * Converts a GeoJSON feature into a SMART Waypoint object (with uuids
	 * provided in the JSON).
	 * 
	 * NOTE: incidentUuid, groupUuids, observationUuids are provided if the
	 * appear in the JSON.  These uuids should be set to NULL
	 * before saving the object to the database.
	 * 
	 * @param feature
	 * @param ca
	 * @param session
	 * @return
	 */
	protected Waypoint createWaypoint(JSONObject feature, ConservationArea ca, Session session, Locale locale) throws IOException{

		// get cordinates
		
		Coordinate location = getPosition(feature);
		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);

		LocalDateTime wpdatetime = getDateTime(props);

		JSONObject atts = (JSONObject) props.get(JSON_SMARTATTRIBUTES);

		Waypoint wp = new Waypoint();
		wp.setConservationArea(ca);
		if (wpdatetime != null) wp.setDateTime(wpdatetime);
		if (location != null) {
			wp.setRawX(location.x);
			wp.setRawY(location.y);
		}

		if (atts.containsKey("incidentId")) { //$NON-NLS-1$
			wp.setId(atts.get("incidentId").toString()); //$NON-NLS-1$
		}else if (atts.containsKey("id")) { //$NON-NLS-1$
			wp.setId(atts.get("id").toString()); //$NON-NLS-1$
		}

		if (atts.containsKey(WaypointMetadata.DISTANCE.key)) { 
			wp.setDistance(((Number) atts.get(WaypointMetadata.DISTANCE.key)).floatValue()); 
		}

		if (atts.containsKey(WaypointMetadata.BEARING.key)) { 
			wp.setDirection(((Number) atts.get(WaypointMetadata.BEARING.key)).floatValue()); 
		}

		if (atts.containsKey(WaypointMetadata.COMMENT.key)) { 
			wp.setComment(atts.get(WaypointMetadata.COMMENT.key).toString()); 
		}

		if (atts.containsKey(WaypointMetadata.CMUUID.key)) {
			UUID uuid = null; 
			try {
				uuid = UuidUtils.stringToUuid((String)atts.get(WaypointMetadata.CMUUID.key));
			}catch (Exception ex) {
				throw new IOException(MessageFormat.format(Messages.INVALID_CM_UUID.getMessage(locale), atts.get(WaypointMetadata.CMUUID.key)));
			}
			ConfigurableModel cm = session.get(ConfigurableModel.class, uuid);
			if (cm == null) {
				warnings.add(MessageFormat.format(Messages.CM_MISSING.getMessage(locale), (String)atts.get(WaypointMetadata.CMUUID.key)));
			}
			wp.setSourceConfigurableModel(cm);
		}
		
		wp.setAttachments(new ArrayList<>());

		if (atts.containsKey("attachments")) { //$NON-NLS-1$
			List<WaypointAttachment> attachments = parseAttachments(atts, ca, session, locale);
			attachments.forEach(a->{
				a.setWaypoint(wp);
				wp.getAttachments().add(a);
			});
		}
		
		if (atts.containsKey(JSON_INCIDENTUUID_KEY)) { 
			//incident to update 
			UUID incidentUuid = UuidUtils.stringToUuid((String)atts.get(JSON_INCIDENTUUID_KEY)); 
			wp.setUuid(incidentUuid);
		}
		
		Employee wpobserver = null;
		if (atts.containsKey(JSON_OBSERVER_KEY)) { 
			String euuid = atts.get(JSON_OBSERVER_KEY).toString(); 
			wpobserver = findEmployee(euuid, ca, session);
			if (wpobserver == null) {
				warnings.add(MessageFormat
						.format(Messages.EMPLOYEE_NOT_FOUND.getMessage(locale), euuid));
			}
		}
		
		wp.setObservationGroups(new ArrayList<>());
		if (atts.containsKey("observationGroups")) { //$NON-NLS-1$
			JSONArray groups = (JSONArray) atts.get("observationGroups"); //$NON-NLS-1$
			for (int j = 0; j < groups.size(); j++) {
				JSONObject group = (JSONObject) groups.get(j);

				WaypointObservationGroup wgroup = new WaypointObservationGroup();
		
				if (group.containsKey(JSON_OBSGROUPUUID_KEY)) { 
					//group to update
					UUID groupUuid = UuidUtils.stringToUuid(group.get(JSON_OBSGROUPUUID_KEY).toString()); 
					wgroup.setUuid(groupUuid);
				}
				
				wgroup.setWaypoint(wp);
				
				wgroup.setObservations(new ArrayList<>());

				if (group.containsKey("observations")) { //$NON-NLS-1$
					JSONArray obs = (JSONArray) group.get("observations"); //$NON-NLS-1$

					for (int i = 0; i < obs.size(); i++) {

						JSONObject ob = (JSONObject) obs.get(i);
						WaypointObservation wo = createWaypointObservation(ob, ca, session, locale);
						if (wo != null) {
							wo.setObservationGroup(wgroup);	
							if (wo.getObserver() == null) wo.setObserver(wpobserver);
							wgroup.getObservations().add(wo);
						}
					}
				}
				
				if (wgroup.getObservations().size() > 0) {
					wp.getObservationGroups().add(wgroup);
				}
			}
		}
		return wp;

	}

	/**
	 * If the JSON represented by the attributes object contains the observer key, 
	 * this updates the observer fields of all observations in the waypoint.
	 * 
	 * @param toUpdate
	 * @param attributes
	 * @param ca
	 * @param session
	 * @param l
	 * @throws IOException
	 */
	protected void updateObserver(Waypoint toUpdate, JSONObject attributes, ConservationArea ca, Session session, Locale l) throws IOException{
		if (attributes.containsKey(JSON_OBSERVER_KEY)) {
			Employee wpobserver = null;
			String euuid = attributes.get(JSON_OBSERVER_KEY).toString(); 
			wpobserver = findEmployee(euuid, ca, session);
			if (wpobserver == null) {
				warnings.add(MessageFormat.format(IJsonFeatureProcessor.Messages.EMPLOYEE_NOT_FOUND.getMessage(l), euuid));
			}else {
				//update all observations
				for (WaypointObservation wo : toUpdate.getAllObservations()) {
					wo.setObserver(wpobserver);
				}
			}
		}
	}
	/*
	 * Parses waypoint attchments from JSON object.
	 */
	private List<WaypointAttachment> parseAttachments(JSONObject atts, ConservationArea ca, Session session, Locale locale) throws IOException{
		JSONArray jattachments = (JSONArray)atts.get("attachments"); //$NON-NLS-1$
		List<WaypointAttachment> attachments = new ArrayList<>();
		
		for (int i = 0; i < jattachments.size(); i ++) {
			JSONObject jattachment = (JSONObject)jattachments.get(i);
			
			String fname = (String) jattachment.get("filename"); //$NON-NLS-1$
			String data = (String)jattachment.get("data"); //base64 encoded //$NON-NLS-1$
			String sigtype = null;
			JSONArray tags = null;
			if (jattachment.containsKey(JSON_SIGNATURETYPE_KEY)) { 
				sigtype = (String) jattachment.get(JSON_SIGNATURETYPE_KEY);
			}
			if (jattachment.containsKey(JSON_TAGS_KEY)) {
				tags = (JSONArray) jattachment.get(JSON_TAGS_KEY);
			}
			//decode data
			byte[] decoded = DatatypeConverter.parseBase64Binary(data);
			
			//write to temp file
			Path tempFile =  Files.createTempFile("", fname); //$NON-NLS-1$
			tempFiles.add(tempFile);
			try(OutputStream writer = Files.newOutputStream(tempFile)){
				writer.write(decoded);
			}
			
			WaypointAttachment attachment = new WaypointAttachment();
			attachment.setFilename(fname);
			attachment.setCopyFromLocation(tempFile);
			attachment.setAttachmentTags(new ArrayList<>());
			attachments.add(attachment);
			
			if (sigtype != null) {
				SignatureType stype = QueryFactory.buildQuery(session, SignatureType.class,
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"keyId", sigtype}).uniqueResult(); //$NON-NLS-1$	
				if (stype != null) {
					attachment.setSignatureType(stype);
				}else {
					warnings.add(MessageFormat.format(Messages.SIGNATURE_TYPE_NOT_FOUND.getMessage(locale), sigtype));
				}
			}
			
			if (tags != null) {
				for (Object tag: tags) {
					String stag = (String)tag;
					
					AttachmentTag at = QueryFactory.buildQuery(session, AttachmentTag.class,
							new Object[] {"conservationArea", ca}, //$NON-NLS-1$
							new Object[] {"keyId", stag}).uniqueResult(); //$NON-NLS-1$	
					if (at != null) {
						AttachmentTagLink link = new AttachmentTagLink();
						link.setTag(at);
						link.setWaypointAttachment(attachment);
						attachment.getAttachmentTags().add(link);
					}else {
						warnings.add(MessageFormat.format(Messages.ATTACHMENT_TAG_NOT_FOUND.getMessage(locale), stag));
					}
					
				}
			}
		}
		return attachments;
	}
	
	/*
	 * Finds a cateogry with the given hkey.  The hkey can optionally have a
	 * dot at the end.  Will return null if no category found. 
	 */
	private Category findCategory(String hkey, ConservationArea ca, Session session) {
		if (!hkey.endsWith(".")) hkey = hkey + ".";  //$NON-NLS-1$//$NON-NLS-2$
		Category c = QueryFactory.buildQuery(session, Category.class, new Object[] { "conservationArea", ca }, //$NON-NLS-1$
				new Object[] { "hkey", hkey }).uniqueResult(); //$NON-NLS-1$
		return c;
	}

	/*
	 * Find attribute with the given key in the provided category. Will return null
	 * if attribute not found.
	 */
	private Attribute findAttribute(String key, Category c, Session session) {
		for (CategoryAttribute a : c.getAllAttributes()) {
			if (a.getAttribute().getKeyId().equals(key)) return a.getAttribute();
		}
		return null;
	}

	/*
	 * Finds the employee with the given uuid or returns null if not found.
	 */
	protected Employee findEmployee(String uuid, ConservationArea ca, Session session) {
		UUID euuid = UuidUtils.stringToUuid(uuid);
		Employee e = session.get(Employee.class, euuid);
		if (e != null && e.getConservationArea().equals(ca))
			return e;
		return null;
	}

	/**
	 * Updates the value of the observation attribute. Throws an exception
	 * if a valid value cannot be parsed from provided object.
	 * 
	 * @param a item to update
	 * @param value new value
	 * @param locale
	 * @throws Exception
	 */
	private void updateObservationAttribute(WaypointObservationAttribute a, Object value, Locale locale) throws Exception {
		if (value == null) return;
	
		switch (a.getAttribute().getType()) {
		case BOOLEAN:
			try {
				if (parseBoolean(value)) {
					a.setNumberValue(1.0);
				}else {
					a.setNumberValue(0.0);
				}
			}catch (Exception ex) {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_BOOLEAN_ATTRIBUTE.getMessage(locale),
						value.toString(), a.getAttribute().getName()));
			}
			break;
		case DATE:
			try {
				a.setDateValue(parseDate(value));
			}catch (Exception ex) {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_DATE_ATTRIBUTE.getMessage(locale),
						value.toString(), a.getAttribute().getName()));
			}
			break;
		case TIME:
			try {
				a.setTimeValue(parseTime(value));
			}catch (Exception ex) {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_TIME_ATTRIBUTE.getMessage(locale),
						value.toString(), a.getAttribute().getName()));
			}
			break;
		case LIST:
			String key = value.toString();
			AttributeListItem item = null;
			for (AttributeListItem li : a.getAttribute().getAttributeList()) {
				if (li.getKeyId().equalsIgnoreCase(key)) {
					item = li;
				}
			}
			if (item != null) {
				a.setAttributeListItem(item);
			} else {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_LIST_ATTRIBUTE.getMessage(locale),
						 key, a.getAttribute().getName()));
			}

			break;
		case MLIST:
			if (value instanceof JSONArray) {
				List<AttributeListItem> items = new ArrayList<>();
				for (int i = 0; i < ((JSONArray) value).size(); i++) {
					String itemkey = ((JSONArray) value).get(i).toString();
					item = null;
					for (AttributeListItem li : a.getAttribute().getAttributeList()) {
						if (li.getKeyId().equalsIgnoreCase(itemkey)) {
							item = li;
						}
					}
					if (item == null) {
						throw new Exception(MessageFormat.format(
								Messages.INVALID_MLIST_ATTRIBUTE.getMessage(locale),
								itemkey, a.getAttribute().getName()));
					} else {
						items.add(item);
					}
				}
				a.setAttributeListItems(new ArrayList<>());
				for (AttributeListItem li : items) {
					WaypointObservationAttributeList al = new WaypointObservationAttributeList();
					al.setAttributeLisItem(li);
					al.setObservationAttribute(a);
					a.getAttributeListItems().add(al);

				}
			} else {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_MLIST2_ATTRIBUTE.getMessage(locale),
						value.toString(), a.getAttribute().getName()));
			}
			break;
		case NUMERIC:
			try {
				a.setNumberValue(parseNumeric(value));
			} catch (Exception ex) {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_NUMBER_ATTRIBUTE.getMessage(locale),
						value.toString(), a.getAttribute().getName()));
			}
			break;
		case TEXT:
			a.setStringValue(value.toString());
			break;
		case TREE:

			String tkey = value.toString();
			if (!tkey.endsWith(".")) tkey = tkey + ".";  //$NON-NLS-1$//$NON-NLS-2$
			
			Deque<AttributeTreeNode> q = new ArrayDeque<>();
			q.addAll(a.getAttribute().getTree());
			AttributeTreeNode found = null;
			while (!q.isEmpty()) {
				AttributeTreeNode n = q.remove();
				if (n.getHkey().equalsIgnoreCase(tkey)) {
					found = n;
					break;
				}
				q.addAll(n.getChildren());
			}
			if (found != null) {
				a.setAttributeTreeNode(found);
			} else {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_TREE_ATTRIBUTE.getMessage(locale),
						tkey,a.getAttribute().getName()));
			}
			break;
		case LINE:
		case POLYGON:
			JSONObject jvalue = (JSONObject) value;
			
			Attribute.GeometrySource source = Attribute.GeometrySource.UNKNOWN;
			try {
				JSONObject properties = (JSONObject) jvalue.get("properties"); //$NON-NLS-1$
				if (properties != null) {
					String src = properties.get("source").toString(); //$NON-NLS-1$
					source = Attribute.GeometrySource.valueOf(src.toUpperCase());
				}
			}catch (Exception ex) {
				warnings.add(MessageFormat.format(
						Messages.INVALID_GEOMETRY_SRC_ATTRIBUTE.getMessage(locale),
						source, a.getAttribute().getName()));
			}
			Geometry geom = null;
			try {
				geom = GeoJsonUtil.toJTSGeometry((JSONObject) jvalue.get("geometry")); //$NON-NLS-1$
			}catch (Exception ex) {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_GEOMETRY_ATTRIBUTE.getMessage(locale),
						a.getAttribute().getName()));
			}
			if (a.getAttribute().getType() == Attribute.AttributeType.LINE) {
				if ( !(geom instanceof LineString || geom instanceof MultiLineString) ) {
					throw new Exception(MessageFormat.format(
							Messages.INVALID_LINE_ATTRIBUTE.getMessage(locale),
							a.getAttribute().getName()));
				}
			}else if (a.getAttribute().getType() == Attribute.AttributeType.POLYGON) {
				if ( !(geom instanceof Polygon || geom instanceof MultiPolygon) ) {
					throw new Exception(MessageFormat.format(
							Messages.INVALID_POLYGON_ATTRIBUTE.getMessage(locale),
							a.getAttribute().getName()));
				} 
			}
			
			
			a.setGeometry(new GeometryAttributeValue(geom,source));
			break;			
		}
	}
	
	/**
	 * Parses the object as boolean or throws an exception
	 * @param value
	 * @return
	 * @throws Exception
	 */
	protected Boolean parseBoolean( Object value) throws Exception{
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof Double) {
			return (((Double)value) > 0.5);
		}else if (value instanceof String) {
			if (((String) value).equalsIgnoreCase("true")) { //$NON-NLS-1$
				return Boolean.TRUE;
			}else if (((String) value).equalsIgnoreCase("false")) { //$NON-NLS-1$
				return Boolean.FALSE;
			}
		}
		throw new Exception(MessageFormat.format("Could not parse boolean value from {0}", value.toString())); //$NON-NLS-1$
	}

	/**
	 * Parses the object as a date or throws an exception
	 * @param value
	 * @return
	 * @throws Exception
	 */
	protected LocalDate parseDate( Object value ) throws Exception{
		if (value instanceof String) {
			return LocalDate.parse((String) value, DateTimeFormatter.ISO_LOCAL_DATE);
		} else if (value instanceof LocalDate) {
			return ((LocalDate) value);
		}
		throw new Exception(MessageFormat.format("Could not parse date value from {0}", value.toString())); //$NON-NLS-1$
	}
	
	/**
	 * Parses the object as a time or throws an exception
	 * @param value
	 * @return
	 * @throws Exception
	 */
	protected LocalTime parseTime( Object value ) throws Exception{
		if (value instanceof String) {
			return LocalTime.parse((String) value, DateTimeFormatter.ISO_LOCAL_TIME);
		} else if (value instanceof LocalDate) {
			return ((LocalTime) value);
		}
		throw new Exception(MessageFormat.format("Could not parse time value from {0}", value.toString())); //$NON-NLS-1$
	}

	/**
	 * Cobverts the object to a double or throws an exception
	 * 
	 * @param value
	 * @return
	 * @throws Exception
	 */
	protected Double parseNumeric( Object value ) throws Exception{
		if (value instanceof Number) return (((Number) value).doubleValue());
		throw new Exception(MessageFormat.format("Could not parse number value from {0}", value.toString())); //$NON-NLS-1$
	}
	
	/**
	 * Find the waypoint observation group that is linked to the given provider uuid.  Will
	 * return null if no link found.
	 * 
	 * @param providerUuid
	 * @param ca
	 * @param session
	 * @return
	 */
	protected WaypointObservationGroup findWaypointObservationGroup(UUID providerUuid, ConservationArea ca, Session session) {
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", LinkDataType.OBSERVATION_GROUP.getKey()) //$NON-NLS-1$
			.uniqueResult();
		
		if (link == null) return null;
		
		WaypointObservationGroup p = session.get(WaypointObservationGroup.class, link.getSmartId());
		if (p == null) {
			session.remove(link);
			return null;
		}
		if (!p.getWaypoint().getConservationArea().equals(ca)) return null;
		
		//update last modified
		link.setLastModified(LocalDateTime.now());
		return p;
	}
	
	/**
	 * Find the waypoint observation that is linked to the given provider uuid.  Will
	 * return null if no link found.
	 * 
	 * @param providerUuid
	 * @param ca
	 * @param session
	 * @return
	 * @throws Exception
	 */
	protected WaypointObservation findObservationLink(UUID providerUuid, ConservationArea ca, Session session) throws Exception{
		DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
			.setParameter("ca",ca) //$NON-NLS-1$
			.setParameter("puuid", providerUuid) //$NON-NLS-1$
			.setParameter("datatype", LinkDataType.OBSERVATION.getKey()) //$NON-NLS-1$
			.uniqueResult();
		
		if (link == null) return null;
		
		WaypointObservation observation = session.get(WaypointObservation.class, link.getSmartId());
		if (observation == null) {
			session.remove(link);
			return null;
		}
		if (!observation.getWaypoint().getConservationArea().equals(ca)) return null;
		
		link.setLastModified(LocalDateTime.now());
		return observation;
	}	
}
