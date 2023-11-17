/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.cybertracker.json.JsonImportWarning.Type;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.ImageSizeOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.OptionID;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.util.UuidUtils;

/**
 * SMART Mobile JSON processing utilities class
 * 
 * @author Emily
 *
 */
public class CtJsonUtil {
	
	/**
	 * Json configuration keys
	 * 
	 * @author Emily
	 *
	 */
	public static enum JsonDataModelKey{
		CATEGORY("c"), //$NON-NLS-1$
		ATTRIBUTE("a"), //$NON-NLS-1$
		EMPLOYEE("e"), //$NON-NLS-1$
		ATTRIBUTE_LIST("l"), //$NON-NLS-1$
		ATTRIBUTE_MULTILIST("ml"), //$NON-NLS-1$
		ATTRIBUTE_TREE("t"); //$NON-NLS-1$
		
		public String key;
		
		private JsonDataModelKey(String key){
			this.key = key;
		}
		
	}
	
	public enum JsonKey{
		DATATYPE ("SMART_DataType"), //$NON-NLS-1$
		ID ("SMART_PatrolID"), //$NON-NLS-1$
		PAUSED ("SMART_Paused"), //$NON-NLS-1$
		DEFAULT_METADATA_VALUES("SMART_DefaultPatrolValues"), //$NON-NLS-1$
		NEW_WAYPOINT("SMART_NewWaypoint"), //$NON-NLS-1$
		END_WAYPOINT_GROUP("SMART_WaypointGroupEnd"), //$NON-NLS-1$
		DEFAULT_ATTRIBUTE_VALUES("SMART_DefaultAttributeValues"), //$NON-NLS-1$
		OBSERVER("SMART_Observer"), //$NON-NLS-1$
		PHOTO("SMART_Photo"), //$NON-NLS-1$
		AUDIO("SMART_Audio"), //$NON-NLS-1$
		SIGNATURE("SMART_Signature_"), //$NON-NLS-1$
		OBSERVATION_COUNTER("SMART_ObsCounter"), //$NON-NLS-1$
		OBSERVATION_GROUP("SMART_SightingGroupId"), //$NON-NLS-1$
		OBSERVATION("SMART_ObservationType"); //$NON-NLS-1$

	
		public String key;
		
		JsonKey(String key){
			this.key = key;
		}
	}
	
	//date attributes come through in a different format; only applicable to date attributes
	public static final String JSON_ATTRIBUTE_DATE_FORMAT_STR = "yyyy/MM/dd";  //$NON-NLS-1$
	
	//example string: "2019-12-30T22:48:26.0-08:00"
	private static final String JSON_DATE_FORMAT_STRX = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; //$NON-NLS-1$
	public static final String JSON_DATE_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss.SSS"; //$NON-NLS-1$
		
	public static final int MULTI_SELECT_INDEX = -1;
	
	public static final Character KEY_SEP = ':'; 
	public static final String NULL_KEY = "null"; //$NON-NLS-1$
	
	
	
	/**
	 * JSON dates come in the format "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" where
	 * the time is the local time and the time we want to use.  We want to 
	 * throw out the timezone information because we don't care about that 
	 * and it causes issues with conversions if the local computer is in 
	 * a different timezone from other computers.
	 * @param value
	 * @throws DateTimeParseException 
	 */
	public static LocalDateTime parseJsonDateTime(String value) throws DateTimeParseException {
		try {
			//this parses the date/time string throwing out any timezone information
			return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(JSON_DATE_FORMAT_STRX));
		}catch (DateTimeParseException ex) {
			
		}
		return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(JSON_DATE_FORMAT_STR));
	}
	
	private static <T> T findObject(Class<T> clazz, String uuid, Session session) throws Exception{
		return session.get(clazz, UuidUtils.stringToUuid(uuid));
	}
	
	/**
	 * Can return null if attribute not found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static Attribute findAttribute(String uuid, Session session) throws Exception{
		return findObject(Attribute.class, uuid, session);
	}
	
	/**
	 * Can return null if category not found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static Category findCategory(String uuid, Session session) throws Exception{
		return findObject(Category.class, uuid, session);
	}
	
	/**
	 * Can return null if not list item found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static AttributeListItem findAttributeListItem(String uuid, Session session) throws Exception{
		return findObject(AttributeListItem.class, uuid, session);
	}
	
	/**
	 * Can return null if the tree node is not found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static AttributeTreeNode findAttributeTreeNode(String uuid, Session session) throws Exception{
		return findObject(AttributeTreeNode.class, uuid, session);
	}	
	
	/**
	 * Gets the cybertracker image resize option
	 * @param ca
	 * @param session
	 * @return
	 */
	public static CyberTrackerPropertiesOption getImageResizeOption(ConservationArea ca, Session session){
		return QueryFactory.buildQuery(session, CyberTrackerPropertiesOption.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"optionId", OptionID.RESIZE_IMAGE.name()}).uniqueResult(); //$NON-NLS-1$
	}
	
	/**
	 * Gets the maximum files size for cybertracker image attachments before it
	 * attempts to resize it.  Returns 0 if value not set.
	 * @param ca
	 * @param session
	 * @return
	 */
	public static double getImageMaxSizeOption(ConservationArea ca, Session session){
		CyberTrackerPropertiesOption opImageMaxSize = QueryFactory.buildQuery(session, CyberTrackerPropertiesOption.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"optionId", OptionID.MAX_IMAGE_SIZE.name()}).uniqueResult(); //$NON-NLS-1$
		if (opImageMaxSize == null || opImageMaxSize.getDoubleValue() == null) return 0;
		return opImageMaxSize.getDoubleValue();
	}
	
	/**
	 * Gets the target width/height to resize image to.
	 * 
	 * @param ca
	 * @param session
	 * @return
	 */
	public static int[] getImageAutoResizeSizeOption(ConservationArea ca, Session session){
		CyberTrackerPropertiesOption opImageSize = QueryFactory.buildQuery(session, CyberTrackerPropertiesOption.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"optionId", OptionID.IMAGE_SIZE.name()}).uniqueResult(); //$NON-NLS-1$
		
		if (opImageSize == null) return new int[]{-1,-1};
		if (opImageSize.getStringValue().startsWith(ImageSizeOption.CUSTOM.name())){
			String[] bits = opImageSize.getStringValue().split(CyberTrackerPropertiesOption.PROP_SEP); 
			int width = -1;
			int height = -1;
			try{
				width = Integer.parseInt(bits[1]);
				height = Integer.parseInt(bits[2]);
			}catch(Exception ex){}
			return new int[]{width, height};
		}else{
			for (ImageSizeOption op : ImageSizeOption.values()){
				if (opImageSize.getStringValue().equalsIgnoreCase(op.name())){
					return new int[]{op.width, op.height};
				}
			}
		}
		
		return new int[]{-1,-1};
	}
	
	/**
	 * Converts the value to an object.  Well return null if value
	 * is null or value is not an instanceo of a String or Boolean
	 * 
	 * @param value
	 * @return
	 */
	public static Boolean convertToBoolean(Object value) {
		if (value == null) return null;
		if (value instanceof Boolean) {
			return (Boolean)value;
		}else if (value instanceof String) {
			return (Boolean.valueOf((String)value));
		}
		return null;
	}

	/**
	 * updates the attribute value with the value represented by the object
	 * @param toUpdate
	 * @param value
	 * @param session
	 * @param warnings
	 * @return
	 * @throws Exception
	 */
	public static boolean setAttributeValue(WaypointObservationAttribute toUpdate, Object value, Session session, List<JsonImportWarning> warnings ) throws Exception{
		
		Attribute att = toUpdate.getAttribute();
	
		if (att.getType() == AttributeType.BOOLEAN){
			Boolean v = convertToBoolean(value);
			if (v == null) {
				//data type not supported
				warnings.add(new JsonImportWarning(Type.COULD_NOT_PARSE_BOOLEAN, value.toString()));
				return false;
			}else if (v) {
				toUpdate.setNumberValue(1.0);
			}else if (!v) {
				toUpdate.setNumberValue(0.0);
			}
			
		}else if (att.getType() == AttributeType.DATE){
			LocalDateTime date = null;
			try {
				date = CtJsonUtil.parseJsonDateTime((String)value);				
			}catch (Exception ex) {}
			try {
				date = LocalDate.parse((String)value, DateTimeFormatter.ofPattern(JSON_ATTRIBUTE_DATE_FORMAT_STR)).atStartOfDay();
			}catch (Exception ex) {}
			
			if (date == null) {
				warnings.add(new JsonImportWarning(Type.COULD_NOT_PARSE_DATE, value.toString()));
				return false;
			}
			toUpdate.setDateValue(date.toLocalDate());
			
		}else if (att.getType() == AttributeType.LIST){
			String listElement = (String) value;
			if (!listElement.startsWith(JsonDataModelKey.ATTRIBUTE_LIST.key + KEY_SEP)) {
				warnings.add(new JsonImportWarning(Type.LIST_ATTRIBUTE_NOT_FOUND, listElement));
				return false;
			}
			
			AttributeListItem li = findAttributeListItem(listElement.substring(2), session);	
			if (li == null){
				warnings.add(new JsonImportWarning(Type.LIST_ATTRIBUTE_NOT_FOUND, listElement));
				return false;
			}
			toUpdate.setAttributeListItem(li);
			
		}else if (att.getType() == AttributeType.MLIST) {
			@SuppressWarnings("unchecked")
			List<AttributeListItem> items = (List<AttributeListItem>)value;
			
			List<WaypointObservationAttributeList> listitems = new ArrayList<>();
			for (AttributeListItem li : items) {
				WaypointObservationAttributeList woli = new WaypointObservationAttributeList();
				woli.setAttributeLisItem(li);
				woli.setObservationAttribute(toUpdate);
				listitems.add(woli);
			}
			//nothing to add
			if (listitems.isEmpty()) return false;
			
			toUpdate.setAttributeListItems(listitems);
			
		}else if (att.getType() == AttributeType.NUMERIC){
			Double value2 = null;
			if (value instanceof Number){
				value2 = ((Number)value).doubleValue();
			}
			toUpdate.setNumberValue(value2);
			
		}else if (att.getType() == AttributeType.TEXT){
			toUpdate.setStringValue((String)value);
			
		}else if (att.getType() == AttributeType.TREE){
			String treeElement = (String)value;
			if (!treeElement.startsWith(JsonDataModelKey.ATTRIBUTE_TREE.key + KEY_SEP)) {
				warnings.add(new JsonImportWarning(Type.TREE_NODE_NOT_FOUND, treeElement));
				return false;
			}
			AttributeTreeNode li = findAttributeTreeNode(treeElement.substring(2), session);
			if (li == null){
				warnings.add(new JsonImportWarning(Type.TREE_NODE_NOT_FOUND, treeElement));
				return false;
			}
			toUpdate.setAttributeTreeNode(li);
			
		}
		return true;
	}
	

	
	@SuppressWarnings("unchecked")
	public static ParseResult parseDefaultAttributeValues(JSONObject defaultValues, Session session){
		if (defaultValues == null) return new ParseResult();
		List<WaypointObservationAttribute> defaultAttributes = new ArrayList<>();
		List<JsonImportWarning> warnings = new ArrayList<>();
		
		for (Entry<String,String> defaultValue : (Set<Entry<String,String>>)defaultValues.entrySet()){
			
			String key = (String) defaultValue.getKey();
			String value = (String) defaultValue.getValue();
			
			String part = JsonDataModelKey.ATTRIBUTE.key + KEY_SEP;
			if (key.startsWith(part)){
				Attribute a = null;
				try{
					a = findAttribute(key.substring(part.length()), session);
				}catch (Exception ex){
					//TODO: log exception
					warnings.add(new JsonImportWarning(Type.DEFAULT_ATTRIBUTE_NOT_FOUND, key));
					continue;
				}
				
				if (a == null){
					//No attribute found for uuid {0}.  The default setting for this attribute will be ignored.
					warnings.add(new JsonImportWarning(Type.DEFAULT_ATTRIBUTE_NOT_FOUND, key));
					continue;
				}
				
				try{
					WaypointObservationAttribute woa = new WaypointObservationAttribute();
					woa.setAttribute(a);
					if (setAttributeValue(woa, value, session, warnings)){
						defaultAttributes.add(woa);
					}
				}catch (Exception ex){
					//TODO: log exception
					//CyberTrackerPlugIn.log(ex.getMessage(), ex);
					warnings.add(new JsonImportWarning(Type.DEFAULT_ATTRIBUTE_PARSE_ERROR, a.getName(), value.toString(), ex.getLocalizedMessage()));

				}
				
			}
		}
		return new ParseResult(defaultAttributes, warnings);
	}

	public static List<JSONObject> parseFeaturesFromJsonString(String json) throws Exception {
		JSONObject jsonData = null;
		try {
			Object obj = (new JSONParser()).parse(json);
			jsonData = (JSONObject) obj;
		} catch (Exception ex) {
			throw new Exception((new JsonError(JsonError.Type.JSON_PARSE_ERROR, ex.getMessage())).getMessage(), ex); 
		}

		JSONArray jsFeatures = (JSONArray) jsonData.get(CtJsonObservationParser.FEATURES_KEY);
		if (jsFeatures == null)
			throw new Exception((new JsonError(JsonError.Type.FEATURES_NOT_FOUND, CtJsonObservationParser.FEATURE_KEY)).getMessage());			

		List<JSONObject> features = new ArrayList<JSONObject>();
		for (int i = 0; i < jsFeatures.size(); i++) {
			JSONObject feature = (JSONObject) jsFeatures.get(i);
			features.add(feature);
		}
		return features;
	}
	
	
	/**
	 * Determines if the jSONOBject represents a track feature.  We assume
	 * track points are point features that contain properties but not sighting
	 * information.
	 * 
	 * @param feature
	 * @return
	 */
	public static boolean isTrackPoint(JSONObject feature){
		//only want to process features with no sighting data
		JSONObject properties = (JSONObject) feature.get(CtJsonObservationParser.PROPERTIES_KEY);
		if (properties == null) return false;
		JSONObject sighting = (JSONObject)properties.get(CtJsonObservationParser.SIGHTINGS_KEY);
		if (sighting != null) return false;
		
		JSONObject geom = (JSONObject) feature.get(CtJsonObservationParser.GEOMETRY_KEY);
		if (!"Point".equalsIgnoreCase((String)geom.get(CtJsonObservationParser.GEOMETRY_TYPE_KEY))){ //$NON-NLS-1$
			//only parse points
			return false;
		}
		
		return true;
	}
	
	/**
	 * Determines if the time represented by date1 is between the times
	 * represented by date2 and date3.  Only compares time parts not
	 * date parts. 
	 * Also drop milliseconds and only compares seconds - see #3530.
	 * @return
	 */
	public static boolean isTimeBetween(LocalTime d1, LocalTime d2, LocalTime d3){
		if (d3 == null) d3 = LocalTime.MAX;
		
		d1 = d1.withNano(0);
		d2 = d2.withNano(0);
		d3 = d3.withNano(0);
		
		return ((d1.equals(d2) || d1.isAfter(d2)) && 
				(d1.equals(d3) || d1.isBefore(d3)));
	}
	
	/**
	 * Determines if the date represented by date1 is between the date
	 * represented by date2 and date3.  Only compares date parts not time
	 * 
	 * @return
	 */
	public static boolean isDateBetween(LocalDate d1, LocalDate d2, LocalDate d3){
		return ((d1.isEqual(d2) || d1.isAfter(d2)) && 
				(d1.isEqual(d3) || d1.isBefore(d3)));
	}
	
	public static class ParseResult{
		private List<WaypointObservationAttribute> attributes;
		private List<JsonImportWarning> warnings;
		
		public ParseResult(){
			this.attributes = Collections.emptyList();
			this.warnings = Collections.emptyList();
		}
		
		public ParseResult(List<WaypointObservationAttribute> attributes, List<JsonImportWarning> warnings){
			this.attributes = attributes;
			this.warnings = warnings;
		}
		
		public List<JsonImportWarning> getWarnings(){
			return this.warnings;
		}
		public List<WaypointObservationAttribute> getAttributes(){
			return attributes;
		}
	}
}
