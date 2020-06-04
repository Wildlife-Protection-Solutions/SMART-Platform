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
package org.wcs.smart.cybertracker.incident;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.IIncidentCtPackage;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.dataentry.model.xml.CmSmartToXmlConverter;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Package contribution for adding incident model to CT package
 * @author Emily
 *
 */
public class IncidentPackageContribution implements IPackageContribution{

	//package files
	public static final String INCIDENT_MODEL_FILE = "incident_model.xml"; //$NON-NLS-1$
	public static final String INCIDENT_METADATA_FILE = "incident_metadata.json"; //$NON-NLS-1$

	//incident resource key
	public static final String INCIDENT_RESOURCE_ID = "incident"; //$NON-NLS-1$

	@Override
	public IPackageUiContribution getUiController() {
		return new IncidentPackageUiContribution();
	}

	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {
		if (!(ctpackage instanceof IIncidentCtPackage)) return;
		IIncidentCtPackage pp = (IIncidentCtPackage)ctpackage;
		
		Label l = new Label(parent, SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setText(Messages.IncidentPackageContribution_ModelLabel);
		((GridData)l.getLayoutData()).verticalIndent = 5;
		
		l = new Label(parent, SWT.NONE);
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		if (pp.getHasIncident()) {
			if (pp.getIncidentModel() != null) {
				l.setText( pp.getIncidentModel().getName() );
			}else {
				l.setText( Messages.IncidentPackageContribution_OriginalDmLabel);
			}
		}else {
			l.setText(Messages.IncidentPackageContribution_NoneOption);
		}
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IEclipseContext context, IProgressMonitor monitor) throws IOException {
		if (!(ctpackage instanceof IIncidentCtPackage)) return null;
		IIncidentCtPackage pp = (IIncidentCtPackage)ctpackage;
		monitor.subTask(Messages.IncidentPackageContribution_TaskName);
		ConfigurableModel cm = null;
		if (pp.getHasIncident() && pp.getIncidentModel() == null) {
			try(Session session = HibernateManager.openSession()){
				DataModelWrapper wrapper = new DataModelWrapper();
				cm = wrapper.buildConfigurableModel(session, monitor);
				cm.setConservationArea(SmartDB.getCurrentConservationArea());
			}
		}else if (pp.getHasIncident()) {
			cm = (ConfigurableModel)pp.getIncidentModel();
		}else {
			return null;
		}
		
		Path tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		Path incidentFile = tempDir.resolve(INCIDENT_MODEL_FILE);
		
		PackageContribution updates = new PackageContribution() {
			@Override
			public void cleanUp() throws IOException{
				//delete tempDir and any subfiles
				Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						if (exc != null) throw exc;
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
				}});
			}
		};
	
		//convert to xml
		org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel = CmSmartToXmlConverter.convert(cm, true, monitor);
		//create and add help files
		for (File f :  CtJsonExportUtils.addHelpFiles(xmlModel, tempDir)  ) {
			updates.addFile(f.toPath());
		}

		//write file
		try(OutputStream out = Files.newOutputStream(incidentFile)){
			try {
				CmXmlManager.writeDataModel(xmlModel, out);
			} catch (JAXBException e) {
				throw new IOException(e);
			}
		}
		updates.addFile(incidentFile);
		
		try(Session s = HibernateManager.openSession()){
			ConfigurableModel tm = cm;
			if (tm.getUuid() != null) {
				tm = s.get(ConfigurableModel.class, cm.getUuid());
			}
			includeDmIcons(tm, updates, tempDir);	
		}
		
		
		Path metadataFile = tempDir.resolve(INCIDENT_METADATA_FILE);
		createIncidentJson(metadataFile);
		updates.addFile(metadataFile);
		
		
		JSONObject metadata = new JSONObject();
		metadata.put("decoder","sourceparser_smartconfigurabledatamodel"); //$NON-NLS-1$ //$NON-NLS-2$
		metadata.put("definition",incidentFile.getFileName().toString()); //$NON-NLS-1$
		metadata.put("metadata", metadataFile.getFileName().toString()); //$NON-NLS-1$
		updates.addProjectMetadata("incident", metadata); //$NON-NLS-1$
		return updates;
		
	}

	@SuppressWarnings("unchecked")
	private void createIncidentJson(Path incidentJson) throws IOException {
		JSONArray metadataScreens = new JSONArray();
		metadataScreens.add(CtJsonExportUtils.createDataType(INCIDENT_RESOURCE_ID));
		try(BufferedWriter fw = Files.newBufferedWriter(incidentJson)){
			fw.write(metadataScreens.toJSONString());
		}
	}
	
	private void includeDmIcons(ConfigurableModel cm, PackageContribution updates, Path tempDir) throws IOException {
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
					processFile(node.getCategory(), node, cm, updates, tempDir);
				}
				if (node.getCmAttributes() != null) {
					toProcess.addAll(node.getCmAttributes());
				}
			}else if (objectNode instanceof CmAttribute) {
				CmAttribute node = (CmAttribute)objectNode;
				
				if (!node.hasCustomImage() && node.getAttribute() != null && node.getAttribute().getIcon() != null ) {
					processFile(node.getAttribute(), node, cm, updates, tempDir);
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
					processFile(node.getListItem(), node, cm, updates, tempDir);
				}
			}else if (objectNode instanceof CmAttributeTreeNode) {
				CmAttributeTreeNode node = (CmAttributeTreeNode)objectNode;
				
				if (!node.hasCustomImage() && node.getDmTreeNode() != null && node.getDmTreeNode().getIcon() != null ) {
					processFile(node.getDmTreeNode(), node, cm, updates, tempDir);
				}
				
				
				if (node.getChildren() != null) {
					toProcess.addAll(node.getChildren());
				}
			}
		}
		
	}
	
	private void processFile(DmObject object, IImageAssociatedObject cmObject, ConfigurableModel cm, 
			PackageContribution updates, Path tempDir) throws IOException {
		
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
			updates.addFile(toPath);
		}
	}
	
}
