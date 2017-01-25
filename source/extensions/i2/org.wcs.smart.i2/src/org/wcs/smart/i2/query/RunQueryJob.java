/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query;

import java.util.Date;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.engine.IntelObservationQueryEngine;
import org.wcs.smart.i2.ui.editors.query.ProgressPanel;
import org.wcs.smart.i2.ui.editors.query.QueryProgressMonitor;

/**
 * Job for running query and displaying progress in the UI.
 * @author Emily
 *
 */
public abstract class RunQueryJob extends Job {

	private IntelRecordObservationQuery query;
	
	private HashMap<String,Object> parameters ;
	
	private ProgressPanel progressPanel;
	
	public RunQueryJob(IntelRecordObservationQuery query) {
		super("executing intelligence query: " + query.getName());
		this.query = query;
		parameters = new HashMap<String, Object>();
		
	}

	protected abstract void onError(Exception ex);
	
	protected abstract void onComplete(IPagedQueryResultSet results);
	
	protected abstract void onCancel();
	
	public void setDateFilter(Date[] dateFilter){
		parameters.put(Date.class.getName(), dateFilter);
	}
	
	public void configureParameter(String parameterName, Object value){
		parameters.put(parameterName, value);
	}
	
	public void setProgresPanel(ProgressPanel panel){
		this.progressPanel = panel;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (progressPanel != null){
			monitor = new QueryProgressMonitor(progressPanel);
			parameters.put(IProgressMonitor.class.getName(), monitor);
		}
		if (!parameters.containsKey(IProgressMonitor.class.getName())) parameters.put(IProgressMonitor.class.getName(), monitor);
		if (!parameters.containsKey(ConservationArea.class.getName())) parameters.put(ConservationArea.class.getName(), SmartDB.getCurrentConservationArea());
		
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
		if (monitor.isCanceled()){
			onCancel();
			return Status.CANCEL_STATUS;
		}
		onComplete(results);
		return Status.OK_STATUS;
		
	}

}
