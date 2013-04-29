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
package org.wcs.smart.query.engine;

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
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.observation.AttributeQueryColumn;
import org.wcs.smart.query.model.observation.CategoryQueryColumn;
import org.wcs.smart.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.model.observation.QueryColumn;
import org.wcs.smart.util.SmartUtils;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyQueryResult {
	
	private static String[][] FIXED_COLUMN_KEY_TO_ROW  = {
		 //NOTE: order is important as we don't want to change "patrolleg" to "pleg"
		{"patrolleg", "pl"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"patrol", "p"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"waypoint", "wp"} //$NON-NLS-1$ //$NON-NLS-2$
	};
	
	private String queryTempTable;
	private String sortSql = ""; //$NON-NLS-1$
	private int pageSize = 30;
	private int itemCount = 0;
	private int wpCount = 0;

	private ResultSet lastResultSet;

	public DerbyQueryResult(String queryTempTable) {
		this.queryTempTable = queryTempTable;
	}

	public DerbyQueryResult(String queryTempTable, int itemCount, int wpCount) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.wpCount = wpCount;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DerbyQueryResult) {
			if (queryTempTable == null)
				return super.equals(obj);
			DerbyQueryResult r2 = (DerbyQueryResult) obj;
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

	public List<QueryResultItem> getData(final int offset) {
		final Session session = HibernateManager.openSession();
		//NOTE: session will not be closed on purpose!!!!
		//as we want related ResultSet to remain opened for performance reasons
		List<QueryResultItem> result = getNextData(session, offset);
		if (result == null) {
			result = getData(session, offset);
		}
		return result;
	}
	
	private List<QueryResultItem> getNextData(final Session session, final int offset) {
		if (lastResultSet == null)
			return null;
		final List<QueryResultItem> result = new ArrayList<QueryResultItem>();
		try {
			result.addAll(getResults(lastResultSet, offset));
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
	
	private List<QueryResultItem> getData(final Session session, final int offset) {
		final List<QueryResultItem> result = new ArrayList<QueryResultItem>();
		final String dataSql = "SELECT r.* FROM "+queryTempTable+" r "+getSortSql();  //$NON-NLS-1$ //$NON-NLS-2$
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				lastResultSet = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);
				try {
					result.addAll(getResults(lastResultSet, offset));
				} finally {
					//rs.close();
				}
				attachObservations(result, c, session);
			}
		});
		return result;
	}

	private void attachObservations(List<QueryResultItem> result, Connection c, Session session) throws SQLException {
		boolean hasObservations = false;
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.p_ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(queryTempTable);
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid in ("); //$NON-NLS-1$
		for (QueryResultItem it : result) {
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
			for (QueryResultItem it : result) {
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
	
	private String buildSortSql(QueryColumn sortColumn, int direction) {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
			for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
				key = key.replace(data[0], data[1]);
			}
			result = "order by r."+key; //$NON-NLS-1$
		}
		if (sortColumn instanceof CategoryQueryColumn) {
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
			result = "order by r."+key; //$NON-NLS-1$
		}
		if (sortColumn instanceof AttributeQueryColumn) {
			String key = sortColumn.getKey();
			key = key.split(":")[1]; //$NON-NLS-1$
			Attribute attribute = QueryDataModelManager.getInstance().getAttribute(HibernateManager.openSession(), key); //session will not be closed on purpose
			switch (attribute.getType()) {
			case BOOLEAN:
			case NUMERIC:
				result = "left join (select wpoa.observation_uuid as ob_uuid, wpoa.NUMBER_VALUE as a_value, a.KEYID as a_key from smart.wp_observation_attributes wpoa inner join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid AND a.KEYID = '"+key+"') x on x.ob_uuid = r.OB_UUID order by x.a_value"; //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case TEXT:
				result = "left join (select wpoa.observation_uuid as ob_uuid, wpoa.STRING_VALUE as a_value, a.KEYID as a_key from smart.wp_observation_attributes wpoa inner join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid AND a.KEYID = '"+key+"') x on x.ob_uuid = r.OB_UUID order by x.a_value"; //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case LIST:
				result = "left join (select rl.value, wpoa.observation_uuid from "+queryTempTable+"_LIST rl inner join smart.wp_observation_attributes wpoa on rl.uuid = wpoa.LIST_ELEMENT_UUID inner join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid AND a.KEYID = '"+key+"') x on x.OBSERVATION_UUID = r.OB_UUID order by x.value";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				break;
			case TREE:
				result = "left join (select rl.value, wpoa.observation_uuid from "+queryTempTable+"_TREE rl inner join smart.wp_observation_attributes wpoa on rl.uuid = wpoa.TREE_NODE_UUID inner join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid AND a.KEYID = '"+key+"') x on x.OBSERVATION_UUID = r.OB_UUID order by x.value";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				break;
			}
		}
		
		if (!result.isEmpty()) {
			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	
	
	
	private String getSortSql() {
		//return "order by r.wp_x asc";
		return sortSql;
	}

	public void setSorting(QueryColumn sortColumn, int direction) {
		String newSql = buildSortSql(sortColumn, direction);
		if (newSql.equals(sortSql)) {
			return;
		}
		sortSql = newSql;
		dropResultSet();
	}

	private void dropResultSet() {
		if (lastResultSet != null) {
			try {
				lastResultSet.close();
			} catch (SQLException e) {
				//nothing
				e.printStackTrace();
			}
			lastResultSet = null;
		}
	}
	
	protected List<QueryResultItem> getResults(ResultSet rs, int from) throws SQLException {
		List<QueryResultItem> items = new ArrayList<QueryResultItem>();
		/*
		1	P_CA_UUID
		2	P_UUID
		3	P_ID
		4	P_STATION_UUID
		5	P_TEAM_UUID
		6	P_OBJECTIVE
		7	P_MANDATE_UUID
		8	P_TYPE
		9	P_IS_ARMED
		10	P_START_DATE
		11	P_END_DATE
		12	PL_UUID
		13	PL_ID (same as P_LEGID)
		14	PL_TRANSPORT_UUID
		15	PLD_UUID
		16	PLD_PATROL_DAY (same as WP_DATE)
		17	WP_UUID
		18	WP_ID
		19	WP_X
		20	WP_Y
		21	WP_DIRECTION
		22	WP_DISTANCE
		23	WP_TIME
		24	WP_WP_COMMENT
		25	OB_UUID
		26	OB_CATEGORY_UUID
		27	PLM_LEADER
		28	PLM_PILOT
		29	P_STATION
		30	P_TEAM
		31	P_MANDATE
		32	P_TRANSPORT
		33	P_LEADER
		34	P_PILOT
		35+	CATEGORY_0 -> CATEGORY_N (N last category number)
		 */
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		int columnCount = rs.getMetaData().getColumnCount();
		for(int x = from; x < to; x++) {
			rs.next();
//		while (rs.next()) {
			QueryResultItem it = new QueryResultItem();
			
			it.setPatrolUuid(rs.getBytes(2));
			it.setPatrolId(rs.getString(3));
			it.setPatrolStartDate(rs.getDate(10));
			it.setPatrolEndDate(rs.getDate(11));
			it.setStation(rs.getString(29));				
			it.setTeam(rs.getString(30));	
			it.setObjective(rs.getString(6));
			it.setMandate(rs.getString(31));
			it.setPatrolType(PatrolType.Type.valueOf(rs.getString(8)));
			it.setArmed(rs.getBoolean(9));
			it.setTransportType(rs.getString(32));
			it.setPatrolLegId(rs.getString(13));
			it.setWpDateTime(rs.getDate(16));
			
			it.setLeader(rs.getString(33));
			it.setPilot(rs.getString(34));
			it.setWaypointUuid(rs.getBytes(17));
			it.setWaypointId(rs.getInt(18));
			it.setWaypointX(rs.getDouble(19));
			it.setWaypointY(rs.getDouble(20));
			it.setWaypointTime(rs.getTime(23));
			it.setWaypointDirection(rs.getFloat(21));
			it.setWaypointDistance(rs.getFloat(22));
			it.setWaypointComment(rs.getString(24));
			it.setObservationUuid(rs.getBytes(25));
			//build categories
			List<String> categories = new ArrayList<String>();
			for (int j = 35; j <= columnCount; j++) {
				String category = rs.getString(j);
				if (category == null) {
					break;
				}
				categories.add(category);
			}
			it.setCategory(categories.toArray(new String[categories.size()]));
			
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

	public Iterator<QueryResultItem> iterator() {
		return new LazyQueryIterator();
	}
	
	private MapByteArrayKey wrap(byte[] array) {
		return new MapByteArrayKey(array);
	}
	
	public int getPageSize() {
		return pageSize;
	}

	protected void setPageSize(int pageSize) {
		this.pageSize = pageSize;
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
	private class LazyQueryIterator implements Iterator<QueryResultItem> {
		
		private int itOffset = 0; //offset of element at which list begins
		private int itIndex = 0;
		private List<QueryResultItem> data;

		@Override
		public boolean hasNext() {
			return itOffset + itIndex + 1 < itemCount;
		}

		@Override
		public QueryResultItem next() {
			if (!hasNext())
				throw new NoSuchElementException();
			if (data == null) {
				itOffset = 0;
				itIndex = 0;
				data = getData(itOffset);
				return data.get(itIndex);
			}
			itIndex++;
			if (itIndex < data.size()) {
				return data.get(itIndex);
			}
			//we need to load new portion of data
			itOffset += data.size();
			itIndex = 0;
			data = getData(itOffset);
			return data.get(itIndex);
		}

		@Override
		public void remove() {
			throw new IllegalStateException("Remove operation is not supported."); //$NON-NLS-1$
		}
		
	}
	
	private class CleanUpJob extends Job {

		public CleanUpJob() {
			super(Messages.DerbyQueryResult_CleanUpJob_Title);
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
						}
						//list elements value table
						try {
							String sql = "DROP TABLE " + queryTempTable + "_LIST"; //$NON-NLS-1$ //$NON-NLS-2$
							c.createStatement().execute(sql);
							QueryPlugIn.logSql(sql);
						} catch (Exception ex) {
							// eatme
						}
						//tree elements value table
						try {
							String sql = "DROP TABLE " + queryTempTable + "_TREE"; //$NON-NLS-1$ //$NON-NLS-2$
							c.createStatement().execute(sql);
							QueryPlugIn.logSql(sql);
						} catch (Exception ex) {
							// eatme
						}
					}
				});
			} finally {
				session.getTransaction().commit();
				session.close();
			}
			return Status.OK_STATUS;
		}
		
	}
}
