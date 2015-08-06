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
package org.wcs.smart.entity.query.engine;


import java.sql.SQLException;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.query.model.filter.WaypointSourceFilter;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.CategoryAttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * Converts filters to sql for the Derby query engine.
 * 
 * @author Emily
 *
 */
public class EntityFilterToSqlGenerator extends DerbyFilterToSqlGenerator  {

	public static final EntityFilterToSqlGenerator INSTANCE = new EntityFilterToSqlGenerator();
	
	private EntityFilterToSqlGenerator(){
		
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
		if (filter instanceof ConservationAreaFilter){
			return asSql((ConservationAreaFilter)filter, engine.tablePrefix(Waypoint.class), engine);
		}else if (filter instanceof EntityAttributeFilter){
			return asSql((EntityAttributeFilter)filter, engine);
		}else if (filter instanceof WaypointSourceFilter){
			return asSql((WaypointSourceFilter)filter, engine);
		}
		return super.toSql(filter, engine);
		
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
		String col = ((DerbyEntityQueryEngine)engine).filterTables.get(filter);
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
			String p1 = engine.addParameterValue((String)filter.getValue());
			String p2 = engine.addParameterValue(((String)filter.getValue()).substring(0,  ((String)filter.getValue()).length() -1) + "/"); //$NON-NLS-1$
			return "( " + tableName + ".value >= " + p1 + " and " + tableName + ".value < " + p2 + ")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		return "";  //$NON-NLS-1$
	}
	
	
	/*
	 * Attribute filter
	 */
	@Override
	protected String asSql(AttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((DerbyEntityQueryEngine)engine).filterTables.get(filter);
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
		String col = ((DerbyEntityQueryEngine)engine).filterTables.get(filter);
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
		String col = ((DerbyEntityQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		return super.asSql(filter, engine);	
	}
}
