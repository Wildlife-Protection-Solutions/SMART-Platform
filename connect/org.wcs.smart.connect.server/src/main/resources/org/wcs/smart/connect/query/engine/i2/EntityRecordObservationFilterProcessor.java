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

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.AreaFilter;
import org.wcs.smart.i2.query.observation.filter.BooleanFilter;
import org.wcs.smart.i2.query.observation.filter.BracketFilter;
import org.wcs.smart.i2.query.observation.filter.DataModelFilter;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.NotFilter;
import org.wcs.smart.i2.query.observation.filter.RecordAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;
import org.wcs.smart.util.UuidUtils;

/**
 * filter processor for processing observation filters
 * 
 * @author Emily
 *
 */
public class EntityRecordObservationFilterProcessor {

	private IQueryFilter filter;
	private Session s;
	
	private Exception visitorException;
	private List<Object[]> filterToColumnName = new ArrayList<>();
	private IQueryItemProvider itemProvider;
	
	private String dataModelTable = null;
	private Set<IntelProfile> profileFilter;
	
	public EntityRecordObservationFilterProcessor(IQueryFilter filter, Set<IntelProfile> profileFilter, IQueryItemProvider itemProvider, Session s){
		this.filter = filter;
		this.s = s;
		this.itemProvider = itemProvider;
		this.profileFilter = profileFilter;
	}
	
	/**
	 * Object[] is a two value array: <IQueryFilter, String>
	 * @return
	 */
	public List<Object[]> getFilterToColumnNames(){
		return filterToColumnName;
	}
	
	private void dispose() {
		if (dataModelTable != null) s.createNativeQuery("DROP TABLE " + dataModelTable).executeUpdate(); //$NON-NLS-1$
	}
	
	/**
	 * Returns a table with a list of observations that match filters
	 * 
	 * @return
	 * @throws Exception
	 */
	public String processFilter() throws Exception{
		try {
			
		
			//1. - Observation Query Filter
			//create a table of all entities using date filter
			String entityTable = SqlGenerator.createTempTableName();
					
			StringBuilder tableColumns = new StringBuilder();
			tableColumns.append("entity_uuid uuid, date_modified timestamp, entity_type_key varchar(128)"); //$NON-NLS-1$
			tableColumns.append(",ca_uuid uuid, ca_id varchar(8), ca_name varchar(256), profile_uuid uuid"); //$NON-NLS-1$

			List<String> tableColumnNames = new ArrayList<>();
			tableColumnNames.add("entity_uuid"); //$NON-NLS-1$
			tableColumnNames.add("date_modified"); //$NON-NLS-1$
			tableColumnNames.add("entity_type_key"); //$NON-NLS-1$
			tableColumnNames.add("ca_uuid"); //$NON-NLS-1$
			tableColumnNames.add("ca_id"); //$NON-NLS-1$
			tableColumnNames.add("ca_name"); //$NON-NLS-1$
			tableColumnNames.add("profile_uuid"); //$NON-NLS-1$

			StringBuilder dataModelColumns = new StringBuilder();
			dataModelColumns.append("entity_uuid uuid, obs_uuid uuid "); //$NON-NLS-1$
			StringBuilder dataModelColumnsExtra = new StringBuilder();
			
			StringBuilder sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(entityTable);
			sql.append("("); //$NON-NLS-1$
			sql.append(tableColumns);
			sql.append(")"); //$NON-NLS-1$
			logString(sql.toString());
			s.createNativeQuery(sql.toString()).executeUpdate();
					
			sql = new StringBuilder();
			sql.append("INSERT INTO " + entityTable); //$NON-NLS-1$
			sql.append(" SELECT l.uuid, l.date_modified, o.keyid, ca.uuid, ca.id, ca.name, l.profile_uuid  FROM smart.i_entity l "); //$NON-NLS-1$
			sql.append(" JOIN smart.i_entity_type o on l.entity_type_uuid = o.uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.conservation_area ca on l.ca_uuid = ca.uuid " ); //$NON-NLS-1$
			sql.append( " WHERE "); //$NON-NLS-1$
			sql.append(" l.ca_uuid in (:cas) and l.profile_uuid in (:profiles)"); //$NON-NLS-1$
			Collection<UUID> profileUuids = profileFilter.stream().map(p->p.getUuid()).collect(Collectors.toSet());
			for (UUID uuid : profileUuids) {
				logString(UuidUtils.uuidToString(uuid));
			}
			List<UUID> caUuids = itemProvider.getConservationAreas().stream().map(e->e.getUuid()).collect(Collectors.toList());
			for (UUID uuid : caUuids) {
				logString(UuidUtils.uuidToString(uuid));
			}
			logString(sql.toString());
			
			NativeQuery<?> query = s.createNativeQuery(sql.toString());
			query.setParameterList("cas", caUuids); //$NON-NLS-1$
			query.setParameterList("profiles", profileUuids); //$NON-NLS-1$

			query.executeUpdate();
			
			//create indexes to help with performance
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + SqlGenerator.createIndexName(entityTable) + "_entity_uuid_idx on " + entityTable + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logString(sql.toString());
			s.createNativeQuery(sql.toString()).executeUpdate();
				
			//for each filter add a column for that filter
			//set the filter value to true or false depending on the filter
			if(filter != null){	
				filter.accept(new IFilterVisitor() {
					private int columnCnt = 1;
					
					String tempTable = SqlGenerator.createTempTableName();
					
					private String createColumn(IQueryFilter filter){
						
						String columnName = "filter_" + columnCnt++; //$NON-NLS-1$
						if (filter instanceof DataModelFilter) {
							dataModelColumnsExtra.append(", " + columnName + " boolean "); //$NON-NLS-1$ //$NON-NLS-2$
						}else {
							tableColumns.append(", " + columnName + " boolean "); //$NON-NLS-1$ //$NON-NLS-2$
							tableColumnNames.add(columnName);
						}
						StringBuilder sql = new StringBuilder();
						sql.append("CREATE TABLE "); //$NON-NLS-1$
						sql.append(tempTable);
						sql.append(" ("); //$NON-NLS-1$
						if (filter instanceof DataModelFilter) {
							sql.append(dataModelColumns);
							sql.append(dataModelColumnsExtra);
						}else {
							sql.append(tableColumns);
						}
						sql.append (")"); //$NON-NLS-1$
						
						logString(sql.toString());
						s.createNativeQuery(sql.toString()).executeUpdate();
						
						filterToColumnName.add(new Object[] {filter, columnName});
						
						return columnName;
					}
					
					@Override
					public void visitElement(IQueryFilter filter) {
							if (visitorException != null) return;
						try{
							if (filter instanceof AreaFilter){
								String columnName = createColumn(filter);
								addFilterColumn((AreaFilter) filter, entityTable, tempTable, columnName);
								switchTables(tempTable, entityTable, true, false, s);
							}else if (filter instanceof DataModelFilter){
								String columnName = createColumn(filter);
								addFilterColumn((DataModelFilter) filter, entityTable, tempTable, columnName);
								switchTables(tempTable, dataModelTable, true, true, s);
							}else if (filter instanceof EntityFilter){
								String columnName = createColumn(filter);
								addFilterColumn((EntityFilter) filter, entityTable, tempTable,  columnName);
								switchTables(tempTable, entityTable, true, false, s);
							}else if (filter instanceof EntityTypeFilter){
								String columnName = createColumn(filter);
								addFilterColumn((EntityTypeFilter) filter, entityTable, tempTable, columnName);
								switchTables(tempTable, entityTable, true, false, s);
							}else if (filter instanceof IntelAttributeFilter){
								String columnName = createColumn(filter);
								addFilterColumn((IntelAttributeFilter) filter, entityTable, tempTable, columnName);
								switchTables(tempTable, entityTable, true, false, s);
							}else if (filter instanceof RecordAttributeFilter) {
								String columnName = createColumn(filter);
								addFilterColumn((RecordAttributeFilter) filter, entityTable, tempTable, columnName);
								switchTables(tempTable, entityTable, true, false, s);
							}else if (filter instanceof RecordAttributeFilter) {
								String columnName = createColumn(filter);
								addFilterColumn((RecordAttributeFilter) filter, entityTable, tempTable, columnName);
								switchTables(tempTable, entityTable, true, false, s);
							}else if (filter instanceof SystemAttributeFilter) {
								String columnName = createColumn(filter);
								addFilterColumn((SystemAttributeFilter)filter, entityTable, tempTable, columnName);
								switchTables(tempTable, entityTable, true, false, s);
							}
						}catch(Exception e){
							visitorException = e;
						}
					}
					}
				);
				if (visitorException != null) throw visitorException;
			}					
			
			//run the query; getting a list of observations
			
			//create a results table based on that list of observations; adding the fields necessary
			if (filter != null){
				String tempTable = SqlGenerator.createTempTableName();
				
				sql = new StringBuilder();
				sql.append("CREATE TABLE " + tempTable ); //$NON-NLS-1$
				sql.append("("); //$NON-NLS-1$
				sql.append(tableColumns);
				if (dataModelTable != null) {
					sql.append(dataModelColumnsExtra);
				}
				sql.append(")"); //$NON-NLS-1$
				logString(sql.toString());
				s.createNativeQuery(sql.toString()).executeUpdate();	
				
				final StringBuilder deleteSql = new StringBuilder();
				deleteSql.append("INSERT INTO " + tempTable ); //$NON-NLS-1$
				deleteSql.append(" SELECT "); //$NON-NLS-1$
				for (String col : tableColumnNames) {
					deleteSql.append("a." + col); //$NON-NLS-1$
					deleteSql.append(","); //$NON-NLS-1$
				}
				deleteSql.deleteCharAt(deleteSql.length() - 1);
				if (dataModelTable != null) {
					String[] cols = dataModelColumnsExtra.substring(1).replaceAll("boolean", "").replaceAll(" ", "").split(","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					for (String c : cols) {
						deleteSql.append(","); //$NON-NLS-1$
						deleteSql.append(" case when max( case when b." + c + " then 1 else 0 end ) = 1 then true else false end "); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				deleteSql.append(" FROM "); //$NON-NLS-1$
				deleteSql.append( entityTable + " a" ); //$NON-NLS-1$
				if (dataModelTable != null) {
					deleteSql.append(" JOIN "); //$NON-NLS-1$
					deleteSql.append(dataModelTable);
					deleteSql.append(" b ON "); //$NON-NLS-1$
					deleteSql.append(" a.entity_uuid = b.entity_uuid"); //$NON-NLS-1$
				}
				deleteSql.append(" WHERE "); //$NON-NLS-1$
				filter.accept(new IFilterVisitor() {
					private Set<BracketFilter> filters = new HashSet<>();
					@Override
					public void visitElement(IQueryFilter filter) {
						if (visitorException != null) return;
						try{
							String columnName = null;
							for (Object[] i : filterToColumnName) {
								if (i[0].equals(filter)) {
									columnName = (String) i[1];
									break;
								}
							}
							if (columnName != null){
								deleteSql.append(" ( " + columnName + " is not null ) "); //$NON-NLS-1$ //$NON-NLS-2$
								deleteSql.append(" "); //$NON-NLS-1$
							}else if (filter.getClass().equals(BooleanFilter.class)){
								deleteSql.append(  SqlGenerator.operatorToSql(((BooleanFilter)filter).getOperator()));
								deleteSql.append(" "); //$NON-NLS-1$
							}else if (filter.getClass().equals(NotFilter.class)){
								deleteSql.append( SqlGenerator.operatorToSql(Operator.NOT));
								deleteSql.append(" "); //$NON-NLS-1$
							}else if (filter.getClass().equals(BracketFilter.class)){
								if (filters.contains(filter)){
									deleteSql.append(SqlGenerator.operatorToSql(Operator.BRACKET_CLOSE));
								}else{
									deleteSql.append(SqlGenerator.operatorToSql(Operator.BRACKET_OPEN));
									filters.add((BracketFilter) filter);
								}
								deleteSql.append(" "); //$NON-NLS-1$
							}
						}catch (Exception ex){
							visitorException = ex;
						}
					}
					
				});		
				if (visitorException != null) throw visitorException;
				
				deleteSql.append(" GROUP BY "); //$NON-NLS-1$
				for (String col : tableColumnNames) {
					deleteSql.append("a." + col); //$NON-NLS-1$
					deleteSql.append(","); //$NON-NLS-1$
				}
				deleteSql.deleteCharAt(deleteSql.length() - 1);
				
				logString(deleteSql.toString());
				s.createNativeQuery(deleteSql.toString()).executeUpdate();
				
				switchTables(tempTable, entityTable, true, false, s);
			}
			
			return entityTable;
		}finally {
			dispose();
		}
	}
	
	private void addFilterColumn(DataModelFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		
		if (dataModelTable == null) {
			dataModelTable = SqlGenerator.createTempTableName();
			//create a table of all entity_uuid, waypoint_uuid
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE " ); //$NON-NLS-1$
			sb.append(dataModelTable);
			sb.append(" (entity_uuid uuid, obs_uuid uuid )"); //$NON-NLS-1$
			logString(sb.toString());
			s.createNativeQuery(sb.toString()).executeUpdate();
			
			sb = new StringBuilder();
			sb.append(" INSERT INTO "); //$NON-NLS-1$
			sb.append(dataModelTable);
			sb.append(" SELECT e.entity_uuid, i.uuid FROM smart.i_entity_location e join smart.i_observation i on i.location_uuid = e.location_uuid "); //$NON-NLS-1$
			sb.append(" WHERE e.entity_uuid in (SELECT entity_uuid FROM " + obsTable + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			
			logString(sb.toString());
			s.createNativeQuery(sb.toString()).executeUpdate();
		}
		
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(entity_uuid uuid, obs_uuid uuid) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		if (filter.getAttributeKey() == null){
			//only a category filter
			sql = new StringBuilder();
			sql.append("INSERT INTO " + t2 + " " ); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" SELECT distinct a.entity_uuid, o.uuid as obs_uuid "); //$NON-NLS-1$
			sql.append(" FROM " + dataModelTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_entity_location l on l.entity_uuid = a.entity_uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.i_observation o on l.location_uuid = o.location_uuid and a.obs_uuid = o.uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.dm_category c on c.uuid = o.category_uuid "); //$NON-NLS-1$
			sql.append(" WHERE (c.hkey like :hkey1 ) "); //$NON-NLS-1$
			String hkey1 = filter.getCategoryKey() + "%"; //$NON-NLS-1$
			
			logString(hkey1);
			
			NativeQuery<?> query = s.createNativeQuery(sql.toString());
			query.setParameter("hkey1", hkey1); //$NON-NLS-1$
			
			logString(sql.toString());
			query.executeUpdate();
			
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + SqlGenerator.createIndexName("entity_uuid_tmp") + " on " + t2 + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			logString(sql.toString());
			s.createNativeQuery(sql.toString()).executeUpdate();
			
			sql = new StringBuilder();
			sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
			sql.append(" SELECT a.*, CASE WHEN b.entity_uuid is null then null else true end "); //$NON-NLS-1$
			sql.append(" FROM " + dataModelTable + " a LEFT JOIN " + t2 + " b on a.entity_uuid = b.entity_uuid and a.obs_uuid = b.obs_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logString(sql.toString());
			query = s.createNativeQuery(sql.toString());
			query.executeUpdate();
			
			sql = new StringBuilder();
			sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
			logString(sql.toString());
			s.createNativeQuery(sql.toString()).executeUpdate();
			
			return;
			
		}
		//category and perhaps an attribute filter		
		sql = new StringBuilder();
		sql.append("INSERT INTO " + t2 ); //$NON-NLS-1$
		sql.append(" SELECT distinct a.entity_uuid, o.uuid as obs_uuid "); //$NON-NLS-1$
		sql.append( "FROM " + dataModelTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" JOIN smart.i_entity_location l on l.entity_uuid = a.entity_uuid "); //$NON-NLS-1$
		sql.append(" JOIN smart.i_observation o on o.location_uuid = l.location_uuid and o.uuid = a.obs_uuid "); //$NON-NLS-1$
		if (filter.getCategoryKey() != null){
			sql.append(" JOIN smart.dm_category c on c.uuid = o.category_uuid "); //$NON-NLS-1$
		}
		sql.append(" JOIN smart.i_observation_attribute ia on ia.observation_uuid = o.uuid "); //$NON-NLS-1$
		sql.append(" JOIN smart.dm_attribute dma on dma.uuid = ia.attribute_uuid "); //$NON-NLS-1$

		if (filter.getAttributeType() == Attribute.AttributeType.TREE ){
			sql.append(" JOIN smart.dm_attribute_tree ta ON ia.tree_node_uuid = ta.uuid "); //$NON-NLS-1$
		}
		if (filter.getAttributeType() == Attribute.AttributeType.LIST ){
			sql.append(" JOIN smart.dm_attribute_list tl ON ia.list_element_uuid = tl.uuid "); //$NON-NLS-1$
		}
		sql.append(" WHERE dma.keyId = :attributeKey "); //$NON-NLS-1$
		if (filter.getCategoryKey() != null){
			sql.append(" AND (c.hkey like :hkey1) "); //$NON-NLS-1$
		}
		sql.append(" AND "); //$NON-NLS-1$
		
		switch(filter.getAttributeType()){
		case BOOLEAN:
			sql.append(" ia.double_value " + SqlGenerator.operatorToSql(Operator.GREATERTHAN) + " 0.5"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case DATE:
			sql.append(" cast(ia.string_value as date) " + SqlGenerator.operatorToSql(filter.getOperator()) + " cast(:value1 as date) and cast(:value2 as date)"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case LIST:
			if (filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
				sql.append(" ia.list_element_uuid is not null "); //$NON-NLS-1$
			}else{
				sql.append(" tl.keyid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			break;
		case NUMERIC:
			sql.append(" ia.double_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case TEXT:
			sql.append(" ia.string_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case TREE:
			sql.append( " ( ta.hkey like :tree1 ) "); //$NON-NLS-1$
			break;
		default:
			break;
		}
		NativeQuery<?> query = s.createNativeQuery(sql.toString());
		query.setParameter("attributeKey", filter.getAttributeKey()); //$NON-NLS-1$
		logString(filter.getAttributeKey());
		
		if (filter.getCategoryKey() != null){
			String hkey1 = filter.getCategoryKey() + "%"; //$NON-NLS-1$
			logString(hkey1);
			query.setParameter("hkey1", hkey1); //$NON-NLS-1$
		}
		switch(filter.getAttributeType()){
		case BOOLEAN:
			break;
		case DATE:
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
			query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  ); //$NON-NLS-1$
			query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  ); //$NON-NLS-1$
			break;
		case LIST:
			if (!filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
				logString(filter.getKeyValue());
				query.setParameter("value",  filter.getKeyValue()); //$NON-NLS-1$
			}
			break;
		case TREE:
			String tree1 = filter.getKeyValue() + "%"; //$NON-NLS-1$
			logString(tree1);
			query.setParameter("tree1", tree1); //$NON-NLS-1$
			break;
		case NUMERIC:
			logString(filter.getNumberValue().toString());
			query.setParameter("value", filter.getNumberValue()); //$NON-NLS-1$
			break;
		case TEXT:
			logString(filter.getStringValue());
			query.setParameter("value", filter.getStringValue()); //$NON-NLS-1$
			break;
		default:
			break;
		}
		
		logString(sql.toString());
		query.executeUpdate();
		
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("entity_uuid_tmp") + " on " + t2 + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.entity_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + dataModelTable + " a LEFT JOIN " + t2 + " b on a.entity_uuid = b.entity_uuid and a.obs_uuid = b.obs_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		query = s.createNativeQuery(sql.toString());
		query.executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
	}
	
	private void addFilterColumn(RecordAttributeFilter filter, String entityType, String tempTable, String columnName) throws Exception{
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(entity_uuid uuid) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		if (filter.getAttributeType() == null) {
			//entity
			UUID entityUuid = null;
			if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)){
				entityUuid = UuidUtils.stringToUuid(filter.getKeyValue());
			}
			
			sql = new StringBuilder();
			sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
			sql.append (" SELECT distinct a.entity_uuid "); //$NON-NLS-1$
			sql.append(" FROM " + entityType + " a JOIN smart.i_entity_record er on a.entity_uuid = er.entity_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_record r on er.record_uuid = r.uuid "); //$NON-NLS-1$
			if (filter.getRecordSourceKey() != null) {
				sql.append("JOIN smart.i_recordsource rs on r.source_uuid = rs.uuid"); //$NON-NLS-1$
			}
			sql.append(" JOIN smart.i_record_attribute_value v on v.record_uuid = r.uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.i_record_attribute_value_list vl on vl.value_uuid = v.uuid "); //$NON-NLS-1$
			
			sql.append(" WHERE "); //$NON-NLS-1$
			if (filter.getRecordSourceKey() != null) {
				sql.append( " rs.keyid = :recordKey "); //$NON-NLS-1$
			}
			if (entityUuid != null) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(" vl.element_uuid = :entityuuid "); //$NON-NLS-1$
			}
			
			logString(sql.toString());
			logString(filter.getRecordSourceKey());
			logString(filter.getKeyValue());
			
			NativeQuery<?> query = s.createNativeQuery(sql.toString());
			if (filter.getRecordSourceKey() != null) {
				query.setParameter("recordKey", filter.getRecordSourceKey()); //$NON-NLS-1$
			}
			if (entityUuid != null) {
				query.setParameter("entityuuid",  entityUuid); //$NON-NLS-1$
			}
			query.executeUpdate();	
			
		}else if (filter.getAttributeType() != null) {
			IntelAttribute attribute = itemProvider.getAttribute(filter.getAttributeKey(), s);
			if (attribute == null) throw new Exception(MessageFormat.format("Intel attribute with key {0} not found.", filter.getAttributeKey())); //$NON-NLS-1$
		
			IntelAttributeListItem listItem = null;
			if (filter.getAttributeType() == AttributeType.LIST && filter.getKeyValue() != null){
				if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)){
					List<IntelAttributeListItem> items = itemProvider.getAttributeListItems(filter.getAttributeKey(), s);
					for (IntelAttributeListItem item : items) {
						if (item.getKeyId().equals(filter.getKeyValue())) {
							listItem = item;
							break;
						}
					}
					if (listItem == null) throw new Exception(MessageFormat.format("Intel attribute list item with key {0} not found.", filter.getAttributeKey())); //$NON-NLS-1$
				}
			}
			
			Employee employee = null;
			if (filter.getAttributeType() == AttributeType.EMPLOYEE && filter.getKeyValue() != null) {
				if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)) {
					//find the employee
					Employee e = s.get(Employee.class, UuidUtils.stringToUuid(filter.getKeyValue()));
					if (e != null && !itemProvider.getConservationAreas().contains(e.getConservationArea())) {
						e = null;
					}
					if (e == null) {
						throw new Exception(MessageFormat.format("Employee with identifier {0} not found.", filter.getKeyValue())); //$NON-NLS-1$
					}
					employee = e;
				}
			}
		
			sql = new StringBuilder();
			sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
			sql.append (" SELECT distinct a.entity_uuid "); //$NON-NLS-1$
			sql.append(" FROM " + entityType + " a JOIN smart.i_entity_record er on a.entity_uuid = er.entity_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_record r on er.record_uuid = r.uuid "); //$NON-NLS-1$
			if (filter.getRecordSourceKey() != null) {
				sql.append("JOIN smart.i_recordsource rs on r.source_uuid = rs.uuid"); //$NON-NLS-1$
			}
			sql.append(" JOIN smart.i_record_attribute_value v on v.record_uuid = r.uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.i_recordsource_attribute ra on ra.uuid = v.attribute_uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.i_attribute a on a.uuid = ra.attribute_uuid "); //$NON-NLS-1$
			
			if (filter.getAttributeType() == AttributeType.LIST || filter.getAttributeType() == AttributeType.EMPLOYEE) {
				sql.append(" JOIN smart.i_record_attribute_value_list vl on vl.value_uuid = v.uuid "); //$NON-NLS-1$
				if (listItem != null ) {
					sql.append(" JOIN smart.i_attribute_list_item ali on ali.uuid = vl.element_uuid "); //$NON-NLS-1$
				}
			}
			
			sql.append(" WHERE "); //$NON-NLS-1$
			if (filter.getRecordSourceKey() != null) {
				sql.append( " rs.keyid = :recordKey "); //$NON-NLS-1$
				sql.append(" AND "); //$NON-NLS-1$
			}
			sql.append(" a.keyid = :attributekey "); //$NON-NLS-1$
			sql.append(" AND "); //$NON-NLS-1$
			
			if (filter.getAttributeType() == AttributeType.BOOLEAN) {
				sql.append(" v.double_value " + SqlGenerator.operatorToSql(Operator.GREATERTHAN) + " 0.5"); //$NON-NLS-1$ //$NON-NLS-2$

			}else if (filter.getAttributeType() == AttributeType.DATE) {
				sql.append(" case when a.type = 'DATE' then cast(v.string_value as date) " + SqlGenerator.operatorToSql(filter.getOperator()) + " cast(:value1 as date) and cast(:value2 as date) else false end"); //$NON-NLS-1$ //$NON-NLS-2$

			}else if (filter.getAttributeType() == AttributeType.LIST) {
				if (listItem == null){
					//any option
					sql.append(" vl.element_uuid is not null "); //$NON-NLS-1$
				}else{
					sql.append(" ali.keyid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}else if (filter.getAttributeType() == AttributeType.EMPLOYEE) {
				if (employee == null) {
					//any option
					sql.append(" vl.element_uuid is not null "); //$NON-NLS-1$
				}else {
					sql.append(" vl.element_uuid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}else if (filter.getAttributeType() == AttributeType.NUMERIC) {
				sql.append(" v.double_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$

			}else if (filter.getAttributeType() == AttributeType.TEXT) {
				sql.append(" v.string_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$

			}else {
				throw new Exception(MessageFormat.format("Filter for attribute type {0} not supported.", filter.getAttributeType().name())); //$NON-NLS-1$
			}
			logString(sql.toString());
			logString(filter.getRecordSourceKey());
			logString(filter.getAttributeKey());
			
			NativeQuery<?> query = s.createNativeQuery(sql.toString());
			if (filter.getRecordSourceKey() != null) {
				query.setParameter("recordKey", filter.getRecordSourceKey()); //$NON-NLS-1$
			}
			query.setParameter("attributekey",  filter.getAttributeKey()); //$NON-NLS-1$
			
			if (filter.getAttributeType() == AttributeType.DATE) {
				logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
				logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
				query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  ); //$NON-NLS-1$
				query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  ); //$NON-NLS-1$
			}else if (filter.getAttributeType() == AttributeType.LIST) {				
				if (listItem != null){
					logString(listItem.getKeyId());
					query.setParameter("value", listItem.getKeyId()); //$NON-NLS-1$
				}
			}else if (filter.getAttributeType() == AttributeType.EMPLOYEE) {
				if (employee != null){
					logString(UuidUtils.uuidToString(employee.getUuid()));
					query.setParameter("value", employee.getUuid()); //$NON-NLS-1$
				}
			}else if (filter.getAttributeType() == AttributeType.NUMERIC) {
				logString(filter.getNumberValue().toString());
				query.setParameter("value", filter.getNumberValue()); //$NON-NLS-1$
			}else if (filter.getAttributeType() == AttributeType.TEXT) {
				String value = filter.getStringValue();
				if (filter.getOperator() == Operator.STR_CONTAINS || filter.getOperator() == Operator.STR_NOTCONTAINS) {
					value = "%" + value + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				logString(value);
				query.setParameter("value", value); //$NON-NLS-1$
			}
			query.executeUpdate();
		}
			
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("entity_uuid") + " on " + t2 + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
			
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.entity_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + entityType + " a LEFT JOIN " + t2 + " b on a.entity_uuid = b.entity_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		NativeQuery<?> query = s.createNativeQuery(sql.toString());
		query.executeUpdate();
			
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
	}
	
	
	private void addFilterColumn(EntityFilter filter, String obsTable, String tempTable, String columnName){
		StringBuilder sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN a.entity_uuid = :uuid then true else null end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$ 
		logString(sql.toString());
		
		NativeQuery<?> query = s.createNativeQuery(sql.toString());
		query.setParameter("uuid",  filter.getEntityUuid()); //$NON-NLS-1$
		query.executeUpdate();
	}
	
	private void addFilterColumn(EntityTypeFilter filter, String obsTable, String tempTable, String columnName){
		
		//todo: configure uuid
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(entity_uuid uuid) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
				
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
		sql.append (" SELECT distinct a.entity_uuid "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a JOIN smart.i_entity e on a.entity_uuid = e.uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" JOIN smart.i_entity_type t on e.entity_type_uuid = t.uuid "); //$NON-NLS-1$
		sql.append( " WHERE t.keyId = :typeKey "); //$NON-NLS-1$
				
		logString(sql.toString());
		logString(filter.getTypeKey());
		NativeQuery<?> query = s.createNativeQuery(sql.toString());
		query.setParameter("typeKey",  filter.getTypeKey()); //$NON-NLS-1$
		query.executeUpdate();
				
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("entity_uuid") + " on " + t2 + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
				
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.entity_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.entity_uuid = b.entity_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		query = s.createNativeQuery(sql.toString());
		query.executeUpdate();
				
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
	}
	
	private void addFilterColumn(IntelAttributeFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		
		IntelAttribute attribute = itemProvider.getAttribute(filter.getAttributeKey(), s);
		if (attribute == null) throw new Exception(MessageFormat.format("Intel attribute with key {0} not found. ", filter.getAttributeKey())) ; //$NON-NLS-1$
		
		IntelAttributeListItem listItem = null;
		if (filter.getAttributeType() == AttributeType.LIST && filter.getKeyValue() != null){
			if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)){
				List<IntelAttributeListItem> items = itemProvider.getAttributeListItems(filter.getAttributeKey(), s);
				for (IntelAttributeListItem item : items) {
					if (item.getKeyId().equals(filter.getKeyValue())) {
						listItem = item;
						break;
					}
				}
				if (listItem == null) throw new Exception(MessageFormat.format("Intel Attribute list item with key {0} not found.", filter.getAttributeKey())); //$NON-NLS-1$
			}
		}
		
		Employee employee = null;
		if (filter.getAttributeType() == AttributeType.EMPLOYEE && filter.getKeyValue() != null) {
			if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)) {
				//find the employee
				Employee e = s.get(Employee.class, UuidUtils.stringToUuid(filter.getKeyValue()));
				if (e != null && !itemProvider.getConservationAreas().contains(e.getConservationArea())) {
					e = null;
				}
				if (e == null) {
					throw new Exception(MessageFormat.format("No employee with key {0} found.", filter.getKeyValue())); //$NON-NLS-1$
				}
				employee = e;
			}
		}

		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(entity_uuid uuid) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
		sql.append (" SELECT distinct a.entity_uuid "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a JOIN smart.i_entity e on a.entity_uuid = e.uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" JOIN smart.i_entity_attribute_value v on v.entity_uuid = e.uuid "); //$NON-NLS-1$
		sql.append(" JOIN smart.i_attribute ia on ia.uuid = v.attribute_uuid "); //$NON-NLS-1$
		if (listItem != null) {
			sql.append(" LEFT JOIN smart.i_attribute_list_item ali on ali.uuid = v.list_item_uuid "); //$NON-NLS-1$	
		}
		if (filter.getEntityTypeKey() != null){
			sql.append(" LEFT JOIN smart.i_entity_type et on et.uuid = e.entity_type_uuid "); //$NON-NLS-1$
		}
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(" ia.keyId = :attributeKey "); //$NON-NLS-1$
		if (filter.getEntityTypeKey() != null){
			sql.append(" AND et.keyId = :entityTypeKey "); //$NON-NLS-1$
		}
		
		sql.append(" AND "); //$NON-NLS-1$
		switch(filter.getAttributeType()){
		case BOOLEAN:
			sql.append(" v.double_value " + SqlGenerator.operatorToSql(Operator.GREATERTHAN) + " 0.5"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case DATE:
			sql.append(" case when ia.type = 'DATE' then cast(v.string_value as date) " + SqlGenerator.operatorToSql(filter.getOperator()) + " cast(:value1 as date) and cast(:value2 as date) else false end"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case LIST:
			if (listItem == null){
				//any option
				sql.append(" v.list_item_uuid is not null "); //$NON-NLS-1$
			}else{
				sql.append(" ali.keyid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			break;
		case EMPLOYEE:
			if (employee == null) {
				//any option
				sql.append(" v.employee_uuid is not null "); //$NON-NLS-1$
			}else {
				sql.append(" v.employee_uuid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			break;
		case NUMERIC:
			sql.append(" v.double_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case TEXT:
			sql.append(" v.string_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		default:
			break;
		}
		
		logString(sql.toString());
		
		NativeQuery<?> query = s.createNativeQuery(sql.toString());
		logString(attribute.getKeyId());
		query.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		if (filter.getEntityTypeKey() != null){
			logString(filter.getEntityTypeKey());
			query.setParameter("entityTypeKey", filter.getEntityTypeKey()); //$NON-NLS-1$
		}
		
		switch(filter.getAttributeType()){
		case BOOLEAN:
			break;
		case DATE:
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
			query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  ); //$NON-NLS-1$
			query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  ); //$NON-NLS-1$
			break;
		case LIST:
			if (listItem != null){
				logString(listItem.getKeyId());
				query.setParameter("value", listItem.getKeyId()); //$NON-NLS-1$
			}
			break;
		case EMPLOYEE:
			if (employee != null){
				logString(UuidUtils.uuidToString(employee.getUuid()));
				query.setParameter("value", employee.getUuid()); //$NON-NLS-1$
			}
			break;
		case NUMERIC:
			logString(filter.getNumberValue().toString());
			query.setParameter("value", filter.getNumberValue()); //$NON-NLS-1$
			break;
		case TEXT:
			String value = filter.getStringValue();
			if (filter.getOperator() == Operator.STR_CONTAINS || filter.getOperator() == Operator.STR_NOTCONTAINS) {
				value = "%" + value + "%"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			logString(value);
			query.setParameter("value", value); //$NON-NLS-1$
			break;
		default:
			break;
		}

		query.executeUpdate();
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("entity_uuid") + " on " + t2 + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.entity_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.entity_uuid = b.entity_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		query = s.createNativeQuery(sql.toString());
		query.executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
	}
	
	private void addFilterColumn(AreaFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(entity_uuid uuid) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append("SELECT uuid FROM smart.area_geometries WHERE ca_uuid = :ca AND keyId = :keyid AND area_type = :type"); //$NON-NLS-1$
		
		logString(sql.toString());
		logString(filter.getKey());
		logString(filter.getType().name());
		logString(UuidUtils.uuidToString(itemProvider.getQueryConservationArea().getUuid()));
		
		NativeQuery<?> query = s.createNativeQuery(sql.toString());
		query.setParameter("ca", itemProvider.getQueryConservationArea().getUuid()); //$NON-NLS-1$
		query.setParameter("keyid", filter.getKey()); //$NON-NLS-1$
		query.setParameter("type", filter.getType().name()); //$NON-NLS-1$
		
		Object x = query.uniqueResult();
		if (x == null) throw new Exception(MessageFormat.format("Area with key {0} not found.", filter.getKey())); //$NON-NLS-1$
		UUID areaUuid = null;
		if (x instanceof UUID){
			areaUuid = (UUID) x;
		}else if (x instanceof byte[]){
			areaUuid = UuidUtils.byteToUUID((byte[])x);
		}
		
		sql = new StringBuilder();
		sql.append("INSERT INTO " + t2 + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("SELECT distinct ss.entity_uuid " ); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(obsTable + " ss JOIN smart.i_entity_location ll on ll.entity_uuid = ss.entity_uuid "); //$NON-NLS-1$
		sql.append("JOIN smart.i_location l on ll.location_uuid = l.uuid, "); //$NON-NLS-1$
		sql.append( " smart.area_geometries a "); //$NON-NLS-1$
		sql.append(" WHERE a.uuid = :areauuid "); //$NON-NLS-1$
		sql.append(" AND smart.intersects(a.geom, l.geometry) "); //$NON-NLS-1$
		
		logString(sql.toString());
		logString(UuidUtils.uuidToString(areaUuid));
		query = s.createNativeQuery(sql.toString());
		query.setParameter("areauuid", areaUuid); //$NON-NLS-1$
		query.executeUpdate();
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("entity_uuid") + " on " + t2 + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.entity_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.entity_uuid = b.entity_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		query = s.createNativeQuery(sql.toString());
		query.executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
	}
	
	private void addFilterColumn(SystemAttributeFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(entity_uuid uuid) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		
		if (filter.getAttribute() == SystemAttribute.RECORD_SOURCE) {
			sql = new StringBuilder();
			sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
			sql.append (" SELECT distinct a.entity_uuid "); //$NON-NLS-1$
			sql.append(" FROM " + obsTable + " a JOIN smart.i_entity_record er on a.entity_uuid = er.entity_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_record r on er.record_uuid = r.uuid JOIN smart.i_recordsource rs on r.source_uuid = rs.uuid"); //$NON-NLS-1$
			sql.append( " WHERE rs.keyid = :recordKey "); //$NON-NLS-1$
			
			logString(sql.toString());
			logString(filter.getStringKey());
			NativeQuery<?> query = s.createNativeQuery(sql.toString());
			query.setParameter("recordKey", filter.getStringKey()); //$NON-NLS-1$
			query.executeUpdate();
		}else if (filter.getAttribute() == SystemAttribute.RECORD_STATUS) {
			sql = new StringBuilder();
			sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
			sql.append (" SELECT distinct a.entity_uuid "); //$NON-NLS-1$
			sql.append(" FROM " + obsTable + " a JOIN smart.i_entity_record er on a.entity_uuid = er.entity_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_record r on er.record_uuid = r.uuid "); //$NON-NLS-1$
			sql.append( " WHERE r.status = :statusKey "); //$NON-NLS-1$
			
			logString(sql.toString());
			logString(filter.getStringKey());
			NativeQuery<?> query = s.createNativeQuery(sql.toString());
			query.setParameter("statusKey", filter.getStringKey()); //$NON-NLS-1$
			query.executeUpdate();
		}else if (filter.getAttribute() == SystemAttribute.RECORD_DATE_CREATED || 
				filter.getAttribute() == SystemAttribute.RECORD_DATE_MODIFIED || 
				filter.getAttribute() == SystemAttribute.RECORD_DATE) {
			sql = new StringBuilder();
			sql.append("INSERT INTO " + t2 ); //$NON-NLS-1$
			sql.append(" SELECT distinct a.entity_uuid "); //$NON-NLS-1$
			sql.append( "FROM " + obsTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_entity_record er on er.entity_uuid = a.entity_uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.i_record r on r.uuid = er.record_uuid "); //$NON-NLS-1$
			
			sql.append(" WHERE "); //$NON-NLS-1$
			
			if (filter.getAttribute() == SystemAttribute.RECORD_DATE_CREATED) {
				sql.append(" cast(r.date_created as date) "); //$NON-NLS-1$
			}else if (filter.getAttribute() == SystemAttribute.RECORD_DATE_MODIFIED) {
				sql.append(" cast(r.last_modified_date as date) "); //$NON-NLS-1$
			}else if (filter.getAttribute() == SystemAttribute.RECORD_DATE) {
				sql.append(" cast(r.primary_date as date) "); //$NON-NLS-1$
			}
			sql.append(SqlGenerator.operatorToSql(filter.getOperator()));
			sql.append(" cast(:value1 as date) and cast(:value2 as date)"); //$NON-NLS-1$
		
			NativeQuery<?> query = s.createNativeQuery(sql.toString());
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
			query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  ); //$NON-NLS-1$
			query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  ); //$NON-NLS-1$
			
		
			logString(sql.toString());
			query.executeUpdate();
			
		}else if (filter.getAttribute() == SystemAttribute.ENTITY_DATE_CREATED || filter.getAttribute() == SystemAttribute.ENTITY_DATE_MODIFIED) {
			
			sql = new StringBuilder();
			sql.append("INSERT INTO " + t2 ); //$NON-NLS-1$
			sql.append(" SELECT distinct e.uuid "); //$NON-NLS-1$
			sql.append( "FROM " + obsTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_entity e on e.uuid = a.entity_uuid "); //$NON-NLS-1$
			sql.append(" WHERE "); //$NON-NLS-1$
			
			if (filter.getAttribute() == SystemAttributeFilter.SystemAttribute.ENTITY_DATE_CREATED) {
				sql.append(" cast (e.date_created as date) "); //$NON-NLS-1$
			}else if (filter.getAttribute() == SystemAttributeFilter.SystemAttribute.ENTITY_DATE_MODIFIED) {
				sql.append(" cast( e.date_modified as date) "); //$NON-NLS-1$
			}
			sql.append(SqlGenerator.operatorToSql(filter.getOperator()));
			sql.append(" cast(:value1 as date) and cast(:value2 as date)"); //$NON-NLS-1$
		
			NativeQuery<?> query = s.createNativeQuery(sql.toString());
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
			query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  ); //$NON-NLS-1$
			query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  ); //$NON-NLS-1$
			
		
			logString(sql.toString());
			query.executeUpdate();
		
		}
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("entity_uuid_tmp") + " on " + t2 + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.entity_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.entity_uuid = b.entity_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		NativeQuery<?> query = s.createNativeQuery(sql.toString());
		query.executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
	}

	private void logString(String string){
		SqlGenerator.logString(string);
	}
	
	public static void switchTables(String tempTable, String obsTable, boolean entityIndex, boolean observationIndex, Session s){
		StringBuilder sql = new StringBuilder();
		sql.append("DROP TABLE " + obsTable); //$NON-NLS-1$
		SqlGenerator.logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		String newname = obsTable;
		int index = newname.indexOf('.');
		if (index > 0) {
			newname = newname.substring(index+1);
		}
		sql.append("ALTER TABLE " + tempTable + " RENAME TO " + newname); //$NON-NLS-1$ //$NON-NLS-2$
		SqlGenerator.logString(sql.toString());
		s.createNativeQuery(sql.toString()).executeUpdate();

		if (entityIndex || observationIndex){
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + SqlGenerator.createIndexName(obsTable) + "_entity_uuid_idx on " + obsTable + " (entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			SqlGenerator.logString(sql.toString());
			s.createNativeQuery(sql.toString()).executeUpdate();
		}
	}
	
}
