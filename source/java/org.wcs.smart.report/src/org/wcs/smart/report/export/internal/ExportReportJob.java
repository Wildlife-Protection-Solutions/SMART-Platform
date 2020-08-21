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

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.IReportExporter;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;

/**
 * Job that runs an IReportExporter to export a report
 * @author Emily
 *
 */
public class ExportReportJob extends Job {

	private Report report = null;
	private Path outputFile = null;
	private IReportExporter exporter = null;
	
	private Map<String, Object> reportParameters = null; 
	
	/**
	 * @param report report to export
	 * @param file file to export to; will overwrite if file exists
	 * @param exporter output format info
	 * @param reportParams report parameters
	 */
	public ExportReportJob(Report report, Path file, IReportExporter exporter, Map<String, Object> reportParams){
		super(MessageFormat.format(Messages.ExportReportJob_ExportReportJobName, new Object[]{ report.getName()}));
		
		this.report = report;
		this.outputFile = file;
		this.exporter = exporter;
		this.reportParameters = reportParams;
		
	}
		
	/**
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			exporter.exportReport(outputFile, report, reportParameters, monitor);
		} catch (Exception e) {
			ReportPlugIn.log("Error exporting report", e); //$NON-NLS-1$
			return new Status(Status.ERROR, ReportPlugIn.PLUGIN_ID, Messages.ExportReportJob_ErrorExportingReport + e.getLocalizedMessage() );
		}
		return Status.OK_STATUS;
	}


}

