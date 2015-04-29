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
import java.text.MessageFormat;
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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.IExportFormat;
import org.wcs.smart.report.export.IReportExporter;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.export.ParameterCollecter;
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

	/**
	 * Export options extention
	 */
	public static final String REPORT_EXPORT_EXTENSION_ID = "org.wcs.smart.report.exporter"; //$NON-NLS-1$
		
	/**
	 * Export jobs
	 */
	private final static HashSet<Job> jobs = new HashSet<Job>();
	
	/**
	 * Exports a collection of reports using an IReportExporter or BIRT emitter.  Only
	 * one of outputFormat or exporter should be supplied.
	 * 
	 * @param reports reports to export
	 * @param directory output directory. Only one of directory or file is used and only one of them should be supplied.
	 * @param file the full path file to output to; only valid is reports.size() == 1
	 * @param outputFormat output format
	 * @param exporter the report exportor
	 */
	private static void exportReports(List<Report> reports, File directory, File file, EmitterInfo outputFormat, IReportExporter exporter){
		if (exporter == null && outputFormat == null){
			return;
		}
		HashMap<String, Object> params = null;
		if (outputFormat != null){
			params  = collectParameters(reports);
			if (params == null) return;
		}else if (exporter != null && exporter.requiresParameters()){
			params = collectParameters(reports);
			if (params == null) return;
		}
		
		boolean yesToOverwrite = false;
		JobChangeAdapter endJob = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				jobs.remove(event.getJob());
				checkJobs();
			}
		};
		
		for (int i = 0; i < reports.size(); i ++){
			File outputFile = file;
			if (file == null){
				if (outputFormat != null){
					outputFile = getOutputFileName(reports.get(i), directory,outputFormat.getFormat());
				}else if (exporter != null){
					outputFile = getOutputFileName(reports.get(i), directory,exporter.getExportFormat());
				}
			}
			if (outputFile.exists()){
				if (!yesToOverwrite){
					MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(),
							Messages.ExportReportEngine_OverwriteFile,
							MessageDialog.getImage(Dialog.DLG_IMG_MESSAGE_INFO),
							MessageFormat.format(Messages.ExportReportEngine_FileExists, new Object[]{outputFile.toString()}), 
							MessageDialog.INFORMATION,
							new String[]{IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.YES_LABEL, IDialogConstants.SKIP_LABEL},
							0);
					int ret = md.open();
					if (ret == 2){
						//skip
						continue;
					}else if (ret == 0){
						yesToOverwrite = true;
					}
				}
			}
			
			Job rr = null;
			if (outputFormat != null){
				rr = new RunReportJob(reports.get(i), outputFile, outputFormat, params);
			}else if (exporter != null){
				rr = new ExportReportJob(reports.get(i), outputFile, exporter, params);
			}
			if (rr != null){
				rr.addJobChangeListener(endJob);
				jobs.add(rr);
			}
		}
		
		for (Job j : jobs){
			j.schedule();
		}
	}
	/**
	 * Exports a collection of reports using a BIRT emitter.
	 * 
	 * @param reports reports to export
	 * @param directory output directory. Only one of directory or file is used and only one of them should be supplied.
	 * @param file the full path file to output to; only valid is reports.size() == 1
	 * @param outputFormat output format
	 */
	public static void exportReports(List<Report> reports, File directory, File file, EmitterInfo outputFormat){
		exportReports(reports, directory, file, outputFormat, null);
	}
	
	/**
	 * Exports a collection of reports using an IReportExporter
	 * 
	 * @param reports reports to export
	 * @param directory output directory. Only one of directory or file is used and only one of them should be supplied.
	 * @param file the full path file to output to; only valid is reports.size() == 1
	 * @param outputFormat output format
	 */
	public static void exportReports(List<Report> reports, File directory, File file, IReportExporter exporter){
		exportReports(reports, directory, file, null, exporter);
		
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
			ReportPlugIn.displayLog(Messages.ExportReportEngine_Error_GatheringParameters + ex.getLocalizedMessage(), ex);
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
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.ExportReportEngine_ExportComplete_DialogTitle, Messages.ExportReportEngine_ExportComplete_DialogMessage);
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
	
	/**
	 * Generates a filename for a given report.  The filename will
	 * be based on the existing report filename with a new extension.
	 * <p>
	 * 
	 * @param report report to generate filename for
	 * @param directory location of output file can be null
	 * @param extension new file extension
	 */
	public static File getOutputFileName(Report report, File directory, String extension){
		String f = URLUtils.cleanFilename(report.getName() + "_" + report.getId()); //$NON-NLS-1$
		int index = f.lastIndexOf('.');
		if (index > 0){
			f = f.substring(0, index);
		}
		if (directory != null){
			return new File(directory, f + "." + extension); //$NON-NLS-1$
		}else{
			return new File(f + "." + extension); //$NON-NLS-1$
		}
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
			System.out.println("NOT SUPPORTED"); //$NON-NLS-1$
			//ERROR
			return;
		}
		
		File reportFile = 	report.getFullReportFilename();;
	
		if (reportFile == null ){
			throw new Exception("Cannot run report."); //$NON-NLS-1$
		}
		IReportEngine engine = ReportEngineManager.getBirtReportEngine();
		
		final IReportRunnable design = engine.openReportDesign(reportFile.getAbsolutePath());
		IRenderOption options = new RenderOption();
		try(ByteArrayOutputStream out = new ByteArrayOutputStream()){
		
			options.setOutputStream(out);
			options.setEmitterID("org.eclipse.birt.report.engine.emitter.pdf"); //$NON-NLS-1$
			IRunAndRenderTask task = engine.createRunAndRenderTask(design);
			try{
				task.setRenderOption(options);
				task.setParameterValues(reportParameters);
				task.run();
			}finally{
				task.close();
			}
			
			try(InputStream pin = new ByteArrayInputStream(out.toByteArray())){
				PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
				pras.add(new Copies(data.copyCount));
				pras.add(data.collate ? SheetCollate.COLLATED : SheetCollate.UNCOLLATED);
				
				DocPrintJob job = service.createPrintJob();
				DocFlavor[] supported = service.getSupportedDocFlavors();
				System.out.println("SUPPORTED---------------"); //$NON-NLS-1$
				for(int i = 0; i < supported.length; i ++){
					System.out.println(supported[i]);
				}
				System.out.println("SUPPORTED---------------"); //$NON-NLS-1$
				
				Doc doc = new SimpleDoc(pin, DocFlavor.INPUT_STREAM.PDF, null);
				job.print(doc, pras);
				pin.close();
					
				
				doc.getStreamForBytes().close();
			}
		}		
	}
	
	/**
	 * Supported formats include all BIRT emitters
	 * and any REPORT_EXPORT_EXTENSION_ID extensions.
	 * 
	 * @return an array of support export formats
	 */
	public static IExportFormat[] getSupportedExportFormats(){
		EmitterInfo[] info = ReportEngineManager.getBirtReportEngine().getEmitterInfo();
		List<IExportFormat> formats = new ArrayList<IExportFormat>();
		for (int i = 0; i < info.length; i ++){
			formats.add(new BirtEmitterExportFormat(info[i]));
		}
		if (Platform.getExtensionRegistry() != null){
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(REPORT_EXPORT_EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					IReportExporter prop = (IReportExporter) e.createExecutableExtension("class"); //$NON-NLS-1$
					formats.add(new ReportExporterFormat(prop));
				}
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
		return formats.toArray(new IExportFormat[formats.size()]);
	}
}
