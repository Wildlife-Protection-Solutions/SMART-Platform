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
package org.wcs.smart.plan.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;

/**
 * Main class for managing the exporting of plans to 
 * pdf using the BIRT reporting interface.
 * 
 * @author Emily
 * @since 2.0.0
 */
public class ReportPlan {

	/**
	 * plan template file name
	 */
	public static final String PLAN_TEMPLATE = "planTemplate.rptdesign"; //$NON-NLS-1$
		
	/**
	 * Plan template parameters
	 */
	public final static String PLAN_UUID = "PlanUUID"; //$NON-NLS-1$
	public final static String PLAN_ID = "PlanID"; //$NON-NLS-1$
	public final static String PLAN_NAME = "PlanName"; //$NON-NLS-1$
	public final static String PLAN_DESCRIPTION = "PlanDescription"; //$NON-NLS-1$
	public final static String PLAN_TYPE = "PlanType"; //$NON-NLS-1$
	public final static String UNAVAILABLE_EMPLOYEES = "UnavailableEmployees"; //$NON-NLS-1$
	public final static String AVAILABLE_EMPLOYEES = "AvailableEmployees"; //$NON-NLS-1$
	public final static String PLAN_START_DATE = "StartDate"; //$NON-NLS-1$
	public final static String PLAN_END_DATE = "EndDate"; //$NON-NLS-1$
	public final static String PLAN_STATION = "Station"; //$NON-NLS-1$
	public final static String PLAN_TEAM = "Team"; //$NON-NLS-1$
	public final static String PLAN_PATROLS = "Patrols"; //$NON-NLS-1$
	public final static String PLAN_PARENT = "ParentPlan"; //$NON-NLS-1$
	public final static String PLAN_CREATOR = "Creator"; //$NON-NLS-1$
	public final static String PLAN_COMMENT = "Comment"; //$NON-NLS-1$
	
	private static IEditorPart templateEditor = null;
	
	/**
	 * 
	 * @return the custom plan template file or null
	 * if file does not exist
	 */
	public static File getCustomPlanTemplateLocation(){
		File f = new File(SmartPlanPlugIn.getDefault().getPlanDirectory(), PLAN_TEMPLATE);
		if (!f.exists()){
			return null;
		}
		return f;
	}
	
	/**
	 * 
	 * @return the current plan template
	 * @throws Exception
	 */
	public static InputStream getPlanTemplate() throws Exception{
		File custom = getCustomPlanTemplateLocation();
		if (custom != null){
			return new FileInputStream(custom);
		}
		return SmartPlanPlugIn.getDefault().getBundle().getResource("/org/wcs/smart/plan/report/planTemplate.rptdesign").openStream(); //$NON-NLS-1$
	}

	/**
	 * Imports the given file as the new plan template.
	 * 
	 * @param newTemplate
	 * @throws IOException
	 */
	public static void importPlanTemplate(File newTemplate) throws IOException{
		File f = new File(SmartPlanPlugIn.getDefault().getPlanDirectory(), PLAN_TEMPLATE);
		FileUtils.copyFile(newTemplate, f);
	}
	
	/**
	 * Export the plan to pdf
	 * @param planUuid
	 */
	public static void exportPlan(byte[] planUuid){
		ExportPlanJob job = new ExportPlanJob(planUuid);
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
			if (getCustomPlanTemplateLocation() == null){
				File f = new File(SmartPlanPlugIn.getDefault().getPlanDirectory(), PLAN_TEMPLATE);
				InputStream in = getPlanTemplate();
				OutputStream out = new FileOutputStream(f);
				try{
					IOUtils.copy(in, out);
				}finally{
					in.close();
					out.close();
				}
			}
			
			PlatformUI.getWorkbench().showPerspective(PlanReportPerspective.ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			ReportPlanEditorInput input = new ReportPlanEditorInput(getCustomPlanTemplateLocation());
			templateEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, IReportEditorContants.DESIGN_EDITOR_ID);
		}catch (Exception ex){
			SmartPlanPlugIn.displayLog(Messages.ReportPlan_ErrorEditingPlanTemplate + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
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
