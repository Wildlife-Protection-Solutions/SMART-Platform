package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.util.Date;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class ErWaypointQueryResult implements IDbTableResultSet {

	private PsqlErWaypointEngine engine;
	
	public ErWaypointQueryResult(PsqlErWaypointEngine engine){
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
			return rs.getDate("survey_id");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_START.getKey())){
			return rs.getDate("survey_startdate");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SURVEY_END.getKey())){
			return rs.getDate("survey_enddate");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT.getKey())){
			return rs.getDate("samplingunit_id");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_ID.getKey())){
			return rs.getInt("wp_id");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
			return rs.getDouble("wp_x");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
			return rs.getDouble("wp_y");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE.getKey())){
			return rs.getDate("wp_date");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION.getKey())){
			return rs.getDouble("wp_direction");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE.getKey())){
			return rs.getDouble("wp_distance");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT.getKey())){
			return rs.getDouble("wp_comment");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_OBSERVER.getKey())){
			return rs.getDouble("ob_observer");
		}
			
		//TODO: add mission attributes
		return null;
	}
}

