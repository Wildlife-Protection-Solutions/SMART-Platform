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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.entity.query.model.columns.FixedQueryColumn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.UuidUtils;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class EntityWaypointQueryResult extends AbstractDbFeatureResultSet {

	private PsqlEntityWaypointEngine engine;
	private boolean includeUuids;
	
	public EntityWaypointQueryResult(PsqlEntityWaypointEngine engine, int itemCnt, boolean includeUuids){
		this.engine = engine;
		this.includeUuids = includeUuids;
		setItemCount(itemCnt);
	}
	
	@Override
	public List<QueryColumn> getQueryColumns(SimpleQuery query, Locale l, Session session, IProjectionProvider prj){
		List<QueryColumn> cols = super.getQueryColumns(query, l, session, prj);
		if (!includeUuids) return cols;
		QueryColumn wpUuidCol = new QueryColumn(getWaypointColumnName(l), WP_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((EntityQueryResultItem)item).getWaypointUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((EntityQueryResultItem)item).getWaypointUuid());
			}
		};
		cols.add(wpUuidCol);
		
		QueryColumn caUuidCol = new QueryColumn(getConservationAreaColumnName(l), CA_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((EntityQueryResultItem)item).getConservationAreaUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((EntityQueryResultItem)item).getConservationAreaUuid());
			}
		};
		cols.add(caUuidCol);
		
		return cols;
	}
	
	@Override
	public ResultSet getResultSet(final Session session) {
		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				if(sortColumn != null){
					return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * FROM " + engine.getQueryDataTable() + " ORDER BY sortkeydbl " +direction.sql+ ", sortkeytxt " + direction.sql);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				StringBuilder sb = new StringBuilder();
				sb.append("SELECT * FROM "); //$NON-NLS-1$
				sb.append(engine.getQueryDataTable());
				sb.append(" ORDER BY "); //$NON-NLS-1$
				sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
				sb.append(" DESC "); //$NON-NLS-1$
				
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(sb.toString());
			}
		});
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
	public List<IResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<IResultItem> items = new ArrayList<IResultItem>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			EntityQueryResultItem it = asQueryResultItem(rs);
			items.add(it);
		}
		return items;
	}

	@Override
	public String getGeometryType() {
		return POINT_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(IResultItem rs) throws Exception {
		EntityQueryResultItem i = ((EntityQueryResultItem)rs);
		return gf.createPoint(new Coordinate(i.getWaypointX(null), i.getWaypointY(null))); 
	}

	@Override
	public String createId(IResultItem rs) throws Exception {
		return ((EntityQueryResultItem)rs).getWaypointId() + "." + System.nanoTime(); //$NON-NLS-1$
	}
	
	protected EntityQueryResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		EntityQueryResultItem it = new EntityQueryResultItem();
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setConservationAreaUuid((UUID)rs.getObject("p_ca_uuid")); //$NON-NLS-1$
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
		it.setWaypointUuid((UUID)rs.getObject("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getString("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWpDateTime(rs.getTimestamp("wp_time").toLocalDateTime()); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setLastModifiedDate(rs.getTimestamp("wp_lastmodified").toLocalDateTime()); //$NON-NLS-1$
		it.setLastModifiedBy(rs.getString("wp_lastmodifiedbyname")); //$NON-NLS-1$
		
		return it;
	}
	
	@Override
	public void dispose(Session session) throws SQLException{
		super.dispose(session);
		engine.cleanUp(session);		
	}

	@Override
	public void updateSortColumn(Session session) throws SQLException {
		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "_LIST", "_TREE", "uuid");		 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
}
