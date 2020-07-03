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
package org.wcs.smart.asset.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections.comparators.NullComparator;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StringType;
import org.locationtech.jts.geom.Envelope;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IDesktopPagedImageResultSet;
import org.wcs.smart.query.common.engine.IQueryImageData;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.AbstractPagedQueryResultSet;
import org.wcs.smart.query.common.model.ISearchabledResultSet;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.common.ui.image.PagedImageQueryResults;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

public class AssetPagedWaypointResult extends AbstractPagedQueryResultSet implements IUpdateableResultSet, IWaypointUpdateableResultSet, ISearchabledResultSet, IDesktopPagedImageResultSet{

	private final static NullComparator NULL_COMPARATOR = new NullComparator(false);
	
	protected String queryTempTable;
	protected Envelope bounds = null;

	//next sort column
	protected QueryColumn lastSortColumn = null;
	protected QueryColumn sortColumn = null;
	protected int direction = SWT.UP;
	protected AssetQueryEngine engine;

	//image results
	private PagedImageQueryResults imageResults = new PagedImageQueryResults() {
		@Override
		protected void initImageData() {
			AssetPagedWaypointResult.this.initImageData();
		}
	};
	
	public AssetPagedWaypointResult(String queryTempTable, AssetQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		AssetPagedWaypointResult o = (AssetPagedWaypointResult)obj;
		return Objects.equals(queryTempTable, o.queryTempTable);
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(queryTempTable);
	}
	
	@Override
	public void dispose(Session session)throws SQLException {
		super.dispose(session);
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				engine.dropTables(c);
				if (imageResults.getResultsTable() != null) engine.dropTable(c, imageResults.getResultsTable());
			}
		});
		
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
	
	protected String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = FixedQueryColumn.getDbColumnName(sortColumn.getKey());
			if (sortColumn.getKey().equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r." + key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
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
			AssetQueryResultItem it = engine.asQueryResultItem(rs, null);
			items.add(it);
		}
		
		return items;
	}
	
	/**
	 *Opens a result set in the given session that accessed the query results
	 */
	@Override
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


	protected void updateSortColumn(QueryColumn sortColumn, Session session, Connection c) throws SQLException{
		
	}
	
	@Override
	public boolean canUpdate(Class<? extends IResultItem> item) {
		return (item.equals(AssetQueryResultItem.class));
	}

	@Override
	public boolean update(QueryColumn column, IResultItem item, Object newValue) throws Exception{
		if (!(item instanceof AssetQueryResultItem)) return false;
		if (column instanceof FixedQueryColumn){
			return updateWaypointDetails((FixedQueryColumn)column, (AssetQueryResultItem)item, newValue);
		}
		return false;
	}

	private boolean updateWaypointDetails(FixedQueryColumn column, AssetQueryResultItem item, Object value) throws Exception{
		Waypoint wp = null;
		boolean change = false;
		try(Session s = HibernateManager.openSession()){
			try {
				s.getTransaction().begin();
				wp = (Waypoint) s.get(Waypoint.class, item.getWaypointUuid());
				if (wp != null) {
					switch (column.getColumn()) {
						case WAYPOINT_COMMENT:
							if (value instanceof String) {
								if (((String) value).length() == 0) value = null;
								if (NULL_COMPARATOR.compare(value, wp.getComment()) != 0) {
									change = true;
									updateWaypointComment(wp,(String) value, s);
								}
							}
							break;
						case WAYPOINT_TIME:
							if (value instanceof Date) {
								Date newDate = (Date) value;
								if (!SharedUtils.isSameTime(newDate,
										wp.getDateTime())) {
									change = true;
									updateWaypointTime(wp, newDate, s);
								}
							}
							break;
//						case WAYPOINT_DIRECTION:
//							if (value == null && wp.getDirection() == null) break;
//							if (value != null && wp.getDirection() != null && ((Double)value).floatValue() == wp.getDirection()) break;
//							change = true;
//							updateWaypointDirection(wp, value == null ? null : ((Double)value).floatValue(), s);
//							break;
//						case WAYPOINT_DISTANCE:
//							if (value == null && wp.getDistance() == null) break;
//							if (value != null && wp.getDistance() != null && ((Double)value).floatValue() == wp.getDistance()) break;
//							change = true;
//							updateWaypointDistance(wp, value == null ? null : ((Double)value).floatValue(), s);
//							break;
						case WAYPOINT_ID:
							if (value instanceof Integer) {
								if (!value.equals(wp.getId())) {
									change = true;
									updateWaypointId(wp, (Integer) value, s);
								}
							}
							break;
						case WAYPOINT_X:
							if (value instanceof Double) {
								if (!value.equals(wp.getX())) {
									change = true;
									updateWaypointPosition(wp, (Double) value, wp.getY(), s);
								}
							}
							break;
						case WAYPOINT_Y:
							if (value instanceof Double) {
								if (!value.equals(wp.getX())) {
									change = true;
									updateWaypointPosition(wp, wp.getX(), (Double) value, s);
								}
							}
							break;
						default:
							break;
					}
					
				}
				
				s.flush();
				if (change) {
					updateLastModified(wp, s);
				}
				s.getTransaction().commit();
			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wp);
			getEventBroker().post(AssetEvents.ASSETDATA, null);
			return true;
		}
		return false;
	}
	
	protected void updateLastModified(Waypoint wp, Session s) {
		NativeQuery<?> q = s.createNativeQuery("update " + queryTempTable + " SET wp_lastmodified = :lastmodified, wp_lastmodifiedbyname = :lastmodifiedby WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("lastmodified", wp.getLastModified()); //$NON-NLS-1$
		q.setParameter("lastmodifiedby", SmartLabelProvider.getShortLabel(SmartDB.getCurrentEmployee())); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointPosition(Waypoint wp, double newX, double newY, Session session){
		wp.setRawX(newX);
		wp.setRawY(newY);
		
		NativeQuery<?> q = session.createNativeQuery("update " + queryTempTable + " SET wp_x = :x, wp_y = :y WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("x", newX); //$NON-NLS-1$
		q.setParameter("y", newY); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
		
	}
//	private void updateWaypointDistance(Waypoint wp, Float newDistance, Session session){
//		wp.setDistance(newDistance);
//		
//		
//		NativeQuery<?> q = session.createNativeQuery("update " + queryTempTable + " SET wp_distance = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
//		
//		q.setParameter("id", newDistance, org.hibernate.type.FloatType.INSTANCE); //$NON-NLS-1$
//		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
//		q.executeUpdate();
//	}
//	
//	private void updateWaypointDirection(Waypoint wp, Float newDirection, Session session){
//		wp.setDirection(newDirection);
//		
//		NativeQuery<?> q = session.createNativeQuery("update " + queryTempTable + " SET wp_direction = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
//		q.setParameter("id", newDirection, org.hibernate.type.FloatType.INSTANCE); //$NON-NLS-1$
//		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
//		q.executeUpdate();	
//	}
	
	private void updateWaypointId(Waypoint wp, int newId, Session session){
		wp.setId(newId);
		
		NativeQuery<?> q = session.createNativeQuery("update " + queryTempTable + " SET wp_id = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("id", newId); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointTime(Waypoint wp, Date newTime, Session session){
		Date dttime = SmartUtils.combineDateTime(wp.getDateTime(), newTime);
		wp.setDateTime(dttime);
		
		NativeQuery<?> q = session.createNativeQuery("update " + queryTempTable + " SET wp_date = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("id", dttime); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointComment(Waypoint wp, String newComment, Session session){
		wp.setComment(newComment);
		
		NativeQuery<?> q = session.createNativeQuery("update " + queryTempTable + " SET wp_comment = :cmt WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("cmt", newComment,  StringType.INSTANCE); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		
		q.executeUpdate();
	}
	
	protected IEventBroker getEventBroker() {
		return EclipseContextFactory.getServiceContext(AssetQueryPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
	}
	
	@Override
	public boolean deleteWaypoint(UUID waypointUuid) throws Exception{
		Waypoint wp = null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			try{
				s.getTransaction().begin();
				
				Waypoint wo = (Waypoint) s.get(Waypoint.class, waypointUuid);
				if (wo == null) return false;
				wp = wo;
				
				//delete asset waypoints 
				List<AssetWaypoint> assetWaypoints =  QueryFactory.buildQuery(s, AssetWaypoint.class, new Object[] {"waypoint", wo}).list(); //$NON-NLS-1$
				for (AssetWaypoint aw : assetWaypoints) {
					for (AssetWaypointAttachment attlink : aw.getAttachments()) {
						s.delete(attlink);
					}
					s.delete(aw);
				}
				//delete waypoint
				s.delete(wo);
				s.flush();
				
				//update category names in query results table
				StringBuilder sql = new StringBuilder();
				sql.append(" DELETE FROM " + queryTempTable + " WHERE wp_uuid = :uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				NativeQuery<?> queryUpdate = s.createNativeQuery(sql.toString());
				queryUpdate.setParameter("uuid", waypointUuid); //$NON-NLS-1$
				queryUpdate.executeUpdate();
					
				if (engine instanceof IDerbyWaypointEngine) {
					((IDerbyWaypointEngine) engine).updateResultCount(s, this);
				}
				
				s.getTransaction().commit();
				
			}catch(Exception ex){
				s.getTransaction().rollback();
				throw ex;
			}
		}
		
		WaypointEventManager.getInstance().waypointModified(wp);
		getEventBroker().post(AssetEvents.ASSETDATA, null);
		return true;
	}
	
	
	@Override
	public boolean updateWaypointPosition(AssetQueryResultItem item, Double x, Double y) throws Exception{
		Waypoint wp = null;
		boolean change = false;
		try(Session s = HibernateManager.openSession()){
			try {
				s.getTransaction().begin();
				wp = (Waypoint) s.get(Waypoint.class, item.getWaypointUuid());
				if (wp != null) {
					updateWaypointPosition(wp, x, y, s);
					change = true;
				}
				s.getTransaction().commit();
			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wp);
			getEventBroker().post(AssetEvents.ASSETDATA, null);
			return true;
		}
		return false;
	}
	
	/**
	 * Search for all results items within the bounding box.  The bound
	 * box values must be supplied in lat/lon coordinates
	 */
	@Override
	public List<IResultItem> search(double x1, double y1, double x2, double y2) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM "); //$NON-NLS-1$
		sb.append(queryTempTable);
		sb.append(" WHERE wp_x >= "); //$NON-NLS-1$
		sb.append(Math.min(x1,x2));
		sb.append(" AND wp_x <= "); //$NON-NLS-1$
		sb.append(Math.max(x1,x2));
		sb.append(" AND wp_y >= "); //$NON-NLS-1$
		sb.append(Math.min(y1,y2));
		sb.append(" AND wp_y <= "); //$NON-NLS-1$
		sb.append(Math.max(y1,y2));
		
		List<IResultItem> items = new ArrayList<IResultItem>();
		
		
		try(Session session = HibernateManager.openSession()){
			session.doWork(new Work(){
				@Override
				public void execute(Connection connection) throws SQLException {
					ResultSet rs = connection.createStatement().executeQuery(sb.toString());
					while(rs.next()){
						IResultItem item = engine.asQueryResultItem(rs, session);
						items.add(item);
					}
				}
				
			});
		}catch (Exception ex){
			AssetQueryPlugIn.log(ex.getMessage(), ex);
			return Collections.emptyList();
		}
		
		return items;
	}
	
	@Override
	public List<IQueryImageData> getImageData(int offset, int pageSize) {
		return imageResults.getImageData(offset, pageSize);
	}

	@Override
	public void createTooltip(IQueryImageData data, final Composite parent) {
		AssetAttachmentTooltipProvider job = new AssetAttachmentTooltipProvider(data, parent);
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
				sb.append("(attach_uuid char(16) for bit data, seq_order integer GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1))"); //$NON-NLS-1$
				s.createNativeQuery(sb.toString()).executeUpdate();
				
				sb = new StringBuilder();
				sb.append(" INSERT INTO "); //$NON-NLS-1$
				sb.append(imageTempTable + " (attach_uuid) "); //$NON-NLS-1$
				sb.append(" SELECT z.uuid "); //$NON-NLS-1$
				sb.append("FROM "); //$NON-NLS-1$
				sb.append(" (SELECT distinct e.uuid, a.wp_date, a.wp_id FROM "); //$NON-NLS-1$
				sb.append(queryTempTable);
				sb.append(" a join "); //$NON-NLS-1$
				sb.append("(SELECT uuid, wp_uuid as wp_uuid FROM smart.wp_attachments "); //$NON-NLS-1$
				sb.append(" UNION "); //$NON-NLS-1$
				sb.append("SELECT b.uuid, g.wp_uuid as wp_uuid FROM "); //$NON-NLS-1$
				sb.append(" smart.wp_observation_group g join smart.wp_observation c on c.wp_group_uuid = g.uuid "); //$NON-NLS-1$
				sb.append(" join smart.observation_attachment b on c.uuid = b.obs_uuid) e "); //$NON-NLS-1$
				sb.append("on a.wp_uuid = e.wp_uuid"); //$NON-NLS-1$
				sb.append(" ORDER BY a.wp_date desc, a.wp_id ) z "); //$NON-NLS-1$
				
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
