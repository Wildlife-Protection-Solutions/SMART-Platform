package org.wcs.smart.query.common.engine.test;

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
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.common.engine.AttachmentResultSetIterator;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IObservationQueryResultItem;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
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
	
	public static final String TXT_SORT = "sortKeyTxt";
	public static final String NUMBER_SORT = "sortKeyDbl";
	
	int wp_count = 0;
	protected boolean hasSortColumns = false;

	
	public ObservationQueryResult(ObservationQueryEngine<T> engine, int itemCount, int wpCount) {
		super(engine, itemCount);
		this.wp_count = wpCount;
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
					result = "order by DATE(" + TXT_SORT + ")"; //$NON-NLS-1$
					break;
				default:
					result = "order by UPPER(" + TXT_SORT + ")"; //$NON-NLS-1$
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
				c.createStatement().execute("ALTER TABLE " + getResultsTable() + " add column " + NUMBER_SORT + " double"); //$NON-NLS-1$ //$NON-NLS-2$
				c.createStatement().execute("ALTER TABLE " + getResultsTable() + " add column " + TXT_SORT + " varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
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
				sql.append(getResultsTable());
				sql.append(" SET ");
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
				sql.append(" SET ");
				sql.append(TXT_SORT);
				sql.append("  = null "); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			}
			
			switch (attribute.getType()) {
			case BOOLEAN:
			case NUMERIC:
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(getResultsTable());
				sql.append(" SET ");
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
				sql.append(" SET ");
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
				sql.append(" SET ");
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
		
		return new AttachmentResultSetIterator(session, 
				e->engine.asQueryAttachmentResultItem(e, session),
				()->sb.toString());
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

		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, ");
		attrSql.append("wpoa.string_value, ll.value as label_value, r.ca_uuid "); //$NON-NLS-1$
		attrSql.append(" FROM ");
		attrSql.append(queryTempTable);
		attrSql.append(" r join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid ");
		attrSql.append(" join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(labelTable);
		attrSql.append(" ll on ll.uuid = ");
		attrSql.append(" case when wpoa.list_element_uuid is not null then wpoa.list_element_uuid else wpoa.tree_node_uuid end "); //$NON-NLS-1$
		attrSql.append(" WHERE (wpoa.number_value is not null or wpoa.string_value is not null or wpoa.list_element_uuid is not null or wpoa.tree_node_uuid is not null ) AND ");
		attrSql.append(" r.ob_uuid in ("); //$NON-NLS-1$
		
		String obs =  result.stream()
				.filter(e->e.getObservationUuid() != null)
				.map(e-> (String)("x'" + UuidUtils.uuidToString(e.getObservationUuid()) + "'"))
				.collect(Collectors.joining(","));
		
		if (obs.isEmpty()) return;
		
		attrSql.append(obs);
		attrSql.append(") UNION ");

		attrSql.append("SELECT r.ob_uuid, a.keyid, cast(null as double precision), ");
		attrSql.append("cast(null as varchar(500)), cast(smart.concatagg(ll.value) as varchar(32000)), r.ca_uuid "); //$NON-NLS-1$
		attrSql.append(" FROM ");
		attrSql.append(queryTempTable);
		attrSql.append(" r join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid ");
		attrSql.append(" join smart.wp_observation_attributes_list al on al.observation_attribute_uuid = wpoa.uuid ");
		attrSql.append(" join ");
		attrSql.append(labelTable);
		attrSql.append(" ll on ll.uuid = al.list_element_uuid ");
		attrSql.append(" join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE r.ob_uuid in ("); //$NON-NLS-1$
		attrSql.append(obs);
		attrSql.append(") ");
		attrSql.append(" GROUP BY r.ob_uuid, a.keyid, r.ca_uuid");
		
		System.out.println(attrSql.toString());
		
		
		
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
