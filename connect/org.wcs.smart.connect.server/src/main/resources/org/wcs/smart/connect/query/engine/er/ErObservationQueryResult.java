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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.model.column.SamplingUnitAttributeQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

/**
 * Survey observation query results.
 * 
 * @author Emily
 *
 */
public class ErObservationQueryResult implements IDbTableResultSet {

	private PsqlErObservationEngine engine;
	private HashMap<String, Object> attributeToValue;
	private UUID obUuid;
	
	public ErObservationQueryResult(PsqlErObservationEngine engine){
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
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT.getKey())){
			return rs.getString("samplingunit_id");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_ID.getKey())){
			return rs.getInt("wp_id");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
			return rs.getDouble("wp_x");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
			return rs.getDouble("wp_y");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE.getKey())){
			return rs.getDate("wp_date");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
			return rs.getTime("wp_date");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION.getKey())){
			Object x = rs.getObject("wp_direction");
			if (x == null) return null;
			return (Double)x;
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE.getKey())){
			Object x = rs.getObject("wp_distance");
			if (x == null) return null;
			return (Double)x;
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT.getKey())){
			return rs.getString("wp_comment");
		}else if (columnKey.equals(SurveyQueryColumn.FixedColumns.WAYPOINT_OBSERVER.getKey())){
			return rs.getString("ob_observer");
		}else if (columnKey.startsWith("category:")){
			String level = columnKey.split(":")[1];
			return rs.getString("category_"+level);
		}else if (columnKey.startsWith("attribute:")){
			UUID obuuid = (UUID) rs.getObject("ob_uuid");
			if (obuuid == null) return null;
			if (!obuuid.equals(obUuid)){
				attributeToValue = new HashMap<String, Object>();
				obUuid = obuuid;
				attachObservations(obuuid, c);
			}
			String key = columnKey.split(":")[1];
			return attributeToValue.get(key);
		}else if (columnKey.startsWith(MissionPropertyQueryColumn.KEY_PREFIX)){
			String key = columnKey.split(":")[1];
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
			String key = columnKey.split(":")[1];
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
	
	private void attachObservations(UUID obUuid, Connection c) throws SQLException {
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable());
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid = ? "); //$NON-NLS-1$
		
		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		ps.setObject(1, obUuid);
		ResultSet rs = ps.executeQuery();
		while(rs.next()){
			String key = rs.getString(2);
			
			//double
			if (rs.getObject(3) != null){
				attributeToValue.put(key,  rs.getDouble(3));
				continue;
			}
			//string
			String v = rs.getString(4);
			if (v != null){
				attributeToValue.put(key, v);
				continue;
			}
			//list
			v = rs.getString(5);
			if (v != null){
				attributeToValue.put(key, v);
				continue;
			}
			//tree
			v = rs.getString(6);
			if (v != null){
				attributeToValue.put(key,  v);
				continue;
			}
		}
	}
}

