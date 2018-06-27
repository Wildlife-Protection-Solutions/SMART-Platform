package org.wcs.smart.r.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.RScriptManager;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.r.ui.RPreferencePage;
import org.wcs.smart.util.UuidUtils;

public class REngine {

	private List<QueryConfiguration> queryConfigs;
	private String rParameters;
	
	private OutputStream outStream;
	private RScript script;
	
	public REngine(RScript script, List<QueryConfiguration> queryConfigs, String rParameters, OutputStream outStream) {
		this.queryConfigs = queryConfigs;
		this.rParameters = rParameters;
		this.outStream = outStream;
		this.script = script;
	}
	
	private void writeString(String string) throws IOException {
		if (outStream == null) return;
		outStream.write(string.getBytes());
		outStream.write("\n".getBytes());
	}
	
	public void execute()  {
		executeJob.schedule();
		
	}
	
	private Job executeJob = new Job("executing R script") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				List<Path> queryFiles = new ArrayList<>();
				for (QueryConfiguration query : queryConfigs) {
					writeString("Executing Query: " + query.getQuery().getName());
					writeString("Date Filter: " + query.getDateFilter().asString());
					writeString("Format: " + query.getQueryExporter().getDefaultExtension());
					
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
					
					String filename = UuidUtils.uuidToString(query.getQuery().getUuid()) + "." + System.nanoTime() + "." + query.getQueryExporter().getDefaultExtension();
					
					Path p = SmartContext.INSTANCE.getTempFilestoreLocation().toPath().resolve(filename).normalize();
					queryFiles.add(p);
					HashMap<String, Object> parameters = new HashMap<>();
					
					//TODO: delete temporary query files after run
					query.getQueryExporter().export(query.getQuery(), results, p.toFile(), parameters, new NullProgressMonitor());
					writeString("Query Exported to : " + p.toString());
					writeString("-----------------------------------------------------------------------------------------------");
				}
				
				StringBuilder sb = new StringBuilder();
				sb.append("\"");
				sb.append(RPreferencePage.getRSystemProperty());
				sb.append("\" \"");
				sb.append(RScriptManager.INSTANCE.getScriptPath(script).toAbsolutePath().toString());
				sb.append("\"");
				if (rParameters != null) {
					sb.append(" ");
					sb.append(rParameters);
					sb.append(" ");
				}
				for (Path p : queryFiles) {
					sb.append(" \"");
					sb.append(p.toString());
					sb.append("\"");
				}
				
				writeString("Command: " + sb.toString());
				writeString("---------------------------------------- SCRIPT OUTPUT ----------------------------------------");
				
				//TODO: 
				//run command
				runCommand(sb.toString(), outStream);
				
			}catch (Exception ex) {
				try {
					writeString("ERROR: " + ex.getMessage());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					RPlugIn.displayLog(ex.getMessage(), ex);
				}
				RPlugIn.displayLog(ex.getMessage(), ex);
			}finally {
				try {
					outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return Status.OK_STATUS;
		}
		
	};
	
	private void runCommand(String command, OutputStream outStream) {
		try {  
	        Process p = Runtime.getRuntime().exec(command);
	        
	        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

	        String s = null;
	        while ((s = stdInput.readLine()) != null) {
	        	writeString(s);
	        }

	        while ((s = stdError.readLine()) != null) {
	        	writeString("ERROR: " + s);
	        	
	        }
	        
	    } catch(Exception e) {  
	        System.out.println(e.toString());  
	        e.printStackTrace();  
	    }  
	}
}
