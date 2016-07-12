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
package org.wcs.smart.query.compound.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IService;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.common.model.CompoundMapQueryResults;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Run compound query job.  This job should be reused 
 * when the compound query is run multiple times.  It tracks
 * the query map layers and removes/updates as required. 
 * 
 * @author Emily
 *
 */
public class RunCompoundQueryJob extends Job{

	private CompoundQueryEditor editor;
	
	private MapLayerTracker mapLayerTracker = new MapLayerTracker();
	
	public RunCompoundQueryJob(CompoundQueryEditor editor){
		super(Messages.QueryResultsEditor_RunQueryJobName);
		this.editor = editor;
	}
	
	/**
	 * Disposes of all services added to map as part of running queries.
	 */
	public void dispose(){
		for (IQueryService service : mapLayerTracker.getServices()){
			CatalogPlugin.getDefault().getLocalCatalog().remove((IService)service);
			((IService)service).dispose(null);
		}
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		CompoundMapQuery query = (CompoundMapQuery) editor.getQueryProxy().getQuery();
		
		//TODO thread safe results
		CompoundMapQueryResults results = (CompoundMapQueryResults) query.getCachedResults();
		if (results == null || results.isDisposed()){
			results = new CompoundMapQueryResults();
			query.setCachedResults(results);
		}else{
			Session session  = HibernateManager.openSession();
			try{
				results.clear(session);
			}catch (Exception ex){
				ex.printStackTrace();
			}finally{
				session.close();
			}
		}

		setName(Messages.QueryResultsEditor_RunQueryJobName + query.getName());
			
		//load the current view projection
		Projection currentPrj = HibernateManager.getCurrentViewProjection();
		if (currentPrj != null && currentPrj.getParsedCoordinateReferenceSystem() == null){
			try{
				currentPrj.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(currentPrj.getDefinition()));
			}catch (Exception ex){
				//eat me
			}
		}
		editor.setProjection(currentPrj);

		List<QueryItem> items = new ArrayList<QueryItem>();
		
		Session s = HibernateManager.openSession();
		try{
			for (CompoundMapQueryLayer layer : query.getLayers()){
				IQueryType type =  QueryTypeManager.INSTANCE.findQueryType(layer.getQueryType());
				//always reload query here so we have the latest query definition
				Query q = QueryHibernateManager.getInstance().findQuery(s, layer.getQueryUuid(),type);
				
				//we want the ability to run with different date filters
				//so we need to clone the query here
				Query clone = q.clone(null);
				clone.setId(q.getId());
				clone.setUuid(q.getUuid());
				if (q != null){
					try{

						QueryItem qi = new QueryItem(layer, clone, type);
						qi.setStatus(QueryItem.Status.PROCESSING);
						items.add(qi);
					}catch (Exception ex){
						QueryPlugIn.displayLog(ex.getMessage(), ex);
					}
				}
			}
		}finally{
			s.close();
		}
		
		//these old query layers and services 
		//that need to be removed from the map
		//we don't reuse because the query object may have be updated
		//with a new definition when reloaded previously
		mapLayerTracker.clearAll(editor.getMap(), monitor);
		
		//update table
		editor.page1.setupTable(items);
		
		//create jobs
		for (final QueryItem i : items){
			RunCompoundQueryLayerJob job = new RunCompoundQueryLayerJob(i, editor, mapLayerTracker);
			job.schedule();
		}
		
		return Status.OK_STATUS;
		
	}
}