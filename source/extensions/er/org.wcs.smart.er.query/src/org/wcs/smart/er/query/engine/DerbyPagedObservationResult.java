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
import java.util.Arrays;
import java.util.HashMap;
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
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Envelope;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyPagedObservationResult implements IObservationPagedQueryResultSet{
	
	private static String[][] FIXED_COLUMN_KEY_TO_ROW  = {
		 //NOTE: order is important as we don't want to change "patrolleg" to "pleg"
		{"patrolleg", "pl"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"patrol", "p"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"waypoint", "wp"} //$NON-NLS-1$ //$NON-NLS-2$
	};
	
	private String queryTempTable;

	private int itemCount = 0;
	private int wpCount = 0;

	private ResultSet lastResultSet;
	
	private Envelope bounds = null;

	//next sort column
	private QueryColumn sortColumn = null;
	//last sort column
	private QueryColumn lastSortColumn = null;
	//current direction
	private int direction = SWT.UP;
	private boolean hasSortColumns = false;
	private DerbySurveyQueryEngine engine;

	
	public DerbyPagedObservationResult(String queryTempTable, DerbySurveyQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}

	public DerbyPagedObservationResult(String queryTempTable, int itemCount, int wpCount, DerbySurveyQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.wpCount = wpCount;
		this.engine = engine;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DerbyPagedObservationResult) {
			if (queryTempTable == null)
				return super.equals(obj);
			DerbyPagedObservationResult r2 = (DerbyPagedObservationResult) obj;
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
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				attachObservations(result, c, session);
			}
		});
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
	
	private void updateSortColumn(QueryColumn sortColumn, Session session, Connection c) throws SQLException{
		//TODO:
//		if (sortColumn instanceof PatrolAttributeQueryColumn){
//			if (!hasSortColumns){
//				//add the sort columns
//				c.createStatement().execute("ALTER TABLE " + queryTempTable + " add column sortKeyDbl double"); //$NON-NLS-1$ //$NON-NLS-2$
//				c.createStatement().execute("ALTER TABLE " + queryTempTable + " add column sortKeyTxt varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
//				c.commit();
//				hasSortColumns = true;
//			}
//			String key = sortColumn.getKey();
//			key = key.split(":")[1]; //$NON-NLS-1$
//			Attribute attribute = null;
//			
//			session.beginTransaction();
//			try{
//				attribute = QueryDataModelManager.getInstance().getAttribute(session, key); //session will not be closed on purpose
//			}finally{
//				session.getTransaction().rollback();
//			}
//			
//			switch (attribute.getType()) {
//			case BOOLEAN:
//			case NUMERIC:
//				// nullify first
//				StringBuilder sql = new StringBuilder();
//				sql.append("UPDATE "); //$NON-NLS-1$
//				sql.append(queryTempTable);
//				sql.append(" SET sortKeyDbl = null "); //$NON-NLS-1$
//				c.createStatement().execute(sql.toString());
//				break;
//			case TEXT:
//			case LIST:
//			case TREE:
//			case DATE:
//				sql = new StringBuilder();
//				sql.append("UPDATE "); //$NON-NLS-1$
//				sql.append(queryTempTable);
//				sql.append(" SET sortKeyTxt = null"); //$NON-NLS-1$
//				c.createStatement().execute(sql.toString());
//				break;
//			}
//			
//			switch (attribute.getType()) {
//			case BOOLEAN:
//			case NUMERIC:
//				StringBuilder sql = new StringBuilder();
//				sql.append("UPDATE "); //$NON-NLS-1$
//				sql.append(queryTempTable);
//				sql.append(" SET sortKeyDbl = "); //$NON-NLS-1$
//				sql.append("(SELECT wpoa.NUMBER_VALUE FROM "); //$NON-NLS-1$
//				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
//				sql.append("and a.keyid = '"); //$NON-NLS-1$
//				sql.append(key);
//				sql.append("'"); //$NON-NLS-1$
//				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
//				sql.append( queryTempTable );
//				sql.append(".ob_uuid)"); //$NON-NLS-1$
//				c.createStatement().execute(sql.toString());
//				break;
//			case TEXT:
//			case DATE:
//				sql = new StringBuilder();
//				sql.append("UPDATE "); //$NON-NLS-1$
//				sql.append(queryTempTable);
//				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
//				sql.append("(SELECT wpoa.STRING_VALUE FROM "); //$NON-NLS-1$
//				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
//				sql.append("and a.keyid = '"); //$NON-NLS-1$
//				sql.append( key );
//				sql.append("'"); //$NON-NLS-1$
//				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
//				sql.append( queryTempTable );
//				sql.append(".ob_uuid)"); //$NON-NLS-1$
//				c.createStatement().execute(sql.toString());
//				break;
//			case LIST:
//				sql = new StringBuilder();
//				sql.append("UPDATE "); //$NON-NLS-1$
//				sql.append(queryTempTable);
//				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
//				sql.append("(SELECT rl.value FROM "); //$NON-NLS-1$
//				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
//				sql.append( queryTempTable );
//				sql.append( "_LIST rl on rl.uuid = wpoa.list_element_uuid "); //$NON-NLS-1$
//				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
//				sql.append( key );
//				sql.append("'"); //$NON-NLS-1$
//				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
//				sql.append( queryTempTable); 
//				sql.append(".ob_uuid)"); //$NON-NLS-1$
//				c.createStatement().execute(sql.toString());
//				
//				break;
//			case TREE:
//				sql = new StringBuilder();
//				sql.append("UPDATE ");//$NON-NLS-1$
//				sql.append(queryTempTable);
//				sql.append(" SET sortKeyTxt = ");//$NON-NLS-1$
//				sql.append("(SELECT rl.value FROM smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
//				sql.append( queryTempTable );
//				sql.append("_TREE rl on rl.uuid = wpoa.tree_node_uuid "); //$NON-NLS-1$
//				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
//				sql.append( key );
//				sql.append("'"); //$NON-NLS-1$
//				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
//				sql.append( queryTempTable );
//				sql.append( ".ob_uuid)"); //$NON-NLS-1$
//				c.createStatement().execute(sql.toString());
//				
//				break;
//			}
//		}
//		c.commit();
	}
		
		
	
	private List<SurveyQueryResultItem> getData(final Session session, final int offset, final int pageSize) {
		final List<SurveyQueryResultItem> result = new ArrayList<SurveyQueryResultItem>();
		final String dataSql = "SELECT r.* FROM " + queryTempTable + " r "+ buildSortSql();  //$NON-NLS-1$ //$NON-NLS-2$
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				if ((lastSortColumn == null && sortColumn != null) 
						|| (lastSortColumn != null && sortColumn != null && !lastSortColumn.equals(sortColumn)) ){
					updateSortColumn(sortColumn, session, c);
				}
				lastResultSet = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);
				//this forces garbage collection; without this the program
				//will fail with out of memory error when sorting
				//on columns multiple times.
				System.gc();
				
				result.addAll(getResults(lastResultSet, offset, pageSize));
				attachObservations(result, c, session);
			}
		});
		return result;
	}

	private void attachObservations(List<SurveyQueryResultItem> result, Connection c, Session session) throws SQLException {
		boolean hasObservations = false;
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.p_ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(queryTempTable);
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid in ("); //$NON-NLS-1$
		for (SurveyQueryResultItem it : result) {
			if (it.getObservationUuid() != null) {
				if (hasObservations) {
					attrSql.append(',');
				}
				hasObservations = true;
				attrSql.append("x'").append(SmartUtils.encodeHex(it.getObservationUuid())).append('\''); //$NON-NLS-1$
			}
		}
		
		
		if (!hasObservations) {
			//no observations in current data fragment, so no need to select attributes as they will be empty
			return;
		}
		attrSql.append(')');

		ResultSet rs = c.createStatement().executeQuery(attrSql.toString());
		try {
			HashMap<MapByteArrayKey, HashMap<String, Object>> attrMap = getResultsAttributes(rs, session);
			for (SurveyQueryResultItem it : result) {
				if (it.getObservationUuid() != null) {
					HashMap<String, Object> attributes = attrMap.get(wrap(it.getObservationUuid()));
					if (attributes != null) {
						it.setAttributes(attributes);
					}
				}
			}
		} finally {
			rs.close();
		}		
	}
	
	private String buildSortSql() {
		return "";
//		if (sortColumn == null || direction == SWT.NONE)
//			return ""; //$NON-NLS-1$
//		
//		String result = ""; //$NON-NLS-1$
//		if (sortColumn instanceof FixedQueryColumn) {
//			String key = sortColumn.getKey();
//			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
//			for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
//				key = key.replace(data[0], data[1]);
//			}
//			if (sortColumn.getKey().equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
//				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
//			}else if (sortColumn.getType() == ColumnType.STRING){
//				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
//			}else{
//				result = "order by r."+key; //$NON-NLS-1$
//			}
//		}
//		if (sortColumn instanceof PatrolCategoryQueryColumn) {
//			String key = sortColumn.getKey();
//			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
//			result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		if (sortColumn instanceof PatrolAttributeQueryColumn) {
//			String key = sortColumn.getKey();
//			key = key.split(":")[1]; //$NON-NLS-1$
//			switch (sortColumn.getType()) {
//				case BOOLEAN:
//				case NUMBER:
//				case INTEGER:
//					result = "order by sortKeyDbl"; //$NON-NLS-1$
//					break;
//				case DATE:
//					result = "order by DATE(sortKeyTxt)"; //$NON-NLS-1$
//					break;
//				default:
//					result = "order by UPPER(sortKeyTxt)"; //$NON-NLS-1$
//					break;
//			}
//		}
//		if (!result.isEmpty()) {
//			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.model.IPagedQueryResultSet#setSorting(org.wcs.smart.query.model.observation.QueryColumn, int)
	 */
	public void setSorting(final QueryColumn sortColumn, int direction) {
		this.lastSortColumn = this.sortColumn;
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

	protected HashMap<MapByteArrayKey, HashMap<String, Object>> getResultsAttributes(ResultSet rs, Session s) throws SQLException {
		HashMap<MapByteArrayKey, HashMap<String, Object>> attrMap = new HashMap<MapByteArrayKey, HashMap<String, Object>>();
		/*
		1	OB_UUID
		2	KEYID
		3	NUMBER_VALUE
		4	STRING_VALUE
		5	LIST_VALUE
		6	TREE_VALUE
		7	P_CA_UUID
		*/
		while (rs.next()) {
			byte[] obUuid = rs.getBytes(1);
			if (obUuid == null)
				continue;
			MapByteArrayKey keyObj = wrap(obUuid);
			HashMap<String, Object> attributes = attrMap.get(keyObj);
			if (attributes == null) {
				attributes = new HashMap<String, Object>();
				attrMap.put(keyObj, attributes);
			}
			String key = rs.getString(2);
			if (key != null) {
				Object value = getAttributeValue(rs, s);
				attributes.put(key, value);
			}
		}
		return attrMap;
	}

	/**
	 * Gets the attribute value from the result set for the given attribute.
	 * 
	 * @param att
	 * @param rs
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	protected Object getAttributeValue(ResultSet rs, Session session) throws SQLException {
		/*
		1	OB_UUID
		2	KEYID
		3	NUMBER_VALUE
		4	STRING_VALUE
		5	LIST_VALUE
		6	TREE_VALUE
		7	P_CA_UUID
		*/
		if (rs.getObject(3) != null) {
			return rs.getDouble(3);
		}
		String result = rs.getString(4); //string
		if (result != null) {
			return result;
		}
		result = rs.getString(5); //list
		if (result != null) {
			return result;
		}
		result = rs.getString(6); //tree
		if (result != null) {
			return result;
		}
		return null;
	}

	public Iterator<SurveyQueryResultItem> iterator(int pageSize) {
		return new LazyQueryIterator(pageSize);
	}
	
	private MapByteArrayKey wrap(byte[] array) {
		return new MapByteArrayKey(array);
	}

	public int getItemCount() {
		return itemCount;
	}

	protected void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public int getWpCount() {
		return wpCount;
	}

	protected void setWpCount(int wpCount) {
		this.wpCount = wpCount;
	}

	/**
	 * This is a wrapper for byte[] so we can use it as HashMap key.
	 * The reason for creating this wrapper is that byte[] do not provide required equals and hashCode operations.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class MapByteArrayKey {
		private final byte[] data;
		
		public MapByteArrayKey(byte[] data) {
			this.data = data;
		}

		@Override
		public boolean equals(Object arg) {
			if (arg instanceof MapByteArrayKey)
				return Arrays.equals(data, ((MapByteArrayKey)arg).data);
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
			super("Clean up query tables.");
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
						//list elements value table
						try {
							String sql = "DROP TABLE " + queryTempTable + "_LIST"; //$NON-NLS-1$ //$NON-NLS-2$
							c.createStatement().execute(sql);
							QueryPlugIn.logSql(sql);
						} catch (Exception ex) {
							// eatme
							ex.printStackTrace();
						}
						//tree elements value table
						try {
							String sql = "DROP TABLE " + queryTempTable + "_TREE"; //$NON-NLS-1$ //$NON-NLS-2$
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
}
