package org.wcs.smart.i2.query;

import java.util.Date;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.engine.IntelObservationQueryEngine;

public abstract class RunQueryJob extends Job {

	private IntelRecordObservationQuery query;
	
	private HashMap<String,Object> parameters ;
	
	
	public RunQueryJob(IntelRecordObservationQuery query) {
		super("executing intelligence query: " + query.getName());
		this.query = query;
		parameters = new HashMap<String, Object>();
		
	}

	protected abstract void onError(Exception ex);
	
	protected abstract void onComplete(IPagedQueryResultSet results);
	
	public void setDateFilter(Date[] dateFilter){
		parameters.put(Date.class.getName(), dateFilter);
	}
	
	public void configureParameter(String parameterName, Object value){
		parameters.put(parameterName, value);
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		if (!parameters.containsKey(IProgressMonitor.class.getName())) parameters.put(IProgressMonitor.class.getName(), monitor);
		
		IPagedQueryResultSet results = null;
		Session session = HibernateManager.openSession();
		try{
			parameters.put(Session.class.getName(), session);
			results = (new IntelObservationQueryEngine()).executeQuery(query, parameters);	
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error running query. " + ex.getMessage(), ex);
			onError(ex);
			
			return Status.OK_STATUS;
		}finally{
			session.close();
		}
		onComplete(results);
		return Status.OK_STATUS;
		
	}

}
