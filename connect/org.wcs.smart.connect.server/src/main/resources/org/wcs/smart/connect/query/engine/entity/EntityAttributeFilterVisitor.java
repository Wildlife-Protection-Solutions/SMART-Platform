/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

/**
 * Process entity attribute filters but creating a temporary table
 * for matching entities for the given filter.
 * 
 * <p>This filter makes a collection of temporary tables which
 * should be dropped by calling the dropTemporaryTables function</p>
 * @author Emily
 *
 */
public class EntityAttributeFilterVisitor  implements IFilterVisitor{

	private final Logger logger = Logger.getLogger(EntityAttributeFilterVisitor.class.getName());
	
	private HashSet<String> addedTableNames = new HashSet<String>();
	private HashSet<String> toDrop = new HashSet<String>();
	
	private StringBuilder sql;
	private AbstractQueryEngine engine;
	
	private ConservationAreaFilter caFilter;
	private Connection c;
	
	/**
	 * Creates a new visitor
	 * @param sql sql to append to
	 * @param engine query engine
	 * @param usedTables list of tables already added to sql (Track.class)
	 * only needs to be added once
	 */
	public EntityAttributeFilterVisitor(StringBuilder sql, 
			AbstractQueryEngine engine, ConservationAreaFilter caFilter, Connection c){
		this.sql = sql;
		this.engine = engine;
	
		this.caFilter = caFilter;
		this.c = c;
	}
	
	
	@Override
	public void visit(IFilter filter) {
		if (filter instanceof EntityAttributeFilter){
			EntityAttributeFilter ff = (EntityAttributeFilter)filter;
			
			String tableName = ff.getEntityKey() + "_" + ff.getEntityAttributeKey(); //$NON-NLS-1$
			
			if (!addedTableNames.contains(tableName)) {
				addedTableNames.add(tableName);

				//create temporary table that contains all the matching
				//entity attribute values 
				String tmpTable = engine.createTempTableName();
				toDrop.add(tmpTable);
				
				StringBuilder tmp = new StringBuilder();
				tmp.append("CREATE TABLE "); //$NON-NLS-1$
				tmp.append(tmpTable);
				tmp.append("(entity_keyid char(128), value "); //$NON-NLS-1$
				if (ff.getAttributeType() == AttributeType.NUMERIC || 
					ff.getAttributeType() == AttributeType.BOOLEAN){
					tmp.append("double precision"); //$NON-NLS-1$
				}else if (ff.getAttributeType() == AttributeType.TEXT ||
						ff.getAttributeType() == AttributeType.DATE){
					tmp.append("varchar(1024)"); //$NON-NLS-1$
				}else if (ff.getAttributeType() == AttributeType.LIST ||
						ff.getAttributeType() == AttributeType.TREE){
					tmp.append("varchar(128)"); //$NON-NLS-1$
				}else{
					throw new RuntimeException(MessageFormat.format("Attribute type {0} not supported.", new Object[]{ff.getAttributeType()}));
				}
				tmp.append(")"); //$NON-NLS-1$
				logger.finest(tmp.toString());
				try{
					c.createStatement().execute(tmp.toString());
				}catch (Exception ex){
					throw new RuntimeException(ex);
				}
				
				//temp engine to support query parameters for query
				AbstractQueryEngine tempEngine = new AbstractQueryEngine() {
					@Override
					public boolean canExecute(String arg0) {
						return false;
					}
					@Override
					public IQueryResult executeQuery(Query arg0,
							HashMap<String, Object> arg1) throws SQLException {
						return null;
					}
					@Override
					public void cleanUp(Session session) {
						
					}
					@Override
					public String getTemporaryTableSelectClause(
							boolean includeObservations) {
						return null;
					}
					@Override
					public String getTemporaryTableCreateClause(String tableName) {
						return null;
					}
					@Override
					public String getSurveySamplingUnitJoinFieldName() {
						return null;
					}
				};
				tmp = new StringBuilder();
				tmp.append("INSERT INTO "); //$NON-NLS-1$
				tmp.append(tmpTable);
				tmp.append(" SELECT "); //$NON-NLS-1$
				tmp.append("el.keyid, "); //$NON-NLS-1$
			
				if (ff.getAttributeType() == AttributeType.NUMERIC || 
						ff.getAttributeType() == AttributeType.BOOLEAN){
						tmp.append(engine.tablePrefix(EntityAttributeValue.class));
						tmp.append(".number_value"); //$NON-NLS-1$
				}else if (ff.getAttributeType() == AttributeType.TEXT ||
						ff.getAttributeType() == AttributeType.DATE){
					tmp.append(engine.tablePrefix(EntityAttributeValue.class));
					tmp.append(".string_value"); //$NON-NLS-1$
				}else if (ff.getAttributeType() == AttributeType.LIST){
					tmp.append(engine.tablePrefix(AttributeListItem.class));
					tmp.append(".keyid"); //$NON-NLS-1$
				}else if(ff.getAttributeType() == AttributeType.TREE){
					tmp.append(engine.tablePrefix(AttributeTreeNode.class));
					tmp.append(".hkey"); //$NON-NLS-1$
				}else{
					throw new RuntimeException(MessageFormat.format("Attribute type {0} not supported.", new Object[]{ff.getAttributeType()})); //$NON-NLS-1$
				}
				tmp.append(" FROM "); //$NON-NLS-1$
				tmp.append(engine.tableNamePrefix(EntityType.class));
				tmp.append(" join "); //$NON-NLS-1$
				tmp.append(engine.tableNamePrefix(Entity.class));
				tmp.append(" on "); //$NON-NLS-1$
				tmp.append(engine.tablePrefix(EntityType.class) + ".uuid = " + engine.tablePrefix(Entity.class) + ".entity_type_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				tmp.append(" join "); //$NON-NLS-1$
				tmp.append(engine.tableName(AttributeListItem.class));
				tmp.append(" el on el.uuid = "); //$NON-NLS-1$
				tmp.append(engine.tablePrefix(Entity.class));
				tmp.append(".attribute_list_item_uuid"); //$NON-NLS-1$
				tmp.append(" join "); //$NON-NLS-1$
				tmp.append(engine.tableNamePrefix(EntityAttributeValue.class));
				tmp.append(" on "); //$NON-NLS-1$
				tmp.append(engine.tablePrefix(EntityAttributeValue.class) + ".entity_uuid = " + engine.tablePrefix(Entity.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				tmp.append(" join "); //$NON-NLS-1$
				tmp.append(engine.tableNamePrefix(EntityAttribute.class));
				tmp.append(" on "); //$NON-NLS-1$
				tmp.append(engine.tablePrefix(EntityAttribute.class) + ".entity_type_uuid = " + engine.tablePrefix(EntityType.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				tmp.append(" and " + engine.tablePrefix(EntityAttribute.class) + ".uuid = " + engine.tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				if (ff.getAttributeType() == AttributeType.LIST){
					tmp.append(" join "); //$NON-NLS-1$
					tmp.append(engine.tableNamePrefix(AttributeListItem.class));
					tmp.append(" ON "); //$NON-NLS-1$
					tmp.append(engine.tablePrefix(EntityAttributeValue.class) + ".list_element_uuid = "); //$NON-NLS-1$
					tmp.append(engine.tablePrefix(AttributeListItem.class) + ".uuid"); //$NON-NLS-1$
				}else if(ff.getAttributeType() == AttributeType.TREE){
					tmp.append(" join "); //$NON-NLS-1$
					tmp.append(engine.tableNamePrefix(AttributeTreeNode.class));
					tmp.append(" ON "); //$NON-NLS-1$
					tmp.append(engine.tablePrefix(EntityAttributeValue.class) + ".tree_node_uuid = "); //$NON-NLS-1$
					tmp.append(engine.tablePrefix(AttributeTreeNode.class) + ".uuid"); //$NON-NLS-1$
				}
				
				tmp.append(" WHERE "); //$NON-NLS-1$
				tmp.append(engine.tablePrefix(EntityType.class));
				String p1 = tempEngine.addParameterValue(ff.getEntityKey());
				tmp.append(".keyId = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$ 
				tmp.append(" AND "); //$NON-NLS-1$
				tmp.append(engine.tablePrefix(EntityAttribute.class));
				p1 = tempEngine.addParameterValue(ff.getEntityAttributeKey());
				tmp.append(".keyId = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
				
				tmp.append(" AND "); //$NON-NLS-1$
				try{
					tmp.append(PsqlFilterToSqlGenerator.INSTANCE.asSql(caFilter, engine.tablePrefix(EntityType.class), tempEngine));
					logger.finest(tmp.toString());
					tempEngine.parseQueryString(c, tmp.toString()).executeUpdate();
				}catch (Exception ex){
					throw new RuntimeException(ex);
				}
				
				
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(tmpTable);
				sql.append(" " + tableName); //$NON-NLS-1$
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tableName + ".entity_keyid = "); //$NON-NLS-1$
				sql.append("qa." + engine.getEntityDmAttributeKey(ff.getEntityKey(), caFilter, c)); //$NON-NLS-1$

			}
		}
	}
	
	/**
	 * Drops any temporary tables creating while
	 * processing entity attribute filters.
	 * 
	 * @param c
	 * @param engine
	 * @throws SQLException
	 */
	public void dropTemporaryTables(Connection c, AbstractQueryEngine engine) throws SQLException {
		for (String t : toDrop){
			engine.dropTable(c,t);
		}
	}

}
