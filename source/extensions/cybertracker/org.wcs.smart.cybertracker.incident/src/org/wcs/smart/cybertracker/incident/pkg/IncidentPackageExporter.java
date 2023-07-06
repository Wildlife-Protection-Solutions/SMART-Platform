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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.incident.IncidentPackageContribution;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.cybertracker.incident.model.IncidentCtPackage;
import org.wcs.smart.cybertracker.incident.model.IncidentMetadataField;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXml;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * SMART Collect Cybertracker package exporter.
 */
public class IncidentPackageExporter {
	
	private static final String CM_MODEL_FILE = "cm_model.xml"; //$NON-NLS-1$
	private static final String CT_PROFILE_FILE = "ct_profile.json"; //$NON-NLS-1$
	private static final String SMARTCOLLECT_METADATA_FILE = "incident_metadata.json"; //$NON-NLS-1$

	
	public static void exportPackage(IncidentCtPackage ctPackage, 
			List<IPackageContribution.PackageContribution> updates, Path exportFile, 
			IProgressMonitor monitor) throws Exception{
		
		IncidentPackageExporter exporter = new IncidentPackageExporter(ctPackage, updates, exportFile);
		exporter.exportPackageInternal(monitor);
		
	}
	
	public static void exportIncidentMetadata(AbstractCtPackage ctPackage, 
			Session session, Path exportFile, Path workingDir,
			IProgressMonitor monitor) throws IOException{
		
		IncidentPackageExporter exporter = new IncidentPackageExporter(ctPackage, Collections.emptyList(), exportFile);
		exporter.session = session;
		exporter.workingDir = workingDir;
		exporter.createIncidentMetadataJson(exportFile);
		
	}
	
	private AbstractCtPackage ctpackage; 
	private List<IPackageContribution.PackageContribution> contribs; 
	private Path exportFile;
	
	private Path workingDir;
	private Session session;
	
	private IncidentPackageExporter(AbstractCtPackage ctpackage, 
			List<IPackageContribution.PackageContribution> updates, 
			Path exportFile) {
		
		this.ctpackage = ctpackage;
		this.contribs = updates;
		this.exportFile = exportFile;
		
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
	
		SubMonitor sub = SubMonitor.convert(monitor, Messages.IncidentPackageExporter_TaskName, 8);
		workingDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		Set<Path> toIncludeInZip = new HashSet<>();
		
		try {
			try(Session session = HibernateManager.openSession()){
				this.session = session;
				//the ctpackage object is configured with the model to use
				ConfigurableModel modelToExport = ((IncidentCtPackage)ctpackage).getConfigurableModel();
				if (modelToExport.getUuid() != null) {
					modelToExport = session.get(ConfigurableModel.class, modelToExport.getUuid());
				}
				
				//reload package so we don't have hibernate issues
				ctpackage = session.get(IncidentCtPackage.class, ctpackage.getUuid());
				
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
				CtJsonExportUtils.addHelpFiles(modelToExport, xmlModel, workingDir);
				
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
				Path logo = modelToExport.getConservationArea().getLogo();
				if (logo != null && Files.exists(logo)) {
					toIncludeInZip.add(logo);
				}

				sub.split(1);
				Path profileFile = workingDir.resolve(CT_PROFILE_FILE);
				ObservationOptions ops = ObservationHibernateManager.getPatrolOptions(ctpackage.getConservationArea(),session);
				profileToJson(session.get(CyberTrackerPropertiesProfile.class, ctpackage.getCtProfile().getUuid()), 
						ops.getTrackDistanceDirection(), ops.getTrackObserver(),
						profileFile, ctprofileAdditions);
				
				//get version number from output file
				String version = null;
				if (ctpackage.getUuid() != null) {
					String fname = exportFile.getFileName().toString();
					int start = fname.indexOf('.') + 1;
					int end = fname.lastIndexOf('.');
					version = fname.substring(start,end);
				}
				
				sub.split(1);
				
				//metadata
				Path metadataFile = workingDir.resolve(SMARTCOLLECT_METADATA_FILE);
				createIncidentMetadataJson(metadataFile);
								
				//project file
				Path projectFile = workingDir.resolve(CtJsonExportUtils.PROJECT_FILE);
				writeProjectFile(ctpackage.getName(), modelToExport, version, logo, projectFile, metadataFile, projectAdditions);
				
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
				CyberTrackerPlugIn.log("Error cleaning up directory after exporting ct patrol package", e); //$NON-NLS-1$
			}
			for (IPackageContribution.PackageContribution update : contribs) {
				update.cleanUp();
			}
		}
	}
	
	@SuppressWarnings("unchecked")

	private void createIncidentMetadataJson(Path incidentJson) throws IOException {
		IconSet defaultSet = QueryFactory.buildQuery(session, 
				IconSet.class, new Object[] {"conservationArea", ctpackage.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"isDefault", true}).getSingleResult(); //$NON-NLS-1$
		
		JSONArray metadataScreens = new JSONArray();
		metadataScreens.add(CtJsonExportUtils.createDataType(IncidentPackageContribution.INCIDENT_RESOURCE_ID));
		
		//observer employee options
		MetadataFieldValue md = null;
		for (MetadataFieldValue v : ctpackage.getMetadataValues()) {
			if (v.getMetadataKey().equalsIgnoreCase(IncidentMetadataField.MEMBERS.name())) {
				md = v;
				break;
			}
		}
		if (md != null) {
			ObservationOptions ops = ObservationHibernateManager.getPatrolOptions(ctpackage.getConservationArea(), session);
			md.setVisible(ops.getTrackObserver());
			
			JSONObject emp = CtJsonExportUtils.convertEmployees(md, false,
					IncidentMetadataField.MEMBERS.getIcon(defaultSet),
					workingDir, session, ctpackage.getConservationArea());
			metadataScreens.add(emp);
		}
				
		try(BufferedWriter fw = Files.newBufferedWriter(incidentJson)){
			fw.write(metadataScreens.toJSONString());
		}
	}
	
	
	private void writeProjectFile(String name, ConfigurableModel cm, String version, Path logoFile, Path outputFile, Path metadataFile, HashMap<String, Object> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(name, version, CM_MODEL_FILE, logoFile, outputFile, metadataFile, projectAdditions);
	}

	private void profileToJson(CyberTrackerPropertiesProfile profile, 
			boolean distanceDirection, boolean collectObserver, 
			Path outputFile, HashMap<String, Object> additions ) throws IOException {
		
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(CtJsonExportUtils.toJson(profile, distanceDirection, collectObserver, 
					additions, session));
		}
	}

	

}
