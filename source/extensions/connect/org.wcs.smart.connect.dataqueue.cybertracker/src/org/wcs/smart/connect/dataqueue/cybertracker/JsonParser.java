package org.wcs.smart.connect.dataqueue.cybertracker;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.UuidUtils;

public class JsonParser {

//	public static final String JSON = "{ \"type\": \"FeatureCollection\", \"features\": [{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.131124, 47.632635] }, \"properties\": { \"ctVersion\": \"3.417\", \"dbName\": \"C:\\Users\\Emily\\AppData\\Local\\Temp\\$CTB5BC.tmp.mdb\", \"appName\": \"Abc\", \"deviceId\": \"7b063b84-85a8-43ab-a421-23f24e44aa8a\", \"id\": \"dce5e303-58f4-4c9f-99c7-c30f53f9d055\", \"dateTime\": \"2016-07-15T09:37:44.871-07:00\", \"latitude\": 47.632635, \"longitude\": -122.131124, \"altitude\": 108.645454, \"accuracy\": 10.000000, \"sighting\": { \"Start New Patrol\": null, \"SMART_PatrolID\": \"a4d190e281c1413fa7b5f6c060852fce\", \"Begin Patrol\": null, \"SMART_PatrolStartDate\": \"2016/07/15\", \"SMART_PatrolStartTime\": \"09:37:20\", \"SMART_PatrolType\": \"GROUND\", \"SMART_PatrolTransport\": \"025143565df847748ce452f59fc4c4c1\", \"SMART_Armed\": \"false\", \"SMART_Team\": \"3efdcfcf2f674044a5765012359471d5\", \"SMART_Station\": \"8d173bcfe4104ec1b67d29ea126c3411\", \"SMART_Mandate\": \"fc067b87c1ee4720b5c16de775820d05\", \"m:96b20879d4f54a099bb8113d0f23ea0b\": true, \"m:5f10cce8bea6470398f737bcd7bb44b4\": true, \"m:d27d511500734daca18cf7a5196e522b\": true, \"m:167533be2946495eb79797843bada003\": true, \"SMART_Leader\": \"m:167533be2946495eb79797843bada003\", \"SMART_DefaultPatrolValues\": \"[]\", \"Make Observation\": null, \"c:0\": \"272754683b30415fb47432405570afd1\", \"l:0:45019a7bf91f4bbc900e3858e5d0f515\": true, \"l:1:486b5fc59a604666ab198c889f38ee8b\": true, \"a:0:da5351faee804a1cb487a0884942d9a0\": \"l:b82fbb6c6fc94ceaab51dbbd18915f79\", \"a:0:f24847023fd24d8888d9bb644a561a23\": 8, \"a:0:b331de8aa26a481bb0b8dfb3248a45c2\": \"l:6bb5bb665f31439a9c4c51bd5c0f6821\", \"a:1:da5351faee804a1cb487a0884942d9a0\": \"l:9fe9fcf638444855bfb53d9533551169\", \"a:1:f24847023fd24d8888d9bb644a561a23\": 12, \"a:1:b331de8aa26a481bb0b8dfb3248a45c2\": \"l:62feed1fd0a749e18fd867d44ad332f9\", \"SMART_DefaultAttributeValues\": \"[]\", \"SMART_NewWaypoint\": \"true\" } }},{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.131124, 47.632635] }, \"properties\": { \"deviceId\": \"7b063b84-85a8-43ab-a421-23f24e44aa8a\", \"id\": \"00000000-0000-0000-0000-000000000000\", \"dateTime\": \"2016-07-15T09:37:44.871-07:00\", \"altitude\": 108.645454, \"accuracy\": 10.000000 }}]}";
	public static final String JSON = "{ \"type\": \"FeatureCollection\", \"features\": [{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.132757, 47.633578] }, \"properties\": { \"ctVersion\": \"3.417\", \"dbName\": \"C:\\Users\\Emily\\AppData\\Local\\Temp\\$CT7977.tmp.mdb\", \"appName\": \"defaultvalues\", \"deviceId\": \"0ef80d17-5d2d-4c00-8166-5dcb5bc4e422\", \"id\": \"874787c4-759b-4dcb-96c9-71349f9b24af\", \"dateTime\": \"2016-07-15T12:52:43.119-07:00\", \"latitude\": 47.633578, \"longitude\": -122.132757, \"altitude\": 119.098301, \"accuracy\": 10.000000, \"sighting\": { \"Start New Patrol\": null, \"SMART_PatrolID\": \"577a4c3c531840c89f7408c1ea027892\", \"Begin Patrol\": null, \"SMART_PatrolStartDate\": \"2016/07/15\", \"SMART_PatrolStartTime\": \"12:52:24\", \"SMART_PatrolType\": \"Ground\", \"SMART_PatrolTransport\": \"monkey\", \"SMART_Armed\": \"Yes\", \"SMART_Team\": \"Community Team 1\", \"SMART_Station\": \"Fixed Patrol Post 1\", \"SMART_Mandate\": \"Anti-poaching\", \"m:96b20879d4f54a099bb8113d0f23ea0b\": true, \"m:5f10cce8bea6470398f737bcd7bb44b4\": true, \"m:d27d511500734daca18cf7a5196e522b\": true, \"m:167533be2946495eb79797843bada003\": true, \"SMART_Leader\": \"m:96b20879d4f54a099bb8113d0f23ea0b\", \"SMART_DefaultPatrolValues\": \"[]\", \"Make Observation\": null, \"c:0\": \"Top\", \"c:1\": \"RecentNest\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:a8aa8fcb53ae4e50a00c6b079ecfa48e\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:54db0e557d9041079e630822a2b49f2b\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:b03123ec32884b1c97a001493f66cb13\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:7dd83c4065674344a77e28dc771b0a18\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:49314616277a4208b1260e848e009dda\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:246ac7441d41463f988b14179e30bc82\", \"SMART_DefaultAttributeValues\": \"{\"a:a6e5c90fc4bb4e55890ae1656d7f4f67\":\"t:fbbfb5b6da9f40978d3b02f0746e35d2\",\"a:96f397453cfe434f8e41132b97491d4\":\"l:3817ef13f895479dbdbbbb950fec351e\",\"a:e8de56ea413c462fb46808934483388f\":\"l:393310efdf3049ef828320dd9e53de4b\",\"a:3616d7268e154651a7f88889efc7f236\":\"l:b6280ecf8500403db6cedd4f31cf9fac\"}\", \"SMART_NewWaypoint\": \"Save As New Waypoint\" } }},{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.120755, 47.634839] }, \"properties\": { \"ctVersion\": \"3.417\", \"dbName\": \"C:\\Users\\Emily\\AppData\\Local\\Temp\\$CT7977.tmp.mdb\", \"appName\": \"defaultvalues\", \"deviceId\": \"0ef80d17-5d2d-4c00-8166-5dcb5bc4e422\", \"id\": \"1e7bf256-4f01-48d0-97c9-9a0e37364515\", \"dateTime\": \"2016-07-15T12:52:57.383-07:00\", \"latitude\": 47.634839, \"longitude\": -122.120755, \"altitude\": 133.086939, \"accuracy\": 10.000000, \"sighting\": { \"Start New Patrol\": null, \"SMART_PatrolID\": \"577a4c3c531840c89f7408c1ea027892\", \"Begin Patrol\": null, \"SMART_PatrolStartDate\": \"2016/07/15\", \"SMART_PatrolStartTime\": \"12:52:24\", \"SMART_PatrolType\": \"Ground\", \"SMART_PatrolTransport\": \"monkey\", \"SMART_Armed\": \"Yes\", \"SMART_Team\": \"Community Team 1\", \"SMART_Station\": \"Fixed Patrol Post 1\", \"SMART_Mandate\": \"Anti-poaching\", \"m:96b20879d4f54a099bb8113d0f23ea0b\": true, \"m:5f10cce8bea6470398f737bcd7bb44b4\": true, \"m:d27d511500734daca18cf7a5196e522b\": true, \"m:167533be2946495eb79797843bada003\": true, \"SMART_Leader\": \"m:96b20879d4f54a099bb8113d0f23ea0b\", \"SMART_DefaultPatrolValues\": \"[]\", \"Make Observation\": null, \"c:0\": \"Top\", \"c:1\": \"OldNest\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:a8aa8fcb53ae4e50a00c6b079ecfa48e\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:54db0e557d9041079e630822a2b49f2b\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:b03123ec32884b1c97a001493f66cb13\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:7dd83c4065674344a77e28dc771b0a18\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:49314616277a4208b1260e848e009dda\", \"a:0:2389593093a14c83855d50771f175f2b\": \"t:a03013f9029d444cb60c7317704a2693\", \"a:0:96f397453cfe434f8e41132b97491d40\": \"l:df2a76f72b194606a271188ed1cc094f\", \"SMART_DefaultAttributeValues\": \"{\\\"a:a6e5c90fc4bb4e55890ae1656d7f4f67\\\":\\\"t:e532569963054d5baf742627a31e846f\\\"}\", \"SMART_NewWaypoint\": \"Save As New Waypoint\" } }},{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.132757, 47.633578] }, \"properties\": { \"deviceId\": \"0ef80d17-5d2d-4c00-8166-5dcb5bc4e422\", \"id\": \"00000000-0000-0000-0000-000000000000\", \"dateTime\": \"2016-07-15T12:52:43.119-07:00\", \"altitude\": 119.098301, \"accuracy\": 10.000000 }},{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [-122.120755, 47.634839] }, \"properties\": { \"deviceId\": \"0ef80d17-5d2d-4c00-8166-5dcb5bc4e422\", \"id\": \"00000000-0000-0000-0000-000000000000\", \"dateTime\": \"2016-07-15T12:52:57.383-07:00\", \"altitude\": 133.086939, \"accuracy\": 10.000000 }}]}";
	
	private static DateFormat db = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	
	public static final String SIGHTINGS_KEY = "sighting";
	public static final String FEATURE_TYPE_KEY = "type";
	public static final String PROPERTIES_KEY = "properties";
	
	public static final String LONGITUDE_KEY = "longitude";
	public static final String LATITUDE_KEY = "latitude";
	public static final String DATETIME_KEY = "dateTime";
	
	private JSONParser parser = new JSONParser();
	
	public List<JSONObject> parseFeaturesFromJsonString(String json) throws Exception{
//		json = FileUtils.readFileToString(new File("C:\\temp\\ct.json"));
		JSONObject jsonData = null; 
		try {
			Object obj = parser.parse(json);
			jsonData = (JSONObject) obj;
		}catch (Exception ex){
			ex.printStackTrace();
			throw new Exception("Unable to parse json text.");
		}
		
		JSONArray jsFeatures = (JSONArray) jsonData.get("features");
		List<JSONObject> features = new ArrayList<JSONObject>();
		for (int i = 0; i < jsFeatures.size(); i ++){
			JSONObject feature = (JSONObject) jsFeatures.get(i);
			features.add(feature);
		}
		return features;
	}
	
	/**
	 * Creates a waypoint from the JSON feature object
	 * 
	 * @param sighting
	 * @return
	 */
	public Waypoint createWaypoint(JSONObject feature, Session session) throws Exception{
		
		if (!((String)feature.get(FEATURE_TYPE_KEY)).equalsIgnoreCase("feature")){
			throw new Exception("type is not feature");
		}
		
		JSONObject properties = (JSONObject) feature.get(PROPERTIES_KEY);
		
		//parse x and y may be null
		Waypoint newWaypoint = new Waypoint();
		Double x = (Double)properties.get(LONGITUDE_KEY);
		if (x != null) newWaypoint.setX(x);
		Double y = (Double)properties.get(LATITUDE_KEY);
		if (y != null) newWaypoint.setY(y);
			
		Date dt = db.parse((String)properties.get(DATETIME_KEY));
		newWaypoint.setDateTime(dt);

		newWaypoint.setObservations(new ArrayList<WaypointObservation>());
			
		JSONObject observations = (JSONObject)properties.get(SIGHTINGS_KEY);
		if (observations == null) return newWaypoint;
		
		String categoryUuid = null;
		int catLevel = -1;
		HashMap<Integer, List<ObservationInfo>> attributes = new HashMap<Integer, List<ObservationInfo>>();
		Set<Entry<?,?>> defaultAttributeValues = null;
		Set<Entry> items = observations.entrySet();
		
		for (Entry e : items){
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
				
				String[] bits = key.split (CyberTrackerConfExporter.KEY_SEP);
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
					JSONObject defaults = (JSONObject)parser.parse(jsonDefaults);
					defaultAttributeValues = defaults.entrySet();
				}		
			}
		}
		
		//configure data into observations
		Category category = null;
		if (categoryUuid != null ){
			category = (Category) session.get(Category.class, UuidUtils.stringToUuid(categoryUuid));
			if (category == null){
				throw new Exception("Category not found " + categoryUuid);
			}
		}else{
			throw new Exception("Not cateogry found for observations.");
		}
		
				
		//configure default values
		List<WaypointObservationAttribute> defaultAttributes = new ArrayList<WaypointObservationAttribute>();
		for (Entry defaultValue : defaultAttributeValues){
			String key = (String) defaultValue.getKey();
			String value = (String) defaultValue.getValue();
			
			if (key.startsWith(JsonKey.ATTRIBUTE.key + CyberTrackerConfExporter.KEY_SEP)){
				Attribute a = findAttribute(key.substring(2), session);
				
				WaypointObservationAttribute woa = new WaypointObservationAttribute();
				woa.setAttribute(a);
				setAttributeValue(woa, value, session);
				defaultAttributes.add(woa);
			}
		}
		
		List<ObservationInfo> applyAll = attributes.get(-1);
		List<WaypointObservationAttribute>  applyAllObs = createWaypointObservationAttribute(applyAll, category, defaultAttributes, session);
		if (attributes.entrySet().isEmpty() && categoryUuid != null){
			//create an observation with no attributes (or only default attributes)
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
				if (order == -1) continue;
				List<ObservationInfo> values = (List<ObservationInfo>) e.getValue();
						
				WaypointObservation wp = new WaypointObservation();
				wp.setWaypoint(newWaypoint);
				newWaypoint.getObservations().add(wp);
				wp.setCategory(category);
				wp.setAttributes(new ArrayList<WaypointObservationAttribute>());
						
				for (WaypointObservationAttribute ob : applyAllObs){
					ob.setObservation(wp);
					wp.getAttributes().add(ob);
				}
						
				List<WaypointObservationAttribute>  obs = createWaypointObservationAttribute(values, category, defaultAttributes, session);
				for (WaypointObservationAttribute ob : obs){
					ob.setObservation(wp);
					wp.getAttributes().add(ob);
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
	
	private Attribute findAttribute(String uuid, Session session) throws Exception{
		UUID attributeUuid = UuidUtils.stringToUuid(uuid);
		Attribute a = (Attribute) session.get(Attribute.class, attributeUuid);
		if (a == null) throw new Exception("Attribute not found ");
		return a;
	}
	
	private AttributeListItem findAttributeListItem(String uuid, Session session) throws Exception{
		AttributeListItem ai = (AttributeListItem)session.get(AttributeListItem.class, UuidUtils.stringToUuid(uuid));
		if (ai == null ) throw new Exception("Attribute list item not found .");
		return ai;
	}
	private AttributeTreeNode findAttributeTreeNode(String uuid, Session session) throws Exception{
		AttributeTreeNode ai = (AttributeTreeNode)session.get(AttributeTreeNode.class, UuidUtils.stringToUuid(uuid));
		if (ai == null ) throw new Exception("Attribute tree node not found .");
		return ai;
	}	
	
	private List<WaypointObservationAttribute> createWaypointObservationAttribute(List<ObservationInfo> values, Category c, List<WaypointObservationAttribute> defaultAttributes, Session session) throws Exception{
		
		List<WaypointObservationAttribute> results = new ArrayList<WaypointObservationAttribute>();
		if (values == null) return results;
		//add default values; these will be overridden if actual values are provided
		Set<WaypointObservationAttribute> defaultClones = new HashSet();
		for (WaypointObservationAttribute a : defaultAttributes){
			WaypointObservationAttribute clone = a.clone();
			results.add(clone);
			defaultClones.add(clone);
		}
		
		List<Attribute> validAttributes = new ArrayList<Attribute>();
		c.getAllAttribute(validAttributes, null);
		
		for (ObservationInfo obj : values){
			if (obj.keyType.equals(JsonKey.ATTRIBUTE_LIST.key)){
				//this identifies this list element occur; switch to attribute=element format
				AttributeListItem li =  findAttributeListItem(obj.uuid, session);
				obj.keyType = JsonKey.ATTRIBUTE.key;
				obj.uuid = UuidUtils.uuidToString( li.getAttribute().getUuid() );
			}
			
			if (obj.keyType.equals(JsonKey.ATTRIBUTE.key)){
				Attribute att = findAttribute( obj.uuid, session);
				
				if (!validAttributes.contains(att)) throw new Exception("Attribute " + att.getName() + " not associated with category " + c.getName());
				
				WaypointObservationAttribute wpatt = new WaypointObservationAttribute();

				wpatt.setAttribute(att);
				setAttributeValue(wpatt, obj.value, session);
				
				//check if this attribute already exists for the observation.  Attribute
				//can only exist once except for the tree nodes
				boolean add = true;
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
								throw new Exception("Same attribute cannot be specified twice for single observation");
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
				if (add) results.add(wpatt);
			}
		}
		return results;
	}
	
	private void setAttributeValue(WaypointObservationAttribute toUpdate, Object value, Session session ) throws Exception{
		Attribute att = toUpdate.getAttribute();
	
		if (att.getType() == AttributeType.BOOLEAN){
			if (Boolean.valueOf((String)value)){
				toUpdate.setNumberValue(1.0);
			}else{
				toUpdate.setNumberValue(0.0);
			}	
		}else if (att.getType() == AttributeType.DATE){
			Date date = db.parse((String)value);
			toUpdate.setDateValue(date);
			
		}else if (att.getType() == AttributeType.LIST){
			String listElement = (String) value;
			if (!listElement.startsWith(JsonKey.ATTRIBUTE_LIST.key + CyberTrackerConfExporter.KEY_SEP)) throw new Exception("Invalid value for list attribute: " +listElement);
			
			AttributeListItem li = findAttributeListItem(listElement.substring(2), session);					
			toUpdate.setAttributeListItem(li);
			
		}else if (att.getType() == AttributeType.NUMERIC){
			Double value2 = null;
			if (value instanceof Long){
				value2 = ((Long)value).doubleValue();
			}else if (value instanceof Double){
				value2 = (Double) value;
			}
			toUpdate.setNumberValue(value2);
		}else if (att.getType() == AttributeType.TEXT){
			toUpdate.setStringValue((String)value);
		}else if (att.getType() == AttributeType.TREE){
			String treeElement = (String)value;
			if (!treeElement.startsWith(JsonKey.ATTRIBUTE_TREE.key + CyberTrackerConfExporter.KEY_SEP)) throw new Exception("Invalid value for tree attribute: " +treeElement);
			AttributeTreeNode li = findAttributeTreeNode(treeElement.substring(2), session);
			toUpdate.setAttributeTreeNode(li);
			
		}
	
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
