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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.dataentry.model.xml.CmSmartToXmlConverter;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.meta.PatrolScreenOptionMeta;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
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
	public void exportPackage(ConfigurableModel cm, CyberTrackerPropertiesProfile profile, Path mapDirectory, Path exportFile, List<IPackageContribution.PackageContribution> updates, IProgressMonitor monitor) throws Exception{
		
		SubMonitor sub = SubMonitor.convert(monitor, Messages.PatrolPackageExporter_TaskName, 7);
		
		Path tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		try {
			try(Session session = HibernateManager.openSession()){
				ConfigurableModel modelToExport = cm;
				if (cm.getUuid() != null) {
					modelToExport = session.get(ConfigurableModel.class, cm.getUuid());
				}
				
				List<File> toIncludeInZip = new ArrayList<>();
				HashMap<String, JSONObject> projectAdditions = new HashMap<>();
				for (IPackageContribution.PackageContribution update : updates) {
					for (Path p : update.getAddedFiles()) {
						Path moveTo = tempDir.resolve(p.getFileName().toString());
						Files.move(p, moveTo);
						toIncludeInZip.add(moveTo.toFile());
					}
					if (update.getProjectMetadataKey() != null) {
						projectAdditions.put(update.getProjectMetadataKey(), update.getProjectMetdata());
					}
				}
				
				Path cmFile = tempDir.resolve(CM_MODEL_FILE);
				org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel = CmSmartToXmlConverter.convert(modelToExport, sub.split(1));
				try(OutputStream out = Files.newOutputStream(cmFile)){
					CmXmlManager.writeDataModel(xmlModel, out);
				}
				toIncludeInZip.add(cmFile.toFile());
				
				//include configurable model image files
				sub.split(1);
				File dataFolder = new File(cm.getFileDataStoreLocation());
				if (dataFolder != null && dataFolder.exists() && dataFolder.isDirectory()) {
					toIncludeInZip.addAll(Arrays.asList(dataFolder.listFiles()));
				}
				
				//include ca logo
				Path logo = cm.getConservationArea().getLogo();
				if (logo != null && Files.exists(logo)) {
					toIncludeInZip.add(logo.toFile());
				}
				
				sub.split(1);
				Path metadataFile = tempDir.resolve(PATROL_METADATA_FILE);
				metadataToJson(modelToExport.getConservationArea(), session,  metadataFile);
				toIncludeInZip.add(metadataFile.toFile());
				
				sub.split(1);
				Path profileFile = tempDir.resolve(CT_PROFILE_FILE);
				profileToJson(session.get(CyberTrackerPropertiesProfile.class, profile.getUuid()), modelToExport, session, profileFile);
				toIncludeInZip.add(profileFile.toFile());
				
				sub.split(1);
				//copy map files
				Path mapfiles = null;
				if (mapDirectory != null) {
					mapfiles = CtJsonExportUtils.copyMapFiles(mapDirectory, tempDir);
					if (mapfiles != null) toIncludeInZip.add(mapfiles.toFile());
				}
				
				sub.split(1);
				Path projectFile = tempDir.resolve(CtJsonExportUtils.PROJECT_FILE);
				writeProjectFile(cm, logo, mapfiles, projectFile, metadataFile, projectAdditions);
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
	
	private void writeProjectFile(ConfigurableModel cm, Path logoFile, Path mapfiles, Path outputFile, Path metadataFile, HashMap<String, JSONObject> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(cm.getName(), CM_MODEL_FILE, logoFile, mapfiles, outputFile, metadataFile, projectAdditions);
	}
	
	private void profileToJson(CyberTrackerPropertiesProfile profile, ConfigurableModel cm, Session session, Path outputFile) throws IOException {
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(CtJsonExportUtils.toJson(profile, cm, session));
		}
	}
	

	
	
	@SuppressWarnings("unchecked")
	private void metadataToJson(ConservationArea ca, Session session, Path outputFile) throws IOException {
		Map<PatrolScreenOptionMeta, ScreenOption> options = PatrolHibernateManager.getScreenOptions(ca, session);
		
		JSONArray metadataScreens = new JSONArray();
		
		metadataScreens.add(CtJsonExportUtils.convertKeyOptions(options.get(PatrolScreenOptionMeta.TRANSPORT), PatrolTransportType.class, PatrolScreenOptionMeta.TRANSPORT.key, Messages.PatrolPackageExporter_TransportTypePageLabel, PatrolScreenOptionMeta.TRANSPORT.isRequired(), session, ca));
		metadataScreens.add(convertArmed(options.get(PatrolScreenOptionMeta.ARMED), session, ca));
		metadataScreens.add(CtJsonExportUtils.convertKeyOptions(options.get(PatrolScreenOptionMeta.TEAM), Team.class, PatrolScreenOptionMeta.TEAM.key, Messages.PatrolPackageExporter_TeamPageLabel, PatrolScreenOptionMeta.TEAM.isRequired(), session, ca));
		metadataScreens.add(convertStations(options.get(PatrolScreenOptionMeta.STATION), session, ca));
		metadataScreens.add(CtJsonExportUtils.convertKeyOptions(options.get(PatrolScreenOptionMeta.MANDATE), PatrolMandate.class, PatrolScreenOptionMeta.MANDATE.key, Messages.PatrolPackageExporter_MandatePageLabel,PatrolScreenOptionMeta.MANDATE.isRequired(),  session, ca));
		metadataScreens.add(CtJsonExportUtils.convertStringOp(options.get(PatrolScreenOptionMeta.OBJECTIVE), PatrolScreenOptionMeta.OBJECTIVE.key, Messages.PatrolPackageExporter_ObjectivePageLabel, PatrolScreenOptionMeta.OBJECTIVE.isRequired(), session, ca));
		metadataScreens.add(CtJsonExportUtils.convertStringOp(options.get(PatrolScreenOptionMeta.COMMENT), PatrolScreenOptionMeta.COMMENT.key, Messages.PatrolPackageExporter_CommentPageLabel, PatrolScreenOptionMeta.COMMENT.isRequired(), session, ca));
		metadataScreens.add(CtJsonExportUtils.convertEmployees(options.get(PatrolScreenOptionMeta.MEMBERS),PatrolScreenOptionMeta.MEMBERS.isRequired(), session, ca));
		metadataScreens.add(CtJsonExportUtils.convertLeaderPilot(options.get(PatrolScreenOptionMeta.LEADER), PatrolScreenOptionMeta.LEADER.key, Messages.PatrolPackageExporter_LeaderPageLabel, PatrolScreenOptionMeta.LEADER.isRequired(), session, ca));
		metadataScreens.add(CtJsonExportUtils.convertLeaderPilot(options.get(PatrolScreenOptionMeta.PILOT), PatrolScreenOptionMeta.PILOT.key, Messages.PatrolPackageExporter_PilotPageLabel, PatrolScreenOptionMeta.PILOT.isRequired(), session, ca));
		metadataScreens.add(CtJsonExportUtils.createDataType(PatrolScreenOptionMeta.PATROL_RESOURCE_ID));
		metadataScreens.add(CtJsonExportUtils.createPatrolId());
		metadataScreens.add(CtJsonExportUtils.createStartDate());
		metadataScreens.add(CtJsonExportUtils.createEndDate());
		
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(metadataScreens.toJSONString());
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject convertArmed(ScreenOption armedOp, Session session, ConservationArea ca) {
		JSONObject isArmed = new JSONObject();
		isArmed.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.BOOLEAN.name());
		isArmed.put(CtJsonExportUtils.JSON_OPTION_LABEL_KEY, Messages.PatrolPackageExporter_ArmedPageLabel);
		isArmed.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY,  PatrolScreenOptionMeta.ARMED.isRequired());
		if (armedOp != null) {
			isArmed.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, armedOp.isVisible());
			if (!armedOp.isVisible() && armedOp.getBooleanValue() != null) {
				isArmed.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, armedOp.getBooleanValue());
			}
		}else {
			isArmed.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONObject isArmedOp = new JSONObject();
		isArmedOp.put(PatrolScreenOptionMeta.ARMED.key, isArmed);
		return isArmedOp;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject convertStations(ScreenOption stationOp, Session session, ConservationArea ca) {
		JSONObject stationType = new JSONObject();
		stationType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.SINGLE_CHOICE.name());
		stationType.put(CtJsonExportUtils.JSON_OPTION_LABEL_KEY, Messages.PatrolPackageExporter_StationFieldKey);
		stationType.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY,  PatrolScreenOptionMeta.STATION.isRequired());
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
			ttype.put("label_default", t.findName(t.getConservationArea().getDefaultLanguage())); //$NON-NLS-1$
			for (Label l : t.getNames()) {
				ttype.put("label_" + l.getLanguage().getCode(), l.getValue()); //$NON-NLS-1$
				
			}
			teamOptions.add(ttype);
		}
		stationType.put(CtJsonExportUtils.JSON_OPTION_PROP_KEY, teamOptions);
		
		JSONObject stationTypeOp = new JSONObject();
		stationTypeOp.put(PatrolScreenOptionMeta.STATION.key, stationType);
		return stationTypeOp;
	}
	
	
}
