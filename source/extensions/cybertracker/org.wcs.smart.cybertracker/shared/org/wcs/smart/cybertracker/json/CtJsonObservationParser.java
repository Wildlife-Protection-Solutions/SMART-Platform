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
package org.wcs.smart.cybertracker.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.AttachmentTagManager;
import org.wcs.smart.SignatureTypeManager;
import org.wcs.smart.ca.AttachmentTag;
//import org.wcs.smart.SignatureTypeManager;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.cybertracker.ImageProcessor;
import org.wcs.smart.cybertracker.json.CtJsonUtil.JsonDataModelKey;
import org.wcs.smart.cybertracker.json.JsonImportWarning.Type;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
//import org.wcs.smart.cybertracker.properties.ReSizeImageDialog;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.observation.model.AttachmentTagLink;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.UuidUtils;

import jakarta.xml.bind.DatatypeConverter;

/**
 * Parses sighting data from cybertracker JSON data.
 * 
 * After processing is complete, users should call cleanUp
 * to remove any temporary files
 * 
 * @author Emily
 *
 */
public class CtJsonObservationParser {

	protected Logger logger = Logger.getLogger(CtJsonObservationParser.class.getName());

	public static final String FEATURE_KEY = "feature"; //$NON-NLS-1$
	public static final String FEATURES_KEY = "features"; //$NON-NLS-1$
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
	
	public static final String CM_UUID_KEY = "SMART_cmUuid"; //$NON-NLS-1$

	public static final String DISTANCE_KEY = "distance"; //$NON-NLS-1$
	public static final String DIRECTION_KEY = "bearing"; //$NON-NLS-1$
	
	public static final String DEVICE_ID = "deviceId"; //$NON-NLS-1$
	
	private static final String PHOTO_KEY = "ct_photo"; //$NON-NLS-1$
	
	private static final String WAVE_EXT = "wav"; //$NON-NLS-1$
	private static final String AUDIO_KEY = "ct_audio"; //$NON-NLS-1$
	
	/*
	 * These are keys for the new BETA CT Json format
	 */
	public static final String OBSERVATION_TYPE_START_PATROL_KEY = "NewPatrol"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_OBSERVATION_KEY = "Observation"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_END_PATROL_KEY = "StopPatrol"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_PAUSE_PATROL_KEY = "PausePatrol"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_RESUME_PATROL_KEY = "ResumePatrol"; //$NON-NLS-1$
	public static final String OBSERVATION_TYPE_CHANGE_PATROL_KEY = "ChangePatrol"; //$NON-NLS-1$
		
	private JSONParser parser = new JSONParser();
	
	private List<JsonImportWarning> warnings = null;
	
	private List<WaypointObservationAttribute> applyToAllObservations;
	private Locale l;
	private List<Path> tempFiles;
	
	public CtJsonObservationParser(Locale l) {
		this.l = l;
		this.tempFiles = new ArrayList<>();
	}
	
	private void logException(Exception ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex);
	}
	
	public List<JsonImportWarning> getWarnings(){
		return this.warnings;
	}
	
	public List<Path> getTemporaryFiles() {
		return tempFiles;
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
		if (!sighting.containsKey(CtJsonUtil.JsonKey.OBSERVATION_COUNTER.key)){
			//no observation counter; we cannot process this
			return null;
		}
		Integer observationCounter = null;
		Object o = sighting.get(CtJsonUtil.JsonKey.OBSERVATION_COUNTER.key);
		if (o instanceof Number) {
			observationCounter = ((Number)o).intValue();
		}else {
			throw new Exception((new JsonError(JsonError.Type.INVALID_OBS_COUNTER, o.toString())).getMessage(l)); 
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
		
		warnings = new ArrayList<JsonImportWarning>();
		
		if (!((String)feature.get(FEATURE_TYPE_KEY)).equalsIgnoreCase(FEATURE_KEY)){
			throw new Exception((new JsonError(JsonError.Type.FEATURE_OBJECT_NOT_FOUND, FEATURE_KEY)).getMessage(l));
		}
		
		JSONObject properties = (JSONObject) feature.get(PROPERTIES_KEY);
		
		//parse x and y may be null
		
		Waypoint newWaypoint = new Waypoint();
		newWaypoint.setConservationArea(ca);
		Coordinate c = readXYFromProperties(feature);
		if (c == null) {
			throw new Exception((new JsonError(JsonError.Type.LAT_LONG_NOT_FOUND)).getMessage(l));
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
		
		LocalDateTime dt = CtJsonUtil.parseJsonDateTime((String)properties.get(DATETIME_KEY));
		newWaypoint.setDateTime(dt);

		newWaypoint.setObservationGroups(new ArrayList<>());

		//observations are saved in the sightings object
		JSONObject observations = (JSONObject)properties.get(SIGHTINGS_KEY);
		if (observations == null) return newWaypoint;
		
		//TODO: what happens with a waypoint with no observations
		if (observations.containsKey(CM_UUID_KEY)) {
			//configure cm associated with waypoint (new 7.5.7)
			String cmuuid = observations.get(CM_UUID_KEY).toString();
			ConfigurableModel cm = null;
			UUID cmUuid = null;
			try {
				cmUuid = UuidUtils.stringToUuid(cmuuid);
				cm = session.get(ConfigurableModel.class, cmUuid);
				if (cm == null) warnings.add(new JsonImportWarning(JsonImportWarning.Type.INVALID_CM, cmuuid));				
			}catch (Exception ex) {
				logException(ex);
				warnings.add(new JsonImportWarning(JsonImportWarning.Type.INVALID_CM, cmuuid));
			}
			
			newWaypoint.setSourceConfigurableModel(cm);
		}
		
		
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
		
		//parse other observation details (categories/attribute/ etc)
		for (Entry<?,?> e : (Set<Entry<?,?>>)observations.entrySet()){
			
			String key = (String)e.getKey();
			
			//CATEGORY
			if (key.startsWith(JsonDataModelKey.CATEGORY.key + CtJsonUtil.KEY_SEP)){
				//cateogries; if set to null this was a group from the configurable model
				//otherwise we want to find the "leaf" category
				//FORM: '"c:0": "null", "c:1": "<uuid>"'
				if (!((String)e.getValue()).equals(CtJsonUtil.NULL_KEY)){
					int level = Integer.parseInt(key.substring(2));
					if (level > catLevel){
						categoryUuid = (String)e.getValue();
					}
				}
			}
			
			//ATTRIBUTES
			if (key.startsWith(JsonDataModelKey.ATTRIBUTE.key + CtJsonUtil.KEY_SEP) ||
					key.startsWith(JsonDataModelKey.ATTRIBUTE_LIST.key + CtJsonUtil.KEY_SEP) ||
					key.startsWith(JsonDataModelKey.ATTRIBUTE_MULTILIST.key + CtJsonUtil.KEY_SEP)
					){
				
				String[] bits = key.split (CtJsonUtil.KEY_SEP.toString());
				//identifies which observation this attribute applies to
				int obsnum = Integer.parseInt(bits[1]);
				List<ObservationInfo> data = attributes.get(obsnum);
				if (data == null){
					data = new ArrayList<ObservationInfo>();
					attributes.put(obsnum, data);
				}
				
				if (key.startsWith(JsonDataModelKey.ATTRIBUTE.key + CtJsonUtil.KEY_SEP)){	
					//attributes
					//FORM: '"a:0:<uuid>": "value"'
					ObservationInfo info = new ObservationInfo(bits[0], bits[2], e.getValue());
					data.add(info);
				}
				if (key.startsWith(JsonDataModelKey.ATTRIBUTE_LIST.key + CtJsonUtil.KEY_SEP)){
					//attribute list item
					//FORM: '"al:0:<uuid>": true'
					data.add(new ObservationInfo(JsonDataModelKey.ATTRIBUTE_LIST.key, (String)bits[2], JsonDataModelKey.ATTRIBUTE_LIST.key + CtJsonUtil.KEY_SEP + (String)bits[2]));
				}
			
				if (key.startsWith(JsonDataModelKey.ATTRIBUTE_MULTILIST.key + CtJsonUtil.KEY_SEP)){
					//multi lists
					//FORM: '"ml:0:<attributeuuid>:l:<listitemuuid>": value'
					//number attribute
					data.add(new ObservationInfo(JsonDataModelKey.ATTRIBUTE.key, (String)bits[2], e.getValue()));
					//list attribute
					data.add(new ObservationInfo(JsonDataModelKey.ATTRIBUTE_LIST.key, (String)bits[4], JsonDataModelKey.ATTRIBUTE_LIST.key + CtJsonUtil.KEY_SEP + (String)bits[4]));
				}
			}
			
			//PHOTO/AUDIO file
			if (key.startsWith(CtJsonUtil.JsonKey.PHOTO.key) || key.startsWith(CtJsonUtil.JsonKey.AUDIO.key)){
				//SMART_Photo0:0 SMART_Photo1:1 SMART_Photo0:0
				AttachmentInfo.AttachmentType type = AttachmentInfo.AttachmentType.PHOTO;
				if (key.startsWith(CtJsonUtil.JsonKey.AUDIO.key)) {
					type = AttachmentInfo.AttachmentType.AUDIO;
				}
				AttachmentInfo info = new AttachmentInfo(type, (String)e.getValue(), 
						AttachmentInfo.parseTagsFromData(((String)e.getValue()), session, ca, warnings),
						imagecounter++);
				if (key.contains(String.valueOf(CtJsonUtil.KEY_SEP))) {
					int obsnum = Integer.parseInt(key.split(String.valueOf(CtJsonUtil.KEY_SEP))[1]); 
				
					List<AttachmentInfo> data = observationAttachments.get(obsnum);
					if (data == null){
						data = new ArrayList<AttachmentInfo>();
						observationAttachments.put(obsnum, data);
					}
					data.add(info);
				}else {
					waypointAttachments.add(info);
				}
			}else if (key.startsWith(CtJsonUtil.JsonKey.SIGNATURE.key)) {
				//"SMART_Signature_<typekey>:<obsnum>"
				//"SMART_Signature_signaturetypea:0"
				
				//associate signature with observations
				int obsnum = 0;
				String keypart = key;
				if (key.contains(String.valueOf(CtJsonUtil.KEY_SEP))) {
					String[] bits = key.split(String.valueOf(CtJsonUtil.KEY_SEP));
					obsnum = Integer.parseInt(bits[1]);
					keypart = bits[0];
				}
				String keyId = keypart.replaceFirst(CtJsonUtil.JsonKey.SIGNATURE.key, ""); //$NON-NLS-1$
				SignatureType stype = SignatureTypeManager.INSTANCE.findType(keyId, ca, session);
				
				AttachmentInfo ainfo = null;
				String imagedata = ((String)e.getValue());
				List<AttachmentTag> tags = AttachmentInfo.parseTagsFromData(imagedata, session, ca, warnings);
				if (stype == null) {
					warnings.add(new JsonImportWarning(Type.INVALID_SIGNATURE, keyId));
					ainfo = new AttachmentInfo(AttachmentInfo.AttachmentType.PHOTO, imagedata,tags, imagecounter++);
				}else {
					ainfo = new AttachmentInfo(AttachmentInfo.AttachmentType.SIGNATURE, imagedata, tags, imagecounter++, stype);
				}
				List<AttachmentInfo> data = observationAttachments.get(obsnum);
				if (data == null){
					data = new ArrayList<AttachmentInfo>();
					observationAttachments.put(obsnum, data);
				}
				data.add(ainfo);
			}
			
			//default values
			if (key.equalsIgnoreCase(CtJsonUtil.JsonKey.DEFAULT_ATTRIBUTE_VALUES.key) ){
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
			try {
				category = CtJsonUtil.findCategory(categoryUuid, session);
				if (category == null || !category.getConservationArea().equals(ca)){
					//category not found, lets return the waypoint without observations so we can
					//still load the rest of the data if desired
					warnings.add(new JsonImportWarning(JsonImportWarning.Type.CATEGORY_NOT_FOUND, categoryUuid));
					return newWaypoint;
				}
			}catch (Exception ex) {
				logException(ex);
				warnings.add(new JsonImportWarning(JsonImportWarning.Type.CATEGORY_NOT_FOUND, categoryUuid));
				return newWaypoint;
			}
			
		}else{
			//no category found, so lets assume no observations
			//happens on patrol pause/resume
			return newWaypoint;
		}
		
				
		//configure default values
		CtJsonUtil.ParseResult defaults = CtJsonUtil.parseDefaultAttributeValues(defaultValues, session);
		warnings.addAll(defaults.getWarnings());
		
		List<WaypointObservationAttribute> defaultAttributes = defaults.getAttributes(); 
				
		//these attribute values must be applied to all observations
		List<WaypointObservationAttribute>  applyAllObs = createWaypointObservationAttribute(attributes.get(CtJsonUtil.MULTI_SELECT_INDEX), category, defaultAttributes, session);
		applyToAllObservations = applyAllObs;
		
		//observer
		Employee observer = null;
		if (observations.containsKey(CtJsonUtil.JsonKey.OBSERVER.key)){
			String ob = (String) observations.get(CtJsonUtil.JsonKey.OBSERVER.key);
			if (ob.startsWith(JsonDataModelKey.EMPLOYEE.key + CtJsonUtil.KEY_SEP)){
				String uuid = ob.substring(JsonDataModelKey.EMPLOYEE.key.length() + 1);
				try {
					observer = (Employee) session.get(Employee.class, UuidUtils.stringToUuid(uuid));
					if (observer == null){
						warnings.add(new JsonImportWarning(Type.INVALID_OBSERVER, uuid));
					}
				}catch (Exception ex) {
					logException(ex);
					warnings.add(new JsonImportWarning(Type.INVALID_OBSERVER, uuid));
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
						for (AttachmentTagLink link : wa.getAttachmentTags()) {
							AttachmentTagLink clone = new AttachmentTagLink();
							clone.setTag(link.getTag());
							clone.setObservationAttachment(oa);
							oa.getAttachmentTags().add(clone);
						}
						wp.getAttachments().add(oa);
					}
				}
			}
		}else{
			for (Entry<Integer, List<ObservationInfo>> e : attributes.entrySet()){
				int order = e.getKey();
				if (order == CtJsonUtil.MULTI_SELECT_INDEX) continue; //skip the defaults
				
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
							oa.setAttachmentTags(new ArrayList<>());
							for (AttachmentTagLink link : wa.getAttachmentTags()) {
								AttachmentTagLink clone = new AttachmentTagLink();
								clone.setTag(link.getTag());
								clone.setObservationAttachment(oa);
								oa.getAttachmentTags().add(clone);
							}
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
	 * 
	 * @param waypoint the waypoint to size
	 * @param selectedSize the size to apply to the waypoint (to support apply to all); can be null
	 * @param session
	 * @return
	 */
	public void processImages(Waypoint waypoint, Session session){
		if (waypoint == null) return;
		
		ConservationArea ca = waypoint.getConservationArea();
		CyberTrackerPropertiesOption opResize =  CtJsonUtil.getImageResizeOption(ca, session);
		
		if (opResize == null) return;		
		if (opResize.getStringValue().equalsIgnoreCase(CyberTrackerPropertiesOption.ImageResizeOption.NONE.name())) return;
		
		double maxsizebytes =  CtJsonUtil.getImageMaxSizeOption(ca, session) * 1048576l;
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
			int[] size = CtJsonUtil.getImageAutoResizeSizeOption(ca, session);		
			for (ISmartAttachment attachment : attachments){
				Path[] files = ImageProcessor.INSTANCE.processAttachment(attachment,size[0], size[1]);
				for (Path p : files) tempFiles.add(p);
			}
		}
	}
	
	private WaypointAttachment parseAttachment(AttachmentInfo info) throws Exception{
		if (info.getType() == AttachmentInfo.AttachmentType.PHOTO
				|| info.getType() == AttachmentInfo.AttachmentType.SIGNATURE) {
			
			// picture object; create a temporary file add it to waypoint observation
			
			//determine file extension
			String ext = null;
			byte[] data = DatatypeConverter.parseBase64Binary(info.getData());
			
			try(ByteArrayInputStream bis = new ByteArrayInputStream(data);
					ImageInputStream iis = ImageIO.createImageInputStream(bis)){
				Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
				if (iter.hasNext()) {
					ImageReader ir = iter.next();
					ext = ir.getFormatName();
				}
			}
								
			String fileName = PHOTO_KEY + "_" + info.getImageCount(); //$NON-NLS-1$
			Path temp = null;
			if (ext == null){
				//don't know the image format so write without an extension and generate a warning
				warnings.add(new JsonImportWarning(Type.INVALID_PHOTO_ATTACHMENT, info.getImageCount()));
				
				fileName = PHOTO_KEY + "_" + info.getImageCount(); //$NON-NLS-1$
				temp = Files.createTempFile("SMART_" + System.nanoTime(), "unknown"); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					Files.write(temp, data);
					tempFiles.add(temp);
				}catch (IOException ex) {
					warnings.add(new JsonImportWarning(Type.INVALID_ATTACHMENT, info.getImageCount()));
					logger.log(Level.WARNING, ex.getMessage(), ex);
					return null;
				}
				
				
				
			} else {
				fileName = PHOTO_KEY + "_" + info.getImageCount() + "." + ext; //$NON-NLS-1$//$NON-NLS-2$
				temp = Files.createTempFile("SMART_" + System.nanoTime(), "." + ext); //$NON-NLS-1$//$NON-NLS-2$
				try {
					Files.write(temp, data);
					tempFiles.add(temp);
				}catch (IOException ex) {
					warnings.add(new JsonImportWarning(Type.INVALID_ATTACHMENT, info.getImageCount()));
					logger.log(Level.WARNING, ex.getMessage(), ex);
					return null;
				}
				
			}
			
			WaypointAttachment attachment = new WaypointAttachment();
			attachment.setCopyFromLocation(temp);
			attachment.setFilename(fileName);
			if (info.getType() == AttachmentInfo.AttachmentType.SIGNATURE) {
				attachment.setSignatureType(info.getSignatureType());
			}
			attachment.setAttachmentTags(new ArrayList<>());
			for (AttachmentTag t : info.getTags()) {
				AttachmentTagLink link = new AttachmentTagLink();
				link.setTag(t);
				link.setWaypointAttachment(attachment);
				attachment.getAttachmentTags().add(link);
			}
			
			return attachment;
			
		} else if (info.getType() == AttachmentInfo.AttachmentType.AUDIO) {
			String fileName = AUDIO_KEY + "_" + info.getImageCount() + "." + WAVE_EXT; //$NON-NLS-1$//$NON-NLS-2$
			Path temp = Files.createTempFile("SMART_" + System.nanoTime(), "." + WAVE_EXT); //$NON-NLS-1$//$NON-NLS-2$
			try (InputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(info.getData()))) {
				Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
				tempFiles.add(temp);
			}
			WaypointAttachment attachment = new WaypointAttachment();
			attachment.setCopyFromLocation(temp);
			attachment.setFilename(fileName);
			attachment.setAttachmentTags(new ArrayList<>());

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
	@SuppressWarnings("unchecked")
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
			if (obj.keyType.equals(JsonDataModelKey.ATTRIBUTE_LIST.key)){
				//this identifies this list element occur; switch to attribute=element format
				AttributeListItem li = CtJsonUtil.findAttributeListItem(obj.uuid, session);
				if (li == null){
					warnings.add(new JsonImportWarning(Type.LIST_ATTRIBUTE_NOT_FOUND, obj.uuid));
				}else{
					if (li.getAttribute().getType() == AttributeType.MLIST) {
						ObservationInfo info = mitems.get(li.getAttribute());
						if (info == null) {
							info = new ObservationInfo(JsonDataModelKey.ATTRIBUTE.key, UuidUtils.uuidToString(li.getAttribute().getUuid()), new ArrayList<>());
							mitems.put(li.getAttribute(), info);
						}
						
						((ArrayList<AttributeListItem>)info.value).add(li);
					}else {
						obj.keyType = JsonDataModelKey.ATTRIBUTE.key;
						obj.uuid = UuidUtils.uuidToString( li.getAttribute().getUuid() );
					}
				}
			}
		}
		values.addAll(mitems.values());
		
		for (ObservationInfo obj : values){
			if (obj.keyType.equals(JsonDataModelKey.ATTRIBUTE_LIST.key))continue; //processed above
			if (obj.keyType.equals(JsonDataModelKey.ATTRIBUTE.key)){
				Attribute att = CtJsonUtil.findAttribute( obj.uuid, session);
				if (att == null){
					warnings.add(new JsonImportWarning(Type.ATTRIBUTE_NOT_FOUND, obj.uuid));
					continue;
				}
				if (!validAttributes.contains(att)) {
					warnings.add(new JsonImportWarning(Type.ATT_CAT_NOT_ASSOCIATED, att.getName(), c.getName()));
					continue;
				}
				
				boolean add = true;
				WaypointObservationAttribute wpatt = new WaypointObservationAttribute();
				try{
					wpatt.setAttribute(att);
					if (!CtJsonUtil.setAttributeValue(wpatt, obj.value, session, warnings)){
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
										warnings.add(new JsonImportWarning(Type.DUPLICATE_ATTRIBUTES, att.getName()));
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
					logException(ex);
					warnings.add(new JsonImportWarning(Type.OBS_ATTRIBUTE_PARSE_ERROR, att.getName(), obj.value, ex.getLocalizedMessage()));
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
		private List<AttachmentTag> tags;
		
		public static final List<AttachmentTag> parseTagsFromData(String data, Session session, ConservationArea ca, List<JsonImportWarning> warnings) {
			
			int tagsindex = data.indexOf(':');
			if (tagsindex < 0) return Collections.emptyList();
			
			List<AttachmentTag> tags = new ArrayList<>();
			
			List<AttachmentTag> dbtags = AttachmentTagManager.INSTANCE.getTags(session, ca);
					
			String alltags = data.substring(tagsindex + 1);
			data = data.substring(0, tagsindex);
			Set<String> processed = new HashSet<>();
			
			for (String t : alltags.split(":")) { //$NON-NLS-1$
				t = t.strip();
				if (processed.contains(t)) continue;
				processed.add(t);
				
				AttachmentTag found = null;
				for (AttachmentTag tag : dbtags) {
					if (tag.getKeyId().equalsIgnoreCase(t)) {
						found = tag;
						break;
					}
				}
				if (found != null) {
					tags.add(found);
				}else {
					warnings.add(new JsonImportWarning(Type.TAG_NOT_FOUND, t));
				}
			}
			return tags;			
		}
		
		
		public AttachmentInfo(AttachmentType type, String data, List<AttachmentTag> tags,  int count, SignatureType stype) {
			this(type, data, tags, count);
			this.stype = stype;
		}
		
		public AttachmentInfo(AttachmentType type, String data, List<AttachmentTag> tags, int count) {
			this.data = data;
			this.type = type;
			this.stype = null;
			this.tags = tags;
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
		public List<AttachmentTag> getTags(){
			return this.tags;
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
