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
package org.wcs.smart.connect.query.engine.observation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class ObsWaypointQueryResult extends AbstractDbFeatureResultSet {

	private PsqlObsWaypointEngine engine;
	
	public ObsWaypointQueryResult(PsqlObsWaypointEngine engine){
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
		return null;
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
