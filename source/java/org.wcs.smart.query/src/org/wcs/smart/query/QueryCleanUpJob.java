package org.wcs.smart.query;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.internal.Messages;

public class QueryCleanUpJob extends Job{

	public QueryCleanUpJob() {
		super(Messages.QueryPlugIn_QueryCleanUpJobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//clean up queries directory
		File dir = QueryPlugIn.getDefault().getQueryTempDirectory();
		if (dir.exists() && dir.isDirectory()){
			File[] toDel = dir.listFiles();
			for (int i = 0; i < toDel.length; i ++){
				toDel[i].delete();
			}
		}
		
		//cleanup query tables
		
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			SQLQuery q = session.createSQLQuery("CALL smart.cleanUpTempData()"); //$NON-NLS-1$
			q.executeUpdate();
			session.getTransaction().commit();
		}catch (Exception ex){
			QueryPlugIn.log("Could not cleanup query temporary tables.", ex); //$NON-NLS-1$
		}finally{
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.close();
		}
		return Status.OK_STATUS;
	}

}
