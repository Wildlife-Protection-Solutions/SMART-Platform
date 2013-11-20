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
package org.wcs.smart.patrol.query.engine;


import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
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
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.util.SmartUtils;

/**
 * Converts filters to sql for the Derby query engine.
 * 
 * @author Emily
 *
 */
public class DerbyFilterToSqlGenerator {

	/**
	 * converts the given filter to sql
	 * 
	 * @param filter the filter to convert
	 * @param engine the query engine
	 * @return sql string
	 * @throws SQLException
	 */
	public static String toSql(IFilter filter, DerbyQueryEngine2 engine) throws SQLException{
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
		}else if (filter instanceof PatrolFilter){
			return asSql((PatrolFilter)filter, engine);
		}else if (filter instanceof ConservationAreaFilter){
			return asSql((ConservationAreaFilter)filter, engine.tablePrefix(Patrol.class));
		}else if (filter instanceof DateFilter){
			return asSql((DateFilter)filter, engine);
		}else if (filter instanceof IExtensionFilter){
			return asSql((IExtensionFilter)filter, engine);
		}
		
		throw new SQLException(MessageFormat.format(Messages.DerbyFilterToSqlGenerator_FilterTypeNotSupported, new Object[]{filter.getClass().getCanonicalName()}));
		
	}
	
	
	/*
	 * Area filter
	 */
	private static String asSql(AreaFilter filter, DerbyQueryEngine2 engine){
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
	private static String asSql(AttributeFilter filter, DerbyQueryEngine2 engine) throws SQLException{
		String col = engine.filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
	
		String attprefix = engine.tablePrefix(Attribute.class);
		if (attprefix == null){
			throw new IllegalStateException(Messages.AttributeFilter_InvalidAttributePrefix);
		}
		String attObprefix = engine.tablePrefix(WaypointObservationAttribute.class);
		if (attObprefix == null){
			throw new IllegalStateException(Messages.AttributeFilter_InvalidWaypointObservationPrefix);
		}

		if (filter.getAttributeType() == AttributeType.BOOLEAN){
			return " (qa." + filter.getAttributeKey() + " > 0.5 ) ";			//$NON-NLS-1$ //$NON-NLS-2$
		}else if (filter.getAttributeType() == AttributeType.NUMERIC){
			return " (qa." + filter.getAttributeKey() + " " + asSql(filter.getOperator()) + " " + String.valueOf((Double)filter.getValue()) + ") "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}else if (filter.getAttributeType() == AttributeType.TEXT){
			String queryStr = ""; //$NON-NLS-1$
			//TODO: look into escape % & _ as these are wild card characters
			// SELECT a FROM tabA WHERE a LIKE '%=_' ESCAPE '='  (must specify escape character)
			String val = (String)filter.getValue();
			val = val.replaceAll("'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				queryStr = "( LOWER(qa." + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " '%" + val.toLowerCase() + "%' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$	
			}else if (filter.getOperator() == Operator.STR_EQUALS){
				queryStr = "( LOWER(qa." + filter.getAttributeKey() + ") " + asSql(filter.getOperator()) + " '" + val.toLowerCase() + "' )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			return queryStr;
		}else if (filter.getAttributeType() == AttributeType.LIST ){
			return "( qa."+ filter.getAttributeKey()  + " " + asSql(filter.getOperator()) + " '" + (String)filter.getValue() + "' )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
		}else if (filter.getAttributeType() == AttributeType.TREE){
			return "( qa." + filter.getAttributeKey() + " >= '" + (String)filter.getValue()+ "' and qa." + filter.getAttributeKey() + "<'" + ((String)filter.getValue()).substring(0,  ((String)filter.getValue()).length() -1) + "/')";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
		}
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * boolean filter
	 */
	private static String asSql(BooleanExpression filter, DerbyQueryEngine2 engine) throws SQLException{
		String part1 = toSql(filter.getFilter1(), engine);
		String part2 = toSql(filter.getFilter2(), engine);
		return part1 + " " + asSql(filter.getOperator()) + " " + part2; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * bracket filter
	 */
	private static String asSql(BracketFilter filter, DerbyQueryEngine2 engine) throws SQLException{
		return "( " + toSql(filter.getFilter(), engine) + " )"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * Category filter
	 */
	private static String asSql(CategoryFilter filter, DerbyQueryEngine2 engine) throws SQLException{
		String col = engine.filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null ";  //$NON-NLS-1$
		}
		
		String keyPart = filter.getCategoryKey();
		String prefix = engine.tablePrefix(Category.class);
		if (prefix == null){
			throw new IllegalStateException(Messages.CategoryFilter_InvalidPrefix);
		}
		return "( " + prefix + ".hkey >= '" + keyPart + "' and " + prefix + ".hkey < '" + keyPart.substring(0,  keyPart.length() -1) + "/') "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$	
	}
	
	/*
	 * Category attribute filter
	 */
	private static String asSql(CategoryAttributeFilter filter, DerbyQueryEngine2 engine) throws SQLException{
		String col = engine.filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		return "( " + toSql(filter.getCategoryFilter(), engine) + asSql(Operator.AND) + toSql(filter.getAttributeFilter(), engine) + " )"; //$NON-NLS-1$ //$NON-NLS-2$	
	}
	
	/*
	 * not expression
	 */
	private static String asSql(NotExpression filter, DerbyQueryEngine2 engine) throws SQLException{
		return asSql(Operator.NOT) + " ( " + toSql(filter.getFilter(), engine) + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
	}
	
	/*
	 * not expression
	 */
	private static String asSql(IExtensionFilter filter, DerbyQueryEngine2 engine) throws SQLException{
		return PatrolContributionFactory.getSql(DerbyQueryEngine2.tablePrefix, filter);	
	}
	
	/**
	 * Converts a conservation area filter to sql
	 * 
	 * @param filter the filter
	 * @param caTablePrefix the prefix of the table name that contains the ca_uuid field to filter on
	 * @return
	 * @throws SQLException
	 */
	public static String asSql(ConservationAreaFilter filter, String caTablePrefix) throws SQLException{
		ArrayList<byte[]> localFilters = new ArrayList<byte[]>();
		if (filter.includeAll()){
			//include all current conservation areas
			if (SmartDB.getConservationAreaConfiguration() != null){
				for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
					localFilters.add(ca.getUuid());
				}
			}else{
				localFilters.add(SmartDB.getCurrentConservationArea().getUuid());
			}
		}else{
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
			String uuid = SmartUtils.encodeHex(localFilters.get(i));
			sb.append("x'" + uuid + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}
	
	/*
	 * Patrol Filter
	 */
	private static String asSql(PatrolFilter filter, DerbyQueryEngine2 engine) throws SQLException{
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
			x += " employee_uuid = x'" + value2 + "')"; //$NON-NLS-1$ //$NON-NLS-2$
			return x;			
		}		
		String prefix = engine.tablePrefix(filter.getPatrolOption().getPatrolAttributeClass());
		if (prefix == null){
			throw new SQLException(MessageFormat.format(
					Messages.PatrolFilter_InvalidPrefix, new Object[]{ filter.getPatrolOption().getKey()}));
		}
		
		if (option.getType() == PatrolQueryOptionType.STRING){
			if (option == PatrolQueryOption.PATROL_TYPE){
				String x = prefix + "." + option.getColumnName() + " = '" + SmartUtils.stripQuotes((String)filter.getValue()) + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return x;				
			}else{
				String value1 = SmartUtils.stripQuotes((String)filter.getValue());
				if (filter.getOperator() == Operator.STR_CONTAINS || 
						filter.getOperator() == Operator.STR_NOTCONTAINS){
					value1 = "%" + value1 + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				String x = "LOWER(" + prefix + "." + option.getColumnName() + ") " + asSql(filter.getOperator()) + " '" + value1.toLowerCase() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
				String x = prefix + "." + option.getColumnName() + " = x'" + value2 + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return x;
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
			
		}else if (option.getType() == PatrolQueryOptionType.KEY){
			String key = SmartUtils.stripQuotes((String)filter.getValue());
			return prefix + "." + option.getColumnName() + " IN ( select uuid from " + engine.tableName(option.getSourceClass()) + " where keyid = '" + key + "') "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			
		}
		return ""; //$NON-NLS-1$	
	}
	
	/*
	 * Date Filter
	 */
	private static String asSql(DateFilter filter, DerbyQueryEngine2 engine) throws SQLException{
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
			f = " ( " +field + " >= '" + bits[0].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (bits.length == 2 && filter.getDateFilterOption().isEndDateInclusive()){ 
			f = " ( " + field + " >= '" + bits[0].toString() + "' and " + field  + " <= '" + bits[1].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else if (bits.length == 2){
			f = " ( " + field + " >= '" + bits[0].toString() + "' and " + field  + " < '" + bits[1].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
		}
		throw new SQLException(MessageFormat.format(Messages.DerbyFilterToSqlGenerator_OperatorNotSupported, new Object[]{op.getGuiValue()}));
	}
}
