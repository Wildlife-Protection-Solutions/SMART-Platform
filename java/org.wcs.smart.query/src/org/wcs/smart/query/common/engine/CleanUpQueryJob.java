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
package org.wcs.smart.query.common.engine;

import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job of cleaning up query results.  Opens db connection,
 * cleans up tables, then closes connection.
 * 
 * @author Emily
 *
 */
public class CleanUpQueryJob extends Job{

	public static void schedule(IQueryResult results){
		CleanUpQueryJob job = new CleanUpQueryJob(results);
		job.setSystem(true);
		job.schedule();
	}
	
	private IQueryResult results;
	
	public CleanUpQueryJob(IQueryResult results){
		super("Remove Query"); //$NON-NLS-1$
		this.results = results;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			results.dispose(s);
			s.getTransaction().commit();
		}catch(SQLException ex){
			ex.printStackTrace();
			s.getTransaction().rollback();
		}finally{
			s.close();
		}
		return Status.OK_STATUS;
	}
}
