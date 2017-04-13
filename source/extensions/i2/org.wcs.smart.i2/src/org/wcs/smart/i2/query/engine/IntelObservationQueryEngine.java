/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query.engine;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.DataModelColumn;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Query engine for intelligence observation queries.
 * 
 * @author Emily
 *
 */
public class IntelObservationQueryEngine {

	
	private IntelObservationQueryResults queryResults;
	
	/**
	 * Parameters required are session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	public IPagedQueryResultSet executeQuery(IntelRecordObservationQuery query,  HashMap<String, Object> parameters) throws Exception{
		
		Session session = (Session) parameters.get(Session.class.getName());
		IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		if (monitor == null){
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(Messages.IntelObservationQueryEngine_Progress1, 6);
		//one or both element of array may be null
		Date[] dfilter = (Date[]) parameters.get(Date.class.getName());
		if (dfilter == null) return null;
		
		Locale locale = (Locale) parameters.get(Locale.class.getName());
		if (locale == null){
			locale = Locale.getDefault();
		}
		
		ConservationArea ca = (ConservationArea)parameters.get(ConservationArea.class.getName());
		if (ca == null){
			 throw new Exception(Messages.IntelObservationQueryEngine_InvalidCaParameter);
		}
		monitor.subTask(Messages.IntelObservationQueryEngine_Progress2);
		ParsedObservationQuery parsedQuery = IntelRecordObservationQuery.parseQuery(query.getQueryString());
		monitor.worked(1);
		final IProgressMonitor fmonitor = monitor;
		final Locale flocale = locale;
		return session.doReturningWork(new ReturningWork<IPagedQueryResultSet>() {
			@Override
			public IPagedQueryResultSet execute(Connection connection) throws SQLException {
				connection.setAutoCommit(true);
				try{
					if (parsedQuery.getFilterType() == IQueryFilter.FilterType.OBSERVATION){
						queryResults = new IntelObservationQueryResults();
						
						//session.beginTransaction();
						ObservationFilterProcessor parser = new ObservationFilterProcessor(parsedQuery.getFilter(), dfilter, ca, session); 
						String dataTable = parser.processFilter(new SubProgressMonitor(fmonitor, 2));
						if (fmonitor.isCanceled()) return null;
						queryResults.setFilterToColumnMap(parser.getFilterToColumnNames());
						
						if (fmonitor.isCanceled()) return null;
						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress3);
						configureTableContents(dataTable, parser.getFilterToColumnNames(), true, session);
						
						String sql = "DROP TABLE " + dataTable; //$NON-NLS-1$
						SqlGenerator.logString(sql);
						session.createSQLQuery(sql).executeUpdate();
						fmonitor.worked(1);
						
						if (fmonitor.isCanceled()) return null;
						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress4);
						computeQueryColumns(session, flocale, query);
						fmonitor.worked(1);
						
						if (fmonitor.isCanceled()) return null;
						computeCount(session);
						computeBounds(session);
						
						fmonitor.worked(1);
						
						//session.getTransaction().commit();
						return queryResults;
					}else if (parsedQuery.getFilterType() == IQueryFilter.FilterType.WAYPOINT){
						queryResults = new IntelObservationQueryResults();
						
						//session.beginTransaction();
						WaypointFilterProcessor parser = new WaypointFilterProcessor(parsedQuery.getFilter(), dfilter, ca, session); 
						String dataTable = parser.processFilter(new SubProgressMonitor(fmonitor, 2));
						if (fmonitor.isCanceled()) return null;
						queryResults.setFilterToColumnMap(parser.getFilterToColumnNames());
						
						if (fmonitor.isCanceled()) return null;
						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress5);
						configureTableContents(dataTable, parser.getFilterToColumnNames(), false, session);
						
						String sql = "DROP TABLE " + dataTable; //$NON-NLS-1$
						SqlGenerator.logString(sql);
						session.createSQLQuery(sql).executeUpdate();
						fmonitor.worked(1);
						
						if (fmonitor.isCanceled()) return null;
						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress6);
						computeQueryColumns(session, flocale, query);
						fmonitor.worked(1);
						
						if (fmonitor.isCanceled()) return null;
						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress7);
						computeCount(session);
						computeBounds(session);
						
						fmonitor.worked(1);
						
						//session.getTransaction().commit();
						return queryResults;
						//process waypoint filter
					}
				}catch (Exception ex){
					throw new SQLException(ex);
				}finally{
					connection.setAutoCommit(false);
				}
				return null;
			}
		});		
	}
	
	private void computeCount(Session session){
		Integer obs = (Integer) session.createSQLQuery("SELECT count(*) FROM " + queryResults.getQueryDataTable()).uniqueResult(); //$NON-NLS-1$
		Integer wp = (Integer) session.createSQLQuery("SELECT count(distinct location_uuid) FROM " + queryResults.getQueryDataTable()).uniqueResult(); //$NON-NLS-1$
		queryResults.setResultCount(obs, wp);
	}
	
	private void computeBounds(Session session){
//		very slow: Query q = session.createSQLQuery("SELECT geometry FROM smart.i_location WHERE uuid in (select location_uuid FROM " + queryResults.getQueryDataTable() + ")");
		Query q = session.createSQLQuery("SELECT loc_geometry FROM " + queryResults.getQueryDataTable() ); //$NON-NLS-1$
		List<?> geoms = q.list();
		Envelope env = null;
		for (Object x : geoms){
			if (x == null)  continue;
			if (!(x instanceof Blob))  continue;
			
			Blob b = (Blob)x;
			WKBReader reader = new WKBReader();
			try{
				Envelope e = reader.read(b.getBytes(1l, (int) b.length())).getEnvelopeInternal();
				if (env == null){
					env = e;
				}else{
					env.expandToInclude(e);
				}
			}catch(Exception ex){
				//eat this
			}
		}
		queryResults.setBounds(env);
	}
	/*
	 * Configures the query columns; removing non populated attribute columns
	 */
	@SuppressWarnings("unchecked")
	private void computeQueryColumns(Session session, Locale locale, IntelRecordObservationQuery query) throws Exception{
		List<IQueryColumn> columns = IntelQueryColumnProvider.getInstance().getQueryColumns(query, locale, session);
		//remove unused attribute columns
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT distinct a.keyid FROM "); //$NON-NLS-1$
		sb.append(" smart.dm_attribute a join smart.i_observation_attribute b ON a.uuid = b.attribute_uuid "); //$NON-NLS-1$
		sb.append(" join " + queryResults.getQueryDataTable() + " c on c.observation_uuid = b.observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		List<String> populatedAttributes = session.createSQLQuery(sb.toString()).list();
		for (Iterator<IQueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
			IQueryColumn column = (IQueryColumn) iterator.next();
			if (column instanceof DataModelColumn ){
				String attribute = ((DataModelColumn)column).getAttributeKey();
				if (attribute != null && !populatedAttributes.contains(attribute)){
					//iterator.remove();
					((DataModelColumn) column).setVisible(false);
				}
			}
			
		}
		queryResults.setQueryColumns(columns);
	}
	
	/*
	 * create the results table from the data table add necessary results columns
	 */
	@SuppressWarnings("unchecked")
	private void configureTableContents(String observationTable, HashMap<IQueryFilter, String> filterToColumn, boolean obsFilter, Session session){
		
		HashMap<String, Integer> columnNameToIndex = new HashMap<>();
		
		String newTable = SqlGenerator.createTempTableName();
		
		String[][] columns = new String[][]{
			{"observation_uuid", "char(16) for bit data"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"location_uuid", "char(16) for bit data"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"record_uuid", "char(16) for bit data"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"record_status", "varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"record_title", "varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"loc_id", "varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"loc_datetime", "timestamp"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"loc_comment", "varchar(4096)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"loc_geometry", "blob"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"category_uuid", "char(16) for bit data"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		String[][] sortColumns = new String[][]{
			{"str_sort", "varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"dbl_sort", "double"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"date_sort", "date"}		 //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		StringBuilder insert = new StringBuilder();
		StringBuilder select = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		int columnIndex = 0;
		sb.append("CREATE TABLE " + newTable + " ( "); //$NON-NLS-1$ //$NON-NLS-2$
		for (String[] c : columns){
			sb.append( c[0] + " " + c[1] + ","); //$NON-NLS-1$ //$NON-NLS-2$
			columnNameToIndex.put(c[0], columnIndex++);
			insert.append(c[0] + ","); //$NON-NLS-1$
		}
		for (String[] c : sortColumns){
			sb.append( c[0] + " " + c[1] + ","); //$NON-NLS-1$ //$NON-NLS-2$
			columnNameToIndex.put(c[0], columnIndex++);
		}
		for (String v : filterToColumn.values()){
			sb.append( v + " boolean,"); //$NON-NLS-1$
			columnNameToIndex.put(v, columnIndex++);
			insert.append(v + ","); //$NON-NLS-1$
			select.append(", a." + v ); //$NON-NLS-1$
		}
		
		insert.deleteCharAt(insert.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")"); //$NON-NLS-1$
		
		SqlGenerator.logString(sb.toString());
		session.createSQLQuery(sb.toString()).executeUpdate();
		
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO " + newTable + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" ( " + insert.toString() + ")" ); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("SELECT o.uuid, a.location_uuid, r.uuid, r.status, r.title, l.id, l.datetime, l.comment, l.geometry, o.category_uuid "); //$NON-NLS-1$
		sb.append(select);
		sb.append(" FROM " + observationTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" JOIN smart.i_location l on a.location_uuid = l.uuid "); //$NON-NLS-1$
		sb.append(" JOIN smart.i_record r on r.uuid = l.record_uuid "); //$NON-NLS-1$
		sb.append(" LEFT JOIN smart.i_observation o on o.location_uuid = a.location_uuid "); //$NON-NLS-1$
		if (obsFilter){
			sb.append(" AND o.uuid = a.observation_uuid "); //$NON-NLS-1$
		}
		SqlGenerator.logString(sb.toString());
		session.createSQLQuery(sb.toString()).executeUpdate();
		
		
		//add category details
		sb = new StringBuilder();
		sb.append("SELECT distinct category_uuid FROM " + newTable + " WHERE category_uuid is not null "); //$NON-NLS-1$ //$NON-NLS-2$
		List<byte[]> categories = session.createSQLQuery(sb.toString()).list();
		HashMap<Category, List<String>> categoryItems = new HashMap<>();
		int maxCategoryLength = 0;
		for (byte[] c : categories){
			Category category = (Category) session.get(Category.class, UuidUtils.byteToUUID(c));
			List<String> labels = parseCategoryLabels(category);
			maxCategoryLength = Math.max(maxCategoryLength,labels.size());
			
			categoryItems.put(category, labels);
		}
		
		if (maxCategoryLength >= 0){
			for (int i = 0; i < maxCategoryLength; i ++){
				session.createSQLQuery("ALTER TABLE " + newTable + " ADD column category_" + i + " varchar(1024) ").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				columnNameToIndex.put("category_" + i, columnIndex++); //$NON-NLS-1$
			}
			for (Entry<Category, List<String>> update : categoryItems.entrySet()){
				sb = new StringBuilder();
				sb.append("UPDATE " + newTable ); //$NON-NLS-1$
				sb.append(" SET "); //$NON-NLS-1$
				
				for (int i = 0; i < update.getValue().size(); i ++){
					if (i > 0){
						sb.append(","); //$NON-NLS-1$
					}
					sb.append("category_" + i + " = :c" + i); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sb.append(" WHERE category_uuid = :uuid"); //$NON-NLS-1$
				SQLQuery query = session.createSQLQuery(sb.toString());
				query.setParameter("uuid", update.getKey().getUuid()); //$NON-NLS-1$
				
				for (int i = 0; i < update.getValue().size(); i ++){
					query.setParameter("c" + i, update.getValue().get(i)); //$NON-NLS-1$
				}
				query.executeUpdate();
			}
		}
		
		queryResults.setResultsTable(newTable);
		queryResults.setCategoryCount(maxCategoryLength);
		queryResults.setColumnNameToIndexMap(columnNameToIndex);
		
	}
	
	private List<String> parseCategoryLabels(Category c){
		List<String> labels = new ArrayList<>();
		while(c != null){
			labels.add(0, c.getName());
			c = c.getParent();
		}
		return labels;
	}
	
}
