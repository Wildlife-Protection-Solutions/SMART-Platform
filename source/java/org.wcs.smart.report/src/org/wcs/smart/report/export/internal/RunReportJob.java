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
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;

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
		
		reportFile = report.getFullReportFilename();
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
			IReportEngine engine = ReportEngineManager.getBirtReportEngine();

			final IReportRunnable design = engine.openReportDesign(reportFile.getAbsolutePath());
			
			IRenderOption options = new RenderOption();
			FileOutputStream fout = new FileOutputStream(outputFile);
			try{
				options.setOutputStream(fout);
				options.setEmitterID(info.getID());
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

		} catch (Exception e) {
			ReportPlugIn.log("Error exporting report", e); //$NON-NLS-1$
			return new Status(Status.ERROR, ReportPlugIn.PLUGIN_ID, Messages.RunReportJob_Error_RunningReport + e.getLocalizedMessage() );
		}
		return Status.OK_STATUS;
	}


}
