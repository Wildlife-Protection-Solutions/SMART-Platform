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

/**
 * Cleans up query temprary tables from the database.
 * 
 * @author Emily
 *
 */
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
