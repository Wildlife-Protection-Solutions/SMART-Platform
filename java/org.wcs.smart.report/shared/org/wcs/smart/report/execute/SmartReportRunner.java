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
package org.wcs.smart.report.execute;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.report.model.Report;

/**
 * BIRT Report running.  This initailizes the required parameters for running
 * the report, then runs the report.
 * 
 * @author Emily
 *
 */
public enum SmartReportRunner {

	INSTANCE;
	
	/**
	 * Conservation Area report parameter 
	 */
	public static final String CA_PARAM = "org.wcs.smart.ca"; //$NON-NLS-1$

	/**
	 * Database session report parameter
	 */
	public static final String SESSION_PARAM = "org.wcs.smart.session"; //$NON-NLS-1$
	
	/**
	 * Runs a SMART Report
	 * @param report
	 * @param engine
	 * @param options
	 * @param session
	 * @param reportParameters
	 * @throws Exception
	 */
	public void runReport(Report report, IReportEngine engine, IRenderOption options, 
			Session session, HashMap<String, Object> reportParameters) throws Exception{
		
		File reportFile = new File(report.getConservationArea().getFileDataStoreLocation()
				+ File.separator
				+ Report.REPORT_DIR + File.separator + report.getFilename());
		
		runFile(reportFile, report.getConservationArea(), engine, options, session, reportParameters);
	}
	

	/**
	 * Runs a report file
	 * @param file
	 * @param ca
	 * @param engine
	 * @param options
	 * @param session
	 * @param reportParameters
	 * @throws Exception
	 */
	public void runFile(File file, ConservationArea ca, IReportEngine engine, IRenderOption options, 
			Session session, HashMap<String, Object> reportParameters) throws Exception{
		
		IReportRunnable design = engine.openReportDesign(file.getAbsolutePath());
		IRunAndRenderTask task = engine.createRunAndRenderTask(design);
		
		try{
			task.getAppContext().put(CA_PARAM, ca);
			task.getAppContext().put(SESSION_PARAM, session);
			task.setRenderOption(options);
			task.setParameterValues(reportParameters);
			task.run();
		}finally{
			task.close();
		}
	}
	
	/**
	 * Runs a report file represented by an input stream.
	 * @param is
	 * @param ca
	 * @param engine
	 * @param options
	 * @param session
	 * @param reportParameters
	 * @throws Exception
	 */
	public void runFile(InputStream is, ConservationArea ca, IReportEngine engine, IRenderOption options, 
			Session session, HashMap<String, Object> reportParameters) throws Exception{
		
		IReportRunnable design = engine.openReportDesign(is);
		IRunAndRenderTask task = engine.createRunAndRenderTask(design);
		
		try{
			task.getAppContext().put(CA_PARAM, ca);
			task.getAppContext().put(SESSION_PARAM, session);
			task.setRenderOption(options);
			task.setParameterValues(reportParameters);
			task.run();
		}finally{
			task.close();
		}
	}
}
