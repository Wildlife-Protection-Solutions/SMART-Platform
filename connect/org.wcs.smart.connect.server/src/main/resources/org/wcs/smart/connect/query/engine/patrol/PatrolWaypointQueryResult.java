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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.util.Date;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

/**
 * Result set of patrol waypoint queries.
 * 
 * @author Emily
 *
 */
public class PatrolWaypointQueryResult implements IDbTableResultSet {

	private PsqlWaypointEngine engine;
	
	public PatrolWaypointQueryResult(PsqlWaypointEngine engine){
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
		}
			
		return null;
	}
	
	
}
