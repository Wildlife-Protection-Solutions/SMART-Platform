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

import javax.xml.bind.DatatypeConverter;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.IObservationLabelProvider;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.UuidUtils;


/**
 * Processor for loading data into the standard JSON format into 
 * the smart database.
 *  
 * @author Emily
 *
 */
public abstract class IJsonFeatureProcessor {

	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'H:m:s"; //$NON-NLS-1$
	public static final String JSON_PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String JSON_SMARTDATATYPE = "smartDataType"; //$NON-NLS-1$
	public static final String JSON_SMARTFEATURETYPE = "smartFeatureType"; //$NON-NLS-1$
	public static final String JSON_FT_OBSERVATION = "observation"; //$NON-NLS-1$
	
	public enum Messages{
		EMPLOYEE_NOT_FOUND,
		CATEGORY_NOT_FOUND,
		ATTRIBUTE_NOT_FOUND,
		INVALID_BOOLEAN_ATTRIBUTE,
		INVALID_DATE_ATTRIBUTE,
		INVALID_LIST_ATTRIBUTE,
		INVALID_MLIST_ATTRIBUTE,
		INVALID_MLIST2_ATTRIBUTE,
		INVALID_TREE_ATTRIBUTE,
		INVALID_NUMBER_ATTRIBUTE;
		
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
	 * Processes the given GeoJSON feature.  Returns a string
	 * that describes which features have been processed
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
	 * Converts a GeoJSON feature into a SMART Waypoint object.
	 * 
	 * NOTE: incidentUuid and groupUuids are provided if the
	 * appear in the JSON.  These uuids should be reset
	 * before saving the object to the database.
	 * 
	 * @param feature
	 * @param ca
	 * @param session
	 * @return
	 */
	protected Waypoint createWaypoint(JSONObject feature, ConservationArea ca, Session session, Locale locale) throws IOException{

		// get cordinates
		JSONArray cs = (JSONArray) ((JSONObject) feature.get("geometry")).get("coordinates"); //$NON-NLS-1$ //$NON-NLS-2$
		double x = (Double) cs.get(0);
		double y = (Double) cs.get(1);

		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);

		String strDateTime = props.get("dateTime").toString(); //$NON-NLS-1$

		DateTimeFormatter pattern = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
		LocalDateTime wpdatetime = LocalDateTime.parse(strDateTime, pattern);

		JSONObject atts = (JSONObject) props.get("smartAttributes"); //$NON-NLS-1$

		Waypoint wp = new Waypoint();
		wp.setConservationArea(ca);
		wp.setDateTime(wpdatetime);
		wp.setRawX(x);
		wp.setRawY(y);

		if (atts.containsKey("incidentId")) { //$NON-NLS-1$
			wp.setId(atts.get("incidentId").toString()); //$NON-NLS-1$
		}

		if (atts.containsKey("distance")) { //$NON-NLS-1$
			wp.setDistance(((Number) atts.get("distance")).floatValue()); //$NON-NLS-1$
		}

		if (atts.containsKey("direction")) { //$NON-NLS-1$
			wp.setDirection(((Number) atts.get("direction")).floatValue()); //$NON-NLS-1$
		}

		if (atts.containsKey("comment")) { //$NON-NLS-1$
			wp.setComment(atts.get("comment").toString()); //$NON-NLS-1$
		}

		wp.setAttachments(new ArrayList<>());

		if (atts.containsKey("attachments")) { //$NON-NLS-1$
			List<WaypointAttachment> attachments = parseAttachments(atts);
			attachments.forEach(a->{
				a.setWaypoint(wp);
				wp.getAttachments().add(a);
			});
		}
		
		if (atts.containsKey("incidentUuid")) { //$NON-NLS-1$
			//incident to update 
			UUID incidentUuid = UuidUtils.stringToUuid((String)atts.get("incidentUuid")); //$NON-NLS-1$
			wp.setUuid(incidentUuid);
		}
		
		wp.setObservationGroups(new ArrayList<>());
		if (atts.containsKey("groups")) { //$NON-NLS-1$
			JSONArray groups = (JSONArray) atts.get("groups"); //$NON-NLS-1$
			for (int j = 0; j < groups.size(); j++) {
				JSONObject group = (JSONObject) groups.get(j);

				WaypointObservationGroup wgroup = new WaypointObservationGroup();
		
				if (group.containsKey("groupUuid")) { //$NON-NLS-1$
					//group to update
					UUID groupUuid = UuidUtils.stringToUuid((String)atts.get(group.get("groupUuid"))); //$NON-NLS-1$
					wgroup.setUuid(groupUuid);
				}
				
				wgroup.setWaypoint(wp);
				wp.getObservationGroups().add(wgroup);
				wgroup.setObservations(new ArrayList<>());

				if (group.containsKey("observations")) { //$NON-NLS-1$
					JSONArray obs = (JSONArray) group.get("observations"); //$NON-NLS-1$

					for (int i = 0; i < obs.size(); i++) {

						WaypointObservation wo = new WaypointObservation();
						wo.setObservationGroup(wgroup);
						wo.setAttachments(new ArrayList<>());
						wo.setAttributes(new ArrayList<>());
						

						JSONObject ob = (JSONObject) obs.get(i);

						if (ob.containsKey("observer")) { //$NON-NLS-1$
							String euuid = ob.get("observer").toString(); //$NON-NLS-1$
							Employee e = findEmployee(euuid, ca, session);
							if (e == null) {
								warnings.add(MessageFormat
										.format(Messages.EMPLOYEE_NOT_FOUND.getMessage(locale), euuid));
							} else {
								wo.setObserver(e);
							}
						}

						String categoryKey = ob.get("category").toString(); //$NON-NLS-1$
						Category c = findCategory(categoryKey, ca, session);

						if (ob.containsKey("attachments")) { //$NON-NLS-1$
							List<WaypointAttachment> attachments = parseAttachments(ob);
							attachments.forEach(a->{
								ObservationAttachment oa = new ObservationAttachment();
								oa.setFilename(a.getFilename());
								oa.setCopyFromLocation(a.getCopyFromLocation());
								oa.setObservation(wo);
								wo.getAttachments().add(oa);
							});
						}

						if (c == null) {
							warnings.add(MessageFormat.format(
									Messages.CATEGORY_NOT_FOUND.getMessage(locale),
									categoryKey));

						} else {
							wo.setCategory(c);

							wgroup.getObservations().add(wo);
							
							if (ob.containsKey("attributes")) { //$NON-NLS-1$
								JSONObject obatts = (JSONObject) ob.get("attributes"); //$NON-NLS-1$
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
						}
					}
				}
			}
		}
		return wp;

	}

	private List<WaypointAttachment> parseAttachments(JSONObject atts) throws IOException{
		JSONArray jattachments = (JSONArray)atts.get("attachments"); //$NON-NLS-1$
		List<WaypointAttachment> attachments = new ArrayList<>();
		
		for (int i = 0; i < jattachments.size(); i ++) {
			JSONObject jattachment = (JSONObject)jattachments.get(i);
			
			String fname = (String) jattachment.get("filename"); //$NON-NLS-1$
			String data = (String)jattachment.get("data"); //base64 encoded //$NON-NLS-1$
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
			attachments.add(attachment);
		}
		return attachments;
	}
	private Category findCategory(String hkey, ConservationArea ca, Session session) {
		Category c = QueryFactory.buildQuery(session, Category.class, new Object[] { "conservationArea", ca }, //$NON-NLS-1$
				new Object[] { "hkey", hkey }).uniqueResult(); //$NON-NLS-1$
		return c;
	}

	private Attribute findAttribute(String key, Category c, Session session) {
		List<Attribute> temp = new ArrayList<>();
		c.getAllAttribute(temp, null);
		for (Attribute ca : temp) {
			if (ca.getKeyId().equals(key)) {
				return ca;
			}
		}
		return null;
	}

	private Employee findEmployee(String uuid, ConservationArea ca, Session session) {
		UUID euuid = UuidUtils.stringToUuid(uuid);
		Employee e = session.get(Employee.class, euuid);
		if (e != null && e.getConservationArea().equals(ca))
			return e;
		return null;
	}

	private void updateObservationAttribute(WaypointObservationAttribute a, Object value, Locale locale) throws Exception {
		if (value == null) return;
	
		switch (a.getAttribute().getType()) {
		case BOOLEAN:
			if (value instanceof Boolean) {
				Boolean b = (Boolean) value;
				a.setNumberValue(b ? 1.0 : 0.0);
			} else if (value instanceof Double) {
				a.setNumberValue((Double) value);
			}else if (value instanceof String) {
				if (((String) value).equalsIgnoreCase("true")) { //$NON-NLS-1$
					a.setNumberValue(1.0);
				}else if (((String) value).equalsIgnoreCase("false")) { //$NON-NLS-1$
					a.setNumberValue(0.0);
				}
			}
			if (a.getNumberValue() == null) {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_BOOLEAN_ATTRIBUTE.getMessage(locale),
						value.toString(), a.getAttribute().getName()));
			}
			break;
		case DATE:
			if (value instanceof String) {
				LocalDate d = LocalDate.parse((String) value);
				a.setDateValue(d);
			} else if (value instanceof LocalDate) {
				a.setDateValue((LocalDate) value);
			} else {
				throw new Exception(MessageFormat.format(
						Messages.INVALID_DATE_ATTRIBUTE.getMessage(locale),
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
			if (value instanceof Number) {
				a.setNumberValue(((Number) value).doubleValue());
			} else {
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
		default:
			break;
		}
	}

}
