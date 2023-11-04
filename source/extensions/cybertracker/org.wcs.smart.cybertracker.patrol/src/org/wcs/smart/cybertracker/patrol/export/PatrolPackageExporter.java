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
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils.Type;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonUtils;
import org.wcs.smart.cybertracker.patrol.model.PatrolCtPackage;
import org.wcs.smart.cybertracker.patrol.model.PatrolMetadataField;
import org.wcs.smart.cybertracker.patrol.model.TransportTypeTrackTimerSetting;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXml;
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
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.util.SmartUtils;
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
public class PatrolPackageExporter {

	
	public static final String CM_MODEL_FILE = "cm_model.xml"; //$NON-NLS-1$
	
	public static final String PATROL_METADATA_FILE = "patrol_metadata.json"; //$NON-NLS-1$
	
	public static final String CT_PROFILE_FILE = "ct_profile.json"; //$NON-NLS-1$
	
	public static void exportPackage(PatrolCtPackage ctPackage, 
			List<IPackageContribution.PackageContribution> updates, 
			Path exportFile, IProgressMonitor monitor) throws Exception{

		PatrolPackageExporter exporter = new PatrolPackageExporter(ctPackage, updates, exportFile);
		exporter.exportPackageInternal(monitor);
	};
	
	
	private PatrolCtPackage ctpackage;
	private List<IPackageContribution.PackageContribution> contribs;
	private Path outputFile;
		
	private Path workingDir;
	private Session session;
	
	private PatrolPackageExporter(PatrolCtPackage ctPackage, 
			List<IPackageContribution.PackageContribution> updates, 
			Path exportFile) {
		
		this.ctpackage = ctPackage;
		this.contribs = updates;
		this.outputFile = exportFile;
		
	}
	/**
	 * Exports patrol cybertracker package.
	 * 
	 * @param cm
	 * @param profile
	 * @param exportFile
	 * @param monitor
	 * @throws Exception
	 */
	private void exportPackageInternal(IProgressMonitor monitor) throws Exception{

		//TODO: support cancelling
		SubMonitor sub = SubMonitor.convert(monitor, Messages.PatrolPackageExporter_TaskName, 8);
		workingDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		Set<Path> toIncludeInZip = new HashSet<>();
		
		try {
			try(Session session = HibernateManager.openSession()){
				this.session = session;
				
				//the ctpackage object is configured with the model to use
				ConfigurableModel modelToExport = ctpackage.getConfigurableModel();
				if (modelToExport.getUuid() != null) {
					modelToExport = session.get(ConfigurableModel.class, modelToExport.getUuid());
				}

				//reload package so we don't have hiberante issues
				this.ctpackage = session.get(PatrolCtPackage.class, ctpackage.getUuid());

				HashMap<String, Object> projectAdditions = new HashMap<>();
				HashMap<String, Object> ctprofileAdditions = new HashMap<>();
				
				for (IPackageContribution.PackageContribution update : contribs) {
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
//				toIncludeInZip.addAll( 
					CtJsonExportUtils.addHelpFiles(modelToExport, xmlModel, workingDir);
					//);

				//write xml
				Path cmFile = workingDir.resolve(CM_MODEL_FILE);
				try(OutputStream out = Files.newOutputStream(cmFile)){
					CmXmlManager.writeDataModel(xmlModel, out);
				}
//				toIncludeInZip.add(cmFile);
				
				//include data model image files and update xmlModel
				sub.split(1);
				for (Entry<String,Path> icon : convert.getReferencedFiles().entrySet()) {
					Path toPath = workingDir.resolve(icon.getKey());
					Path fromPath = icon.getValue();
					if (!Files.exists(toPath)) Files.copy(fromPath, toPath);
//					toIncludeInZip.add(toPath);
				}

				//include ca logo
				Path logo = modelToExport.getConservationArea().getLogo();
				if (logo != null && Files.exists(logo)) {
					toIncludeInZip.add(logo);
				}
				
				sub.split(1);
				Path metadataFile = workingDir.resolve(PATROL_METADATA_FILE);
				metadataToJson(metadataFile);
//				toIncludeInZip.add(metadataFile);

				sub.split(1);
				Path profileFile = workingDir.resolve(CT_PROFILE_FILE);
				ObservationOptions ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
				profileToJson(session.get(CyberTrackerPropertiesProfile.class, ctpackage.getCtProfile().getUuid()),
						ops.getTrackDistanceDirection(), ops.getTrackObserver(),
						 profileFile, ctprofileAdditions);
//				toIncludeInZip.add(profileFile);
				
				
				//get version number from output file
				String version = null;
				if (ctpackage.getUuid() != null) {
					String fname = outputFile.getFileName().toString();
					int start = fname.indexOf('.') + 1;
					int end = fname.lastIndexOf('.');
					version = fname.substring(start,end);
				}
				
				sub.split(1);
				Path projectFile = workingDir.resolve(CtJsonExportUtils.PROJECT_FILE);
				writeProjectFile(ctpackage.getName(), modelToExport, version, logo, projectFile, metadataFile, projectAdditions);
				
				//add all files in working directory to package
				try(Stream<Path> files = Files.list(workingDir)){
					files.forEach(file->toIncludeInZip.add(file));
				}
				
				ZipUtil.createZip(toIncludeInZip, outputFile, sub.split(1));
			}
		}finally {
			try {
				SmartUtils.deleteDirectory(workingDir);
			} catch (IOException e) {
				CyberTrackerPlugIn.log("Error cleaning up directory after exporting ct patrol package", e); //$NON-NLS-1$
			}
			for (IPackageContribution.PackageContribution update : contribs) {
				update.cleanUp();
			}
		}
	}
	
	
	private void writeProjectFile(String name, ConfigurableModel cm, String version, Path logoFile, Path outputFile, Path metadataFile, HashMap<String, Object> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(name, version, CM_MODEL_FILE, logoFile, outputFile, metadataFile, projectAdditions);
	}

	private void profileToJson(CyberTrackerPropertiesProfile profile, boolean distanceDirection, boolean collectObserver, Path targetFile, HashMap<String, Object> additions ) throws IOException {
		try(BufferedWriter fw = Files.newBufferedWriter(targetFile)){
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
	private void metadataToJson(Path metadataFile) throws IOException {
		
		List<MetadataFieldValue> metadataFields = ((PatrolCtPackage)ctpackage).getMetadataValues();
		if (metadataFields == null) metadataFields = new ArrayList<>();

		Map<String, MetadataFieldValue> map = new HashMap<>();
		for (MetadataFieldValue v : metadataFields) {
			map.put(v.getMetadataKey(),v);
		}
	
		String iconset = null;
		if (map.containsKey(PatrolMetadataField.PATROL_ICONSET_KEY)) {
			iconset = map.get(PatrolMetadataField.PATROL_ICONSET_KEY).getStringValue();
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
		
		JSONObject transportScreen = CtJsonExportUtils.convertKeyOptions(map.get(PatrolMetadataField.TRANSPORT.name()), 
				PatrolTransportType.class, PatrolMetadataField.TRANSPORT.getJsonKey(), 
				Messages.PatrolPackageExporter_TransportTypePageLabel,
				getTranslations(Messages.PatrolPackageExporter_TransportTypePageLabel, "PatrolPackageExporter_TransportTypePageLabel"), //$NON-NLS-1$
				PatrolMetadataField.TRANSPORT.getIcon(set),
				false, session, ctpackage.getConservationArea(),
				set, workingDir,
				(item, json)->{
					PatrolType ptype = session.createQuery("FROM PatrolType WHERE id.type = :type and id.conservationArea = :ca", PatrolType.class) //$NON-NLS-1$
							.setParameter("type",  ((PatrolTransportType)item).getPatrolType()) //$NON-NLS-1$
							.setParameter("ca", ((PatrolTransportType)item).getConservationArea()) //$NON-NLS-1$
							.uniqueResult();
					
					int max = ((PatrolTransportType)item).getPatrolType().getDefaultMaxSpeed();
					if (ptype != null) {
						max = ptype.getMaxSpeed();
					}
					json.put("max_speed", max); //$NON-NLS-1$
				}
			);
		JSONObject joo = (JSONObject) transportScreen.get(PatrolMetadataField.TRANSPORT.getJsonKey());
		JSONArray it = (JSONArray) joo.get(CtJsonExportUtils.JSON_OPTION_PROP_KEY);
		if (it.size() == 0) throw new IOException(Messages.PatrolPackageExporter_NoPatrolTypes);
		
		if (map.containsKey(TransportTypeTrackTimerSetting.METADATA_KEY)) {
			List<TransportTypeTrackTimerSetting> tts = TransportTypeTrackTimerSetting.fromString(map.get(TransportTypeTrackTimerSetting.METADATA_KEY).getStringValue(), ctpackage.getConservationArea(), session);
			for (int i = 0; i < it.size(); i ++) {
				JSONObject o = (JSONObject)it.get(i);
				String tkey = o.get(CtJsonExportUtils.JSON_PROP_KEY).toString();
				if (tkey == null) continue;
				
				for (TransportTypeTrackTimerSetting s : tts) {
					if (s.getTransportType().getKeyId().equalsIgnoreCase(tkey)) {
						JSONObject ctsettings = new JSONObject();
						o.put("ct_settings", ctsettings); //$NON-NLS-1$
						
						ctsettings.put(CyberTrackerPropertiesProfileOption.ProfileOptionID.WAYPOINT_TIMER_TYPE.name().toLowerCase(), s.getTrackTimerOption().name());
						ctsettings.put(CyberTrackerPropertiesProfileOption.ProfileOptionID.WAYPOINT_TIMER.name().toLowerCase(), s.getValue());
						break;
					}
				}
			}
		}
		
		
		metadataScreens.add(transportScreen);
		
		metadataScreens.add(convertArmed(map.get(PatrolMetadataField.ARMED.name()),
				PatrolMetadataField.ARMED.getIcon(set)));
		
		metadataScreens.add(CtJsonExportUtils.convertKeyOptions(map.get(PatrolMetadataField.TEAM.name()), 
				Team.class, PatrolMetadataField.TEAM.getJsonKey(), 
				Messages.PatrolPackageExporter_TeamPageLabel, 
				getTranslations(Messages.PatrolPackageExporter_TeamPageLabel, "PatrolPackageExporter_TeamPageLabel"), //$NON-NLS-1$
				PatrolMetadataField.TEAM.getIcon(set),
				PatrolMetadataField.TEAM.isFixed(), session, ctpackage.getConservationArea(), set, workingDir));
		
		metadataScreens.add(convertStations(map.get(PatrolMetadataField.STATION.name()), PatrolMetadataField.STATION.getIcon(set), set));
		
		JSONObject mandateScreen = CtJsonExportUtils.convertKeyOptions(map.get(PatrolMetadataField.MANDATE.name()), 
				PatrolMandate.class, PatrolMetadataField.MANDATE.getJsonKey(), 
				Messages.PatrolPackageExporter_MandatePageLabel,
				getTranslations(Messages.PatrolPackageExporter_MandatePageLabel, "PatrolPackageExporter_MandatePageLabel"), //$NON-NLS-1$
				PatrolMetadataField.MANDATE.getIcon(set),
				PatrolMetadataField.MANDATE.isFixed(), session, ctpackage.getConservationArea(), set, workingDir);
		metadataScreens.add(mandateScreen);
		JSONObject mds = (JSONObject) mandateScreen.get(PatrolMetadataField.MANDATE.getJsonKey());
		JSONArray mit = (JSONArray) mds.get(CtJsonExportUtils.JSON_OPTION_PROP_KEY);
		if (mit.size() == 0) throw new IOException(Messages.PatrolPackageExporter_MandatesRequired);
		
		metadataScreens.add(CtJsonExportUtils.convertStringOp(map.get(PatrolMetadataField.OBJECTIVE.name()), 
				PatrolMetadataField.OBJECTIVE.getJsonKey(), 
				Messages.PatrolPackageExporter_ObjectivePageLabel,
				getTranslations(Messages.PatrolPackageExporter_ObjectivePageLabel, "PatrolPackageExporter_ObjectivePageLabel"), //$NON-NLS-1$
				PatrolMetadataField.OBJECTIVE.isFixed(),
				PatrolMetadataField.OBJECTIVE.getIcon(set), workingDir, session, 
				ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertStringOp(map.get(PatrolMetadataField.COMMENT.name()), 
				PatrolMetadataField.COMMENT.getJsonKey(),
				Messages.PatrolPackageExporter_CommentPageLabel,
				getTranslations(Messages.PatrolPackageExporter_CommentPageLabel, "PatrolPackageExporter_CommentPageLabel"), //$NON-NLS-1$
				PatrolMetadataField.COMMENT.isFixed(), 
				PatrolMetadataField.COMMENT.getIcon(set), workingDir, session, 
				ctpackage.getConservationArea()));
		
		metadataScreens.add(CtJsonExportUtils.convertEmployees(map.get(PatrolMetadataField.MEMBERS.name()),
				PatrolMetadataField.MEMBERS.isFixed(),
				PatrolMetadataField.MEMBERS.getIcon(set), workingDir, 
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
					getTranslations(Messages.PatrolPackageExporter_LeaderPageLabel, "PatrolPackageExporter_LeaderPageLabel"), //$NON-NLS-1$
					PatrolMetadataField.LEADER.isFixed(),
					PatrolMetadataField.LEADER.getIcon(set), workingDir, 
					session, ctpackage.getConservationArea()));
			
			JSONObject oo = CtJsonExportUtils.convertLeaderPilot((MetadataFieldValue)null, PatrolMetadataField.PILOT.getJsonKey(), 
					Messages.PatrolPackageExporter_PilotPageLabel, 
					getTranslations(Messages.PatrolPackageExporter_PilotPageLabel, "PatrolPackageExporter_PilotPageLabel"), //$NON-NLS-1$
					PatrolMetadataField.PILOT.isFixed(),
					PatrolMetadataField.PILOT.getIcon(set), workingDir, 
					session, ctpackage.getConservationArea());
			((JSONObject)oo.get(PatrolMetadataField.PILOT.getJsonKey())).put("required_by", jr); //$NON-NLS-1$
			metadataScreens.add(oo);
		}else {
			metadataScreens.add(CtJsonExportUtils.convertLeaderPilot(map.get(PatrolMetadataField.LEADER.name()),
					PatrolMetadataField.LEADER.getJsonKey(), 
					Messages.PatrolPackageExporter_LeaderPageLabel,
					getTranslations(Messages.PatrolPackageExporter_LeaderPageLabel, "PatrolPackageExporter_LeaderPageLabel"), //$NON-NLS-1$
					PatrolMetadataField.LEADER.isFixed(), 
					PatrolMetadataField.LEADER.getIcon(set), workingDir, session, 
					ctpackage.getConservationArea()));
			
			JSONObject oo = CtJsonExportUtils.convertLeaderPilot(map.get(PatrolMetadataField.PILOT.name()), 
					PatrolMetadataField.PILOT.getJsonKey(), 
					Messages.PatrolPackageExporter_PilotPageLabel,
					getTranslations(Messages.PatrolPackageExporter_PilotPageLabel, "PatrolPackageExporter_PilotPageLabel"), //$NON-NLS-1$
					PatrolMetadataField.PILOT.isFixed(), 
					PatrolMetadataField.PILOT.getIcon(set), workingDir, session,
					ctpackage.getConservationArea());
			((JSONObject)oo.get(PatrolMetadataField.PILOT.getJsonKey())).put("required_by", jr); //$NON-NLS-1$
			metadataScreens.add(oo);
		}
		metadataScreens.add(CtJsonExportUtils.createDataType(PatrolMetadataField.PATROL_RESOURCE_ID));
		metadataScreens.add(CtJsonExportUtils.createPatrolId());
		metadataScreens.add(CtJsonExportUtils.createStartDate());
		metadataScreens.add(CtJsonExportUtils.createStartTime());
		
		
		JSONObject md = CtJsonExportUtils.createConfigurableModelUuid(PatrolMetadataField.CM_ID.getJsonKey(), ((PatrolCtPackage)ctpackage).getConfigurableModel() );
		if (md != null) metadataScreens.add(md);
		
		//custom attributes
		List<PatrolAttribute> attributes = QueryFactory.buildQuery(session, PatrolAttribute.class, 
				new Object[] {"conservationArea", ctpackage.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"isActive", true}).getResultList(); //$NON-NLS-1$
		
		for (PatrolAttribute a : attributes) {
			metadataScreens.add(covertPatrolAttribute(a, map.get(PatrolMetadataField.generateKey(a)), set));
		}
		
		try(BufferedWriter fw = Files.newBufferedWriter(metadataFile)){
			fw.write(metadataScreens.toJSONString());
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject convertArmed(MetadataFieldValue armedOp, URI metadataIcon) throws IOException {
		JSONObject isArmed = new JSONObject();
		isArmed.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.BOOLEAN.name());
		isArmed.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, Messages.PatrolPackageExporter_ArmedPageLabel);
		for (Entry<String,String> t : getTranslations(Messages.PatrolPackageExporter_ArmedPageLabel, "PatrolPackageExporter_ArmedPageLabel").entrySet()) { //$NON-NLS-1$
			isArmed.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		boolean isRequired = true;
		if (armedOp != null) isRequired = armedOp.isRequired();
		isArmed.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY,  isRequired);
		isArmed.put(CtJsonExportUtils.JSON_FIXED_PROP_KEY,  PatrolMetadataField.ARMED.isFixed());
		if (armedOp != null) {
			isArmed.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, armedOp.isVisible());
			if (!armedOp.isVisible() && armedOp.getBooleanValue() != null) {
				isArmed.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, armedOp.getBooleanValue());
			}
		}else {
			isArmed.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, true);
		}
		CtJsonExportUtils.addMetadataIconToJson(metadataIcon, workingDir, isArmed);
		
		JSONObject isArmedOp = new JSONObject();
		isArmedOp.put(PatrolMetadataField.ARMED.getJsonKey(), isArmed);
		return isArmedOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject convertStations(MetadataFieldValue stationOp, URI metadataIcon, IconSet set) throws IOException {
		JSONObject stationType = new JSONObject();
		stationType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.SINGLE_CHOICE.name());

		stationType.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, Messages.PatrolPackageExporter_StationFieldKey);
		for (Entry<String,String> t : getTranslations(Messages.PatrolPackageExporter_StationFieldKey, "PatrolPackageExporter_StationFieldKey").entrySet()) { //$NON-NLS-1$
			stationType.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		boolean isRequired = true;
		if (stationOp != null) isRequired = stationOp.isRequired();
		stationType.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY,  isRequired);
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
		CtJsonExportUtils.addMetadataIconToJson(metadataIcon, workingDir, stationType);
		
		JSONArray teamOptions = new JSONArray();
		
		List<Station> stations = QueryFactory.buildQuery(session, Station.class, 
				new Object[] {"conservationArea", ctpackage.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"isActive", true}).list(); //$NON-NLS-1$
		for (Station t : stations) {
			JSONObject ttype = new JSONObject();
			ttype.put("uuid", UuidUtils.uuidToString(t.getUuid())); //$NON-NLS-1$
			ttype.put(CtJsonExportUtils.JSON_OPTION_LABEL_DEFAULT_KEY, t.findName(t.getConservationArea().getDefaultLanguage()));
			for (Label l : t.getNames()) {
				ttype.put(CtJsonExportUtils.JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
			}
			CtJsonExportUtils.addIconToJson(t, set, ttype, workingDir, session);
			
			teamOptions.add(ttype);
		}
		stationType.put(CtJsonExportUtils.JSON_OPTION_PROP_KEY, teamOptions);
		
		JSONObject stationTypeOp = new JSONObject();
		stationTypeOp.put(PatrolMetadataField.STATION.getJsonKey(), stationType);
		return stationTypeOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject covertPatrolAttribute(PatrolAttribute pa, 
			MetadataFieldValue metadata, IconSet set) throws IOException {
		
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
		optionType.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, metadata == null ? Boolean.FALSE : metadata.isRequired());
		optionType.put(CtJsonExportUtils.JSON_FIXED_PROP_KEY, false);
		optionType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, metadata == null ? Boolean.TRUE : metadata.isVisible());
		
		if (metadata != null && !metadata.isVisible()) {
			if (metadata.getUuidValue() != null)  optionType.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, UuidUtils.uuidToString(metadata.getUuidValue()));
			if (metadata.getBooleanValue() != null)  optionType.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, metadata.getBooleanValue());
			if (metadata.getStringValue() != null) {
				String value = metadata.getStringValue();
				if (pa.getType() == Attribute.AttributeType.DATE) {
					value = DateTimeFormatter.ofPattern(CtJsonUtil.JSON_ATTRIBUTE_DATE_FORMAT_STR).format( LocalDate.parse(metadata.getStringValue()));			
				}
				optionType.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, value);
			}
		}
		if (pa.getIcon() != null) {
			IconFile file = pa.getIcon().getIconFile(set);
			if (file != null) {
				file.computeFileLocation(session);
				URI iconMetadata = pa.getIcon().getIconFile(set).getAttachmentFile().toUri();
				CtJsonExportUtils.addMetadataIconToJson(iconMetadata, workingDir, optionType);
			}
		}
		
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
				CtJsonExportUtils.addIconToJson(item, set, ttype, workingDir, session);
				optionOptions.add(ttype);
			}
			optionType.put(CtJsonExportUtils.JSON_OPTION_PROP_KEY, optionOptions);
		}
		
		JSONObject attributeOp = new JSONObject();
		attributeOp.put(PatrolJsonUtils.ATTRIBUTE_PREFIX + UuidUtils.uuidToString(pa.getUuid()), optionType);
		return attributeOp;
	}
	
}
