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
package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.WKBReader;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.model.ISurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.model.column.SamplingUnitAttributeQueryColumn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.model.AbstractPagedQueryResultSet;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Paged result set for mission queries. 
 * 
 * @author Emily
 *
 */
public class DerbyPagedMissionResult extends AbstractPagedQueryResultSet<ISurveyQueryResultItem> {

	private int missionCnt;
	private int itemCnt;
	
	private DerbyMissionEngine engine;
	private DerbyMissionTrackEngine engine2;
	protected boolean hasSortColumns = false;

	
	private Envelope bounds;
	
	protected  QueryColumn sortColumn = null;
	protected  QueryColumn lastSortColumn = null;
	protected int direction = SWT.UP;
	
	
	public DerbyPagedMissionResult(DerbyMissionEngine engine) {
		this.engine = engine;
	}

	public DerbyPagedMissionResult(DerbyMissionTrackEngine engine) {
		this.engine2 = engine;
	}
	
	/**
	 * Sets the unique mission cnt
	 * @param cnt
	 */
	public void setMissionCnt(int cnt){
		this.missionCnt = cnt;
	}
	
	public void setItemCnt(int cnt) {
		this.itemCnt = cnt;
	}
	
	/**
	 * 
	 * @return the unique mission cnt
	 */
	public int getMissionCnt(){
		return this.missionCnt;
	}
	
	public int getItemCnt() {
		return this.itemCnt;
	}
	
	@Override
	public void dispose(Session session) throws SQLException{
		super.dispose(session);
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				if (engine != null) engine.dropTables(c);
				if (engine2 != null) engine2.dropTables(c);

			}
		});
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DerbyPagedMissionResult o = (DerbyPagedMissionResult)obj;
		if (engine2 != null) {
			return Objects.equals(engine2.getQueryDataTable(), o.engine2.getQueryDataTable());
		}
		return Objects.equals(engine.getQueryDataTable(), o.engine.getQueryDataTable());
	}
	
	@Override
	public int hashCode(){
		if (engine2 != null) return Objects.hash(engine2.getQueryDataTable()); 
		return Objects.hash(engine.getQueryDataTable());
	}
	
	
	@Override
	public List<ISurveyQueryResultItem> getResults(final Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		final List<ISurveyQueryResultItem> items = new ArrayList<>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			ISurveyQueryResultItem it = engine == null ? engine2.asQueryResultItem(rs, session) : engine.asQueryResultItem(rs, session);
			items.add(it);
		}
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				SurveyPagedResultUtils.attachMissionProperties( items, c, session);
				SurveyPagedResultUtils.attachSamplingUnitAttributes(items, c, session);
			}
		});
		return items;
	}

	
	protected String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE) {
			//default sort by mission start time
			StringBuilder sb = new StringBuilder();
			sb.append("ORDER BY "); //$NON-NLS-1$
			sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION_START.getKey()));
			sb.append(" DESC, "); //$NON-NLS-1$
			sb.append(SurveyQueryColumn.getDbColumnName(SurveyQueryColumn.FixedColumns.MISSION.getKey()));
			return sb.toString();
			
		}
		return SurveyPagedResultUtils.buildSortSql(sortColumn, direction);
	}
	
	/**
	 * Opens a result set in the given session that accessed the query results
	 */
	@Override
	public ResultSet getResultSet(final Session session) {
		String dataTable = engine != null ? engine.getQueryDataTable() : engine2.getQueryDataTable();
		
		final String dataSql = "SELECT r.* FROM " + dataTable + " r " + buildSortSql(); //$NON-NLS-1$ //$NON-NLS-2$

		return session.doReturningWork(new ReturningWork<ResultSet>() {

			@Override
			public ResultSet execute(Connection c) throws SQLException {
				if ((lastSortColumn == null && sortColumn != null)
						|| (lastSortColumn != null && sortColumn != null && !lastSortColumn
								.equals(sortColumn))) {
					updateSortColumn(sortColumn, session, c);
				}
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);
			}
		});
	}

	
	private void updateSortColumn(QueryColumn sortColumn, Session session, Connection c) throws SQLException{
		if (sortColumn instanceof MissionPropertyQueryColumn ||
			sortColumn instanceof SamplingUnitAttributeQueryColumn) {
			
			if (engine != null) {
				SurveyPagedResultUtils.processSortColumn(engine.getQueryDataTable(), 
					engine.getQueryLabelTable(), engine, hasSortColumns, sortColumn, c, session);
				hasSortColumns = true;
			}else if (engine2 != null) {
				SurveyPagedResultUtils.processSortColumn(engine2.getQueryDataTable(), 
						engine2.getQueryLabelTable(), engine2, hasSortColumns, sortColumn, c, session);
				hasSortColumns = true;
			}
		}
		c.commit();
		lastSortColumn = sortColumn;
	}
	
	@Override
	public Envelope getEnvelope(){
		if (this.bounds == null){
			try(Session s = HibernateManager.openSession()){
				StringBuilder sb = new StringBuilder();
				
				if (engine != null) {
					sb.append("SELECT geometry FROM smart.mission_track "); //$NON-NLS-1$
					sb.append("where mission_day_uuid in "); //$NON-NLS-1$
					sb.append("(SELECT day.uuid FROM smart.mission_day day join "); //$NON-NLS-1$
					sb.append(engine.getQueryDataTable());
					sb.append(" q on q.mission_uuid = day.mission_uuid"); //$NON-NLS-1$
					sb.append(" )"); //$NON-NLS-1$
				}else{
					sb.append("SELECT geometry FROM smart.mission_track "); //$NON-NLS-1$
					sb.append("where missionday_uuid in (SELECT mission_day_uuid FROM "); //$NON-NLS-1$
					sb.append(engine2.getQueryDataTable());
					sb.append(" )"); //$NON-NLS-1$
				}
				
				
				s.doWork(new Work(){
	
					@Override
					public void execute(Connection c) throws SQLException {
						WKBReader reader = new WKBReader();
						Envelope results = null;
						try(ResultSet q = c.createStatement().executeQuery(sb.toString())){
							while(q.next()){
								byte[] ob = q.getBytes(1);
								if (ob != null && ob.length > 0){
									LineString ls = (LineString)reader.read(ob);
									if (results == null){
										results = ls.getEnvelopeInternal();
									}else{
										results.expandToInclude(ls.getEnvelopeInternal());
									}
								}
							}
						}catch (Exception ex){
							ERQueryPlugIn.log(ex.getMessage(), ex);
						}
						bounds = results;
					}	
				});
			}
		}
		return bounds;	
	}

	@Override
	public void setSorting(QueryColumn sortColumn, int direction) {
		this.lastSortColumn = this.sortColumn;
		this.sortColumn = sortColumn;
		this.direction = direction;		
	}
}
