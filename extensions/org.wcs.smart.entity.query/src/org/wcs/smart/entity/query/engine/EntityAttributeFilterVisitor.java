package org.wcs.smart.entity.query.engine;

import java.sql.Connection;
import java.text.MessageFormat;
import java.util.HashSet;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

public class EntityAttributeFilterVisitor  implements IFilterVisitor{

	private HashSet<String> addedTableNames = new HashSet<String>();
	private StringBuilder sql;
	private DerbyEntityQueryEngine engine;
	
	private ConservationAreaFilter catFilter;
	private Connection c;
	private String observationTable;
	
	/**
	 * Creates a new visitor
	 * @param sql sql to append to
	 * @param engine query engine
	 * @param usedTables list of tables already added to sql (Track.class)
	 * only needs to be added once
	 */
	public EntityAttributeFilterVisitor(StringBuilder sql, 
			DerbyEntityQueryEngine engine, ConservationAreaFilter query, Connection c, String observationTable){
		this.sql = sql;
		this.engine = engine;
	
		this.catFilter = query;
		this.c = c;
		this.observationTable = observationTable;
	}
	
	
	@Override
	public void visit(IFilter filter) {
		if (filter instanceof EntityAttributeFilter){
			EntityAttributeFilter ff = (EntityAttributeFilter)filter;
			
			String tableName = ff.getEntityKey() + "_" + ff.getEntityAttributeKey();
			
			if (!addedTableNames.contains(tableName)) {
				addedTableNames.add(tableName);

				//create temporary table
				String tmpTable = engine.createTempTableName();
				StringBuilder tmp = new StringBuilder();
				tmp.append("CREATE TABLE ");
				tmp.append(tmpTable);
				tmp.append("(entity_keyid char(128), value ");
				if (ff.getAttributeType() == AttributeType.NUMERIC || 
					ff.getAttributeType() == AttributeType.BOOLEAN){
					tmp.append("double");
				}else if (ff.getAttributeType() == AttributeType.TEXT ||
						ff.getAttributeType() == AttributeType.DATE){
					tmp.append("varchar(1024)");
				}else if (ff.getAttributeType() == AttributeType.LIST ||
						ff.getAttributeType() == AttributeType.TREE){
//					tmp.append("char(16) for bit data");
					tmp.append("varchar(128)");
				}else{
					throw new RuntimeException(MessageFormat.format("Attribute type {0} not supported.", new Object[]{ff.getAttributeType()}));
				}
				tmp.append(")");
				QueryPlugIn.logSql(tmp.toString());
				try{
					c.createStatement().execute(tmp.toString());
				}catch (Exception ex){
					throw new RuntimeException(ex);
				}
				
				
				tmp = new StringBuilder();
				tmp.append("INSERT INTO ");
				tmp.append(tmpTable);
				tmp.append(" SELECT ");
				tmp.append("el.keyid, ");
			
				if (ff.getAttributeType() == AttributeType.NUMERIC || 
						ff.getAttributeType() == AttributeType.BOOLEAN){
						tmp.append(engine.tablePrefix(EntityAttributeValue.class));
						tmp.append(".number_value");
				}else if (ff.getAttributeType() == AttributeType.TEXT ||
						ff.getAttributeType() == AttributeType.DATE){
					tmp.append(engine.tablePrefix(EntityAttributeValue.class));
					tmp.append(".string_value");
				}else if (ff.getAttributeType() == AttributeType.LIST){
					tmp.append(engine.tablePrefix(AttributeListItem.class));
					tmp.append(".keyid");
				}else if(ff.getAttributeType() == AttributeType.TREE){
					tmp.append(engine.tablePrefix(AttributeTreeNode.class));
					tmp.append(".hkey");
				}else{
					throw new RuntimeException(MessageFormat.format("Attribute type {0} not supported.", new Object[]{ff.getAttributeType()}));
				}
				tmp.append(" FROM ");
				tmp.append(engine.tableNamePrefix(EntityType.class));
				tmp.append(" join ");
				tmp.append(engine.tableNamePrefix(Entity.class));
				tmp.append(" on ");
				tmp.append(engine.tablePrefix(EntityType.class) + ".uuid = " + engine.tablePrefix(Entity.class) + ".entity_type_uuid");
				tmp.append(" join ");
				tmp.append(engine.tableName(AttributeListItem.class));
				tmp.append(" el on el.uuid = ");
				tmp.append(engine.tablePrefix(Entity.class));
				tmp.append(".attribute_list_item_uuid");
				tmp.append(" join ");
				tmp.append(engine.tableNamePrefix(EntityAttributeValue.class));
				tmp.append(" on ");
				tmp.append(engine.tablePrefix(EntityAttributeValue.class) + ".entity_uuid = " + engine.tablePrefix(Entity.class) + ".uuid");
				tmp.append(" join ");
				tmp.append(engine.tableNamePrefix(EntityAttribute.class));
				tmp.append(" on ");
				tmp.append(engine.tablePrefix(EntityAttribute.class) + ".entity_type_uuid = " + engine.tablePrefix(EntityType.class) + ".uuid");
				tmp.append(" and " + engine.tablePrefix(EntityAttribute.class) + ".uuid = " + engine.tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid");
				
				if (ff.getAttributeType() == AttributeType.LIST){
					tmp.append(" join ");
					tmp.append(engine.tableNamePrefix(AttributeListItem.class));
					tmp.append(" ON ");
					tmp.append(engine.tablePrefix(EntityAttributeValue.class) + ".list_element_uuid = ");
					tmp.append(engine.tablePrefix(AttributeListItem.class) + ".uuid");
				}else if(ff.getAttributeType() == AttributeType.TREE){
					tmp.append(" join ");
					tmp.append(engine.tableNamePrefix(AttributeTreeNode.class));
					tmp.append(" ON ");
					tmp.append(engine.tablePrefix(EntityAttributeValue.class) + ".tree_node_uuid = ");
					tmp.append(engine.tablePrefix(AttributeTreeNode.class) + ".uuid");
				}
				
				tmp.append(" WHERE ");
				tmp.append(engine.tablePrefix(EntityType.class));
				tmp.append(".keyId = '" + ff.getEntityKey() + "'");
				tmp.append(" AND ");
				tmp.append(engine.tablePrefix(EntityAttribute.class));
				tmp.append(".keyId = '" + ff.getEntityAttributeKey() + "'");
				tmp.append(" AND ");
				try{
					tmp.append(EntityFilterToSqlGenerator.INSTANCE.asSql(catFilter, engine.tablePrefix(EntityType.class)));
					
					QueryPlugIn.logSql(tmp.toString());
					c.createStatement().execute(tmp.toString());
				}catch (Exception ex){
					throw new RuntimeException(ex);
				}
				
				
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(tmpTable);
				sql.append(" " + tableName);
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tableName + ".entity_keyid = ");
				sql.append("qa." + ff.getEntityDmAttributeKey(c, engine));

			}
		}
	}

}
