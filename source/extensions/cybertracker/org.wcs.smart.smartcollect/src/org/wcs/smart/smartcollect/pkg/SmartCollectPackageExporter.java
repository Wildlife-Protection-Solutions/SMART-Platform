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
package org.wcs.smart.smartcollect.pkg;

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
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXml;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.model.SmartCollectPackage;
import org.wcs.smart.smartcollect.model.SmartCollectionMetadata;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * SMART Collect Cybertracker package exporter.
 */
public class SmartCollectPackageExporter {

	
	
	private static final String CM_MODEL_FILE = "cm_model.xml"; //$NON-NLS-1$
	private static final String CT_PROFILE_FILE = "ct_profile.json"; //$NON-NLS-1$
	private static final String SMARTCOLLECT_METADATA_FILE = "smartcollect_metadata.json"; //$NON-NLS-1$


	public static void exportPackage(SmartCollectPackage ctPackage, 
			List<IPackageContribution.PackageContribution> updates, Path exportFile, 
			IProgressMonitor monitor) throws Exception{
		SmartCollectPackageExporter exporter = new SmartCollectPackageExporter(ctPackage, updates, exportFile);
		exporter.exportPackageInternal(monitor);
	}
	
	private SmartCollectPackage ctPackage; 
	private List<IPackageContribution.PackageContribution> contribs; 
	private Path exportFile;
	
	
	private Session session;
	private Path workingDir;
	
	private SmartCollectPackageExporter (SmartCollectPackage ctPackage, 
			List<IPackageContribution.PackageContribution> updates, 
			Path exportFile) {
		
		this.ctPackage = ctPackage;
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
		SubMonitor sub = SubMonitor.convert(monitor, Messages.SmartCollectPackageExporter_TaskName, 8);
		workingDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		try {
			try(Session session = HibernateManager.openSession()){
				this.session = session;
				//the ctpackage object is configured with the model to use
				ConfigurableModel modelToExport = ctPackage.getConfigurableModel();
				if (modelToExport.getUuid() != null) {
					modelToExport = session.get(ConfigurableModel.class, modelToExport.getUuid());
				}
				
				//reload package so we don't have hiberante issues
				this.ctPackage = session.get(SmartCollectPackage.class, ctPackage.getUuid());
				
				
				Set<Path> toIncludeInZip = new HashSet<>();
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
				ObservationOptions ops = ObservationHibernateManager.getPatrolOptions(ctPackage.getConservationArea(), session);
				profileToJson(session.get(CyberTrackerPropertiesProfile.class, ctPackage.getCtProfile().getUuid()), ops.getTrackDistanceDirection(), profileFile, ctprofileAdditions);
				
				
				//get version number from output file
				String version = null;
				if (ctPackage.getUuid() != null) {
					String fname = exportFile.getFileName().toString();
					int start = fname.indexOf('.') + 1;
					int end = fname.lastIndexOf('.');
					version = fname.substring(start,end);
				}
				
				sub.split(1);
				
				//metadata
				Path metadataFile = workingDir.resolve(SMARTCOLLECT_METADATA_FILE);
				createMetadata(metadataFile);
				
				//project file
				Path projectFile = workingDir.resolve(CtJsonExportUtils.PROJECT_FILE);
				writeProjectFile(ctPackage.getName(), modelToExport, version, logo, projectFile, metadataFile, projectAdditions);
				
				
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
	private void createMetadata(Path incidentJson) throws IOException {
		JSONArray metadataScreens = new JSONArray();
		metadataScreens.add(CtJsonExportUtils.createDataType(SmartCollectPackage.PACKAGE_TYPENAME.toUpperCase()));
		
		
		JSONObject dataType = new JSONObject();
		dataType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.TEXT.name());
		dataType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_OPTION_GENERATED_KEY, true);
		dataType.put(CtJsonExportUtils.JSON_REQUIRED_PROP_KEY, true);
		JSONObject typeOp = new JSONObject();
		typeOp.put(SmartCollectionMetadata.USERNAMEMETADATA_KEY, dataType);
		metadataScreens.add(typeOp);
		
		try(BufferedWriter fw = Files.newBufferedWriter(incidentJson)){
			fw.write(metadataScreens.toJSONString());
		}
	}
	
	
	private void writeProjectFile(String name, ConfigurableModel cm, String version, Path logoFile, Path outputFile, Path metadataFile, HashMap<String, Object> projectAdditions) throws IOException {
		CtJsonExportUtils.writeProjectJson(name, version, CM_MODEL_FILE, logoFile, outputFile, metadataFile, projectAdditions);
	}

	private void profileToJson(CyberTrackerPropertiesProfile profile, boolean distanceDirection, 
			Path outputFile, HashMap<String, Object> additions ) throws IOException {
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(CtJsonExportUtils.toJson(profile, distanceDirection, false, additions, session));
		}
	}

	

}
