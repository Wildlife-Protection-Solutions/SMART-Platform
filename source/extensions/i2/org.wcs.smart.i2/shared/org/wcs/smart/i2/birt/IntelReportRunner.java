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
package org.wcs.smart.i2.birt;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.impl.ReportEngine;
import org.eclipse.birt.report.engine.api.impl.RunAndRenderTask;
import org.hibernate.Session;
import org.wcs.smart.SmartTimezoneWrapper;
import org.wcs.smart.ca.ConservationArea;

import com.ibm.icu.util.TimeZone;

/**
 * BIRT Report running.  This initailizes the required parameters for running
 * the report, then runs the report.
 * 
 * @author Emily
 *
 */
public enum IntelReportRunner {

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
	 * App context projection provider variable
	 */
	public static final String PROJECTION_PROVIDER_CONTEXT_VAR = "org.wcs.smart.report.crs"; //$NON-NLS-1$
	
//	/**
//	 * Runs a SMART Report
//	 * @param report
//	 * @param engine
//	 * @param options
//	 * @param session
//	 * @param reportParameters
//	 * @throws Exception
//	 */
//	public void runReport(Report report, String currentUser, IReportEngine engine, IRenderOption options, 
//			Session session, HashMap<String, Object> reportParameters) throws Exception{
//		
//		File reportFile = new File(report.getConservationArea().getFileDataStoreLocation()
//				+ File.separator
//				+ Report.REPORT_DIR + File.separator + report.getFilename());
//		
//		runFile(reportFile, report.getConservationArea(), currentUser, engine, options, session, reportParameters);
//	}
//	
//

	@SuppressWarnings("unchecked")
	public void runFile(File file, ConservationArea ca, String currentUser, IReportEngine engine, IRenderOption options, 
			Session session, HashMap<String, Object> reportParameters) throws Exception{
		
		IReportRunnable design = engine.openReportDesign(file.getAbsolutePath());
		IRunAndRenderTask task = new SmartRunAndRender((ReportEngine) engine, design, ca, currentUser);
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
//	
//	/**
//	 * Runs a report file represented by an input stream.
//	 * @param is
//	 * @param ca
//	 * @param engine
//	 * @param options
//	 * @param session
//	 * @param reportParameters
//	 * @throws Exception
//	 */
//	@SuppressWarnings("unchecked")
//	public void runFile(InputStream is, ConservationArea ca, String currentUser, IReportEngine engine, IRenderOption options, 
//			Session session, HashMap<String, Object> reportParameters) throws Exception{
//		
//		IReportRunnable design = engine.openReportDesign(is);
//		IRunAndRenderTask task = new SmartRunAndRender((ReportEngine) engine, design, ca, currentUser);
//		
//		try{
//			task.getAppContext().put(CA_PARAM, ca);
//			task.getAppContext().put(SESSION_PARAM, session);
//			task.setRenderOption(options);
//			task.setParameterValues(reportParameters);
//			task.run();
//		}finally{
//			task.close();
//		}
//	}
//
	public static class SmartRunAndRender extends RunAndRenderTask{
		public SmartRunAndRender(ReportEngine engine, IReportRunnable runnable, ConservationArea ca, String currentUser) {
			super(engine, runnable);
		
			//hack to allow smart birt functions to have access to ca and user
			TimeZone tz = executionContext.getScriptContext().getTimeZone();
			SmartTimezoneWrapper ss = new SmartTimezoneWrapper(tz, ca, currentUser);
			executionContext.getScriptContext().setTimeZone(ss);
		}
		
	}
	
}
