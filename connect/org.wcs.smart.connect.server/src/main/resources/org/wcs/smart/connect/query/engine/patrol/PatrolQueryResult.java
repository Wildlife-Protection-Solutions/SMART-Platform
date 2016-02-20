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

import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Result set of patrol waypoint queries.
 * 
 * @author Emily
 *
 */
public class PatrolQueryResult extends AbstractDbFeatureResultSet {

	private PsqlPatrolEngine engine;
	
	public PatrolQueryResult(PsqlPatrolEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		return c.createStatement().executeQuery(engine.getDataQuery());
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
			return rs.getString("r_p_id"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_TYPE.getKey())){
			return org.wcs.smart.patrol.model.PatrolType.Type.valueOf(rs.getString("r_p_type")).getGuiName(engine.getLocale()); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_START_DATE.getKey())){
			return rs.getDate("r_p_start_date"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_END_DATE.getKey())){
			return rs.getDate("r_p_end_date"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_OBJETIVE.getKey())){
			return rs.getString("r_p_objective"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_ARMED.getKey())){
			return rs.getBoolean("r_p_is_armed"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_ID.getKey())){
			return rs.getString("r_pl_id"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_START_DATE.getKey())){
			return rs.getDate("r_pl_start_date"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_END_DATE.getKey())){
			return rs.getDate("r_pl_end_date"); //$NON-NLS-1$
			
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_STATION.getKey())){
			return rs.getString("p_station"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_TEAM.getKey())){
			return rs.getString("p_team"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_MANDATE.getKey())){
			return rs.getString("p_mandate"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_LEADER.getKey())){
			return rs.getString("p_leader"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.PATROL_LEG_PILOT.getKey())){
			return rs.getString("p_pilot"); //$NON-NLS-1$
		}else if (columnKey.equals(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE.getKey())){
			return rs.getString("p_transporttype"); //$NON-NLS-1$
		}else if (columnKey.equals("track")){ //$NON-NLS-1$
			return rs.getString("track"); //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public String getGeometryType() {
		return LINESTRING_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(ResultSet rs) throws Exception {
//		return gf.createPoint(new Coordinate(rs.getDouble("wp_x"), rs.getDouble("wp_y"))); //$NON-NLS-1$ //$NON-NLS-2$
		//TODO: do this
		return null;
	}

	@Override
	public String createId(ResultSet rs) throws Exception {
		return rs.getDouble("r_p_id") + "." + System.nanoTime(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
