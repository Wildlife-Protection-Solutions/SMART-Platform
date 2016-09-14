package org.wcs.smart.i2.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.search.BasicSearch;
import org.wcs.smart.i2.search.IIntelSearch;

public abstract class EntitySearchJob extends Job{

	private IIntelSearch search = new BasicSearch(null);
	
	public EntitySearchJob() {
		super("Entity Search");
	}

	public void setSearch(IIntelSearch search){
		this.search = search;
	}
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		beforeSearch();
		
		List<IntelEntity> entities = null; 
		Session s = HibernateManager.openSession();
		try{
			entities = search.doSearch(s);
		}catch (Exception ex){
			Intelligence2PlugIn.log(ex.getMessage(), ex);
			onError(ex);
			return Status.OK_STATUS;
		}finally{
			s.close();
		}
		
		afterSearch(entities);
		
		return Status.OK_STATUS;
	}

	
	public abstract void onError(Exception ex);
	
	public abstract void afterSearch(List<IntelEntity> entities);
	
	public abstract void beforeSearch();
}
