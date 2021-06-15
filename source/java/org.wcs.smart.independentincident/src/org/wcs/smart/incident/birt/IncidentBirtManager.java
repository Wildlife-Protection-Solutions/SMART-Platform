/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.birt.ui.IncidentBirtPerspective;
import org.wcs.smart.incident.birt.ui.IncidentBirtTemplateEditorInput;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Main class for managing the BIRT templates for exporting 
 * incidents to pdf.
 * 
 * @author Emily
 * @since 7.0.0
 */
public enum IncidentBirtManager {
	
	INSTANCE;

	/**
	 * plan template file name
	 */
	public static final String PLAN_TEMPLATE = "incidentTemplate.rptdesign"; //$NON-NLS-1$
	
	private IEditorPart templateEditor = null;
	
	
	/**
	 * Gets the location of plan files within the smart file store.
	 * If the directory does not exist it will create it.
	 * @return
	 */
	public Path getIncidentDirectory() {
		Path f =  Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
					.resolve(IndepedentIncidentSource.FILESTORE_LOC);
		if (!Files.exists(f)){
			try {
				Files.createDirectories(f);
			} catch (IOException e) {
				IncidentPlugIn.log(e.getMessage(), e);
			}
		}
		return f;
	}
	
	/**
	 * 
	 * @return the custom plan template file or null
	 * if file does not exist
	 */
	public Path getCustomIncidentTemplateLocation(){
		Path f = getIncidentDirectory().resolve(PLAN_TEMPLATE);
		if (!Files.exists(f)) return null;
		return f;
	}
	
	/**
	 * 
	 * @return the current plan template
	 * @throws Exception
	 */
	public InputStream getIncidentTemplate() throws Exception{
		Path custom = getCustomIncidentTemplateLocation();
		if (custom != null){
			return Files.newInputStream(custom);
		}
		return IncidentPlugIn.getDefault().getBundle().getResource("/org/wcs/smart/incident/birt/"+PLAN_TEMPLATE).openStream(); //$NON-NLS-1$
	}

	/**
	 * Imports the given file as the new plan template.
	 * 
	 * @param newTemplate
	 * @throws IOException
	 */
	public void importIncidentTemplate(Path newTemplate) throws IOException{
		Path f = getIncidentDirectory().resolve(PLAN_TEMPLATE);
		SmartUtils.copyFile(newTemplate, f, StandardCopyOption.REPLACE_EXISTING);
	}
	
	/**
	 * Export the incident to PDF
	 * @param incidentUuid
	 */
	public void exportToPdf(UUID incidentUuid){
		ExportIncidentJob job = new ExportIncidentJob(incidentUuid);
		job.schedule();
	}
	
	public void exportToPdf(Set<UUID> incidentUuids, Path output){
		ExportIncidentJob job = new ExportIncidentJob(incidentUuids, output);
		job.schedule();
	}
	
	/**
	 * Edits the plan template
	 * @param event
	 */
	public void editTemplate(){
		try{
			//copy the default template to the template location if 
			//it doesn't already exist
			if (getCustomIncidentTemplateLocation() == null){
				Path f = getIncidentDirectory().resolve(PLAN_TEMPLATE);
				try(InputStream in = getIncidentTemplate();
						OutputStream out = Files.newOutputStream(f)){
					IOUtils.copy(in, out);
				}
			}
			
			PlatformUI.getWorkbench().showPerspective(IncidentBirtPerspective.ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			IncidentBirtTemplateEditorInput input = new IncidentBirtTemplateEditorInput(getCustomIncidentTemplateLocation());
			templateEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, IReportEditorContants.DESIGN_EDITOR_ID);
		}catch (Exception ex){
			IncidentPlugIn.displayLog(Messages.IncidentBirtManager_editerror + "\n\n" + ex.getLocalizedMessage(), ex);  //$NON-NLS-1$
			return;
		}
	}
	
	/**
	 * Closes the current template editor.
	 * 
	 * @return true if editor is open and closed, false if not open
	 */
	public boolean closeTemplateEditor(){
		if (templateEditor != null){
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(templateEditor, false);
			templateEditor = null;
			return true;
		}
		return false;
	}
}
