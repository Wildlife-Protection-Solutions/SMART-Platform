package org.wcs.smart.i2.query.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.BooleanFilter;
import org.wcs.smart.i2.query.observation.filter.BracketFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.NotFilter;
import org.wcs.smart.i2.query.observation.filter.RecordAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;
import org.wcs.smart.util.UuidUtils;

public class RecordFilterProcessor {

	private String[][] cols = new String[][] {
		new String[] {"record_uuid", "char(16) for bit data"},
		new String[] {"record_date", "date"},
		new String[] {"record_status", "varchar(128)"},
		new String[] {"record_source_key", "varchar(128)"},
		new String[] {"source_uuid", "char(16) for bit data"},
		new String[] {"ca_uuid", "char(16) for bit data"},
		new String[] {"record_title", "varchar(1024)"},
		new String[] {"date_created", "date"},
		new String[] {"date_modified", "date"},
		new String[] {"profile_uuid", "char(16) for bit data"},
	};
	
	HashMap<String, Integer> colName2Index = new HashMap<>();
	
	public String processFilter(IQueryFilter filter, Set<String> profiles, 
			Date[] dates, Collection<ConservationArea> cas, Session session, IProgressMonitor monitor) throws Exception {
		
		SubMonitor sub = SubMonitor.convert(monitor);
		sub.beginTask("Processing Filter", 3);
		
		sub.subTask("Creating temporary table");
		String tempTable = createTemporaryRecordTable(session, profiles, dates, cas);
		sub.worked(1);
		
		sub.subTask("Processing filters");
		Map<IQueryFilter,String> filterColumns = addAttributeColumns(filter, tempTable, session, sub.newChild(1));
		
		sub.subTask("Filtering");
		tempTable = filterDataTable(session, tempTable, filter,filterColumns);
		sub.worked(1);
		
		return tempTable;
	}
	/*
	 * create temporary entity table and populate with all entities
	 * that match the given conservation area
	 */
	private String createTemporaryRecordTable(Session session, Set<String> profiles, Date[] dates, Collection<ConservationArea> cas) {
		String obsTable = SqlGenerator.createTempTableName();
			
		//create table
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(obsTable);
		sb.append("(");
		for (int i = 0; i < cols.length; i ++) {
			sb.append( cols[i][0] );
			sb.append(" " );
			sb.append( cols[i][1] );
			sb.append(",");
			colName2Index.put(cols[i][0], i);
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append("INSERT INTO "); //$NON-NLS-1$
		sb.append(obsTable);
		sb.append("(");
		for (int i = 0; i < cols.length; i ++) {
			sb.append( cols[i][0] );
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		sb.append(" SELECT a.uuid, cast(a.primary_date as date), a.status, b.keyid, a.source_uuid , a.ca_uuid, a.title, ");
		sb.append(" cast(a.date_created as date), cast(a.last_modified_date as date), ");
		sb.append(" a.profile_uuid");
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
	private Map<IQueryFilter, String> addAttributeColumns(IQueryFilter filter, String queryTable, Session session, IProgressMonitor monitor) {
		if (filter == null) return Collections.emptyMap();
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		
		Map<IQueryFilter, String> filterToColumn = new HashMap<>();
				
		progress.setWorkRemaining(1);
		filter.accept(new IFilterVisitor() {
			int colnumber = 1;
			@Override
			public void visitElement(IQueryFilter filter) {
				
				
				if (filter instanceof RecordAttributeFilter) {
					RecordAttributeFilter f = (RecordAttributeFilter)filter;
					if (f.getAttributeKey() != null) {
						String colname = "filter" + colnumber;
						colnumber++;
						addAttributeFilter(queryTable,colname,f,session);
						filterToColumn.put(f, colname);
					}else if (f.getEntityTypeKey() != null) {
						String colname = "filter" + colnumber;
						colnumber++;
						addEntityAttributeFilter(queryTable,colname,f,session);
						filterToColumn.put(f, colname);
					}
				}					
			}
		});
		progress.worked(1);
		return filterToColumn;
	}
	
	/*
	 * add an attribute column to the entity type
	 */
	private void addAttributeFilter(String queryTable, String columnName, RecordAttributeFilter filter, Session session)  {
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE "); //$NON-NLS-1$
		sb.append(queryTable);
		sb.append( " ADD COLUMN " ); //$NON-NLS-1$
		sb.append( columnName );
		sb.append(" boolean default false "); //$NON-NLS-1$	
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append(queryTable);
		sb.append( " SET " ); //$NON-NLS-1$
		sb.append( columnName );
		sb.append(" = true where record_uuid in ( SELECT  "); //$NON-NLS-1$
		sb.append( "v.record_uuid ");
		sb.append(" FROM smart.i_record_attribute_value v join smart.i_recordsource_attribute b on b.uuid = v.attribute_uuid ");
		sb.append(" JOIN smart.i_record_source c on c.uuid = b.source_uuid and c.keyid = :sourceid");
		if (filter.getAttributeType() == AttributeType.DATE) {
			sb.append(" JOIN smart.i_attribute r on r.uuid = b.attribute_uuid and r.type = 'DATE' ");
		}
		if (filter.getAttributeType() == AttributeType.LIST || filter.getAttributeType() == AttributeType.EMPLOYEE) {
			sb.append(" JOIN smart.i_record_attribute_value_list li on li.value_uuid = v.uuid ");
			if (filter.getAttributeType() == AttributeType.LIST) {
				sb.append(" JOIN smart.i_attribute_list_item ali on ali.uuid = li.element_uuid ");
			}
		}
		sb.append(" WHERE "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" b.keyid = :keyid "); //$NON-NLS-1$
		
		try {
			if (filter.getAttributeType() == AttributeType.BOOLEAN) {
				sb.append(" AND v.double_value > 0.5 ");
			}else if (filter.getAttributeType() == AttributeType.DATE) {
				sb.append(" AND v.string_value is not null AND cast(v.string_value as date) ");
				sb.append( SqlGenerator.operatorToSql(filter.getOperator()) );
				sb.append(" :value and :value2 ");
			}else if (filter.getAttributeType() == AttributeType.NUMERIC) {
				sb.append(" AND v.double_value ");
				sb.append( SqlGenerator.operatorToSql(filter.getOperator()) );
				sb.append(" :value ");
			}else if (filter.getAttributeType() == AttributeType.TEXT) {
				sb.append(" AND v.string_value ");
				sb.append( SqlGenerator.operatorToSql(filter.getOperator()) );
				sb.append(" :value ");
			}else if (filter.getAttributeType() == AttributeType.EMPLOYEE) {
				sb.append(" AND li.element_uuid = :value ");
			}else if (filter.getAttributeType() == AttributeType.LIST) {
				if (filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)) {
					sb.append(" AND ali.keyid is not null ");
				}else {
					sb.append(" AND ali.keyid = :value ");
				}
			}
		}catch (Exception ex) {
			//TODO: fix me
		}
		sb.append(")");
		
		
		logme(sb);
		
		Query<?> q = session.createNativeQuery(sb.toString())
			.setParameter("keyid", filter.getAttributeKey()) //$NON-NLS-1$
			.setParameter("sourceid", filter.getRecordSourceKey());
		
		
		if (filter.getAttributeType() == AttributeType.DATE) {
			q.setParameter("value", filter.getDateValues()[0]);
			q.setParameter("value2", filter.getDateValues()[1]);
		}else if (filter.getAttributeType() == AttributeType.NUMERIC) {
			q.setParameter("value", filter.getNumberValue());
		}else if (filter.getAttributeType() == AttributeType.TEXT) {
			if (filter.getOperator() == Operator.STR_CONTAINS || filter.getOperator() == Operator.STR_NOTCONTAINS) {
				q.setParameter("value", "%" + filter.getStringValue() + "%");
			}else {
				q.setParameter("value", filter.getStringValue());
			}
		}else if (filter.getAttributeType() == AttributeType.EMPLOYEE) {
			q.setParameter("value",  UuidUtils.stringToUuid(filter.getKeyValue()));
		}else if (filter.getAttributeType() == AttributeType.LIST) {
			if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)) {
				q.setParameter("value", filter.getKeyValue());
			}
		}
		q.executeUpdate();
		
	}
	
	/*
	 * add an attribute column to the entity type
	 */
	private void addEntityAttributeFilter(String queryTable, String columnName, RecordAttributeFilter filter, Session session) {
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE "); //$NON-NLS-1$
		sb.append(queryTable);
		sb.append( " ADD COLUMN ");
		sb.append(columnName);
		sb.append(" boolean default false "); //$NON-NLS-1$
		
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		
		sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append(queryTable);
		sb.append( " SET " ); //$NON-NLS-1$
		sb.append( columnName );
		sb.append(" = true where record_uuid in ( SELECT  "); //$NON-NLS-1$
		sb.append( " v.record_uuid ");
		sb.append(" FROM smart.i_record_attribute_value v join smart.i_recordsource_attribute b on b.uuid = v.attribute_uuid ");
		sb.append(" JOIN smart.i_record_source c on c.uuid = b.source_uuid and c.keyid = :sourceid");
		sb.append(" JOIN smart.i_record_attribute_value_list lt on lt.value_uuid = v.uuid ");
		sb.append(" WHERE "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" b.keyid = :keyid ");
		if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)) {
			sb.append("AND  lt.element_uuid = :value "); //$NON-NLS-1$"
		}else {
			sb.append(" AND lt.element_uuid IS NOT NULL ");
		}
		sb.append(" ) "); //$NON-NLS-1$
		
		logme(sb);
		
		Query<?> q = session.createNativeQuery(sb.toString())
			.setParameter("keyid", filter.getEntityTypeKey()) //$NON-NLS-1$;
			.setParameter("sourceid", filter.getRecordSourceKey());
		if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)) {
			q.setParameter("value", UuidUtils.stringToUuid(filter.getKeyValue()));
		}
		q.executeUpdate();		
	}
	
	
	/*
	 * Filters the data table by creating a new data table and only 
	 * including the elements that match the filter,  
	 */
	private String filterDataTable(Session session, String dataTable, IQueryFilter queryFilter, Map<IQueryFilter,String> filterToColumn) throws Exception {
		String table2 = SqlGenerator.createTempTableName();
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(table2);
		sb.append("(");
		for (int i = 0; i < cols.length; i ++) {
			sb.append( cols[i][0] );
			sb.append(" " );
			sb.append( cols[i][1] );
			sb.append(",");
			
			colName2Index.put(cols[i][0], i);
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO " ); //$NON-NLS-1$
		sb.append(table2);
		
		sb.append(" SELECT ");
		for (int i = 0; i < cols.length; i ++) {
			sb.append( cols[i][0] );
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		
		
		sb.append(" FROM " ); //$NON-NLS-1$
		sb.append(dataTable + " a"); //$NON-NLS-1$
				
		HashMap<String,Object> parameters = new HashMap<>();

		if (queryFilter != null) {
			sb.append(" WHERE "); //$NON-NLS-1$
			processFilter(queryFilter, filterToColumn, sb, parameters);
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
			session.createNativeQuery("DROP TABLE " + dataTable); //$NON-NLS-1$
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return table2;
	}
	
	
	private void processFilter(IQueryFilter queryFilter,  Map<IQueryFilter,String> filterToColumn, StringBuilder whereSql, HashMap<String,Object> parameters) throws Exception {
		
		if (filterToColumn.containsKey(queryFilter)) {
			whereSql.append(" " + filterToColumn.get(queryFilter) + " ");
			return;
		}
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
			processFilter(f.getFilter1(), filterToColumn, whereSql, parameters);
			whereSql.append( SqlGenerator.operatorToSql(f.getOperator()) );
			processFilter(f.getFilter2(), filterToColumn, whereSql, parameters);
			
		}else if (queryFilter instanceof BracketFilter) {
			BracketFilter f = (BracketFilter)queryFilter;
			whereSql.append(" ( "); //$NON-NLS-1$
			processFilter(f.getFilter(), filterToColumn, whereSql, parameters);
			whereSql.append(" ) "); //$NON-NLS-1$
		}else if (queryFilter instanceof NotFilter) {
			NotFilter nf = (NotFilter)queryFilter;
			whereSql.append(" "); //$NON-NLS-1$
			whereSql.append(SqlGenerator.operatorToSql(Operator.NOT));
			whereSql.append(" "); //$NON-NLS-1$
			processFilter(nf.getFilter(), filterToColumn, whereSql, parameters);
		}
	}
	
	private void logme(StringBuilder sb) {
		logme(sb.toString());
	}
	private void logme(String sb) {
		System.out.println(sb.toString());
	}
	
}
