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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.query.CaQueryItemProvider;
import org.wcs.smart.i2.query.DesktopCcaaQueryItemProvider;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.IntelAttributeQueryColumn;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;

/**
 * Query engine for intelligence observation queries.
 * 
 * @author Emily
 *
 */
public class IntelEntityRecordQueryEngine implements IIntelQueryEngine {

	
	private IntelEntityRecordQueryResults queryResults;
	
	/**
	 * Parameters required are session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	public IPagedQueryResultSet executeQuery(AbstractIntelQuery iquery,  HashMap<String, Object> parameters) throws Exception{
		
		IntelEntityRecordQuery query = (IntelEntityRecordQuery) iquery;
		
		Session session = (Session) parameters.get(Session.class.getName());
		IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		SubMonitor progress = SubMonitor.convert(monitor, Messages.IntelObservationQueryEngine_Progress1, 6);

		Locale locale = (Locale) parameters.get(Locale.class.getName());
		if (locale == null){
			locale = Locale.getDefault();
		}
		
		Collection<ConservationArea> cas = (Collection<ConservationArea>)parameters.get(ConservationArea.class.getName());
		if (cas == null){
			 throw new Exception(Messages.IntelObservationQueryEngine_InvalidCaParameter);
		}
		IQueryItemProvider itemProvider = new DesktopCcaaQueryItemProvider(cas, query.getConservationArea());
		if (cas.size() == 1) {
			itemProvider = new CaQueryItemProvider(cas.iterator().next(), query.getConservationArea());
		}
		
		progress.subTask(Messages.IntelObservationQueryEngine_Progress2);
		ParsedObservationQuery parsedQuery = IntelEntityRecordQuery.parseQuery(query.getQueryString());
		progress.worked(1);
		final SubMonitor fmonitor = progress;
		final Locale flocale = locale;
		final IQueryItemProvider fItemProvider = itemProvider;
		return session.doReturningWork(new ReturningWork<IPagedQueryResultSet>() {
			@Override
			public IPagedQueryResultSet execute(Connection connection) throws SQLException {
				
				connection.setAutoCommit(true);
				try{
					if (parsedQuery.getFilterType() == IQueryFilter.FilterType.OBSERVATION){
						queryResults = new IntelEntityRecordQueryResults();
						
						//session.beginTransaction();
						EntityRecordObservationFilterProcessor parser = new EntityRecordObservationFilterProcessor(parsedQuery.getFilter(), fItemProvider, session); 
						String dataTable = parser.processFilter(fmonitor.split(2));
						
						queryResults.setFilterToColumnMap(parser.getFilterToColumnNames());
						
						fmonitor.checkCanceled();
						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress3);
						configureTableContents(dataTable, parser.getFilterToColumnNames(), true, fItemProvider, session);
						
//						String sql = "DROP TABLE " + dataTable; //$NON-NLS-1$
//						SqlGenerator.logString(sql);
//						session.createNativeQuery(sql).executeUpdate();
//						fmonitor.worked(1);
						
						fmonitor.checkCanceled();
						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress4);
						computeQueryColumns(session, flocale, query, fItemProvider);
						fmonitor.worked(1);
						
						fmonitor.checkCanceled();
						computeCount(session);
						
						fmonitor.worked(1);
						
						return queryResults;
					}else if (parsedQuery.getFilterType() == IQueryFilter.FilterType.WAYPOINT){
						queryResults = new IntelEntityRecordQueryResults();
						//TODO:
//						
//						//session.beginTransaction();
//						WaypointFilterProcessor parser = new WaypointFilterProcessor(parsedQuery.getFilter(), dfilter, fItemProvider, session); 
//						String dataTable = parser.processFilter(fmonitor.split(2));
//						
//						queryResults.setFilterToColumnMap(parser.getFilterToColumnNames());
//						
//						fmonitor.checkCanceled();
//						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress5);
//						configureTableContents(dataTable, parser.getFilterToColumnNames(), false, session);
//						
//						String sql = "DROP TABLE " + dataTable; //$NON-NLS-1$
//						SqlGenerator.logString(sql);
//						session.createNativeQuery(sql).executeUpdate();
//						fmonitor.worked(1);
//						
//						fmonitor.checkCanceled();
//						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress6);
//						computeQueryColumns(session, flocale, query);
//						fmonitor.worked(1);
//						
//						fmonitor.checkCanceled();
//						fmonitor.subTask(Messages.IntelObservationQueryEngine_Progress7);
//						computeCount(session);
//						computeBounds(session);
//						
//						fmonitor.worked(1);
//						
//						//session.getTransaction().commit();
						return queryResults;
						//process waypoint filter
					}
				}catch (OperationCanceledException ex) {
					return null;
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
		Integer obs = (Integer) session.createNativeQuery("SELECT count(*) FROM " + queryResults.getQueryDataTable()).uniqueResult(); //$NON-NLS-1$
		queryResults.setResultCount(obs);
	}
	
	/*
	 * Configures the query columns; removing non populated attribute columns
	 */
	@SuppressWarnings("unchecked")
	private void computeQueryColumns(Session session, Locale locale, IntelEntityRecordQuery query, IQueryItemProvider fItemProvider) throws Exception{
		List<IQueryColumn> columns = IntelQueryColumnProvider.getInstance().getQueryColumns(query, fItemProvider, locale, session);
		
		//hide columns that are not part of and entity type
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT DISTINCT entity_type_key FROM ");
		sb.append(queryResults.getQueryDataTable());
		
		List<Object> typeKeys = session.createNativeQuery(sb.toString()).list();
		Set<String> attributeKeys = new HashSet<>();
		for (Object t : typeKeys) {
			String entityTypeKey = (String)t;
			IntelEntityType type = fItemProvider.getEntityType(entityTypeKey, session);
			
			for (IntelEntityTypeAttribute aa : fItemProvider.getEntityTypeAttributes(type, session)) {
				if (aa.getAttribute() != null) attributeKeys.add(aa.getAttribute().getKeyId());
				if (aa.getEntityType() != null) attributeKeys.add(aa.getEntityType().getKeyId());
			}
		}
		
		for (Iterator<IQueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
			IQueryColumn column = (IQueryColumn) iterator.next();
			if (column instanceof IntelAttributeQueryColumn ){
				String attribute = ((IntelAttributeQueryColumn)column).getAttribute().getKeyId();
				if (!attributeKeys.contains(attribute)) {
					((IntelAttributeQueryColumn) column).setVisible(false);
				}
			}
		}
			
		//remove unused attribute columns
//		StringBuilder sb = new StringBuilder();
//		sb.append("SELECT distinct a.keyid FROM "); //$NON-NLS-1$
//		sb.append(" smart.dm_attribute a join smart.i_observation_attribute b ON a.uuid = b.attribute_uuid "); //$NON-NLS-1$
//		sb.append(" join " + queryResults.getQueryDataTable() + " c on c.observation_uuid = b.observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
//		List<String> populatedAttributes = session.createNativeQuery(sb.toString()).list();
//		for (Iterator<IQueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
//			IQueryColumn column = (IQueryColumn) iterator.next();
//			if (column instanceof DataModelColumn ){
//				String attribute = ((DataModelColumn)column).getAttributeKey();
//				if (attribute != null && !populatedAttributes.contains(attribute)){
//					//iterator.remove();
//					((DataModelColumn) column).setVisible(false);
//				}
//			}
//			
//		}
		queryResults.setQueryColumns(columns);
	}
	
	/*
	 * create the results table from the data table add necessary results columns
	 */
	@SuppressWarnings("unchecked")
	private void configureTableContents(String observationTable, HashMap<IQueryFilter, String> filterToColumn, boolean obsFilter, IQueryItemProvider fItemProvider, Session session){
		
		
		StringBuilder sb = new StringBuilder();
		sb.append(" ALTER TABLE ");
		sb.append(observationTable);
		sb.append(" add column entity_type varchar(1024)");
		
		SqlGenerator.logString(sb.toString());
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		
		sb = new StringBuilder();
		sb.append("SELECT DISTINCT entity_type_key FROM ");
		sb.append(observationTable);
		
		List<Object> typeKeys = session.createNativeQuery(sb.toString()).list();
		for (Object t : typeKeys) {
			String entityTypeKey = (String)t;
			IntelEntityType type = fItemProvider.getEntityType(entityTypeKey, session);
			
			sb = new StringBuilder();
			sb.append("UPDATE ");
			sb.append(observationTable);
			sb.append(" set entity_type = :name ");
			sb.append(" WHERE entity_type_key = :key ");
				
			NativeQuery<?> update = session.createNativeQuery(sb.toString());
			update.setParameter("key", entityTypeKey);
			if(type == null) {
				update.setParameter("name", entityTypeKey);
			}else {
				update.setParameter("name", type.getName());
			}
			update.executeUpdate();
		}
		
		HashMap<String, Integer> columnNameToIndex = new HashMap<>();
		
		int columnIndex = 0;
		columnNameToIndex.put("entity_uuid", columnIndex++);
		columnNameToIndex.put("entity_type_key", columnIndex++);
		for (String v : filterToColumn.values()){
			columnNameToIndex.put(v, columnIndex++);
		}
		columnNameToIndex.put("entity_type", columnIndex++);
		
//		
//		String newTable = SqlGenerator.createTempTableName();
//		
//		String[][] columns = new String[][]{
//			{"observation_uuid", "char(16) for bit data"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"location_uuid", "char(16) for bit data"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"record_uuid", "char(16) for bit data"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"record_source_uuid", "char(16) for bit data"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"record_status", "varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"record_title", "varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"loc_id", "varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"loc_datetime", "timestamp"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"loc_comment", "varchar(4096)"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"loc_geometry", "blob"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"category_uuid", "char(16) for bit data"} //$NON-NLS-1$ //$NON-NLS-2$
//		};
//		
//		String[][] sortColumns = new String[][]{
//			{"str_sort", "varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"dbl_sort", "double"}, //$NON-NLS-1$ //$NON-NLS-2$
//			{"date_sort", "date"}		 //$NON-NLS-1$ //$NON-NLS-2$
//		};
//		
//		StringBuilder insert = new StringBuilder();
//		StringBuilder select = new StringBuilder();
//		StringBuilder sb = new StringBuilder();
//		int columnIndex = 0;
//		sb.append("CREATE TABLE " + newTable + " ( "); //$NON-NLS-1$ //$NON-NLS-2$
//		for (String[] c : columns){
//			sb.append( c[0] + " " + c[1] + ","); //$NON-NLS-1$ //$NON-NLS-2$
//			columnNameToIndex.put(c[0], columnIndex++);
//			insert.append(c[0] + ","); //$NON-NLS-1$
//		}
//		for (String[] c : sortColumns){
//			sb.append( c[0] + " " + c[1] + ","); //$NON-NLS-1$ //$NON-NLS-2$
//			columnNameToIndex.put(c[0], columnIndex++);
//		}
//		for (String v : filterToColumn.values()){
//			sb.append( v + " boolean,"); //$NON-NLS-1$
//			columnNameToIndex.put(v, columnIndex++);
//			insert.append(v + ","); //$NON-NLS-1$
//			select.append(", a." + v ); //$NON-NLS-1$
//		}
//		
//		insert.deleteCharAt(insert.length() - 1);
//		sb.deleteCharAt(sb.length() - 1);
//		sb.append(")"); //$NON-NLS-1$
//		
//		SqlGenerator.logString(sb.toString());
//		session.createNativeQuery(sb.toString()).executeUpdate();
//		
//		
//		sb = new StringBuilder();
//		sb.append(" INSERT INTO " + newTable + " "); //$NON-NLS-1$ //$NON-NLS-2$
//		sb.append(" ( " + insert.toString() + ")" ); //$NON-NLS-1$ //$NON-NLS-2$
//		sb.append("SELECT o.uuid, a.location_uuid, r.uuid, r.source_uuid, r.status, r.title, l.id, l.datetime, l.comment, l.geometry, o.category_uuid "); //$NON-NLS-1$
//		sb.append(select);
//		sb.append(" FROM " + observationTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
//		sb.append(" JOIN smart.i_location l on a.location_uuid = l.uuid "); //$NON-NLS-1$
//		sb.append(" JOIN smart.i_record r on r.uuid = l.record_uuid "); //$NON-NLS-1$
//		sb.append(" LEFT JOIN smart.i_observation o on o.location_uuid = a.location_uuid "); //$NON-NLS-1$
//		if (obsFilter){
//			sb.append(" AND o.uuid = a.observation_uuid "); //$NON-NLS-1$
//		}
//		SqlGenerator.logString(sb.toString());
//		session.createNativeQuery(sb.toString()).executeUpdate();
		
		queryResults.setResultsTable(observationTable);
		queryResults.setColumnNameToIndexMap(columnNameToIndex);
		
	}
	
}
