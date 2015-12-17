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

import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Survey mission track query results.
 * 
 * @author Emily
 *
 */
public class ErMissionTrackQueryResult implements IDbTableResultSet {

	private PsqlErMissionTrackEngine engine;
	
	public ErMissionTrackQueryResult(PsqlErMissionTrackEngine engine){
		this.engine = engine;
	}
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException{
		return c.createStatement().executeQuery("SELECT * FROM " + engine.getQueryDataTable());
	}

	
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		return column.getValueAsString(getValue(rs, column, c));
	}
	
	@Override
	public Object getValue(ResultSet rs, QueryColumn column, Connection c) throws SQLException{
		String columnKey = column.getKey();
		
		if (columnKey.equals(SurveyQueryColumn.FixedColumns.CA_ID.getKey())){
			return rs.getString("ca_id");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.CA_NAME.getKey())){
			return rs.getString("ca_name");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION.getKey())){
			return rs.getString("mission_id");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_START.getKey())){
			return rs.getDate("mission_startdate");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_END.getKey())){
			return rs.getDate("mission_enddate");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_LEADER.getKey())){
			return rs.getString("mission_leader");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN.getKey())){
			return rs.getString("surveydesign_name");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END.getKey())){
			return rs.getDate("surveydesign_enddate");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START.getKey())){
			return rs.getDate("surveydesign_startdate");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY.getKey())){
			return rs.getString("survey_id");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_START.getKey())){
			return rs.getDate("survey_startdate");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_END.getKey())){
			return rs.getDate("survey_enddate");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_TRACKTYPE.getKey())){
			return TrackType.valueOf(rs.getString("mission_tracktype")).getGuiName(engine.getLocale());
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_TRACKDATE.getKey())){
			return rs.getDate("missionday_date");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_TRACKID.getKey())){
			return rs.getString("mission_trackid");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.MISSION_TRACKLENGTH.getKey())){
			return rs.getDouble("mission_tracklength");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT.getKey())){
			return rs.getString("samplingunit_id");
		}else if (columnKey.startsWith(MissionPropertyQueryColumn.KEY_PREFIX)){
			String key = columnKey.split(":")[1];
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

}

