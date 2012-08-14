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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.IExportFormat;
import org.wcs.smart.report.export.IReportExporter;
import org.wcs.smart.report.internal.ui.export.ParameterCollecter;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.SmartUtils;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

/**
 * Engine for exporting reports.
 * <p>This engine if responsible for gathering parameter information
 * then running the reports and writing the results to a file.
 * <p>
 * @author egouge
 * @since 1.0.0
 */
public class ExportReportEngine {

	/**
	 * Export options extention
	 */
	public static final String REPORT_EXPORT_EXTENSION_ID = "org.wcs.smart.report.exporter";
		
	/**
	 * Export jobs
	 */
	private final static HashSet<Job> jobs = new HashSet<Job>();
	
	/**
	 * Exports a collection of reports using a BIRT emitter.
	 * 
	 * @param reports reports to export
	 * @param directory output directory or file if reports.size() == 1
	 * @param outputFormat output format
	 */
	public static void exportReports(List<Report> reports, File directory, EmitterInfo outputFormat){
		HashMap<String, Object> params  = collectParameters(reports);
		if (params == null) return;
		
		
		for (int i = 0; i < reports.size(); i ++){
			File outputFile = directory;
			if (reports.size() > 1){
				outputFile = getOutputFileName(reports.get(i), directory,outputFormat.getFormat()); 
			}
			final RunReportJob rr = new RunReportJob(
					reports.get(i), 
					outputFile,
					outputFormat, params);
			
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
	 * Exports a collection of reports using an IReportExporter
	 * 
	 * @param reports reports to export
	 * @param directory output directory or file is reports.size() == 1
	 * @param outputFormat output format
	 */
	public static void exportReports(List<Report> reports, File directory, IReportExporter exporter){
		HashMap<String, Object> params = null;
		if (exporter.requiresParameters()){
			params = collectParameters(reports);
			if (params == null) return;
		}
		
		for (int i = 0; i < reports.size(); i ++){
			File outputFile = directory;
			if (reports.size() > 1){
				outputFile = getOutputFileName(reports.get(i), directory,exporter.getExportFormat()); 
			}
			final ExportReportJob rr = new ExportReportJob(reports.get(i), 
					outputFile,
					exporter, params);

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
	
	public static HashMap<String, Object> collectParameters(List<Report> reports){
		HashMap<String, Object> params  = null;
		try{
			ParameterCollecter paramCollector = new ParameterCollecter();
			params = paramCollector.getParameters(reports.toArray(new Report[reports.size()]));
			if (params == null){
				//cancel pressed
				return  null;
			}
		}catch (Exception ex){
			ReportPlugIn.displayLog("Error occured while gathering paramter information.  Reports could not be run. " + ex.getMessage(), ex);
			return null;
		}
		return params;
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
	public static void validateDirectory(File directory){
		if (!directory.exists()){
			SmartUtils.createDirectory(directory);
		}
	}
	
	/*
	 * Converts a report name to a output file name
	 */
	private static File getOutputFileName(Report report, File directory, String extension){
		return new File(directory, report.getName().replaceAll("[^a-zA-z0-9]", "") + "." + extension);
	}
	
	// test for printing directly to printer - does not currently work export
	//for postscript printers
	public static void printReport(Report report) throws Exception{
		
		
		ParameterCollecter paramCollector = new ParameterCollecter();
		HashMap<String, Object> reportParameters = paramCollector.getParameters(new Report[]{report});
		
		PrintDialog pd = new PrintDialog(Display.getDefault().getActiveShell());
		PrinterData data = pd.open();
		if (data == null){
			return;
		}
		
		PrintService[] services = PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.PDF, null);
		PrintService service = null;
		for (int i = 0; i < services.length; i++) {
			if (services[i].getName().equals(data.name)) {
				service  = services[i];
				break;
			}
		}
		if (service == null){
			System.out.println("NOT SUPPORTED");
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
		options.setEmitterID("org.eclipse.birt.report.engine.emitter.pdf");
		
		task.setRenderOption(options);
		task.setParameterValues(reportParameters);
		task.run();
		task.close();
		
		PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
		pras.add(new Copies(data.copyCount));
		pras.add(data.collate ? SheetCollate.COLLATED : SheetCollate.UNCOLLATED);
		
		
		
		InputStream pin = new ByteArrayInputStream(out.toByteArray());
		out.close();
		
		DocPrintJob job = service.createPrintJob();
		DocFlavor[] supported = service.getSupportedDocFlavors();
		System.out.println("SUPPORTED---------------");
		for(int i = 0; i < supported.length; i ++){
			System.out.println(supported[i]);
		}
		System.out.println("SUPPORTED---------------");
		
		Doc doc = new SimpleDoc(pin, DocFlavor.INPUT_STREAM.PDF, null);
		job.print(doc, pras);
		pin.close();
		
		doc.getStreamForBytes().close();
//		
//		PdfReader pdfReader = new PdfReader(pin);
//
//		PdfStamper pdfStamper = new PdfStamper(pdfReader,out);
//		pdfStamper.addJavaScript("this.print({bUI: true, bSilent:false});\r");
//		pdfStamper.close();
//
//		pin.close();
//		Doc doc = new SimpleDoc(arg0, arg1, arg2)
		
		
		
	}
	
	/**
	 * Supported formats include all BIRT emitters
	 * and any REPORT_EXPORT_EXTENSION_ID extensions.
	 * 
	 * @return an array of support export formats
	 */
	public static IExportFormat[] getSupportedExportFormats(){
		EmitterInfo[] info = ReportManager.getReportEngine().getEmitterInfo();
		List<IExportFormat> formats = new ArrayList<IExportFormat>();
		for (int i = 0; i < info.length; i ++){
			formats.add(new BirtEmitterExportFormat(info[i]));
		}
		if (Platform.getExtensionRegistry() != null){
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(REPORT_EXPORT_EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					IReportExporter prop = (IReportExporter) e.createExecutableExtension("class");
					formats.add(new ReportExporterFormat(prop));
				}
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
		return formats.toArray(new IExportFormat[formats.size()]);
	}
}
