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
package org.wcs.smart.query.common.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartWorkbenchWindowAdvisor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.QueryResultSetIterator;
import org.wcs.smart.query.internal.Messages;

/**
 * An abstract class containing some common functions for paged query result set.
 * 
 * @author Emily
 *
 */
public abstract class AbstractPagedQueryResultSet implements IPagedQueryResultSet {

	protected int itemCount = 0;

	
	/**
	 * Opens a session, creates a result set and loads the data from 
	 * the given offset, pagesize.
	 */
	@Override
	public List<IResultItem> getData(final int offset, final int pageSize) {
		List<IResultItem> result = null;
		
		Session session = HibernateManager.openSession();
		try{
			try(ResultSet rs = getResultSet(session)){
				result = getResults(session, rs, offset, pageSize);
			}
		}catch (SQLException ex){
			QueryPlugIn.displayLog("Error loading query results from database.", ex);
		}finally{
			session.close();
		}
		return result;
	}

	
	/**
	 * Gets results from the given result get.
	 * 
	 * @param rs
	 * @param from
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	public abstract List<IResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException;
	
	/**
	 * Opens a result set in the given session that accessed the query results
	 */
	public abstract ResultSet getResultSet(Session session);

	/**
	 * Get all temporary tables used to support the result set
	 * 
	 * @return
	 */
	public abstract String[] getTemporaryTableNames();

	/**
	 * destroys the result set and removes any temporary tables
	 */
	public void destroy() {
		// simply closing result set and deleting temporary table
		Job cleanUpJob = new CleanUpJob();
		
		// we don't want this job to be displayed to user
		cleanUpJob.setSystem(true); 
		cleanUpJob.schedule();
	}

	/**
	 * Creates a new feature iterator that intrates over all
	 * features in the result set using the given page size.  This
	 * iterator MUST BE CLOSED when you are done with it to properly
	 * close the session.
	 */
	public QueryResultSetIterator<IResultItem> iterator(int pageSize) {
		return new QueryResultSetIterator<IResultItem>(this, pageSize);
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}


	private class CleanUpJob extends Job {

		public CleanUpJob() {
			super(Messages.AbstractPagedQueryResultSet_CleanupTablesJobName);
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == SmartWorkbenchWindowAdvisor.SHUTDOWN_JOB_FAMILY;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final Session session = HibernateManager.openSession();
			session.beginTransaction();
			try {
				session.doWork(new Work() {
					@Override
					public void execute(Connection c) throws SQLException {
						// original table
						for (String tableName: getTemporaryTableNames()){
							try {
								String sql = "DROP TABLE " + tableName; //$NON-NLS-1$
								QueryPlugIn.logSql(sql);
								c.createStatement().execute(sql);
							} catch (Exception ex) {
								// eatme
								ex.printStackTrace();
							}
						}
					}
				});
			} catch (Exception ex) {
				QueryPlugIn.log("Failed to cleanup temp query tables", ex); //$NON-NLS-1$
			} finally {

				try {
					session.getTransaction().commit();
				} catch (Exception ex) {
					SmartPlugIn.log(ex.getMessage(), ex);
				}
				try {
					session.close();
				} catch (Exception ex) {
					SmartPlugIn.log(ex.getMessage(), ex);
				}
			}
			return Status.OK_STATUS;
		}
	}

}