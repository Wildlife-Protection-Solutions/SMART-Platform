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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
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
import org.wcs.smart.util.UuidUtils;

/**
 * filter processor for processing observation filters
 * 
 * @author Emily
 *
 */
public class ObservationFilterProcessor {

	private IQueryFilter filter;
	private Date[] dFilter;
	private Session s;
	
	private Exception visitorException;
	private HashMap<IQueryFilter, String> filterToColumnName = new HashMap<IQueryFilter, String>();
	
	public ObservationFilterProcessor(IQueryFilter filter, Date[] dFilter, Session s){
		this.filter = filter;
		this.dFilter = dFilter;
		this.s = s;
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
							filter instanceof IntelAttributeFilter){
							filtercnt[0] = filtercnt[0]+1;
						}
					}catch(Exception e){
						visitorException = e;
					}
				}
			});
		}
		monitor.beginTask("Processing Filter", filtercnt[0] + 2);
		//1. - Observation Query Filter
		//create a table of all observations using date filter
		monitor.subTask("Creating temporary observation table");
		String obsTable = SqlGenerator.createTempTableName();
				
		StringBuilder tableColumns = new StringBuilder();
		tableColumns.append("location_uuid char(16) for bit data, observation_uuid char(16) for bit data");
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE ");
		sql.append(obsTable);
		sql.append("(");
		sql.append(tableColumns);
		sql.append(")");
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
				
		sql = new StringBuilder();
		sql.append("INSERT INTO " + obsTable);
		sql.append(" SELECT l.uuid, o.uuid FROM smart.i_location l ");
		sql.append(" LEFT JOIN smart.i_observation o on l.uuid = o.location_uuid ");
		sql.append( " WHERE ");
		sql.append(" l.ca_uuid = :ca ");
		
		String dateFilter = SqlGenerator.generateDateClause(dFilter, "datetime");
		if (dateFilter != null){
			sql.append(" AND ");
			sql.append(dateFilter);
		}
		
		logString(UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()));		
		logString(sql.toString());
		SQLQuery query = s.createSQLQuery(sql.toString());
		query.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid());
		query.executeUpdate();
		
		//create indexes to help with performance
		sql = new StringBuilder();
		sql.append("CREATE INDEX location_uuid_idx on " + obsTable + " (location_uuid)");
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
		sql = new StringBuilder();
		sql.append("CREATE INDEX observation_uuid_idx on " + obsTable + " (observation_uuid)");
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
		
		monitor.worked(1);
		
		//for each filter add a column for that filter
		//set the filter value to true or false depending on the filter
		if(filter != null){	
			filter.accept(new IFilterVisitor() {
				private int columnCnt = 1;
				
				String tempTable = SqlGenerator.createTempTableName();
				
				private String createColumn(IQueryFilter filter){
					monitor.subTask(MessageFormat.format("Processing filter {0}/{1}", columnCnt,filtercnt[0] ));
					String columnName = "filter_" + columnCnt++;
					tableColumns.append(", " + columnName + " boolean ");
					
					StringBuilder sql = new StringBuilder();
					sql.append("CREATE TABLE ");
					sql.append(tempTable);
					sql.append(" (");
					sql.append(tableColumns);
					sql.append (")");
					
					logString(sql.toString());
					s.createSQLQuery(sql.toString()).executeUpdate();
					
					filterToColumnName.put(filter, columnName);
					monitor.worked(1);
					return columnName;
				}
				
				@Override
				public void visitElement(IQueryFilter filter) {
					if (visitorException != null) return;
					try{
						if (filter instanceof AreaFilter){
							String columnName = createColumn(filter);
							addFilterColumn((AreaFilter) filter, obsTable, tempTable, columnName);
							switchTables(tempTable, obsTable);
						}else if (filter instanceof DataModelFilter){
							String columnName = createColumn(filter);
							addFilterColumn((DataModelFilter) filter, obsTable, tempTable, columnName);
							switchTables(tempTable, obsTable);
						}else if (filter instanceof EntityFilter){
							String columnName = createColumn(filter);
							addFilterColumn((EntityFilter) filter, obsTable, tempTable,  columnName);
							switchTables(tempTable, obsTable);
						}else if (filter instanceof EntityTypeFilter){
							String columnName = createColumn(filter);
							addFilterColumn((EntityTypeFilter) filter, obsTable, tempTable, columnName);
							switchTables(tempTable, obsTable);
						}else if (filter instanceof IntelAttributeFilter){
							String columnName = createColumn(filter);
							addFilterColumn((IntelAttributeFilter) filter, obsTable, tempTable, columnName);
							switchTables(tempTable, obsTable);
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
		monitor.subTask("Filtering observations");
		if (filter != null){
			String tempTable = SqlGenerator.createTempTableName();
			
			sql = new StringBuilder();
			sql.append("CREATE TABLE " + tempTable );
			sql.append("(");
			sql.append(tableColumns);
			sql.append(")");
			logString(sql.toString());
			s.createSQLQuery(sql.toString()).executeUpdate();	
			
			final StringBuilder deleteSql = new StringBuilder();
			deleteSql.append("INSERT INTO " + tempTable );
			deleteSql.append(" SELECT * FROM ");
			deleteSql.append( obsTable );
			deleteSql.append(" WHERE ");
			filter.accept(new IFilterVisitor() {
				private Set<BracketFilter> filters = new HashSet<>();
				@Override
				public void visitElement(IQueryFilter filter) {
					if (visitorException != null) return;
					try{
					String columnName = filterToColumnName.get(filter);
					if (columnName != null){
						deleteSql.append(" ( " + columnName + " is not null ) ");
						deleteSql.append(" ");
					}else if (filter.getClass().equals(BooleanFilter.class)){
						deleteSql.append(  SqlGenerator.operatorToSql(((BooleanFilter)filter).getOperator()));
						deleteSql.append(" ");
					}else if (filter.getClass().equals(NotFilter.class)){
						deleteSql.append( SqlGenerator.operatorToSql(Operator.NOT));
						deleteSql.append(" ");
					}else if (filter.getClass().equals(BracketFilter.class)){
						if (filters.contains(filter)){
							deleteSql.append(SqlGenerator.operatorToSql(Operator.BRACKET_CLOSE));
						}else{
							deleteSql.append(SqlGenerator.operatorToSql(Operator.BRACKET_OPEN));
							filters.add((BracketFilter) filter);
						}
						deleteSql.append(" ");
					}
					}catch (Exception ex){
						visitorException = ex;
					}
				}
				
			});		
			if (visitorException != null) throw visitorException;
			
			logString(deleteSql.toString());
			s.createSQLQuery(deleteSql.toString()).executeUpdate();
			
			switchTables(tempTable, obsTable);
		}
		monitor.worked(1);
		
		return obsTable;
	}
	
	
	
	private void addFilterColumn(DataModelFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		if (filter.getAttributeKey() == null){
			//only a category filter
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO " + tempTable + " " );
			sql.append(" SELECT a.*, CASE WHEN c.hkey is null IS NULL THEN NULL ELSE case when (c.hkey >= :hkey1 and c.hkey < :hkey2 ) then TRUE else null end END ");
			sql.append(" FROM " + obsTable + " a LEFT JOIN ");
			sql.append(" smart.i_observation o on a.observation_uuid = o.uuid ");
			sql.append(" LEFT JOIN smart.dm_category c on c.uuid = o.category_uuid ");
			
			String hkey1 = filter.getCategoryKey();
			String hkey2 = filter.getCategoryKey().substring(0, filter.getCategoryKey().length() - 1) + "/";
			
			logString(hkey1);
			logString(hkey2);
			
			SQLQuery query = s.createSQLQuery(sql.toString());
			query.setParameter("hkey1", hkey1);
			query.setParameter("hkey2", hkey2);
			logString(sql.toString());
			query.executeUpdate();
			
			return;
			
		}
		//category and perhaps an attribute filter

		Attribute attribute = (Attribute)s.createCriteria(Attribute.class)
				.add(Restrictions.eq("keyId", filter.getAttributeKey()))
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.uniqueResult();
		if (attribute == null){
			throw new Exception(MessageFormat.format("No attribute with key {0} found in data model." , filter.getAttributeKey()));
		}
		AttributeListItem li = null;
		AttributeTreeNode treenode = null;
		if (filter.getAttributeType() == Attribute.AttributeType.LIST){
			if (!filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
				li = (AttributeListItem)s.createCriteria(AttributeListItem.class)
					.add(Restrictions.eq("keyId", filter.getKeyValue()))
					.add(Restrictions.eq("attribute", attribute))
					.uniqueResult();
				if (li == null) throw new Exception(MessageFormat.format("No list item with key {0} found for attribute {1}.", filter.getKeyValue(), attribute.getName()));
			}
		}else if (filter.getAttributeType() == Attribute.AttributeType.TREE){
			treenode = (AttributeTreeNode)s.createCriteria(AttributeTreeNode.class)
					.add(Restrictions.eq("hkey", filter.getKeyValue()))
					.add(Restrictions.eq("attribute", attribute))
					.uniqueResult();
			if (treenode == null) throw new Exception(MessageFormat.format("No tree node item with key {0} found for attribute {1}.", filter.getKeyValue(), attribute.getName()));
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + tempTable );
		sql.append(" SELECT a.*, CASE WHEN o.uuid IS NULL THEN NULL ELSE CASE WHEN ");
		if (filter.getCategoryKey() != null){
			sql.append(" (c.hkey >= :hkey1 and c.hkey < :hkey2) AND ");
		}
		switch(filter.getAttributeType()){
		case BOOLEAN:
			sql.append(" ia.double_value " + SqlGenerator.operatorToSql(Operator.GREATERTHAN) + " 0.5");
			break;
		case DATE:
			sql.append(" cast(ia.string_value as date) " + SqlGenerator.operatorToSql(filter.getOperator()) + " cast(:value1 as date) and cast(:value2 as date)");
			break;
		case LIST:
			if (li == null){
				sql.append(" ia.list_element_uuid is not null ");
			}else{
				sql.append(" ia.list_element_uuid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value");
			}
			break;
		case NUMERIC:
			sql.append(" ia.double_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value");
			break;
		case TEXT:
			sql.append(" ia.string_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value");
			break;
		case TREE:
			sql.append(" ia.tree_node_uuid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value");
			break;
		default:
			break;
		}
		sql.append(" THEN TRUE ELSE NULL END END ");
		sql.append(" FROM " + obsTable + " a LEFT JOIN ");
		sql.append(" smart.i_observation o on a.observation_uuid = o.uuid ");
		if (filter.getCategoryKey() != null){
			sql.append(" LEFT JOIN smart.dm_category c on c.uuid = o.category_uuid ");
		}
		sql.append(" LEFT JOIN smart.i_observation_attribute ia on ia.observation_uuid = o.uuid and ia.attribute_uuid = :attributeUuid ");
		
		SQLQuery query = s.createSQLQuery(sql.toString());
		query.setParameter("attributeUuid", attribute.getUuid());
		logString(UuidUtils.uuidToString(attribute.getUuid()));
		
		if (filter.getCategoryKey() != null){
			String hkey1 = filter.getCategoryKey();
			String hkey2 = filter.getCategoryKey().substring(0, filter.getCategoryKey().length() - 1) + "/";
			logString(hkey1);
			logString(hkey2);
			logString(UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()));
			
			query.setParameter("hkey1", hkey1);
			query.setParameter("hkey2", hkey2);
		}
		switch(filter.getAttributeType()){
		case BOOLEAN:
			break;
		case DATE:
			logString((new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
			logString((new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
			query.setParameter("value1", (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  );
			query.setParameter("value2", (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  );
			break;
		case LIST:
			if (li != null){
				logString(UuidUtils.uuidToString(li.getUuid()));
				query.setParameter("value", li.getUuid());
			}
			break;
		case TREE:
			logString(UuidUtils.uuidToString(treenode.getUuid()));
			query.setParameter("value", treenode.getUuid());
			break;
		case NUMERIC:
			logString(filter.getNumberValue().toString());
			query.setParameter("value", filter.getNumberValue());
			break;
		case TEXT:
			logString(filter.getStringValue());
			query.setParameter("value", filter.getStringValue());
			break;
		default:
			break;
		}
		
		logString(sql.toString());
		query.executeUpdate();
	}
	
	//select * from table where (column name is not null) OR NOT (columnname is not null)
	private void addFilterColumn(EntityFilter filter, String obsTable, String tempTable, String columnName){
		//todo: configure uuid
		StringBuilder sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable);
		sql.append(" SELECT a.*, CASE WHEN l.location_uuid IS NULL THEN NULL ELSE case when l.entity_uuid = :uuid then true else null end END ");
		sql.append(" FROM " + obsTable + " a LEFT JOIN ");
		sql.append(" smart.i_entity_location l ON a.location_uuid = l.location_uuid");
		
		logString(sql.toString());
		logString(UuidUtils.uuidToString(filter.getEntityUuid()));
		
		SQLQuery query = s.createSQLQuery(sql.toString());
		query.setParameter("uuid", filter.getEntityUuid());
		query.executeUpdate();
	}
	
	private void addFilterColumn(EntityTypeFilter filter, String obsTable, String tempTable, String columnName){
		StringBuilder sql = new StringBuilder();
		sql.append(" INSERT INTO " + tempTable);
		sql.append(" SELECT a.*, CASE WHEN t.keyId IS NULL THEN NULL ELSE CASE WHEN t.keyId = :typeKey then true else null end END ");
		sql.append(" FROM " + obsTable + " a LEFT JOIN ");
		sql.append(" smart.i_entity_location l ON a.location_uuid = l.location_uuid ");
		sql.append(" LEFT JOIN smart.i_entity e ON l.entity_uuid = e.uuid  ");
		sql.append(" LEFT JOIN smart.i_entity_type t on e.entity_type_uuid = t.uuid ");
		
		logString(sql.toString());
		logString(filter.getTypeKey());
		
		SQLQuery query = s.createSQLQuery(sql.toString());
		query.setParameter("typeKey",  filter.getTypeKey());
		query.executeUpdate();
	}
	
	private void addFilterColumn(IntelAttributeFilter filter, String obsTable, String tempTable, String columnName) throws Exception{
		
		
		IntelAttribute attribute = (IntelAttribute)s.createCriteria(IntelAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.add(Restrictions.eq("keyId", filter.getAttributeKey()))
				.uniqueResult();
		
		if (attribute == null) throw new Exception(MessageFormat.format("Unable to find intelligence attribute with key {0}", filter.getAttributeKey()));
		IntelAttributeListItem listItem = null;
		if (filter.getAttributeType() == AttributeType.LIST && filter.getKeyValue() != null){
			if (!filter.getKeyValue().equalsIgnoreCase(IQueryFilter.ANY_OPTION_KEY)){
				listItem = (IntelAttributeListItem)s.createCriteria(IntelAttributeListItem.class)
					.add(Restrictions.eq("attribute", attribute))
					.add(Restrictions.eq("keyId", filter.getKeyValue()))
					.uniqueResult();	
				if (listItem == null) throw new Exception(MessageFormat.format("Unable to find intelligence list item attribute with key {0}", filter.getAttributeKey()));
			}
		}
		
		IntelEntityType type = null;
		if (filter.getEntityTypeKey() != null){
			type = (IntelEntityType)s.createCriteria(IntelEntityType.class)
					.add(Restrictions.eq("keyId", filter.getEntityTypeKey()))
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.uniqueResult();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + tempTable);
		sql.append(" SELECT a.*, cast when l.location_uuid is null then null else true end ");
		sql.append(" FROM " + obsTable + " a LEFT JOIN ");
		sql.append(" smart.i_entity_location l on a.location_uuid = l.location_uuid ");
		sql.append(" LEFT JOIN smart.i_entity_attribute_value v on v.entity_uuid = l.entity_uuid and (v.attribute_uuid is null OR v.attribute_uuid = :attributeUuid) ");
		if (filter.getEntityTypeKey() != null){
			sql.append("LEFT JOIN smart.i_entity e on l.entity_uuid = e.uuid and (e.entity_type_uuid is null OR e.entity_type_uuid = :entityTypeUuid) ");
		}
		sql.append(" WHERE ");
		sql.append(" l.location_uuid is null OR ( ");
		switch(filter.getAttributeType()){
		case BOOLEAN:
			sql.append(" v.double_value " + SqlGenerator.operatorToSql(Operator.GREATERTHAN) + " 0.5");
			break;
		case DATE:
			sql.append(" cast(v.string_value as date) " + SqlGenerator.operatorToSql(filter.getOperator()) + " cast(:value1 as date) and cast(:value2 as date)");
			break;
		case LIST:
			if (listItem == null){
				//any option
				sql.append(" v.list_item_uuid is not null ");
			}else{
				sql.append(" v.list_item_uuid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value");
			}
			break;
		case NUMERIC:
			sql.append(" v.double_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value");
			break;
		case TEXT:
			sql.append(" v.string_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value");
			break;
		default:
			break;
		}
		sql.append(" ))");
		
		logString(sql.toString());
		
		SQLQuery query = s.createSQLQuery(sql.toString());
		if (filter.getEntityTypeKey() != null){
			logString(UuidUtils.uuidToString(type.getUuid()));
			query.setParameter("entityTypeUuid", type.getUuid());
		}
		query.setParameter("attributeUuid", attribute.getUuid());
		switch(filter.getAttributeType()){
		case BOOLEAN:
			break;
		case DATE:
			logString((new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0]));
			logString((new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1]));
			query.setParameter("value1", (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[0])  );
			query.setParameter("value2", (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(filter.getDateValues()[1])  );
			break;
		case LIST:
			if (listItem != null){
				logString(UuidUtils.uuidToString(listItem.getUuid()));
				query.setParameter("value", listItem.getUuid());
			}
			break;
		case NUMERIC:
			logString(filter.getNumberValue().toString());
			query.setParameter("value", filter.getNumberValue());
			break;
		case TEXT:
			logString(filter.getStringValue());
			query.setParameter("value", filter.getStringValue());
			break;
		default:
			break;
		}

		query.executeUpdate();
		
		
		
	}
	
	private void addFilterColumn(AreaFilter filter, String obsTable, String tempTable, String columnName){
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT uuid FROM smart.area_geometries WHERE ca_uuid = :ca AND keyId = :keyid AND area_type = :type");
		
		logString(sql.toString());
		logString(filter.getKey());
		logString(filter.getType().name());
		logString(UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()));
		
		SQLQuery query = s.createSQLQuery(sql.toString());
		query.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid());
		query.setParameter("keyid", filter.getKey());
		query.setParameter("type", filter.getType().name());
		
		Object x = query.uniqueResult();
		UUID areaUuid = null;
		if (x instanceof UUID){
			areaUuid = (UUID) x;
		}else if (x instanceof byte[]){
			areaUuid = UuidUtils.byteToUUID((byte[])x);
		}
		
		sql = new StringBuilder();
		sql.append("INSERT INTO " + tempTable + " ");
		sql.append("SELECT a.*, CASE WHEN l.uuid IS NULL then null else case when smart.intersects(a.geom, l.geometry) then true ELSE null end end ");
		sql.append(" FROM ");
		sql.append(obsTable + " ss LEFT JOIN ");
		sql.append(" smart.i_location l on ss.location_uuid = l.uuid, smart.area_geometries a where a.uuid = :areauuid ");
		
		logString(sql.toString());
		logString(UuidUtils.uuidToString(areaUuid));
		query = s.createSQLQuery(sql.toString());
		query.setParameter("areauuid", areaUuid);
		query.executeUpdate();
	}
	
	private void switchTables(String tempTable, String obsTable){
		StringBuilder sql = new StringBuilder();
		sql.append("DROP TABLE " + obsTable);
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append("RENAME TABLE " + tempTable + " TO " + obsTable);
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX location_uuid_idx on " + obsTable + " (location_uuid)");
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append("CREATE INDEX observation_uuid_idx on " + obsTable + " (observation_uuid)");
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
	}
	
	private void logString(String string){
		SqlGenerator.logString(string);
	}
	
}
