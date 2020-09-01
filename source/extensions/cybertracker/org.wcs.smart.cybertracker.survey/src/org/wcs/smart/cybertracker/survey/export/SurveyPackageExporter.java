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
package org.wcs.smart.cybertracker.survey.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils.Type;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.MissionMetadataField;
import org.wcs.smart.cybertracker.survey.model.SurveyCtPackage;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.dataentry.model.xml.CmSmartToXmlConverter;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.meta.MissionScreenOptionMeta;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Survey Cybertracker package exporter. This exports the following details into
 * a single zip file for use with Cybertracker. Included are: 
 * configurable model xml (cm_model.xml), 
 * configurable model image files, 
 * cybertracker configuration options (ct_profile.json)
 * the patrol metadata options (patrol_metadata.json).
 * the conservation area logo 
 * directory with map files to use in the ct application (selected by user on export)
 * 
 */
public enum SurveyPackageExporter {

	INSTANCE;
	
	public static final String CM_MODEL_FILE = "cm_model.xml"; //$NON-NLS-1$
	
	public static final String PATROL_METADATA_FILE = "survey_metadata.json"; //$NON-NLS-1$
	
	public static final String CT_PROFILE_FILE = "ct_profile.json"; //$NON-NLS-1$
	
	/**
	 * Exports survey data to package for cybertracker.
	 * 
	 * @param design
	 * @param profile
	 * @param exportFile
	 * @param monitor
	 * @throws Exception
	 */
	public void exportPackage(SurveyCtPackage ctpackage, Path exportFile, List<IPackageContribution.PackageContribution> contributions, IEclipseContext context, IProgressMonitor monitor) throws Exception{
		
		SubMonitor sub = SubMonitor.convert(monitor, Messages.SurveyPackageExporter_TaskName, 7);
		Path tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		try {
			try(Session session = HibernateManager.openSession()){
				ctpackage = session.get(SurveyCtPackage.class, ctpackage.getUuid());
				SurveyDesign sd = ctpackage.getSurveyDesign();
				
				ConfigurableModel modelToExport = null;
				if (sd.getConfigurableModel() != null) {
					modelToExport = sd.getConfigurableModel();
				}else {
					throw new Exception("Survey design requires a configurable model at this time.  Data models are not exported."); //$NON-NLS-1$
				}
				
				List<Path> toIncludeInZip = new ArrayList<>();
				
				//contribution files
				HashMap<String, Object> projectAdditions = new HashMap<>();
				HashMap<String, Object> ctprofileAdditions = new HashMap<>();

				for (IPackageContribution.PackageContribution update : contributions) {
					for (Path p : update.getAddedFiles()) {
						if (Files.isDirectory(p)) {
							Path dirPath = tempDir.resolve(p.getFileName().toString());
							Files.createDirectory(dirPath);
							Path mapfiles = CtJsonExportUtils.copyFiles(p, dirPath);
							if (mapfiles != null) toIncludeInZip.add(mapfiles);
							
						}else {
							Path moveTo = tempDir.resolve(p.getFileName().toString());
							Files.move(p, moveTo);
							toIncludeInZip.add(moveTo);
						}
					}
					if (update.getProjectMetadata() != null) {
						projectAdditions.putAll(update.getProjectMetadata());
					}
					if (update.getProfileMetadata() != null) {
						ctprofileAdditions.putAll(update.getProfileMetadata());
					}
				}
				
				
				//convert to xml
				Path cmFile = tempDir.resolve(CM_MODEL_FILE);
				org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel = CmSmartToXmlConverter.convert(modelToExport, sub.split(1));
				
				//create and add help files
				toIncludeInZip.addAll( CtJsonExportUtils.addHelpFiles(xmlModel, tempDir) );
				
				//export xml
				try(OutputStream out = Files.newOutputStream(cmFile)){
					CmXmlManager.writeDataModel(xmlModel, out);
				}
				toIncludeInZip.add(cmFile);
				
				//include configurable model image files
				sub.split(1);
				Path dataFolder = modelToExport.getFileDataStoreLocation();
				if (dataFolder != null && Files.exists(dataFolder) && Files.isDirectory(dataFolder)) {
					toIncludeInZip.addAll(Files.list(dataFolder).collect(Collectors.toList()));
				}
				
				//include data model image files that are part of configurable model node
				includeDmIcons(modelToExport, toIncludeInZip, tempDir, session);
				
				//include ca logo
				Path logo = sd.getConservationArea().getLogo();
				if (logo != null && Files.exists(logo)) {
					toIncludeInZip.add(logo);
				}
				
				sub.split(1);
				Path metadataFile = tempDir.resolve(PATROL_METADATA_FILE);
				metadataToJson(ctpackage, sd, session,  metadataFile);
				toIncludeInZip.add(metadataFile);
				
				sub.split(1);
				Path profileFile = tempDir.resolve(CT_PROFILE_FILE);
				
				profileToJson(ctpackage.getCtProfile(), sd.getTrackDistanceDirection(), session, context, profileFile, ctprofileAdditions);

				toIncludeInZip.add(profileFile);
								
				//get version number from output file
				String fname = exportFile.getFileName().toString();
				int start = fname.indexOf('.') + 1;
				int end = fname.lastIndexOf('.');
				String version = fname.substring(start,end);
				
				sub.split(1);
				Path projectFile = tempDir.resolve(CtJsonExportUtils.PROJECT_FILE);
				writeProjectFile( ctpackage.getName(), modelToExport, version, logo, projectFile, metadataFile, projectAdditions);
				toIncludeInZip.add(projectFile);
				
				ZipUtil.createZip(toIncludeInZip, exportFile, sub.split(1));
			}
		}finally {
			try {
				SmartUtils.deleteDirectory(tempDir);
			} catch (IOException e) {
				CyberTrackerPlugIn.log("Error deleting temp directory creating during ct survey package export.", e); //$NON-NLS-1$
			}
			for (IPackageContribution.PackageContribution update : contributions) {
				update.cleanUp();
			}
		}
	}
	

	private void processFile(DmObject object, IImageAssociatedObject cmObject, ConfigurableModel cm, 
			List<Path> toIncludeInZip, Path tempDir, Session session) throws IOException {
		IconFile file = object.getIcon().getIconFile(cm.getIconSet());
		if (file != null) {
			file.computeFileLocation(session);
			Path fromPath = file.getAttachmentFile();
			String fileName = cmObject.getImageFile() == null? cmObject.getDefaultImageFileName() : cmObject.getImageFile().getFileName().toString();			Path toPath = tempDir.resolve(SharedUtils.getFilenameWithoutExtension(fileName) + "." + SharedUtils.getFilenameExtension(fromPath.getFileName().toString())); //$NON-NLS-1$
			Files.copy(fromPath, toPath);
			if (!toIncludeInZip.contains(toPath)) toIncludeInZip.add(toPath);
		}
	}
	
	
	private void includeDmIcons(ConfigurableModel cm, List<Path> toIncludeInZip,
			Path tempDir, Session session) throws IOException {
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
					processFile(node.getCategory(), node, cm, toIncludeInZip, tempDir, session);
				}
				if (node.getCmAttributes() != null) {
					toProcess.addAll(node.getCmAttributes());
				}
			}else if (objectNode instanceof CmAttribute) {
				CmAttribute node = (CmAttribute)objectNode;
				
				if (!node.hasCustomImage() && node.getAttribute() != null && node.getAttribute().getIcon() != null ) {
					processFile(node.getAttribute(), node, cm, toIncludeInZip, tempDir, session);
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
					processFile(node.getListItem(), node, cm, toIncludeInZip, tempDir, session);
				}
			}else if (objectNode instanceof CmAttributeTreeNode) {
				CmAttributeTreeNode node = (CmAttributeTreeNode)objectNode;
				
				if (!node.hasCustomImage() && node.getDmTreeNode() != null && node.getDmTreeNode().getIcon() != null ) {
					processFile(node.getDmTreeNode(), node, cm, toIncludeInZip, tempDir, session);
				}
				
				
				if (node.getChildren() != null) {
					toProcess.addAll(node.getChildren());
				}
			}
		}
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
	private void metadataToJson(AbstractCtPackage ctpackage, SurveyDesign design, Session session, Path outputFile) throws IOException {
		
		JSONArray metadataScreens = new JSONArray();
		
		//add mission attribute options
		design = session.get(SurveyDesign.class,  design.getUuid());
		for (MissionProperty prop : design.getMissionProperties()) {
			MissionAttribute a = session.get(MissionAttribute.class, prop.getAttribute().getUuid());
			//only supports number, text and list
			if (prop.getAttribute().getType() == AttributeType.NUMERIC) {
				metadataScreens.add(covertNumberProperty(a, session, ctpackage.getConservationArea()));
			}else if (prop.getAttribute().getType() == AttributeType.TEXT) {
				metadataScreens.add(covertStringProperty(a, session, ctpackage.getConservationArea()));
			}else if (prop.getAttribute().getType() == AttributeType.LIST) {
				metadataScreens.add(covertListProperty(a, session, ctpackage.getConservationArea()));
			}	
		}
				
		List<MetadataFieldValue> metadataFields = ((SurveyCtPackage)ctpackage).getMetadataValues();

		Map<String, MetadataFieldValue> map = metadataFields.stream().collect(Collectors.toMap(e->e.getMetadataKey(), e->e));
	
		metadataScreens.add(CtJsonExportUtils.convertStringOp(map.get(MissionMetadataField.COMMENT.name()), 
				MissionMetadataField.COMMENT.getJsonKey(), 
				Messages.SurveyPackageExporter_CommentPageLabel,
				getTranslations(Messages.SurveyPackageExporter_CommentPageLabel, "SurveyPackageExporter_CommentPageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
				MissionScreenOptionMeta.COMMENT.isRequired(), false, session, ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertEmployees(map.get(MissionMetadataField.MEMBERS.name()), 
				MissionMetadataField.MEMBERS.isRequired(), false, session, ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertLeaderPilot(map.get(MissionMetadataField.LEADER.name()),
				MissionMetadataField.LEADER.getJsonKey(), 
				Messages.SurveyPackageExporter_LeaderPageLabel,
				getTranslations(Messages.SurveyPackageExporter_LeaderPageLabel, "SurveyPackageExporter_LeaderPageLabel", ctpackage.getConservationArea()), //$NON-NLS-1$
				MissionScreenOptionMeta.LEADER.isRequired(), false, session, ctpackage.getConservationArea())); 
		
		metadataScreens.add(CtJsonExportUtils.createDataType(SurveyScreensUtil.DATATYPE_SURVEY));
		metadataScreens.add(CtJsonExportUtils.createPatrolId());
		metadataScreens.add(CtJsonExportUtils.createStartDate());
		metadataScreens.add(CtJsonExportUtils.createStartTime());
		metadataScreens.add(createSurveyDesign(design));
				
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(metadataScreens.toJSONString());
		}
	}

	private void writeProjectFile(String name, ConfigurableModel cm, String version, Path logoFile, Path outputFile, Path metadataFile, HashMap<String, Object> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(name, version, CM_MODEL_FILE, logoFile, outputFile, metadataFile, projectAdditions);
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject covertNumberProperty(MissionAttribute prop, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.NUMERIC.name());
		objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, prop.getName());
		for (Label l : prop.getNames()) {
			objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
		}
		objective.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, false);
		objective.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, true);
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(SurveyScreensUtil.RESULT_MISSION_PROPETY_PREFIX + prop.getKeyId(), objective);
		return objectiveOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject covertStringProperty(MissionAttribute prop, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.TEXT.name());	
		objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, prop.getName());
		for (Label l : prop.getNames()) {
			objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
		}
		objective.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, false);
		objective.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, true);
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(SurveyScreensUtil.RESULT_MISSION_PROPETY_PREFIX + prop.getKeyId(), objective);
		return objectiveOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject covertListProperty(MissionAttribute prop, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
		objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, prop.getName());
		for (Label l : prop.getNames()) {
			objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
		}
		objective.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, false);
		objective.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, true);
		
		JSONArray optionOptions = new JSONArray();
		
		for (MissionAttributeListItem item : prop.getAttributeList()) {
			JSONObject ttype = new JSONObject();
			ttype.put("uuid", SurveyScreensUtil.JsonSurveyKey.MISSION_ATT_LIST.key + CyberTrackerConfExporter.KEY_SEP + UuidUtils.uuidToString(item.getUuid())); //$NON-NLS-1$
			ttype.put("label", item.getName()); //$NON-NLS-1$
			optionOptions.add(ttype);
		}
		objective.put(CtJsonExportUtils.JSON_OPTION_PROP_KEY, optionOptions);
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(SurveyScreensUtil.RESULT_MISSION_PROPETY_PREFIX + prop.getKeyId(), objective);
		return objectiveOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject createSurveyDesign(SurveyDesign design) {
		JSONObject dataType = new JSONObject();
		dataType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.TEXT.name());
		dataType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_OPTION_GENERATED_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, true);
		dataType.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, design.getKeyId());
		JSONObject typeOp = new JSONObject();
		typeOp.put(SurveyScreensUtil.RESULT_SURVEY_DESIGN, dataType);
		return typeOp;
	}
}
