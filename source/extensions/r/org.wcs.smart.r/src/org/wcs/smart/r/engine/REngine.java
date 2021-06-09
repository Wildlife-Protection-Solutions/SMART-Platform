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
package org.wcs.smart.r.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.RScriptManager;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.r.ui.RPreferencePage;
import org.wcs.smart.util.UuidUtils;

/**
 * RScript engine that runs the queries then the script 
 * 
 * @author Emily
 *
 */
public class REngine {

	private static final String ERROR_PREFIX = Messages.REngine_ErrorPrefix;
	private List<QueryConfiguration> queryConfigs;
	private String rParameters;
	
	private OutputStream outStream;
	private RScript script;
	
	/**
	 * Creates a new engine; does not run the script.  Must call execute() to run the script
	 * 
	 * @param script
	 * @param queryConfigs
	 * @param rParameters
	 * @param outStream
	 */
	public REngine(RScript script, List<QueryConfiguration> queryConfigs, String rParameters, OutputStream outStream) {
		this.queryConfigs = queryConfigs;
		this.rParameters = rParameters;
		this.outStream = outStream;
		this.script = script;
	}
	
	private void writeString(String string) throws IOException {
		if (outStream == null) return;
		outStream.write(string.getBytes());
		outStream.write("\n".getBytes()); //$NON-NLS-1$
	}
	
	public void execute()  {
		if (RPreferencePage.getRSystemProperty()== null || RPreferencePage.getRSystemProperty().isEmpty()) {
			//open preference page
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.REngine_Title, Messages.REngine_RRequiredMessage);
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, RPreferencePage.ID, null, null);
			dialog.open();
			return;
		}
		executeJob.schedule();
	}
	
	private Job executeJob = new Job(Messages.REngine_JobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Path> queryFiles = new ArrayList<>();
			try {
				for (QueryConfiguration query : queryConfigs) {
					writeString(MessageFormat.format(Messages.REngine_QueryName, query.getQuery().getName()));
					writeString(MessageFormat.format(Messages.REngine_DateFilter, query.getDateFilter().asString()));
					writeString(MessageFormat.format(Messages.REngine_Format, query.getQueryExporter().getDefaultExtension()));
					
					//execute query
					IQueryType qtype = QueryTypeManager.INSTANCE.findQueryType(query.getQuery().getTypeKey());
					IQueryEngine engine = QueryTypeManager.INSTANCE.getQueryEngine(qtype);
					
					IQueryResult results = null;
							
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						try {
							query.getQuery().setDateFilter(query.getDateFilter());
							HashMap<String, Object> parameters = new HashMap<>();
							parameters.put(Session.class.getName(), session);
							parameters.put(IProgressMonitor.class.getName(), new NullProgressMonitor());
							
							results = engine.executeQuery(query.getQuery(), parameters);
							session.getTransaction().commit();
						}catch (Exception ex) {
							session.getTransaction().rollback();
							throw ex;
						}
					}
					
					String filename = UuidUtils.uuidToString(query.getQuery().getUuid()) + "." + System.nanoTime() + "." + query.getQueryExporter().getDefaultExtension(); //$NON-NLS-1$ //$NON-NLS-2$
					
					Path p = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(filename).normalize();
					queryFiles.add(p);
					HashMap<String, Object> parameters = new HashMap<>();
					
					//we don't delete these until smart is re-run; that way users can use them for debugging if desired
					query.getQueryExporter().export(query.getQuery(), results, p, parameters, new NullProgressMonitor());
					writeString(Messages.REngine_ExportFile + p.toString());
					writeString("-----------------------------------------------------------------------------------------------"); //$NON-NLS-1$
				}
				
				List<String> params = new ArrayList<>();
				
				params.add(RPreferencePage.getRSystemProperty());
				params.add(RScriptManager.INSTANCE.getScriptPath(script).toRealPath().toString());
				if (rParameters != null && !rParameters.strip().isBlank()) {
					String[] parts = rParameters.strip().split("\\s+"); //$NON-NLS-1$
					for(String b : parts) if (!b.strip().isBlank()) params.add(b);
				}
				for (Path p : queryFiles) {
					params.add(p.toRealPath().toString());
				}
				
				StringBuilder sb = new StringBuilder();
				for (String p : params) sb.append("\"" + p + "\" ");  //$NON-NLS-1$//$NON-NLS-2$
				writeString(Messages.REngine_CommandLabel + sb.toString());
				writeString("---------------------------------------- " + Messages.REngine_ScriptOutput + "----------------------------------------"); //$NON-NLS-1$ //$NON-NLS-2$
				
				//run command
				runCommand(params, outStream);
				
			}catch (Exception ex) {
				try {
					writeString(ERROR_PREFIX + ex.getMessage());
				} catch (IOException e) {
					RPlugIn.log(e.getMessage(), e);
				}
				RPlugIn.log(ex.getMessage(), ex);
			}finally {
				try {
					outStream.close();
				} catch (IOException e) {
					RPlugIn.log(e.getMessage(), e);
				}
			}
			

			return Status.OK_STATUS;
		}
		
	};
	
	private void runCommand(List<String> parameters, OutputStream outStream) {
		try {  
	        Process p = (new ProcessBuilder(parameters)).start();
	        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

	        String s = null;
	        while ((s = stdInput.readLine()) != null) {
	        	writeString(s);
	        }

	        while ((s = stdError.readLine()) != null) {
	        	writeString(ERROR_PREFIX + s);
	        }
	        
	    } catch(Exception e) {  
	    	try {
				writeString(ERROR_PREFIX + e.getMessage());
			} catch (IOException e1) {
				RPlugIn.log(e1.getMessage(), e1);
			}
	    	RPlugIn.log(e.getMessage(), e);
	    }  
	}
}
