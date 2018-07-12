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
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.dataentry.model.xml.CmSmartToXmlConverter;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.meta.MissionScreenOptionMeta;
import org.wcs.smart.hibernate.HibernateManager;
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
	public void exportPackage(SurveyDesign design, CyberTrackerPropertiesProfile profile, Path mapDirectory, Path exportFile, List<IPackageContribution.PackageContribution> contributions, IProgressMonitor monitor) throws Exception{
		
		SubMonitor sub = SubMonitor.convert(monitor, Messages.SurveyPackageExporter_TaskName, 7);
		Path tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		try {
			try(Session session = HibernateManager.openSession()){
				SurveyDesign sd = session.get(SurveyDesign.class, design.getUuid());
				
				ConfigurableModel modelToExport = null;
				if (sd.getConfigurableModel() != null) {
					modelToExport = sd.getConfigurableModel();
				}else {
					throw new Exception("Survey design requires a configurable model at this time.  Data models are not exported."); //$NON-NLS-1$
				}
				
				List<File> toIncludeInZip = new ArrayList<>();
				
				//contribution files
				HashMap<String, JSONObject> projectAdditions = new HashMap<>();
				for (IPackageContribution.PackageContribution update : contributions) {
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
				File dataFolder = new File(modelToExport.getFileDataStoreLocation());
				if (dataFolder != null && dataFolder.exists() && dataFolder.isDirectory()) {
					toIncludeInZip.addAll(Arrays.asList(dataFolder.listFiles()));
				}
				
				//include ca logo
				Path logo = design.getConservationArea().getLogo();
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
				writeProjectFile(modelToExport, logo, mapfiles, projectFile, metadataFile, projectAdditions);
				toIncludeInZip.add(projectFile.toFile());
				
				ZipUtil.createZip(toIncludeInZip.toArray(new File[toIncludeInZip.size()]), exportFile.toFile(), sub.split(1));
			}
		}finally {
			try {
				FileUtils.forceDelete(tempDir.toFile());
			} catch (IOException e) {
				CyberTrackerPlugIn.log("Error deleting temp directory creating during ct survey package export.", e); //$NON-NLS-1$
			}
			for (IPackageContribution.PackageContribution update : contributions) {
				update.cleanUp();
			}
		}
	}
	
	private void profileToJson(CyberTrackerPropertiesProfile profile, ConfigurableModel cm, Session session, Path outputFile) throws IOException {
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(CtJsonExportUtils.toJson(profile, cm, session));
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private void metadataToJson(ConservationArea ca, Session session, Path outputFile) throws IOException {
		Map<MissionScreenOptionMeta, ScreenOption> options = SurveyHibernateManager.getMissionScreenOptions(ca, session);
		
		JSONArray metadataScreens = new JSONArray();
		
		metadataScreens.add(CtJsonExportUtils.convertStringOp(options.get(MissionScreenOptionMeta.COMMENT), MissionScreenOptionMeta.COMMENT.key, Messages.SurveyPackageExporter_CommentPageLabel, MissionScreenOptionMeta.COMMENT.isRequired(), session, ca)); 
		metadataScreens.add(CtJsonExportUtils.convertEmployees(options.get(MissionScreenOptionMeta.MEMBERS), MissionScreenOptionMeta.MEMBERS.isRequired(), session, ca));
		metadataScreens.add(CtJsonExportUtils.convertLeaderPilot(options.get(MissionScreenOptionMeta.LEADER), MissionScreenOptionMeta.LEADER.key, Messages.SurveyPackageExporter_LeaderPageLabel, MissionScreenOptionMeta.LEADER.isRequired(), session, ca)); 
			
		metadataScreens.add(CtJsonExportUtils.createDataType(MissionScreenOptionMeta.MISSION_RESOURCE_ID));
		metadataScreens.add(CtJsonExportUtils.createPatrolId());
		metadataScreens.add(CtJsonExportUtils.createStartDate());
		metadataScreens.add(CtJsonExportUtils.createEndDate());
		
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(metadataScreens.toJSONString());
		}
	}

	private void writeProjectFile(ConfigurableModel cm, Path logoFile, Path mapFileDir, Path outputFile, Path metadataFile, HashMap<String, JSONObject> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(cm.getName(), CM_MODEL_FILE, logoFile, mapFileDir, outputFile, metadataFile, projectAdditions);
	}
	
}
