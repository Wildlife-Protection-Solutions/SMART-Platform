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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.common.ui.image.PagedImageQueryResults;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.UuidUtils;

//wp_x, wp_y

/**
 * For queries which return observation query results.  
 *  
 * @author Emily
 *
 * @param <T>
 */
public abstract class ObservationQueryResult<T extends IObservationQueryResultItem> extends WaypointQueryResult implements IObservationPagedQueryResultSet<T>{
	
	public static final String TXT_SORT = "sortKeyTxt"; //$NON-NLS-1$
	public static final String NUMBER_SORT = "sortKeyDbl"; //$NON-NLS-1$
	
	int wp_count = 0;
	protected boolean hasSortColumns = false;

	
	public ObservationQueryResult(ObservationQueryEngine<T> engine, int itemCount, int wpCount) {
		super(engine, itemCount);
		this.wp_count = wpCount;
		
		this.imageResults = new PagedImageQueryResults(engine) {		
			@Override
			protected String getImageQuery() {
				return ObservationQueryResult.this.getImageQuery();
			}

			@Override
			protected void initImageData() {
				ObservationQueryResult.this.initImageData();
			}
		};
	}

	public ObservationQueryEngine<T> getEngineInternal() {
		return (ObservationQueryEngine<T>) super.engine;
	}
	
	/**
	 * Builds the sort string for CategoryQueryColumn and AttributeQueryColumn
	 * @param sortColumn
	 * @return
	 */
	public String getSortColumnString(QueryColumn sortColumn) {
		String result = null;
		
		if (sortColumn instanceof CategoryQueryColumn) {
			String key = sortColumn.getKey();
			key = CategoryQueryColumn.getDbColumnName(key);
			result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (sortColumn instanceof AttributeQueryColumn) {
			String key = sortColumn.getKey();
			key = key.split(":")[1]; //$NON-NLS-1$
			switch (sortColumn.getType()) {
				case BOOLEAN:
				case NUMBER:
				case INTEGER:
					result = "order by " + NUMBER_SORT; //$NON-NLS-1$
					break;
				case DATE:
					result = "order by DATE(" + TXT_SORT + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					break;
				default:
					result = "order by UPPER(" + TXT_SORT + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					break;
			}
		}
		return result;
	}
	
	@Override
	protected void updateSortColumn(Session session, Connection c) throws SQLException{
		if (sortColumn instanceof AttributeQueryColumn){
			if (!hasSortColumns){
				//add the sort columns
				c.createStatement().execute("ALTER TABLE " + getResultsTable() + " add column " + NUMBER_SORT + " double"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				c.createStatement().execute("ALTER TABLE " + getResultsTable() + " add column " + TXT_SORT + " varchar(" + Attribute.STRING_ATTRIBUTE_MAX_LENGTH + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
			case MLIST: 
				throw new UnsupportedOperationException("Sorting not supported on multi-list attribute"); //$NON-NLS-1$
			case BOOLEAN:
			case NUMERIC:
				// nullify first
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(getResultsTable());
				sql.append(" SET "); //$NON-NLS-1$
				sql.append(NUMBER_SORT);
				sql.append("  = null "); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case TEXT:
			case LIST:
			case TREE:
			case DATE:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(getResultsTable());
				sql.append(" SET "); //$NON-NLS-1$
				sql.append(TXT_SORT);
				sql.append("  = null "); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			}
			
			switch (attribute.getType()) {
			case MLIST: 
				throw new UnsupportedOperationException("Sorting not supported on multi-list attribute"); //$NON-NLS-1$
			case BOOLEAN:
			case NUMERIC:
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(getResultsTable());
				sql.append(" SET "); //$NON-NLS-1$
				sql.append(NUMBER_SORT);
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT wpoa.NUMBER_VALUE FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append("and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( getResultsTable() );
				sql.append(".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case TEXT:
			case DATE:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(getResultsTable());
				sql.append(" SET "); //$NON-NLS-1$
				sql.append(TXT_SORT);
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT wpoa.STRING_VALUE FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append("and a.keyid = '"); //$NON-NLS-1$
				sql.append( key );
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( getResultsTable() );
				sql.append(".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
		
			case LIST:
			case TREE:
				sql = new StringBuilder();
				sql.append("UPDATE ");//$NON-NLS-1$
				sql.append(getResultsTable());
				sql.append(" SET "); //$NON-NLS-1$
				sql.append(TXT_SORT);
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT rl.value FROM smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
				sql.append( getEngineInternal().getObservationLabelTable() );
				if (attribute.getType() == AttributeType.TREE) {
					sql.append(" rl on rl.uuid = wpoa.tree_node_uuid "); //$NON-NLS-1$
				}else if (attribute.getType() == AttributeType.LIST) {
					sql.append(" rl on rl.uuid = wpoa.list_element_uuid "); //$NON-NLS-1$
				}
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append( key );
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( getResultsTable() );
				sql.append( ".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				
				break;
			}
		}
		c.commit();
	}
	/**
	 * Gets results from the given result set.
	 * 
	 * @param rs
	 * @param from
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	@Override
	public List<T> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<T> items = super.getResults(session, rs, from, pageSize);
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				attachObservations(items, getResultsTable(), getEngineInternal().getObservationLabelTable(), c, session);		
			}
			
		});
		return items;
	}

	@Override
	public IQueryResultSetIterator<? extends IAttachmentResultItem> getImageIterator(Session session) throws SQLException{

		initImageData();
		
		return new AttachmentResultSetIterator(session, 
				e->engine.asQueryAttachmentResultItem(e, session),
				()->getImageQuery());
	}
	

	private String getImageQuery() {
		StringBuilder sb = new StringBuilder();
		String part = getDistinctWaypointQuery("r.", true); //$NON-NLS-1$
		
		//join together attachments specifically associated with an observation
		//or attachments associated with a waypoint that has an observation
		//in the result set
		sb.append("SELECT " + part + ", b.attach_uuid as attach_uuid FROM " ); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(getResultsTable() + " r "); //$NON-NLS-1$
		sb.append(" join " + imageResults.getResultsTable() + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("on ( b.ob_uuid is not null and r.ob_uuid = b.ob_uuid ) "); //$NON-NLS-1$
		
		sb.append(" UNION "); //$NON-NLS-1$
		
		part = getDistinctWaypointQuery("r.", false); //$NON-NLS-1$
		
		sb.append("SELECT foo.*, b.attach_uuid as attach_uuid FROM "); //$NON-NLS-1$
		sb.append("(SELECT DISTINCT " + part + " FROM " + getResultsTable() + " r ) foo"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" join " + imageResults.getResultsTable() + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" ON b.wp_uuid = foo.wp_uuid and b.ob_uuid is null"); //$NON-NLS-1$
		
		return sb.toString();
	}
	
	@Override
	protected synchronized void initImageData() {
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				String imageTempTable = engine.createTempTableName();
				
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE "); //$NON-NLS-1$
				sb.append(imageTempTable);
				sb.append("(attach_uuid char(16) for bit data, wp_uuid char(16) for bit data, ob_uuid char(16) for bit data, seq_order integer GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1))"); //$NON-NLS-1$
				s.createNativeQuery(sb.toString()).executeUpdate();
				
				sb = new StringBuilder();
				sb.append(" INSERT INTO "); //$NON-NLS-1$
				sb.append(imageTempTable + "(attach_uuid, wp_uuid, ob_uuid) "); //$NON-NLS-1$
				sb.append(" SELECT z.attach_uuid, z.wp_uuid, z.ob_uuid FROM ( "); //$NON-NLS-1$
				sb.append("SELECT "); //$NON-NLS-1$
				sb.append(engine.tablePrefix(ObservationAttachment.class) + ".uuid as attach_uuid, "); //$NON-NLS-1$
				sb.append("a.wp_time, a.wp_id, a.wp_uuid as wp_uuid, a.ob_uuid as ob_uuid "); //$NON-NLS-1$
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( getResultsTable() + " a "); //$NON-NLS-1$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(engine.tableNamePrefix(ObservationAttachment.class ));
				sb.append(" ON "); //$NON-NLS-1$
				sb.append( engine.tablePrefix(ObservationAttachment.class) + ".obs_uuid = a.ob_uuid "); //$NON-NLS-1$ 
				sb.append( " UNION "); //$NON-NLS-1$
				sb.append("SELECT c.uuid as attach_uuid, a.wp_time, a.wp_id, a.wp_uuid as wp_uuid, cast(null as char(16) for bit data) as ob_uuid " ); //$NON-NLS-1$
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( getResultsTable() + " a "); //$NON-NLS-1$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(" smart.wp_attachments c on c.wp_uuid = a.wp_uuid "); //$NON-NLS-1$
				sb.append(" ) z ORDER BY z.wp_time desc, z.wp_id "); //$NON-NLS-1$
				s.createNativeQuery(sb.toString()).executeUpdate();
				
				sb = new StringBuilder();
				sb.append("SELECT count(*) FROM "); //$NON-NLS-1$
				sb.append(imageTempTable);
				
				int imageDataCnt = (int) s.createNativeQuery(sb.toString()).uniqueResult();
				
				imageResults.setResults(imageTempTable, imageDataCnt);
				s.getTransaction().commit();
			}catch (Exception ex) {
				imageResults.setResults(null, -1);
				s.getTransaction().rollback();
				QueryPlugIn.log("Error computing attachment details: " + ex.getMessage(), ex); //$NON-NLS-1$
			}
		}
	}
	
	protected abstract String getDistinctWaypointQuery(String prefix, boolean includeObservation);
	

	public int getWpCount() {
		return wp_count;
	}

	public void setWpCount(int wpCount) {
		this.wp_count = wpCount;
	}

	protected void attachObservations(List<? extends IObservationQueryResultItem> result,
			String queryTempTable,
			String labelTable,
			Connection c, Session session) throws SQLException {

		String obs =  result.stream()
				.filter(e->e.getObservationUuid() != null)
				.map(e-> (String)("x'" + UuidUtils.uuidToString(e.getObservationUuid()) + "'")) //$NON-NLS-1$ //$NON-NLS-2$
				.collect(Collectors.joining(",")); //$NON-NLS-1$
		
		if (obs.isEmpty()) return;
		
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".number_value,"); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".string_value,"); //$NON-NLS-1$
		attrSql.append(" ll.value as label_value, r.ca_uuid "); //$NON-NLS-1$
		attrSql.append(" FROM "); //$NON-NLS-1$
		attrSql.append(queryTempTable);
		attrSql.append(" r join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(WaypointObservationAttribute.class));
		attrSql.append(" on r.ob_uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		attrSql.append(" join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(Attribute.class));
		attrSql.append(" on "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(Attribute.class) + ".uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid ");  //$NON-NLS-1$//$NON-NLS-2$
		attrSql.append(" left join "); //$NON-NLS-1$
		attrSql.append(labelTable);
		attrSql.append(" ll on ll.uuid = "); //$NON-NLS-1$
		attrSql.append(" case when "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid is not null then "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid else "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".tree_node_uuid end "); //$NON-NLS-1$
		attrSql.append(" WHERE ("); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".number_value is not null or "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".string_value is not null or "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid is not null or "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".tree_node_uuid is not null ) "); //$NON-NLS-1$
		attrSql.append(" AND r.ob_uuid IN ("); //$NON-NLS-1$
		attrSql.append(obs);
		attrSql.append(") UNION "); //$NON-NLS-1$

		attrSql.append("SELECT r.ob_uuid, a.keyid, cast(null as double precision), "); //$NON-NLS-1$
		attrSql.append("cast(null as varchar(500)), cast(smart.concatagg(ll.value) as varchar(32000)), r.ca_uuid "); //$NON-NLS-1$
		attrSql.append(" FROM "); //$NON-NLS-1$
		attrSql.append(queryTempTable);
		attrSql.append(" r join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(WaypointObservationAttribute.class));
		attrSql.append(" on r.ob_uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		attrSql.append(" join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(WaypointObservationAttributeList.class));
		attrSql.append(" on " + engine.tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid = "); //$NON-NLS-1$ //$NON-NLS-2$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".uuid "); //$NON-NLS-1$ 
		attrSql.append(" join "); //$NON-NLS-1$
		attrSql.append(labelTable);
		attrSql.append(" ll on ll.uuid = " + engine.tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		attrSql.append(" join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(Attribute.class));
		attrSql.append(" on "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(Attribute.class) + ".uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid ");  //$NON-NLS-1$//$NON-NLS-2$
		attrSql.append(" WHERE r.ob_uuid in ("); //$NON-NLS-1$
		attrSql.append(obs);
		attrSql.append(") "); //$NON-NLS-1$
		attrSql.append(" GROUP BY r.ob_uuid, a.keyid, r.ca_uuid"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(attrSql.toString());
		
		HashMap<UUID, HashMap<String, Object>> attrMap = new HashMap<UUID, HashMap<String, Object>>();
		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
			while (rs.next()) {
				UUID obUuid = UuidUtils.byteToUUID(rs.getBytes(1));
				
				if (obUuid == null)
					continue;
				HashMap<String, Object> attributes = attrMap.get(obUuid);
				if (attributes == null) {
					attributes = new HashMap<String, Object>();
					attrMap.put(obUuid, attributes);
				}
				String key = rs.getString(2);
				if (key != null) {
					Object value = null;
					
					if (rs.getObject(3) != null) value = rs.getDouble(3); //double
					if (rs.getObject(4) != null) value = rs.getString(4); //string
					if (rs.getObject(5) != null) value = rs.getString(5); //list/tree
						
					attributes.put(key, value);
				}
			}
		}
		
		for (IObservationQueryResultItem it : result){
			if (it.getObservationUuid() == null) continue;
			HashMap<String, Object> attributes = attrMap.get(it.getObservationUuid());
			if (attributes != null) it.setAttributes(attributes);
		}
		
	}
}
