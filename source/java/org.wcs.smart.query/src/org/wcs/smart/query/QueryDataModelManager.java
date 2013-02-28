package org.wcs.smart.query;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

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
/**
 * Class responsible for loading the SMART data model
 * to support querying.
 * 
 * @author Emily
 *
 */
public class QueryDataModelManager {

	private DataModel dm = null;
	
	private static QueryDataModelManager instance = null;
	
	public static QueryDataModelManager getInstance(){
		if (instance == null){
			instance = new QueryDataModelManager();
		}
		return instance;
	}
	
	/**
	 * Clears the current data model
	 */
	public void clearDataModel(){
		dm = null;
	}
	
	/**
	 * In the case of single conservation area querying this will
	 * return the data model of the current conservation area.  Otherwise
	 * this will return the merged data model of all the existing conservation
	 * areas.
	 * <p>This will block until the data model is loaded</p>
	 * @return the data model for querying
	 */
	public DataModel getDataModel(){
		if (dm == null){
			Job job = getDataModelJob();
			synchronized (instance) {
				if (job.getState() == Job.NONE || job.getState() == Job.SLEEPING){
					job.schedule();
				}
			}
			
			try{
				//wait for the current job to finish
				job.join();
			}catch (Exception ex){
				QueryPlugIn.log(ex.getMessage(), ex);
			}
		}
		return this.dm;
	}
	
	private Job getDataModelJob(){
		if (SmartDB.isMultipleAnalysis()){
			return loadAndMergeDataModelJob;
		}else{
			return loadDataModelJob;
		}
	}
	
	private Job loadAndMergeDataModelJob = new Job("Load And Merge Data Models"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				DataModelMerger merger = new DataModelMerger();
				dm = merger.mergeDataModels(SmartDB.getSelectedConservationAreas().toArray(new ConservationArea[SmartDB.getSelectedConservationAreas().size()]), session);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			return Status.OK_STATUS;
		}
	};
	
	private Job loadDataModelJob = new Job("Load Data Model"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
				//load into memory; no-lazy loading here.
				for (Category cat: dm.getCategories()){
					visitCategory(cat);
				}
				for (Attribute att: dm.getAttributes()){
					att.getAggregations().size();
				}
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
		
			return Status.OK_STATUS;
		}
	
		/**
		 * visits a child and gets all attributes.
		 * 	<p>This is to ensure all data model elements
		 * are loaded in the hibernate session.  Circumvents
		 * the hibernate lazy-loading.</p>
		 * @param cat
		 */
		private void visitCategory(Category cat){
			for (Category child : cat.getActiveChildren()){
				visitCategory(child);
				child.getName();
			}
			for (CategoryAttribute ca: cat.getAttributes()){
				ca.getAttribute().getName();
			}	
		}
	
	};
}
