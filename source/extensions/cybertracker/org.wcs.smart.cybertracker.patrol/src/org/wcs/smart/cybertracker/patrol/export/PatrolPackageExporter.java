/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils.Type;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.PatrolCtPackage;
import org.wcs.smart.cybertracker.patrol.model.PatrolMetadataField;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.dataentry.model.xml.CmSmartToXmlConverter;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Patrol Cybertracker package exporter. This exports the following details into
 * a single zip file for use with Cybertracker. Included are: 
 * configurable model xml (cm_model.xml), 
 * configurable model image files, 
 * cybertracker configuration options (ct_profile.json)
 * the patrol metadata options (patrol_metadata.json).
 * the conservation area logo 
 * directory with map files to use in the ct application (selected by user on export)
 * 
 * 
 * This may be expanded in the future to include mapping
 * data or other data useful for Cybertracker.
 */
public enum PatrolPackageExporter {

	INSTANCE;
	
	public static final String CM_MODEL_FILE = "cm_model.xml"; //$NON-NLS-1$
	
	public static final String PATROL_METADATA_FILE = "patrol_metadata.json"; //$NON-NLS-1$
	
	public static final String CT_PROFILE_FILE = "ct_profile.json"; //$NON-NLS-1$
	
	/**
	 * Exports patrol cybertracker package.
	 * 
	 * @param cm
	 * @param profile
	 * @param exportFile
	 * @param monitor
	 * @throws Exception
	 */
	public void exportPackage(PatrolCtPackage ctPackage, List<IPackageContribution.PackageContribution> updates, Path exportFile, IEclipseContext context, IProgressMonitor monitor) throws Exception{
		//TODO: support cancelling
		SubMonitor sub = SubMonitor.convert(monitor, Messages.PatrolPackageExporter_TaskName, 8);
		Path tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		try {
			try(Session session = HibernateManager.openSession()){
				//the ctpackage object is configured with the model to use
				ConfigurableModel modelToExport = ctPackage.getConfigurableModel();
				if (modelToExport.getUuid() != null) {
					modelToExport = session.get(ConfigurableModel.class, modelToExport.getUuid());
				}
				
				//reload package so we don't have hiberante issues
				PatrolCtPackage localpackage = session.get(PatrolCtPackage.class, ctPackage.getUuid());		

				List<File> toIncludeInZip = new ArrayList<>();
				HashMap<String, Object> projectAdditions = new HashMap<>();
				HashMap<String, Object> ctprofileAdditions = new HashMap<>();
				
				for (IPackageContribution.PackageContribution update : updates) {
					for (Path p : update.getAddedFiles()) {
						if (Files.isDirectory(p)) {
							Path dirPath = tempDir.resolve(p.getFileName().toString());
							Files.createDirectory(dirPath);
							Path mapfiles = CtJsonExportUtils.copyFiles(p, dirPath);
							if (mapfiles != null) toIncludeInZip.add(mapfiles.toFile());
							
						}else {
							Path moveTo = tempDir.resolve(p.getFileName().toString());
							Files.move(p, moveTo);
							toIncludeInZip.add(moveTo.toFile());
						}
					}
					if (update.getProjectMetadata() != null) {
						projectAdditions.putAll(update.getProjectMetadata());
					}
					if (update.getProfileMetadata() != null) {
						ctprofileAdditions.putAll(update.getProfileMetadata());
					}
				}
				
				Path cmFile = tempDir.resolve(CM_MODEL_FILE);
				
				
				//convert to xml
				org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel = CmSmartToXmlConverter.convert(modelToExport, true, sub.split(1));

				//create and add help files
				toIncludeInZip.addAll( CtJsonExportUtils.addHelpFiles(xmlModel, tempDir) );

				//write xml
				try(OutputStream out = Files.newOutputStream(cmFile)){
					CmXmlManager.writeDataModel(xmlModel, out);
				}
				toIncludeInZip.add(cmFile.toFile());
				
				//include configurable model image files
				sub.split(1);
				File dataFolder = new File(modelToExport.getFileDataStoreLocation());
				if (dataFolder != null && dataFolder.exists() && dataFolder.isDirectory()) {
					toIncludeInZip.addAll(Arrays.asList(dataFolder.listFiles()));
				}
				
				//include data model image files and update xmlModel
				sub.split(1);
				includeDmIcons(modelToExport, toIncludeInZip, tempDir);
				
				//include ca logo
				Path logo = modelToExport.getConservationArea().getLogo();
				if (logo != null && Files.exists(logo)) {
					toIncludeInZip.add(logo.toFile());
				}
				
				sub.split(1);
				Path metadataFile = tempDir.resolve(PATROL_METADATA_FILE);
				metadataToJson(localpackage, session,  metadataFile);
				toIncludeInZip.add(metadataFile.toFile());

				sub.split(1);
				Path profileFile = tempDir.resolve(CT_PROFILE_FILE);
				ObservationOptions ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
				profileToJson(session.get(CyberTrackerPropertiesProfile.class, localpackage.getCtProfile().getUuid()), ops.getTrackDistanceDirection(), session, context, profileFile, ctprofileAdditions);
				toIncludeInZip.add(profileFile.toFile());
				
				
				//get version number from output file
				String version = null;
				if (localpackage.getUuid() != null) {
					String fname = exportFile.getFileName().toString();
					int start = fname.indexOf('.') + 1;
					int end = fname.lastIndexOf('.');
					version = fname.substring(start,end);
				}
				
				sub.split(1);
				Path projectFile = tempDir.resolve(CtJsonExportUtils.PROJECT_FILE);
				
				
				writeProjectFile(localpackage.getName(), modelToExport, version, logo, projectFile, metadataFile, projectAdditions);
				toIncludeInZip.add(projectFile.toFile());
				
				ZipUtil.createZip(toIncludeInZip.toArray(new File[toIncludeInZip.size()]), exportFile.toFile(), sub.split(1));
			}
		}finally {
			try {
				FileUtils.forceDelete(tempDir.toFile());
			} catch (IOException e) {
				CyberTrackerPlugIn.log("Error cleaning up directory after exporting ct patrol package", e); //$NON-NLS-1$
			}
			for (IPackageContribution.PackageContribution update : updates) {
				update.cleanUp();
			}
		}
	}
	
	private void processFile(DmObject object, IImageAssociatedObject cmObject, ConfigurableModel cm, 
			List<File> toIncludeInZip, Path tempDir) throws IOException {
		IconFile file = object.getIcon().getIconFile(cm.getIconSet());
		if (file != null) {
			Path fromPath = file.getAttachmentFile().toPath();
			String fileName = cmObject.getImageFile().getName();
			if (cmObject.getUuid() == null) {
				fileName = UuidUtils.uuidToString(object.getUuid());
			}
			Path toPath = tempDir.resolve(SharedUtils.getFilenameWithoutExtension(fileName) + "." + SharedUtils.getFilenameExtension(fromPath.getFileName().toString())); //$NON-NLS-1$
			if (Files.exists(toPath)) return;
			Files.copy(fromPath, toPath);
			if (!toIncludeInZip.contains(toPath.toFile())) toIncludeInZip.add(toPath.toFile());
		}
	}
	
	
	private void includeDmIcons(ConfigurableModel cm, List<File> toIncludeInZip, Path tempDir) throws IOException {
		List<Object> toProcess = new ArrayList<>();
		toProcess.addAll(cm.getNodes());
		List<Object> processed = new ArrayList<>();
		while(!toProcess.isEmpty()) {
			Object objectNode = toProcess.remove(0);
			if (processed.contains(objectNode)) continue;
			processed.add(objectNode);
			
			if (objectNode instanceof CmNode) {
				CmNode node = (CmNode)objectNode;
				toProcess.addAll(node.getChildren());
				
				if (!node.hasCustomImage() && node.getCategory() != null && node.getCategory().getIcon() != null ) {
					processFile(node.getCategory(), node, cm, toIncludeInZip, tempDir);
				}
				if (node.getCmAttributes() != null) {
					toProcess.addAll(node.getCmAttributes());
				}
			}else if (objectNode instanceof CmAttribute) {
				CmAttribute node = (CmAttribute)objectNode;
				
				if (!node.hasCustomImage() && node.getAttribute() != null && node.getAttribute().getIcon() != null ) {
					processFile(node.getAttribute(), node, cm, toIncludeInZip, tempDir);
				}
				
				
				if (node.getCurrentList() != null) {
					toProcess.addAll(node.getCurrentList());
				}
				if (node.getCurrentTree() != null) {
					toProcess.addAll(node.getCurrentTree());
				}
			}else if (objectNode instanceof CmAttributeListItem) {
				CmAttributeListItem node = (CmAttributeListItem)objectNode;
				
				if (!node.hasCustomImage() && node.getListItem() != null && node.getListItem().getIcon() != null ) {
					processFile(node.getListItem(), node, cm, toIncludeInZip, tempDir);
				}
			}else if (objectNode instanceof CmAttributeTreeNode) {
				CmAttributeTreeNode node = (CmAttributeTreeNode)objectNode;
				
				if (!node.hasCustomImage() && node.getDmTreeNode() != null && node.getDmTreeNode().getIcon() != null ) {
					processFile(node.getDmTreeNode(), node, cm, toIncludeInZip, tempDir);
				}
				
				
				if (node.getChildren() != null) {
					toProcess.addAll(node.getChildren());
				}
			}
		}
	}
	
	private void writeProjectFile(String name, ConfigurableModel cm, String version, Path logoFile, Path outputFile, Path metadataFile, HashMap<String, Object> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(name, version, CM_MODEL_FILE, logoFile, outputFile, metadataFile, projectAdditions);
	}

	private void profileToJson(CyberTrackerPropertiesProfile profile, boolean distanceDirection, Session session, IEclipseContext context, Path outputFile, HashMap<String, Object> additions ) throws IOException {
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(CtJsonExportUtils.toJson(profile, distanceDirection, additions, context, session));
		}
	}

	private HashMap<String,String> getTranslations(String defaultLabel, String key, ConservationArea ca){
		HashMap<String,String> translations = new HashMap<>();
		
		//english
		String enl = ResourceBundle.getBundle(Messages.BUNDLE_NAME, Locale.ROOT).getString(key);
		translations.put("en", enl); //$NON-NLS-1$
		
		Locale locale = Locale.getDefault();
		if (!locale.getLanguage().equalsIgnoreCase("en")) { //$NON-NLS-1$
			if(!defaultLabel.equalsIgnoreCase(enl))
				translations.put(locale.getLanguage(), defaultLabel);	
		}
		
		for (Language l : ca.getLanguages()) {
			locale = new Locale(l.getCode());
			locale = new Locale(locale.getLanguage());
			
			if (locale.getLanguage().equalsIgnoreCase("en")) continue; //$NON-NLS-1$
			if (locale.getLanguage().equalsIgnoreCase(Locale.getDefault().getLanguage())) continue;
			
			try {
				ResourceBundle b = ResourceBundle.getBundle(Messages.BUNDLE_NAME, locale);
				if (b != null) {
					String value = b.getString(key);
					if (value.equalsIgnoreCase(defaultLabel) || value.equalsIgnoreCase(enl)) continue;
					translations.put(locale.getLanguage(), value);	
				}
			}catch (Exception ex) {
			}
		}
		
		return translations;
	}
	
	@SuppressWarnings("unchecked")
	private void metadataToJson(AbstractCtPackage ctpackage, Session session, Path outputFile) throws IOException {
		
		List<MetadataFieldValue> metadataFields = ((PatrolCtPackage)ctpackage).getMetadataValues();
		if (metadataFields == null) metadataFields = new ArrayList<>();

		Map<String, MetadataFieldValue> map = new HashMap<>();
		for (MetadataFieldValue v : metadataFields) {
			map.put(v.getMetadataKey(),v);
		}
	
		JSONArray metadataScreens = new JSONArray();
		
		JSONObject transportScreen = CtJsonExportUtils.convertKeyOptions(map.get(PatrolMetadataField.TRANSPORT.name()), 
				PatrolTransportType.class, PatrolMetadataField.TRANSPORT.getJsonKey(), 
				Messages.PatrolPackageExporter_TransportTypePageLabel,
				getTranslations(Messages.PatrolPackageExporter_TransportTypePageLabel, "PatrolPackageExporter_TransportTypePageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
				PatrolMetadataField.TRANSPORT.isRequired(), 
				false, session, ctpackage.getConservationArea());
		JSONObject joo = (JSONObject) transportScreen.get(PatrolMetadataField.TRANSPORT.getJsonKey());
		JSONArray it = (JSONArray) joo.get(CtJsonExportUtils.JSON_OPTION_PROP_KEY);
		if (it.size() == 0) throw new IOException(Messages.PatrolPackageExporter_NoPatrolTypes);
		metadataScreens.add(transportScreen);
		
		metadataScreens.add(convertArmed(map.get(PatrolMetadataField.ARMED.name()), session, ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertKeyOptions(map.get(PatrolMetadataField.TEAM.name()), 
				Team.class, PatrolMetadataField.TEAM.getJsonKey(), 
				Messages.PatrolPackageExporter_TeamPageLabel, 
				getTranslations(Messages.PatrolPackageExporter_TeamPageLabel, "PatrolPackageExporter_TeamPageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
				PatrolMetadataField.TEAM.isRequired(), 
				PatrolMetadataField.TEAM.isFixed(), session, ctpackage.getConservationArea()));
		
		metadataScreens.add(convertStations(map.get(PatrolMetadataField.STATION.name()), session, ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertKeyOptions(map.get(PatrolMetadataField.MANDATE.name()), 
				PatrolMandate.class, PatrolMetadataField.MANDATE.getJsonKey(), 
				Messages.PatrolPackageExporter_MandatePageLabel,
				getTranslations(Messages.PatrolPackageExporter_MandatePageLabel, "PatrolPackageExporter_MandatePageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
				PatrolMetadataField.MANDATE.isRequired(), 
				PatrolMetadataField.MANDATE.isFixed(), session, ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertStringOp(map.get(PatrolMetadataField.OBJECTIVE.name()), 
				PatrolMetadataField.OBJECTIVE.getJsonKey(), 
				Messages.PatrolPackageExporter_ObjectivePageLabel,
				getTranslations(Messages.PatrolPackageExporter_ObjectivePageLabel, "PatrolPackageExporter_ObjectivePageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
				PatrolMetadataField.OBJECTIVE.isRequired(), PatrolMetadataField.OBJECTIVE.isFixed(), session, 
				ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertStringOp(map.get(PatrolMetadataField.COMMENT.name()), 
				PatrolMetadataField.COMMENT.getJsonKey(),
				Messages.PatrolPackageExporter_CommentPageLabel,
				getTranslations(Messages.PatrolPackageExporter_CommentPageLabel, "PatrolPackageExporter_CommentPageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
				PatrolMetadataField.COMMENT.isRequired(), PatrolMetadataField.COMMENT.isFixed(), session, 
				ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertEmployees(map.get(PatrolMetadataField.MEMBERS.name()),
				PatrolMetadataField.MEMBERS.isRequired(), PatrolMetadataField.MEMBERS.isFixed(), 
				session, ctpackage.getConservationArea()));
		
		//transport types that require pilot
		List<PatrolTransportType> requiredBy = new ArrayList<>();
		for (PatrolTransportType tt : QueryFactory.buildQuery(session, PatrolTransportType.class, 
				new Object[] {"conservationArea", ctpackage.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"isActive", true}).list()) { //$NON-NLS-1$
			if (tt.getPatrolType().requiresPilot()) requiredBy.add(tt);
		}
		
		JSONArray items = new JSONArray();
		requiredBy.forEach(e->items.add(UuidUtils.uuidToString(e.getUuid())));
		JSONObject jr = new JSONObject();
		jr.put(PatrolMetadataField.TRANSPORT.getJsonKey(), items);
		
		if (map.get(PatrolMetadataField.MEMBERS.name()) == null || map.get(PatrolMetadataField.MEMBERS.name()).isVisible()) {
			//force leader and pilot to be visible as well; ticket #2690
			metadataScreens.add(CtJsonExportUtils.convertLeaderPilot((MetadataFieldValue)null, PatrolMetadataField.LEADER.getJsonKey(),
					Messages.PatrolPackageExporter_LeaderPageLabel,
					getTranslations(Messages.PatrolPackageExporter_LeaderPageLabel, "PatrolPackageExporter_LeaderPageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
					PatrolMetadataField.LEADER.isRequired(), 
					PatrolMetadataField.LEADER.isFixed(), session, ctpackage.getConservationArea()));
			
			JSONObject oo = CtJsonExportUtils.convertLeaderPilot((MetadataFieldValue)null, PatrolMetadataField.PILOT.getJsonKey(), 
					Messages.PatrolPackageExporter_PilotPageLabel, 
					getTranslations(Messages.PatrolPackageExporter_PilotPageLabel, "PatrolPackageExporter_PilotPageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
					PatrolMetadataField.PILOT.isRequired(), 
					PatrolMetadataField.PILOT.isFixed(), session, ctpackage.getConservationArea());
			((JSONObject)oo.get(PatrolMetadataField.PILOT.getJsonKey())).put("required_by", jr); //$NON-NLS-1$
			metadataScreens.add(oo);
		}else {
			metadataScreens.add(CtJsonExportUtils.convertLeaderPilot(map.get(PatrolMetadataField.LEADER.name()),
					PatrolMetadataField.LEADER.getJsonKey(), 
					Messages.PatrolPackageExporter_LeaderPageLabel,
					getTranslations(Messages.PatrolPackageExporter_LeaderPageLabel, "PatrolPackageExporter_LeaderPageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
					PatrolMetadataField.LEADER.isRequired(), PatrolMetadataField.LEADER.isFixed(), session, 
					ctpackage.getConservationArea()));
			
			JSONObject oo = CtJsonExportUtils.convertLeaderPilot(map.get(PatrolMetadataField.PILOT.name()), 
					PatrolMetadataField.PILOT.getJsonKey(), 
					Messages.PatrolPackageExporter_PilotPageLabel,
					getTranslations(Messages.PatrolPackageExporter_PilotPageLabel, "PatrolPackageExporter_PilotPageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
					PatrolMetadataField.PILOT.isRequired(), PatrolMetadataField.PILOT.isFixed(), session,
					ctpackage.getConservationArea());
			((JSONObject)oo.get(PatrolMetadataField.PILOT.getJsonKey())).put("required_by", jr); //$NON-NLS-1$
			metadataScreens.add(oo);
		}
		metadataScreens.add(CtJsonExportUtils.createDataType(PatrolMetadataField.PATROL_RESOURCE_ID));
		metadataScreens.add(CtJsonExportUtils.createPatrolId());
		metadataScreens.add(CtJsonExportUtils.createStartDate());
		metadataScreens.add(CtJsonExportUtils.createStartTime());
		
		//custom attributes
		List<PatrolAttribute> attributes = QueryFactory.buildQuery(session, PatrolAttribute.class, 
				new Object[] {"conservationArea", ctpackage.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"isActive", true}).getResultList(); //$NON-NLS-1$
		
		for (PatrolAttribute a : attributes) {
			metadataScreens.add(covertPatrolAttribute(a, session));
		}
		
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(metadataScreens.toJSONString());
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject convertArmed(MetadataFieldValue armedOp, Session session, ConservationArea ca) {
		JSONObject isArmed = new JSONObject();
		isArmed.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.BOOLEAN.name());
		isArmed.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, Messages.PatrolPackageExporter_ArmedPageLabel);
		for (Entry<String,String> t : getTranslations(Messages.PatrolPackageExporter_ArmedPageLabel, "PatrolPackageExporter_ArmedPageLabel", ca).entrySet()) { //$NON-NLS-1$
			isArmed.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		
		isArmed.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY,  PatrolMetadataField.ARMED.isRequired());
		isArmed.put(CtJsonExportUtils.JSON_FIXED_PROP_KEY,  PatrolMetadataField.ARMED.isFixed());
		if (armedOp != null) {
			isArmed.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, armedOp.isVisible());
			if (!armedOp.isVisible() && armedOp.getBooleanValue() != null) {
				isArmed.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, armedOp.getBooleanValue());
			}
		}else {
			isArmed.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONObject isArmedOp = new JSONObject();
		isArmedOp.put(PatrolMetadataField.ARMED.getJsonKey(), isArmed);
		return isArmedOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject convertStations(MetadataFieldValue stationOp, Session session, ConservationArea ca) {
		JSONObject stationType = new JSONObject();
		stationType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.SINGLE_CHOICE.name());

		stationType.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, Messages.PatrolPackageExporter_StationFieldKey);
		for (Entry<String,String> t : getTranslations(Messages.PatrolPackageExporter_StationFieldKey, "PatrolPackageExporter_StationFieldKey", ca).entrySet()) { //$NON-NLS-1$
			stationType.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		
		stationType.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY,  PatrolMetadataField.STATION.isRequired());
		stationType.put(CtJsonExportUtils.JSON_FIXED_PROP_KEY,  PatrolMetadataField.STATION.isFixed());
		if (stationOp != null) {
			stationType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, stationOp.isVisible());
			if (!stationOp.isVisible()) {
				if (stationOp.getUuidValue() != null) {
					stationType.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, UuidUtils.uuidToString(stationOp.getUuidValue()));
				}else {
					stationType.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, null);
				}
			}
		}else {
			stationType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONArray teamOptions = new JSONArray();
		
		List<Station> stations = QueryFactory.buildQuery(session, Station.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"isActive", true}).list(); //$NON-NLS-1$
		for (Station t : stations) {
			JSONObject ttype = new JSONObject();
			ttype.put("uuid", UuidUtils.uuidToString(t.getUuid())); //$NON-NLS-1$
			ttype.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, t.findName(t.getConservationArea().getDefaultLanguage()));
			for (Label l : t.getNames()) {
				ttype.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
			}
			teamOptions.add(ttype);
		}
		stationType.put(CtJsonExportUtils.JSON_OPTION_PROP_KEY, teamOptions);
		
		JSONObject stationTypeOp = new JSONObject();
		stationTypeOp.put(PatrolMetadataField.STATION.getJsonKey(), stationType);
		return stationTypeOp;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject covertPatrolAttribute(PatrolAttribute pa, Session session) throws IOException {
		
		JSONObject optionType = new JSONObject();
		
		switch(pa.getType()) {
		case BOOLEAN:
			optionType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.BOOLEAN.name());
			break;
		case DATE:
			optionType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.DATE.name());
			break;
		case LIST:
			optionType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
			break;
		case NUMERIC:
			optionType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.NUMERIC.name());
			break;
		case TEXT:
			optionType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.TEXT.name());
			break;
		default:
			throw new IOException(MessageFormat.format(Messages.PatrolPackageExporter_attributeTypeNotSupported, pa.getType()));
		}
		
		optionType.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, pa.getName());
		for (Label l : pa.getNames()) {
			optionType.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
		}
		optionType.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, false);
		optionType.put(CtJsonExportUtils.JSON_FIXED_PROP_KEY, false);
		optionType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, true);
		
		if (pa.getType() == Attribute.AttributeType.LIST) {
			JSONArray optionOptions = new JSONArray();
			
			for(PatrolAttributeListItem item : pa.getAttributeList()) {
			
				JSONObject ttype = new JSONObject();
				ttype.put("uuid", UuidUtils.uuidToString(item.getUuid())); //$NON-NLS-1$
				ttype.put("key", item.getKeyId()); //$NON-NLS-1$
				ttype.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, item.findName(pa.getConservationArea().getDefaultLanguage())); 
				for (Label l : item.getNames()) {
					ttype.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
					
				}
				optionOptions.add(ttype);
			}
			optionType.put(CtJsonExportUtils.JSON_OPTION_PROP_KEY, optionOptions);
		}
		
		JSONObject attributeOp = new JSONObject();
		attributeOp.put(PatrolJsonUtils.ATTRIBUTE_PREFIX + UuidUtils.uuidToString(pa.getUuid()), optionType);
		return attributeOp;
	}
	
}
