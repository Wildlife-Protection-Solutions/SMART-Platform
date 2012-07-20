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

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.wcs.smart.report.ReportPlugIn;
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
			RunReportJob rr = new RunReportJob(reports.get(0), getOutputFileName(reports.get(i), directory,outputFormat),outputFormat, params);
			rr.schedule();
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
		RunReportJob rr = new RunReportJob(report,outputFile, outputFormat, params);
		rr.schedule();
		
	}
}
