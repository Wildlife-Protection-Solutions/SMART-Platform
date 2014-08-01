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

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionFilter;
import org.wcs.smart.er.query.filter.MissionPropertyFilter;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.filter.SurveyFilter;
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
	 * Attribute filter
	 */
	@Override
	protected String asSql(AttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((DerbySurveyQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		return super.asSql(filter, engine);
	}
	
	
	/*
	 * Category filter
	 */
	@Override
	protected String asSql(CategoryFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((DerbySurveyQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null ";  //$NON-NLS-1$
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
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
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
			String x = "LOWER(" + engine.tablePrefix(Survey.class) + ".id) " + asSql(filter.getOperator()) + " '" + value1.toLowerCase() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			return x;
		}else if (filter.getType() == SurveyFilter.Type.UUID){
			return engine.tablePrefix(Survey.class) + ".uuid = x'" + filter.getValue() + "'";
		}
		return "";
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
			String x = "LOWER(" + engine.tablePrefix(Mission.class) + ".id) " + asSql(filter.getOperator()) + " '" + value1.toLowerCase() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			return x;
		}else if (filter.getType() == MissionFilter.Type.UUID){
			return engine.tablePrefix(Mission.class) + ".uuid = x'" + filter.getValue() + "'";
		}
		return "";
	}
	
	
	/*
	 * Mission property filter
	 */
	protected String asSql(MissionPropertyFilter filter, IQueryEngine engine)
			throws SQLException {
		String attprefix = engine.tablePrefix(MissionAttribute.class);
		if (attprefix == null) {
			throw new IllegalStateException(
					"mission attribute table not found.");
		}
		String attObprefix = engine.tablePrefix(MissionProperty.class);
		if (attObprefix == null) {
			throw new IllegalStateException("mission property table not found.");
		}

		if (filter.getAttributeType() == AttributeType.NUMERIC) {
			return " (qa.ma_" + filter.getAttributeKey() + " " + asSql(filter.getOperator()) + " " + String.valueOf((Double) filter.getValue()) + ") "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		} else if (filter.getAttributeType() == AttributeType.TEXT) {
			String queryStr = ""; //$NON-NLS-1$
			// TODO: look into escape % & _ as these are wild card characters
			// SELECT a FROM tabA WHERE a LIKE '%=_' ESCAPE '=' (must specify
			// escape character)
			String val = (String) filter.getValue();
			val = val.replaceAll("'", "''"); //$NON-NLS-1$ //$NON-NLS-2$

			if (filter.getOperator() == Operator.STR_CONTAINS
					|| filter.getOperator() == Operator.STR_NOTCONTAINS) {
				queryStr = "( LOWER(qa.ma_" + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " '%" + val.toLowerCase() + "%' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$	
			} else if (filter.getOperator() == Operator.STR_EQUALS) {
				queryStr = "( LOWER(qa.ma_" + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " '" + val.toLowerCase() + "' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			return queryStr;
		} else if (filter.getAttributeType() == AttributeType.LIST) {
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION.getKey())) {
				// any option
				return "( qa.ma_" + filter.getAttributeKey() + " is not null )"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				return "( qa.ma_" + filter.getAttributeKey() + " " + asSql(filter.getOperator()) + " '" + (String) filter.getValue() + "' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
			table = engine.tablePrefix(Waypoint.class);
			field = "datetime"; //$NON-NLS-1$
		}else{
			throw new SQLException(MessageFormat.format("Date filter {0} not supported for survey queries.", new Object[]{filter.getDateFieldOption().getGuiName()}));
		}
		
		field = table + "." + field; //$NON-NLS-1$
		
		java.sql.Date[] bits = filter.getDateFilterOption().getDates(); 
		String f = ""; //$NON-NLS-1$
		if (bits == null){
			return ""; //$NON-NLS-1$
		}
		if (bits.length == 1){
			f = " ( " +field + " >= '" + bits[0].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (bits.length == 2 && filter.getDateFilterOption().isEndDateInclusive()){ 
			f = " ( " + field + " >= '" + bits[0].toString() + "' and " + field  + " <= '" + bits[1].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else if (bits.length == 2){
			f = " ( " + field + " >= '" + bits[0].toString() + "' and " + field  + " < '" + bits[1].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		return f;
	}
	
}
