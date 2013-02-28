package org.wcs.smart.query.ui.queryfilter;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;

public class QueryDataModelManager {

	private DataModel dm = null;
	
	private static QueryDataModelManager instance = null;
	
	public static QueryDataModelManager getInstance(){
		if (instance == null){
			instance = new QueryDataModelManager();
		}
		return instance;
	}
	
	
	public void clearDataModel(){
		dm = null;
	}
	
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
	private Job loadAndMergeDataModelJob = new Job("LoadAndMergeDataModels"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				DataModelMerger merger = new DataModelMerger();
				dm = merger.mergeDataModels(SmartDB.getSelectedConservationAreas().toArray(new ConservationArea[SmartDB.getSelectedConservationAreas().size()]), session);
			}finally{
				System.out.println(session.isConnected() );
				System.out.println(session.isOpen() );
				session.getTransaction().rollback();
				session.close();
			}
			return Status.OK_STATUS;
		}
	};
	
	private Job loadDataModelJob = new Job("LoadDataModel"){
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
