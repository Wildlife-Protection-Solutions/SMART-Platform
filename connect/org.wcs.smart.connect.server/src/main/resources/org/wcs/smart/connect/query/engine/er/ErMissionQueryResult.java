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
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Survey Mission Query Results
 * @author Emily
 *
 */
public class ErMissionQueryResult extends AbstractDbFeatureResultSet {

	private PsqlErMissionEngine engine;
	private WKBReader reader = new WKBReader();
	
	public ErMissionQueryResult(PsqlErMissionEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		StringBuilder sb = new StringBuilder();
		try(ResultSet rs = c.getMetaData().getColumns(null, null, engine.getQueryDataTable(), null)){
			while(rs.next()){
				sb.append("foo." + rs.getString(4)); //$NON-NLS-1$
				sb.append(","); //$NON-NLS-1$
			}
		}
		//TODO: figure out 3d gometries
		return c.createStatement().executeQuery("SELECT " + sb.toString() //$NON-NLS-1$
				+ "st_asbinary(st_force2d(st_collect(st_geomfromwkb(bar.geometry)))) as trackgeom FROM "  //$NON-NLS-1$
				+ engine.getQueryDataTable()
				+ " foo, " + engine.tableName(MissionTrack.class) + " bar, " //$NON-NLS-1$ //$NON-NLS-2$
				+ engine.tableName(MissionDay.class) + " c " //$NON-NLS-1$
				+ " WHERE bar.mission_day_uuid = c.uuid AND " //$NON-NLS-1$
				+ " c.mission_uuid = foo.mission_uuid" //$NON-NLS-1$
				+ " GROUP BY " //$NON-NLS-1$
				+ sb.toString().substring(0, sb.length() - 1)
				);
	}
	
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
		}else if (columnKey.startsWith(MissionPropertyQueryColumn.KEY_PREFIX)){
			String key = columnKey.split(":")[1]; //$NON-NLS-1$
			String columnName = engine.getMissionAttributeColumnName(key);
			if (rs.getMetaData().getColumnType(rs.findColumn(columnName)) == Types.VARCHAR){
				return rs.getString(columnName);
			}else{
				//assume double
				return rs.getDouble(columnName);
			}
		}
		return null;
	}
	
	@Override
	public String getGeometryType() {
		return MULTI_LINESTRING_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(ResultSet rs) throws Exception {
		byte[] b = rs.getBytes("trackgeom"); //$NON-NLS-1$
		if (b == null){
			return new GeometryCollection(new Geometry[]{}, gf);	
		}
		return reader.read(b);
	}

	@Override
	public String createId(ResultSet rs) throws Exception {
		return rs.getString("mission_id") + "." + System.nanoTime(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}

