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
package org.wcs.smart.connect.query.engine.patrol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

/**
 * Observation query result for patrol queries.
 * 
 * @author Emily
 *
 */
public class PatrolObservationQueryResult implements IDbTableResultSet {

	private PsqlObservationEngine engine;
	
	public PatrolObservationQueryResult(PsqlObservationEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		return c.createStatement().executeQuery("SELECT * FROM " + engine.getQueryDataTable());
	}

	
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		Object v = getValue(rs, column.getKey(), c);
		if (v == null) return "";
		if (v instanceof String){
			return (String)v;
		}
		if(v instanceof Time){
			return DateFormat.getTimeInstance(DateFormat.DEFAULT, engine.getLocale()).format((Time)v);
		}else if (v instanceof Date){
			return DateFormat.getDateInstance(DateFormat.DEFAULT, engine.getLocale()).format((Date)v);
		}else if (v instanceof Double){
			Double d = (Double)v;
			if (column.getType() == ColumnType.BOOLEAN){
				if (d < 0.5) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE,engine.getLocale());
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, engine.getLocale());
			}
			return Double.toString((Double)v);
		}
		return v.toString();
	}
	
	public Object getValue(ResultSet rs, String columnKey, Connection c) throws SQLException{
		
		if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_ID.getKey())){
			return rs.getString("ca_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_NAME.getKey())){
			return rs.getString("ca_name");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_ID.getKey())){
			return rs.getString("p_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_TYPE.getKey())){
			return org.wcs.smart.patrol.model.PatrolType.Type.valueOf(rs.getString("p_type")).getGuiName(engine.getLocale());
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_START_DATE.getKey())){
			return rs.getDate("p_startdate");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_END_DATE.getKey())){
			return rs.getDate("p_enddate");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_STATION.getKey())){
			return rs.getString("p_station");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_TEAM.getKey())){
			return rs.getString("p_team");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_OBJETIVE.getKey())){
			return rs.getString("p_objective");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_MANDATE.getKey())){
			return rs.getString("p_mandate");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_ARMED.getKey())){
			return rs.getBoolean("p_armed");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_ID.getKey())){
			return rs.getString("p_legid");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_LEADER.getKey())){
			return rs.getString("p_leader");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_PILOT.getKey())){
			return rs.getString("p_pilot");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE.getKey())){
			return rs.getString("p_transporttype");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_ID.getKey())){
			return rs.getString("wp_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey())){
			return rs.getDate("wp_date");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
			return rs.getTime("wp_time");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
			return rs.getDouble("wp_x");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
			return rs.getDouble("wp_y");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION.getKey())){
			return rs.getDouble("wp_direction");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE.getKey())){
			return rs.getDouble("wp_distance");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT.getKey())){
			return rs.getString("wp_comment");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER.getKey())){
			return rs.getString("ob_observer");
		}else if (columnKey.startsWith("category:")){
			String level = columnKey.split(":")[1];
			return rs.getString("category_"+level);
		}else if (columnKey.startsWith("attribute:")){
			UUID obuuid = (UUID) rs.getObject("ob_uuid");
			if (obuuid == null) return null;
			if (!obuuid.equals(obUuid)){
				attributeToValue = new HashMap<String, Object>();
				obUuid = obuuid;
				attachObservations(obuuid, c);
			}
			String key = columnKey.split(":")[1];
			return attributeToValue.get(key);
		}
			
			
		return null;
	}
	
	
	private HashMap<String, Object> attributeToValue;
	private UUID obUuid;
	
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
