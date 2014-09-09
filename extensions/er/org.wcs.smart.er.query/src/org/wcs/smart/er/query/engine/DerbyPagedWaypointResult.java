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
package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartWorkbenchWindowAdvisor;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Envelope;

public class DerbyPagedWaypointResult implements IPagedQueryResultSet, ISurveyQueryMissionResult{


	private String queryTempTable;

	private int itemCount = 0;

	private ResultSet lastResultSet;
	
	private Envelope bounds = null;

	//next sort column
	private QueryColumn sortColumn = null;
	//current direction
	private int direction = SWT.UP;

	private DerbyWaypointEngine engine;
	
	public DerbyPagedWaypointResult(String queryTempTable,DerbyWaypointEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}

	public DerbyPagedWaypointResult(String queryTempTable, int itemCount, DerbyWaypointEngine engine) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.engine = engine;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DerbyPagedWaypointResult) {
			if (queryTempTable == null)
				return super.equals(obj);
			DerbyPagedWaypointResult r2 = (DerbyPagedWaypointResult) obj;
			return queryTempTable.equals(r2.queryTempTable);
		}
		return super.equals(obj);
	}
	
	public void destroy() {
		//simply closing result set and deleting temporary table
		dropResultSet();
		Job cleanUpJob = new CleanUpJob();
		cleanUpJob.setSystem(true); //we don't want this job to be displayed to user
		cleanUpJob.schedule();
	}
	
	public List<SurveyQueryResultItem> getData(final int offset, final int pageSize) {
		final Session session = HibernateManager.openSession();
		//NOTE: session will not be closed on purpose!!!!
		//as we want related ResultSet to remain opened for performance reasons
		List<SurveyQueryResultItem> result = getNextData(session, offset, pageSize);
		if (result == null) {
			result = getData(session, offset, pageSize);
		}
		return result;
	}
	
	private List<SurveyQueryResultItem> getNextData(final Session session, final int offset, final int pageSize) {
		if (lastResultSet == null)
			return null;
		final List<SurveyQueryResultItem> result = new ArrayList<SurveyQueryResultItem>();
		try {
			result.addAll(getResults(lastResultSet, offset, pageSize));
		} catch (SQLException e) {
			//most likely someone closed our old session/connection and old ResultSet is not working
			lastResultSet = null;
			return null;
		}
		return result;
	}
	
	
	@Override
	public Envelope getEnvelope(){
		if (this.bounds == null){
			Session s = HibernateManager.openSession();
			final String sql = "SELECT min(wp_x), max(wp_x), min(wp_y), max(wp_y) FROM " + queryTempTable; //$NON-NLS-1$
			s.doWork(new Work(){
				@Override
				public void execute(Connection c) throws SQLException {
					ResultSet q = c.createStatement().executeQuery(sql);
					try{
						q.next();
						double minx = q.getDouble(1);
						double maxx = q.getDouble(2);
						double miny = q.getDouble(3);
						double maxy = q.getDouble(4);
						bounds = new Envelope(minx, maxx, miny, maxy);
					}finally{
						q.close();
					}
				}
			});
			
		}
		return bounds;
		
	}
	
	private List<SurveyQueryResultItem> getData(final Session session, final int offset, final int pageSize) {
		final List<SurveyQueryResultItem> result = new ArrayList<SurveyQueryResultItem>();
		final String dataSql = "SELECT r.* FROM " + queryTempTable + " r "+ buildSortSql();  //$NON-NLS-1$ //$NON-NLS-2$
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				lastResultSet = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);
				//this forces garbage collection; without this the program
				//will fail with out of memory error when sorting
				//on columns multiple times.
				System.gc();
				
				result.addAll(getResults(lastResultSet, offset, pageSize));
			}
		});
		return result;
	}

	
	private String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
//		if (sortColumn instanceof FixedQueryColumn) {
//			String key = sortColumn.getKey();
//			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
//			for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
//				key = key.replace(data[0], data[1]);
//			}
//			if (sortColumn.getType() == ColumnType.STRING){
//				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
//			}else{
//				result = "order by r."+key; //$NON-NLS-1$
//			}
//		}
		
		if (!result.isEmpty()) {
			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.model.IPagedQueryResultSet#setSorting(org.wcs.smart.query.model.observation.QueryColumn, int)
	 */
	public void setSorting(final QueryColumn sortColumn, int direction) {
		this.sortColumn = sortColumn;
		this.direction = direction;
		dropResultSet();
	}

	private void dropResultSet() {
		if (lastResultSet != null) {
			try {
				if (!lastResultSet.isClosed()){
					lastResultSet.getStatement().close();
					lastResultSet.close();
				}
			} catch (SQLException e) {
				//nothing
				e.printStackTrace();
			}
			lastResultSet = null;
		}
	}
	
	protected List<SurveyQueryResultItem> getResults(ResultSet rs, int from, int pageSize) throws SQLException {
		List<SurveyQueryResultItem> items = new ArrayList<SurveyQueryResultItem>();
		
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			SurveyQueryResultItem it = engine.asQueryResultItem(rs, null);
			items.add(it);
		}
		return items;
	}

	public Iterator<SurveyQueryResultItem> iterator(int pageSize) {
		return new LazyQueryIterator(pageSize);
	}
	
	public int getItemCount() {
		return itemCount;
	}

	protected void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}


	
	/**
	 * Iterator that uses lazy approach
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class LazyQueryIterator implements Iterator<SurveyQueryResultItem> {
		
		private int itOffset = -1; //offset of element at which list begins
		private int itIndex = 0;
		private List<SurveyQueryResultItem> data;
		private int pageSize = 0;

		public LazyQueryIterator(int pageSize){
			this.pageSize = pageSize;
		}
		
		@Override
		public boolean hasNext() {
			return itOffset + itIndex + 1 < itemCount;
		}

		@Override
		public SurveyQueryResultItem next() {
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
			//we need to load new portion of data
			itOffset += data.size();
			itIndex = 0;
			data = getData(itOffset, pageSize);
			return data.get(itIndex);
		}

		@Override
		public void remove() {
			throw new IllegalStateException("Remove operation is not supported."); //$NON-NLS-1$
		}
		
	}
	
	private class CleanUpJob extends Job {

		public CleanUpJob() {
			super("Clearing temporary tables");
		}

		
		@Override
		public boolean belongsTo(Object family){
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
						//original table
						try {
							String sql = "DROP TABLE " + queryTempTable; //$NON-NLS-1$
							c.createStatement().execute(sql);
							QueryPlugIn.logSql(sql);
						} catch (Exception ex) {
							// eatme
							ex.printStackTrace();
						}
					}
				});
			}catch (Exception ex){
				ERQueryPlugIn.log("Failed to cleanup temp query tables", ex); //$NON-NLS-1$
			} finally {
				try{
					session.getTransaction().commit();
				}catch (Exception ex){
					SmartPlugIn.log(ex.getMessage(), ex);
				}
				try{
					session.close();
				}catch (Exception ex){
					SmartPlugIn.log(ex.getMessage(), ex);
				}
			}
			return Status.OK_STATUS;
		}
		
	}

	/**
	 * Mission uuids associated with query results
	 */
	@Override
	public List<byte[]> getMissionUuids() {
		final Session session = HibernateManager.openSession();
		final List<byte[]> uuids = new ArrayList<byte[]>();
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				String sql = "SELECT distinct mission_uuid FROM " + queryTempTable; //$NON-NLS-1$
				ResultSet rs = c.createStatement().executeQuery(sql);
				while(rs.next()){
					uuids.add(rs.getBytes(1));
				}
				rs.close();
			}});
		
		return uuids;
	}
}
