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

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
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
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.security.AdvIntelAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.query.CaQueryItemProvider;
import org.wcs.smart.i2.query.CcaaQueryItemProvider;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.IntelAttributeQueryColumn;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.engine.EntityRecordQueryResultItem;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter.FilterType;
import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for intelligence observation queries.
 * 
 * @author Emily
 *
 */
public class IntelEntityRecordQueryEngine implements IIntelQueryEngine {

	
	private IntelEntityRecordQueryResults queryResults;
	private boolean adduuids = false;
	private Locale locale;
	
	/**
	 * Parameters required are session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	public IPagedQueryResultSet executeQuery(AbstractIntelQuery iquery,  HashMap<String, Object> parameters) throws Exception{
		
		IntelEntityRecordQuery query = (IntelEntityRecordQuery) iquery;
		
		Session session = (Session) parameters.get(Session.class.getName());
		String username = ((String)parameters.get(Principal.class.getName()));
		
		locale = (Locale) parameters.get(Locale.class.getName());
		if (locale == null){
			locale = Locale.getDefault();
		}
		
		if (parameters.containsKey(AbstractQueryEngine.INCLUDE_UUID_PARAMETER)) {
			adduuids = (Boolean)parameters.get(AbstractQueryEngine.INCLUDE_UUID_PARAMETER);
		}
		
		@SuppressWarnings("unchecked")
		Collection<ConservationArea> cas = (Collection<ConservationArea>)parameters.get(ConservationArea.class.getName());
		if (cas == null){
			 throw new Exception("No conservation areas specified"); //$NON-NLS-1$
		}
		
		Set<IntelProfile> profiles = new HashSet<>();
		for (String ip : AbstractIntelQuery.convertFromProfileFilter(query.getProfileFilter())) {
			List<IntelProfile> items = session.createQuery("FROM IntelProfile WHERE keyId = :keyId and conservationArea in (:cas)", IntelProfile.class) //$NON-NLS-1$
					.setParameter("keyId",  ip) //$NON-NLS-1$
					.setParameter("cas", cas).list(); //$NON-NLS-1$
			if (SecurityManager.INSTANCE.canAccess(session, username, AdvIntelAction.RUNQUERY_KEY, iquery.getUuid()) ||
					SecurityManager.INSTANCE.canAccess(session, username, AdvIntelAction.RUNQUERY_KEY, iquery.getConservationArea().getUuid())) { 
				//we have permission to run this query so use all profiles
				profiles.addAll(items);
			}
		}
		
		if (profiles.isEmpty()) {
			throw new Exception(Messages.getString("IntelEntityRecordQueryEngine.NoProfileFilterForQuery", locale)); //$NON-NLS-1$
		}
		
		IQueryItemProvider itemProvider = null;
		if (!query.getConservationArea().getIsCcaa()) {
			itemProvider = new CaQueryItemProvider(cas.iterator().next(), query.getConservationArea());
		}else {
			itemProvider = new CcaaQueryItemProvider(profiles, query.getConservationArea());
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
						EntityRecordObservationFilterProcessor parser = new EntityRecordObservationFilterProcessor(parsedQuery.getFilter(), profiles, fItemProvider, session); 
						dataTable = parser.processFilter();
						filterToColumnNames = parser.getFilterToColumnNames();
					}else {
						EntityRecordWaypointFilterProcessor parser = new EntityRecordWaypointFilterProcessor(parsedQuery.getFilter(), profiles, fItemProvider, session); 
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


		if (adduuids) {
			IQueryColumn entityUuid = new IQueryColumn() {
				@Override
				public String getColumnName() {
					return Messages.getString("IntelEntityRecordQueryEngine.EntityUuidColumnName", locale); //$NON-NLS-1$
				}
				@Override
				public Type getDataType() {
					return Type.STRING;
				}
				@Override
				public String getKey() {
					return "entity_uuid"; //$NON-NLS-1$
				}
				@Override
				public Object getValue(org.wcs.smart.i2.query.IResultItem item) {
					if (((EntityRecordQueryResultItem)item).getEntityUuid() == null) return ""; //$NON-NLS-1$
					return UuidUtils.uuidToString( ((EntityRecordQueryResultItem)item).getEntityUuid());
				}
				@Override
				public String getValue(org.wcs.smart.i2.query.IResultItem item, Locale arg1) {
					return (String)getValue(item);
				}
				@Override
				public boolean isVisible() {
					return true;
				}
			};
			IQueryColumn entityLastModified = new IQueryColumn() {
				@Override
				public String getColumnName() {
					return Messages.getString("IntelEntityRecordQueryEngine.EntityLastModifiedColumnName", locale); //$NON-NLS-1$
				}
				@Override
				public Type getDataType() {
					return Type.DATE;
				}
				@Override
				public String getKey() {
					return "entity_last_modified"; //$NON-NLS-1$
				}
				@Override
				public Object getValue(org.wcs.smart.i2.query.IResultItem item) {
					if (((EntityRecordQueryResultItem)item).getEntityLastModified() == null) return null; 
					return ((EntityRecordQueryResultItem)item).getEntityLastModified();
				}
				@Override
				public String getValue(org.wcs.smart.i2.query.IResultItem item, Locale locale) {
					Date d = (Date) getValue(item);
					if (d == null) return ""; //$NON-NLS-1$
					return DateTimeFormatter.ISO_DATE_TIME.format(  (new java.sql.Timestamp(d.getTime())).toLocalDateTime());
				}
				
				@Override
				public boolean isVisible() {
					return true;
				}
			};
			
			columns.add(entityUuid);
			columns.add(entityLastModified);
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
		columnNameToIndex.put("date_modified", columnIndex++); //$NON-NLS-1$
		columnNameToIndex.put("entity_type_key", columnIndex++); //$NON-NLS-1$
		columnNameToIndex.put("ca_id", columnIndex++); //$NON-NLS-1$
		columnNameToIndex.put("ca_name", columnIndex++); //$NON-NLS-1$
		columnNameToIndex.put("profile_uuid", columnIndex++); //$NON-NLS-1$
		for (Object[] v : filterToColumn){
			columnNameToIndex.put((String)v[1], columnIndex++);
		}
		columnNameToIndex.put("entity_type", columnIndex++); //$NON-NLS-1$
	
		queryResults.setResultsTable(observationTable);
		queryResults.setColumnNameToIndexMap(columnNameToIndex);		
	}
}
