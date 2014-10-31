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
package org.wcs.smart.er.query.engine;


import java.sql.SQLException;
import java.text.MessageFormat;

import org.apache.commons.lang.StringEscapeUtils;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionFilter;
import org.wcs.smart.er.query.filter.MissionMemberFilter;
import org.wcs.smart.er.query.filter.MissionPropertyFilter;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.filter.MissionTrackDateField;
import org.wcs.smart.er.query.filter.SamplingUnitAttributeFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter.Source;
import org.wcs.smart.er.query.filter.SamplingUnitFilter.Type;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.filter.SurveyFilter;
import org.wcs.smart.er.query.filter.TrackTypeFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.AreaFilter.AreaFilterGeometryType;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.CategoryAttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.util.SmartUtils;

/**
 * Converts filters to sql for the Derby query engine.
 * 
 * @author Emily
 *
 */
public class SurveyFilterSqlGenerator extends DerbyFilterToSqlGenerator{

	
	public static final SurveyFilterSqlGenerator INSTANCE = new SurveyFilterSqlGenerator();
	
	
	private SurveyFilterSqlGenerator(){
		
	}
	
	/**
	 * converts the given filter to sql
	 * 
	 * @param filter the filter to convert
	 * @param engine the query engine
	 * @return sql string
	 * @throws SQLException
	 */
	@Override
	public String toSql(IFilter filter, IQueryEngine engine) throws SQLException{
		if (filter instanceof SurveyFilter){
			return asSql((SurveyFilter)filter, engine);
		}else if (filter instanceof MissionFilter){
			return asSql((MissionFilter)filter, engine);
		}else if (filter instanceof MissionPropertyFilter){
			return asSql((MissionPropertyFilter)filter, engine);
		}else if (filter instanceof ConservationAreaFilter){
			return asSql((ConservationAreaFilter)filter, engine.tablePrefix(SurveyDesign.class));
		}else if (filter instanceof SamplingUnitFilter){
			return asSql((SamplingUnitFilter)filter, engine);
		}else if (filter instanceof SamplingUnitAttributeFilter){
			return asSql((SamplingUnitAttributeFilter)filter, engine);
		}else if (filter instanceof SurveyDesignFilter){
			return asSql((SurveyDesignFilter)filter, engine);
		}else if (filter instanceof MissionMemberFilter){
			return asSql((MissionMemberFilter)filter, engine);
		}else if (filter instanceof TrackTypeFilter){
			return asSql((TrackTypeFilter) filter, engine);
		}else{
			return super.toSql(filter, engine); 
		}
		
	}
	
	
	/*
	 * Area filter
	 */
	@Override
	protected String asSql(AreaFilter filter, IQueryEngine engine){
		StringBuilder sb = new StringBuilder();
		if (filter.getGeometryType() == AreaFilterGeometryType.WAYPOINT){
			sb.append("smart.pointinpolygon(" );  //$NON-NLS-1$
			sb.append(engine.tablePrefix(Waypoint.class) + ".x, ");  //$NON-NLS-1$
			sb.append(engine.tablePrefix(Waypoint.class) + ".y, ");  //$NON-NLS-1$
			sb.append( filter.getType().name() + "_" + filter.getKey() + ".geom");  //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(")");  //$NON-NLS-1$
		}else if (filter.getGeometryType() == AreaFilterGeometryType.TRACK){
			sb.append("smart.intersects(");  //$NON-NLS-1$
			sb.append(engine.tablePrefix(MissionTrack.class) + ".geometry, ");  //$NON-NLS-1$
			sb.append(filter.getType().name() + "_" + filter.getKey() + ".geom");  //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(")");  //$NON-NLS-1$
		}
		return sb.toString();
	}
	
	/*
	 * Track type filter
	 */
	protected String asSql(TrackTypeFilter filter, IQueryEngine engine) throws SQLException{
		return engine.tablePrefix(MissionTrack.class) + ".track_type = '" + filter.getTrackType().name() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * Attribute filter
	 */
	@Override
	protected String asSql(AttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((DerbySurveyQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + "." + ((DerbySurveyQueryEngine)engine).getFilterTablesJoinColum() + " is not null "; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.asSql(filter, engine);
	}
	
	/*
	 * Survey design filter
	 */
	protected String asSql(SurveyDesignFilter filter, IQueryEngine engine) throws SQLException{
		return engine.tablePrefix(SurveyDesign.class) + ".keyId = '" + filter.getKey() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * Mission member filter
	 */
	protected String asSql(MissionMemberFilter filter, IQueryEngine engine) throws SQLException{
		StringBuilder sb = new StringBuilder();
		sb.append(engine.tablePrefix(Mission.class));
		sb.append(".uuid IN ("); //$NON-NLS-1$
		sb.append("SELECT mission_uuid FROM "); //$NON-NLS-1$
		sb.append(engine.tableNamePrefix(MissionMember.class));
		sb.append(" WHERE "); //$NON-NLS-1$
		if (filter.isLeader()){
			sb.append("is_leader AND "); //$NON-NLS-1$
		}
		sb.append(" employee_uuid = x'" + SmartUtils.encodeHex(filter.getUuid()) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}
	
	/*
	 * Category filter
	 */
	@Override
	protected String asSql(CategoryFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((DerbySurveyQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + "." + ((DerbySurveyQueryEngine)engine).getFilterTablesJoinColum() + " is not null "; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.asSql(filter, engine);
	}
	
	/*
	 * Category attribute filter
	 */
	@Override
	protected String asSql(CategoryAttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((DerbySurveyQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + "." + ((DerbySurveyQueryEngine)engine).getFilterTablesJoinColum() + " is not null "; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.asSql(filter, engine);	
	}
		
	
	/*
	 * Survey Filter
	 */
	protected String asSql(SurveyFilter filter, IQueryEngine engine) throws SQLException{
		if (filter.getType() == SurveyFilter.Type.ID){
			
			String value1 = SmartUtils.stripQuotes((String)filter.getValue());
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				value1 = "%" + value1 + "%"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String x = "LOWER(" + engine.tablePrefix(Survey.class) + ".id) " + asSql(filter.getOperator()) + " '" + value1.toLowerCase() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			return x;
		}else if (filter.getType() == SurveyFilter.Type.UUID){
			return engine.tablePrefix(Survey.class) + ".uuid = x'" + filter.getValue() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * Sampling unit filter
	 */
	protected String asSql(SamplingUnitFilter filter, IQueryEngine engine) throws SQLException{		
		if (filter.getSource() == null){
			throw new SQLException("Sampling unit filter source table not set.");
		}
		
		if (filter.getSource() == Source.TRACK){
			//match on mission track
			if (filter.getType() == Type.SAMPLINGUNIT){
				if (filter.isNone()){
					return " ( " + engine.tablePrefix(MissionTrack.class) + ".sampling_unit_uuid is null AND "  //$NON-NLS-1$ //$NON-NLS-2$
							+ engine.tablePrefix(MissionTrack.class) + ".uuid is not null )"; //$NON-NLS-1$
				}else{
					return engine.tablePrefix(MissionTrack.class) + ".sampling_unit_uuid = x'" + filter.getUuid() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}else if (filter.getType() == Type.TRACK){
				return engine.tablePrefix(MissionTrack.class) + ".uuid = x'" + filter.getUuid() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
			}			
		}else if (filter.getSource() == Source.OBSERVATION){
			//observation 
			if (filter.getType() == Type.SAMPLINGUNIT){
				if (filter.isNone()){
					return " (" + engine.tablePrefix(SurveyWaypoint.class) + ".sampling_unit_uuid is null " //$NON-NLS-1$ //$NON-NLS-2$
							+ " AND " + engine.tablePrefix(SurveyWaypoint.class) + ".wp_uuid is not null )"; //$NON-NLS-1$ //$NON-NLS-2$
				}else{
					return engine.tablePrefix(SurveyWaypoint.class) + ".sampling_unit_uuid = x'" + filter.getUuid() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ 
				}
			}else if (filter.getType() == Type.TRACK){
				return engine.tablePrefix(SurveyWaypoint.class) + ".mission_track_uuid = x'" + filter.getUuid() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * Sampling unit attribute filter
	 */
	protected String asSql(SamplingUnitAttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((DerbySurveyQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + "." + ((DerbySurveyQueryEngine)engine).getFilterTablesJoinColum() + " is not null "; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (filter.getAttributeType() == AttributeType.NUMERIC){
			return " (sua.sua_" + filter.getSamplingUnitAttributeKey() + " " + asSql(filter.getOperator()) + " " + String.valueOf((Double)filter.getValue()) + ") ";   //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		}else if (filter.getAttributeType() == AttributeType.TEXT){
			String queryStr = ""; //$NON-NLS-1$
			String val = StringEscapeUtils.escapeSql((String)filter.getValue());

			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				queryStr = "( LOWER(sua.sua_" + filter.getSamplingUnitAttributeKey() + ") " + asSql(filter.getOperator()) + " '%" + val.toLowerCase() + "%' )";	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}else if (filter.getOperator() == Operator.STR_EQUALS){
				queryStr = "( LOWER(sua.sua_" + filter.getSamplingUnitAttributeKey() + ") " + asSql(filter.getOperator()) + " '" + val.toLowerCase() + "' )";      //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
			}
			return queryStr;
		}else if (filter.getAttributeType() == AttributeType.LIST) {
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION.getKey())) {
				// any option
				return "( sua.sua_" + filter.getSamplingUnitAttributeKey() + " is not null )"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				return "( sua.sua_" + filter.getSamplingUnitAttributeKey() + " " + asSql(filter.getOperator()) + " '" + (String) filter.getValue() + "' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}else{
			throw new IllegalStateException("Only numeric and text sampling unit attribute types are supported."); //$NON-NLS-1$
		}
	}
	
	/*
	 * Mission Filter
	 */
	protected String asSql(MissionFilter filter, IQueryEngine engine) throws SQLException{
		if (filter.getType() == MissionFilter.Type.ID){
			
			String value1 = SmartUtils.stripQuotes((String)filter.getValue());
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				value1 = "%" + value1 + "%"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String x = "LOWER(" + engine.tablePrefix(Mission.class) + ".id) " + asSql(filter.getOperator()) + " '" + value1.toLowerCase() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			return x;
		}else if (filter.getType() == MissionFilter.Type.UUID){
			return engine.tablePrefix(Mission.class) + ".uuid = x'" + filter.getValue() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return ""; //$NON-NLS-1$
	}
	
	
	/*
	 * Mission property filter
	 */
	protected String asSql(MissionPropertyFilter filter, IQueryEngine engine)
			throws SQLException {
		String col = ((DerbySurveyQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + "." + ((DerbySurveyQueryEngine)engine).getFilterTablesJoinColum() + " is not null "; //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		String attprefix = engine.tablePrefix(MissionAttribute.class);
		if (attprefix == null) {
			throw new IllegalStateException(
					Messages.SurveyFilterSqlGenerator_missionAttributeError);
		}
		String attObprefix = engine.tablePrefix(MissionProperty.class);
		if (attObprefix == null) {
			throw new IllegalStateException(Messages.SurveyFilterSqlGenerator_missionPropertyError);
		}

		if (filter.getAttributeType() == AttributeType.NUMERIC) {
			return " (mt.ma_" + filter.getAttributeKey() + " " + asSql(filter.getOperator()) + " " + String.valueOf((Double) filter.getValue()) + ") "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		} else if (filter.getAttributeType() == AttributeType.TEXT) {
			String queryStr = ""; //$NON-NLS-1$

			String val = StringEscapeUtils.escapeSql((String) filter.getValue());
			if (filter.getOperator() == Operator.STR_CONTAINS
					|| filter.getOperator() == Operator.STR_NOTCONTAINS) {
				queryStr = "( LOWER(mt.ma_" + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " '%" + val.toLowerCase() + "%' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$	
			} else if (filter.getOperator() == Operator.STR_EQUALS) {
				queryStr = "( LOWER(mt.ma_" + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " '" + val.toLowerCase() + "' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			return queryStr;
		} else if (filter.getAttributeType() == AttributeType.LIST) {
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION.getKey())) {
				// any option
				return "( mt.ma_" + filter.getAttributeKey() + " is not null )"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				return "( mt.ma_" + filter.getAttributeKey() + " " + asSql(filter.getOperator()) + " '" + (String) filter.getValue() + "' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * Date Filter
	 */
	@Override
	protected String asSql(DateFilter filter, IQueryEngine engine) throws SQLException{
		String table = ""; //$NON-NLS-1$
		String field = ""; //$NON-NLS-1$
		
		if (filter.getDateFieldOption() == MissionStartDateField.INSTANCE){
			table = engine.tablePrefix(Mission.class);
			field = "start_datetime"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == MissionEndDateField.INSTANCE){
			table = engine.tablePrefix(Mission.class);
			field = "end_datetime"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == WaypointDateField.INSTANCE){
			table = engine.tablePrefix(MissionDay.class);
			field = "mission_day"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == MissionTrackDateField.INSTANCE){
			table = engine.tablePrefix(MissionDay.class);
			field = "mission_day"; //$NON-NLS-1$
		}else{
			throw new SQLException(MessageFormat.format(Messages.SurveyFilterSqlGenerator_DateFilterNotSupported, new Object[]{filter.getDateFieldOption().getGuiName()}));
		}
		
		field = table + "." + field; //$NON-NLS-1$
		
		java.sql.Date[] bits = filter.getDateFilterOption().getDates(); 
		String f = ""; //$NON-NLS-1$
		if (bits == null){
			return ""; //$NON-NLS-1$
		}
		if (bits.length == 1){
			f = " ( cast(" + field + " as date) >= '" + bits[0].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (bits.length == 2 && filter.getDateFilterOption().isEndDateInclusive()){ 
			f = " ( cast(" + field + " as date) >= '" + bits[0].toString() + "' and cast(" + field + " as date) <= '" + bits[1].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else if (bits.length == 2){
			f = " ( cast(" + field + " as date) >= '" + bits[0].toString() + "' and cast(" + field + " as date) < '" + bits[1].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		return f;
	}
	
}
