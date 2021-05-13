/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.incident.pkg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.incident.IncidentPackageContribution;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.cybertracker.incident.model.IncidentCtPackage;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXml;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * SMART Collect Cybertracker package exporter.
 */
public enum IncidentPackageExporter {

	INSTANCE;
	
	private static final String CM_MODEL_FILE = "cm_model.xml"; //$NON-NLS-1$
	private static final String CT_PROFILE_FILE = "ct_profile.json"; //$NON-NLS-1$
	private static final String SMARTCOLLECT_METADATA_FILE = "incident_metadata.json"; //$NON-NLS-1$

	/**
	 * Exports patrol cybertracker package.
	 * 
	 * @param cm
	 * @param profile
	 * @param exportFile
	 * @param monitor
	 * @throws Exception
	 */
	public void exportPackage(IncidentCtPackage ctPackage, List<IPackageContribution.PackageContribution> updates, Path exportFile, IEclipseContext context, IProgressMonitor monitor) throws Exception{
		SubMonitor sub = SubMonitor.convert(monitor, Messages.IncidentPackageExporter_TaskName, 8);
		Path tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		try {
			try(Session session = HibernateManager.openSession()){

				//the ctpackage object is configured with the model to use
				ConfigurableModel modelToExport = ctPackage.getConfigurableModel();
				if (modelToExport.getUuid() != null) {
					modelToExport = session.get(ConfigurableModel.class, modelToExport.getUuid());
				}
				
				//reload package so we don't have hibernate issues
				IncidentCtPackage localpackage = session.get(IncidentCtPackage.class, ctPackage.getUuid());
				
				
				Set<Path> toIncludeInZip = new HashSet<>();
				HashMap<String, Object> projectAdditions = new HashMap<>();
				HashMap<String, Object> ctprofileAdditions = new HashMap<>();
				
				for (IPackageContribution.PackageContribution update : updates) {
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
				
				Path cmFile = tempDir.resolve(CM_MODEL_FILE);
				
				//convert to xml
				CmSmartToXml convert = new CmSmartToXml(session, true);
				convert.convert(modelToExport, monitor);
				org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel = convert.getXmlModel();
				

				//write xml
				try(OutputStream out = Files.newOutputStream(cmFile)){
					CmXmlManager.writeDataModel(xmlModel, out);
				}
				toIncludeInZip.add(cmFile);
				
				//include data model image files and update xmlModel
				sub.split(1);
				for (Entry<String,Path> icon : convert.getReferencedFiles().entrySet()) {
					Path toPath = tempDir.resolve(icon.getKey());
					Path fromPath = icon.getValue();
					if (!Files.exists(toPath)) Files.copy(fromPath, toPath);
					toIncludeInZip.add(toPath);
				}
				//create and add help files
				toIncludeInZip.addAll( CtJsonExportUtils.addHelpFiles(xmlModel, tempDir) );

				//include ca logo
				Path logo = modelToExport.getConservationArea().getLogo();
				if (logo != null && Files.exists(logo)) {
					toIncludeInZip.add(logo);
				}

				sub.split(1);
				Path profileFile = tempDir.resolve(CT_PROFILE_FILE);
				ObservationOptions ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
				profileToJson(session.get(CyberTrackerPropertiesProfile.class, localpackage.getCtProfile().getUuid()), 
						ops.getTrackDistanceDirection(), ops.getTrackObserver(),
						session, context, profileFile, ctprofileAdditions);
				
				toIncludeInZip.add(profileFile);
				
				
				//get version number from output file
				String version = null;
				if (localpackage.getUuid() != null) {
					String fname = exportFile.getFileName().toString();
					int start = fname.indexOf('.') + 1;
					int end = fname.lastIndexOf('.');
					version = fname.substring(start,end);
				}
				
				sub.split(1);
				
				//metadata
				Path metadataFile = tempDir.resolve(SMARTCOLLECT_METADATA_FILE);
				createMetadata(metadataFile);
				toIncludeInZip.add(metadataFile);
				
				//project file
				Path projectFile = tempDir.resolve(CtJsonExportUtils.PROJECT_FILE);
				writeProjectFile(localpackage.getName(), modelToExport, version, logo, projectFile, metadataFile, projectAdditions);
				toIncludeInZip.add(projectFile);
				
				ZipUtil.createZip(toIncludeInZip.toArray(new Path[toIncludeInZip.size()]), exportFile, sub.split(1));
			}
		}finally {
			try {
				SmartUtils.deleteDirectory(tempDir);
			} catch (IOException e) {
				CyberTrackerPlugIn.log("Error cleaning up directory after exporting ct patrol package", e); //$NON-NLS-1$
			}
			for (IPackageContribution.PackageContribution update : updates) {
				update.cleanUp();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createMetadata(Path incidentJson) throws IOException {
		JSONArray metadataScreens = new JSONArray();
		metadataScreens.add(CtJsonExportUtils.createDataType(IncidentPackageContribution.INCIDENT_RESOURCE_ID));
		
		
		JSONObject dataType = new JSONObject();
		dataType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.TEXT.name());
		dataType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_OPTION_GENERATED_KEY, true);
		dataType.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, true);
		
		try(BufferedWriter fw = Files.newBufferedWriter(incidentJson)){
			fw.write(metadataScreens.toJSONString());
		}
	}
	
	
	private void writeProjectFile(String name, ConfigurableModel cm, String version, Path logoFile, Path outputFile, Path metadataFile, HashMap<String, Object> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(name, version, CM_MODEL_FILE, logoFile, outputFile, metadataFile, projectAdditions);
	}

	private void profileToJson(CyberTrackerPropertiesProfile profile, boolean distanceDirection, boolean collectObserver, Session session, IEclipseContext context, Path outputFile, HashMap<String, Object> additions ) throws IOException {
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(CtJsonExportUtils.toJson(profile, distanceDirection, collectObserver, additions, context, session));
		}
	}

	

}
