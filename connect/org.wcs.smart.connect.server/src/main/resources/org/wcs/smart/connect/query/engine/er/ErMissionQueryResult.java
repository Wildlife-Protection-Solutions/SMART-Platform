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
import java.sql.Time;
import java.sql.Types;
import java.text.DateFormat;
import java.util.Date;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

/**
 * Survey Mission Query Results
 * @author Emily
 *
 */
public class ErMissionQueryResult implements IDbTableResultSet {

	private PsqlErMissionEngine engine;
	
	public ErMissionQueryResult(PsqlErMissionEngine engine){
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
		}else if (columnKey.startsWith("missionatt")){
			String key = columnKey.split(":")[1];
			if (rs.getMetaData().getColumnType(rs.findColumn("ma_"+key)) == Types.VARCHAR){
				return rs.getString("ma_" + key);
			}else{
				//assume double
				return rs.getDouble("ma_"+ key);
			}
		}
		return null;
	}
	
	
}

