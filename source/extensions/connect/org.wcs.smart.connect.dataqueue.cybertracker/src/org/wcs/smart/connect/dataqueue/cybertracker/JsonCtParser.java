/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.dataqueue.cybertracker;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.dataqueue.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Parses sighting data from cybertracker JSON data.
 * 
 * @author Emily
 *
 */
public class JsonCtParser {

	public static final String SIGHTINGS_KEY = "sighting"; //$NON-NLS-1$
	public static final String FEATURE_TYPE_KEY = "type"; //$NON-NLS-1$
	public static final String PROPERTIES_KEY = "properties"; //$NON-NLS-1$
	public static final String GEOMETRY_KEY = "geometry"; //$NON-NLS-1$
	public static final String GEOMETRY_TYPE_KEY = "type"; //$NON-NLS-1$
	public static final String GEOMETRY_COORDINATE_KEY = "coordinates"; //$NON-NLS-1$
	public static final String LONGITUDE_KEY = "longitude"; //$NON-NLS-1$
	public static final String LATITUDE_KEY = "latitude"; //$NON-NLS-1$
	public static final String DATETIME_KEY = "dateTime"; //$NON-NLS-1$

	public static final String DEVICE_ID = "deviceId"; //$NON-NLS-1$
	
	private static final String JPEG_EXT = "jpeg"; //$NON-NLS-1$
	private static final String PHOTO_KEY = "ct_photo"; //$NON-NLS-1$
	
	public static List<JSONObject> parseFeaturesFromJsonString(String json) throws Exception{
		JSONObject jsonData = null; 
		try {
			Object obj = (new JSONParser()).parse(json);
			jsonData = (JSONObject) obj;
		}catch (Exception ex){
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
			throw new Exception(Messages.JsonCtParser_ParseError + ":" + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
		JSONArray jsFeatures = (JSONArray) jsonData.get("features"); //$NON-NLS-1$
		List<JSONObject> features = new ArrayList<JSONObject>();
		for (int i = 0; i < jsFeatures.size(); i ++){
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
		JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
		if (properties == null) return false;
		JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
		if (sighting != null) return false;
		
		JSONObject geom = (JSONObject) feature.get(JsonCtParser.GEOMETRY_KEY);
		if (!"Point".equalsIgnoreCase((String)geom.get(JsonCtParser.GEOMETRY_TYPE_KEY))){ //$NON-NLS-1$
			//only parse points
			return false;
		}
		
		return true;
	}
	/**
	 * Time in seconds
	 * @param date
	 * @return
	 */
	public static Time getTime(Date d){
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		
		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis(0);
		int[] fields = new int[]{Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY};
		for (int field : fields){
			c2.set(field, c.get(field));
		}
		return new Time(c2.getTimeInMillis());
	}

	
	/**
	 * Determines if the time represented by date1 is between the times
	 * represented by date2 and date3.  Only compares time parts not
	 * date parts.
	 * @return
	 */
	public static boolean isTimeBetween(Date date1, Date date2, Date date3){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		Calendar cal3 = Calendar.getInstance();
		cal3.setTime(date3);
		
		int cal1seconds = cal.get(Calendar.SECOND) + cal.get(Calendar.MINUTE) * 60  + cal.get(Calendar.HOUR_OF_DAY) * 60 * 60;
		int cal2seconds = cal2.get(Calendar.SECOND) + cal2.get(Calendar.MINUTE) * 60  + cal2.get(Calendar.HOUR_OF_DAY) * 60 * 60;
		int cal3seconds = cal3.get(Calendar.SECOND) + cal3.get(Calendar.MINUTE) * 60  + cal3.get(Calendar.HOUR_OF_DAY) * 60 * 60;
		
		return cal1seconds >= cal2seconds && cal1seconds <= cal3seconds;
	}
	
	/**
	 * Determines if the date represented by date1 is between the date
	 * represented by date2 and date3.  Only compares date parts not time
	 * 
	 * @return
	 */
	public static boolean isDateBetween(Date date1, Date date2, Date date3){
		Date d1 = SharedUtils.getDatePart(date1, false);
		Date d2 = SharedUtils.getDatePart(date2, false);
		Date d3 = SharedUtils.getDatePart(date3, false);
		
		return ((SharedUtils.isSameDate(d1, d2) || d1.after(d2)) && 
				(SharedUtils.isSameDate(d1, d3) || d1.before(d3)));
	}
	
	private JSONParser parser = new JSONParser();
	
	private List<String> warnings = null;
	
	private List<WaypointObservationAttribute> applyToAllObservations;
	
	public List<String> getWarnings(){
		return this.warnings;
	}
	
	/**
	 * returns the observation attributes in the json data that
	 * should be applied to all observations.  This includes the default
	 * observation values and any observation values with the multi/select 
	 * index.
	 *  
	 * @return
	 */
	public List<WaypointObservationAttribute> getApplyToAdd(){
		return applyToAllObservations;
	}
	public Coordinate readXYFromProperties(JSONObject feature){
		JSONObject properties = (JSONObject) feature.get(PROPERTIES_KEY);
		Double x = (Double)properties.get(LONGITUDE_KEY);
		Double y = (Double)properties.get(LATITUDE_KEY);
		if (x == null || y == null) return null;
		return new Coordinate(x,y);
		
	}
	/**
	 * Creates a waypoint from the JSON feature object
	 * 
	 * @param sighting
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Waypoint createWaypoint(JSONObject feature, Session session) throws Exception{
		
		warnings = new ArrayList<String>();
		
		if (!((String)feature.get(FEATURE_TYPE_KEY)).equalsIgnoreCase("feature")){ //$NON-NLS-1$
			throw new Exception(Messages.JsonCtParser_NoFeatureFound);
		}
		
		JSONObject properties = (JSONObject) feature.get(PROPERTIES_KEY);
		
		//parse x and y may be null
		Waypoint newWaypoint = new Waypoint();
		Double x = (Double)properties.get(LONGITUDE_KEY);
		if (x != null) newWaypoint.setX(x);
		Double y = (Double)properties.get(LATITUDE_KEY);
		if (y != null) newWaypoint.setY(y);
			
		Date dt = JsonUtils.JSON_DATE_FORMAT.parse((String)properties.get(DATETIME_KEY));
		newWaypoint.setDateTime(dt);

		newWaypoint.setObservations(new ArrayList<WaypointObservation>());

		//observations are saved in the sightings object
		JSONObject observations = (JSONObject)properties.get(SIGHTINGS_KEY);
		if (observations == null) return newWaypoint;
		
		//category uuid and level; this uuid is associated with the "largest" level
		String categoryUuid = null;
		int catLevel = -1;
		
		//attribute information
		HashMap<Integer, List<ObservationInfo>> attributes = new HashMap<Integer, List<ObservationInfo>>();
		List<String> waypointAttachments = new ArrayList<String>();
		//default values
		JSONObject defaultValues = null;
		
		for (Entry<?,?> e : (Set<Entry<?,?>>)observations.entrySet()){
			String key = (String)e.getKey();
			if (key.startsWith(JsonKey.CATEGORY.key + CyberTrackerConfExporter.KEY_SEP)){
				//cateogries; if set to null this was a group from the configurable model
				//otherwise we want to find the "leaf" category
				//FORM: '"c:0": "null", "c:1": "<uuid>"'
				if (!((String)e.getValue()).equals(CyberTrackerConfExporter.NULL_KEY)){
					int level = Integer.parseInt(key.substring(2));
					if (level > catLevel){
						categoryUuid = (String)e.getValue();
					}
				}
			}
				
			if (key.startsWith(JsonKey.ATTRIBUTE.key + CyberTrackerConfExporter.KEY_SEP) ||
					key.startsWith(JsonKey.ATTRIBUTE_LIST.key + CyberTrackerConfExporter.KEY_SEP) ||
					key.startsWith(JsonKey.ATTRIBUTE_MULTILIST.key + CyberTrackerConfExporter.KEY_SEP)
					){
				
				String[] bits = key.split (CyberTrackerConfExporter.KEY_SEP.toString());
				//identifies which observation this attribute applies to
				int obsnum = Integer.parseInt(bits[1]);
				List<ObservationInfo> data = attributes.get(obsnum);
				if (data == null){
					data = new ArrayList<ObservationInfo>();
					attributes.put(obsnum, data);
				}
				
				if (key.startsWith(JsonKey.ATTRIBUTE.key + CyberTrackerConfExporter.KEY_SEP)){	
					//attributes
					//FORM: '"a:0:<uuid>": "value"'
					ObservationInfo info = new ObservationInfo(bits[0], bits[2], e.getValue());
					data.add(info);
				}
				if (key.startsWith(JsonKey.ATTRIBUTE_LIST.key + CyberTrackerConfExporter.KEY_SEP)){
					//attribute list item
					//FORM: '"al:0:<uuid>": true'
					data.add(new ObservationInfo(JsonKey.ATTRIBUTE_LIST.key, (String)bits[2], JsonKey.ATTRIBUTE_LIST.key + CyberTrackerConfExporter.KEY_SEP + (String)bits[2]));
				}
			
				if (key.startsWith(JsonKey.ATTRIBUTE_MULTILIST.key + CyberTrackerConfExporter.KEY_SEP)){
					//multi lists
					//FORM: '"ml:0:<attributeuuid>:l:<listitemuuid>": value'
					//number attribute
					data.add(new ObservationInfo(JsonKey.ATTRIBUTE.key, (String)bits[2], e.getValue()));
					//list attribute
					data.add(new ObservationInfo(JsonKey.ATTRIBUTE_LIST.key, (String)bits[4], JsonKey.ATTRIBUTE_LIST.key + CyberTrackerConfExporter.KEY_SEP + (String)bits[4]));
				}
			}
			if (key.startsWith(ScreensUtil.RESULT_PHOTO)){
				waypointAttachments.add((String)e.getValue());
			}
			
			//default values
			if (key.equalsIgnoreCase(ScreensUtil.RESULT_DEFAULT_ATTRIBUTE_VALUES) ){
				String jsonDefaults = (String) e.getValue();
				if (jsonDefaults != null && !jsonDefaults.isEmpty()){
					defaultValues = (JSONObject)parser.parse(jsonDefaults);
				}		
			}
		}
		
		//configure data into observations
		Category category = null;
		if (categoryUuid != null ){
			category = (Category) session.get(Category.class, UuidUtils.stringToUuid(categoryUuid));
			if (category == null){
				throw new Exception(MessageFormat.format(Messages.JsonCtParser_NoCateogyr, categoryUuid));
			}
		}else{
			//no category found, so lets assume no observations
			//happens on patrol pause/resume
			return newWaypoint;
		}
		
				
		//configure default values
		JsonUtils.ParseResult defaults = JsonUtils.parseDefaultAttributeValues(defaultValues, session);
		warnings.addAll(defaults.getWarnings());
		
		List<WaypointObservationAttribute> defaultAttributes = defaults.getAttributes(); 
				
		//these attribute values must be applied to all observations
		List<WaypointObservationAttribute>  applyAllObs = createWaypointObservationAttribute(attributes.get(CyberTrackerConfExporter.MULTI_SELECT_INDEX), category, defaultAttributes, session);
		applyToAllObservations = applyAllObs;
		
		
		Employee observer = null;
		if (observations.containsKey(ScreensUtil.RESULT_OBSERVER)){
			String ob = (String) observations.get(ScreensUtil.RESULT_OBSERVER);
			if (ob.startsWith(JsonKey.EMPLOYEE.key + CyberTrackerConfExporter.KEY_SEP)){
				String uuid = ob.substring(JsonKey.EMPLOYEE.key.length() + 1);
				observer = (Employee) session.get(Employee.class, UuidUtils.stringToUuid(uuid));
				if (observer == null){
					warnings.add(MessageFormat.format(Messages.JsonCtParser_ObserverNotFound, uuid));
				}
			}
		}
		if (attributes.entrySet().isEmpty()){
			//create an observation with only default attribute values
			WaypointObservation wp = new WaypointObservation();
			wp.setObserver(observer);
			wp.setWaypoint(newWaypoint);
			newWaypoint.getObservations().add(wp);
			wp.setCategory(category);
			wp.setAttributes(new ArrayList<WaypointObservationAttribute>());
			for (WaypointObservationAttribute ob : applyAllObs){
				ob.setObservation(wp);
				wp.getAttributes().add(ob);
			}
		}else{
			for (Entry<Integer, List<ObservationInfo>> e : attributes.entrySet()){
				int order = e.getKey();
				if (order == CyberTrackerConfExporter.MULTI_SELECT_INDEX) continue; //skip the defaults
				
				List<ObservationInfo> values = (List<ObservationInfo>) e.getValue();
						
				WaypointObservation wp = new WaypointObservation();
				wp.setObserver(observer);
				wp.setWaypoint(newWaypoint);
				newWaypoint.getObservations().add(wp);
				wp.setCategory(category);
				
				wp.setAttributes(new ArrayList<WaypointObservationAttribute>());
				
				//add attributes
				List<WaypointObservationAttribute>  obs = createWaypointObservationAttribute(values, category, null, session);
				for (WaypointObservationAttribute ob : obs){
					ob.setObservation(wp);
					wp.getAttributes().add(ob);
				}
				
				//these are the attributes to apply to all observations		
				for (WaypointObservationAttribute ob : applyAllObs){
					boolean add = true;
					for (WaypointObservationAttribute existing : wp.getAttributes()){
						if (existing.getAttribute().equals(ob.getAttribute())){
							add = false;
							break;
						}
					}
					if (add){
						ob = ob.clone();
						ob.setObservation(wp);
						wp.getAttributes().add(ob);
					}
				}
				
			}
			
			//parse attachments
			List<WaypointAttachment> attachments = parseAttachments(waypointAttachments);
			if (!attachments.isEmpty() && newWaypoint.getAttachments()== null){
				newWaypoint.setAttachments(new ArrayList<WaypointAttachment>());
			}
			for (WaypointAttachment att : attachments){
				att.setWaypoint(newWaypoint);
				newWaypoint.getAttachments().add(att);
			}
		}
		return newWaypoint;
	}
	
	/**
	 * may return null if attribute not found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private Attribute findAttribute(String uuid, Session session) throws Exception{
		return JsonUtils.findAttribute(uuid, session);
	}
	
	/**
	 * Can return null if not list item found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private AttributeListItem findAttributeListItem(String uuid, Session session) throws Exception{
		return JsonUtils.findAttributeListItem(uuid, session);
	}
	
//	/**
//	 * Can return null if the tree node is not found
//	 * @param uuid
//	 * @param session
//	 * @return
//	 * @throws Exception
//	 */
//	private AttributeTreeNode findAttributeTreeNode(String uuid, Session session) throws Exception{
//		return JsonUtils.findAttributeTreeNode(uuid, session);
//	}	
	private List<WaypointAttachment> parseAttachments(List<String> values) throws Exception{
		int imagecnt = 0;
		List<WaypointAttachment> attachments = new ArrayList<WaypointAttachment>();
		
		for (String value : values){
			
			//picture object; create a temporary file add it to waypoint observation
			String fileName = PHOTO_KEY + "_" + imagecnt + "." + JPEG_EXT;   //$NON-NLS-1$//$NON-NLS-2$
				
			Path temp = Files.createTempFile("SMART_" + System.nanoTime(), "." + JPEG_EXT);   //$NON-NLS-1$//$NON-NLS-2$
			BufferedImage image = null;
			try(InputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(value))){
				image = ImageIO.read(in);					
			}
			if (image == null){
				warnings.add(MessageFormat.format(Messages.JsonCtParser_CouldNotImportPhoto, value));
			}else{
				ImageIO.write(image, JPEG_EXT.toUpperCase(), temp.toFile());	
				WaypointAttachment attachment = new WaypointAttachment();
				attachment.setCopyFromLocation(temp.toFile());
				attachment.setFilename(fileName);
				attachments.add(attachment);
			}
		}
		return attachments;
	}
	/**
	 * Parses the values into waypoint observation attributes
	 * @param values
	 * @param c
	 * @param defaultAttributes
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private List<WaypointObservationAttribute> createWaypointObservationAttribute(List<ObservationInfo> values, Category c, List<WaypointObservationAttribute> defaultAttributes, Session session) throws Exception{
		
		List<WaypointObservationAttribute> results = new ArrayList<WaypointObservationAttribute>();
		List<Attribute> validAttributes = new ArrayList<Attribute>();
		c.getAllAttribute(validAttributes, null);
		
		//add default values; these will be overridden if actual values are provided
		Set<WaypointObservationAttribute> defaultClones = new HashSet<>();
		if (defaultAttributes != null){
			for (WaypointObservationAttribute a : defaultAttributes){
				if (validAttributes.contains(a.getAttribute())){
					WaypointObservationAttribute clone = a.clone();
					results.add(clone);
					defaultClones.add(clone);
				}else{
					//attribute not valid for category; ignore default values
				}
			}
		}
		if (values == null) return results;
		
		for (ObservationInfo obj : values){
			if (obj.keyType.equals(JsonKey.ATTRIBUTE_LIST.key)){
				//this identifies this list element occur; switch to attribute=element format
				AttributeListItem li =  findAttributeListItem(obj.uuid, session);
				if (li == null){
					warnings.add(MessageFormat.format(Messages.JsonCtParser_ListAttributeNotFound, obj.uuid));
				}else{
					obj.keyType = JsonKey.ATTRIBUTE.key;
					obj.uuid = UuidUtils.uuidToString( li.getAttribute().getUuid() );
				}
			}
			
			if (obj.keyType.equals(JsonKey.ATTRIBUTE.key)){
				Attribute att = findAttribute( obj.uuid, session);
				if (att == null){
					warnings.add(MessageFormat.format(Messages.JsonCtParser_AttributeNotFound, obj.uuid));
					continue;
				}
				if (!validAttributes.contains(att)) throw new Exception(MessageFormat.format(Messages.JsonCtParser_CatAttributeNotFound, att.getName(), c.getName()));
				
				boolean add = true;
				WaypointObservationAttribute wpatt = new WaypointObservationAttribute();
				try{
					wpatt.setAttribute(att);
					if (!JsonUtils.setAttributeValue(wpatt, obj.value, session, warnings)){
						add = false;
					}else{
					//check if this attribute already exists for the observation.  Attribute
					//can only exist once except for the tree nodes
						for (Iterator<WaypointObservationAttribute> iterator = results.iterator(); iterator.hasNext();) {
							WaypointObservationAttribute wpa = (WaypointObservationAttribute) iterator.next();
						
							if (wpa.getAttribute().equals(att)){
								if (defaultClones.contains(wpa)){
									//already defined - probably the default should overwrite
									wpatt = wpa;
									wpatt.setNumberValue(null);
									wpatt.setAttributeListItem(null);
									wpatt.setAttributeTreeNode(null);
									wpatt.setStringValue(null);
								}else{
									if (wpa.getAttribute().getType()!=AttributeType.TREE){
										warnings.add(MessageFormat.format(Messages.JsonCtParser_MultiValuesSameAttribute, att.getName()));
										add = false;
									}
									//trees can be specified as each node is specified; we want to pick the longest one
									if (Category.hkeyLength(wpatt.getAttributeTreeNode().getHkey()) > Category.hkeyLength(wpa.getAttributeTreeNode().getHkey())){
										add = true;
										iterator.remove();
									}else{
										add = false;
									}
								}
							}
						}
					}
				}catch (Exception ex){
					CyberTrackerPlugIn.log(ex.getMessage(), ex);
					warnings.add(MessageFormat.format(Messages.JsonCtParser_CouldNotParseValue, att.getName(), obj.value, ex.getMessage()));				
					add = false;
				}
				if (add) results.add(wpatt);
			}
		}
		return results;
	}
	
	
	
	private class ObservationInfo{
		public String keyType;
		public String uuid;
		public Object value;
		
		public ObservationInfo(String keyType, String uuid, Object value){
			this.keyType = keyType;
			this.uuid = uuid;
			this.value = value;
		}
	}
	
}
