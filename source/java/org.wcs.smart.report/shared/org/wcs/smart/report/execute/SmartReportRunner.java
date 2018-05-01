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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IRenderTask;
import org.eclipse.birt.report.engine.api.IReportDocument;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.IRunTask;
import org.eclipse.birt.report.engine.api.impl.ReportEngine;
import org.hibernate.Session;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.birt.SmartRunAndRender;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.report.model.Report;

/**
 * BIRT Report running.  This initailizes the required parameters for running
 * the report, then runs the report.  Reports can be run in a single runandrender task
 * or as separate run tasks and render tasks
 * 
 * @author Emily
 *
 */
/*
 * See assembla ticket 2390 for more info on runandrender tasks vs run task and render tast
 */
public enum SmartReportRunner {

	INSTANCE;
	
	/**
	 * Runs a SMART Report as a separate run task and render task.  Thhe
	 * run task is writting to the temporary file.
	 * 
	 * @param renderFile file to write report run task to.  File will be deleted if it 
	 * already exists
	 * @param report
	 * @param engine
	 * @param options
	 * @param session
	 * @param reportParameters
	 * @throws Exception
	 */
	public void runReport(Report report, String currentUser, IReportEngine engine, IRenderOption options, 
			Session session, HashMap<String, Object> reportParameters,
			Path renderFile) throws Exception{
		
		File reportFile = new File(report.getConservationArea().getFileDataStoreLocation()
				+ File.separator
				+ Report.REPORT_DIR + File.separator + report.getFilename());
		

		IReportRunnable reportRunnable = engine.openReportDesign(reportFile.getAbsolutePath());
		
		IRunTask runTask = engine.createRunTask(reportRunnable);
		runTask.setParameterValues(reportParameters);
		runTask.getAppContext().put(BirtConstants.CA_PARAM, report.getConservationArea());
		runTask.getAppContext().put(BirtConstants.SESSION_PARAM, session);
		
		if (Files.exists(renderFile)) Files.delete(renderFile);
		runTask.run(renderFile.toAbsolutePath().toString());
		runTask.close();
		
		renderFile(engine, options, renderFile, report.getConservationArea(), session);
	}
	
	/**
	 * Renders a report document file to an output format
	 * @throws EngineException 
	 */
	public void renderFile(IReportEngine engine, IRenderOption options, Path reportDoc, ConservationArea ca, Session session) throws EngineException {
		IReportDocument reportDocument = engine.openReportDocument(reportDoc.toAbsolutePath().toString());
		try {
			IRenderTask task = engine.createRenderTask(reportDocument);
			try {
				task.getAppContext().put(BirtConstants.CA_PARAM, ca);
				task.getAppContext().put(BirtConstants.SESSION_PARAM, session);
				task.setRenderOption(options);
				task.render();
			}finally {
				task.close();
			}
		}finally {
			reportDocument.close();
		}
	}
	
	/**
	 * Runs a SMART Report as a single run and render task
	 * @param report
	 * @param engine
	 * @param options
	 * @param session
	 * @param reportParameters
	 * @throws Exception
	 */
	public void runReport(Report report, String currentUser, IReportEngine engine, IRenderOption options, 
			Session session, HashMap<String, Object> reportParameters) throws Exception{
		
		File reportFile = new File(report.getConservationArea().getFileDataStoreLocation()
				+ File.separator
				+ Report.REPORT_DIR + File.separator + report.getFilename());
		
		runFile(reportFile, report.getConservationArea(), currentUser, engine, options, session, reportParameters);
	}
	

	/**
	 * Runs a report file in a single runandrender task
	 * @param file
	 * @param ca
	 * @param engine
	 * @param options
	 * @param session
	 * @param reportParameters
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void runFile(File file, ConservationArea ca, String currentUser, IReportEngine engine, IRenderOption options, 
			Session session, HashMap<String, Object> reportParameters) throws Exception{
		
		IReportRunnable design = engine.openReportDesign(file.getAbsolutePath());
		IRunAndRenderTask task = new SmartRunAndRender((ReportEngine) engine, design, ca, currentUser);
		try{
			task.getAppContext().put(BirtConstants.CA_PARAM, ca);
			task.getAppContext().put(BirtConstants.SESSION_PARAM, session);
			task.setRenderOption(options);
			task.setParameterValues(reportParameters);
			task.run();
		}finally{
			task.close();
		}

	}
	
	/**
	 * Runs a report file represented by an input stream as a single 
	 * runandrender task
	 * @param is
	 * @param ca
	 * @param engine
	 * @param options
	 * @param session
	 * @param reportParameters
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void runFile(InputStream is, ConservationArea ca, String currentUser, IReportEngine engine, IRenderOption options, 
			Session session, HashMap<String, Object> reportParameters) throws Exception{
		
		IReportRunnable design = engine.openReportDesign(is);
		IRunAndRenderTask task = new SmartRunAndRender((ReportEngine) engine, design, ca, currentUser);
		
		try{
			task.getAppContext().put(BirtConstants.CA_PARAM, ca);
			task.getAppContext().put(BirtConstants.SESSION_PARAM, session);
			task.setRenderOption(options);
			task.setParameterValues(reportParameters);
			task.run();
		}finally{
			task.close();
		}
	}
	
}
