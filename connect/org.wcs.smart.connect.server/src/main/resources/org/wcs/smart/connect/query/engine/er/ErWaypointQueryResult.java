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
package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.model.column.SamplingUnitAttributeQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Survey waypoint query results.
 * 
 * @author Emily
 *
 */
public class ErWaypointQueryResult extends AbstractDbFeatureResultSet {

	private PsqlErWaypointEngine engine;
	
	public ErWaypointQueryResult(PsqlErWaypointEngine engine){
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
		if (columnKey.equals(SurveyQueryColumn.FixedColumns.CA_ID.getKey())){
			return rs.getString("ca_id"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.CA_NAME.getKey())){
			return rs.getString("ca_name"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION.getKey())){
			return rs.getString("mission_id"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_START.getKey())){
			return rs.getDate("mission_startdate"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_END.getKey())){
			return rs.getDate("mission_enddate"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_LEADER.getKey())){
			return rs.getString("mission_leader"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN.getKey())){
			return rs.getString("surveydesign_name"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END.getKey())){
			return rs.getDate("surveydesign_enddate"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START.getKey())){
			return rs.getDate("surveydesign_startdate"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY.getKey())){
			return rs.getString("survey_id"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_START.getKey())){
			return rs.getDate("survey_startdate"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_END.getKey())){
			return rs.getDate("survey_enddate"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT.getKey())){
			return rs.getString("samplingunit_id"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_ID.getKey())){
			return rs.getInt("wp_id"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
			return rs.getDouble("wp_x"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
			return rs.getDouble("wp_y"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE.getKey())){
			return rs.getDate("wp_date"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
			return rs.getTime("wp_date"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION.getKey())){
			Object x = rs.getObject("wp_direction"); //$NON-NLS-1$
			if (x == null) return null;
			return (Double)x;
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE.getKey())){
			Object x = rs.getObject("wp_distance"); //$NON-NLS-1$
			if (x == null) return null;
			return (Double)x;
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT.getKey())){
			return rs.getString("wp_comment"); //$NON-NLS-1$
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_OBSERVER.getKey())){
			return rs.getString("ob_observer"); //$NON-NLS-1$
		}else if (columnKey.startsWith(MissionPropertyQueryColumn.KEY_PREFIX)){
			String key = columnKey.split(":")[1]; //$NON-NLS-1$
			String columnName = engine.getMissionAttributeColumnName(key);
			if (rs.getMetaData().getColumnType(rs.findColumn(columnName)) == Types.VARCHAR){
				return rs.getString(columnName);
			}else{
				//assume double
				Object x = rs.getObject(columnName);
				if (x == null) return null;
				return (Double)x;
			}
		}else if (columnKey.startsWith(SamplingUnitAttributeQueryColumn.KEY_PREFIX)){
			String key = columnKey.split(":")[1]; //$NON-NLS-1$
			String columnName = engine.getSamplingUnitAttributeColumnName(key);
			if (rs.getMetaData().getColumnType(rs.findColumn(columnName)) == Types.VARCHAR){
				return rs.getString(columnName);
			}else{
				//assume double
				Object x = rs.getObject(columnName);
				if (x == null) return null;
				return (Double)x;
			}
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

