/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.entity.query.model.columns.EntityAttributeQueryColumn;
import org.wcs.smart.entity.query.model.columns.FixedQueryColumn;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class EntityObservationQueryResult implements IDbTableResultSet {

	private PsqlEntityObservationEngine engine;
	
	public EntityObservationQueryResult(PsqlEntityObservationEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		return c.createStatement().executeQuery("SELECT * FROM " + engine.getQueryDataTable()); //$NON-NLS-1$
	}

	
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		return column.getValueAsString(getValue(rs, column, c));
	}
	
	public Object getValue(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		String columnKey = column.getKey();
		if (column instanceof FixedQueryColumn){
			if (column.equals(FixedQueryColumn.FixedColumns.CA_ID.getKey())){
				return rs.getString("ca_id"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_NAME.getKey())){
				return rs.getString("ca_name"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_ID.getKey())){
				return rs.getInt("wp_id"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey())){
				return rs.getDate("wp_time"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				return rs.getTime("wp_time"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
				return rs.getDouble("wp_x"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
				return rs.getDouble("wp_y"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION.getKey())){
				Object x = rs.getObject("wp_direction"); //$NON-NLS-1$
				if (x == null) return null;
				return (Double)x;
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_SOURCE.getKey())){
				return rs.getString("wp_source"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE.getKey())){
				Object x = rs.getObject("wp_distance"); //$NON-NLS-1$
				if (x == null) return null;
				return (Double)x;
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT.getKey())){
				return rs.getString("wp_comment"); //$NON-NLS-1$
			}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER.getKey())){
				return rs.getString("ob_observer"); //$NON-NLS-1$
			}
		}else if (column instanceof CategoryQueryColumn){
			if (columnKey.startsWith("category:")){ //$NON-NLS-1$
				String level = columnKey.split(":")[1]; //$NON-NLS-1$
				return rs.getString("category_"+level); //$NON-NLS-1$
			}
		}else if (column instanceof AttributeQueryColumn){
			if (columnKey.startsWith("attribute:")){ //$NON-NLS-1$
				UUID obuuid = (UUID) rs.getObject("ob_uuid"); //$NON-NLS-1$
				if (obuuid == null) return null;
				if (!obuuid.equals(obUuid)){
					attributeToValue = new HashMap<String, Object>();
					entityAttributeValues = new HashMap<String, Object>();
					obUuid = obuuid;
					attachObservations(obuuid, c);
					attachEntityAttributes(obuuid, c);
				}
				String key = columnKey.split(":")[1]; //$NON-NLS-1$
				return attributeToValue.get(key);
			}
		}else if (column instanceof EntityAttributeQueryColumn){
			String[] parts = columnKey.split(":"); //$NON-NLS-1$
			String ekey = parts[0];
			String eakey = parts[1];
			return entityAttributeValues.get(ekey +":" + eakey); //$NON-NLS-1$
		}
			
			
		return null;
	}
	
	
	private HashMap<String, Object> attributeToValue;
	private HashMap<String, Object> entityAttributeValues;
	private UUID obUuid;
	
	private void attachEntityAttributes(UUID obUuid, Connection c) throws SQLException{
		if (engine.getEntityTypes().size() > 0){
			//attach entities
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT r.ob_uuid, a.keyid as entitykey, ea.keyid as entityattributekey, eav.number_value, eav.string_value, rl.value as list_value, rt.value as tree_value "); //$NON-NLS-1$
				sql.append(" FROM "); //$NON-NLS-1$
				sql.append(engine.getQueryDataTable());
				sql.append(" r join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid join smart.entity_type a on a.dm_attribute_uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append(" join smart.entity e on e.attribute_list_item_uuid = wpoa.list_element_uuid "); //$NON-NLS-1$
				sql.append(" join smart.entity_attribute_value eav on eav.entity_uuid = e.uuid "); //$NON-NLS-1$
				sql.append(" join smart.entity_attribute ea on ea.uuid = eav.entity_attribute_uuid left join "); //$NON-NLS-1$
				sql.append(engine.getQueryDataTable());
				sql.append("_list rl on eav.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
				sql.append(engine.getQueryDataTable());
				sql.append("_tree rt on eav.tree_node_uuid = rt.UUID "); //$NON-NLS-1$	
			
				sql.append("WHERE r.ob_uuid = ? "); //$NON-NLS-1$
				
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("a.keyid in ("); //$NON-NLS-1$
				for (String et : engine.getEntityTypes()){
					sql.append("'" + et + "',");  //$NON-NLS-1$//$NON-NLS-2$
				}
				sql.deleteCharAt(sql.length() - 1);
				sql.append(")"); //$NON-NLS-1$
				
				PreparedStatement ps = c.prepareStatement(sql.toString());
				ps.setObject(1, obUuid);
				
				try(ResultSet rs = ps.executeQuery()){
					while(rs.next()){
//						UUID obuuid = (UUID) rs.getObject(1);
						String entityKey = rs.getString(2);
						String entityAttributeKey = rs.getString(3);
						Object value = null;
						if (rs.getObject(4) != null){
							//number value
							value = rs.getDouble(4);
						}else if (rs.getObject(5) != null){
							//string
							value = rs.getString(5);
						}else if (rs.getObject(6) != null){
							//list string
							value = rs.getString(6);
						}else if (rs.getObject(7) != null){
							//tree string
							value = rs.getString(7);
						}
						String key = entityKey +":" + entityAttributeKey; //$NON-NLS-1$
						entityAttributeValues.put(key, value);
					}
				}
				
			}
	}
	private void attachObservations(UUID obUuid, Connection c) throws SQLException {
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.p_ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable());
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid = ? "); //$NON-NLS-1$
		
		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		ps.setObject(1, obUuid);
		ResultSet rs = ps.executeQuery();
		while(rs.next()){
			String key = rs.getString(2);
			
			//double
			if (rs.getObject(3) != null){
				attributeToValue.put(key,  rs.getDouble(3));
				continue;
			}
			//string
			String v = rs.getString(4);
			if (v != null){
				attributeToValue.put(key, v);
				continue;
			}
			//list
			v = rs.getString(5);
			if (v != null){
				attributeToValue.put(key, v);
				continue;
			}
			//tree
			v = rs.getString(6);
			if (v != null){
				attributeToValue.put(key,  v);
				continue;
			}
		}

	}
}
