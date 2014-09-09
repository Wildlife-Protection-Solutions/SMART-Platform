package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.derby.jdbc.EmbeddedBaseDataSource;
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
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.ui.columns.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.ui.columns.SurveyAttributeQueryColumn;
import org.wcs.smart.er.query.ui.columns.SurveyQueryColumn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBReader;

public class DerbyPagedMissionResult implements IObservationPagedQueryResultSet {
	
	private String queryTempTable;

	private int itemCount = 0;
	private ResultSet lastResultSet;
	private Envelope bounds = null;

	//next sort column
	private QueryColumn sortColumn = null;
	private QueryColumn lastSortColumn = null;
	private int direction = SWT.UP;
	private boolean hasSortColumns = false;

	private DerbySurveyQueryEngine engine;

	
	public DerbyPagedMissionResult(String queryTempTable, DerbySurveyQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}

	public DerbyPagedMissionResult(String queryTempTable, int itemCount, DerbySurveyQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.engine = engine;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DerbyPagedMissionResult) {
			if (queryTempTable == null)
				return super.equals(obj);
			DerbyPagedMissionResult r2 = (DerbyPagedMissionResult) obj;
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
			result.addAll(getResults(lastResultSet, offset, pageSize, session));
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
			final String sql = "SELECT geometry FROM smart.mission_track where mission_uuid in (SELECT mission_uuid FROM " + queryTempTable + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			s.doWork(new Work(){

				@Override
				public void execute(Connection c) throws SQLException {
					WKBReader reader = new WKBReader();
					Envelope results = null;
					
					ResultSet q = c.createStatement().executeQuery(sql);
					try{
						while(q.next()){
							byte[] ob = q.getBytes(1);
							if (ob != null && ob.length > 0){
								LineString ls = (LineString)reader.read(ob);
								if (results == null){
									results = ls.getEnvelopeInternal();
								}else{
									results.expandToInclude(ls.getEnvelopeInternal());
								}
							}
						}
					}catch (Exception ex){
						ERQueryPlugIn.log(ex.getMessage(), ex);
					}finally{
						q.close();
					}
					bounds = results;
				}	
			});
		}
		return bounds;
		
	}
	
	private void updateSortColumn(QueryColumn sortColumn, Session session, Connection c) throws SQLException{
		if (sortColumn instanceof SurveyAttributeQueryColumn){
			if (!hasSortColumns){
				//add the sort columns
				c.createStatement().execute("ALTER TABLE " + queryTempTable + " add column sortKeyDbl double"); //$NON-NLS-1$ //$NON-NLS-2$
				c.createStatement().execute("ALTER TABLE " + queryTempTable + " add column sortKeyTxt varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
				c.commit();
				hasSortColumns = true;
			}
			String key = sortColumn.getKey();
			key = key.split(":")[1]; //$NON-NLS-1$
			Attribute attribute = null;
			
			session.beginTransaction();
			try{
				attribute = QueryDataModelManager.getInstance().getAttribute(session, key); //session will not be closed on purpose
			}finally{
				session.getTransaction().rollback();
			}
			
			switch (attribute.getType()) {
			case BOOLEAN:
			case NUMERIC:
				// nullify first
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyDbl = null "); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case TEXT:
			case LIST:
			case TREE:
			case DATE:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyTxt = null"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			}
			
			switch (attribute.getType()) {
			case BOOLEAN:
			case NUMERIC:
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyDbl = "); //$NON-NLS-1$
				sql.append("(SELECT wpoa.NUMBER_VALUE FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append("and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append(".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case TEXT:
			case DATE:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
				sql.append("(SELECT wpoa.STRING_VALUE FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append("and a.keyid = '"); //$NON-NLS-1$
				sql.append( key );
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append(".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case LIST:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
				sql.append("(SELECT rl.value FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append( "_LIST rl on rl.uuid = wpoa.list_element_uuid "); //$NON-NLS-1$
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append( key );
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( queryTempTable); 
				sql.append(".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				
				break;
			case TREE:
				sql = new StringBuilder();
				sql.append("UPDATE ");//$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyTxt = ");//$NON-NLS-1$
				sql.append("(SELECT rl.value FROM smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append("_TREE rl on rl.uuid = wpoa.tree_node_uuid "); //$NON-NLS-1$
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append( key );
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append( ".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				
				break;
			}
		}
		c.commit();
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
				
				result.addAll(getResults(lastResultSet, offset, pageSize, session));
				attachMissionProperties(result, c, session);
			}
		});
		return result;
	}

	private void attachMissionProperties(List<SurveyQueryResultItem> result, Connection c, Session session) throws SQLException {
		
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT mpv.mission_uuid, ma.keyid, mpv.number_value,  mpv.string_value, mpv.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.mission_attribute ma join smart.mission_property_value mpv on mpv.mission_attribute_uuid = ma.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE mpv.mission_uuid IN ("); //$NON-NLS-1$

		boolean hasItem = false;
		for (SurveyQueryResultItem it : result) {
			if (it.getMissionUuid() != null){
				if (hasItem) attrSql.append(","); //$NON-NLS-1$
				attrSql.append("x'").append(SmartUtils.encodeHex(it.getMissionUuid())).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
				hasItem = true;
			}
		}
		
		
		if (!hasItem) {
			//no missions
			return;
		}
		attrSql.append(')');

		ResultSet rs = c.createStatement().executeQuery(attrSql.toString());
		try {
			while(rs.next()){
				byte[] muuid = rs.getBytes(1);
				String key = rs.getString(2);
				Double dvalue = rs.getDouble(3);
				String svalue = rs.getString(4);
				
				for (SurveyQueryResultItem it : result){
					if (Arrays.equals(muuid,it.getMissionUuid())){
						if (rs.getObject(3) != null){
							it.addMissionPropertyValue(key, dvalue);
						}else if (svalue != null){
							it.addMissionPropertyValue(key,  svalue);
						}else if (rs.getObject(5) != null){
							it.addMissionPropertyValue(key, 
									((MissionAttributeListItem)session.load(MissionAttributeListItem.class, rs.getBytes(5))).getName());
						}
					}
				}
				
			}
		} finally {
			rs.close();
		}		
	}
	

	
	private String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof SurveyQueryColumn) {
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 

			if (sortColumn.getKey().equals(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		}else if (sortColumn instanceof SurveyAttributeQueryColumn) {
			String key = sortColumn.getKey();
			key = key.split(":")[1]; //$NON-NLS-1$
			switch (sortColumn.getType()) {
				case BOOLEAN:
				case NUMBER:
				case INTEGER:
					result = "order by sortKeyDbl"; //$NON-NLS-1$
					break;
				case DATE:
					result = "order by DATE(sortKeyTxt)"; //$NON-NLS-1$
					break;
				default:
					result = "order by UPPER(sortKeyTxt)"; //$NON-NLS-1$
					break;
			}
		}else if (sortColumn instanceof MissionPropertyQueryColumn){
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$
			key = key.replace("missionatt", "ma"); //$NON-NLS-1$ //$NON-NLS-2$
			switch (sortColumn.getType()) {
				case BOOLEAN:
				case NUMBER:
				case INTEGER:
					result = "order by r." + key; //$NON-NLS-1$
					break;
				case DATE:
					result = "order by DATE(r." + key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					break;
				default:
					result = "order by UPPER(r." + key +")"; //$NON-NLS-1$ //$NON-NLS-2$
					break;
			}
		}
		if (!result.isEmpty()) {
			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
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
	
	protected List<SurveyQueryResultItem> getResults(ResultSet rs, int from, int pageSize, Session session) throws SQLException {
		List<SurveyQueryResultItem> items = new ArrayList<SurveyQueryResultItem>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			SurveyQueryResultItem it = (SurveyQueryResultItem) engine.asQueryResultItem(rs, session);
			items.add(it);
		}
		return items;
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
			super(Messages.DerbyPagedObservationResult_cleanupjobname);
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

	@Override
	public int getWpCount() {
		return -1;
	}

}
