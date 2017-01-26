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
package org.wcs.smart.connect.query.engine;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.UUID;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.query.engine.er.PsqlErGridEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErMissionEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErMissionTrackEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErObservationEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErSummaryEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErWaypointEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolGridEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolObservationEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolSummaryEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolWaypointEngine;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.query.filter.CmCategoryAttributeFilter;
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
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.intelligence.query.IntelligencePatrolQueryFilter;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.query.model.filter.WaypointSourceFilter;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.ext.IExtensionFilter;
import org.wcs.smart.patrol.query.model.PatrolEndDateField;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolUuidFilter;
import org.wcs.smart.plan.query.PlanPatrolQueryFilter;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.AreaFilter.AreaFilterGeometryType;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.BooleanExpression;
import org.wcs.smart.query.model.filter.BracketFilter;
import org.wcs.smart.query.model.filter.CategoryAttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.NotExpression;
import org.wcs.smart.query.model.filter.ObserverFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Transforms a query filter into SQL.
 * 
 * @author Emily
 *
 */
public enum PsqlFilterToSqlGenerator {
	
	INSTANCE;
	
	private PsqlFilterToSqlGenerator(){
		
	}
	
	/**
	 * converts the given filter to sql
	 * 
	 * @param filter the filter to convert
	 * @param engine the query engine
	 * @return sql string
	 * @throws SQLException
	 */
	public String toSql(IFilter filter, IQueryEngine engine) throws SQLException{
		if (filter instanceof AreaFilter){
			return asSql((AreaFilter)filter, engine);
		}else if (filter instanceof AttributeFilter){
			return asSql((AttributeFilter)filter, engine);
		}else if (filter instanceof BooleanExpression){
			return asSql((BooleanExpression)filter, engine);
		}else if (filter instanceof BracketFilter){
			return asSql((BracketFilter)filter, engine);
		}else if (filter instanceof CategoryAttributeFilter){
			return asSql((CategoryAttributeFilter)filter, engine);
		}else if (filter instanceof CategoryFilter){
			return asSql((CategoryFilter)filter, engine);
		}else if (filter instanceof EmptyFilter){
			return ""; //$NON-NLS-1$
		}else if (filter instanceof NotExpression){
			return asSql((NotExpression)filter, engine);
		}else if (filter instanceof DateFilter){
			return asSql((DateFilter)filter, engine);
		}else if (filter instanceof ObserverFilter){
			return asSql((ObserverFilter)filter, engine);
		}else  if (filter instanceof PatrolFilter){
			return asSql((PatrolFilter)filter, engine);
		}else if (filter instanceof PatrolUuidFilter){
			return asSql((PatrolUuidFilter)filter, engine);
		}else if (filter instanceof IExtensionFilter){
			return asSql((IExtensionFilter)filter, engine);
		}else if (filter instanceof ConservationAreaFilter){
			if (engine instanceof PsqlPatrolObservationEngine ||
					engine instanceof PsqlPatrolEngine ||
					engine instanceof PsqlPatrolSummaryEngine ||
					engine instanceof PsqlPatrolWaypointEngine ||
					engine instanceof PsqlPatrolGridEngine){
				//patrol baseed
				return asSql((ConservationAreaFilter)filter, engine.tablePrefix(Patrol.class), engine);
			}else if (engine instanceof PsqlErObservationEngine ||
					engine instanceof PsqlErMissionTrackEngine ||
					engine instanceof PsqlErSummaryEngine ||
					engine instanceof PsqlErWaypointEngine ||
					engine instanceof PsqlErGridEngine ||
					engine instanceof PsqlErMissionEngine){
				return asSql((ConservationAreaFilter)filter, engine.tablePrefix(SurveyDesign.class), engine);
			}else{
				//waypoint based
				//observation and entity
				return asSql((ConservationAreaFilter)filter, engine.tablePrefix(Waypoint.class), engine);
			}
		}else if (filter instanceof EntityAttributeFilter){
			return asSql((EntityAttributeFilter)filter, engine);
		}else if (filter instanceof WaypointSourceFilter){
			return asSql((WaypointSourceFilter)filter, engine);
		}else if (filter instanceof SurveyFilter){
			return asSql((SurveyFilter)filter, engine);
		}else if (filter instanceof MissionFilter){
			return asSql((MissionFilter)filter, engine);
		}else if (filter instanceof MissionPropertyFilter){
			return asSql((MissionPropertyFilter)filter, engine);
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
		}else if (filter instanceof CmCategoryAttributeFilter){
			CmCategoryAttributeFilter afilter = (CmCategoryAttributeFilter)filter;
			return asSql(new CategoryAttributeFilter(afilter.getCategoryFilter(), afilter.getAttributeFilter()), engine);
		}
		throw new SQLException(MessageFormat.format("Filter not supported '{0}'.", filter.asString())); //$NON-NLS-1$
		
	}
	
	/*
	 * Observer source filter
	 */
	protected String asSql(ObserverFilter filter, IQueryEngine engine) throws SQLException{
		try {
			String param = engine.addParameterValue(UuidUtils.stringToUuid(filter.getValue())); 
			StringBuilder sb = new StringBuilder();
			sb.append(engine.tablePrefix(WaypointObservation.class));
			sb.append(".employee_uuid "); //$NON-NLS-1$
			sb.append(" =  " + param); //$NON-NLS-1$
			return sb.toString();
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}
	
	/*
	 * Area filter
	 */
	protected String asSql(AreaFilter filter, IQueryEngine engine){
		StringBuilder sb = new StringBuilder();
		if (filter.getGeometryType() == AreaFilterGeometryType.WAYPOINT){
			sb.append("smart.pointinpolygon(" );  //$NON-NLS-1$
			sb.append(engine.tablePrefix(Waypoint.class) + ".x, ");  //$NON-NLS-1$
			sb.append(engine.tablePrefix(Waypoint.class) + ".y, ");  //$NON-NLS-1$
			sb.append( filter.getType().name() + "_" + filter.getKey() + ".geom");  //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(")");  //$NON-NLS-1$
		}else if (filter.getGeometryType() == AreaFilterGeometryType.TRACK){
			if (engine.canExecute(PatrolObservationQuery.KEY) ||
					engine.canExecute(PatrolGriddedQuery.KEY) ||
					engine.canExecute(PatrolQuery.KEY) ||
					engine.canExecute(PatrolSummaryQuery.KEY) ||
					engine.canExecute(PatrolWaypointQuery.KEY)){
				//For Patrol Queries use track
				//user track table
				sb.append("smart.trackIntersects(");  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Track.class) + ".geometry, ");  //$NON-NLS-1$
				sb.append(filter.getType().name() + "_" + filter.getKey() + ".geom");  //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(")");  //$NON-NLS-1$
			}else if (engine.canExecute(SurveyObservationQuery.KEY) ||
					engine.canExecute(SurveyGriddedQuery.KEY) ||
					engine.canExecute(MissionQuery.KEY) ||
					engine.canExecute(MissionTrackQuery.KEY) ||
					engine.canExecute(SurveySummaryQuery.KEY) ||
					engine.canExecute(SurveyWaypointQuery.KEY)){
				//survey queries use mission track table
				sb.append("smart.trackIntersects(");  //$NON-NLS-1$
				sb.append(engine.tablePrefix(MissionTrack.class) + ".geometry, ");  //$NON-NLS-1$
				sb.append(filter.getType().name() + "_" + filter.getKey() + ".geom");  //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(")");  //$NON-NLS-1$
			}
		}
		
		return sb.toString();
	}
	

	/*
	 * Attribute filter
	 */
	protected String asSql(AttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		
		String attprefix = engine.tablePrefix(Attribute.class);
		if (attprefix == null){
			throw new IllegalStateException("Invalid attribute filter."); //$NON-NLS-1$
		}
		String attObprefix = engine.tablePrefix(WaypointObservationAttribute.class);
		if (attObprefix == null){
			throw new IllegalStateException("Invalid attribute filter."); //$NON-NLS-1$
		}

		if (filter.getAttributeType() == AttributeType.BOOLEAN){
			return " (qa." + filter.getAttributeKey() + " > 0.5 ) ";			//$NON-NLS-1$ //$NON-NLS-2$
		}else if (filter.getAttributeType() == AttributeType.NUMERIC){
			
			return " (qa." + filter.getAttributeKey() + " " + asSql(filter.getOperator()) + " " + engine.addParameterValue((Double)filter.getValue()) +" ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}else if (filter.getAttributeType() == AttributeType.TEXT){
			String queryStr = ""; //$NON-NLS-1$
			String val = (String)filter.getValue();
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				String param = engine.addParameterValue("%" + val.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ 
				queryStr = "( LOWER(qa." + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " " + param + " )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 	
			}else if (filter.getOperator() == Operator.STR_EQUALS){
				String param = engine.addParameterValue(val.toLowerCase()); 
				queryStr = "( LOWER(qa." + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " " + param + " )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			}
			return queryStr;
		}else if (filter.getAttributeType() == AttributeType.DATE){
			String p1 = engine.addParameterValue((String) filter.getValue()); 
			String p2 = engine.addParameterValue((String) filter.getValue2()); 
			return "( qa." + filter.getAttributeKey() + " is not null AND DATE(qa." + filter.getAttributeKey() + ") " + " " + asSql(filter.getOperator()) + " CAST(" + p1 + " as DATE) " + asSql(Operator.AND) + " CAST(" + p2 + " as DATE) )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		}else if (filter.getAttributeType() == AttributeType.LIST ){
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION_KEY)){
				//any option
				return "( qa."+ filter.getAttributeKey()  + " is not null )";  //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				String p = engine.addParameterValue(filter.getValue()); 
				return "( qa."+ filter.getAttributeKey()  + " " + asSql(filter.getOperator()) + " " + p + ")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			}
		}else if (filter.getAttributeType() == AttributeType.TREE){
			String p1 = engine.addParameterValue(filter.getValue() + "%");   //$NON-NLS-1$
			return "( qa." + filter.getAttributeKey() + " like " + p1 + " ) ";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		}
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * boolean filter
	 */
	protected String asSql(BooleanExpression filter, IQueryEngine engine) throws SQLException{
		String part1 = toSql(filter.getFilter1(), engine);
		String part2 = toSql(filter.getFilter2(), engine);
		return part1 + " " + asSql(filter.getOperator()) + " " + part2; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * bracket filter
	 */
	protected String asSql(BracketFilter filter, IQueryEngine engine) throws SQLException{
		return "( " + toSql(filter.getFilter(), engine) + " )"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * Category filter
	 */
	protected String asSql(CategoryFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null ";  //$NON-NLS-1$
		}
		
		String keyPart = filter.getCategoryKey();
		String prefix = engine.tablePrefix(Category.class);
		if (prefix == null){
			throw new IllegalStateException("Invalid category filter."); //$NON-NLS-1$
		}
		
		String p1 = engine.addParameterValue(keyPart + "%");  //$NON-NLS-1$
		return "( " + prefix + ".hkey like " + p1 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$	
	}
	
	/*
	 * Category attribute filter
	 */
	protected String asSql(CategoryAttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		return "( " + toSql(filter.getCategoryFilter(), engine) + " " + asSql(Operator.AND) + " " + toSql(filter.getAttributeFilter(), engine) + " )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$	
	}
	
	/*
	 * not expression
	 */
	protected String asSql(NotExpression filter, IQueryEngine engine) throws SQLException{
		return asSql(Operator.NOT) + " ( " + toSql(filter.getFilter(), engine) + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
	}
	
	/**
	 * Converts a conservation area filter to sql
	 * 
	 * @param filter the filter
	 * @param caTablePrefix the prefix of the table name that contains the ca_uuid field to filter on
	 * @return
	 * @throws SQLException
	 */
	public String asSql(ConservationAreaFilter filter, String caTablePrefix, IQueryEngine engine) throws SQLException{
		ArrayList<UUID> localFilters = new ArrayList<UUID>();
		if (filter.includeAll()){
			//we don't want to include all conservation area here as 
			//this may include conservation areas this user does not have access to view
			
			//for now we force the user to provide the conservation areas
			throw new SQLException("Conservation Area filter not populated.  At least one conservation area must be provided in the Conservation Area filter."); //$NON-NLS-1$
		}else{
			//TODO: this may include ca's the user does not have access to view
			//include only selected conservation areas
			localFilters.addAll(filter.getConservationAreaFilterIds());
		}
		
		if (localFilters.size() == 0){
			return "false"; //$NON-NLS-1$
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(caTablePrefix);
		sb.append(".ca_uuid IN ("); //$NON-NLS-1$
		for (int i = 0; i < localFilters.size(); i++) {
			if (i != 0){
				sb.append(","); //$NON-NLS-1$
			}
			String p = engine.addParameterValue(localFilters.get(i));
			sb.append(p); 
		}
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}
	
	
	/*
	 * Date Filter
	 */
	protected String asSql(DateFilter filter, IQueryEngine engine) throws SQLException{
		String table = ""; //$NON-NLS-1$
		String field = ""; //$NON-NLS-1$
		
		if (filter.getDateFieldOption() == WaypointDateField.INSTANCE){
			field = ((AbstractQueryEngine)engine).getDateFilterField();
			table = ((AbstractQueryEngine)engine).getDateFilterTable();
			if (field == null || table == null){
				table = engine.tablePrefix(Waypoint.class);
				field = "datetime"; //$NON-NLS-1$
			}
		}else if (filter.getDateFieldOption() == WaypointDateField.INSTANCE){
			table = engine.tablePrefix(PatrolLegDay.class);
			field = "patrol_day"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == WaypointDateField.INSTANCE){
			table = engine.tablePrefix(MissionDay.class);
			field = "mission_day"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == PatrolEndDateField.INSTANCE){
			table = engine.tablePrefix(Patrol.class);
			field = "end_date"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == PatrolStartDateField.INSTANCE){
			table = engine.tablePrefix(Patrol.class);
			field = "start_date"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == MissionStartDateField.INSTANCE){
			table = engine.tablePrefix(Mission.class);
			field = "start_datetime"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == MissionEndDateField.INSTANCE){
			table = engine.tablePrefix(Mission.class);
			field = "end_datetime"; //$NON-NLS-1$
		
		}else if (filter.getDateFieldOption() == MissionTrackDateField.INSTANCE){
			table = engine.tablePrefix(MissionDay.class);
			field = "mission_day"; //$NON-NLS-1$
		}else{
			throw new SQLException("Date format not supported"); //$NON-NLS-1$
		}
		
		
		field = table + "." + field; //$NON-NLS-1$
		
		java.sql.Date[] bits = filter.getDateFilterOption().getDates(); 
		String f = ""; //$NON-NLS-1$
		if (bits == null){
			return ""; //$NON-NLS-1$
		}

		String p1 = engine.addParameterValue(bits[0]);
		if (bits.length == 1){
			f = " ( cast(" + field + " as date) >= " + p1 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		}else if (bits.length == 2 && filter.getDateFilterOption().isEndDateInclusive()){
			String p2 = engine.addParameterValue(bits[1]);
			f = " ( cast(" + field + " as date) >= "+ p1 + " and cast(" + field  + " as date) <= " + p2 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else if (bits.length == 2){
			String p2 = engine.addParameterValue(bits[1]);
			f = " ( cast(" + field + " as date) >= " + p1 + " and cast(" + field  + " as date) < " + p2 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		return f;
	}
	
	/**
	 * Converts an operator to sql
	 * 
	 * @param op
	 * @return
	 * @throws SQLException
	 */
	public static String asSql(Operator op) throws SQLException{
		if (op == Operator.EQUALS){
			return "="; //$NON-NLS-1$
		}else if (op == Operator.LESSTHAN){
			return "<"; //$NON-NLS-1$
		}else if (op == Operator.LESSTHANEQUALS){
			return "<="; //$NON-NLS-1$
		}else if (op == Operator.GREATERTHAN){
			return ">"; //$NON-NLS-1$
		}else if (op == Operator.GREATERTHANEQUALS){
			return ">="; //$NON-NLS-1$
		}else if (op == Operator.NOTEQUALS){
			return "<>"; //$NON-NLS-1$
		}else if (op == Operator.STR_EQUALS){
			return "="; //$NON-NLS-1$
		}else if (op == Operator.STR_CONTAINS){
			return "like"; //$NON-NLS-1$
		}else if (op == Operator.STR_NOTCONTAINS){
			return "not like"; //$NON-NLS-1$
		}else if (op == Operator.AND){
			return "and"; //$NON-NLS-1$
		}else if (op == Operator.OR){
			return "or"; //$NON-NLS-1$
		}else if (op == Operator.NOT){
			return "not"; //$NON-NLS-1$
		}else if (op == Operator.BETWEEN){
			return "between"; //$NON-NLS-1$
		}else if (op == Operator.NOT_BETWEEN){
			return "not between"; //$NON-NLS-1$
		}
		throw new SQLException(MessageFormat.format("Operator {0} not supported.", new Object[]{op.getGuiValue()})); //$NON-NLS-1$
	}
		
	/*
	 * extension filters
	 */
	protected String asSql(IExtensionFilter filter, IQueryEngine engine) throws SQLException{
		if (filter instanceof IntelligencePatrolQueryFilter){
			IntelligencePatrolQueryFilter qFilter = (IntelligencePatrolQueryFilter)filter;
			String prefix = engine.tablePrefix(qFilter.getPatrolQueryOption().getPatrolAttributeClass());
			String v = SharedUtils.stripQuotes((String)qFilter.getValue());
			//if v is empty this means that this is "Any Plan" case
			
			String intelPart = ""; //$NON-NLS-1$
			if (!qFilter.isAnyIntelligence()){
				String param = engine.addParameterValue(UuidUtils.stringToUuid(v));
				intelPart = !qFilter.isAnyIntelligence() ? " AND p2i.intelligence_uuid = " + param  : "";  //$NON-NLS-1$//$NON-NLS-2$ 
			}
			String sql = "EXISTS (SELECT * FROM smart.patrol_intelligence p2i WHERE p2i.patrol_uuid = " + prefix + ".uuid" + intelPart + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return sql;
		}else if (filter instanceof PlanPatrolQueryFilter){
			PlanPatrolQueryFilter qfilter = (PlanPatrolQueryFilter)filter;
			
			String prefix = engine.tablePrefix(qfilter.getOption().getPatrolAttributeClass());
			String v = SharedUtils.stripQuotes((String)qfilter.getValue());
			String planSqlPart = ""; //$NON-NLS-1$
			if (!qfilter.isAnyPlan()) {
				StringBuilder sb = new StringBuilder();
				sb.append("AND pa2pl.plan_uuid IN ("); //$NON-NLS-1$
				sb.append("WITH RECURSIVE childpatrols(uuid) AS ("); //$NON-NLS-1$
				sb.append("SELECT uuid FROM smart.plan WHERE uuid = "); //$NON-NLS-1$
				String p = engine.addParameterValue(UuidUtils.stringToUuid(v));
				sb.append(p);
				sb.append("	UNION ALL SELECT p.uuid FROM childpatrols cp, smart.plan p WHERE cp.uuid = p.parent_uuid )"); //$NON-NLS-1$
				sb.append(" SELECT uuid FROM childpatrols )"); //$NON-NLS-1$
				planSqlPart = sb.toString();
			}
			String sql = "EXISTS (SELECT * FROM smart.patrol_plan pa2pl WHERE pa2pl.patrol_uuid = "+prefix+".uuid "+planSqlPart+")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return sql;
		}
		throw new IllegalStateException(MessageFormat.format("Filter {0} not supported.", filter.asString()));	 //$NON-NLS-1$
	}
	/*
	 * Patrol Filter
	 */
	protected String asSql(PatrolFilter filter, IQueryEngine engine) throws SQLException{
		PatrolQueryOption option = filter.getPatrolOption();
		if (option.isEmployeeItem()){
			if (option == PatrolQueryOption.AGENCY || 
					option == PatrolQueryOption.RANK){
				String field = option == PatrolQueryOption.AGENCY ? "agency_uuid" : "rank_uuid"; //$NON-NLS-1$ //$NON-NLS-2$
				String p1 = engine.addParameterValue(UuidUtils.stringToUuid(SharedUtils.stripQuotes(filter.getValue())));
				StringBuilder sb = new StringBuilder();
				sb.append(engine.tablePrefix(PatrolLeg.class));
				sb.append(".uuid IN (SELECT patrol_leg_uuid FROM "); //$NON-NLS-1$
				sb.append(engine.tableNamePrefix(PatrolLegMember.class));
				sb.append(","); //$NON-NLS-1$
				sb.append(engine.tableNamePrefix(Employee.class));
				sb.append(" WHERE "); //$NON-NLS-1$
				sb.append(engine.tablePrefix(PatrolLegMember.class));
				sb.append(".employee_uuid = "); //$NON-NLS-1$
				sb.append(engine.tablePrefix(Employee.class));
				sb.append(".uuid AND e." + field + " = "); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(p1);
				sb.append(")"); //$NON-NLS-1$
				return sb.toString();
				
			}else{
				String prefix = engine.tablePrefix(PatrolLeg.class);
				String x = prefix + ".uuid IN ( select patrol_leg_uuid from smart.patrol_leg_members "  //$NON-NLS-1$
						+ " where "; //$NON-NLS-1$
				if (option == PatrolQueryOption.LEADER) {
					x += " is_leader  AND "; //$NON-NLS-1$
				} else if (option == PatrolQueryOption.PILOT) {
					x += " is_pilot AND "; //$NON-NLS-1$
				}
				String value2 = SharedUtils.stripQuotes((String)filter.getValue());
				try {
					String p1 = engine.addParameterValue(UuidUtils.stringToUuid(value2));
					x += " employee_uuid = " + p1 + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception e) {
					throw new SQLException(e);
				}	 
				return x;			
			}
		}	
		
		
		
		String prefix = engine.tablePrefix(option.getPatrolAttributeClass());
		if (prefix == null){
			throw new SQLException(MessageFormat.format(
					"Invalid patrol option {0}", new Object[]{ option.getKey()})); //$NON-NLS-1$
		}
		
		if (option.getType() == PatrolQueryOptionType.STRING){
			if (option == PatrolQueryOption.PATROL_TYPE){
				String p1 = engine.addParameterValue(SharedUtils.stripQuotes((String)filter.getValue()) );
				String x = prefix + "." + option.getColumnName() + " = " + p1; //$NON-NLS-1$ //$NON-NLS-2$ 
				return x;				
			}else{
				String value1 = SharedUtils.stripQuotes((String)filter.getValue());
				if (filter.getOperator() == Operator.STR_CONTAINS || 
						filter.getOperator() == Operator.STR_NOTCONTAINS){
					value1 = "%" + value1 + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				String p1 = engine.addParameterValue(value1.toLowerCase());
				String x = "LOWER(" + prefix + "." + option.getColumnName() + ") " + asSql(filter.getOperator()) + " " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				return x;
			}
		}else if (option.getType() == PatrolQueryOptionType.BOOLEAN){
			//boolean
			String x = prefix + "." + option.getColumnName() ; //+ " = 'true'" ; //$NON-NLS-1$
			return x;
		}else if (option.getType() == PatrolQueryOptionType.UUID){
			//uuid
			try{
				String value2 = SharedUtils.stripQuotes((String)filter.getValue());
				String p1 = engine.addParameterValue(UuidUtils.stringToUuid(value2));
				String x = prefix + "." + option.getColumnName() + " = " + p1; //$NON-NLS-1$ //$NON-NLS-2$ 
				return x;
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
			
		}else if (option.getType() == PatrolQueryOptionType.KEY){
			String key = SharedUtils.stripQuotes((String)filter.getValue());
			String p1 = engine.addParameterValue(key);
			return prefix + "." + option.getColumnName() + " IN ( select uuid from " + engine.tableName(option.getSourceClass()) + " where keyid = " + p1 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			
		}
		return ""; //$NON-NLS-1$	
	}
	
	/**
	 * Patrol uuid filter.
	 * @param filter
	 * @param engine
	 * @return
	 * @throws SQLException
	 */
	protected String asSql(PatrolUuidFilter filter, IQueryEngine engine) throws SQLException{
		String prefix = engine.tablePrefix(Patrol.class);
		if (filter.getOperator().equals(Operator.STR_EQUALS)){
			if (filter.getValue().length() != 0){
				try {
					String p1 = engine.addParameterValue(
							UuidUtils.stringToUuid(SharedUtils.stripQuotes(filter.getValue())));
					return prefix + ".uuid = " + p1; //$NON-NLS-1$
				} catch (Exception e) {
					throw new SQLException(e);
				}
			}else{
				return "false"; //$NON-NLS-1$
			}
		}else{
			throw new SQLException("Only equals operator is supported for patrol uuid filters."); //$NON-NLS-1$
		}
	}

	/*
	 * Waypoint source filter
	 */
	protected String asSql(WaypointSourceFilter filter, IQueryEngine engine) throws SQLException{
		StringBuilder sb = new StringBuilder();
		sb.append(engine.tablePrefix(Waypoint.class));
		sb.append(".source "); //$NON-NLS-1$
		sb.append(asSql(filter.getOperator()));
		String p1 = engine.addParameterValue(SharedUtils.stripQuotes(filter.getWaypointSourceKey()));
		sb.append(" " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
		return sb.toString();
	}

	
	
	public String asSql(EntityAttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		
		String tableName = filter.getEntityKey() + "_" + filter.getEntityAttributeKey(); //$NON-NLS-1$
	
		if (filter.getAttributeType() == AttributeType.BOOLEAN){
			return " (" + tableName + ".value  > 0.5 ) ";			//$NON-NLS-1$ //$NON-NLS-2$
		}else if (filter.getAttributeType() == AttributeType.NUMERIC){
			String p1 = engine.addParameterValue((Double)filter.getValue());
			return " ( " + tableName + ".value " + asSql(filter.getOperator()) + " " + p1 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}else if (filter.getAttributeType() == AttributeType.TEXT){
			String queryStr = ""; //$NON-NLS-1$
			String val = (String)filter.getValue();
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				String p1 = engine.addParameterValue("%" + val.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$
				queryStr = "( LOWER(" + tableName + ".value) " + asSql(filter.getOperator()) + " " + p1 + " )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}else if (filter.getOperator() == Operator.STR_EQUALS){
				String p1 = engine.addParameterValue(val.toLowerCase());
				queryStr = "( LOWER(" + tableName + ".value) " + asSql(filter.getOperator()) + " " + p1 + " )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			return queryStr;
		}else if (filter.getAttributeType() == AttributeType.DATE){
			String date1 = (String) filter.getValue();
			String date2 = (String) filter.getValue2();
			String p1 = engine.addParameterValue(date1);
			String p2 = engine.addParameterValue(date2);
			return "( " + tableName + ".value is not null AND DATE(" + tableName + ".value) " + " " + asSql(filter.getOperator()) + " CAST(" + p1 + " as DATE) " + asSql(Operator.AND) + " CAST(" + p2 + " as DATE) )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		}else if (filter.getAttributeType() == AttributeType.LIST ){
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION_KEY)){
				//any option
				return "( " + tableName + ".value is not null )";  //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				String p1 = engine.addParameterValue((String)filter.getValue());
				return "( " + tableName + ".value " + asSql(filter.getOperator()) + " " + p1 + " )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}else if (filter.getAttributeType() == AttributeType.TREE){
			String p1 = engine.addParameterValue((String)filter.getValue() + "%"); //$NON-NLS-1$
			return "( " + tableName + ".value like " + p1 + " )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		}
		return "";  //$NON-NLS-1$
	}
	/*
	 * Track type filter
	 */
	protected String asSql(TrackTypeFilter filter, IQueryEngine engine) throws SQLException{
		return engine.tablePrefix(MissionTrack.class) + ".track_type = '" + filter.getTrackType().name() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	
	/*
	 * Survey design filter
	 */
	protected String asSql(SurveyDesignFilter filter, IQueryEngine engine) throws SQLException{
		String p1 = engine.addParameterValue(filter.getKey());
		return engine.tablePrefix(SurveyDesign.class) + ".keyId = " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$
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
		String p1 = engine.addParameterValue(filter.getUuid());
		sb.append(" employee_uuid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}

	
	/*
	 * Survey Filter
	 */
	protected String asSql(SurveyFilter filter, IQueryEngine engine) throws SQLException{
		if (filter.getType() == SurveyFilter.Type.ID){
			
			String value1 = SharedUtils.stripQuotes((String)filter.getValue());
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				value1 = "%" + value1 + "%"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String p1 = engine.addParameterValue(value1.toLowerCase());
			String x = "LOWER(" + engine.tablePrefix(Survey.class) + ".id) " + asSql(filter.getOperator()) + " " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			return x;
		}else if (filter.getType() == SurveyFilter.Type.UUID){
			try{
				String p1 = engine.addParameterValue(UuidUtils.stringToUuid(filter.getValue()));
				return engine.tablePrefix(Survey.class) + ".uuid = " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$
			}catch (Exception ex){
				throw new SQLException(ex);
			}
			 
		}
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * Sampling unit filter
	 */
	protected String asSql(SamplingUnitFilter filter, IQueryEngine engine) throws SQLException{		
		if (filter.getSource() == null){
			throw new SQLException("Not source for sampling unit filter."); //$NON-NLS-1$
		}
		
		try{
			if (filter.getSource() == Source.TRACK){
				//match on mission track
				if (filter.getType() == Type.SAMPLINGUNIT){
					if (filter.isNone()){
						return " ( " + engine.tablePrefix(MissionTrack.class) + ".sampling_unit_uuid is null AND "  //$NON-NLS-1$ //$NON-NLS-2$
							+ engine.tablePrefix(MissionTrack.class) + ".uuid is not null )"; //$NON-NLS-1$
					}else{
						String p1 = engine.addParameterValue(UuidUtils.stringToUuid(filter.getUuid()));
						return engine.tablePrefix(MissionTrack.class) + ".sampling_unit_uuid = " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$ 
					}
				}else if (filter.getType() == Type.TRACK){
					String p1 = engine.addParameterValue(UuidUtils.stringToUuid(filter.getUuid()));
					return engine.tablePrefix(MissionTrack.class) + ".uuid = " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$
				}			
			}else if (filter.getSource() == Source.OBSERVATION){
				//observation 
				if (filter.getType() == Type.SAMPLINGUNIT){
					if (filter.isNone()){
						return " (" + engine.tablePrefix(SurveyWaypoint.class) + ".sampling_unit_uuid is null " //$NON-NLS-1$ //$NON-NLS-2$
							+ " AND " + engine.tablePrefix(SurveyWaypoint.class) + ".wp_uuid is not null )"; //$NON-NLS-1$ //$NON-NLS-2$
					}else{
						String p1 = engine.addParameterValue(UuidUtils.stringToUuid(filter.getUuid()));
						return engine.tablePrefix(SurveyWaypoint.class) + ".sampling_unit_uuid = " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$ 
					}
				}else if (filter.getType() == Type.TRACK){
					String p1 = engine.addParameterValue(UuidUtils.stringToUuid(filter.getUuid()));
					return engine.tablePrefix(SurveyWaypoint.class) + ".mission_track_uuid = " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}catch (Exception ex){
			throw new SQLException (ex);
		}
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * Sampling unit attribute filter
	 */
	protected String asSql(SamplingUnitAttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + "." + ((AbstractQueryEngine)engine).getSurveySamplingUnitJoinFieldName() + " is not null "; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (filter.getAttributeType() == AttributeType.NUMERIC){
			String p1 = engine.addParameterValue((Double)filter.getValue());
			return " (sua.sua_" + filter.getSamplingUnitAttributeKey() + " " + asSql(filter.getOperator()) + " " + p1 + ") ";   //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		}else if (filter.getAttributeType() == AttributeType.TEXT){
			String queryStr = ""; //$NON-NLS-1$
			String val = (String) filter.getValue();
			
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				String p1 = engine.addParameterValue("%" + val.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$
				queryStr = "( LOWER(sua.sua_" + filter.getSamplingUnitAttributeKey() + ") " + asSql(filter.getOperator()) + " " + p1 + " )";	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}else if (filter.getOperator() == Operator.STR_EQUALS){
				String p1 = engine.addParameterValue(val.toLowerCase());
				queryStr = "( LOWER(sua.sua_" + filter.getSamplingUnitAttributeKey() + ") " + asSql(filter.getOperator()) + " " + p1 + " )";  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
			}
			return queryStr;
		}else if (filter.getAttributeType() == AttributeType.LIST) {
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION_KEY)) {
				// any option
				return "( sua.sua_" + filter.getSamplingUnitAttributeKey() + " is not null )"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				String p1 = engine.addParameterValue((String) filter.getValue() );
				return "( sua.sua_" + filter.getSamplingUnitAttributeKey() + " " + asSql(filter.getOperator()) + " " + p1 + " )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
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
			
			String value1 = SharedUtils.stripQuotes((String)filter.getValue());
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				value1 = "%" + value1 + "%"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String p1 = engine.addParameterValue(value1.toLowerCase());
			String x = "LOWER(" + engine.tablePrefix(Mission.class) + ".id) " + asSql(filter.getOperator()) + " " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			return x;
		}else if (filter.getType() == MissionFilter.Type.UUID){
			try{
				String p1 = engine.addParameterValue(UuidUtils.stringToUuid(filter.getValue()));
				return engine.tablePrefix(Mission.class) + ".uuid = " + p1 + " "; //$NON-NLS-1$ //$NON-NLS-2$
			}catch (Exception ex){
				throw new SQLException (ex);
			}
			
		}
		return ""; //$NON-NLS-1$
	}
	
	
	/*
	 * Mission property filter
	 */
	protected String asSql(MissionPropertyFilter filter, IQueryEngine engine)
			throws SQLException {
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + "." + ((AbstractQueryEngine)engine).getSurveySamplingUnitJoinFieldName() + " is not null "; //$NON-NLS-1$ //$NON-NLS-2$
		}
		
//		String attprefix = engine.tablePrefix(MissionAttribute.class);
//		String attObprefix = engine.tablePrefix(MissionProperty.class);
		

		if (filter.getAttributeType() == AttributeType.NUMERIC) {
			String p1 = engine.addParameterValue((Double) filter.getValue());
			return " (mt.ma_" + filter.getAttributeKey() + " " + asSql(filter.getOperator()) + " " + p1 + ") "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		} else if (filter.getAttributeType() == AttributeType.TEXT) {
			String queryStr = ""; //$NON-NLS-1$

			String val = (String)filter.getValue();
			if (filter.getOperator() == Operator.STR_CONTAINS
					|| filter.getOperator() == Operator.STR_NOTCONTAINS) {
				String p1 = engine.addParameterValue("%" + val.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$
				queryStr = "( LOWER(mt.ma_" + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " " + p1 + " )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 	
			} else if (filter.getOperator() == Operator.STR_EQUALS) {
				String p1 = engine.addParameterValue(val.toLowerCase());
				queryStr = "( LOWER(mt.ma_" + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " " + p1 + " )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			}
			return queryStr;
		} else if (filter.getAttributeType() == AttributeType.LIST) {
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION_KEY)) {
				// any option
				return "( mt.ma_" + filter.getAttributeKey() + " is not null )"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				String p1 = engine.addParameterValue((String) filter.getValue());
				return "( mt.ma_" + filter.getAttributeKey() + " " + asSql(filter.getOperator()) + " " + p1 + " )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		return ""; //$NON-NLS-1$
	}
	
}
