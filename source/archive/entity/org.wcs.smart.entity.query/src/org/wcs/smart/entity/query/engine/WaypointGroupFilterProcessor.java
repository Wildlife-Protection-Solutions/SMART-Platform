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
package org.wcs.smart.entity.query.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class WaypointGroupFilterProcessor extends org.wcs.smart.observation.query.engine.WaypointGroupFilterProcessor {
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public WaypointGroupFilterProcessor(String tableName, AbstractQueryEngine engine, Query query){
		super(tableName, engine, query);
	}
	
	@Override
	protected DerbyFilterToSqlGenerator getSqlGenerator() {
		return EntityFilterToSqlGenerator.INSTANCE;
	}
	
	@Override
	protected boolean columnRequired(IFilter filter) {
		if (super.columnRequired(filter)) return true;
		return filter instanceof EntityAttributeFilter ;
	}
	
	@Override
	protected void processFilter(IFilter filter,  ConservationAreaFilter caFilter, FilterTable table, Connection c) throws SQLException {
		
		if (filter instanceof EntityAttributeFilter){
			processEntityAttributeFilter((EntityAttributeFilter)filter, caFilter, table, c);
		}else {
			super.processFilter(filter, caFilter, table, c);
		}
	}
	
	protected void processEntityAttributeFilter(EntityAttributeFilter lfilter, ConservationAreaFilter caFilter, FilterTable t, Connection c) throws SQLException {

		engine.clearParameters();

		createTemporaryFilterTable(t, c);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(t.tablename + " (" + t.primarykey + ", " + t.secondarykey + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" SELECT distinct a.wp_uuid, "); //$NON-NLS-1$ 
		sql.append(prefix(WaypointObservation.class));
		sql.append(".wp_group_uuid"); //$NON-NLS-1$

		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(waypointTable + " a "); //$NON-NLS-1$

		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservation.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservation.class));
		sql.append(".wp_group_uuid = "); //$NON-NLS-1$
		sql.append("a.wp_group_uuid "); //$NON-NLS-1$

		// get the dm model attribute repesenting the entity
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservationAttribute.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservation.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(Attribute.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Attribute.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(AttributeListItem.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationAttribute.class) + ".list_element_uuid = "); //$NON-NLS-1$
		sql.append(prefix(AttributeListItem.class) + ".uuid"); //$NON-NLS-1$

		sql.append(" join "); //$NON-NLS-1$
		sql.append(" ( SELECT "); //$NON-NLS-1$
		sql.append("el.keyid as entity_key_id , "); //$NON-NLS-1$

		EntityAttributeFilter ff = (EntityAttributeFilter) lfilter;

		if (ff.getAttributeType() == AttributeType.NUMERIC || ff.getAttributeType() == AttributeType.BOOLEAN) {
			sql.append(engine.tablePrefix(EntityAttributeValue.class));
			sql.append(".number_value"); //$NON-NLS-1$
		} else if (ff.getAttributeType() == AttributeType.TEXT || ff.getAttributeType() == AttributeType.DATE) {
			sql.append(engine.tablePrefix(EntityAttributeValue.class));
			sql.append(".string_value"); //$NON-NLS-1$
		} else if (ff.getAttributeType() == AttributeType.LIST) {
			sql.append(engine.tablePrefix(AttributeListItem.class));
			sql.append(".keyid"); //$NON-NLS-1$
		} else if (ff.getAttributeType() == AttributeType.TREE) {
			sql.append(engine.tablePrefix(AttributeTreeNode.class));
			sql.append(".hkey"); //$NON-NLS-1$
		} else {
			throw new RuntimeException(MessageFormat.format(Messages.WaypointFilterProcessor_AttributeTypeNotSupported,
					new Object[] { ff.getAttributeType() }));
		}
		sql.append(" as value FROM "); //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(EntityType.class));
		sql.append(" join "); //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(Entity.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityType.class) + ".uuid = " + engine.tablePrefix(Entity.class) //$NON-NLS-1$
				+ ".entity_type_uuid"); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(engine.tableName(AttributeListItem.class));
		sql.append(" el on el.uuid = "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(Entity.class));
		sql.append(".attribute_list_item_uuid"); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(EntityAttributeValue.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityAttributeValue.class) + ".entity_uuid = " + engine.tablePrefix(Entity.class) //$NON-NLS-1$
				+ ".uuid"); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(EntityAttribute.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityAttribute.class) + ".entity_type_uuid = " //$NON-NLS-1$
				+ engine.tablePrefix(EntityType.class) + ".uuid"); //$NON-NLS-1$
		sql.append(" and " + engine.tablePrefix(EntityAttribute.class) + ".uuid = " //$NON-NLS-1$ //$NON-NLS-2$
				+ engine.tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid"); //$NON-NLS-1$

		if (ff.getAttributeType() == AttributeType.LIST) {
			sql.append(" join "); //$NON-NLS-1$
			sql.append(engine.tableNamePrefix(AttributeListItem.class));
			sql.append(" ON "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(EntityAttributeValue.class) + ".list_element_uuid = "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(AttributeListItem.class) + ".uuid"); //$NON-NLS-1$
		} else if (ff.getAttributeType() == AttributeType.TREE) {
			sql.append(" join "); //$NON-NLS-1$
			sql.append(engine.tableNamePrefix(AttributeTreeNode.class));
			sql.append(" ON "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(EntityAttributeValue.class) + ".tree_node_uuid = "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(AttributeTreeNode.class) + ".uuid"); //$NON-NLS-1$
		}

		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityType.class));
		String p1 = engine.addParameterValue(ff.getEntityKey());
		sql.append(".keyId = " + p1); //$NON-NLS-1$
		sql.append(" AND "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityAttribute.class));
		p1 = engine.addParameterValue(ff.getEntityAttributeKey());
		sql.append(".keyId = " + p1); //$NON-NLS-1$
		sql.append(" AND "); //$NON-NLS-1$
		sql.append(getSqlGenerator().asSql(caFilter, engine.tablePrefix(EntityType.class), engine));
		sql.append(") foo "); //$NON-NLS-1$
		sql.append(" on foo.entity_key_id = "); //$NON-NLS-1$
		sql.append(prefix(AttributeListItem.class) + ".keyid"); //$NON-NLS-1$

		sql.append(" WHERE "); //$NON-NLS-1$
		EntityAttributeFilter efilter = (EntityAttributeFilter) lfilter;
		if (efilter.getAttributeType() == AttributeType.BOOLEAN) {
			sql.append(" (foo.value  > 0.5 ) "); //$NON-NLS-1$
		} else if (efilter.getAttributeType() == AttributeType.NUMERIC) {
			p1 = engine.addParameterValue((Double) efilter.getValue());
			sql.append(" ( foo.value " + getSqlGenerator().asSql(efilter.getOperator()) + " " + p1 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else if (efilter.getAttributeType() == AttributeType.TEXT) {
			String queryStr = ""; //$NON-NLS-1$
			String val = (String) efilter.getValue();
			if (efilter.getOperator() == Operator.STR_CONTAINS || efilter.getOperator() == Operator.STR_NOTCONTAINS) {
				p1 = engine.addParameterValue("%" + val + "%"); //$NON-NLS-1$ //$NON-NLS-2$
				queryStr = "( LOWER(foo.value) " + getSqlGenerator().asSql(efilter.getOperator()) + " LOWER(" //$NON-NLS-1$ //$NON-NLS-2$
						+ p1 + ") )"; //$NON-NLS-1$

			} else if (efilter.getOperator() == Operator.STR_EQUALS) {
				p1 = engine.addParameterValue(val);
				queryStr = "( LOWER(foo.value) " + getSqlGenerator().asSql(efilter.getOperator()) + " LOWER(" //$NON-NLS-1$ //$NON-NLS-2$
						+ p1 + ") )"; //$NON-NLS-1$
			}
			sql.append(queryStr);
		} else if (efilter.getAttributeType() == AttributeType.DATE) {
			String date1 = (String) efilter.getValue();
			String date2 = (String) efilter.getValue2();
			p1 = engine.addParameterValue(date1);
			String p2 = engine.addParameterValue(date2);

			sql.append("( foo.value is not null AND DATE(foo.value) "); //$NON-NLS-1$
			sql.append(getSqlGenerator().asSql(efilter.getOperator()));
			sql.append(" CAST(" + p1 + " as date) "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(getSqlGenerator().asSql(Operator.AND));
			sql.append(" CAST(" + p2 + " as date) )"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (efilter.getAttributeType() == AttributeType.LIST) {
			if (efilter.getValue().equals(AttributeFilter.ANY_OPTION_KEY)) {
				// any option
				sql.append("( foo.value is not null )"); //$NON-NLS-1$
			} else {
				p1 = engine.addParameterValue((String) efilter.getValue());
				sql.append("( foo.value " + getSqlGenerator().asSql(efilter.getOperator()) + " " + p1 + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} else if (efilter.getAttributeType() == AttributeType.TREE) {
			p1 = engine.addParameterValue((String) efilter.getValue());
			String p2 = engine.addParameterValue(
					((String) efilter.getValue()).substring(0, ((String) efilter.getValue()).length() - 1) + "/"); //$NON-NLS-1$
			sql.append("( foo.value >= " + p1 + " and foo.value < " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		QueryPlugIn.logSql(sql.toString());
		engine.parseQueryString(c, sql.toString()).executeUpdate();

	}
	
}
