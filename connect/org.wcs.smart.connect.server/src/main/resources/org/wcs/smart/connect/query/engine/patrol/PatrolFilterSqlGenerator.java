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
package org.wcs.smart.connect.query.engine.patrol;


import java.sql.SQLException;
import java.text.MessageFormat;

import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolEndDateField;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.parser.IExtensionFilter;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolContributionFactory;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolUuidFilter;
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
import org.wcs.smart.util.UuidUtils;

/**
 * Converts filters to sql for the Derby query engine.
 * 
 * @author Emily
 *
 */
public class PatrolFilterSqlGenerator extends DerbyFilterToSqlGenerator{

	
	public static final PatrolFilterSqlGenerator INSTANCE = new PatrolFilterSqlGenerator();
	
	
	private PatrolFilterSqlGenerator(){
		
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
		if (filter instanceof PatrolFilter){
			return asSql((PatrolFilter)filter, engine);
		}else if (filter instanceof PatrolUuidFilter){
			return asSql((PatrolUuidFilter)filter, engine);
		}else if (filter instanceof IExtensionFilter){
			return asSql((IExtensionFilter)filter, engine);
		}else if (filter instanceof ConservationAreaFilter){
			return asSql((ConservationAreaFilter)filter, engine.tablePrefix(Patrol.class), engine);
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
			sb.append(engine.tablePrefix(Track.class) + ".geometry, ");  //$NON-NLS-1$
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
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
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
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
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
		String col = ((AbstractQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		return super.asSql(filter, engine);	
	}
		
	/*
	 * not expression
	 */
	protected String asSql(IExtensionFilter filter, IQueryEngine engine) throws SQLException{
		return PatrolContributionFactory.getSql(engine, filter);	
	}
	/*
	 * Patrol Filter
	 */
	protected String asSql(PatrolFilter filter, IQueryEngine engine) throws SQLException{
		PatrolQueryOption option = filter.getPatrolOption();
		if (option.isEmployeeItem()){
			String prefix = engine.tablePrefix(PatrolLeg.class);
			String x = prefix + ".uuid IN ( select patrol_leg_uuid from smart.patrol_leg_members "  //$NON-NLS-1$
					+ " where "; //$NON-NLS-1$
			if (option == PatrolQueryOption.LEADER) {
				x += " is_leader  AND "; //$NON-NLS-1$
			} else if (option == PatrolQueryOption.PILOT) {
				x += " is_pilot AND "; //$NON-NLS-1$
			}
			
			String value2 = SmartUtils.stripQuotes((String)filter.getValue());
			try {
				String p1 = engine.addParameterValue(UuidUtils.stringToUuid(value2));
				x += " employee_uuid = " + p1 + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			} catch (Exception e) {
				throw new SQLException(e);
			}
			 
			return x;			
		}		
		String prefix = engine.tablePrefix(filter.getPatrolOption().getPatrolAttributeClass());
		if (prefix == null){
			throw new SQLException(MessageFormat.format(
					Messages.PatrolFilter_InvalidPrefix, new Object[]{ filter.getPatrolOption().getKey()}));
		}
		
		if (option.getType() == PatrolQueryOptionType.STRING){
			if (option == PatrolQueryOption.PATROL_TYPE){
				String p1 = engine.addParameterValue(SmartUtils.stripQuotes((String)filter.getValue()) );
				String x = prefix + "." + option.getColumnName() + " = " + p1; //$NON-NLS-1$ //$NON-NLS-2$ 
				return x;				
			}else{
				String value1 = SmartUtils.stripQuotes((String)filter.getValue());
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
				String value2 = SmartUtils.stripQuotes((String)filter.getValue());
				String p1 = engine.addParameterValue(UuidUtils.stringToUuid(value2));
				String x = prefix + "." + option.getColumnName() + " = " + p1; //$NON-NLS-1$ //$NON-NLS-2$ 
				return x;
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
			
		}else if (option.getType() == PatrolQueryOptionType.KEY){
			String key = SmartUtils.stripQuotes((String)filter.getValue());
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
					String p1 = engine.addParameterValue(UuidUtils.stringToUuid(SmartUtils.stripQuotes(filter.getValue())));
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
	 * Date Filter
	 */
	@Override
	protected String asSql(DateFilter filter, IQueryEngine engine) throws SQLException{
		String table = ""; //$NON-NLS-1$
		String field = ""; //$NON-NLS-1$
		
		if (filter.getDateFieldOption() == PatrolEndDateField.INSTANCE){
			table = engine.tablePrefix(Patrol.class);
			field = "end_date"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == PatrolStartDateField.INSTANCE){
			table = engine.tablePrefix(Patrol.class);
			field = "start_date"; //$NON-NLS-1$
		}else if (filter.getDateFieldOption() == WaypointDateField.INSTANCE){
			table = engine.tablePrefix(PatrolLegDay.class);
			field = "patrol_day"; //$NON-NLS-1$
		}else{
			throw new SQLException(MessageFormat.format(Messages.DerbyFilterToSqlGenerator_DateFilteNotSupported, new Object[]{filter.getDateFieldOption().getGuiName()}));
		}
		
		field = table + "." + field; //$NON-NLS-1$
		
		java.sql.Date[] bits = filter.getDateFilterOption().getDates(); 
		String f = ""; //$NON-NLS-1$
		if (bits == null){
			return ""; //$NON-NLS-1$
		}
		if (bits.length == 1){
			String p1 = engine.addParameterValue(bits[0].toString());
			f = " ( " +field + " >= " + p1 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (bits.length == 2 && filter.getDateFilterOption().isEndDateInclusive()){
			String p1 = engine.addParameterValue(bits[0].toString());
			String p2 = engine.addParameterValue(bits[1].toString());
			f = " ( " + field + " >= " + p1 + " and " + field  + " <= " + p2 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else if (bits.length == 2){
			String p1 = engine.addParameterValue(bits[0].toString());
			String p2 = engine.addParameterValue(bits[1].toString());
			f = " ( " + field + " >= " + p1 + " and " + field  + " < " + p2 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		return f;
	}
	
}
