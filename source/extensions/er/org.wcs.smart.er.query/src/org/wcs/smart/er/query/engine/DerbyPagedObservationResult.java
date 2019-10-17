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
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IDesktopPagedImageResultSet;
import org.wcs.smart.query.common.engine.IQueryImageData;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.common.ui.image.PagedImageQueryResults;
import org.wcs.smart.query.model.QueryColumn;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyPagedObservationResult extends AbstractSurveyPagedResult implements IObservationPagedQueryResultSet, ISurveyQueryMissionResult, IDesktopPagedImageResultSet{

	private int wpCount = 0;

	private Set<String> dataColumns = null;
	
	private PagedImageQueryResults imageResults = new PagedImageQueryResults() {
		
		@Override
		protected void initImageData() {
			DerbyPagedObservationResult.this.initImageData();
		}
	};
	
	public DerbyPagedObservationResult(String queryTempTable, DerbySurveyQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}

	public DerbyPagedObservationResult(String queryTempTable, int itemCount, int wpCount, DerbySurveyQueryEngine engine) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.wpCount = wpCount;
		this.engine = engine;
	}
	
	public DerbySurveyQueryEngine getEngine() {
		return this.engine;
	}
	
	@Override
	public String getResultsTable() {
		return queryTempTable;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DerbyPagedObservationResult o = (DerbyPagedObservationResult)obj;
		return Objects.equals(queryTempTable, o.queryTempTable);
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(queryTempTable);
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
				attachObservations(items, c, session);
				attachMissionProperties(items, c, session);
				attachSamplingUnitAttributes(items, c, session);
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
	public int getWpCount() {
		return wpCount;
	}

	protected void setWpCount(int wpCount) {
		this.wpCount = wpCount;
	}

	@Override
	public boolean isDataColumn(QueryColumn column) {
		return dataColumns != null && dataColumns.contains(column.getKey());
	}
	
	public void setDataColumns(Set<String> dataColumns) {
		this.dataColumns = dataColumns;
	}

	@Override
	public void dispose(Session session) throws SQLException {
		super.dispose(session);
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				if (imageResults.getResultsTable() != null) engine.dropTable(c, imageResults.getResultsTable());

			}
		});
	}

	@Override
	public List<IQueryImageData> getImageData(int offset, int pageSize) {
		return imageResults.getImageData(offset, pageSize);
	}

	@Override
	public void createTooltip(IQueryImageData data, final Composite parent) {
		SurveyAttachmentTooltipProvider job = new SurveyAttachmentTooltipProvider(data, parent);
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
				sb.append(imageTempTable + "(attach_uuid) "); //$NON-NLS-1$
				sb.append(" SELECT z.attach_uuid FROM ( "); //$NON-NLS-1$
				sb.append("SELECT c.uuid as attach_uuid, a.wp_date, a.wp_id " ); //$NON-NLS-1$
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( queryTempTable + " a "); //$NON-NLS-1$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(" smart.observation_attachment c on a.ob_uuid = c.obs_uuid "); //$NON-NLS-1$
				sb.append( " UNION "); //$NON-NLS-1$
				sb.append("SELECT c.uuid as attach_uuid, a.wp_date, a.wp_id " ); //$NON-NLS-1$
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( queryTempTable + " a "); //$NON-NLS-1$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(" smart.wp_attachments c on c.wp_uuid = a.wp_uuid "); //$NON-NLS-1$
				sb.append(" ) z ORDER BY z.wp_date desc, z.wp_id "); //$NON-NLS-1$
				
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