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
package org.wcs.smart.intelligence.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;


/**
 * Main class for managing the exporting of intelligence to 
 * PDF using the BIRT reporting interface.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ReportIntelligence {

	/**
	 * plan template file name
	 */
	public static final String INTELLIGENCE_TEMPLATE = "intelligenceTemplate.rptdesign"; //$NON-NLS-1$
		
	/**
	 * Plan template parameters
	 */
	public final static String UUID = "IntelligenceUUID"; //$NON-NLS-1$
	public final static String NAME = "IntelligenceName"; //$NON-NLS-1$
	public final static String DESCRIPTION = "IntelligenceDescription"; //$NON-NLS-1$
	public final static String SOURCE = "IntelligenceSource"; //$NON-NLS-1$
	public final static String PATROL_ID = "IntelligencePatrolID"; //$NON-NLS-1$
	public final static String RECEIVED_DATE = "IntelligenceReceivedDate"; //$NON-NLS-1$
	public final static String FROM_DATE = "IntelligenceFromDate"; //$NON-NLS-1$
	public final static String TO_DATE = "IntelligenceToDate"; //$NON-NLS-1$
	public final static String LOCATION = "IntelligenceLocation"; //$NON-NLS-1$
	public final static String CREATOR = "IntelligenceCreator"; //$NON-NLS-1$

	private static IEditorPart templateEditor = null;

	/**
	 * 
	 * @return the custom plan template file or null
	 * if file does not exist
	 */
	public static File getCustomTemplateLocation(){
		File f = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + File.separator + Intelligence.INTELLIGENCE_DIR, INTELLIGENCE_TEMPLATE);
		if (!f.exists()){
			return null;
		}
		return f;
	}

	public static InputStream getIntelligenceTemplate() throws Exception{
		File custom = getCustomTemplateLocation();
		if (custom != null) {
			return new FileInputStream(custom);
		}
		return IntelligencePlugIn.getDefault().getBundle().getResource("/org/wcs/smart/intelligence/report/intelligenceTemplate.rptdesign").openStream(); //$NON-NLS-1$
	}
	
	/**
	 * Export the plan to pdf
	 * @param planUuid
	 */
	public static void export(UUID planUuid){
		ExportIntelligenceJob job = new ExportIntelligenceJob(planUuid);
		job.schedule();
	}

	/**
	 * Edits the plan template
	 * @param event
	 */
	public static void editTemplate(){
		try{
			//copy the default template to the template location if 
			//it doesn't already exist
			if (getCustomTemplateLocation() == null){
				File f = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + File.separator + Intelligence.INTELLIGENCE_DIR, INTELLIGENCE_TEMPLATE);
				if (!f.getParentFile().exists()){
					Files.createDirectory(f.getParentFile().toPath());
				}
				try(InputStream in = getIntelligenceTemplate();
						OutputStream out = new FileOutputStream(f)){
					IOUtils.copy(in, out);
				}
			}
			
			PlatformUI.getWorkbench().showPerspective(IntelligenceReportPerspective.ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			ReportIntelligenceEditorInput input = new ReportIntelligenceEditorInput(getCustomTemplateLocation());
			templateEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, IReportEditorContants.DESIGN_EDITOR_ID);
		}catch (Exception ex){
			IntelligencePlugIn.displayLog(Messages.ReportIntelligence_Open_Template_Error + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return;
		}
	}

	/**
	 * Closes the current template editor.
	 * 
	 * @return true if editor is open and closed, false if not open
	 */
	public static boolean closeTemplateEditor(){
		if (templateEditor != null){
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(templateEditor, false);
			templateEditor = null;
			return true;
		}
		return false;
	}

}
