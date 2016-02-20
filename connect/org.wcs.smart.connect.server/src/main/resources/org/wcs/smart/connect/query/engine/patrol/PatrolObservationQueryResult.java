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
import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Observation query result for patrol queries.
 * 
 * @author Emily
 *
 */
public class PatrolObservationQueryResult extends AbstractDbFeatureResultSet {

	private PsqlPatrolObservationEngine engine;
	
	public PatrolObservationQueryResult(PsqlPatrolObservationEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		return c.createStatement().executeQuery("SELECT * FROM " + engine.getQueryDataTable()); //$NON-NLS-1$
	}

	@Override
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		return column.getValueAsString(getValue(rs, column, c));
	}
	
	@Override
	public Object getValue(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		String columnKey = column.getKey();
		if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_ID.getKey())){
			return rs.getString("ca_id"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_NAME.getKey())){
			return rs.getString("ca_name"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_ID.getKey())){
			return rs.getString("p_id"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_TYPE.getKey())){
			return org.wcs.smart.patrol.model.PatrolType.Type.valueOf(rs.getString("p_type")).getGuiName(engine.getLocale()); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_START_DATE.getKey())){
			return rs.getDate("p_startdate"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_END_DATE.getKey())){
			return rs.getDate("p_enddate"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_STATION.getKey())){
			return rs.getString("p_station"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_TEAM.getKey())){
			return rs.getString("p_team"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_OBJETIVE.getKey())){
			return rs.getString("p_objective"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_MANDATE.getKey())){
			return rs.getString("p_mandate"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_ARMED.getKey())){
			return rs.getBoolean("p_armed"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_ID.getKey())){
			return rs.getString("p_legid"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_LEADER.getKey())){
			return rs.getString("p_leader"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_PILOT.getKey())){
			return rs.getString("p_pilot"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE.getKey())){
			return rs.getString("p_transporttype"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_ID.getKey())){
			return rs.getInt("wp_id"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey())){
			return rs.getDate("wp_date"); //$NON-NLS-1$
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
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE.getKey())){
			Object x = rs.getObject("wp_distance"); //$NON-NLS-1$
			if (x == null) return null;
			return (Double)x;
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT.getKey())){
			return rs.getString("wp_comment"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER.getKey())){
			return rs.getString("ob_observer"); //$NON-NLS-1$
		}else if (columnKey.startsWith("category:")){ //$NON-NLS-1$
			String level = columnKey.split(":")[1]; //$NON-NLS-1$
			return rs.getString("category_"+level); //$NON-NLS-1$
		}else if (columnKey.startsWith("attribute:")){ //$NON-NLS-1$
			UUID obuuid = (UUID) rs.getObject("ob_uuid"); //$NON-NLS-1$
			if (obuuid == null) return null;
			if (!obuuid.equals(obUuid)){
				attributeToValue = new HashMap<String, Object>();
				obUuid = obuuid;
				attachObservations(obuuid, c);
			}
			String key = columnKey.split(":")[1]; //$NON-NLS-1$
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
	
	@Override
	public String getGeometryType() {
		return POINT_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(ResultSet rs) throws Exception {
		return gf.createPoint(new Coordinate(rs.getDouble("wp_x"), rs.getDouble("wp_y"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String createId(ResultSet rs) throws Exception {
		return rs.getDouble("wp_id") + "." + System.nanoTime(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
