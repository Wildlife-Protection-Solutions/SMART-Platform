package org.wcs.smart.connect.dataqueue.cybertracker;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.UuidUtils;

public class JsonCtParser {

	public static final String SIGHTINGS_KEY = "sighting"; //$NON-NLS-1$
	public static final String FEATURE_TYPE_KEY = "type"; //$NON-NLS-1$
	public static final String PROPERTIES_KEY = "properties"; //$NON-NLS-1$
	public static final String LONGITUDE_KEY = "longitude"; //$NON-NLS-1$
	public static final String LATITUDE_KEY = "latitude"; //$NON-NLS-1$
	public static final String DATETIME_KEY = "dateTime"; //$NON-NLS-1$

	public static final String DEVICE_ID = "deviceId"; //$NON-NLS-1$
	
	public static List<JSONObject> parseFeaturesFromJsonString(String json) throws Exception{
		if (json.charAt(json.length()-1) == 0){
			json = json.substring(0, json.length() - 1);
		}
		
		JSONObject jsonData = null; 
		try {
			Object obj = (new JSONParser()).parse(json);
			jsonData = (JSONObject) obj;
		}catch (Exception ex){
			ex.printStackTrace();
			throw new Exception("Unable to parse json text.");
		}
		
		JSONArray jsFeatures = (JSONArray) jsonData.get("features"); //$NON-NLS-1$
		List<JSONObject> features = new ArrayList<JSONObject>();
		for (int i = 0; i < jsFeatures.size(); i ++){
			JSONObject feature = (JSONObject) jsFeatures.get(i);
			features.add(feature);
		}
		return features;
	}

	
	private JSONParser parser = new JSONParser();
	
	private List<String> warnings = null;
	
	
	public List<String> getWarnings(){
		return this.warnings;
	}
	
	/**
	 * Creates a waypoint from the JSON feature object
	 * 
	 * @param sighting
	 * @return
	 */
	public Waypoint createWaypoint(JSONObject feature, Session session) throws Exception{
		
		warnings = new ArrayList<String>();
		
		if (!((String)feature.get(FEATURE_TYPE_KEY)).equalsIgnoreCase("feature")){ //$NON-NLS-1$
			throw new Exception("Feature object does not have type 'feature'");
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
		//default values
		JSONObject defaultValues = null;
		
		for (Entry e : (Set<Entry>)observations.entrySet()){
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
				throw new Exception(MessageFormat.format("Category not found for uuid '{0}'", categoryUuid));
			}
		}else{
			throw new Exception("Invalid JSON observation - no cateogry found.");
		}
		
				
		//configure default values
		JsonUtils.ParseResult defaults = JsonUtils.parseDefaultAttributeValues(defaultValues, session);
		warnings.addAll(defaults.getWarnings());
		
		List<WaypointObservationAttribute> defaultAttributes = defaults.getAttributes(); 
				

		
		//these attribute values must be applied to all observations
		List<WaypointObservationAttribute>  applyAllObs = createWaypointObservationAttribute(attributes.get(-1), category, defaultAttributes, session);
		
		if (attributes.entrySet().isEmpty()){
			//create an observation with only default attribute values
			WaypointObservation wp = new WaypointObservation();
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
				if (order == -1) continue; //skip the defaults
				
				List<ObservationInfo> values = (List<ObservationInfo>) e.getValue();
						
				WaypointObservation wp = new WaypointObservation();
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
		}
			
//			if (newWaypoint.getX() == null){
//				//no position provided we want to add this to the previous waypoint
//				//TODO: we could also check the SMART_NewWaypoint sighting attribute:
//				// "SMART_NewWaypoint": "Save As New Waypoint"
//				// "SMART_NewWaypoint": "Add To Last Waypoint"
//				if (waypoints.size() > 0){
//					waypoints.get(waypoints.size()-1).getObservations().addAll(newWaypoint.getObservations());
//				}else{
//					//the previous waypoint to add to was sent in a previous package; we have to deal with this when importing into patrol
//					waypoints.add(newWaypoint);
//				}
//			}else{
//				waypoints.add(newWaypoint);
//			}
		
		
//		for (Waypoint wp : waypoints){
//			System.out.println("WAYPOINT: ( " + wp.getX() + "," + wp.getY() + ", " + wp.getDateTime().toString() + ")");
//			for (WaypointObservation obs : wp.getObservations()){
//				System.out.println("observation: " + obs.getCategory().getFullCategoryName());
//				for (WaypointObservationAttribute a : obs.getAttributes()){
//					System.out.println(a.getAttribute().getName() + ": " + a.getAttributeValueAsString(Locale.getDefault()));
//				}
//			}
//			System.out.println();
//		}
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
	
	/**
	 * Can return null if the tree node is not found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private AttributeTreeNode findAttributeTreeNode(String uuid, Session session) throws Exception{
		return JsonUtils.findAttributeTreeNode(uuid, session);
	}	
	
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
					warnings.add(MessageFormat.format("No attribute list item found for uuid {0}.  This attribute will be not imported.", obj.uuid));
				}else{
					obj.keyType = JsonKey.ATTRIBUTE.key;
					obj.uuid = UuidUtils.uuidToString( li.getAttribute().getUuid() );
				}
			}
			
			if (obj.keyType.equals(JsonKey.ATTRIBUTE.key)){
				Attribute att = findAttribute( obj.uuid, session);
				if (att == null){
					warnings.add(MessageFormat.format("No attribute found for uuid {0}.  This attribute will be not imported.", obj.uuid));
					continue;
				}
				if (!validAttributes.contains(att)) throw new Exception("Attribute " + att.getName() + " not associated with category " + c.getName());
				
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
										warnings.add(MessageFormat.format("The same attribute ({0}) cannot be specified twice for a single observation.", att.getName()));
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
					warnings.add(MessageFormat.format("Could not parse value for attribute {0}: {1}. {2}", att.getName(), obj.value, ex.getMessage()));				
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
