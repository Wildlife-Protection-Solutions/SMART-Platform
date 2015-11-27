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
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class ErMissionTrackQueryResult implements IDbTableResultSet {

	private PsqlErMissionTrackEngine engine;
	
	public ErMissionTrackQueryResult(PsqlErMissionTrackEngine engine){
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
		}
		return null;
	}

}

