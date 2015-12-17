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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class EntityWaypointQueryResult implements IDbTableResultSet {

	private PsqlEntityWaypointEngine engine;
	
	public EntityWaypointQueryResult(PsqlEntityWaypointEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		return c.createStatement().executeQuery("SELECT * FROM " + engine.getQueryDataTable());
	}
	
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		return column.getValueAsString(getValue(rs, column, c));
	}
	
	public Object getValue(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		String columnKey = column.getKey();
		
		if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_ID.getKey())){
			return rs.getString("ca_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.CA_NAME.getKey())){
			return rs.getString("ca_name");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_ID.getKey())){
			return rs.getInt("wp_id");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey())){
			return rs.getDate("wp_time");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
			return rs.getTime("wp_time");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
			return rs.getDouble("wp_x");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
			return rs.getDouble("wp_y");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION.getKey())){
			Object x = rs.getObject("wp_direction");
			if (x == null) return null;
			return (Double)x;
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_SOURCE.getKey())){
			return rs.getString("wp_source");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE.getKey())){
			Object x = rs.getObject("wp_distance");
			if (x == null) return null;
			return (Double)x;
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT.getKey())){
			return rs.getString("wp_comment");
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER.getKey())){
			return rs.getString("ob_observer");
		}
		return null;
	}

}
