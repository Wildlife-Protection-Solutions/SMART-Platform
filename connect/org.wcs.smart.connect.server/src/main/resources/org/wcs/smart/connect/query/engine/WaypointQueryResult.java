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
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.query.common.engine.AttachmentResultSetIterator;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.UuidUtils;

/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public abstract class WaypointQueryResult<T extends WaypointQueryResultItem> extends AbstractDbFeatureResultSet<T> implements IPagedImageResultSet {

	protected IWOEngine<T> engine;
	protected boolean includeUuids = false;
	
	protected String imageDataTable;
	protected int imageCount;
	
	public WaypointQueryResult(IWOEngine<T >engine, int itemCnt, boolean includeUuids){
		this.engine = engine;
		this.includeUuids = includeUuids;
		setItemCount(itemCnt);
	}
	
	
	protected abstract IAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException;
	
	protected abstract T asQueryResultItem(ResultSet rs) throws SQLException;
	
	public String getDefaultSortBy() {
		StringBuilder sb = new StringBuilder();
		sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
		sb.append(" DESC " ); //$NON-NLS-1$
		return sb.toString();
	}
	
	@Override
	public List<QueryColumn> getQueryColumns(SimpleQuery query, Locale l, Session session, IProjectionProvider prj){
		List<QueryColumn> cols = super.getQueryColumns(query, l, session, prj);
		if (!includeUuids) return cols;
		
		QueryColumn caUuidCol = new QueryColumn(getConservationAreaColumnName(l), CA_UUID_COL_KEY, QueryColumn.ColumnType.STRING) {
			@Override
			public QueryColumn clone() { return this; }
			@Override
			public Object getValue(IResultItem item) {
				if (((WaypointQueryResultItem)item).getConservationAreaUuid() == null) return ""; //$NON-NLS-1$
				return UuidUtils.uuidToString( ((WaypointQueryResultItem)item).getConservationAreaUuid());
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
							ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * FROM " + engine.getQueryDataTable() + " ORDER BY sortkeydbl " +direction.sql+ ", sortkeytxt " + direction.sql); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				//default sort by waypoint date/time
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT * FROM "); //$NON-NLS-1$
				sql.append(engine.getQueryDataTable());
				sql.append(" ORDER BY "); //$NON-NLS-1$
				sql.append(getDefaultSortBy());
				
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, 
						ResultSet.CONCUR_READ_ONLY).executeQuery(sql.toString());
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
	public List<T> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<T> items = new ArrayList<>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			items.add(asQueryResultItem(rs));
		}
		return items;
	}

	@Override
	public String getGeometryType() {
		return POINT_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(T rs) throws Exception {
		WaypointQueryResultItem i = ((WaypointQueryResultItem)rs);
		return gf.createPoint(new Coordinate(i.getWaypointX(null), i.getWaypointY(null))); 
	}

	@Override
	public String createId(T rs) throws Exception {
		return ((WaypointQueryResultItem)rs).getWaypointId() + "." + System.nanoTime(); //$NON-NLS-1$
	}
	
	protected void setFields(T it, ResultSet rs) throws SQLException{
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setConservationAreaUuid((UUID)rs.getObject("ca_uuid")); //$NON-NLS-1$
		it.setWaypointUuid((UUID)rs.getObject("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getString("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointDateTime(rs.getTimestamp("wp_time").toLocalDateTime()); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setLastModifiedDate(rs.getTimestamp("wp_lastmodified").toLocalDateTime()); //$NON-NLS-1$
		it.setLastModifiedBy(rs.getString("wp_lastmodifiedbyname")); //$NON-NLS-1$
	}
	
	@Override
	public List<IAttachmentResultItem> getImageData(int offset, int pageSize){
		throw new UnsupportedOperationException("use getImageIterator"); //$NON-NLS-1$
	}
	
	@Override
	public int getImageCount() {
		return imageCount;
	}

	@Override
	public IQueryResultSetIterator<? extends IAttachmentResultItem> getImageIterator(Session session) throws SQLException{
		
		imageDataTable = engine.createTempTableName();
		imageCount = createImageDataWaypoint(session, engine.getQueryDataTable(), imageDataTable);
		
		String query = getImageQueryWaypoint(engine.getQueryDataTable(), imageDataTable);
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

	@Override
	public void updateSortColumn(Session session) throws SQLException {
		//this is a weird one, it doesn't create any *_list or *_tree tables at all, not sure why yet... the last false just triggers an unsupported exception 
		updateSortColumnGeneral(session, engine.getQueryDataTable(), engine.getCaFilter(), "value", ".ob_", "", "", "uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
	}
}