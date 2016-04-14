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
package org.wcs.smart.report.export.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.execute.SmartReportRunner;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Job that runs a report and writes the results to a file.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class RunReportJob extends Job {

	private File reportFile = null;
	private File outputFile = null;
	private EmitterInfo info = null;
	private Report report;
	private HashMap<String, Object> reportParameters = null; 
	
	/**
	 * Creates a new job
	 * 
	 * @param report report to export
	 * @param file file to export to; will overwrite if file exists
	 * @param info output format info
	 * @param reportParams report parameters
	 */
	public RunReportJob(Report report, File file, EmitterInfo info, HashMap<String, Object> reportParams){
		super(MessageFormat.format(Messages.RunReportJob_JobName, new Object[]{report.getName()}));
		
		this.report = report;
		this.reportFile = ReportPlugIn.getDefault().getReportFile(report);
		this.outputFile = file;
		this.info = info;
		this.reportParameters = reportParams;
		
	}
		
	/**
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			if (reportFile == null || outputFile == null || info == null) {
				throw new Exception(Messages.RunReportJob_Error_NoReportFile);
			}

			IRenderOption options = new RenderOption();
			try(FileOutputStream fout = new FileOutputStream(outputFile)){
				options.setOutputStream(fout);
				options.setEmitterID(info.getID());
				options.setOption(HTMLRenderOption.IMAGE_DIRECTROY, outputFile.getParent());
				options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
				
				Session session = HibernateManager.openSession();
				try{
					SmartReportRunner.INSTANCE.runReport(report, 
							SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee()),
							ReportEngineManager.getBirtReportEngine(), 
							options, session, reportParameters);
				}finally{
					session.close();
				}
			}
		} catch (Exception e) {
			ReportPlugIn.log("Error exporting report", e); //$NON-NLS-1$
			return new Status(Status.ERROR, ReportPlugIn.PLUGIN_ID,
					MessageFormat.format(Messages.RunReportJob_Error_RunningReport1, new Object[]{report.getName()}) + e.getLocalizedMessage() );
		}
		return Status.OK_STATUS;
	}


}
