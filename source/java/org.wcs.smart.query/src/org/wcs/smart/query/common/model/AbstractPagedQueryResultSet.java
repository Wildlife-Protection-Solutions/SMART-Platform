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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.IResultItem;

/**
 * An abstract class containing some common functions for paged query result set.
 * 
 * @author Emily
 *
 */
public abstract class AbstractPagedQueryResultSet implements IPagedQueryResultSet {

	protected int itemCount = 0;

	/**
	 * Gets a set of data
	 * 
	 * @param itOffset
	 * @param pageSize
	 * @return
	 */
	public abstract List<IResultItem> getData(int itOffset, int pageSize);

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

	public Iterator<IResultItem> iterator(int pageSize) {
		return new LazyQueryIterator(pageSize);
	}

	protected MapByteArrayKey wrap(byte[] array) {
		return new MapByteArrayKey(array);
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	/**
	 * This is a wrapper for byte[] so we can use it as HashMap key. The reason
	 * for creating this wrapper is that byte[] do not provide required equals
	 * and hashCode operations.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	protected class MapByteArrayKey {
		private final byte[] data;

		public MapByteArrayKey(byte[] data) {
			this.data = data;
		}

		@Override
		public boolean equals(Object arg) {
			if (arg instanceof MapByteArrayKey)
				return Arrays.equals(data, ((MapByteArrayKey) arg).data);
			return super.equals(arg);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(data);
		}
	}
	
	/**
	 * Iterator that uses lazy approach
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class LazyQueryIterator implements Iterator<IResultItem> {

		private int itOffset = -1; // offset of element at which list begins
		private int itIndex = 0;
		private List<IResultItem> data;
		private int pageSize = 0;

		public LazyQueryIterator(int pageSize) {
			this.pageSize = pageSize;
		}

		@Override
		public boolean hasNext() {
			return itOffset + itIndex + 1 < itemCount;
		}

		@Override
		public IResultItem next() {
			if (!hasNext())
				throw new NoSuchElementException();
			if (data == null) {
				itOffset = 0;
				itIndex = 0;
				data = getData(itOffset, pageSize);
				return data.get(itIndex);
			}
			itIndex++;
			if (itIndex < data.size()) {
				return data.get(itIndex);
			}
			// we need to load new portion of data
			itOffset += data.size();
			itIndex = 0;
			data = getData(itOffset, pageSize);
			return data.get(itIndex);
		}

		@Override
		public void remove() {
			throw new IllegalStateException(
					"Remove operation is not supported."); //$NON-NLS-1$
		}

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