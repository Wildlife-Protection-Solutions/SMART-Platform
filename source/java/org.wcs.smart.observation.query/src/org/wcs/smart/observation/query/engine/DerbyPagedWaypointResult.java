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
package org.wcs.smart.observation.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.locationtech.jts.geom.Envelope;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AttachmentResultSetIterator;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IDesktopPagedImageResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.AbstractPagedQueryResultSet;
import org.wcs.smart.query.common.ui.image.PagedImageQueryResults;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class DerbyPagedWaypointResult extends AbstractPagedQueryResultSet implements IDesktopPagedImageResultSet{

	private String queryTempTable;
	private Envelope bounds = null;

	//next sort column
	private QueryColumn sortColumn = null;
	private int direction = SWT.UP;
	private DerbyWaypointEngine engine;

	private PagedImageQueryResults imageResults = new PagedImageQueryResults() {
		@Override
		protected void initImageData() {
			DerbyPagedWaypointResult.this.initImageData();
		}
	};
	
	public DerbyPagedWaypointResult(String queryTempTable,DerbyWaypointEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}

	public DerbyPagedWaypointResult(String queryTempTable, int itemCount, DerbyWaypointEngine engine) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.engine = engine;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null ) return false;
		if (getClass() != obj.getClass()) return false;
		DerbyPagedWaypointResult o = (DerbyPagedWaypointResult)obj;
		return Objects.equals(queryTempTable, o.queryTempTable);
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(queryTempTable);
	}
	
	@Override
	public Envelope getEnvelope(){
		if (this.bounds == null){
			try(Session s = HibernateManager.openSession()){
				final String sql = "SELECT min(wp_x), max(wp_x), min(wp_y), max(wp_y) FROM " + queryTempTable; //$NON-NLS-1$
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
			}
		}
		return bounds;
		
	}

	private String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = FixedQueryColumn.getDbColumnName(key);
			
			if (sortColumn.getKey().equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				result = "order by r."+key; //$NON-NLS-1$
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
		this.sortColumn = sortColumn;
		this.direction = direction;
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
	public List<IResultItem> getResults(Session session, ResultSet rs,
			int from, int pageSize) throws SQLException {
		List<IResultItem> items = new ArrayList<IResultItem>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for (int x = from; x < to; x++) {
			rs.next();
			ObservationQueryResultItem it = engine.asQueryResultItem(rs, null);
			items.add(it);
		}
		return items;
	}
	
	/**
	 * Opens a result set in the given session that accessed the query results
	 */
	@Override
	public ResultSet getResultSet(Session session) {
		final String dataSql = "SELECT r.* FROM " + queryTempTable + " r " + buildSortSql(); //$NON-NLS-1$ //$NON-NLS-2$
		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);
			}
		});
	}
	
	@Override
	public void dispose(Session session) throws SQLException {
		super.dispose(session);
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				engine.dropTables(c);
				if (imageResults.getResultsTable() != null) engine.dropTable(c, imageResults.getResultsTable());
			}
		});
	}
	
	@Override
	public IQueryResultSetIterator<? extends IAttachmentResultItem> getImageIterator(Session session) throws SQLException{
		initImageData();

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT r.*, b.attach_uuid as attach_uuid FROM " ); //$NON-NLS-1$
		sb.append(queryTempTable + " r "); //$NON-NLS-1$
		sb.append(" join " + imageResults.getResultsTable() + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("on r.wp_uuid = b.wp_uuid "); //$NON-NLS-1$
		
		return new AttachmentResultSetIterator(session, 
				e->engine.asQueryAttachmentResultItem(e, session),
				()->sb.toString());
	}
	
	@Override
	public List<IAttachmentResultItem> getImageData(int offset, int pageSize) {
		return imageResults.getImageData(offset, pageSize);
	}

	@Override
	public void createTooltip(IAttachmentResultItem data, final Composite parent) {
		WaypointAttachmentTooltipProvider job = new WaypointAttachmentTooltipProvider(data, parent);
		job.schedule();
	}

	@Override
	public int getImageCount() {
		return imageResults.getImageCount();
	}
	
	private synchronized void initImageData() {
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				String imageTempTable = engine.createTempTableName();
				
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE "); //$NON-NLS-1$
				sb.append(imageTempTable);
				sb.append("(attach_uuid char(16) for bit data, wp_uuid char(16) for bit data, seq_order integer GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1))"); //$NON-NLS-1$
				s.createNativeQuery(sb.toString()).executeUpdate();
				
				sb = new StringBuilder();
				sb.append(" INSERT INTO "); //$NON-NLS-1$
				sb.append(imageTempTable + " (attach_uuid, wp_uuid) "); //$NON-NLS-1$
				sb.append(" SELECT z.uuid, z.wp_uuid "); //$NON-NLS-1$
				sb.append("FROM "); //$NON-NLS-1$
				sb.append(" (SELECT distinct e.uuid, a.wp_time, a.wp_id, a.wp_uuid FROM "); //$NON-NLS-1$
				sb.append(queryTempTable);
				sb.append(" a join "); //$NON-NLS-1$
				sb.append("(SELECT uuid, wp_uuid as wp_uuid FROM smart.wp_attachments "); //$NON-NLS-1$
				sb.append(" UNION "); //$NON-NLS-1$
				sb.append("SELECT b.uuid, f.wp_uuid as wp_uuid FROM smart.wp_observation_group f join smart.wp_observation c on f.uuid = c.wp_group_uuid join "); //$NON-NLS-1$
				sb.append("smart.observation_attachment b on c.uuid = b.obs_uuid) e "); //$NON-NLS-1$
				sb.append("on a.wp_uuid = e.wp_uuid"); //$NON-NLS-1$
				sb.append(" ORDER BY a.wp_time desc, a.wp_id ) z "); //$NON-NLS-1$
				
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
}
