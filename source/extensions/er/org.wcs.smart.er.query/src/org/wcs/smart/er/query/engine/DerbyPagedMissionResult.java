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

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.engine.IResultItem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Paged result set for mission queries. 
 * 
 * @author Emily
 *
 */
public class DerbyPagedMissionResult extends AbstractSurveyPagedResult {

	private int missionCnt;
	
	public DerbyPagedMissionResult(String queryTempTable, DerbySurveyQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}

	public DerbyPagedMissionResult(String queryTempTable, int itemCount, DerbySurveyQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.engine = engine;
	}
	
	/**
	 * Sets the unique mission cnt
	 * @param cnt
	 */
	public void setMissionCnt(int cnt){
		this.missionCnt = cnt;
	}
	
	/**
	 * 
	 * @return the unique mission cnt
	 */
	public int getMissionCnt(){
		return this.missionCnt;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DerbyPagedMissionResult) {
			if (queryTempTable == null)
				return super.equals(obj);
			DerbyPagedMissionResult r2 = (DerbyPagedMissionResult) obj;
			return queryTempTable.equals(r2.queryTempTable);
		}
		return super.equals(obj);
	}
	
	@Override
	public List<IResultItem> getResults(final Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		final List<IResultItem> items = new ArrayList<IResultItem>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			IResultItem it = engine.asQueryResultItem(rs, session);
			items.add(it);
		}
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				attachMissionProperties(items, c, session);
			}
		});
		return items;
	}

	/**
	 * Opens a result set in the given session that accessed the query results
	 */
	@Override
	public ResultSet getResultSet(final Session session) {
		final String dataSql = "SELECT r.* FROM " + queryTempTable + " r " + buildSortSql(); //$NON-NLS-1$ //$NON-NLS-2$

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

	
	@Override
	public Envelope getEnvelope(){
		if (this.bounds == null){
			Session s = HibernateManager.openSession();
			try{
				final String sql = "SELECT geometry FROM smart.mission_track where mission_day_uuid in (SELECT mission_day_uuid FROM " + queryTempTable + " )"; //$NON-NLS-1$ //$NON-NLS-2$
				s.doWork(new Work(){
	
					@Override
					public void execute(Connection c) throws SQLException {
						WKBReader reader = new WKBReader();
						Envelope results = null;
						try(ResultSet q = c.createStatement().executeQuery(sql)){
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
			}finally{
				s.close();
			}
		}
		return bounds;	
	}
	
	@Override
	public String[] getTemporaryTableNames() {
		return new String[]{queryTempTable};
	}

}
