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
package org.wcs.smart.query.common.engine;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.internal.Messages;
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
		}
		throw new SQLException(MessageFormat.format(Messages.DerbyFilterToSqlGenerator_FilterTypeNotSupported, new Object[]{filter.getClass().getCanonicalName()}));
	}

	
	/*
	 * Observer source filter
	 */
	protected String asSql(ObserverFilter filter, IQueryEngine engine) throws SQLException{
		try {
			String param = engine.addParameterValue(SmartUtils.decodeHex(filter.getValue())); 
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
		}
		return sb.toString();
	}
	
	/*
	 * Attribute filter
	 */
	protected String asSql(AttributeFilter filter, IQueryEngine engine) throws SQLException{
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
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION.getKey())){
				//any option
				return "( qa."+ filter.getAttributeKey()  + " is not null )";  //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				String p = engine.addParameterValue(filter.getValue()); 
				return "( qa."+ filter.getAttributeKey()  + " " + asSql(filter.getOperator()) + " " + p + ")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			}
		}else if (filter.getAttributeType() == AttributeType.TREE){
			String p1 = engine.addParameterValue(filter.getValue()); 
			String p2 = engine.addParameterValue(((String)filter.getValue()).substring(0,  ((String)filter.getValue()).length() -1) + "/"); //$NON-NLS-1$ 
			return "( qa." + filter.getAttributeKey() + " >= " + p1 + " and qa." + filter.getAttributeKey() + "< " + p2 + " )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
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
		String keyPart = filter.getCategoryKey();
		String prefix = engine.tablePrefix(Category.class);
		if (prefix == null){
			throw new IllegalStateException(Messages.CategoryFilter_InvalidPrefix);
		}
		
		String p1 = engine.addParameterValue(keyPart); 
		String p2 = engine.addParameterValue(keyPart.substring(0,  keyPart.length() -1) + "/"); //$NON-NLS-1$ 
		return "( " + prefix + ".hkey >= "+ p1 + " and " + prefix + ".hkey < " + p2 + ") "; //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$	
	}
	
	/*
	 * Category attribute filter
	 */
	protected String asSql(CategoryAttributeFilter filter, IQueryEngine engine) throws SQLException{
		return "( " + toSql(filter.getCategoryFilter(), engine) + asSql(Operator.AND) + toSql(filter.getAttributeFilter(), engine) + " )"; //$NON-NLS-1$ //$NON-NLS-2$	
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
			table = engine.tablePrefix(Waypoint.class);
			field = "datetime"; //$NON-NLS-1$
		}else{
			throw new SQLException(MessageFormat.format(Messages.DerbyFilterToSqlGenerator_DateFilteNotSupported, new Object[]{filter.getDateFieldOption().getGuiName()}));
		}
		
		field = table + "." + field; //$NON-NLS-1$
		
		java.sql.Date[] bits = filter.getDateFilterOption().getDates(); 
		String f = ""; //$NON-NLS-1$
		if (bits == null){
			return ""; //$NON-NLS-1$
		}

		String p1 = engine.addParameterValue(bits[0].toString());
		if (bits.length == 1){
			f = " ( cast(" + field + " as date) >= " + p1 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		}else if (bits.length == 2 && filter.getDateFilterOption().isEndDateInclusive()){
			String p2 = engine.addParameterValue(bits[1].toString());
			f = " ( cast(" + field + " as date) >= "+ p1 + " and cast(" + field  + " as date) <= " + p2 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else if (bits.length == 2){
			String p2 = engine.addParameterValue(bits[1].toString());
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
		throw new SQLException(MessageFormat.format(Messages.DerbyFilterToSqlGenerator_OperatorNotSupported, new Object[]{op.getGuiValue()}));
	}
}
