package org.wcs.smart.i2.query.engine;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

public class ObservationFilterProcessor {

	private IQueryFilter filter;
	private Date[] dFilter;
	private Session s;
	
	private Exception visitorException;
	
	public ObservationFilterProcessor(IQueryFilter filter, Date[] dFilter, Session s){
		this.filter = filter;
		this.dFilter = dFilter;
		this.s = s;
	}
	
	/**
	 * Returns a table with a list of observations that match filters
	 * 
	 * @return
	 * @throws Exception
	 */
	public String processFilter() throws Exception{
		//1. - Observation Query Filter
		//create a table of all observations using date filter
		String obsTable = SqlGenerator.createTempTableName();
				
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + obsTable + " ( location_uuid char(16) for bit data, observation_uuid char(16) for bit data ) ");
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
				
		sql = new StringBuilder();
		sql.append("INSERT INTO " + obsTable);
		sql.append(" SELECT l.uuid, o.uuid FROM smart.i_location l ");
		sql.append(" LEFT JOIN smart.i_observation o on l.uuid = o.location_uuid ");
		String dateFilter = SqlGenerator.generateDateClause(dFilter, "datetime");
		if (dateFilter != null){
			sql.append( " WHERE ");
			sql.append(dateFilter);
		}
				
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
		
		//TODO: look into creating index on location and observation field
		
		//for each filter add a column for that filter
		//set the filter value to true or false depending on the filter
		HashMap<IQueryFilter, String> filterToColumnName = new HashMap<IQueryFilter, String>();
		if(filter != null){	
			filter.accept(new IFilterVisitor() {
				private int columnCnt = 0;
				
				private String createColumn(IQueryFilter filter){
					String columnName = "filter_" + columnCnt++;
					StringBuilder sql = new StringBuilder();
					sql.append("ALTER TABLE " + obsTable + " ADD COLUMN " + columnName + " boolean ");
					
					logString(sql.toString());
					s.createSQLQuery(sql.toString()).executeUpdate();
					
					filterToColumnName.put(filter, columnName);
					return columnName;
				}
				
				@Override
				public void visitElement(IQueryFilter filter) {
					if (visitorException != null) return;
					try{
						if (filter instanceof AreaFilter){
							String columnName = createColumn(filter);
							addFilterColumn((AreaFilter) filter, obsTable, columnName);
						}else if (filter instanceof DataModelFilter){
							String columnName = createColumn(filter);
							addFilterColumn((DataModelFilter) filter, obsTable, columnName);
						}else if (filter instanceof EntityFilter){
							String columnName = createColumn(filter);
							addFilterColumn((EntityFilter) filter, obsTable, columnName);
						}else if (filter instanceof EntityTypeFilter){
							String columnName = createColumn(filter);
							addFilterColumn((EntityTypeFilter) filter, obsTable, columnName);
						}else if (filter instanceof IntelAttributeFilter){
							String columnName = createColumn(filter);
							addFilterColumn((IntelAttributeFilter) filter, obsTable, columnName);
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
			final StringBuilder deleteSql = new StringBuilder();
			deleteSql.append("DELETE FROM " + obsTable );
			deleteSql.append(" WHERE NOT (");
			
			
			filter.accept(new IFilterVisitor() {
	
				private Set<BracketFilter> filters = new HashSet<>();
				@Override
				public void visitElement(IQueryFilter filter) {
					if (visitorException != null) return;
					try{
					String columnName = filterToColumnName.get(filter);
					if (columnName != null){
						deleteSql.append(" ( " + columnName + " is not null ) ");
					}else if (filter.getClass().equals(BooleanFilter.class)){
						deleteSql.append(  SqlGenerator.operatorToSql(((BooleanFilter)filter).getOperator()));
					}else if (filter.getClass().equals(NotFilter.class)){
						deleteSql.append( SqlGenerator.operatorToSql(Operator.NOT));
					}else if (filter.getClass().equals(BracketFilter.class)){
						if (filters.contains(filter)){
							deleteSql.append(SqlGenerator.operatorToSql(Operator.BRACKET_CLOSE));
						}else{
							deleteSql.append(SqlGenerator.operatorToSql(Operator.BRACKET_OPEN));
							filters.add((BracketFilter) filter);
						}
					}
					}catch (Exception ex){
						visitorException = ex;
					}
				}
				
			});
			deleteSql.append(")");		
			if (visitorException != null) throw visitorException;
			
			logString(deleteSql.toString());
			s.createSQLQuery(deleteSql.toString()).executeUpdate();
		}
		return obsTable;
	}
	
	
	
	private void addFilterColumn(DataModelFilter filter, String obsTable, String columnName) throws Exception{
		if (filter.getAttributeKey() == null){
			//only a category filter
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE " + obsTable );
			sql.append(" SET " + columnName + " = true ");
			sql.append(" WHERE observation_uuid IN ( ");	
			sql.append("SELECT a.observation_uuid FROM " + obsTable + " a ");
			sql.append(" JOIN smart.i_observation o on a.observation_uuid = o.uuid ");
			sql.append(" JOIN smart.dm_category c on c.uuid = o.category_uuid ");
			sql.append(" WHERE c.ca_uuid = :cauuid ");
			sql.append(" AND c.hkey >= :hkey1 and c.hkey < :hkey2 ) ");
			
			String hkey1 = filter.getCategoryKey();
			String hkey2 = filter.getCategoryKey().substring(0, filter.getCategoryKey().length() - 1) + "/";
			
			logString(UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()));
			logString(hkey1);
			logString(hkey2);
			
			SQLQuery query = s.createSQLQuery(sql.toString());
			query.setParameter("cauuid", SmartDB.getCurrentConservationArea().getUuid());
			query.setParameter("hkey1", hkey1);
			query.setParameter("hkey2", hkey2);
			logString(sql.toString());
			query.executeUpdate();
			
			return;
			
		}
		//category and perhaps an attribute filter
		
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE " + obsTable );
		sql.append(" SET " + columnName + " = true ");
		sql.append(" WHERE observation_uuid IN ( ");	
		sql.append("SELECT a.ibservatuib_uuid FROM " + obsTable + " a ");
		sql.append(" JOIN smart.i_observation o on a.location_uuid = l.location_uuid ");
		
		if (filter.getCategoryKey() != null){
			sql.append(" JOIN smart.dm_category c on c.uuid = o.category_uuid ");
			sql.append(" AND c.ca_uuid = :cauuid ");
			sql.append(" AND c.hkey >= :hkey1 and c.hkey < :hkey2 ");
		}
		
		Attribute attribute = (Attribute)s.createCriteria(Attribute.class)
				.add(Restrictions.eq("keyId", filter.getAttributeKey()))
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.uniqueResult();
		
		AttributeListItem li = null;
		AttributeTreeNode treenode = null;
		if (filter.getAttributeType() == Attribute.AttributeType.LIST){
			li = (AttributeListItem)s.createCriteria(AttributeListItem.class)
					.add(Restrictions.eq("keyId", filter.getKeyValue()))
					.add(Restrictions.eq("attribute", attribute))
					.uniqueResult();	
		}else if (filter.getAttributeType() == Attribute.AttributeType.TREE){
			treenode = (AttributeTreeNode)s.createCriteria(AttributeTreeNode.class)
					.add(Restrictions.eq("hkey", filter.getKeyValue()))
					.add(Restrictions.eq("attribute", attribute))
					.uniqueResult();	
		}
		sql.append(" JOIN smart.i_observation_attribute a on a.observation_uuid = a.uuid ");
		
		sql.append(" WHERE ");
		
		switch(filter.getAttributeType()){
		case BOOLEAN:
			sql.append(" v.double_value " + SqlGenerator.operatorToSql(Operator.GREATERTHAN) + " 0.5");
			break;
		case DATE:
			sql.append(" cast(v.string_value as date) " + SqlGenerator.operatorToSql(filter.getOperator()) + " cast(:value1 as date) and cast(value2 as date)");
			break;
		case LIST:
			sql.append(" v.list_item_uuid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value");
			break;
		case NUMERIC:
			sql.append(" v.double_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value");
			break;
		case TEXT:
			sql.append(" v.string_value " + SqlGenerator.operatorToSql(filter.getOperator()) + " :value");
			break;
		case TREE:
			sql.append(" v.tree_node_uuid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value");
			break;
		default:
			break;
		}
		sql.append(" )");
		
		
		SQLQuery query = s.createSQLQuery(sql.toString());
		
		if (filter.getCategoryKey() != null){
			String hkey1 = filter.getCategoryKey();
			String hkey2 = filter.getCategoryKey().substring(0, filter.getCategoryKey().length() - 1) + "/";
			logString(hkey1);
			logString(hkey2);
			logString(UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()));
			
			query.setParameter("hkey1", hkey1);
			query.setParameter("hkey2", hkey2);
			query.setParameter("cauuid", SmartDB.getCurrentConservationArea().getUuid());
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
			logString(UuidUtils.uuidToString(li.getUuid()));
			query.setParameter("value", li.getUuid());
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
	private void addFilterColumn(EntityFilter filter, String obsTable, String columnName){
		//todo: configure uuid
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE " + obsTable );
		sql.append(" SET " + columnName + " = true ");
		sql.append(" WHERE location_uuid IN ( ");
		sql.append(" SELECT location_uuid FROM smart.i_entity_location WHERE entity_uuid = :uuid )");
		
		logString(sql.toString());
		logString(UuidUtils.uuidToString(filter.getEntityUuid()));
		
		SQLQuery query = s.createSQLQuery(sql.toString());
		query.setParameter("uuid", filter.getEntityUuid());
		query.executeUpdate();
	}
	
	private void addFilterColumn(EntityTypeFilter filter, String obsTable, String columnName){
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE " + obsTable );
		sql.append(" SET " + columnName + " = true ");
		sql.append(" WHERE location_uuid IN ( ");
		sql.append(" SELECT location_uuid FROM smart.i_entity_location l ");
		sql.append(" JOIN smart.i_entity e on l.entity_uuid = e.uuid ");
		sql.append(" JOIN smart.i_entity_type t on e.entity_type_uuid = t.uuid and t.keyId = :typeKey )");
		
		logString(sql.toString());
		logString(filter.getTypeKey());
		
		SQLQuery query = s.createSQLQuery(sql.toString());
		query.setParameter("typeKey",  filter.getTypeKey());
		query.executeUpdate();
	}
	
	private void addFilterColumn(IntelAttributeFilter filter, String obsTable, String columnName) throws Exception{
		
		
		IntelAttribute attribute = (IntelAttribute)s.createCriteria(IntelAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.add(Restrictions.eq("keyId", filter.getAttributeKey()))
				.uniqueResult();
		
		if (attribute == null) throw new Exception(MessageFormat.format("Unable to find intelligence attribute with key {0}", filter.getAttributeKey()));
		IntelAttributeListItem listItem = null;
		if (filter.getAttributeType() == AttributeType.LIST && filter.getKeyValue() != null){
			listItem = (IntelAttributeListItem)s.createCriteria(IntelAttributeListItem.class)
					.add(Restrictions.eq("attribute", attribute))
					.add(Restrictions.eq("keyId", filter.getKeyValue()))
					.uniqueResult();	
			if (listItem == null) throw new Exception(MessageFormat.format("Unable to find intelligence list item attribute with key {0}", filter.getAttributeKey()));	
		}
		
		IntelEntityType type = null;
		if (filter.getEntityTypeKey() != null){
			type = (IntelEntityType)s.createCriteria(IntelEntityType.class)
					.add(Restrictions.eq("keyId", filter.getEntityTypeKey()))
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.uniqueResult();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE " + obsTable );
		sql.append(" SET " + columnName + " = true ");
		sql.append(" WHERE location_uuid IN ( ");	
		sql.append("SELECT a.location_uuid FROM " + obsTable + " a ");
		sql.append(" JOIN smart.i_entity_location l on a.location_uuid = l.location_uuid ");
		sql.append(" JOIN smart.i_entity_attribute_value v on v.entity_uuid = l.entity_uuid and v.attribute_uuid = :attributeUuid ");
		if (filter.getEntityTypeKey() != null){
			sql.append("JOIN smart.i_entity e on l.entity_uuid = e.uuid and e.entity_type_uuid = :entityTypeUuid ");
		}
		sql.append(" WHERE ");
		
		switch(filter.getAttributeType()){
		case BOOLEAN:
			sql.append(" v.double_value " + SqlGenerator.operatorToSql(Operator.GREATERTHAN) + " 0.5");
			break;
		case DATE:
			sql.append(" cast(v.string_value as date) " + SqlGenerator.operatorToSql(filter.getOperator()) + " cast(:value1 as date) and cast(value2 as date)");
			break;
		case LIST:
			sql.append(" v.list_item_uuid " + SqlGenerator.operatorToSql(Operator.EQUALS) + " :value");
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
		sql.append(" )");
		
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
			logString(UuidUtils.uuidToString(listItem.getUuid()));
			query.setParameter("value", listItem.getUuid());
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
	
	private void addFilterColumn(AreaFilter filter, String obsTable, String columnName){
		
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
		
		UUID areaUuid = (UUID) query.uniqueResult();
		
		sql = new StringBuilder();
		sql.append("UPDATE " + obsTable);
		sql.append(" SET " + columnName + " = true ");
		sql.append(" WHERE location_uuid IN ( ");
		sql.append(" SELECT uuid FROM smart.i_location l, smart.area_geometries a where a.uuid = :areauuid AND smart.intersection(a.geom, l.geometry ) ");
		sql.append(")");
		
		
		logString(sql.toString());
		logString(UuidUtils.uuidToString(areaUuid));
		query = s.createSQLQuery(sql.toString());
		query.setParameter("areauuid", areaUuid);
		query.executeUpdate();
	}
	
	
	private void logString(String string){
		SqlGenerator.logString(string);
	}
	
}
