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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.i2.internal.Messages;
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
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;
import org.wcs.smart.util.UuidUtils;

/**
 * filter processor for processing observation filters
 * 
 * @author Emily
 *
 */
public class ObservationFilterProcessor {

	private IQueryFilter filter;
	private LocalDate[] dFilter;
	private Session s;
	
	private Exception visitorException;
	private HashMap<IQueryFilter, String> filterToColumnName = new HashMap<IQueryFilter, String>();
	private IQueryItemProvider itemProvider;
	
	private Set<IntelProfile> profileFilter;

	
	public ObservationFilterProcessor(IQueryFilter filter, 	Set<IntelProfile> profileFilter, LocalDate[] dFilter, IQueryItemProvider itemProvider, Session s){
		this.filter = filter;
		this.dFilter = dFilter;
		this.s = s;
		this.itemProvider = itemProvider;
		this.profileFilter = profileFilter;
	}
	
	public HashMap<IQueryFilter, String> getFilterToColumnNames(){
		return filterToColumnName;
	}
	
	/**
	 * Returns a table with a list of observations that match filters
	 * 
	 * @return
	 * @throws Exception
	 */
	public String processFilter(IProgressMonitor monitor) throws Exception{
		

		final int[] filtercnt = new int[]{0};
		if(filter != null){	
			//get filter count for progress monitor
			filter.accept(new IFilterVisitor() {
				@Override
				public void visitElement(IQueryFilter filter) {
					if (visitorException != null) return;
					try{
						if (filter instanceof AreaFilter || 
							filter instanceof DataModelFilter ||
							filter instanceof EntityFilter ||
							filter instanceof EntityTypeFilter ||
							filter instanceof SystemAttributeFilter ||
							filter instanceof IntelAttributeFilter){
							filtercnt[0] = filtercnt[0]+1;
						}
					}catch(Exception e){
						visitorException = e;
					}
				}
			});
		}
		monitor.beginTask(Messages.ObservationFilterProcessor_Progress1, filtercnt[0] + 2);
		//1. - Observation Query Filter
		//create a table of all observations using date filter
		monitor.subTask(Messages.ObservationFilterProcessor_Progress2);
		String obsTable = SqlGenerator.createTempTableName();
				
		StringBuilder tableColumns = new StringBuilder();
		tableColumns.append("location_uuid char(16) for bit data, observation_uuid char(16) for bit data"); //$NON-NLS-1$
		tableColumns.append(",ca_uuid char(16) for bit data, ca_id varchar(8), ca_name varchar(256) "); //$NON-NLS-1$
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(obsTable);
		sql.append("("); //$NON-NLS-1$
		sql.append(tableColumns);
		sql.append(")"); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
				
		sql = new StringBuilder();
		sql.append("INSERT INTO " + obsTable); //$NON-NLS-1$
		sql.append(" SELECT l.uuid, o.uuid, ca.uuid, ca.id, ca.name FROM smart.i_location l "); //$NON-NLS-1$
		sql.append(" JOIN smart.conservation_area ca on l.ca_uuid = ca.uuid " ); //$NON-NLS-1$
		sql.append(" JOIN smart.i_record r on r.uuid = l.record_uuid "); //$NON-NLS-1$
		sql.append(" LEFT JOIN smart.i_observation o on l.uuid = o.location_uuid "); //$NON-NLS-1$


		sql.append( " WHERE "); //$NON-NLS-1$
		sql.append(" l.ca_uuid in (:cas) and r.profile_uuid in (:profiles) "); //$NON-NLS-1$
		
		String dateFilter = SqlGenerator.generateDateClause(dFilter, "datetime"); //$NON-NLS-1$
		if (dateFilter != null){
			sql.append(" AND "); //$NON-NLS-1$
			sql.append(dateFilter);
		}
		List<UUID> caUuids = itemProvider.getConservationAreas().stream().map(e->e.getUuid()).collect(Collectors.toList());
		for (UUID uuid : caUuids) {
			logString(UuidUtils.uuidToString(uuid));
		}
		Collection<UUID> profileUuids = profileFilter.stream().map(a->a.getUuid()).collect(Collectors.toSet());
		
		for (UUID uuid : profileUuids) {
			logString(UuidUtils.uuidToString(uuid));
		}
		
		logString(sql.toString());
		
		if (monitor.isCanceled()) return null;
		MutationQuery query = s.createNativeMutationQuery(sql.toString());
		query.setParameterList("cas", caUuids); //$NON-NLS-1$
		query.setParameterList("profiles", profileUuids); //$NON-NLS-1$
		query.executeUpdate();
		
		//create indexes to help with performance
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + obsTable + "_location_uuid_idx on " + obsTable + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		if (monitor.isCanceled()) return null;
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + obsTable + "_observation_uuid_idx on " + obsTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		if (monitor.isCanceled()) return null;
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		monitor.worked(1);
		
		//for each filter add a column for that filter
		//set the filter value to true or false depending on the filter
		if(filter != null){	
			filter.accept(new IFilterVisitor() {
				private int columnCnt = 1;
				
				String tempTable = SqlGenerator.createTempTableName();
				
				private String createColumn(IQueryFilter filter){
					monitor.subTask(MessageFormat.format(Messages.ObservationFilterProcessor_Progress3, columnCnt,filtercnt[0] ));
					String columnName = "filter_" + columnCnt++; //$NON-NLS-1$
					tableColumns.append(", " + columnName + " boolean "); //$NON-NLS-1$ //$NON-NLS-2$
					
					StringBuilder sql = new StringBuilder();
					sql.append("CREATE TABLE "); //$NON-NLS-1$
					sql.append(tempTable);
					sql.append(" ("); //$NON-NLS-1$
					sql.append(tableColumns);
					sql.append (")"); //$NON-NLS-1$
					
					logString(sql.toString());
					s.createNativeMutationQuery(sql.toString()).executeUpdate();
					
					filterToColumnName.put(filter, columnName);
					monitor.worked(1);
					return columnName;
				}
				
				@Override
				public void visitElement(IQueryFilter filter) {
					if (monitor.isCanceled()) return ;
					if (visitorException != null) return;
					try{
						if (filter instanceof AreaFilter){
							String columnName = createColumn(filter);
							addFilterColumn((AreaFilter) filter, obsTable, tempTable, columnName);
							SqlGenerator.switchTables(tempTable, obsTable, true, true, s);
						}else if (filter instanceof DataModelFilter){
							String columnName = createColumn(filter);
							addFilterColumn((DataModelFilter) filter, obsTable, tempTable, columnName);
							SqlGenerator.switchTables(tempTable, obsTable, true, true, s);
						}else if (filter instanceof EntityFilter){
							String columnName = createColumn(filter);
							addFilterColumn((EntityFilter) filter, obsTable, tempTable,  columnName);
							SqlGenerator.switchTables(tempTable, obsTable, true, true, s);
						}else if (filter instanceof EntityTypeFilter){
							String columnName = createColumn(filter);
							addFilterColumn((EntityTypeFilter) filter, obsTable, tempTable, columnName);
							SqlGenerator.switchTables(tempTable, obsTable, true, true, s);
						}else if (filter instanceof IntelAttributeFilter){
							String columnName = createColumn(filter);
							addFilterColumn((IntelAttributeFilter) filter, obsTable, tempTable, columnName);
							SqlGenerator.switchTables(tempTable, obsTable, true, true, s);
						}else if (filter instanceof SystemAttributeFilter) {
							String columnName = createColumn(filter);
							addFilterColumn((SystemAttributeFilter)filter, obsTable, tempTable, columnName);
							SqlGenerator.switchTables(tempTable, obsTable, true, false, s);
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
		if (monitor.isCanceled()) return null;
		monitor.subTask(Messages.ObservationFilterProcessor_Progress4);
		if (filter != null){
			String tempTable = SqlGenerator.createTempTableName();
			
			sql = new StringBuilder();
			sql.append("CREATE TABLE " + tempTable ); //$NON-NLS-1$
			sql.append("("); //$NON-NLS-1$
			sql.append(tableColumns);
			sql.append(")"); //$NON-NLS-1$
			logString(sql.toString());
			s.createNativeMutationQuery(sql.toString()).executeUpdate();	
			
			final StringBuilder deleteSql = new StringBuilder();
			deleteSql.append("INSERT INTO " + tempTable ); //$NON-NLS-1$
			deleteSql.append(" SELECT * FROM "); //$NON-NLS-1$
			deleteSql.append( obsTable );
			deleteSql.append(" WHERE "); //$NON-NLS-1$
			filter.accept(new IFilterVisitor() {
				private Set<BracketFilter> filters = new HashSet<>();
				@Override
				public void visitElement(IQueryFilter filter) {
					if (visitorException != null) return;
					try{
					String columnName = filterToColumnName.get(filter);
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
			
			if (monitor.isCanceled()) return null;
			logString(deleteSql.toString());
			s.createNativeMutationQuery(deleteSql.toString()).executeUpdate();
			
			SqlGenerator.switchTables(tempTable, obsTable, true, true, s);
		}
		monitor.worked(1);
		
		return obsTable;
	}
	
	private void addFilterColumn(DataModelFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(observation_uuid char(16) for bit data) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		if (filter.getAttributeKey() == null){
			//only a category filter
			sql = new StringBuilder();
			sql.append("INSERT INTO " + t2 + " " ); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" SELECT distinct o.uuid "); //$NON-NLS-1$
			sql.append(" FROM " + obsTable + " a JOIN "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" smart.i_observation o on a.observation_uuid = o.uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.dm_category c on c.uuid = o.category_uuid "); //$NON-NLS-1$
			sql.append(" WHERE (c.hkey >= :hkey1 and c.hkey < :hkey2 ) "); //$NON-NLS-1$
			String hkey1 = filter.getCategoryKey();
			String hkey2 = filter.getCategoryKey().substring(0, filter.getCategoryKey().length() - 1) + "/"; //$NON-NLS-1$
			
			logString(hkey1);
			logString(hkey2);
			
			MutationQuery query = s.createNativeMutationQuery(sql.toString());
			query.setParameter("hkey1", hkey1); //$NON-NLS-1$
			query.setParameter("hkey2", hkey2); //$NON-NLS-1$
			logString(sql.toString());
			query.executeUpdate();
			
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + SqlGenerator.createIndexName("observation_uuid_tmp") + " on " + t2 + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			logString(sql.toString());
			s.createNativeMutationQuery(sql.toString()).executeUpdate();
			
			sql = new StringBuilder();
			sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
			sql.append(" SELECT a.*, CASE WHEN b.observation_uuid is null then null else true end "); //$NON-NLS-1$
			sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.observation_uuid = b.observation_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logString(sql.toString());
			s.createNativeMutationQuery(sql.toString()).executeUpdate();
			
			sql = new StringBuilder();
			sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
			logString(sql.toString());
			s.createNativeMutationQuery(sql.toString()).executeUpdate();
			
			return;
			
		}
		Map<String,Object> params = new HashMap<>();

		
		//category and perhaps an attribute filter		
		sql = new StringBuilder();
		sql.append("INSERT INTO " + t2 ); //$NON-NLS-1$
		sql.append(" SELECT distinct o.uuid "); //$NON-NLS-1$
		sql.append( "FROM " + obsTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" JOIN smart.i_observation o on a.observation_uuid = o.uuid "); //$NON-NLS-1$
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
		if (filter.getAttributeType() == Attribute.AttributeType.MLIST) {
			params.putAll( WaypointFilterProcessor.processMultiSelectAttributeFilter(sql, filter) );
		}
		sql.append(" WHERE dma.keyId = :attributeKey "); //$NON-NLS-1$
		if (filter.getCategoryKey() != null){
			sql.append(" AND (c.hkey >= :hkey1 and c.hkey < :hkey2) "); //$NON-NLS-1$
		}
		if (filter.getAttributeType() != Attribute.AttributeType.MLIST)
			sql.append(" AND "); //$NON-NLS-1$
		
		switch(filter.getAttributeType()){
		case BOOLEAN:
			sql.append(" ia.double_value " + SqlGenerator.operatorToSql(Operator.GREATERTHAN) + " 0.5"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case DATE:
			sql.append(" cast(ia.string_value as date) " + SqlGenerator.operatorToSql(filter.getOperator()) + " cast(:value1 as date) and cast(:value2 as date)"); //$NON-NLS-1$ //$NON-NLS-2$
			
			params.put("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  ); //$NON-NLS-1$
			params.put("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  ); //$NON-NLS-1$
			
			break;
		case LIST:
			if (filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
				sql.append(" ia.list_element_uuid is not null "); //$NON-NLS-1$
			}else{
				sql.append(" tl.keyid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
				params.put("value",  filter.getKeyValue()); //$NON-NLS-1$
			}
			break;
		case NUMERIC:
			sql.append(" ia.double_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			params.put("value", filter.getNumberValue()); //$NON-NLS-1$
			break;
		case TEXT:
			sql.append(" ia.string_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$
			params.put("value", filter.getStringValue()); //$NON-NLS-1$
			break;
		case TREE:
			sql.append( " ( ta.hkey >= :tree1 and ta.hkey < :tree2 ) "); //$NON-NLS-1$
			String tree1 = filter.getKeyValue();
			String tree2 = tree1.substring(0, tree1.length() - 1) + "/"; //$NON-NLS-1$
			params.put("tree1", tree1); //$NON-NLS-1$
			params.put("tree2", tree2); //$NON-NLS-1$
			break;
		case POLYGON:
		case LINE:
			String field = "double_value"; //$NON-NLS-1$
			if (filter.getGeometryProperty() ==  AttributeFilter.GeometryProperty.AREA) {
				field = "double_value_2"; //$NON-NLS-1$
			}
			sql.append(" ia." + field + " " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			params.put("value", filter.getNumberValue()); //$NON-NLS-1$
			break;
			
		default:
			break;
		}
		MutationQuery query = s.createNativeMutationQuery(sql.toString());
		query.setParameter("attributeKey", filter.getAttributeKey()); //$NON-NLS-1$
		logString(filter.getAttributeKey());
		
		if (filter.getCategoryKey() != null){
			String hkey1 = filter.getCategoryKey();
			String hkey2 = filter.getCategoryKey().substring(0, filter.getCategoryKey().length() - 1) + "/"; //$NON-NLS-1$
			logString(hkey1);
			logString(hkey2);
			
			query.setParameter("hkey1", hkey1); //$NON-NLS-1$
			query.setParameter("hkey2", hkey2); //$NON-NLS-1$
		}
		for (Entry<String,Object> p : params.entrySet()) {
			logString(p.getKey() + " - " + p.getValue().toString()); //$NON-NLS-1$
			query.setParameter(p.getKey(),p.getValue());
		}
		
		logString(sql.toString());
		query.executeUpdate();
		
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("observation_uuid_tmp") + " on " + t2 + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.observation_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.observation_uuid = b.observation_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
	}
	
	//select * from table where (column name is not null) OR NOT (columnname is not null)
	private void addFilterColumn(EntityFilter filter, String obsTable, String tempTable, String columnName){
		//todo: configure uuid
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(location_uuid char(16) for bit data) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
		sql.append (" SELECT distinct l.location_uuid "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a JOIN smart.i_entity_location l on a.location_uuid = l.location_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append( " WHERE l.entity_uuid = :uuid "); //$NON-NLS-1$
		
		logString(sql.toString());
		logString(UuidUtils.uuidToString(filter.getEntityUuid()));
		s.createNativeMutationQuery(sql.toString())
			.setParameter("uuid", filter.getEntityUuid()) //$NON-NLS-1$
			.executeUpdate();
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("location_uuid") + " on " + t2 + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.location_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.location_uuid = b.location_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
	}
	
	private void addFilterColumn(EntityTypeFilter filter, String obsTable, String tempTable, String columnName){
		
		//todo: configure uuid
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(location_uuid char(16) for bit data) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
				
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
		sql.append (" SELECT distinct l.location_uuid "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a JOIN smart.i_entity_location l on a.location_uuid = l.location_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" JOIN smart.i_entity e ON l.entity_uuid = e.uuid "); //$NON-NLS-1$
		sql.append(" JOIN smart.i_entity_type t on e.entity_type_uuid = t.uuid "); //$NON-NLS-1$
		sql.append( " WHERE t.keyId = :typeKey "); //$NON-NLS-1$
				
		logString(sql.toString());
		logString(filter.getTypeKey());
		s.createNativeMutationQuery(sql.toString())
			.setParameter("typeKey",  filter.getTypeKey()) //$NON-NLS-1$
			.executeUpdate();
				
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("location_uuid") + " on " + t2 + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
				
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.location_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.location_uuid = b.location_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
				
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
	}
	
	private void addFilterColumn(IntelAttributeFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		
		IntelAttribute attribute = itemProvider.getAttribute(filter.getAttributeKey(), s);
		if (attribute == null) throw new Exception(MessageFormat.format(Messages.ObservationFilterProcessor_IntelAttributeKeyNotFound, filter.getAttributeKey()));
		
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
				if (listItem == null) throw new Exception(MessageFormat.format(Messages.ObservationFilterProcessor_IntelListItemNotFound, filter.getAttributeKey()));
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
					throw new Exception(MessageFormat.format(Messages.ObservationFilterProcessor_EmployeeNotFound, filter.getKeyValue()));
				}
				employee = e;
			}
		}

		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(location_uuid char(16) for bit data) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + t2); //$NON-NLS-1$
		sql.append (" SELECT distinct l.location_uuid "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a JOIN smart.i_entity_location l on a.location_uuid = l.location_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" JOIN smart.i_entity_attribute_value v on v.entity_uuid = l.entity_uuid "); //$NON-NLS-1$
		sql.append(" JOIN smart.i_attribute ia on ia.uuid = v.attribute_uuid "); //$NON-NLS-1$
		if (listItem != null) {
			sql.append(" LEFT JOIN smart.i_attribute_list_item ali on ali.uuid = v.list_item_uuid "); //$NON-NLS-1$	
		}
		if (filter.getEntityTypeKey() != null){
			sql.append("LEFT JOIN smart.i_entity e on l.entity_uuid = e.uuid "); //$NON-NLS-1$
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
		
		MutationQuery query = s.createNativeMutationQuery(sql.toString());
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
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("location_uuid") + " on " + t2 + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.location_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.location_uuid = b.location_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		 s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
	}
	
	private void addFilterColumn(AreaFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(location_uuid char(16) for bit data) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append("SELECT uuid FROM smart.area_geometries WHERE ca_uuid = :ca AND keyId = :keyid AND area_type = :type"); //$NON-NLS-1$
		
		logString(sql.toString());
		logString(filter.getKey());
		logString(filter.getType().name());
		logString(UuidUtils.uuidToString(itemProvider.getQueryConservationArea().getUuid()));
		
		NativeQuery<UUID> query = s.createNativeQuery(sql.toString(), UUID.class);
		query.setParameter("ca", itemProvider.getQueryConservationArea().getUuid()); //$NON-NLS-1$
		query.setParameter("keyid", filter.getKey()); //$NON-NLS-1$
		query.setParameter("type", filter.getType().name()); //$NON-NLS-1$
		
		Object x = query.uniqueResult();
		if (x == null) throw new Exception(MessageFormat.format(Messages.ObservationFilterProcessor_AreaKeyNotFound, filter.getKey()));
		UUID areaUuid = null;
		if (x instanceof UUID){
			areaUuid = (UUID) x;
		}else if (x instanceof byte[]){
			areaUuid = UuidUtils.byteToUUID((byte[])x);
		}
		
		sql = new StringBuilder();
		sql.append("INSERT INTO " + t2 + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("SELECT distinct l.uuid " ); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(obsTable + " ss JOIN smart.i_location l on ss.location_uuid = l.uuid, "); //$NON-NLS-1$
		sql.append( " smart.area_geometries a "); //$NON-NLS-1$
		sql.append(" WHERE a.uuid = :areauuid "); //$NON-NLS-1$
		sql.append(" AND smart.intersects(a.geom, l.geometry) "); //$NON-NLS-1$
		
		logString(sql.toString());
		logString(UuidUtils.uuidToString(areaUuid));
		s.createNativeMutationQuery(sql.toString())
			.setParameter("areauuid", areaUuid) //$NON-NLS-1$
			.executeUpdate();
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("location_uuid") + " on " + t2 + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.location_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.location_uuid = b.location_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
	}
	
	private void addFilterColumn(SystemAttributeFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		
		String t2 = SqlGenerator.createTempTableName();
		StringBuilder sql = new StringBuilder();
		sql.append(" CREATE TABLE " + t2); //$NON-NLS-1$
		sql.append ("(location_uuid char(16) for bit data) "); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		
		if (filter.getAttribute() == SystemAttribute.RECORD_DATE_CREATED || filter.getAttribute() == SystemAttribute.RECORD_DATE_MODIFIED) {
			sql = new StringBuilder();
			sql.append("INSERT INTO " + t2 ); //$NON-NLS-1$
			sql.append(" SELECT distinct a.location_uuid "); //$NON-NLS-1$
			sql.append( "FROM " + obsTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_record r on r.location_uuid = a.location_uuid "); //$NON-NLS-1$
			
			sql.append(" WHERE "); //$NON-NLS-1$
			
			if (filter.getAttribute() == SystemAttribute.RECORD_DATE_CREATED) {
				sql.append(" cast( r.date_created as date) "); //$NON-NLS-1$
			}else if (filter.getAttribute() == SystemAttribute.RECORD_DATE_MODIFIED) {
				sql.append(" cast( r.last_modified_date as date) "); //$NON-NLS-1$
			}
			sql.append(SqlGenerator.operatorToSql(filter.getOperator()));
			sql.append(" cast(:value1 as date) and cast(:value2 as date)"); //$NON-NLS-1$
		
			MutationQuery query = s.createNativeMutationQuery(sql.toString());
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
			query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  ); //$NON-NLS-1$
			query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  ); //$NON-NLS-1$
			
		
			logString(sql.toString());
			query.executeUpdate();
			
		}else if (filter.getAttribute() == SystemAttribute.ENTITY_DATE_CREATED || filter.getAttribute() == SystemAttribute.ENTITY_DATE_MODIFIED) {
			sql = new StringBuilder();
			sql.append("INSERT INTO " + t2 ); //$NON-NLS-1$
			sql.append(" SELECT distinct el.location_uuid "); //$NON-NLS-1$
			sql.append( "FROM " + obsTable + " a "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" JOIN smart.i_entity_location el on el.location_uuid = a.location_uuid "); //$NON-NLS-1$
			sql.append(" JOIN smart.i_entity e on e.uuid = el.entity_uuid "); //$NON-NLS-1$
			
			sql.append(" WHERE "); //$NON-NLS-1$
			
			if (filter.getAttribute() == SystemAttribute.ENTITY_DATE_CREATED) {
				sql.append(" cast (e.date_created as date) "); //$NON-NLS-1$
			}else if (filter.getAttribute() == SystemAttribute.ENTITY_DATE_MODIFIED) {
				sql.append(" cast( e.date_modified as date) "); //$NON-NLS-1$
			}
			sql.append(SqlGenerator.operatorToSql(filter.getOperator()));
			sql.append(" cast(:value1 as date) and cast(:value2 as date)"); //$NON-NLS-1$
		
			MutationQuery query = s.createNativeMutationQuery(sql.toString());
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
			logString((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
			query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  ); //$NON-NLS-1$
			query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  ); //$NON-NLS-1$
			
		
			logString(sql.toString());
			query.executeUpdate();
		
		}
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + SqlGenerator.createIndexName("location_uuid") + " on " + t2 + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable); //$NON-NLS-1$
		sql.append(" SELECT a.*, CASE WHEN b.location_uuid is null then null else true end "); //$NON-NLS-1$
		sql.append(" FROM " + obsTable + " a LEFT JOIN " + t2 + " b on a.location_uuid = b.location_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" DROP TABLE " + t2); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
	}

	private void logString(String string){
		SqlGenerator.logString(string);
	}
	
}
