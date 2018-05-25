package org.wcs.smart.r.engine;

import java.io.IOException;
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
import org.wcs.smart.util.UuidUtils;

public class REngine {

	private List<QueryConfiguration> queryConfigs;
	private String rParameters;
	
	private OutputStream outStream;
	
	public REngine(List<QueryConfiguration> queryConfigs, String rParameters, OutputStream outStream) {
		this.queryConfigs = queryConfigs;
		this.rParameters = rParameters;
		this.outStream = outStream;
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
						
						query.getQuery().setDateFilter(query.getDateFilter());
						HashMap<String, Object> parameters = new HashMap<>();
						parameters.put(Session.class.getName(), session);
						parameters.put(IProgressMonitor.class.getName(), new NullProgressMonitor());
						
						results = engine.executeQuery(query.getQuery(), parameters);
					}
					
					String filename = UuidUtils.uuidToString(query.getQuery().getUuid()) + "." + System.nanoTime() + "." + query.getQueryExporter().getDefaultExtension();
					
					Path p = SmartContext.INSTANCE.getTempFilestoreLocation().toPath().resolve(filename);
					queryFiles.add(p);
					HashMap<String, Object> parameters = new HashMap<>();
					query.getQueryExporter().export(query.getQuery(), results, p.toFile(), parameters, new NullProgressMonitor());
					writeString("Query Exported to : " + p.toString());
					writeString("----------------------------------------");
				}
				
				StringBuilder command = new StringBuilder();
				command.append("R_EXEC");
				if (rParameters != null) {
					command.append(" ");
					command.append(rParameters);
				}
				for (Path p : queryFiles) {
					command.append(" " );
					command.append("\"");
					command.append(p.toString());
					command.append("\"");
				}
				writeString(command.toString());
				
				//TODO: 
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
}
