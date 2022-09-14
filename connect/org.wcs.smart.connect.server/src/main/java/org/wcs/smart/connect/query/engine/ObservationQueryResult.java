/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.query.common.engine.AttachmentResultSetIterator;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IObservationQueryResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.UuidUtils;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public abstract class ObservationQueryResult<T extends ObservationQueryResultItem> extends WaypointQueryResult<T> implements IPagedImageResultSet {
	
	public ObservationQueryResult(IWOEngine<T> engine, int resultcount, boolean includeUuids){
		super(engine, resultcount, includeUuids);
	}
	
	@Override
	public List<QueryColumn> getQueryColumns(SimpleQuery query, Locale l, Session session, IProjectionProvider prj){
		List<QueryColumn> cols = super.getQueryColumns(query, l, session, prj);
		if (!includeUuids) return cols;
		
		QueryColumn obsUuidCol = new QueryColumn(getObservationColumnName(l), OBS_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((ObservationQueryResultItem)item).getObservationUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((ObservationQueryResultItem)item).getObservationUuid());
			}
			
		};
		QueryColumn wpUuidCol = new QueryColumn(getWaypointColumnName(l), WP_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((ObservationQueryResultItem)item).getWaypointUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((ObservationQueryResultItem)item).getWaypointUuid());
			}	
		};
		
		cols.add(obsUuidCol);
		cols.add(wpUuidCol);
				
		return cols;
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
				attachObservations(items, c, session);		
			}
		});
		return items;
	}
	
	protected void attachObservations(List<T> result, Connection c, Session session) throws SQLException {
		
		Set<UUID> obs = result.stream()
				.filter(e->e.getObservationUuid()!= null)
				.map(e->e.getObservationUuid())
				.collect(Collectors.toSet());

		
		if (obs.isEmpty()) return;
		
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".number_value,"); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".string_value,"); //$NON-NLS-1$
		attrSql.append(" ll.value as label_value, r.ca_uuid "); //$NON-NLS-1$
		attrSql.append(" FROM "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable());
		attrSql.append(" r join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(WaypointObservationAttribute.class));
		attrSql.append(" on r.ob_uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		attrSql.append(" join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(Attribute.class));
		attrSql.append(" on "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(Attribute.class) + ".uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid ");  //$NON-NLS-1$//$NON-NLS-2$
		attrSql.append(" left join "); //$NON-NLS-1$
		attrSql.append(engine.getObservationLabelTable());
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
		for (int i = 0; i < obs.size(); i ++) {
			if (i != 0) attrSql.append(","); //$NON-NLS-1$
			attrSql.append("?"); //$NON-NLS-1$
		}
		attrSql.append(") UNION "); //$NON-NLS-1$

		attrSql.append("SELECT r.ob_uuid, a.keyid, cast(null as double precision), "); //$NON-NLS-1$
		attrSql.append("cast(null as varchar(500)), string_agg(ll.value, ', ' order by ll.value), r.ca_uuid "); //$NON-NLS-1$
		attrSql.append(" FROM "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable());
		attrSql.append(" r join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(WaypointObservationAttribute.class));
		attrSql.append(" on r.ob_uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		attrSql.append(" join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(WaypointObservationAttributeList.class));
		attrSql.append(" on " + engine.tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid = "); //$NON-NLS-1$ //$NON-NLS-2$
		attrSql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".uuid "); //$NON-NLS-1$ 
		attrSql.append(" join "); //$NON-NLS-1$
		attrSql.append(engine.getObservationLabelTable());
		attrSql.append(" ll on ll.uuid = " + engine.tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		attrSql.append(" join "); //$NON-NLS-1$
		attrSql.append(engine.tableNamePrefix(Attribute.class));
		attrSql.append(" on "); //$NON-NLS-1$
		attrSql.append(engine.tablePrefix(Attribute.class) + ".uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid ");  //$NON-NLS-1$//$NON-NLS-2$
		attrSql.append(" WHERE r.ob_uuid in ("); //$NON-NLS-1$
		for (int i = 0; i < obs.size(); i ++) {
			if (i != 0) attrSql.append(","); //$NON-NLS-1$
			attrSql.append("?"); //$NON-NLS-1$
		}
		attrSql.append(") "); //$NON-NLS-1$
		attrSql.append(" GROUP BY r.ob_uuid, a.keyid, r.ca_uuid"); //$NON-NLS-1$
		
		
		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		int i = 0;
		for (UUID uuid : obs) {
			ps.setObject(i+1, uuid);
			ps.setObject(i+obs.size()+1, uuid);
			i++;
		}
		
		HashMap<UUID, HashMap<String, Object>> attrMap = new HashMap<UUID, HashMap<String, Object>>();
		try(ResultSet rs = ps.executeQuery()){
			while (rs.next()) {
				UUID obUuid = (UUID) rs.getObject(1);
				
				if (obUuid == null) continue;
				
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
	

	
	@Override
	public String getGeometryType() {
		return POINT_GEOM_TYPE;
	}
	
	protected void setFields(T it, ResultSet rs) throws SQLException{
		super.setFields(it, rs);
		
		it.setWaypointObserver(rs.getString("ob_observer")); //$NON-NLS-1$
		UUID obuuid = (UUID) rs.getObject("ob_uuid"); //$NON-NLS-1$
		if (obuuid == null){
			it.setObservationUuid(null);
		}else{
			it.setObservationUuid(obuuid); 
		}
		UUID guuid = (UUID) rs.getObject("wp_group_uuid"); //$NON-NLS-1$
		if (guuid == null){
			it.setObservationGroupUuid(null);
		}else{
			it.setObservationGroupUuid(guuid); 
		}
		//build categories
		List<String> categories = new ArrayList<String>();
		for (int i = 0; i < engine.getCategoryCnt(); i ++){
			String category = rs.getString("category_"+i); //$NON-NLS-1$
			if (category == null){
				break;
			}
			categories.add(category);
		}
		
		it.setCategory(categories.toArray(new String[categories.size()]));
	}
	

	@Override
	public IQueryResultSetIterator<? extends IAttachmentResultItem> getImageIterator(Session session) throws SQLException{
		
		imageDataTable = engine.createTempTableName();		
		imageCount = createImageDataObservation(session, engine.getQueryDataTable(), imageDataTable);
		
		String query = getImageQueryObservation(engine.getQueryDataTable(), imageDataTable, 
				getDistinctWaypointQuery("r.", true),  //$NON-NLS-1$
				getDistinctWaypointQuery("r.", false)); //$NON-NLS-1$
		
		return new AttachmentResultSetIterator(session, 
				e->asAttachmentQueryResultItem(e, session),
				()->query);
	}
	

	@Override
	public void updateSortColumn(Session session) throws SQLException {
		updateSortColumnGeneral(session, engine.getQueryDataTable(),
				engine.getObservationLabelTable(),
				engine.getCaFilter(), ".ob_"); //$NON-NLS-1$
	}
	
	protected abstract String getDistinctWaypointQuery(String prefix, boolean includeObservation);
}


