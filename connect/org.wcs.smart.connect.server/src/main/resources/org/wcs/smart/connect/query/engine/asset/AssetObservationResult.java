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
package org.wcs.smart.connect.query.engine.asset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.asset.query.model.AssetQueryAttachmentResultItem;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.query.common.engine.AttachmentResultSetIterator;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.UuidUtils;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 */
public class AssetObservationResult extends AbstractDbFeatureResultSet implements IPagedImageResultSet {

	private AssetQueryEngine engine;
	private boolean includeUuids;
	
	private String imageDataTable;
	private int imageCount;
	
	public AssetObservationResult(AssetQueryEngine engine, int itemCount, boolean includeUuids) {
		this.engine = engine;
		this.includeUuids = includeUuids;
		setItemCount(itemCount);
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
				if (((AssetQueryResultItem)item).getObservationUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((AssetQueryResultItem)item).getObservationUuid());
			}
			
		};
		QueryColumn wpUuidCol = new QueryColumn(getWaypointColumnName(l), WP_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((AssetQueryResultItem)item).getWaypointUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((AssetQueryResultItem)item).getWaypointUuid());
			}
			
		};
		
		cols.add(obsUuidCol);
		cols.add(wpUuidCol);
		
		return cols;
	}
	
	
	@Override
	public String getGeometryType() {
		return POINT_GEOM_TYPE;
	}
	
	@Override
	public void updateSortColumn(Session session) throws SQLException{
		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "_LIST", "_TREE", "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
		
	@Override
	public List<IAttachmentResultItem> getImageData(int offset, int pageSize){
		throw new UnsupportedOperationException("use getImageIterator"); //$NON-NLS-1$
	}
	@Override
	public int getImageCount() {
		return imageCount;
	}

	protected AssetQueryAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException{
		AssetQueryAttachmentResultItem item = new AssetQueryAttachmentResultItem();
		((AssetObservationEngine)engine).setFields(item, rs);
		setAttachmentField(session, rs, item);
		return item;
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
	public void dispose(Session session) throws SQLException {
		super.dispose(session);
		if (imageDataTable != null) {
			engine.dropTable(session, imageDataTable);
		}
		engine.cleanUp(session);
	}

	private void attachObservations(List<IResultItem> result, Connection c, Session session) throws SQLException {
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.wp_ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable());
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid IN ( "); //$NON-NLS-1$
		
		boolean hasObservations = false;
		List<UUID> uuids = new ArrayList<UUID>();
		for (IResultItem iri : result){
			AssetQueryResultItem it  = (AssetQueryResultItem) iri;
			if (it.getObservationUuid() != null) {
				if (hasObservations) {
					attrSql.append(',');
				}
				hasObservations = true;
				uuids.add(it.getObservationUuid());
				attrSql.append("?"); //$NON-NLS-1$
			}
		}
		if (!hasObservations) return;
		attrSql.append(')');
		
		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		for (int i = 0; i < uuids.size(); i ++){
			ps.setObject(i+1, uuids.get(i));
		}
		try(ResultSet rs = ps.executeQuery()) {
			HashMap<UUID, HashMap<String, Object>> attrMap = getResultsAttributes(rs, session);
			for (IResultItem iri : result){
				AssetQueryResultItem it  = (AssetQueryResultItem) iri;
				if (it.getObservationUuid() != null) {
					HashMap<String, Object> attributes = attrMap.get(it.getObservationUuid());
					if (attributes != null) {
						it.setAttributes(attributes);
					}
				}
			}
		}

	}
	
	@Override
	public ResultSet getResultSet(Session session) {
		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				if(sortColumn != null){
					return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * FROM " + engine.getQueryDataTable() + " ORDER BY sortkeydbl " +direction.sql+ ", sortkeytxt " + direction.sql); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * FROM " + engine.getQueryDataTable()); //$NON-NLS-1$
			}
		});
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
			AssetQueryResultItem it = engine.asQueryResultItem(rs, null);
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
			UUID obUuid = (UUID) rs.getObject(1);
			if (obUuid == null) continue;
			HashMap<String, Object> attributes = attrMap.get(obUuid);
			if (attributes == null) {
				attributes = new HashMap<String, Object>();
				attrMap.put(obUuid, attributes);
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

	
	@Override
	public Geometry createGeometry(IResultItem rs) throws Exception {
		AssetQueryResultItem i = ((AssetQueryResultItem)rs);
		return gf.createPoint(new Coordinate(i.getWaypointX(null), i.getWaypointY(null))); 
	}

	@Override
	public String createId(IResultItem rs) throws Exception {
		return ((AssetQueryResultItem)rs).getWaypointId() + "." + System.nanoTime(); //$NON-NLS-1$
	}
	
	
	private String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		StringBuilder sb = new StringBuilder();

		String[] selectFields = new String[] {
				"ca_id","ca_name","wp_uuid", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"wp_id","wp_x","wp_y","wp_time", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"wp_direction","wp_distance", //$NON-NLS-1$ //$NON-NLS-2$
				"wp_comment","asset_asset","asset_station", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"asset_location","incident_length", //$NON-NLS-1$ //$NON-NLS-2$
				"wp_lastmodified","wp_lastmodifiedbyname" //$NON-NLS-1$ //$NON-NLS-2$
						
		};
		for (String s : selectFields) {
			sb.append(prefix);
			sb.append(s);
			sb.append(","); //$NON-NLS-1$
		}
		
		if (includeObservation) {
			sb.append(prefix);
			sb.append("ob_uuid,"); //$NON-NLS-1$
			sb.append(prefix);
			sb.append("wp_group_uuid"); //$NON-NLS-1$
			for (int i = 0; i < engine.getCategoryCnt(); i ++){
				sb.append(","); //$NON-NLS-1$
				sb.append(prefix);
				sb.append("category_" + i); //$NON-NLS-1$
			}
		
		}else {
			sb.append("cast(null as uuid) as ob_uuid,"); //$NON-NLS-1$
			sb.append("cast(null as uuid) as wp_group_uuid"); //$NON-NLS-1$
			for (int i = 0; i < engine.getCategoryCnt(); i ++){
				sb.append(",cast(null as varchar(32000)) as category_" + i); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}
}
