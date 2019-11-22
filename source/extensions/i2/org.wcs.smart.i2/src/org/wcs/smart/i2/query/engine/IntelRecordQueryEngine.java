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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.query.CaQueryItemProvider;
import org.wcs.smart.i2.query.DesktopCcaaQueryItemProvider;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.BooleanFilter;
import org.wcs.smart.i2.query.observation.filter.BracketFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.RecordAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.NotFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for intelligence observation queries.
 * 
 * @author Emily
 *
 */
public class IntelRecordQueryEngine implements IIntelQueryEngine {

	
	private IntelObservationQueryResults queryResults;
	private DataTable dataTable;
	
	/**
	 * Parameters required are session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	public IPagedQueryResultSet executeQuery(AbstractIntelQuery iquery,  HashMap<String, Object> parameters) throws Exception{
		
		IntelRecordQuery query = (IntelRecordQuery) iquery;
		
		Session session = (Session) parameters.get(Session.class.getName());
		IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		SubMonitor progress = SubMonitor.convert(monitor, Messages.IntelObservationQueryEngine_Progress1, 6);
		//one or both element of array may be null
		Date[] dfilter = (Date[]) parameters.get(Date.class.getName());
		if (dfilter == null) return null;
		
		Locale locale = (Locale) parameters.get(Locale.class.getName());
		if (locale == null){
			locale = Locale.getDefault();
		}
		
		@SuppressWarnings("unchecked")
		Collection<ConservationArea> cas = (Collection<ConservationArea>)parameters.get(ConservationArea.class.getName());
		if (cas == null){
			 throw new Exception(Messages.IntelObservationQueryEngine_InvalidCaParameter);
		}
		IQueryItemProvider itemProvider = new DesktopCcaaQueryItemProvider(cas, query.getConservationArea());
		if (cas.size() == 1) {
			itemProvider = new CaQueryItemProvider(cas.iterator().next(), query.getConservationArea());
		}
		
		progress.subTask(Messages.IntelObservationQueryEngine_Progress2);
		IQueryFilter filter = IntelRecordQuery.parseQuery(query.getQueryString());
		progress.worked(1);
		
		final SubMonitor fmonitor = progress;
		final Locale flocale = locale;
		final IQueryItemProvider fItemProvider = itemProvider;
		
		Set<String> profiles = new HashSet<>();
		for (String ip : IntelEntityRecordQuery.convertFromProfileFilter(query.getProfileFilter())) {
			List<IntelProfile> items = session.createQuery("FROM IntelProfile WHERE keyId = :keyId and conservationArea in (:cas)", IntelProfile.class)
					.setParameter("keyId",  ip)
					.setParameter("cas", cas).list();
			
			for (IntelProfile ip2 : items) {
				ip2.getKeyId();
				if (IntelSecurityManager.INSTANCE.canViewQuery(ip2)) profiles.add(ip2.getKeyId());
			}
		}
		
		//parse temporary table
		progress.subTask("Creating Record Table");
		
		createTemporaryRecordTable(session, profiles, dfilter, cas);
		
		progress.worked(1);
		
		return session.doReturningWork(new ReturningWork<IPagedQueryResultSet>() {
			@Override
			public IPagedQueryResultSet execute(Connection connection) throws SQLException {
				try {
					//add attribute columns
					progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressAttributeColumns);
					
					
					addAttributeColumns(filter, dataTable.tableName, session, progress.split(1));
					progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressFilter);
					filterDataTable(session, filter);
					progress.worked(1);
					
					Integer cnt = (Integer) session.createNativeQuery("SELECT count(*) FROM " + dataTable.tableName).uniqueResult(); //$NON-NLS-1$
					
					List<IQueryColumn> columns = IntelQueryColumnProvider.getInstance().getQueryColumns(query, InternalQueryManager.INSTANCE.getQueryItemProvider(), flocale, session);
					
					IntelRecordQueryResults results = new IntelRecordQueryResults();
					results.setResultCount(cnt);
					results.setResultsTable(dataTable.tableName);
					results.setQueryColumns(columns);
					
					HashMap<String,Integer> map = new HashMap<>();
					for (int i = 0; i < dataTable.corder.size(); i ++) {
						map.put(dataTable.corder.get(i), i);
					}
					results.setColumnNameToIndexMap(map);
//					results.setFilterToColumnMap(filterToColumn);
					return results;
				}catch (OperationCanceledException ex) {
					return null;
				}catch (Exception ex) {
					throw new SQLException(ex);
				}
			}
		});		
	}
	
	
//	/*
//	 * Configures the query columns; removing non populated attribute columns
//	 */
//	@SuppressWarnings("unchecked")
//	private void computeQueryColumns(Session session, Locale locale, IntelRecordObservationQuery query) throws Exception{
//		List<IQueryColumn> columns = IntelQueryColumnProvider.getInstance().getQueryColumns(query, InternalQueryManager.INSTANCE.getQueryItemProvider(), locale, session);
//		//remove unused attribute columns
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
//		queryResults.setQueryColumns(columns);
//	}
//	
//	
	
	
	/*
	 * create temporary entity table and populate with all entities
	 * that match the given conservation area
	 */
	private String createTemporaryRecordTable(Session session, Set<String> profiles, Date[] dates, Collection<ConservationArea> cas) {
		String obsTable = SqlGenerator.createTempTableName();
		
		dataTable = new DataTable(obsTable);
		
		String created = SystemAttributeFilter.SystemAttribute.RECORD_DATE_CREATED.name().toLowerCase(Locale.ROOT); //$NON-NLS-1$
		String modified = SystemAttributeFilter.SystemAttribute.RECORD_DATE_MODIFIED.name().toLowerCase(Locale.ROOT); //$NON-NLS-1$
		String pdate = SystemAttributeFilter.SystemAttribute.RECORD_DATE.name().toLowerCase(Locale.ROOT); //$NON-NLS-1$
		//create table
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(obsTable);
		sb.append(" (record_uuid char(16) for bit data, " + pdate + " date, record_status varchar(16), ");
		sb.append(" record_source_key varchar(128), ca_uuid char(16) for bit data, record_title varchar(1024), "); //$NON-NLS-1$
		sb.append(created );
		sb.append(" date, "); //$NON-NLS-1$
		sb.append(modified);
		sb.append(" date, "); //$NON-NLS-1$
		sb.append(" profile_uuid char(16) for bit data," );
		sb.append(" source_uuid char(16) for bit data" );
		sb.append(" ) "); //$NON-NLS-1$
		
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		dataTable.addColumn("record_uuid",  "char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn(pdate,  "date"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn("record_status",  "varchar(16)"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn("record_source_key",  "varchar(128)"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn("ca_uuid",  "char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn("record_title",  "varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn(created,  "date"); //$NON-NLS-1$ 
		dataTable.addColumn(modified,  "date"); //$NON-NLS-1$
		dataTable.addColumn("profile_uuid",  "char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn("source_uuid",  "char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		
		sb = new StringBuilder();
		sb.append("INSERT INTO "); //$NON-NLS-1$
		sb.append(obsTable);
		sb.append("(record_uuid, " + pdate + ", record_status, record_source_key, ca_uuid, record_title, "); //$NON-NLS-1$
		sb.append(created);
		sb.append(","); //$NON-NLS-1$
		sb.append(modified);
		sb.append(", profile_uuid, source_uuid"); //$NON-NLS-1$
		sb.append(") SELECT a.uuid, cast(a.primary_date as date), a.status, b.keyid, a.ca_uuid, a.title, ");
		sb.append(" cast(a.date_created as date), cast(a.last_modified_date as date), ");
		sb.append(" a.profile_uuid, a.source_uuid ");
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append(" smart.i_record a join smart.i_recordsource b on a.source_uuid = b.uuid join smart.i_profile_config p on p.uuid = a.profile_uuid"); //$NON-NLS-1$
		sb.append(" WHERE b.ca_uuid in (:cauuids) and p.keyid in (:profiles)"); //$NON-NLS-1$
		if (dates[0] != null) {
			sb.append(" AND cast(a.primary_date as date)>= :startd and cast(a.primary_date as date) <= :endd");
		}
		
		List<UUID> cauuids = cas.stream().map(e->e.getUuid()).collect(Collectors.toList());
		
		logme(sb);
		NativeQuery<?> q = session.createNativeQuery(sb.toString())
			.setParameterList("cauuids",  cauuids) //$NON-NLS-1$
			.setParameterList("profiles",  profiles); //$NON-NLS-1$
		if (dates[0] != null) {
			q.setParameter("startd",dates[0])
			.setParameter("endd",dates[1]);
		}
		q.executeUpdate();
		
		return obsTable;
	}
	
	/*
	 * add all attribute columns to entity data table
	 */
	private Map<String, String> addAttributeColumns(IQueryFilter filter, String queryTable, Session session, IProgressMonitor monitor) {
		if (filter == null) return Collections.emptyMap();
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		
		Map<String, String> attributeToColumnKey = new HashMap<>();
				
		progress.setWorkRemaining(1);
		filter.accept(new IFilterVisitor() {
			@Override
			public void visitElement(IQueryFilter filter) {
				if (filter instanceof RecordAttributeFilter) {
					String attributeKey = ((RecordAttributeFilter) filter).getAttributeKey();
					IntelAttribute.AttributeType attributeType = ((RecordAttributeFilter) filter).getAttributeType();
					if (attributeType == AttributeType.POSITION) return; //  position attribute dealt with outside of here
					if (!attributeToColumnKey.containsKey(attributeKey)) {
						String columnName = addAttributeColumn(queryTable, attributeKey, attributeType, session);
						attributeToColumnKey.put(attributeKey, columnName);
					}
				}					
			}
		});
		progress.worked(1);
		return attributeToColumnKey;
	}
	
	/*
	 * get the type of column for the attribute type
	 */
	private String getColumnType(IntelAttribute.AttributeType type) {
		switch(type) {
		case BOOLEAN:
			return "boolean"; //$NON-NLS-1$
		case DATE:
			return "date"; //$NON-NLS-1$
		case EMPLOYEE:
			return "char(16) for bit data"; //$NON-NLS-1$
		case LIST:
			return "char(16) for bit data"; //$NON-NLS-1$
		case NUMERIC:
			return "double"; //$NON-NLS-1$
		case POSITION:
			//group by position attributes are dealt with in a different way; should never execute this code
			throw new IllegalStateException("Should not group by position attributes."); //$NON-NLS-1$
		case TEXT:
			return "varchar (1024)"; //$NON-NLS-1$
		}
		return null;
	}
	
	/*
	 * add an attribute column to the entity type
	 */
	private String addAttributeColumn(String queryTable, String attributeKey, IntelAttribute.AttributeType type, Session session) {
		String columnName = attributeKey;
		String columnType = getColumnType(type);
		
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE "); //$NON-NLS-1$
		sb.append(queryTable);
		sb.append( " ADD COLUMN " ); //$NON-NLS-1$
		sb.append( columnName );
		sb.append(" "); //$NON-NLS-1$
		sb.append( getColumnType(type));
		
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		dataTable.addColumn(columnName,  columnType);
		
		String selectClause = null;
		switch (type) {
		case BOOLEAN:
			selectClause = "case when v.double_value > 0.5 then true else false end"; //$NON-NLS-1$
			break;
			
		case DATE:
			selectClause = "cast(v.string_value as date)"; //$NON-NLS-1$
			break;
		case EMPLOYEE:
			selectClause = "v.uuid"; //$NON-NLS-1$
			break;
		case LIST:
			selectClause = "v.uuid"; //$NON-NLS-1$
			break;
		case NUMERIC:
			selectClause = "v.double_value"; //$NON-NLS-1$
			break;
		case POSITION:
			break;
		case TEXT:
			selectClause = "v.string_value"; //$NON-NLS-1$
			break;
		}

		sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append(queryTable);
		sb.append( " SET " ); //$NON-NLS-1$
		sb.append( columnName );
		sb.append(" = ( SELECT  "); //$NON-NLS-1$
		sb.append( selectClause );
		sb.append(" FROM smart.i_record_attribute_value v join smart.i_recordsource_attribute b on b.uuid = v.attribute_uuid ");
		sb.append(" JOIN smart.i_attribute a on b.attribute_uuid = a.uuid "); //$NON-NLS-1$
		sb.append(" WHERE v.record_uuid = " + queryTable + ".record_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" and a.keyid = :keyid "); //$NON-NLS-1$
		sb.append(" ) "); //$NON-NLS-1$
		
		logme(sb);
		
		session.createNativeQuery(sb.toString())
			.setParameter("keyid", attributeKey) //$NON-NLS-1$
			.executeUpdate();
		
		return columnName;
		
	}
	
	/*
	 * Filters the data table by creating a new data table and only 
	 * including the elements that match the filter,  
	 */
	private void filterDataTable(Session session, IQueryFilter queryFilter) throws Exception {
		String table2 = SqlGenerator.createTempTableName();
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(table2);
		sb.append("("); //$NON-NLS-1$
		for (String name : dataTable.corder) {
			String type = dataTable.columnTypes.get(name);
			sb.append(name);
			sb.append(" ");
			sb.append(type);
			sb.append(","); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")"); //$NON-NLS-1$
		
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO " ); //$NON-NLS-1$
		sb.append(table2);
		sb.append("("); //$NON-NLS-1$
		for (Entry<String,String> entry : dataTable.columnTypes.entrySet()) {
			sb.append(entry.getKey());
			sb.append(","); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")"); //$NON-NLS-1$
		sb.append(" SELECT "); //$NON-NLS-1$
		for (Entry<String,String> entry : dataTable.columnTypes.entrySet()) {
			sb.append("a." + entry.getKey()); //$NON-NLS-1$
			sb.append(","); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" FROM " ); //$NON-NLS-1$
		sb.append(dataTable.tableName + " a"); //$NON-NLS-1$
				
		HashMap<String,Object> parameters = new HashMap<>();

		if (queryFilter != null) {
			final boolean[] requiresEntityType = new boolean[] {false};
			final boolean[] requiresEntity = new boolean[] {false};
			queryFilter.accept(new IFilterVisitor() {			
				@Override
				public void visitElement(IQueryFilter filter) {
					if (requiresEntityType[0]) return;
					if (filter instanceof RecordAttributeFilter) {
						RecordAttributeFilter f = (RecordAttributeFilter) filter;
						if (f.getEntityTypeKey() != null && !f.getEntityTypeKey().isEmpty()) {
							requiresEntityType[0] = true;
							return;
						}
					}
	
				}
			});
			if (requiresEntity[0] || requiresEntityType[0]) {
				sb.append(" JOIN smart.i_entity e on e.uuid = a.entity_uuid "); //$NON-NLS-1$
				if (requiresEntityType[0]) {
					sb.append(" JOIN smart.i_entity_type t ON e.entity_type_uuid = t.uuid "); //$NON-NLS-1$
				}
			}
			
			sb.append(" WHERE "); //$NON-NLS-1$
			processFilter(queryFilter, sb, parameters);
		}
		String querystr = sb.toString();
		if (querystr.endsWith(" WHERE " )) {
			querystr = querystr.substring(0, querystr.length() - " WHERE ".length());
		}
		
		NativeQuery<?> query = session.createNativeQuery(querystr);
		for (Entry<String,Object> parameter : parameters.entrySet()) {
			query.setParameter(parameter.getKey(),  parameter.getValue());
			logme(parameter.getKey() + ":" + parameter.getValue()); //$NON-NLS-1$
		}
		logme(sb);
		query.executeUpdate();
		
		try {
			session.createNativeQuery("DROP TABLE " + dataTable.tableName); //$NON-NLS-1$
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		dataTable.tableName = table2;
	}
	
	
private void processFilter(IQueryFilter queryFilter, StringBuilder whereSql, HashMap<String,Object> parameters) throws Exception {
		
		if (queryFilter instanceof SystemAttributeFilter) {
			SystemAttributeFilter f = (SystemAttributeFilter)queryFilter;
			if (f.getAttribute() == SystemAttribute.RECORD_DATE_CREATED ||
				f.getAttribute() == SystemAttribute.RECORD_DATE_MODIFIED ||
				f.getAttribute() == SystemAttribute.RECORD_SOURCE ||
				f.getAttribute() == SystemAttribute.RECORD_STATUS ||
				f.getAttribute() == SystemAttribute.RECORD_DATE ) {
				
				String columnName = null;
				if (f.getAttribute() == SystemAttributeFilter.SystemAttribute.RECORD_DATE_CREATED) {
					columnName = "a.record_date_created"; //$NON-NLS-1$
					whereSql.append(SqlGenerator.generateDateClause(f.getDateValues(), columnName));
				}else if (f.getAttribute() == SystemAttributeFilter.SystemAttribute.RECORD_DATE_MODIFIED) {
					columnName = "a.record_date_modified"; //$NON-NLS-1$
					whereSql.append(SqlGenerator.generateDateClause(f.getDateValues(), columnName));
				}else if (f.getAttribute() == SystemAttribute.RECORD_SOURCE) {
					String key = "p_" + parameters.size(); //$NON-NLS-1$
					parameters.put(key, f.getStringKey());
					whereSql.append(" a.record_source_key = :" + key); //$NON-NLS-1$
				}else if (f.getAttribute() == SystemAttribute.RECORD_STATUS) {
					String key = "p_" + parameters.size(); //$NON-NLS-1$
					parameters.put(key, f.getStringKey());
					whereSql.append(" a.record_status = :" + key); //$NON-NLS-1$
				}else if (f.getAttribute() == SystemAttributeFilter.SystemAttribute.RECORD_DATE) {
					columnName = "a.record_date"; //$NON-NLS-1$
					whereSql.append(SqlGenerator.generateDateClause(f.getDateValues(), columnName));
				}
				whereSql.append(" ");
			}else {
				throw new IllegalStateException("Entity filters not supported for record queries"); //$NON-NLS-1$
			}
		}else if (queryFilter instanceof BooleanFilter) {
			BooleanFilter f = (BooleanFilter)queryFilter;
			processFilter(f.getFilter1(), whereSql, parameters);
			whereSql.append( SqlGenerator.operatorToSql(f.getOperator()) );
			processFilter(f.getFilter2(), whereSql, parameters);
			
		}else if (queryFilter instanceof BracketFilter) {
			BracketFilter f = (BracketFilter)queryFilter;
			whereSql.append(" ( "); //$NON-NLS-1$
			processFilter(f.getFilter(), whereSql, parameters);
			whereSql.append(" ) "); //$NON-NLS-1$
		
		}else if (queryFilter instanceof RecordAttributeFilter) {
			RecordAttributeFilter f = (RecordAttributeFilter)queryFilter;
			
			boolean close = false;
			if (f.getEntityTypeKey() != null && !f.getEntityTypeKey().isEmpty()) {
				whereSql.append(" ("); //$NON-NLS-1$
				close = true;
				String key = "p_" + parameters.size(); //$NON-NLS-1$
				parameters.put(key, f.getEntityTypeKey());
				whereSql.append(" t.keyId = :" + key); //$NON-NLS-1$
				
				whereSql.append(" AND "); //$NON-NLS-1$
			}
			String columnName = "a." + f.getAttributeKey(); //$NON-NLS-1$
			if (f.getAttributeType() == AttributeType.DATE) {
				whereSql.append(SqlGenerator.generateDateClause(f.getDateValues(), columnName));
			}else if (f.getAttributeType() == AttributeType.BOOLEAN) {
				whereSql.append(" "); //$NON-NLS-1$
				whereSql.append(columnName);
				whereSql.append(" ");				 //$NON-NLS-1$
			}else if (f.getAttributeType() == AttributeType.EMPLOYEE) {
				whereSql.append(" "); //$NON-NLS-1$
				whereSql.append(columnName);
				if (f.getKeyValue().equals(RecordAttributeFilter.ANY_OPTION_KEY)) {
					whereSql.append(" is not null "); //$NON-NLS-1$
				}else {
					whereSql.append( " = " ); //$NON-NLS-1$
					String key = "p_" + parameters.size(); //$NON-NLS-1$
					parameters.put(key, UuidUtils.stringToUuid( f.getKeyValue()) );
					whereSql.append(" :" + key + " "); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}else if (f.getAttributeType() == AttributeType.LIST) {
				whereSql.append(" "); //$NON-NLS-1$
				whereSql.append(columnName);
				whereSql.append(" IN (");
				if (f.getKeyValue().equals(RecordAttributeFilter.ANY_OPTION_KEY)) {
//					whereSql.append(" is not null "); //$NON-NLS-1$
					whereSql.append(" SELECT value_uuid FROM smart.i_record_attribute_value_list ");
				}else {
					
					if (f.getAttributeKey() != null) {
						whereSql.append(" SELECT a.value_uuid FROM smart.i_record_attribute_value_list a join smart.i_attribute_list_item i on a.element_uuid = i.uuid where i.keyid = ");
						String key = "p_" + parameters.size(); //$NON-NLS-1$
						parameters.put(key, f.getKeyValue());
						whereSql.append(" :" + key + " "); //$NON-NLS-1$ //$NON-NLS-2$
					}else if (f.getEntityTypeKey() != null) {
						whereSql.append(" SELECT a.value_uuid FROM smart.i_record_attribute_value_list a where a.uuid = ");
						String key = "p_" + parameters.size(); //$NON-NLS-1$
						parameters.put(key, f.getKeyValue());
						whereSql.append(" :" + key + " "); //$NON-NLS-1$ //$NON-NLS-2$
					}else if (f.getAttributeType() == AttributeType.EMPLOYEE) {
						whereSql.append(" SELECT a.value_uuid FROM smart.i_record_attribute_value_list a where a.uuid = ");
						String key = "p_" + parameters.size(); //$NON-NLS-1$
						parameters.put(key, f.getKeyValue());
						whereSql.append(" :" + key + " "); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				whereSql.append(")");
			}else if (f.getAttributeType() == AttributeType.NUMERIC) {
				whereSql.append(" "); //$NON-NLS-1$
				whereSql.append(columnName);
				whereSql.append( SqlGenerator.operatorToSql(f.getOperator()) );
				String key = "p_" + parameters.size(); //$NON-NLS-1$
				parameters.put(key, f.getNumberValue());
				whereSql.append(" :" + key + " "); //$NON-NLS-1$ //$NON-NLS-2$
			}else if (f.getAttributeType() == AttributeType.POSITION) {
				//Should never get here
				throw new IllegalStateException("Filtering on position attributes is not supported"); //$NON-NLS-1$
			}else if (f.getAttributeType() == AttributeType.TEXT) {
				whereSql.append(" LOWER( " + columnName + ") "); //$NON-NLS-1$ //$NON-NLS-2$
				whereSql.append( SqlGenerator.operatorToSql(f.getOperator()) );
				String key = "p_" + parameters.size(); //$NON-NLS-1$
				String value = f.getStringValue();
				if (f.getOperator() == Operator.STR_CONTAINS || f.getOperator() == Operator.STR_NOTCONTAINS) {
					value = "%" + value + "%";  //$NON-NLS-1$//$NON-NLS-2$
				}
				parameters.put(key, value);
				whereSql.append(" LOWER(:" + key + ") "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			
			if (close) {
				whereSql.append(" ) "); //$NON-NLS-1$
			}
		}else if (queryFilter instanceof NotFilter) {
			NotFilter nf = (NotFilter)queryFilter;
			whereSql.append(" "); //$NON-NLS-1$
			whereSql.append(SqlGenerator.operatorToSql(Operator.NOT));
			whereSql.append(" "); //$NON-NLS-1$
			processFilter(nf.getFilter(), whereSql, parameters);
		}
	}
	
	private void logme(StringBuilder sb) {
		logme(sb.toString());
	}
	private void logme(String sb) {
		System.out.println(sb.toString());
	}
	
	/*
	 * simple class to track the datatable and columns added to the table
	 */
	private class DataTable {
		String tableName;
		HashMap<String, String> columnTypes = new HashMap<>();
		List<String> corder = new ArrayList<>();
		
		public DataTable(String tableName) {
			this.tableName = tableName;
		}
		
		public void addColumn(String name, String type) {
			columnTypes.put(name, type);
			corder.add(name);
		}
	}
	
	
}
