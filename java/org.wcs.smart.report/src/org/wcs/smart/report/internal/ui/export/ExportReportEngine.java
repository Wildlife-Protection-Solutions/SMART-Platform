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
package org.wcs.smart.report.internal.ui.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.SheetCollate;

import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.birt.report.engine.api.IPostscriptRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.SmartUtils;

/**
 * Engine for exporting reports.
 * <p>This engine if responsible for gathering parameter information
 * then running the reports and writing the results to a file.
 * <p>
 * @author egouge
 * @since 1.0.0
 */
public class ExportReportEngine {

	final static HashSet<Job> jobs = new HashSet<Job>();
	
	/**
	 * Exports a collection of reports.
	 * @param reports reports to export
	 * @param directory output directory
	 * @param outputFormat output format
	 */
	public static void exportReports(List<Report> reports, File directory, EmitterInfo outputFormat){
		
		validateDirectory(directory);
		HashMap<String, Object> params  = null;
		try{
			ParameterCollecter paramCollector = new ParameterCollecter();
			params = paramCollector.getParameters(reports.toArray(new Report[reports.size()]));
			if (params == null){
				//cancel pressed
				return;
			}
		}catch (Exception ex){
			ReportPlugIn.displayLog("Error occured while gathering paramter information.  Reports could not be run. " + ex.getMessage(), ex);
			return;
		}
		
		
		for (int i = 0; i < reports.size(); i ++){
			final RunReportJob rr = new RunReportJob(reports.get(i), getOutputFileName(reports.get(i), directory,outputFormat),outputFormat, params);
			jobs.add(rr);
			rr.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					jobs.remove(rr);
					checkJobs();
				}
				
			});
			rr.schedule();
		}
	}
	
	/**
	 * checks if all jobs are done and if so displays info dialog.
	 */
	private static void checkJobs(){
		if (jobs.size() == 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (jobs.size() == 0){  //double check to make sure another export hasn't happened 
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Export Report", "Report export completed.");
					}
				}});
		}
	}
	/*
	 * Ensure the given directory exists
	 */
	private static void validateDirectory(File directory){
		if (!directory.exists()){
			SmartUtils.createDirectory(directory);
		}
	}
	
	/*
	 * Converts a report name to a output file name
	 */
	private static File getOutputFileName(Report report, File directory, EmitterInfo outputFormat){
		return new File(directory, report.getName().replaceAll("[^a-zA-z0-9]", "") + "." + outputFormat.getFormat());
	}
	
	/**
	 * Exports a single report.
	 * @param report report to export
	 * @param outputFile output file
	 * @param outputFormat output format
	 */
	public static void exportReport(Report report, File outputFile, EmitterInfo outputFormat){
		validateDirectory(outputFile.getParentFile());
		
		HashMap<String, Object> params  = null;
		try{
			ParameterCollecter paramCollector = new ParameterCollecter();
			params = paramCollector.getParameters(new Report[]{report});
			if (params == null){
				//cancel pressed
				return;
			}
		}catch (Exception ex){
			ReportPlugIn.displayLog("Error occured while gathering paramter information.  Report could not be run. " + ex.getMessage(), ex);
			return;
		}
		final RunReportJob rr = new RunReportJob(report,outputFile, outputFormat, params);
		rr.schedule();
		jobs.add(rr);
		rr.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				jobs.remove(rr);
				checkJobs();
			}
			
		});
		
	}
	
	public static void printReport(Report report) throws Exception{
		
		
		ParameterCollecter paramCollector = new ParameterCollecter();
		HashMap<String, Object> reportParameters = paramCollector.getParameters(new Report[]{report});
		
		PrintDialog pd = new PrintDialog(Display.getDefault().getActiveShell());
		PrinterData data = pd.open();
		if (data == null){
			return;
		}
		
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		PrintService service = null;
		for (int i = 0; i < services.length; i++) {
			if (services[i].getName().equals(data.name)) {
				service  = services[i];
				break;
			}
		}
		if (service == null){
			
			//ERROR
			return;
		}
		
		File reportFile = 	report.getFullReportFilename();;
	
		if (reportFile == null ){
			throw new Exception("Cannot run report.");
		}
		IReportEngine engine = ReportManager.getReportEngine();
		
		final IReportRunnable design = engine.openReportDesign(reportFile.getAbsolutePath());

		IRunAndRenderTask task = engine.createRunAndRenderTask(design);
		IRenderOption options = new RenderOption();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		options.setOutputStream(out);
		options.setEmitterID("org.eclipse.birt.report.engine.emitter.postscript");
		
		task.setRenderOption(options);
		task.setParameterValues(reportParameters);
		task.run();
		task.close();
		
		PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
		pras.add(new Copies(data.copyCount));
		pras.add(data.collate ? SheetCollate.COLLATED : SheetCollate.UNCOLLATED);
		
		
		DocPrintJob job = service.createPrintJob();
	
		InputStream pin = new ByteArrayInputStream(out.toByteArray());
		out.close();
		Doc doc = new SimpleDoc(pin, DocFlavor.INPUT_STREAM.POSTSCRIPT, null);
		job.print(doc, pras);
		pin.close();
		
		doc.getStreamForBytes().close();
		
//		Doc doc = new SimpleDoc(arg0, arg1, arg2)
		
		
		
	}
}
