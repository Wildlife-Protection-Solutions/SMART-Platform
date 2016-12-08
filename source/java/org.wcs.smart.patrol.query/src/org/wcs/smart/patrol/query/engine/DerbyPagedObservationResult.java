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
package org.wcs.smart.patrol.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolCategoryQueryColumn;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.AbstractPagedQueryResultSet;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyPagedObservationResult extends AbstractPagedQueryResultSet implements IObservationPagedQueryResultSet{
	
	private static String[][] FIXED_COLUMN_KEY_TO_ROW  = {
		 //NOTE: order is important as we don't want to change "patrolleg" to "pleg"
		{"patrolleg", "pl"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"patrol", "p"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"waypoint", "wp"} //$NON-NLS-1$ //$NON-NLS-2$
	};
	
	private String queryTempTable;
	private int wpCount = 0;
	private Envelope bounds = null;

	//next sort column
	private QueryColumn sortColumn = null;
	private QueryColumn lastSortColumn = null;
	private int direction = SWT.UP;
	private boolean hasSortColumns = false;
	
	private DerbyPatrolQueryEngine engine;

	public DerbyPagedObservationResult(String queryTempTable, DerbyPatrolQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}

	public DerbyPagedObservationResult(String queryTempTable, int itemCount, int wpCount, DerbyPatrolQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.wpCount = wpCount;
		this.engine = engine;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		DerbyPagedObservationResult o = (DerbyPagedObservationResult)obj;
		return Objects.equals(queryTempTable, o.queryTempTable);
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(queryTempTable);
	}
	
	@Override
	public Envelope getEnvelope(){
		if (this.bounds == null){
			Session s = HibernateManager.openSession();
			try{final String sql = "SELECT min(wp_x), max(wp_x), min(wp_y), max(wp_y) FROM " + queryTempTable; //$NON-NLS-1$
			s.doWork(new Work(){

				@Override
				public void execute(Connection c) throws SQLException {
					try(ResultSet q = c.createStatement().executeQuery(sql)){
						q.next();
						double minx = q.getDouble(1);
						double maxx = q.getDouble(2);
						double miny = q.getDouble(3);
						double maxy = q.getDouble(4);
				
						bounds = new Envelope(minx, maxx, miny, maxy);
					}
				}	
			});
			}finally{
				s.close();
			}
		}
		return bounds;
		
	}
	
	private void updateSortColumn(QueryColumn sortColumn, Session session, Connection c) throws SQLException{
		if (sortColumn instanceof PatrolAttributeQueryColumn){
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
		
	/**
	 *Opens a result set in the given session that accessed the query results
	 */
	public ResultSet getResultSet(Session session) {
		final String dataSql = "SELECT r.* FROM " + queryTempTable + " r "+ buildSortSql();  //$NON-NLS-1$ //$NON-NLS-2$
		
		return session.doReturningWork(new ReturningWork<ResultSet>() {
		
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				if ((lastSortColumn == null && sortColumn != null) 
						|| (lastSortColumn != null && sortColumn != null && !lastSortColumn.equals(sortColumn)) ){
					updateSortColumn(sortColumn, session, c);
				}
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);	
			}
		});
	}

	private void attachObservations(List<? extends IResultItem> result, Connection c, Session session) throws SQLException {
		boolean hasObservations = false;
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.p_ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(queryTempTable);
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid in ("); //$NON-NLS-1$
		for (IResultItem pit : result){
			PatrolQueryResultItem it  = (PatrolQueryResultItem) pit;
			if (it.getObservationUuid() != null) {
				if (hasObservations) {
					attrSql.append(',');
				}
				hasObservations = true;
				attrSql.append("x'").append(UuidUtils.uuidToString(it.getObservationUuid())).append('\''); //$NON-NLS-1$
			}
		}
		
		
		if (!hasObservations) {
			//no observations in current data fragment, so no need to select attributes as they will be empty
			return;
		}
		attrSql.append(')');

		
		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
			HashMap<UUID, HashMap<String, Object>> attrMap = getResultsAttributes(rs, session);
			for (IResultItem pit : result){
				PatrolQueryResultItem it  = (PatrolQueryResultItem) pit;
				if (it.getObservationUuid() != null) {
					HashMap<String, Object> attributes = attrMap.get(it.getObservationUuid());
					if (attributes != null) {
						it.setAttributes(attributes);
					}
				}
			}
		}
	}
	
	private String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
			for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
				key = key.replace(data[0], data[1]);
			}
			if (sortColumn.getKey().equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		}
		if (sortColumn instanceof PatrolCategoryQueryColumn) {
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
			result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (sortColumn instanceof PatrolAttributeQueryColumn) {
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
	@Override
	public List<IResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<IResultItem> items = new ArrayList<IResultItem>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			PatrolQueryResultItem it = engine.asQueryResultItem(rs, null);
			items.add(it);
		}
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				attachObservations(items, c, session);		
			}
			
		});
		
		return items;
	}

	protected HashMap<UUID, HashMap<String, Object>> getResultsAttributes(ResultSet rs, Session s) throws SQLException {
		HashMap<UUID, HashMap<String, Object>> attrMap = new HashMap<UUID, HashMap<String, Object>>();
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
			UUID keyObj = UuidUtils.byteToUUID(obUuid);
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

	public int getWpCount() {
		return wpCount;
	}

	protected void setWpCount(int wpCount) {
		this.wpCount = wpCount;
	}
	
	@Override
	public void dispose(Session session) throws SQLException{
		super.dispose(session);
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				engine.dropTables(c);
			}
		});
	}


}
