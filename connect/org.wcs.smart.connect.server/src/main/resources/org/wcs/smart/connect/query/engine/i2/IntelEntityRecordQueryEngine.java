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
package org.wcs.smart.connect.query.engine.i2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.query.CaQueryItemProvider;
import org.wcs.smart.i2.query.CcaaQueryItemProvider;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.IntelAttributeQueryColumn;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter.FilterType;
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

		Locale locale = (Locale) parameters.get(Locale.class.getName());
		if (locale == null){
			locale = Locale.getDefault();
		}
		
		@SuppressWarnings("unchecked")
		Collection<ConservationArea> cas = (Collection<ConservationArea>)parameters.get(ConservationArea.class.getName());
		if (cas == null){
			 throw new Exception("No conservation areas specified"); //$NON-NLS-1$
		}
		
		IQueryItemProvider itemProvider = null;
		if (!query.getConservationArea().getIsCcaa()) {
			itemProvider = new CaQueryItemProvider(cas.iterator().next(), query.getConservationArea());
		}else {
			itemProvider = new CcaaQueryItemProvider(cas, query.getConservationArea());
		}
		ParsedObservationQuery parsedQuery = IntelEntityRecordQuery.parseQuery(query.getQueryString());

		final Locale flocale = locale;
		final IQueryItemProvider fItemProvider = itemProvider;
		return session.doReturningWork(new ReturningWork<IPagedQueryResultSet>() {
			@Override
			public IPagedQueryResultSet execute(Connection connection) throws SQLException {
				
				connection.setAutoCommit(true);
				try{
					queryResults = new IntelEntityRecordQueryResults();
						
					String dataTable = null;
					List<Object[]> filterToColumnNames = null;
					if (parsedQuery.getFilterType() == FilterType.OBSERVATION) {
						EntityRecordObservationFilterProcessor parser = new EntityRecordObservationFilterProcessor(parsedQuery.getFilter(), fItemProvider, session); 
						dataTable = parser.processFilter();
						filterToColumnNames = parser.getFilterToColumnNames();
					}else {
						EntityRecordWaypointFilterProcessor parser = new EntityRecordWaypointFilterProcessor(parsedQuery.getFilter(), fItemProvider, session); 
						dataTable = parser.processFilter();	
						filterToColumnNames = parser.getFilterToColumnNames();
					}
					queryResults.setFilterToColumnMap(filterToColumnNames);
						
					configureTableContents(dataTable, filterToColumnNames, true, fItemProvider, session);
					computeQueryColumns(session, flocale, query, fItemProvider);
					computeCount(session);
						
					return queryResults;
				}catch (Exception ex){
					throw new SQLException(ex);
				}finally{
					connection.setAutoCommit(false);
				}
			}
		});		
	}
	
	private void computeCount(Session session){
		Integer obs = ((Number) session.createNativeQuery("SELECT count(*) FROM " + queryResults.getQueryDataTable()).uniqueResult()).intValue(); //$NON-NLS-1$
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
		sb.append("SELECT DISTINCT entity_type_key FROM "); //$NON-NLS-1$
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

		queryResults.setQueryColumns(columns);
	}
	
	/*
	 * create the results table from the data table add necessary results columns
	 */
	@SuppressWarnings("unchecked")
	private void configureTableContents(String observationTable, List<Object[]> filterToColumn, boolean obsFilter, IQueryItemProvider fItemProvider, Session session){
			
		String[][] sortColumns = new String[][]{
			{"str_sort", "varchar(1024)"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"dbl_sort", "double precision"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"date_sort", "date"}, //$NON-NLS-1$ //$NON-NLS-2$
			{"entity_id", "varchar(1024)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		StringBuilder sb = new StringBuilder();
		sb.append(" ALTER TABLE "); //$NON-NLS-1$
		sb.append(observationTable);
		sb.append(" add column entity_type varchar(1024)"); //$NON-NLS-1$
		SqlGenerator.logString(sb.toString());
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		for (String[] items : sortColumns) {
			sb = new StringBuilder();
			sb.append(" ALTER TABLE "); //$NON-NLS-1$
			sb.append(observationTable);
			sb.append(" add column "); //$NON-NLS-1$
			sb.append(items[0]);
			sb.append(" " ); //$NON-NLS-1$
			sb.append(items[1]);
			
			SqlGenerator.logString(sb.toString());
			session.createNativeQuery(sb.toString()).executeUpdate();
		}
		
		sb = new StringBuilder();
		sb.append("SELECT DISTINCT entity_type_key FROM "); //$NON-NLS-1$
		sb.append(observationTable);
		
		List<Object> typeKeys = session.createNativeQuery(sb.toString()).list();
		for (Object t : typeKeys) {
			String entityTypeKey = (String)t;
			IntelEntityType type = fItemProvider.getEntityType(entityTypeKey, session);
			
			sb = new StringBuilder();
			sb.append("UPDATE "); //$NON-NLS-1$
			sb.append(observationTable);
			sb.append(" set entity_type = :name "); //$NON-NLS-1$
			sb.append(" WHERE entity_type_key = :key "); //$NON-NLS-1$
				
			NativeQuery<?> update = session.createNativeQuery(sb.toString());
			update.setParameter("key", entityTypeKey); //$NON-NLS-1$
			if(type == null) {
				update.setParameter("name", entityTypeKey); //$NON-NLS-1$
			}else {
				update.setParameter("name", type.getName()); //$NON-NLS-1$
			}
			update.executeUpdate();
		}
		
		HashMap<String, Integer> columnNameToIndex = new HashMap<>();
		
		int columnIndex = 0;
		columnNameToIndex.put("entity_uuid", columnIndex++); //$NON-NLS-1$
		columnNameToIndex.put("entity_type_key", columnIndex++); //$NON-NLS-1$
		columnNameToIndex.put("ca_id", columnIndex++); //$NON-NLS-1$
		columnNameToIndex.put("ca_name", columnIndex++); //$NON-NLS-1$
		for (Object[] v : filterToColumn){
			columnNameToIndex.put((String)v[1], columnIndex++);
		}
		columnNameToIndex.put("entity_type", columnIndex++); //$NON-NLS-1$
	
		queryResults.setResultsTable(observationTable);
		queryResults.setColumnNameToIndexMap(columnNameToIndex);		
	}
}
