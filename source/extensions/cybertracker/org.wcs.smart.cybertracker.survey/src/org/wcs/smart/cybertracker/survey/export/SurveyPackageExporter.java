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
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.geotools.geojson.geom.GeometryJSON;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils.Type;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.MissionMetadataField;
import org.wcs.smart.cybertracker.survey.model.SurveyCtPackage;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXml;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
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
public class SurveyPackageExporter {

	private static final String CM_MODEL_FILE = "cm_model.xml"; //$NON-NLS-1$
	
	private static final String METADATA_FILE = "survey_metadata.json"; //$NON-NLS-1$
	
	private static final String CT_PROFILE_FILE = "ct_profile.json"; //$NON-NLS-1$
	
	
	public static void exportPackage(SurveyCtPackage ctpackage, Path exportFile, List<IPackageContribution.PackageContribution> contributions, IProgressMonitor monitor) throws Exception{
		SurveyPackageExporter exporter = new SurveyPackageExporter(ctpackage, exportFile, contributions);
		exporter.exportPackageInternal(monitor);
	}
	
	
	private SurveyCtPackage ctpackage;
	private Path exportFile;
	private List<IPackageContribution.PackageContribution> contributions;
	
	private Session session;
	private Path workingDir;
	private SurveyDesign surveyDesign;
	
	private SurveyPackageExporter(SurveyCtPackage ctpackage, Path exportFile, List<IPackageContribution.PackageContribution> contributions) {
		this.ctpackage = ctpackage;
		this.exportFile = exportFile;
		this.contributions = contributions;
		
	}
	/**
	 * Exports survey data to package for cybertracker.
	 * 
	 * @param design
	 * @param profile
	 * @param exportFile
	 * @param monitor
	 * @throws Exception
	 */
	private void exportPackageInternal(IProgressMonitor monitor) throws Exception{
		
		SubMonitor sub = SubMonitor.convert(monitor, Messages.SurveyPackageExporter_TaskName, 7);
		workingDir  = Files.createTempDirectory("smart"); //$NON-NLS-1$
		try {
			try(Session session = HibernateManager.openSession()){
				this.session = session;
				
				ctpackage = session.get(SurveyCtPackage.class, ctpackage.getUuid());
				surveyDesign = ctpackage.getSurveyDesign();
				
				ConfigurableModel modelToExport = null;
				if (surveyDesign.getConfigurableModel() != null) {
					modelToExport = surveyDesign.getConfigurableModel();
				}else {
					throw new Exception("Survey design requires a configurable model at this time.  Data models are not exported."); //$NON-NLS-1$
				}
				
				Set<Path> toIncludeInZip = new HashSet<>();
				
				//contribution files
				HashMap<String, Object> projectAdditions = new HashMap<>();
				HashMap<String, Object> ctprofileAdditions = new HashMap<>();

				for (IPackageContribution.PackageContribution update : contributions) {
					for (Path p : update.getAddedFiles()) {
						if (Files.isDirectory(p)) {
							Path dirPath = workingDir.resolve(p.getFileName().toString());
							Files.createDirectory(dirPath);
							CtJsonExportUtils.copyFiles(p, dirPath);
							
						}else {
							Path moveTo = workingDir.resolve(p.getFileName().toString());
							Files.move(p, moveTo);
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
				CmSmartToXml convert = new CmSmartToXml(session, true);
				convert.convert(modelToExport, monitor);
				org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel = convert.getXmlModel();
				
				//create and add help files
				//must be done before we write to xml as this changes xml
				sub.split(1);
				CtJsonExportUtils.addHelpFiles(modelToExport, xmlModel, workingDir) ;
				
				//write xml
				Path cmFile = workingDir.resolve(CM_MODEL_FILE);
				try(OutputStream out = Files.newOutputStream(cmFile)){
					CmXmlManager.writeDataModel(xmlModel, out);
				}
				
				//include data model image files and update xmlModel
				sub.split(1);
				for (Entry<String,Path> icon : convert.getReferencedFiles().entrySet()) {
					Path toPath = workingDir.resolve(icon.getKey());
					Path fromPath = icon.getValue();
					if (!Files.exists(toPath)) Files.copy(fromPath, toPath);
				}
				
				//include ca logo
				Path logo = surveyDesign.getConservationArea().getLogo();
				if (logo != null && Files.exists(logo)) {
					toIncludeInZip.add(logo);
				}
				
				sub.split(1);
				Path metadataFile = workingDir.resolve(METADATA_FILE);
				metadataToJson(metadataFile);
				
				
				sub.split(1);
				Path profileFile = workingDir.resolve(CT_PROFILE_FILE);
				
				profileToJson(ctpackage.getCtProfile(), surveyDesign.getTrackDistanceDirection(), 
						surveyDesign.getTrackObserver(), profileFile, ctprofileAdditions);
								
				//get version number from output file
				String fname = exportFile.getFileName().toString();
				int start = fname.indexOf('.') + 1;
				int end = fname.lastIndexOf('.');
				String version = fname.substring(start,end);
				
				sub.split(1);
				Path projectFile = workingDir.resolve(CtJsonExportUtils.PROJECT_FILE);
				writeProjectFile( ctpackage.getName(), modelToExport, version, logo, projectFile, metadataFile, projectAdditions);
				
				//add all files in working directory to package
				try(Stream<Path> files = Files.list(workingDir)){
					files.forEach(file->toIncludeInZip.add(file));
				}

				ZipUtil.createZip(toIncludeInZip, exportFile, sub.split(1));
			}
		}finally {
			try {
				SmartUtils.deleteDirectory(workingDir);
			} catch (IOException e) {
				CyberTrackerPlugIn.log("Error deleting temp directory creating during ct survey package export.", e); //$NON-NLS-1$
			}
			for (IPackageContribution.PackageContribution update : contributions) {
				update.cleanUp();
			}
		}
	}
	
	private void profileToJson(CyberTrackerPropertiesProfile profile, boolean distanceDirection, 
			boolean collectObserver, Path outputFile, HashMap<String, Object> additions ) throws IOException {
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(CtJsonExportUtils.toJson(profile, distanceDirection, collectObserver, additions, session));
		}
	}
	
	private HashMap<String,String> getTranslations(String defaultLabel, String key){
		HashMap<String,String> translations = new HashMap<>();
		
		//english
		String enl = ResourceBundle.getBundle(Messages.BUNDLE_NAME, Locale.ROOT).getString(key);
		translations.put("en", enl); //$NON-NLS-1$
		
		Locale locale = Locale.getDefault();
		if (!locale.getLanguage().equalsIgnoreCase("en")) { //$NON-NLS-1$
			if(!defaultLabel.equalsIgnoreCase(enl))
				translations.put(locale.getLanguage(), defaultLabel);	
		}
		
		for (Language l : ctpackage.getConservationArea().getLanguages()) {
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
	private void metadataToJson(Path outputFile) throws IOException {
		
		List<MetadataFieldValue> metadataFields = ((SurveyCtPackage)ctpackage).getMetadataValues();
		Map<String, MetadataFieldValue> map = metadataFields.stream().collect(Collectors.toMap(e->e.getMetadataKey(), e->e));
		
		String iconset = null;
		if (map.containsKey(MissionMetadataField.MISSION_ICONSET_KEY)) {
			iconset = map.get(MissionMetadataField.MISSION_ICONSET_KEY).getStringValue();
		}
		IconSet set = null;
		if (iconset != null) {
			List<IconSet> sets = QueryFactory.buildQuery(session, IconSet.class, 
					new Object[] {"conservationArea", ctpackage.getConservationArea()}, //$NON-NLS-1$
					new Object[] {"keyId", set}).list(); //$NON-NLS-1$
			if (sets.size() > 0) set = sets.get(0);
		}
		if (set == null) {
			//find default icon set
			List<IconSet> sets = QueryFactory.buildQuery(session, IconSet.class, 
					new Object[] {"conservationArea", ctpackage.getConservationArea()}, //$NON-NLS-1$
					new Object[] {"isDefault", true}).list(); //$NON-NLS-1$
			if (sets.size() > 0) set = sets.get(0);
		}
		
		
		JSONArray metadataScreens = new JSONArray();
		
		//add mission attribute options
//		design = session.get(SurveyDesign.class,  design.getUuid());
		for (MissionProperty prop : surveyDesign.getMissionProperties()) {
			MissionAttribute a = session.get(MissionAttribute.class, prop.getAttribute().getUuid());
			//only supports number, text and list
			if (prop.getAttribute().getType() == AttributeType.NUMERIC) {
				metadataScreens.add(covertNumberProperty(map.get(MissionMetadataField.generateKey(a)), a, set));
			}else if (prop.getAttribute().getType() == AttributeType.TEXT) {
				metadataScreens.add(covertStringProperty(map.get(MissionMetadataField.generateKey(a)), a, set));
			}else if (prop.getAttribute().getType() == AttributeType.LIST) {
				metadataScreens.add(covertListProperty(map.get(MissionMetadataField.generateKey(a)), a, set));
			}	
		}
		
		metadataScreens.add(CtJsonExportUtils.convertStringOp(map.get(MissionMetadataField.COMMENT.name()), 
				MissionMetadataField.COMMENT.getJsonKey(), 
				Messages.SurveyPackageExporter_CommentPageLabel,
				getTranslations(Messages.SurveyPackageExporter_CommentPageLabel, "SurveyPackageExporter_CommentPageLabel"), //$NON-NLS-1$
				false, MissionMetadataField.COMMENT.getIcon(set), workingDir, session, ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertEmployees(map.get(MissionMetadataField.MEMBERS.name()),
				false, MissionMetadataField.MEMBERS.getIcon(set), workingDir,
				session, ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertLeaderPilot(map.get(MissionMetadataField.LEADER.name()),
				MissionMetadataField.LEADER.getJsonKey(), 
				Messages.SurveyPackageExporter_LeaderPageLabel,
				getTranslations(Messages.SurveyPackageExporter_LeaderPageLabel, "SurveyPackageExporter_LeaderPageLabel"), //$NON-NLS-1$
				false, MissionMetadataField.MEMBERS.getIcon(set), workingDir,
				session, ctpackage.getConservationArea())); 
		
		
		//add sampling units
		JSONArray sus = new JSONArray();	
		List<SamplingUnit> units = QueryFactory.buildQuery(session, SamplingUnit.class, 
				new Object[] {"surveyDesign", surveyDesign}).list(); //$NON-NLS-1$
		
		GeometryJSON util = new GeometryJSON();
		
		for (SamplingUnit unit : units) {
			JSONObject su = new JSONObject();
			su.put("uuid", UuidUtils.uuidToString(unit.getUuid())); //$NON-NLS-1$
			su.put("id", unit.getId()); //$NON-NLS-1$
			sus.add(su);
			
			try {
				StringWriter ww = new StringWriter();
				util.write(unit.getGeometry(), ww);
				JSONParser p = new JSONParser();
				su.put("geometry", p.parse(ww.getBuffer().toString())); //$NON-NLS-1$
			}catch (Exception ex) {
				throw new IOException("Cannot convert geometry to json object: " + ex.getMessage()); //$NON-NLS-1$
			}
			
		}
		JSONObject su = new JSONObject();
		su.put(MissionMetadataField.SAMPING_UNIT.getJsonKey(), sus);
		metadataScreens.add(su);
		
		metadataScreens.add(CtJsonExportUtils.createDataType(SurveyScreensUtil.DATATYPE_SURVEY));
		metadataScreens.add(CtJsonExportUtils.createPatrolId());
		metadataScreens.add(CtJsonExportUtils.createStartDate());
		metadataScreens.add(CtJsonExportUtils.createStartTime());
		metadataScreens.add(createSurveyDesign(surveyDesign));
				
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(metadataScreens.toJSONString());
		}
	}

	private void writeProjectFile(String name, ConfigurableModel cm, String version, Path logoFile, Path outputFile, Path metadataFile, HashMap<String, Object> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(name, version, CM_MODEL_FILE, logoFile, outputFile, metadataFile, projectAdditions);
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject covertNumberProperty(MetadataFieldValue metadata, 
			MissionAttribute prop, IconSet set) throws IOException {
		
		JSONObject objective = new JSONObject();
		objective.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.NUMERIC.name());
		objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, prop.getName());
		for (Label l : prop.getNames()) {
			objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
		}
		boolean isRequired = false;
		boolean isVisible = true;
		if (metadata != null) {
			isRequired = metadata.isRequired();
			isVisible = metadata.isVisible();
			
			if (!isVisible) {
				objective.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, Double.parseDouble(metadata.getStringValue()));
			}
		}
		objective.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, isRequired);
		objective.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, isVisible);
		if (prop.getIcon() != null) {
			IconFile file = prop.getIcon().getIconFile(set);
			if (file != null) {
				file.computeFileLocation(session);
				URI iconMetadata = prop.getIcon().getIconFile(set).getAttachmentFile().toUri();
				CtJsonExportUtils.addMetadataIconToJson(iconMetadata, workingDir, objective);
			}
		}
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(SurveyScreensUtil.RESULT_MISSION_PROPETY_PREFIX + prop.getKeyId(), objective);
		return objectiveOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject covertStringProperty(MetadataFieldValue metadata, MissionAttribute prop, IconSet set) throws IOException {
		JSONObject objective = new JSONObject();
		objective.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.TEXT.name());	
		objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, prop.getName());
		for (Label l : prop.getNames()) {
			objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
		}
		boolean isRequired = false;
		boolean isVisible = true;
		if (metadata != null) {
			isRequired = metadata.isRequired();
			isVisible = metadata.isVisible();
			if (!isVisible) {
				objective.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, metadata.getStringValue());
			}
		}
		objective.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, isRequired);
		objective.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, isVisible);
		if (prop.getIcon() != null) {
			IconFile file = prop.getIcon().getIconFile(set);
			if (file != null) {
				file.computeFileLocation(session);
				URI iconMetadata = prop.getIcon().getIconFile(set).getAttachmentFile().toUri();
				CtJsonExportUtils.addMetadataIconToJson(iconMetadata, workingDir, objective);
			}
		}
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(SurveyScreensUtil.RESULT_MISSION_PROPETY_PREFIX + prop.getKeyId(), objective);
		return objectiveOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject covertListProperty(MetadataFieldValue metadata, MissionAttribute prop, IconSet set) throws IOException {
		JSONObject objective = new JSONObject();
		objective.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
		objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, prop.getName());
		for (Label l : prop.getNames()) {
			objective.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
		}
		boolean isRequired = false;
		boolean isVisible = true;
		if (metadata != null) {
			isRequired = metadata.isRequired();
			isVisible = metadata.isVisible();
			
			if (!isVisible) {
				objective.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, UuidUtils.uuidToString(metadata.getUuidValue()));
			}
		}
		objective.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, isRequired);
		objective.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, isVisible);
		
		JSONArray optionOptions = new JSONArray();
		
		for (MissionAttributeListItem item : prop.getAttributeList()) {
			JSONObject ttype = new JSONObject();
			ttype.put("uuid", SurveyScreensUtil.JsonSurveyKey.MISSION_ATT_LIST.key + CyberTrackerConfExporter.KEY_SEP + UuidUtils.uuidToString(item.getUuid())); //$NON-NLS-1$
			ttype.put("label", item.getName()); //$NON-NLS-1$
			optionOptions.add(ttype);
			
			CtJsonExportUtils.addIconToJson(item, set, ttype, workingDir, session);
		}
		objective.put(CtJsonExportUtils.JSON_OPTION_PROP_KEY, optionOptions);
		if (prop.getIcon() != null) {
			IconFile file = prop.getIcon().getIconFile(set);
			if (file != null) {
				file.computeFileLocation(session);
				URI iconMetadata = prop.getIcon().getIconFile(set).getAttachmentFile().toUri();
				CtJsonExportUtils.addMetadataIconToJson(iconMetadata, workingDir, objective);
			}
		}
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
