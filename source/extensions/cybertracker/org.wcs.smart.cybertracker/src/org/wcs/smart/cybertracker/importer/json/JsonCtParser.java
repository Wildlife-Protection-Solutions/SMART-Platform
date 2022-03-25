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
package org.wcs.smart.cybertracker.importer.json;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.bind.DatatypeConverter;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.SignatureTypeManager;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.ImageProcessor;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.properties.ReSizeImageDialog;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.UuidUtils;

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
	public static final String ROOT_ID_KEY = "rootId"; //$NON-NLS-1$

	public static final String DISTANCE_KEY = "distance"; //$NON-NLS-1$
	public static final String DIRECTION_KEY = "bearing"; //$NON-NLS-1$
	
	public static final String DEVICE_ID = "deviceId"; //$NON-NLS-1$
	
	private static final String JPEG_EXT = "jpeg"; //$NON-NLS-1$
	private static final String PHOTO_KEY = "ct_photo"; //$NON-NLS-1$
	
	private static final String WAVE_EXT = "wav"; //$NON-NLS-1$
	private static final String AUDIO_KEY = "ct_audio"; //$NON-NLS-1$
	
	/*
	 * These are keys for the new BETA CT Json format
	 */
	public static final String OBSERVATION_TYPE_KEY = "SMART_ObservationType"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_START_PATROL_KEY = "NewPatrol"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_OBSERVATION_KEY = "Observation"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_END_PATROL_KEY = "StopPatrol"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_PAUSE_PATROL_KEY = "PausePatrol"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_RESUME_PATROL_KEY = "ResumePatrol"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_CHANGE_PATROL_KEY = "ChangePatrol"; //$NON-NLS-1$
	
	/**
	 * The number of months old a patrol is before all links to
	 * this patrol and removed from the database
	 */
	public static final int CLEANUP_MONTHS = 6;
	
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
		if (jsFeatures == null) throw new Exception("No JSON object with key 'features' found"); //$NON-NLS-1$
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
	 * 
	 * @param waypoint the waypoint to size
	 * @param selectedSize the size to apply to the waypoint (to support apply to all); can be null
	 * @param session
	 * @return
	 */
	public static Point processImages(Waypoint waypoint, Point selectedSize, Session session){
		final Point[] selectAllSize = new Point[]{selectedSize};
		
		if (waypoint == null) return selectAllSize[0];
		
		ConservationArea ca = waypoint.getConservationArea();
		CyberTrackerPropertiesOption opResize =  AbstractSmartImporter.getImageResizeOption(ca, session);
		
		if (opResize == null || opResize.getStringValue().equalsIgnoreCase(CyberTrackerPropertiesOption.ImageResizeOption.NONE.name())) return selectAllSize[0];
		
		double maxsizebytes =  AbstractSmartImporter.getImageMaxSizeOption(ca, session) * 1048576l;
		List<ISmartAttachment> attachments = new ArrayList<>();
		if (waypoint.getAttachments() != null ){
			for (WaypointAttachment attachment : waypoint.getAttachments()){
				if (attachment.getCopyFromLocation().toAbsolutePath().toFile().length() >= maxsizebytes)
					attachments.add(attachment);
			}
		}
		
		for (WaypointObservation wo : waypoint.getAllObservations()){
			if (wo.getAttachments() == null) continue;
			for (ObservationAttachment attachment : wo.getAttachments()){
				if (attachment.getCopyFromLocation().toAbsolutePath().toFile().length() >= maxsizebytes)
					attachments.add(attachment);
			}						
		}
		
		
		if (opResize.getStringValue().equalsIgnoreCase(CyberTrackerPropertiesOption.ImageResizeOption.AUTO.name())){
			//attempt to resize image automatically
			int[] size = AbstractSmartImporter.getImageAutoResizeSizeOption(ca, session);		
			for (ISmartAttachment attachment : attachments){
				ImageProcessor.processAttachment(attachment,size[0], size[1]);
			}	
		}else if (opResize.getStringValue().equalsIgnoreCase(CyberTrackerPropertiesOption.ImageResizeOption.MANUAL.name())){
			//prompt user for image size
			for (ISmartAttachment attachment : attachments){
				if (attachment.getCopyFromLocation().toAbsolutePath().toFile().length() < maxsizebytes) continue;
				final BufferedImage image = ImageProcessor.readImage(attachment.getCopyFromLocation());
				if (image == null) continue;
				
				Point[] size = new Point[]{null};

				if (selectAllSize[0] != null){
					size[0] = selectAllSize[0];
				}else{
					//prompt
					Display.getDefault().syncExec(()->{
						ReSizeImageDialog dialog = new ReSizeImageDialog(Display.getDefault().getActiveShell(),attachment,image);
						int open = dialog.open();
						size[0] = dialog.getImageSize();
						if (open == IDialogConstants.YES_TO_ALL_ID){
							selectAllSize[0] = size[0];	
						}
						
					});
				}
				if (size[0] == null || size[0].x == -1 || size[0].y == -1) continue; //do not resize
				ImageProcessor.processAttachment(attachment, size[0].x, size[0].y);	
			}
		}
		return selectAllSize[0];
	}
	
	/**
	 * Determines if the time represented by date1 is between the times
	 * represented by date2 and date3.  Only compares time parts not
	 * date parts.
	 * @return
	 */
	public static boolean isTimeBetween(LocalTime d1, LocalTime d2, LocalTime d3){
		if (d3 == null) d3 = LocalTime.MAX;
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
		
		if (!properties.containsKey(LATITUDE_KEY) || !properties.containsKey(LONGITUDE_KEY)) return null;
		
		Double x = ((Number)properties.get(LONGITUDE_KEY)).doubleValue();
		Double y = ((Number)properties.get(LATITUDE_KEY)).doubleValue(); 
		
		return new Coordinate(x,y);
		
	}
	
	
	/**
	 * Returns the value of the SMART_ObsCounter field or null
	 * if field does not exist
	 * @return
	 */
	public Integer parseObservationCounter(JSONObject sighting) throws Exception{
		//Validate counter
		if (!sighting.containsKey(ScreensUtil.RESULT_OBSERVATION_COUNTER)){
			//no observation counter; we cannot process this
			return null;
		}
		Integer observationCounter = null;
		Object o = sighting.get(ScreensUtil.RESULT_OBSERVATION_COUNTER);
		if (o instanceof Integer) {
			observationCounter = (Integer)o;
		}else if (o instanceof Double) {
			observationCounter = ((Double)o).intValue();
		}else if (o instanceof Number) {
			observationCounter = ((Number)o).intValue();
		}else {
			throw new Exception("Invalid value for observation counter: " + o.toString()); //$NON-NLS-1$
		}
		return observationCounter;
	}
	
	/**
	 * Creates a waypoint from the JSON feature object
	 * 
	 * @param sighting
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Waypoint createWaypoint(JSONObject feature, ConservationArea ca, Session session) throws Exception{
		
		warnings = new ArrayList<String>();
		
		if (!((String)feature.get(FEATURE_TYPE_KEY)).equalsIgnoreCase("feature")){ //$NON-NLS-1$
			throw new Exception(Messages.JsonCtParser_NoFeatureFound);
		}
		
		JSONObject properties = (JSONObject) feature.get(PROPERTIES_KEY);
		
		//parse x and y may be null
		
		Waypoint newWaypoint = new Waypoint();
		Coordinate c = readXYFromProperties(feature);
		if (c == null) {
			throw new Exception(Messages.JsonCtParser_latlongnotfound);
		}
		newWaypoint.setRawX(c.x);
		newWaypoint.setRawY(c.y);
			
		Float direction = null;
		Float distance = null;
		if (properties.containsKey(DIRECTION_KEY) && properties.get(DIRECTION_KEY) != null) {
			direction = ((Number)properties.get(DIRECTION_KEY)).floatValue();
		}
		if (properties.containsKey(DISTANCE_KEY) && properties.get(DISTANCE_KEY) != null) {
			distance = ((Number)properties.get(DISTANCE_KEY)).floatValue();
		}
		newWaypoint.setDirection(direction);
		newWaypoint.setDistance(distance);
		
		LocalDateTime dt = JsonUtils.parseJsonDateTime((String)properties.get(DATETIME_KEY));
		newWaypoint.setDateTime(dt);

		newWaypoint.setObservationGroups(new ArrayList<>());

		//observations are saved in the sightings object
		JSONObject observations = (JSONObject)properties.get(SIGHTINGS_KEY);
		if (observations == null) return newWaypoint;
		
		//category uuid and level; this uuid is associated with the "largest" level
		String categoryUuid = null;
		int catLevel = -1;
		
		//attribute information
		HashMap<Integer, List<ObservationInfo>> attributes = new HashMap<>();
		HashMap<Integer, List<AttachmentInfo>> observationAttachments = new HashMap<>();
		List<AttachmentInfo> waypointAttachments = new ArrayList<>();
		
		//default values
		JSONObject defaultValues = null;
		int imagecounter = 0;
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
			if (key.startsWith(ScreensUtil.RESULT_PHOTO) || key.startsWith(ScreensUtil.RESULT_AUDIO)){
				//SMART_Photo0:0 SMART_Photo1:1 SMART_Photo0:0
				AttachmentInfo.AttachmentType type = AttachmentInfo.AttachmentType.PHOTO;
				if (key.startsWith(ScreensUtil.RESULT_AUDIO)) {
					type = AttachmentInfo.AttachmentType.AUDIO;
				}
				AttachmentInfo info = new AttachmentInfo(type, (String)e.getValue(), imagecounter++);
				if (key.contains(String.valueOf(CyberTrackerConfExporter.KEY_SEP))) {
					int obsnum = Integer.parseInt(key.split(String.valueOf(CyberTrackerConfExporter.KEY_SEP))[1]); 
				
					List<AttachmentInfo> data = observationAttachments.get(obsnum);
					if (data == null){
						data = new ArrayList<AttachmentInfo>();
						observationAttachments.put(obsnum, data);
					}
					data.add(info);
				}else {
					waypointAttachments.add(info);
				}
			}else if (key.startsWith(ScreensUtil.RESULT_SIGNATURE)) {
				//"SMART_Signature_<typekey>:<obsnum>"
				//"SMART_Signature_signaturetypea:0"
				
				//associate signature with observations
				int obsnum = 0;
				String keypart = key;
				if (key.contains(String.valueOf(CyberTrackerConfExporter.KEY_SEP))) {
					String[] bits = key.split(String.valueOf(CyberTrackerConfExporter.KEY_SEP));
					obsnum = Integer.parseInt(bits[1]);
					keypart = bits[0];
				}
				String keyId = keypart.replaceFirst(ScreensUtil.RESULT_SIGNATURE, ""); //$NON-NLS-1$
				SignatureType stype = SignatureTypeManager.INSTANCE.findType(keyId, ca, session);
				
				AttachmentInfo ainfo = null;
				if (stype == null) {
					warnings.add(MessageFormat.format(Messages.JsonCtParser_SignatureTypeNotFoundWarning, keyId));
					ainfo = new AttachmentInfo(AttachmentInfo.AttachmentType.PHOTO, ((String)e.getValue()), imagecounter++);
				}else {
					ainfo = new AttachmentInfo(AttachmentInfo.AttachmentType.SIGNATURE, ((String)e.getValue()), imagecounter++, stype);
				}
				List<AttachmentInfo> data = observationAttachments.get(obsnum);
				if (data == null){
					data = new ArrayList<AttachmentInfo>();
					observationAttachments.put(obsnum, data);
				}
				data.add(ainfo);
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
			
			if (newWaypoint.getObservationGroups().isEmpty()) {
				WaypointObservationGroup g = new WaypointObservationGroup();
				g.setWaypoint(newWaypoint);
				newWaypoint.getObservationGroups().add(g);
				g.setObservations(new ArrayList<>());
			}
			
			category = (Category) session.get(Category.class, UuidUtils.stringToUuid(categoryUuid));
			if (category == null || !category.getConservationArea().equals(ca)){
				//category not found, lets return the waypoint without observations so we can
				//still load the rest of the data if desired
				warnings.add(MessageFormat.format(Messages.JsonCtParser_NoCateogyr, categoryUuid));
				return newWaypoint;
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
		

		WaypointObservationGroup grp = newWaypoint.getObservationGroups().get(0);
		
		if (attributes.entrySet().isEmpty()){
			//create an observation with only default attribute values
			WaypointObservation wp = new WaypointObservation();
			wp.setObserver(observer);
			wp.setObservationGroup(grp);
			grp.getObservations().add(wp);
			wp.setCategory(category);
			wp.setAttributes(new ArrayList<WaypointObservationAttribute>());
			for (WaypointObservationAttribute ob : applyAllObs){
				ob.setObservation(wp);
				wp.getAttributes().add(ob);
			}
			
			//add attachments to observation
			wp.setAttachments(new ArrayList<>());
			for(List<AttachmentInfo> data : observationAttachments.values()) {
				for (AttachmentInfo info : data) {
					WaypointAttachment wa = parseAttachment(info);
					if (wa != null) {
						ObservationAttachment oa = new ObservationAttachment();
						oa.setCopyFromLocation(wa.getCopyFromLocation());
						oa.setFilename(wa.getFilename());
						oa.setSignatureType(wa.getSignatureType());
						oa.setObservation(wp);
						wp.getAttachments().add(oa);
					}
				}
			}
		}else{
			for (Entry<Integer, List<ObservationInfo>> e : attributes.entrySet()){
				int order = e.getKey();
				if (order == CyberTrackerConfExporter.MULTI_SELECT_INDEX) continue; //skip the defaults
				
				List<ObservationInfo> values = (List<ObservationInfo>) e.getValue();
						
				WaypointObservation wp = new WaypointObservation();
				wp.setObserver(observer);
				wp.setObservationGroup(grp);
				grp.getObservations().add(wp);
				wp.setCategory(category);
				
				wp.setAttributes(new ArrayList<WaypointObservationAttribute>());
				
				//add attachments to observation
				List<AttachmentInfo> data = observationAttachments.get(order);
				if (data != null) {
					wp.setAttachments(new ArrayList<>());
					for (AttachmentInfo info : data) {
						WaypointAttachment wa = parseAttachment(info);
						if (wa != null) {
							ObservationAttachment oa = new ObservationAttachment();
							oa.setCopyFromLocation(wa.getCopyFromLocation());
							oa.setFilename(wa.getFilename());
							oa.setObservation(wp);
							oa.setSignatureType(wa.getSignatureType());
							wp.getAttachments().add(oa);
						}
					}
				}
				
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
		//parse waypoint attachments (these are sigantures)
		newWaypoint.setAttachments(new ArrayList<>());
		for (AttachmentInfo ai : waypointAttachments) {
			WaypointAttachment att = parseAttachment(ai);
			att.setWaypoint(newWaypoint);
			newWaypoint.getAttachments().add(att);
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
//	private List<WaypointAttachment> parseAttachments(List<AttachmentInfo> values) throws Exception{
//		int imagecnt = 0;
//		List<WaypointAttachment> attachments = new ArrayList<>();
//		
//		for (AttachmentInfo value : values){
//			
//			if (value.getType() == AttachmentInfo.AttachmentType.PHOTO || value.getType() == AttachmentInfo.AttachmentType.SIGNATURE) {
//				//picture object; create a temporary file add it to waypoint observation
//				String fileName = PHOTO_KEY + "_" + imagecnt + "." + JPEG_EXT;   //$NON-NLS-1$//$NON-NLS-2$
//					
//				Path temp = Files.createTempFile("SMART_" + System.nanoTime(), "." + JPEG_EXT);   //$NON-NLS-1$//$NON-NLS-2$
//				BufferedImage image = null;
//				try(InputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(value.getData()))){
//					image = ImageIO.read(in);					
//				}
//				if (image == null){
//					warnings.add(MessageFormat.format(Messages.JsonCtParser_CouldNotImportPhoto, value));
//				}else{
//					ImageIO.write(image, JPEG_EXT.toUpperCase(Locale.ROOT), temp.toAbsolutePath().toFile());	
//					WaypointAttachment attachment = new WaypointAttachment();
//					attachment.setCopyFromLocation(temp);
//					attachment.setFilename(fileName);
//					attachments.add(attachment);
//					
//					if (value.getType() == AttachmentInfo.AttachmentType.SIGNATURE) {
//						attachment.setSignatureType(value.getSignatureType());
//					}
//				}
//			}else if (value.getType() == AttachmentInfo.AttachmentType.AUDIO) {
//				String fileName = AUDIO_KEY + "_" + imagecnt + "." + WAVE_EXT;   //$NON-NLS-1$//$NON-NLS-2$
//				Path temp = Files.createTempFile("SMART_" + System.nanoTime(), "." + WAVE_EXT);   //$NON-NLS-1$//$NON-NLS-2$
//				try(InputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(value.getData()))){
//					Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
//				}
//				WaypointAttachment attachment = new WaypointAttachment();
//				attachment.setCopyFromLocation(temp);
//				attachment.setFilename(fileName);
//				attachments.add(attachment);	
//				
//			}else {
//				throw new IllegalStateException(MessageFormat.format("Attachment type {0} not supported.", value.getType().name() )); //$NON-NLS-1$
//			}
//		}
//		return attachments;
//	}
	
	private WaypointAttachment parseAttachment(AttachmentInfo info) throws Exception{
		if (info.getType() == AttachmentInfo.AttachmentType.PHOTO
				|| info.getType() == AttachmentInfo.AttachmentType.SIGNATURE) {
			
			// picture object; create a temporary file add it to waypoint observation
			BufferedImage image = null;
			
			//determine file extension; assume jpeg by default
			String ext = JPEG_EXT;
			byte[] data = DatatypeConverter.parseBase64Binary(info.getData());
			try(ImageInputStream iis = ImageIO.createImageInputStream(data)){
				Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
				if (iter.hasNext()) {
					ImageReader ir = iter.next();
					ext = ir.getFormatName();
				}
			}
							
			try (InputStream in = new ByteArrayInputStream(data)) {
				image = ImageIO.read(in);
			}
			
			String fileName = PHOTO_KEY + "_" + info.getImageCount() + "." + ext; //$NON-NLS-1$//$NON-NLS-2$
			Path temp = Files.createTempFile("SMART_" + System.nanoTime(), "." + ext); //$NON-NLS-1$//$NON-NLS-2$
				
			if (image == null) {
				warnings.add(MessageFormat.format(Messages.JsonCtParser_CouldNotImportPhoto, info));
				return null;
			} else {
				ImageIO.write(image, ext.toUpperCase(Locale.ROOT), temp.toAbsolutePath().toFile());
				WaypointAttachment attachment = new WaypointAttachment();
				attachment.setCopyFromLocation(temp);
				attachment.setFilename(fileName);
				if (info.getType() == AttachmentInfo.AttachmentType.SIGNATURE) {
					attachment.setSignatureType(info.getSignatureType());
				}
				return attachment;
			}
		} else if (info.getType() == AttachmentInfo.AttachmentType.AUDIO) {
			String fileName = AUDIO_KEY + "_" + info.getImageCount() + "." + WAVE_EXT; //$NON-NLS-1$//$NON-NLS-2$
			Path temp = Files.createTempFile("SMART_" + System.nanoTime(), "." + WAVE_EXT); //$NON-NLS-1$//$NON-NLS-2$
			try (InputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(info.getData()))) {
				Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
			}
			WaypointAttachment attachment = new WaypointAttachment();
			attachment.setCopyFromLocation(temp);
			attachment.setFilename(fileName);
			return attachment;

		} else {
			throw new IllegalStateException(
					MessageFormat.format("Attachment type {0} not supported.", info.getType().name())); //$NON-NLS-1$
		}

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
				
		HashMap<Attribute, ObservationInfo> mitems = new HashMap<>();
		
		for (ObservationInfo obj : values){
			if (obj.keyType.equals(JsonKey.ATTRIBUTE_LIST.key)){
				//this identifies this list element occur; switch to attribute=element format
				AttributeListItem li =  findAttributeListItem(obj.uuid, session);
				if (li == null){
					warnings.add(MessageFormat.format(Messages.JsonCtParser_ListAttributeNotFound, obj.uuid));
				}else{
					if (li.getAttribute().getType() == AttributeType.MLIST) {
						ObservationInfo info = mitems.get(li.getAttribute());
						if (info == null) {
							info = new ObservationInfo(JsonKey.ATTRIBUTE.key, UuidUtils.uuidToString(li.getAttribute().getUuid()), new ArrayList<>());
							mitems.put(li.getAttribute(), info);
						}
						((ArrayList<AttributeListItem>)info.value).add(li);
					}else {
						obj.keyType = JsonKey.ATTRIBUTE.key;
						obj.uuid = UuidUtils.uuidToString( li.getAttribute().getUuid() );
					}
				}
			}
		}
		values.addAll(mitems.values());
		
		for (ObservationInfo obj : values){
			if (obj.keyType.equals(JsonKey.ATTRIBUTE_LIST.key))continue; //processed above
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
	
	
	private static class AttachmentInfo{
		enum AttachmentType {PHOTO, AUDIO, SIGNATURE};
		
		private AttachmentType type;
		private String data;
		private SignatureType stype;
		private int count;
		
		public AttachmentInfo(AttachmentType type, String data, int count, SignatureType stype) {
			this(type, data, count);
			this.stype = stype;
		}
		
		public AttachmentInfo(AttachmentType type, String data, int count) {
			this.data = data;
			this.type = type;
			this.stype = null;
			this.count = count;
		}
		public int getImageCount() {
			return this.count;
		}
		public SignatureType getSignatureType() {
			return this.stype;
		}
		public String getData() {
			return this.data;
		}
		public AttachmentType getType() {
			return this.type;
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
