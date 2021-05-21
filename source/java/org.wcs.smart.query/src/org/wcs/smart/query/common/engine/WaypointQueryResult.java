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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.locationtech.jts.geom.Envelope;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.AbstractPagedQueryResultSet;
import org.wcs.smart.query.common.model.ISearchabledResultSet;
import org.wcs.smart.query.common.ui.image.PagedImageQueryResults;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Abstract class for query results which represent waypoints
 * 
 * @author Emily
 *
 * @param <T>
 */
//wp_x, wp_y
public abstract class WaypointQueryResult<T extends IWaypointQueryResultItem> extends AbstractPagedQueryResultSet<T> implements IDesktopPagedImageResultSet, ISearchabledResultSet<T>, IColumnInfoProvider{
	
	protected Set<String> dataColumns = null;
	private Envelope bounds = null;

	//sort
	protected  QueryColumn sortColumn = null;
	protected  QueryColumn lastSortColumn = null;
	protected int direction = SWT.UP;
	
	
	protected IDesktopWOEngine<T> engine;

	protected PagedImageQueryResults imageResults;
	
	public WaypointQueryResult(IDesktopWOEngine<T> engine, int itemCount) {
		this.engine = engine;
		this.itemCount = itemCount;
		
		this.imageResults = new PagedImageQueryResults(engine) {		
			@Override
			protected void initImageData() {
				WaypointQueryResult.this.initImageData();			
			}

		};

	}


	@Override
	public abstract void createTooltip(IAttachmentResultItem data, final Composite parent);
	
	protected abstract String buildSortSql() ;
	
	/**
	 * sort columns may be added to the table 
	 * for attribute types etc.
	 */
	protected void updateSortColumn(Session session, Connection c) throws SQLException{
		
	}
	
	
	public IDesktopWOEngine<T> getEngine(){
		return this.engine;
	}
	
	public String getResultsTable() {
		return engine.getQueryDataTable();
	}
	
	/**
	 * Search for all results items within the bounding box.  The bound
	 * box values must be supplied in lat/lon coordinates
	 */
	@Override
	public List<T> search(double x1, double y1, double x2, double y2) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM "); //$NON-NLS-1$
		sb.append(engine.getQueryDataTable());
		sb.append(" WHERE "); //$NON-NLS-1$
		sb.append(" smart.waypointWithin(wp_x, wp_y, wp_distance, wp_direction,"); //$NON-NLS-1$
		sb.append(x1 + "," + y1 + "," + x2 + "," + y2 +")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		List<T> items = new ArrayList<>();
		
		try(Session session = HibernateManager.openSession()){
			session.doWork(new Work(){
				@Override
				public void execute(Connection connection) throws SQLException {
					ResultSet rs = connection.createStatement().executeQuery(sb.toString());
					while(rs.next()){
						T item = engine.asQueryResultItem(rs, session);
						items.add(item);
					}
				}
			});
		}catch (Exception ex){
			QueryPlugIn.log(ex.getMessage(), ex);
			return Collections.emptyList();
		}
		
		return items;
	}
	

	
	@Override
	public Envelope getEnvelope(){
		if (this.bounds == null){
			try(Session s = HibernateManager.openSession()){
				final String sql = "SELECT min(wp_x), max(wp_x), min(wp_y), max(wp_y) FROM " + getResultsTable(); //$NON-NLS-1$
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
	

		
	/**
	 *Opens a result set in the given session that accessed the query results
	 */
	@Override
	public ResultSet getResultSet(Session session) {
 		final String dataSql = "SELECT r.* FROM " + getResultsTable() + " r "+ buildSortSql();  //$NON-NLS-1$ //$NON-NLS-2$
		
		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				if ((lastSortColumn == null && sortColumn != null) || (lastSortColumn != null && sortColumn != null && !lastSortColumn.equals(sortColumn)) ){
					updateSortColumn(session, c);
				}
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.model.IPagedQueryResultSet#setSorting(org.wcs.smart.query.model.observation.QueryColumn, int)
	 */
	public void setSorting(final QueryColumn sortColumn, int direction) {
		if (sortColumn instanceof AttributeQueryColumn) {
			if (((AttributeQueryColumn)sortColumn).getAttributeType() == AttributeType.MLIST){
				return;
			}
		}
		this.lastSortColumn = this.sortColumn;
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
	public List<T> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<T> items = new ArrayList<>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			T it = engine.asQueryResultItem(rs, null);
			items.add(it);
		}
		
		return items;
	}

	@Override
	public boolean isDataColumn(QueryColumn column) {
		return dataColumns != null && dataColumns.contains(column.getKey());
	}
	
	public void setDataColumns(Set<String> dataColumns) {
		this.dataColumns = dataColumns;
	}


	@Override
	public void dispose(Session session) throws SQLException{
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
		sb.append(getResultsTable() + " r "); //$NON-NLS-1$
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
	public int getImageCount() {
		return imageResults.getImageCount();
	}
	
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
				sb.append("a.wp_time, a.wp_id, a.wp_uuid as wp_uuid, "); //$NON-NLS-1$
				sb.append(engine.tablePrefix(WaypointObservation.class) + ".uuid as ob_uuid"); //$NON-NLS-1$
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( getResultsTable() + " a "); //$NON-NLS-1$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(engine.tableNamePrefix(WaypointObservationGroup.class ));
				sb.append(" ON " + engine.tablePrefix(WaypointObservationGroup.class) + ".wp_uuid = a.wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(engine.tableNamePrefix(WaypointObservation.class ));
				sb.append(" ON " + engine.tablePrefix(WaypointObservationGroup.class) + ".uuid = "); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append( engine.tablePrefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(engine.tableNamePrefix(ObservationAttachment.class ));
				sb.append(" ON "); //$NON-NLS-1$
				sb.append( engine.tablePrefix(WaypointObservation.class) + ".uuid = " + engine.tablePrefix(ObservationAttachment.class) + ".obs_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
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
	

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null ) return false;
		if (getClass() != obj.getClass()) return false;
		WaypointQueryResult<T> o = (WaypointQueryResult<T>)obj;
		return Objects.equals(getResultsTable(), o.getResultsTable());
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(getResultsTable());
	}
}
