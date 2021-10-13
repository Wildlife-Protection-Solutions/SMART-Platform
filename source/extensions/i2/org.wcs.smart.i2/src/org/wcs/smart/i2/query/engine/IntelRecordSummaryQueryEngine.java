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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSummaryQuery;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.SummaryQueryResult;
import org.wcs.smart.i2.query.SummaryResultKey;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem.GroupByType;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.ValuePart.ValueOption;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Entity summary query engine.
 * 
 * @author Emily
 *
 */
public class IntelRecordSummaryQueryEngine implements IIntelQueryEngine{
	
	private DataTable dataTable;
	
	
	/**
	 * Parameters required are session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	@Override
	public IQueryResult executeQuery(AbstractIntelQuery query,  HashMap<String, Object> parameters) throws Exception{
		
		Session session = (Session) parameters.get(Session.class.getName());
		IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		SubMonitor progress = SubMonitor.convert(monitor, Messages.IntelRecordSummaryQueryEngine_Task1, 8);

		try {
			//Locale
			Locale locale = (Locale) parameters.get(Locale.class.getName());
			if (locale == null){
				locale = Locale.getDefault();
			}
			
			//Conservation Area
			@SuppressWarnings("unchecked")
			Collection<ConservationArea> cas = (Collection<ConservationArea>)parameters.get(ConservationArea.class.getName());
			if (cas == null){
				 throw new Exception(Messages.IntelObservationQueryEngine_InvalidCaParameter);
			}
			IQueryItemProvider itemProvider = InternalQueryManager.INSTANCE.getQueryItemProvider();
			LocalDate[] dates = (LocalDate[]) parameters.get(LocalDate.class.getName());
			
			Set<UUID> profiles = new HashSet<>();
			for (String ip : IntelEntityRecordQuery.convertFromProfileFilter(query.getProfileFilter())) {
				List<IntelProfile> items = session.createQuery("FROM IntelProfile WHERE keyId = :keyId and conservationArea in (:cas)", IntelProfile.class) //$NON-NLS-1$
						.setParameter("keyId",  ip) //$NON-NLS-1$
						.setParameter("cas", cas).list(); //$NON-NLS-1$
				for (IntelProfile ip2 : items) {
					if (IntelSecurityManager.INSTANCE.canViewQuery(ip2)) profiles.add(ip2.getUuid());
				}
			}
			
			//parse query
			progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressParsing);
			SumQueryDefinition parsedQuery = IntelRecordSummaryQuery.parseQuery(query.getQueryString());
			progress.worked(1);

			//parse query
			progress.subTask(Messages.IntelRecordSummaryQueryEngine_Task2);
			RecordFilterProcessor p = new RecordFilterProcessor();
			String fdataTable = p.processFilter(parsedQuery.getFilter(), profiles, dates, cas, session, monitor);
			dataTable = new DataTable(fdataTable);

			if (dates[0] == null) {
				dates = computeDateRange(dataTable.tableName, session);
			}
			progress.worked(1);
			
			//add attribute columns
			progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressAttributeColumns);
			addAttributeColumns(parsedQuery, dataTable.tableName, session, progress.split(1));
			
			progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressLoadingResults);
			LocalDate[] ldates = new LocalDate[2];
			ldates[0] = dates[0];
			ldates[1] = dates[1];
			SummaryQueryResult results = getResults(dataTable.tableName, parsedQuery, ldates, locale, profiles, itemProvider, session);
			progress.worked(1);
			
			
			return results;
		}catch (OperationCanceledException ex) {
			return null;
		}catch (Exception ex) {
			throw ex;
		}finally {
			//drop table
			try {
				if (dataTable != null) session.createNativeQuery("DROP TABLE " + dataTable.tableName).executeUpdate(); //$NON-NLS-1$
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}	
	}
	
//	/*
//	 * create the necessary area group by tables.  For area group by attributes we create
//	 * a separate table as it is possible for a single position attribute to be in multiple
//	 * areas and we want to count each one
//	 */
//	private Map<GroupByItem, String> processAreaGroupBys(String queryTable, SumQueryDefinition definition, Collection<ConservationArea> cas, Session session) {
//		List<GroupByItem> groupByItems = new ArrayList<>();
//		groupByItems.addAll(definition.getRowGroupByPart().getItems());
//		groupByItems.addAll(definition.getColumnGroupByPart().getItems());
//		
//		HashMap<GroupByItem, String> areaToTables = new HashMap<>();
//		for (GroupByItem groupBy : groupByItems) {
//			if (groupBy.getAttributeKey() == null || groupBy.getAttributeType() != AttributeType.POSITION) continue;
//		
//			String tableName = SqlGenerator.createTempTableName();
//			StringBuilder sb = new StringBuilder();
//			sb.append(" CREATE TABLE "); //$NON-NLS-1$
//			sb.append(tableName);
//			sb.append(" (entity_uuid char(16) for bit data, keyid varchar(128)) "); //$NON-NLS-1$
//			
//			logme(sb);
//			session.createNativeQuery(sb.toString()).executeUpdate();
//
//			sb = new StringBuilder();
//			sb.append(" INSERT INTO "); //$NON-NLS-1$
//			sb.append(tableName);
//			sb.append(" (entity_uuid, keyid) "); //$NON-NLS-1$
//			sb.append("SELECT a.entity_uuid, '" + groupBy.getAreaType().name() + "_' || b.keyid "); //$NON-NLS-1$ //$NON-NLS-2$
//			sb.append(" FROM "); //$NON-NLS-1$
//			sb.append(queryTable + " a "); //$NON-NLS-1$
//			sb.append(" JOIN smart.i_entity_attribute_value v ON a.entity_uuid = v.entity_uuid "); //$NON-NLS-1$
//			sb.append(" JOIN smart.i_attribute aa on aa.uuid = v.attribute_uuid and aa.keyid = :attributeKey "); //$NON-NLS-1$
//			sb.append(", "); //$NON-NLS-1$
//			sb.append(" smart.area_geometries b"); //$NON-NLS-1$
//			sb.append(" WHERE "); //$NON-NLS-1$
//			sb.append(" b.area_type = :areaType "); //$NON-NLS-1$
//			sb.append(" AND "); //$NON-NLS-1$
//			sb.append(" smart.pointinpolygon(v.double_value, v.double_value2, null, null, b.geom)"); //$NON-NLS-1$
//			sb.append(" AND b.ca_uuid in (:cas)"); //$NON-NLS-1$
//			
//			logme(sb);
//			session.createNativeQuery(sb.toString())
//				.setParameter("attributeKey",  groupBy.getAttributeKey()) //$NON-NLS-1$
//				.setParameter("areaType",  groupBy.getAreaType().name()) //$NON-NLS-1$
//				.setParameterList("cas", cas) //$NON-NLS-1$
//				.executeUpdate();
//			
//			areaToTables.put(groupBy,  tableName);
//			
//		}
//		
//		return areaToTables;
//	}
	
	/*
	 * compute the minimum and maximum date values over all date attributes, so
	 * we can determine the range for the summary headers
	 */
	private LocalDate[] computeDateRange(String queryTable, Session session) {
		
		String field = SystemAttributeFilter.SystemAttribute.RECORD_DATE.name().toLowerCase(Locale.ROOT);
		Object[] items = (Object[]) session.createNativeQuery("SELECT min(" +field+ "), max(" +field+ ") FROM " + queryTable).uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		LocalDate l1 = LocalDate.now();
		LocalDate l2 = LocalDate.now();
		if (items[0] != null) l1 = ((java.sql.Date)items[0]).toLocalDate();
		if (items[1] != null) l2 = ((java.sql.Date)items[1]).toLocalDate();
		return new LocalDate[] {l1, l2};
	}
	
	/*
	 * Runs a sql statement on the data table to get the results for the table
	 */
	private SummaryQueryResult getResults(String queryTable, SumQueryDefinition definition, LocalDate[] dateRange, 
			Locale l, Set<UUID> profiles, IQueryItemProvider itemProvider, Session session) throws Exception {
		
		StringBuilder selectSql = new StringBuilder();
		StringBuilder groupBySql = new StringBuilder();
		
		int cnt = 0;
		List<GroupByItem> groupByItems = new ArrayList<>();
		groupByItems.addAll(definition.getColumnGroupByPart().getItems());
		groupByItems.addAll(definition.getRowGroupByPart().getItems());

		for (GroupByItem groupBy : groupByItems) {
			if (cnt!= 0) {
				selectSql.append(","); //$NON-NLS-1$
				groupBySql.append(","); //$NON-NLS-1$
			}
			cnt++;
			
			if (groupBy.getGroupByType() == GroupByType.CA) {
				selectSql.append("ca_uuid as c_" + cnt); //$NON-NLS-1$
				groupBySql.append("ca_uuid "); //$NON-NLS-1$
			}else if (groupBy.getGroupByType() == GroupByType.RECORDSOURCE) {
				selectSql.append("record_source_key as c_" + cnt); //$NON-NLS-1$
				groupBySql.append("record_source_key "); //$NON-NLS-1$
			}else if (groupBy.getGroupByType() == GroupByType.RECORDSTATUS) {
				selectSql.append("record_status as c_" + cnt); //$NON-NLS-1$
				groupBySql.append("record_status "); //$NON-NLS-1$
			}else if(groupBy.getGroupByType() == GroupByType.RECORD_ATTRIBUTE) {
				String columnName = groupBy.getAttributeKey();
				if (groupBy.getAttributeType() == AttributeType.DATE) {
					GroupByItem.DateOption dateOp = groupBy.getDateOption();
					switch(dateOp) {
						case DAY:
							selectSql.append(columnName+ " as c_" + cnt); //$NON-NLS-1$
							groupBySql.append(columnName);
							break;
						case MONTH:
							selectSql.append("cast(year(" + columnName + ") as char(4)) || '-' || trim(cast(month(" + columnName + ") as char(2))) as c_" + cnt); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							groupBySql.append("cast(year(" + columnName + ") as char(4)) || '-' || trim(cast(month(" + columnName + ") as char(2)))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							break;
						case YEAR:
							selectSql.append("year(" + columnName + ") as c_" + cnt); //$NON-NLS-1$ //$NON-NLS-2$
							groupBySql.append("year(" + columnName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
							break;
					}
					
				}else if (groupBy.getAttributeType() == AttributeType.LIST) {
					selectSql.append(columnName + " as c_" + cnt); //$NON-NLS-1$
					groupBySql.append(columnName);
					
				}else if (groupBy.getAttributeType() == AttributeType.EMPLOYEE) {
					selectSql.append(columnName + " as c_" + cnt); //$NON-NLS-1$
					groupBySql.append(columnName);
				}	
			}else if (groupBy.getGroupByType() == GroupByType.SYSTEM) {
				SystemAttributeFilter.SystemAttribute attribute = groupBy.getSystemAttribute();
				
				String columnName = attribute.name().toLowerCase(Locale.ROOT);

				GroupByItem.DateOption dateOp = groupBy.getDateOption();
				switch(dateOp) {
					case DAY:
						selectSql.append(columnName+ " as c_" + cnt); //$NON-NLS-1$
						groupBySql.append(columnName);
						break;
					case MONTH:
						selectSql.append("cast(year(" + columnName + ") as char(4)) || '-' || trim(cast(month(" + columnName + ") as char(2))) as c_" + cnt); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						groupBySql.append("cast(year(" + columnName + ") as char(4)) || '-' || trim(cast(month(" + columnName + ") as char(2)))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						break;
					case YEAR:
						selectSql.append("year(" + columnName + ") as c_" + cnt); //$NON-NLS-1$ //$NON-NLS-2$
						groupBySql.append("year(" + columnName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
						break;
				}				
			}
		}
		

		if (selectSql.length() > 0) {
			selectSql.append(","); //$NON-NLS-1$
		}
		if (definition.getValuePart().getValueOption() == ValueOption.NUMBER_RECORDS) {
			selectSql.append(" count(distinct " + queryTable+ ".record_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT "); //$NON-NLS-1$
		sb.append(selectSql);
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append(queryTable);

		if (groupBySql.length() > 0) {
			sb.append(" GROUP BY "); //$NON-NLS-1$
			sb.append(groupBySql);
		}
		
		logme(sb);
		
		SummaryQueryResult results = new SummaryQueryResult();
		EntitySummaryQueryHeaderEngine.INSTANCE.getHeaderInfo(definition, results, dateRange, profiles, itemProvider, l, session);
		
		HashMap<SummaryResultKey, Double> data = new HashMap<>();
		
		NativeQuery<?> query = session.createNativeQuery(sb.toString());
		List<?> dataItems = query.list();
		for (Object item : dataItems) {
			if (item instanceof Number) {
				item = new Object[] {item};
			}
			Object[] rowdata = (Object[])item;
			
			//now what?
			int column = 0;
			
			String[] groupBys = new String[groupByItems.size()];
			int index = 0;
			for (GroupByItem groupBy : groupByItems) {
				String colkey = EntitySummaryQueryHeaderEngine.INSTANCE.computeColumnKey(groupBy);
				Object value = rowdata[column++];
				
				if (groupBy.getAttributeType() != null && groupBy.getAttributeType() == AttributeType.EMPLOYEE && value != null) {
					value = UuidUtils.uuidToString( UuidUtils.byteToUUID((byte[])value) );
				}
				if (groupBy.getGroupByType() == GroupByType.CA) {
					value = UuidUtils.uuidToString( UuidUtils.byteToUUID((byte[])value) );
				}
				
				if (value == null) {
					value = ""; //$NON-NLS-1$
				}
				groupBys[index++] = colkey + ":" + value.toString(); //$NON-NLS-1$
				
			}
			
			SummaryResultKey temp = new SummaryResultKey(definition.getValuePart().getValueOption().getKey(), groupBys);
			//last column is value
			Number value = (Number) rowdata[column++];
			data.put(temp, value.doubleValue());
		}
		results.setData(data);
		return results;
	}


	
	/*
	 * add all attribute columns to entity data table
	 */
	private Map<String, String> addAttributeColumns(SumQueryDefinition def, String queryTable, Session session, IProgressMonitor monitor) {

		SubMonitor progress = SubMonitor.convert(monitor, 1);
		
		List<GroupByItem> allItems = new ArrayList<>();
		allItems.addAll(def.getRowGroupByPart().getItems());
		allItems.addAll(def.getColumnGroupByPart().getItems());
		
		Map<String, String> attributeToColumnKey = new HashMap<>();
				
		progress.setWorkRemaining(allItems.size()+1);
		for (GroupByItem item : allItems) {
			progress.worked(1);
			String attributeKey = item.getAttributeKey();
			if (attributeKey == null) continue;
			if (item.getAttributeType()== AttributeType.POSITION) continue; //  position attribute dealt with outside of here
			if (!attributeToColumnKey.containsKey(attributeKey)) {
				String columnName = addAttributeColumn(queryTable, attributeKey, item.getAttributeType(), item.getOtherKey(), session);
				attributeToColumnKey.put(attributeKey, columnName);
			}
		}
		

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
			return "varchar(128)"; //$NON-NLS-1$
		case NUMERIC:
			return "double"; //$NON-NLS-1$
		case POSITION:
			//group by position attributes are dealt with in a different way; should never execute this code
			throw new IllegalStateException("Should not group by position attributes."); //$NON-NLS-1$
		case TEXT:
			return "varchar (" + IntelAttribute.MAX_TEXT_LENGTH + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}
	
	/*
	 * add an attribute column to the entity type
	 */
	private String addAttributeColumn(String queryTable, String attributeKey, IntelAttribute.AttributeType type, String recordSource, Session session) {
		String columnName = attributeKey;
		String columnType = getColumnType(type);
		
		
		
		dataTable.addColumn(columnName,  columnType);
		
		if (type == AttributeType.DATE) {
			StringBuilder sb = new StringBuilder();
			sb.append("ALTER TABLE "); //$NON-NLS-1$
			sb.append(queryTable);
			sb.append( " ADD COLUMN " ); //$NON-NLS-1$
			sb.append( columnName );
			sb.append(" "); //$NON-NLS-1$
			sb.append( getColumnType(type));
			
			logme(sb);
			session.createNativeQuery(sb.toString()).executeUpdate();
			
			sb = new StringBuilder();
			sb.append("UPDATE "); //$NON-NLS-1$
			sb.append(queryTable);
			sb.append( " SET " ); //$NON-NLS-1$
			sb.append( columnName );
			sb.append(" = ( SELECT cast(v.string_value as date) "); //$NON-NLS-1$
			sb.append(" FROM smart.i_record_attribute_value v join smart.i_recordsource_attribute b on b.uuid = v.attribute_uuid "); //$NON-NLS-1$
			sb.append(" JOIN smart.i_recordsource c on c.uuid = b.source_uuid and c.keyid = :source and b.keyid = :keyid"); //$NON-NLS-1$
			sb.append(" WHERE v.record_uuid = " + queryTable + ".record_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ) "); //$NON-NLS-1$
			
			logme(sb);
			
			session.createNativeQuery(sb.toString())
				.setParameter("keyid", attributeKey) //$NON-NLS-1$
				.setParameter("source", recordSource) //$NON-NLS-1$
				.executeUpdate();
		}else if (type == AttributeType.LIST) {
			String temp = SqlGenerator.createTempTableName();
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE "); //$NON-NLS-1$
			sb.append(temp );
			sb.append( " AS SELECT * FROM " ); //$NON-NLS-1$
			sb.append( dataTable.tableName );
			sb.append(" WITH NO DATA"); //$NON-NLS-1$
			logme(sb);
			session.createNativeQuery(sb.toString()).executeUpdate();
			sb = new StringBuilder();
			sb.append("ALTER TABLE "); //$NON-NLS-1$
			sb.append(temp);
			sb.append( " ADD COLUMN " ); //$NON-NLS-1$
			sb.append( columnName );
			sb.append(" "); //$NON-NLS-1$
			sb.append( getColumnType(type));
			
			logme(sb);
			session.createNativeQuery(sb.toString()).executeUpdate();
			
			sb = new StringBuilder();
			sb.append(" INSERT INTO "); //$NON-NLS-1$
			sb.append( temp );
			sb.append (" SELECT a.*, al.keyid FROM "); //$NON-NLS-1$
			sb.append( dataTable.tableName );
			sb.append(" a LEFT JOIN ( smart.i_record_attribute_value v "); //$NON-NLS-1$
			sb.append(" JOIN smart.i_record_attribute_value_list b on b.value_uuid = v.uuid "); //$NON-NLS-1$
			sb.append(" JOIN smart.i_attribute_list_item al on b.element_uuid = al.uuid "); //$NON-NLS-1$
			sb.append( " JOIN smart.i_recordsource_attribute c on v.attribute_uuid = c.uuid AND c.keyid = :keyid "); //$NON-NLS-1$
			sb.append( " JOIN smart.i_recordsource d on c.source_uuid = d.uuid AND d.keyid = :sourceid )  on v.record_uuid = a.record_uuid "); //$NON-NLS-1$
			
			logme(sb);
			session.createNativeQuery(sb.toString())
				.setParameter("keyid", attributeKey) //$NON-NLS-1$
				.setParameter("sourceid", recordSource).executeUpdate(); //$NON-NLS-1$
			
			session.createNativeQuery("DROP TABLE " + dataTable.tableName).executeUpdate(); //$NON-NLS-1$
			
			session.createNativeQuery("RENAME TABLE " + temp + " TO " + dataTable.tableName).executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
			
		}else if (type == AttributeType.EMPLOYEE) {
			String temp = SqlGenerator.createTempTableName();
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE "); //$NON-NLS-1$
			sb.append(temp );
			sb.append( " AS SELECT * FROM " ); //$NON-NLS-1$
			sb.append( dataTable.tableName );
			sb.append(" WITH NO DATA"); //$NON-NLS-1$
			logme(sb);
			session.createNativeQuery(sb.toString()).executeUpdate();
			sb = new StringBuilder();
			sb.append("ALTER TABLE "); //$NON-NLS-1$
			sb.append(temp);
			sb.append( " ADD COLUMN " ); //$NON-NLS-1$
			sb.append( columnName );
			sb.append(" "); //$NON-NLS-1$
			sb.append( getColumnType(type));
			
			logme(sb);
			session.createNativeQuery(sb.toString()).executeUpdate();
			
			sb = new StringBuilder();
			sb.append(" INSERT INTO "); //$NON-NLS-1$
			sb.append( temp );
			sb.append (" SELECT a.*, al.uuid FROM "); //$NON-NLS-1$
			sb.append( dataTable.tableName );
			sb.append(" a LEFT JOIN ( smart.i_record_attribute_value v "); //$NON-NLS-1$
			sb.append(" JOIN smart.i_record_attribute_value_list b on b.value_uuid = v.uuid "); //$NON-NLS-1$
			sb.append(" JOIN smart.employee al on b.element_uuid = al.uuid "); //$NON-NLS-1$
			sb.append( " JOIN smart.i_recordsource_attribute c on v.attribute_uuid = c.uuid AND c.keyid = :keyid "); //$NON-NLS-1$
			sb.append( " JOIN smart.i_recordsource d on c.source_uuid = d.uuid AND d.keyid = :sourceid )  on v.record_uuid = a.record_uuid "); //$NON-NLS-1$
			
			logme(sb);
			session.createNativeQuery(sb.toString())
				.setParameter("keyid", attributeKey) //$NON-NLS-1$
				.setParameter("sourceid", recordSource).executeUpdate(); //$NON-NLS-1$
			
			session.createNativeQuery("DROP TABLE " + dataTable.tableName).executeUpdate(); //$NON-NLS-1$
			
			session.createNativeQuery("RENAME TABLE " + temp + " TO " + dataTable.tableName).executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
		}

		
		return columnName;
		
	}
	
	private void logme(StringBuilder sb) {
		logme(sb.toString());
	}
	private void logme(String sb) {
		if (Intelligence2PlugIn.LOG_QUERY) System.out.println(sb.toString());
	}
	
	/*
	 * simple class to track the datatable and columns added to the table
	 */
	private class DataTable {
		String tableName;
		HashMap<String, String> columnNames = new HashMap<>();
		
		public DataTable(String tableName) {
			this.tableName = tableName;
		}
		
		public void addColumn(String name, String type) {
			columnNames.put(name, type);
		}
	}
	
	
}
