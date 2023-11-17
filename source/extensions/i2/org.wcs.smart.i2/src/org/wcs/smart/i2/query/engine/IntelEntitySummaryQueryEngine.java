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
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
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
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.SummaryQueryResult;
import org.wcs.smart.i2.query.SummaryResultKey;
import org.wcs.smart.i2.query.observation.filter.BooleanFilter;
import org.wcs.smart.i2.query.observation.filter.BracketFilter;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem.GroupByType;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.NotFilter;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;
import org.wcs.smart.i2.query.observation.filter.ValuePart.ValueOption;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;

/**
 * Entity summary query engine.
 * 
 * @author Emily
 *
 */
public class IntelEntitySummaryQueryEngine implements IIntelQueryEngine{
	
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
		SubMonitor progress = SubMonitor.convert(monitor, Messages.IntelEntitySummaryQueryEngine_progressProcessing, 8);

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
		SumQueryDefinition parsedQuery = IntelEntitySummaryQuery.parseQuery(query.getQueryString());
		progress.worked(1);
		
		//parse query
		progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressEntityType);
		createTemporaryEntityTable(session,profiles,cas);
		progress.worked(1);
		
		try {
			//add attribute columns
			progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressAttributeColumns);
			Map<String, String> attributeKeyToColumn = addAttributeColumns(parsedQuery, dataTable.tableName, session, progress.split(1));
			
			if (parsedQuery.getFilter() != null) {
				progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressFilter);
				filterDataTable(session, parsedQuery.getFilter());
			}
			progress.worked(1);
			
			progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressDateRange);
			LocalDate[] dateRange = computeDateRange(parsedQuery, dataTable.tableName, attributeKeyToColumn, session);
			progress.worked(1);
			
			progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressAreaGroupBy);
			Map<GroupByItem, String> areaTables = processAreaGroupBys(dataTable.tableName, parsedQuery, cas, session);
			progress.worked(1);
			
			progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressLoadingResults);
			SummaryQueryResult results = getResults(dataTable.tableName, parsedQuery, dateRange, areaTables, locale, profiles, itemProvider, session);
			progress.worked(1);
			
			progress.subTask(Messages.IntelEntitySummaryQueryEngine_progressCleanUp);
			for (String tableName : areaTables.values()) {
				try {
					session.createNativeMutationQuery("DROP TABLE " + tableName).executeUpdate(); //$NON-NLS-1$
				}catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			progress.worked(1);
			
			return results;
		}catch (OperationCanceledException ex) {
			return null;
		}catch (Exception ex) {
			throw ex;
		}finally {
			//drop table
			try {
				session.createNativeMutationQuery("DROP TABLE " + dataTable.tableName).executeUpdate(); //$NON-NLS-1$
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}	
	}
	
	/*
	 * create the necessary area group by tables.  For area group by attributes we create
	 * a separate table as it is possible for a single position attribute to be in multiple
	 * areas and we want to count each one
	 */
	private Map<GroupByItem, String> processAreaGroupBys(String queryTable, SumQueryDefinition definition, Collection<ConservationArea> cas, Session session) {
		List<GroupByItem> groupByItems = new ArrayList<>();
		groupByItems.addAll(definition.getRowGroupByPart().getItems());
		groupByItems.addAll(definition.getColumnGroupByPart().getItems());
		
		HashMap<GroupByItem, String> areaToTables = new HashMap<>();
		for (GroupByItem groupBy : groupByItems) {
			if (groupBy.getAttributeKey() == null || groupBy.getAttributeType() != AttributeType.POSITION) continue;
		
			String tableName = SqlGenerator.createTempTableName();
			StringBuilder sb = new StringBuilder();
			sb.append(" CREATE TABLE "); //$NON-NLS-1$
			sb.append(tableName);
			sb.append(" (entity_uuid char(16) for bit data, keyid varchar(128)) "); //$NON-NLS-1$
			
			logme(sb);
			session.createNativeMutationQuery(sb.toString()).executeUpdate();

			sb = new StringBuilder();
			sb.append(" INSERT INTO "); //$NON-NLS-1$
			sb.append(tableName);
			sb.append(" (entity_uuid, keyid) "); //$NON-NLS-1$
			sb.append("SELECT a.entity_uuid, '" + groupBy.getAreaType().name() + "_' || b.keyid "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" FROM "); //$NON-NLS-1$
			sb.append(queryTable + " a "); //$NON-NLS-1$
			sb.append(" JOIN smart.i_entity_attribute_value v ON a.entity_uuid = v.entity_uuid "); //$NON-NLS-1$
			sb.append(" JOIN smart.i_attribute aa on aa.uuid = v.attribute_uuid and aa.keyid = :attributeKey "); //$NON-NLS-1$
			sb.append(", "); //$NON-NLS-1$
			sb.append(" smart.area_geometries b"); //$NON-NLS-1$
			sb.append(" WHERE "); //$NON-NLS-1$
			sb.append(" b.area_type = :areaType "); //$NON-NLS-1$
			sb.append(" AND "); //$NON-NLS-1$
			sb.append(" smart.pointinpolygon(v.double_value, v.double_value2, null, null, b.geom)"); //$NON-NLS-1$
			sb.append(" AND b.ca_uuid in (:cas)"); //$NON-NLS-1$
			
			logme(sb);
			session.createNativeMutationQuery(sb.toString())
				.setParameter("attributeKey",  groupBy.getAttributeKey()) //$NON-NLS-1$
				.setParameter("areaType",  groupBy.getAreaType().name()) //$NON-NLS-1$
				.setParameterList("cas", cas) //$NON-NLS-1$
				.executeUpdate();
			
			areaToTables.put(groupBy,  tableName);
			
		}
		
		return areaToTables;
	}
	
	/*
	 * Runs a sql statement on the data table to get the results for the table
	 */
	private SummaryQueryResult getResults(String queryTable, SumQueryDefinition definition, LocalDate[] dateRange, 
			Map<GroupByItem, String> areaTables, Locale l, Set<UUID> profiles, IQueryItemProvider itemProvider, Session session) throws Exception {
		
		StringBuilder selectSql = new StringBuilder();
		StringBuilder groupBySql = new StringBuilder();
		
		int cnt = 0;
		List<GroupByItem> groupByItems = new ArrayList<>();
		groupByItems.addAll(definition.getColumnGroupByPart().getItems());
		groupByItems.addAll(definition.getRowGroupByPart().getItems());
		
		List<String> entityTypeFilters = new ArrayList<>();
		for (GroupByItem groupBy : groupByItems) {
			if (cnt!= 0) {
				selectSql.append(","); //$NON-NLS-1$
				groupBySql.append(","); //$NON-NLS-1$
			}
			cnt++;
			
			if (groupBy.getGroupByType() == GroupByType.ENTITYTYPE) {
				selectSql.append("entity_type_key as c_" + cnt); //$NON-NLS-1$
				groupBySql.append("entity_type_key"); //$NON-NLS-1$
			}else if (groupBy.getGroupByType() == GroupByType.CA) {
				selectSql.append("ca_uuid as c_" + cnt); //$NON-NLS-1$
				groupBySql.append("ca_uuid "); //$NON-NLS-1$
			}else if(groupBy.getGroupByType() == GroupByType.ENTITY_ATTRIBUTE) {
				String columnName = groupBy.getAttributeKey();
				if (groupBy.getOtherKey() != null && !groupBy.getOtherKey().isEmpty()) {
					entityTypeFilters.add(groupBy.getOtherKey());
				}
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
					
				}else if (groupBy.getAttributeType() == AttributeType.POSITION) {
					String tableName = areaTables.get(groupBy);
					selectSql.append(tableName + ".keyId as c_" + cnt); //$NON-NLS-1$
					groupBySql.append(tableName + ".keyId ");					 //$NON-NLS-1$
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
		if (definition.getValuePart().getValueOption() == ValueOption.NUMBER_ENTITIES) {
			selectSql.append(" count(distinct " + queryTable+ ".entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT "); //$NON-NLS-1$
		sb.append(selectSql);
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append(queryTable);
		for (String tableName : areaTables.values()) {
			sb.append(" JOIN " + tableName + " ON " + queryTable + ".entity_uuid = " + tableName + ".entity_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		if (!entityTypeFilters.isEmpty()) {
			sb.append(" WHERE "); //$NON-NLS-1$
			sb.append("entity_type_key IN (:entityTypeFilters)"); //$NON-NLS-1$
		}
		
		if (groupBySql.length() > 0) {
			sb.append(" GROUP BY "); //$NON-NLS-1$
			sb.append(groupBySql);
		}
		
		logme(sb);
		
		SummaryQueryResult results = new SummaryQueryResult();
		EntitySummaryQueryHeaderEngine.INSTANCE.getHeaderInfo(definition, results, dateRange, profiles, itemProvider, l, session);
		
		HashMap<SummaryResultKey, Double> data = new HashMap<>();
		
		NativeQuery<Tuple> query = session.createNativeQuery(sb.toString(), Tuple.class);
		if (!entityTypeFilters.isEmpty()) {
			query.setParameterList("entityTypeFilters",  entityTypeFilters); //$NON-NLS-1$
		}
		List<Tuple> dataItems = query.list();
		for (Tuple rowdata : dataItems) {

			//now what?
			int column = 0;
			
			String[] groupBys = new String[groupByItems.size()];
			int index = 0;
			for (GroupByItem groupBy : groupByItems) {
				String colkey = EntitySummaryQueryHeaderEngine.INSTANCE.computeColumnKey(groupBy);
				Object value = rowdata.get(column++);
				
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
			Number value = (Number) rowdata.get(column++);
			data.put(temp, value.doubleValue());
		}
		results.setData(data);
		return results;
	}

	/*
	 * create temporary entity table and populate with all entities
	 * that match the given conservation area
	 */
	private String createTemporaryEntityTable(Session session, Set<UUID> profiles, Collection<ConservationArea> cas) {
		String obsTable = SqlGenerator.createTempTableName();
		
		dataTable = new DataTable(obsTable);
		
		String created = SystemAttributeFilter.SystemAttribute.ENTITY_DATE_CREATED.name().toLowerCase(Locale.ROOT); 
		String modified = SystemAttributeFilter.SystemAttribute.ENTITY_DATE_MODIFIED.name().toLowerCase(Locale.ROOT); 
		//create table
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(obsTable);
		sb.append(" (entity_uuid char(16) for bit data, entity_type_key varchar(128), ca_uuid char(16) for bit data, "); //$NON-NLS-1$
		sb.append(created );
		sb.append(" date, "); //$NON-NLS-1$
		sb.append(modified);
		sb.append(" date )"); //$NON-NLS-1$
		
		logme(sb);
		session.createNativeMutationQuery(sb.toString()).executeUpdate();
		dataTable.addColumn("entity_uuid",  "char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn("entity_type_key",  "varchar(128)"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn("ca_uuid",  "char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		dataTable.addColumn(created,  "date"); //$NON-NLS-1$ 
		dataTable.addColumn(modified,  "date"); //$NON-NLS-1$ 
		
		sb = new StringBuilder();
		sb.append("INSERT INTO "); //$NON-NLS-1$
		sb.append(obsTable);
		sb.append("(entity_uuid, entity_type_key, ca_uuid, "); //$NON-NLS-1$
		sb.append(created);
		sb.append(","); //$NON-NLS-1$
		sb.append(modified);
		sb.append(") SELECT a.uuid, b.keyid, a.ca_uuid, cast(a.date_created as date), cast(a.date_modified as date) FROM "); //$NON-NLS-1$
		sb.append(" smart.i_entity a join smart.i_entity_type b on a.entity_type_uuid = b.uuid "); //$NON-NLS-1$
		sb.append(" WHERE b.ca_uuid in (:cauuids) and a.profile_uuid in (:profiles)"); //$NON-NLS-1$
		
		List<UUID> cauuids = cas.stream().map(e->e.getUuid()).collect(Collectors.toList());
		
		logme(sb);
		session.createNativeMutationQuery(sb.toString())
			.setParameterList("cauuids",  cauuids) //$NON-NLS-1$
			.setParameterList("profiles",  profiles) //$NON-NLS-1$
			.executeUpdate();
		
		return obsTable;
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
				String columnName = addAttributeColumn(queryTable, attributeKey, item.getAttributeType(), session);
				attributeToColumnKey.put(attributeKey, columnName);
			}
		}
		if (def.getFilter() != null) {
			def.getFilter().accept(new IFilterVisitor() {
				
				@Override
				public void visitElement(IQueryFilter filter) {
					if (filter instanceof IntelAttributeFilter) {
						String attributeKey = ((IntelAttributeFilter) filter).getAttributeKey();
						IntelAttribute.AttributeType attributeType = ((IntelAttributeFilter) filter).getAttributeType();
						if (attributeType == AttributeType.POSITION) return; //  position attribute dealt with outside of here
						if (!attributeToColumnKey.containsKey(attributeKey)) {
							String columnName = addAttributeColumn(queryTable, attributeKey, attributeType, session);
							attributeToColumnKey.put(attributeKey, columnName);
						}
					}					
				}
			});
			progress.worked(1);
		}

		return attributeToColumnKey;
	}
	
	/*
	 * compute the minimum and maximum date values over all date attributes, so
	 * we can determine the range for the summary headers
	 */
	private LocalDate[] computeDateRange(SumQueryDefinition def, String queryTable,Map<String, String> attributeToColumnKey, Session session) {
		
		LocalDate minDate = null;
		LocalDate maxDate = null;
		Set<String> processed = new HashSet<>();
		
		List<GroupByItem> allItems = new ArrayList<>();
		allItems.addAll(def.getRowGroupByPart().getItems());
		allItems.addAll(def.getColumnGroupByPart().getItems());
		
		for (GroupByItem item : allItems) {
			String attributeKey = item.getAttributeKey();
			
			StringBuilder sb = new StringBuilder();
			if (item.getGroupByType() == GroupByType.SYSTEM) {
				if (item.getSystemAttribute() == SystemAttribute.ENTITY_DATE_CREATED || item.getSystemAttribute() == SystemAttribute.ENTITY_DATE_MODIFIED) {
			
					String columnName = item.getSystemAttribute().name().toLowerCase(Locale.ROOT); 
					
					sb.append("SELECT min("); //$NON-NLS-1$
					sb.append(columnName);
					sb.append("), max("); //$NON-NLS-1$
					sb.append(columnName );
					sb.append(") FROM "); //$NON-NLS-1$
					sb.append(queryTable);
				}else {
					continue;
				}
					
			}else if (attributeKey == null) {
				continue;
			}else if (processed.contains(attributeKey)) {
				continue;
			}else if (item.getAttributeType() != IntelAttribute.AttributeType.DATE) {
				continue;
			}else {
				String columnName = attributeToColumnKey.get(attributeKey);
					
				sb.append("SELECT min("); //$NON-NLS-1$
				sb.append(columnName);
				sb.append("), max("); //$NON-NLS-1$
				sb.append(columnName );
				sb.append(") FROM "); //$NON-NLS-1$
				sb.append(queryTable);
					
				if (item.getOtherKey() != null && !item.getOtherKey().isEmpty()) {
					sb.append(" WHERE "); //$NON-NLS-1$
					sb.append(" entity_type_key = '" + item.getOtherKey() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			
			Tuple dates = session.createNativeQuery(sb.toString(), Tuple.class).uniqueResult();
			if (dates != null  && dates.get(0) != null && dates.get(1) != null) {
				LocalDate lminDate = ((java.sql.Date)dates.get(0)).toLocalDate();
				LocalDate lmaxDate = ((java.sql.Date)dates.get(1)).toLocalDate();
				if (lminDate != null && (minDate == null || lminDate.isBefore(minDate))) {
					minDate = lminDate;
				}
				
				if (lmaxDate != null && (maxDate == null || lmaxDate.isAfter(maxDate))) {
					maxDate = lmaxDate;
				}
			}
		}
		return new LocalDate[] {minDate, maxDate};
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
		session.createNativeMutationQuery(sb.toString()).executeUpdate();
		
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
			selectClause = "v.employee_uuid"; //$NON-NLS-1$
			break;
		case LIST:
			selectClause = "l.keyid"; //$NON-NLS-1$
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
		sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid "); //$NON-NLS-1$
		if (type == AttributeType.LIST) {
			sb.append("JOIN smart.i_attribute_list_item l on l.uuid = v.list_item_uuid"); //$NON-NLS-1$
		}
		sb.append(" WHERE v.entity_uuid = " + queryTable + ".entity_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" and a.keyid = :keyid "); //$NON-NLS-1$
		sb.append(" ) "); //$NON-NLS-1$
		
		logme(sb);
		
		session.createNativeMutationQuery(sb.toString())
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
		for (Entry<String,String> entry : dataTable.columnNames.entrySet()) {
			sb.append(entry.getKey() + " " + entry.getValue()); //$NON-NLS-1$
			sb.append(","); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")"); //$NON-NLS-1$
		
		logme(sb);
		session.createNativeMutationQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO " ); //$NON-NLS-1$
		sb.append(table2);
		sb.append("("); //$NON-NLS-1$
		for (Entry<String,String> entry : dataTable.columnNames.entrySet()) {
			sb.append(entry.getKey());
			sb.append(","); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")"); //$NON-NLS-1$
		sb.append(" SELECT "); //$NON-NLS-1$
		for (Entry<String,String> entry : dataTable.columnNames.entrySet()) {
			sb.append("a." + entry.getKey()); //$NON-NLS-1$
			sb.append(","); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" FROM " ); //$NON-NLS-1$
		sb.append(dataTable.tableName + " a"); //$NON-NLS-1$
		
		final boolean[] requiresEntityType = new boolean[] {false};
		final boolean[] requiresEntity = new boolean[] {false};
		queryFilter.accept(new IFilterVisitor() {
			
			@Override
			public void visitElement(IQueryFilter filter) {
				if (requiresEntityType[0]) return;
				if (filter instanceof EntityTypeFilter) {
					requiresEntityType[0] = true;
					return;
				}
				if (filter instanceof IntelAttributeFilter) {
					IntelAttributeFilter f = (IntelAttributeFilter) filter;
					if (f.getEntityTypeKey() != null && !f.getEntityTypeKey().isEmpty()) {
						requiresEntityType[0] = true;
						return;
					}
				}
				if (filter instanceof SystemAttributeFilter ) {
					requiresEntity[0] = true;
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
		HashMap<String,Object> parameters = new HashMap<>();
		processFilter(queryFilter, sb, parameters);
		
		MutationQuery query = session.createNativeMutationQuery(sb.toString());
		for (Entry<String,Object> parameter : parameters.entrySet()) {
			query.setParameter(parameter.getKey(),  parameter.getValue());
			logme(parameter.getKey() + ":" + parameter.getValue()); //$NON-NLS-1$
		}
		logme(sb);
		query.executeUpdate();
		
		try {
			session.createNativeMutationQuery("DROP TABLE " + dataTable.tableName).executeUpdate(); //$NON-NLS-1$
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		dataTable.tableName = table2;
	}
	
	/**
	 * Creates where statement for query filter 
	 * @param queryFilter
	 * @param whereSql
	 * @param parameters
	 * @throws Exception
	 */
	private void processFilter(IQueryFilter queryFilter, StringBuilder whereSql, HashMap<String,Object> parameters) throws Exception {
		
		if (queryFilter instanceof SystemAttributeFilter) {
			SystemAttributeFilter f = (SystemAttributeFilter)queryFilter;
			if (f.getAttribute() == SystemAttribute.ENTITY_DATE_CREATED || f.getAttribute() == SystemAttribute.ENTITY_DATE_MODIFIED) {
				String columnName = null;
				if (f.getAttribute() == SystemAttributeFilter.SystemAttribute.ENTITY_DATE_CREATED) {
					columnName = "e.date_created"; //$NON-NLS-1$
				}else if (f.getAttribute() == SystemAttributeFilter.SystemAttribute.ENTITY_DATE_MODIFIED) {
					columnName = "e.date_modified"; //$NON-NLS-1$
				}
				whereSql.append(SqlGenerator.generateDateClause(f.getDateValues(), columnName));

			}else {
				throw new IllegalStateException("Group by record dates is not supported for entity summary queries"); //$NON-NLS-1$
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
			
		}else if (queryFilter instanceof EntityFilter) {
			EntityFilter f = (EntityFilter)queryFilter;
			String key = "p_"+parameters.size(); //$NON-NLS-1$
			parameters.put(key, f.getEntityUuid());
			whereSql.append(" entity_uuid = :" + key + " "); //$NON-NLS-1$ //$NON-NLS-2$
			
		}else if (queryFilter instanceof EntityTypeFilter) {
			EntityTypeFilter f = (EntityTypeFilter)queryFilter;
			String key = "p_" + parameters.size(); //$NON-NLS-1$
			parameters.put(key, f.getTypeKey());
			whereSql.append(" t.keyId = :" + key + " "); //$NON-NLS-1$ //$NON-NLS-2$
			
		}else if (queryFilter instanceof IntelAttributeFilter) {
			IntelAttributeFilter f = (IntelAttributeFilter)queryFilter;
			
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
				if (f.getKeyValue().equals(IntelAttributeFilter.ANY_OPTION_KEY)) {
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
				if (f.getKeyValue().equals(IntelAttributeFilter.ANY_OPTION_KEY)) {
					whereSql.append(" is not null "); //$NON-NLS-1$
				}else {
					whereSql.append( " = " ); //$NON-NLS-1$
					String key = "p_" + parameters.size(); //$NON-NLS-1$
					parameters.put(key, f.getKeyValue());
					whereSql.append(" :" + key + " "); //$NON-NLS-1$ //$NON-NLS-2$
				}
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
