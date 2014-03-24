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
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;

import net.refractions.udig.catalog.URLUtils;

import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.util.SmartUtils;
/**
 * Job for exporting a plan to a pdf file for printing
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class ExportPlanJob extends Job {

	private static final String NONE = Messages.ExportPlanJob_NoneLabel;
	
	private byte[] planUuid;
	private File outputFile;
	
	public ExportPlanJob(byte[] planUuid) {
		super(Messages.ExportPlanJob_JobName);
		this.planUuid = planUuid;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		HashMap<String, Object> reportParameters = new HashMap<String, Object>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		Plan plan = null;
		try{
			plan = (Plan) session.load(Plan.class, planUuid);
			this.outputFile = File.createTempFile(URLUtils.cleanFilename(plan.getId()) + "_", ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
			outputFile.deleteOnExit();
			
			reportParameters.put(ReportPlan.PLAN_UUID, SmartUtils.encodeHex(plan.getUuid()));
			reportParameters.put(ReportPlan.PLAN_ID, plan.getId());
			reportParameters.put(ReportPlan.PLAN_NAME, plan.getName());
			reportParameters.put(ReportPlan.PLAN_DESCRIPTION, (plan.getDescription()==null?"" : plan.getDescription())); //$NON-NLS-1$
			reportParameters.put(ReportPlan.PLAN_END_DATE,  new java.sql.Date(plan.getEndDate().getTime()));
			reportParameters.put(ReportPlan.PLAN_START_DATE, new java.sql.Date(plan.getStartDate().getTime()));
			reportParameters.put(ReportPlan.PLAN_TYPE, plan.getType().getName());
			reportParameters.put(ReportPlan.UNAVAILABLE_EMPLOYEES, plan.getUnavailableEmployees());
			reportParameters.put(ReportPlan.AVAILABLE_EMPLOYEES, plan.getActiveEmployees());
			if (plan.getTeam() == null){
				reportParameters.put(ReportPlan.PLAN_TEAM,NONE);
			}else{
				reportParameters.put(ReportPlan.PLAN_TEAM,plan.getTeam().getName());
			}
			
			if (plan.getStation() == null){
				reportParameters.put(ReportPlan.PLAN_STATION,NONE);
			}else{
				reportParameters.put(ReportPlan.PLAN_STATION,plan.getStation().getName());
			}
			reportParameters.put(ReportPlan.PLAN_COMMENT, plan.getComment() == null ? "" : plan.getComment()); //$NON-NLS-1$
			reportParameters.put(ReportPlan.PLAN_CREATOR, plan.getCreator() == null ? "" : plan.getCreator().getFullLabel()); //$NON-NLS-1$
						
			StringBuilder sb = new StringBuilder();
			List<PatrolEditorInput> ins = PlanHibernateManager.getPatrols(plan, session);
			for (PatrolEditorInput in : ins){
				sb.append(in.getPatrolId());
				sb.append(", "); //$NON-NLS-1$
			}
			getChildPlanPatrols(plan, sb, session);
			if (sb.length() > 0){
				sb.delete(sb.length()-2, sb.length());
			}else{
				sb.append(NONE);
			}
			reportParameters.put(ReportPlan.PLAN_PATROLS, sb.toString());
			
			if (plan.getParent() == null){
				reportParameters.put(ReportPlan.PLAN_PARENT, NONE);
			}else{
				reportParameters.put(ReportPlan.PLAN_PARENT, plan.getParent().getLabel());
			}
		}catch (Exception ex){
			SmartPlanPlugIn.displayLog(Messages.ExportPlanJob_ErrorExportingPlan + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return Status.CANCEL_STATUS;
		}finally{
			session.getTransaction().rollback();
			session.close();
		}
		
		try{
			IReportEngine engine = ReportEngineManager.getBirtReportEngine();
		
			final IReportRunnable design = engine.openReportDesign(ReportPlan.getPlanTemplate());
			
			
			FileOutputStream fout = new FileOutputStream(outputFile);
			try{
				IRenderOption options = new RenderOption();
				options.setOutputStream(fout);
				options.setEmitterID("org.eclipse.birt.report.engine.emitter.pdf"); //$NON-NLS-1$
				options.setOption(HTMLRenderOption.IMAGE_DIRECTROY, outputFile.getParent());
				options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
				
				IRunAndRenderTask task = engine.createRunAndRenderTask(design);
				try{
					task.setRenderOption(options);
					task.setParameterValues(reportParameters);
					task.run();
				}finally{
					task.close();
				}
			}finally{
				fout.close();
			}
			
		}catch (Exception ex){
			SmartPlanPlugIn.displayLog(Messages.ExportPlanJob_ErrorExportingPlan + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return Status.CANCEL_STATUS;
		}
		
		//launch file
		AttachmentUtil.launch(outputFile);
		
		//open in system editor
		return Status.OK_STATUS;
	}
	
	
	
	/**
	 * 
	 * @return the current plan associated with the editor
	 */
	private void getChildPlanPatrols(Plan plan, StringBuilder sb, Session session){
		if(plan.getChildren() == null){
			return;
		}
		for (Plan p : plan.getChildren()){
			List<PatrolEditorInput> ins = PlanHibernateManager.getPatrols(p, session);
			for (PatrolEditorInput in : ins){
				sb.append(in.getPatrolId());
				sb.append(", "); //$NON-NLS-1$
			}
			getChildPlanPatrols(p, sb, session);
		}
	}

}
