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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections.comparators.NullComparator;
import org.eclipse.swt.SWT;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.type.StringType;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.AbstractPagedQueryResultSet;
import org.wcs.smart.query.common.model.ISearchabledResultSet;
import org.wcs.smart.query.common.model.IUpdateableResultSet;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Envelope;

public class DerbyPagedWaypointResult extends AbstractPagedQueryResultSet implements IUpdateableResultSet, IWaypointUpdateableResultSet, ISearchabledResultSet{
	
	private static String[][] FIXED_COLUMN_KEY_TO_ROW  = {
		 //NOTE: order is important as we don't want to change "patrolleg" to "pleg"
		{"patrolleg", "pl"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"patrol", "p"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"waypoint", "wp"} //$NON-NLS-1$ //$NON-NLS-2$
	};
	private final static NullComparator NULL_COMPARATOR = new NullComparator(false);
	
	protected String queryTempTable;
	protected Envelope bounds = null;

	//next sort column
	protected QueryColumn lastSortColumn = null;
	protected QueryColumn sortColumn = null;
	protected int direction = SWT.UP;
	protected DerbyPatrolQueryEngine engine;

	public DerbyPagedWaypointResult(String queryTempTable, DerbyPatrolQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		DerbyPagedWaypointResult o = (DerbyPagedWaypointResult)obj;
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
			}
		});
	}

	
	@Override
	public Envelope getEnvelope(){
		if (this.bounds == null){
			Session s = HibernateManager.openSession();
			try{
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
			}finally{
				s.close();
			}
		}
		return bounds;
	}
	
	protected String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
			for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
				key = key.replace(data[0], data[1]);
			}
			if (sortColumn.getType() == ColumnType.STRING){
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
		return (item.equals(PatrolQueryResultItem.class));
	}

	@Override
	public boolean update(QueryColumn column, IResultItem item, Object newValue) throws Exception{
		if (!(item instanceof PatrolQueryResultItem)) return false;
		if (column instanceof FixedQueryColumn){
			return updateWaypointDetails((FixedQueryColumn)column, (PatrolQueryResultItem)item, newValue);
		}
		return false;
	}

	private boolean updateWaypointDetails(FixedQueryColumn column, PatrolQueryResultItem item, Object value) throws Exception{
		Waypoint wp = null;
		Patrol p = null;
		boolean change = false;
		Session s = HibernateManager.openSession();
		try {
			s.getTransaction().begin();
			wp = (Waypoint) s.get(Waypoint.class, item.getWaypointUuid());
			if (wp != null) {
				PatrolWaypoint pw = (PatrolWaypoint) s.createCriteria(PatrolWaypoint.class).add(Restrictions.eq("id.waypoint", wp)).uniqueResult(); //$NON-NLS-1$
				
				if (pw != null) {
					p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
					p.equals(p); //necessary for using outside session
					
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
					case WAYPOINT_DIRECTION:
						if (value instanceof Double) {
							float newValue = ((Double) value).floatValue();
							if (NULL_COMPARATOR.compare(newValue, wp.getDirection()) != 0) {
								change = true;
								updateWaypointDirection(wp, newValue, s);
							}
						}
						break;
					case WAYPOINT_DISTANCE:
						if (value instanceof Double) {
							float newValue = ((Double) value).floatValue();
							if (NULL_COMPARATOR.compare(newValue, wp.getDistance()) != 0) {
								change = true;
								updateWaypointDistance(wp, newValue, s);
							}
						}
						break;
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
			}
			s.getTransaction().commit();
		} catch (Exception ex) {
			s.getTransaction().rollback();
			throw ex;
		} finally {
			s.close();
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wp);
			if (p != null){
				PatrolEventManager.getInstance().patrolSaved(p, true);
			}
			return true;
		}
		return false;
	}
	
	
	private void updateWaypointPosition(Waypoint wp, double newX, double newY, Session session){
		wp.setX(newX);
		wp.setY(newY);
		
		SQLQuery q = session.createSQLQuery("update " + queryTempTable + " SET wp_x = :x, wp_y = :y WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("x", newX); //$NON-NLS-1$
		q.setParameter("y", newY); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
		
	}
	private void updateWaypointDistance(Waypoint wp, float newDistance, Session session){
		wp.setDistance(newDistance);
		
		SQLQuery q = session.createSQLQuery("update " + queryTempTable + " SET wp_distance = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("id", newDistance); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointDirection(Waypoint wp, float newDirection, Session session){
		wp.setDirection(newDirection);
		
		SQLQuery q = session.createSQLQuery("update " + queryTempTable + " SET wp_direction = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("id", newDirection); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();	
	}
	
	private void updateWaypointId(Waypoint wp, int newId, Session session){
		wp.setId(newId);
		
		SQLQuery q = session.createSQLQuery("update " + queryTempTable + " SET wp_id = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("id", newId); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointTime(Waypoint wp, Date newTime, Session session){
		Date dttime = SmartUtils.combineDateTime(wp.getDateTime(), newTime);
		wp.setDateTime(dttime);
		
		SQLQuery q = session.createSQLQuery("update " + queryTempTable + " SET wp_time = :id WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("id", newTime); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void updateWaypointComment(Waypoint wp, String newComment, Session session){
		wp.setComment(newComment);
		
		SQLQuery q = session.createSQLQuery("update " + queryTempTable + " SET wp_comment = :cmt WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("cmt", newComment,  StringType.INSTANCE); //$NON-NLS-1$
		q.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
		
		q.executeUpdate();
	}
	
	@Override
	public boolean deleteWaypoint(UUID waypointUuid) throws Exception{
		Session s = HibernateManager.openSession(new AttachmentInterceptor());
		Waypoint wp = null;
		Patrol p = null;
		try{
			s.getTransaction().begin();
			
			Waypoint wo = (Waypoint) s.get(Waypoint.class, waypointUuid);
			if (wo == null) return false;
			wp = wo;
			
			//find patrol updated for events
			PatrolWaypoint pw = (PatrolWaypoint) s.createCriteria(PatrolWaypoint.class)
					.add(Restrictions.eq("id.waypoint", wp)) //$NON-NLS-1$
					.uniqueResult();
			if (pw == null) throw new Exception("No patrol link found for waypoint.  Waypoint will not be deleted."); //$NON-NLS-1$
			s.delete(pw);
			s.delete(wp);
			
			p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
			p.equals(p);	//required to prevent patrol equals from failing in event manager
			
			s.flush();
			
			//update category names in query results table
			StringBuilder sql = new StringBuilder();
			sql.append(" DELETE FROM " + queryTempTable + " WHERE wp_uuid = :uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			SQLQuery queryUpdate = s.createSQLQuery(sql.toString());
			queryUpdate.setParameter("uuid", waypointUuid); //$NON-NLS-1$
			queryUpdate.executeUpdate();
				
			((DerbyWaypointEngine) engine).updateResultCount(s, this);
			
			s.getTransaction().commit();
			
		}catch(Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}finally{
			s.close();
		}
		
		WaypointEventManager.getInstance().waypointModified(wp);
		if (p != null){
			PatrolEventManager.getInstance().patrolSaved(p, true);
		}
		return true;
	}
	
	
	@Override
	public boolean updateWaypointPosition(PatrolQueryResultItem item, Double x, Double y) throws Exception{
		Waypoint wp = null;
		Patrol p = null;
		boolean change = false;
		Session s = HibernateManager.openSession();
		try {
			s.getTransaction().begin();
			wp = (Waypoint) s.get(Waypoint.class, item.getWaypointUuid());
			if (wp != null) {
				PatrolWaypoint pw = (PatrolWaypoint) s.createCriteria(PatrolWaypoint.class).add(Restrictions.eq("id.waypoint", wp)).uniqueResult(); //$NON-NLS-1$
				
				if (pw != null) {
					p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
					p.equals(p); //necessary for using outside session
					updateWaypointPosition(wp, x, y, s);
					change = true;
				}
			}
			s.getTransaction().commit();
		} catch (Exception ex) {
			s.getTransaction().rollback();
			throw ex;
		} finally {
			s.close();
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wp);
			if (p != null){
				PatrolEventManager.getInstance().patrolSaved(p, true);
			}
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
		
		Session session = HibernateManager.openSession();
		try{
			session.doWork(new Work(){
				@Override
				public void execute(Connection connection) throws SQLException {
					ResultSet rs = connection.createStatement().executeQuery(sb.toString());
					while(rs.next()){
						System.out.println("item"); //$NON-NLS-1$
						IResultItem item = engine.asQueryResultItem(rs, session);
						items.add(item);
					}
				}
				
			});
		}catch (Exception ex){
			PatrolQueryPlugIn.log(ex.getMessage(), ex);
			return Collections.emptyList();
		}finally{
			session.close();
		}
		
		return items;
	}
}
