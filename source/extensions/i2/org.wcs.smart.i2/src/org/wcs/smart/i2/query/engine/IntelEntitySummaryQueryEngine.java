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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.i2.query.SummaryHeader;
import org.wcs.smart.i2.query.SummaryQueryResult;
import org.wcs.smart.i2.query.SummaryResultKey;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem.GroupByType;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.i2.query.observation.filter.ValuePart;
import org.wcs.smart.i2.query.observation.filter.ValuePart.ValueOption;
import org.wcs.smart.util.UuidUtils;

/**
 * Entity summary query engine.
 * 
 * @author Emily
 *
 */
public class IntelEntitySummaryQueryEngine implements IIntelQueryEngine{
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
		SubMonitor progress = SubMonitor.convert(monitor, Messages.IntelObservationQueryEngine_Progress1, 6);

		//Locale
		Locale locale = (Locale) parameters.get(Locale.class.getName());
		if (locale == null){
			locale = Locale.getDefault();
		}
		
		//Conservation Area
		ConservationArea ca = (ConservationArea)parameters.get(ConservationArea.class.getName());
		if (ca == null){
			 throw new Exception(Messages.IntelObservationQueryEngine_InvalidCaParameter);
		}
		progress.subTask(Messages.IntelObservationQueryEngine_Progress2);
		
		//parse query
		SumQueryDefinition parsedQuery = IntelEntitySummaryQuery.parseQuery(query.getQueryString());
		progress.worked(1);
		
		final SubMonitor fmonitor = progress;
		final Locale flocale = locale;
		
		
		String queryTable = createTemporaryEntityTable(session, Collections.singletonList(ca));
		try {
			addAttributeColumns(parsedQuery, queryTable, session);
			SummaryQueryResult results = getResults(queryTable, parsedQuery, session);
			return results;
		}catch (OperationCanceledException ex) {
			return null;
		}catch (Exception ex) {
			throw ex;
		}finally {
			//drop table
			try {
				session.createNativeQuery("DROP TABLE " + queryTable); //$NON-NLS-1$
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}	
	}
	
	
	private SummaryQueryResult getResults(String queryTable, SumQueryDefinition definition, Session session) throws Exception {
		
		StringBuilder selectSql = new StringBuilder();
		StringBuilder groupBySql = new StringBuilder();
		
		int cnt = 0;
		List<GroupByItem> groupByItems = new ArrayList<>();
		groupByItems.addAll(definition.getRowGroupByPart().getItems());
		groupByItems.addAll(definition.getColumnGroupByPart().getItems());
		
		List<String> entityTypeFilters = new ArrayList<>();
		for (GroupByItem groupBy : groupByItems) {
			if (cnt!= 0) {
				selectSql.append(",");
				groupBySql.append(",");
			}
			cnt++;
			
			if (groupBy.getGroupByType() == GroupByType.ENTITYTYPE) {
				selectSql.append("entity_type_key");
				groupBySql.append("entity_type_key");
			}else if(groupBy.getGroupByType() == GroupByType.ATTRIBUTE) {
				String columnName = groupBy.getAttributeKey();
				if (groupBy.getEntityTypeKey() != null && !groupBy.getEntityTypeKey().isEmpty()) {
					entityTypeFilters.add(groupBy.getEntityTypeKey());
				}
				if (groupBy.getAttributeType() == AttributeType.DATE) {
					GroupByItem.DateOption dateOp = groupBy.getDateOption();
					switch(dateOp) {
						case DAY:
							selectSql.append(columnName);
							groupBySql.append(columnName);
							break;
						case MONTH:
							selectSql.append("cast(year(" + columnName + ") as char(4)) || '-' || trim(cast(month(" + columnName + ") as char(2)))");
							groupBySql.append("cast(year(" + columnName + ") as char(4)) || '-' || trim(cast(month(" + columnName + ") as char(2)))");
							break;
						case YEAR:
							selectSql.append("year(" + columnName + ")");
							groupBySql.append("year(" + columnName + ")");
							break;
					}
					
				}else if (groupBy.getAttributeType() == AttributeType.LIST) {
					selectSql.append(columnName);
					groupBySql.append(columnName);
					
				}else if (groupBy.getAttributeType() == AttributeType.EMPLOYEE) {
					selectSql.append(columnName);
					groupBySql.append(columnName);
					
				}else if (groupBy.getAttributeType() == AttributeType.POSITION) {
					//TODO:
				}	
			}
		}
		

		if (selectSql.length() > 0) {
			selectSql.append(",");
		}
		if (definition.getValuePart().getValueOption() == ValueOption.NUMBER_ENTITIES) {
			selectSql.append(" count(entity_uuid)");
		}
		
		//TODO: filter where clause
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(selectSql);
		sb.append(" FROM ");
		sb.append(queryTable);
		
		if (!entityTypeFilters.isEmpty()) {
			sb.append(" WHERE ");
			sb.append("entity_type_key IN (:entityTypeFilters)");
		}
		
		if (groupBySql.length() > 0) {
			sb.append(" GROUP BY ");
			sb.append(groupBySql);
		}
		
		
		
		logme(sb);
		
		
		SummaryQueryResult results = new SummaryQueryResult();
		getHeaderInfo(definition, results, session);
		
//		results.setData(dataResults);
		HashMap<SummaryResultKey, Double> data = new HashMap<>();
		NativeQuery query = session.createNativeQuery(sb.toString());
		if (!entityTypeFilters.isEmpty()) {
			query.setParameterList("entityTypeFilters",  entityTypeFilters);
		}
		List<Object> dataItems = query.list();
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
				String colkey = computeColumnKey(groupBy);
				Object value = rowdata[column++];
				
				if (groupBy.getAttributeType() != null && groupBy.getAttributeType() == AttributeType.EMPLOYEE && value != null) {
					value = UuidUtils.uuidToString( UuidUtils.byteToUUID((byte[])value) );
				}
				
				if (value == null) {
					value = "";
				}
				groupBys[index++] = colkey + ":" + value.toString();
				
			}
			
			SummaryResultKey temp = new SummaryResultKey(definition.getValuePart().getValueOption().getKey(), groupBys);
			//last column is value
			Number value = (Number) rowdata[column++];
			data.put(temp, value.doubleValue());
		}
		results.setData(data);
		return results;
	}

	private String createTemporaryEntityTable(Session session, List<ConservationArea> cas) {
		String obsTable = SqlGenerator.createTempTableName();
		
		//create table
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ");
		sb.append(obsTable);
		sb.append(" (entity_uuid char(16) for bit data, entity_type_key varchar(128))");
		
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append("INSERT INTO ");
		sb.append(obsTable);
		sb.append("(entity_uuid, entity_type_key) ");
		sb.append("SELECT a.uuid, b.keyid FROM ");
		sb.append(" smart.i_entity a join smart.i_entity_type b on a.entity_type_uuid = b.uuid ");
		sb.append(" WHERE b.ca_uuid in (:cauuids)");
		
		List<UUID> cauuids = cas.stream().map(e->e.getUuid()).collect(Collectors.toList());
		
		logme(sb);
		session.createNativeQuery(sb.toString())
			.setParameterList("cauuids",  cauuids)
			.executeUpdate();
		
		return obsTable;
	}
	
	private void addAttributeColumns(SumQueryDefinition def, String queryTable, Session session) {
		List<String> attributes = new ArrayList<>();
		HashMap<String, IntelAttribute.AttributeType> types = new HashMap<>();
		
		for (GroupByItem item : def.getRowGroupByPart().getItems()) {
			String attributeKey = item.getAttributeKey();
			if (attributeKey == null) continue;
			if (attributes.contains(attributeKey)) continue;
			
			attributes.add(attributeKey);
			types.put(attributeKey, item.getAttributeType());
		}
		
		for (String a : attributes) {
			addAttributeColumn(queryTable, a, types.get(a), session);
		}
	}
	
	
	private String getColumnType(IntelAttribute.AttributeType type) {
		switch(type) {
		case BOOLEAN:
			return "boolean";
		case DATE:
			return "date";
		case EMPLOYEE:
			return "char(16) for bit data";
		case LIST:
			return "varchar(128)";
		case NUMERIC:
			return "double";
		case POSITION:
			//TODO:
			break;
		case TEXT:
			return "varchar (1024)";
		}
		return null;
	}
	
	private String addAttributeColumn(String queryTable, String attributeKey, IntelAttribute.AttributeType type, Session session) {
		String columnName = attributeKey;
		
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE ");
		sb.append(queryTable);
		sb.append( " ADD COLUMN " );
		sb.append( columnName );
		sb.append(" ");
		sb.append( getColumnType(type));
		
		logme(sb);
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		String selectClause = null;
		switch (type) {
		case BOOLEAN:
			selectClause = "case when v.double_value > 0.5 then true else false end";
			break;
			
		case DATE:
			selectClause = "cast(v.string_value as date)";
			break;
		case EMPLOYEE:
			selectClause = "v.employee_uuid";
			break;
		case LIST:
			selectClause = "l.keyid";
			break;
		case NUMERIC:
			selectClause = "v.double_value";
			break;
		case POSITION:
			break;
		case TEXT:
			selectClause = "v.string_value";
			break;
		}

		sb = new StringBuilder();
		sb.append("UPDATE ");
		sb.append(queryTable);
		sb.append( " SET " );
		sb.append( columnName );
		sb.append(" = ( SELECT  ");
		sb.append( selectClause );
		sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid ");
		if (type == AttributeType.LIST) {
			sb.append("JOIN smart.i_attribute_list_item l on l.uuid = v.list_item_uuid");
		}
		sb.append(" WHERE v.entity_uuid = " + queryTable + ".entity_uuid ");
//		sb.append(" and v.double_value is not null ");
		sb.append(" and a.keyid = :keyid ");
		sb.append(" ) ");
		
		logme(sb);
		
		session.createNativeQuery(sb.toString())
			.setParameter("keyid", attributeKey)
			.executeUpdate();
		
		return columnName;
		
	}
	
	private void logme(StringBuilder sb) {
		System.out.println(sb.toString());
	}
	
	
	private static String computeColumnKey(GroupByItem item) {
		String colkey = null;
		if (item.getGroupByType() == GroupByType.ENTITYTYPE) {
			colkey = "ET";
		}else {
			colkey = item.getAttributeKey();
			if (item.getEntityTypeKey() != null) {
				colkey += "_" + item.getEntityTypeKey();
			}
		}
		return colkey;
	}
	/**
	 * Computes the header information for a given
	 * query.
	 * 
	 * @param query the summary query
	 * @param results the summary query results to update
	 * @param session hibernate session
	 */
	public static void getHeaderInfo(SumQueryDefinition queryDefinition, SummaryQueryResult results, Session session) throws Exception{
		
		// value headers
		ValuePart vp = queryDefinition.getValuePart();
		SummaryHeader header = new SummaryHeader(vp.getValueOption().name(), vp.getValueOption().name(), vp.getValueOption().getKey(), true);
		results.addValueHeader(header);

		for (GroupByItem item : queryDefinition.getRowGroupByPart().getItems()) {
			List<ListItem> allItems = item.getAllOptions(session, SmartDB.getCurrentConservationArea());
			String colkey = computeColumnKey(item);
			//TODO: date options 
			List<SummaryHeader> rowHeaders = new ArrayList<>();
			
			for (int i = 0; i < allItems.size(); i ++){
				ListItem it = allItems.get(i);
				if (item.getFilterOptions() == null || item.getFilterOptions().isEmpty()) {
					rowHeaders.add( new SummaryHeader( it.getName(), it.getName(), colkey, it.getKeyId(), false) );
				}else {
					for (String key : item.getFilterOptions()) {
						if (key.equals(it.getKeyId())) {
							rowHeaders.add( new SummaryHeader( it.getName(), it.getName(), colkey, it.getKeyId(), false) );
							break;
						}
					}
				}
					
				
			}
			results.addRowHeader(rowHeaders.toArray(new SummaryHeader[rowHeaders.size()]));
		}
		
		for (GroupByItem item : queryDefinition.getColumnGroupByPart().getItems()) {
			List<ListItem> allItems = item.getAllOptions(session, SmartDB.getCurrentConservationArea());
			String colkey = computeColumnKey(item);
			
			List<SummaryHeader> rowHeaders = new ArrayList<>();
			
			for (int i = 0; i < allItems.size(); i ++){
				ListItem it = allItems.get(i);
				if (item.getFilterOptions() == null) {
					rowHeaders.add( new SummaryHeader( it.getName(), it.getName(), colkey, it.getKeyId(), false) );
				}else {
					for (String key : item.getFilterOptions()) {
						if (key.equals(it.getKeyId())) {
							rowHeaders.add( new SummaryHeader( it.getName(), it.getName(), colkey, it.getKeyId(), false) );
							break;
						}
					}
				}
					
				
			}
			results.addColumnHeader(rowHeaders.toArray(new SummaryHeader[rowHeaders.size()]));
		}
		
	}
}
