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
package org.wcs.smart.asset.query.engine;

import java.sql.SQLException;

import org.locationtech.jts.io.WKBReader;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.common.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.AreaFilter.AreaFilterGeometryType;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Converts filters to sql for the Derby query engine.
 * 
 * @author Emily
 *
 */
public class AssetFilterSqlGenerator extends DerbyFilterToSqlGenerator{

	
	public static final AssetFilterSqlGenerator INSTANCE = new AssetFilterSqlGenerator();
	
	
	private AssetFilterSqlGenerator(){
		
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
		if (filter instanceof AssetFilter){
			return toSql((AssetFilter)filter, engine);
		}else if (filter instanceof ConservationAreaFilter){
			return asSql((ConservationAreaFilter)filter, engine.tablePrefix(Waypoint.class), engine);
		}else{
			return super.toSql(filter, engine); 
		}
	}
	
	
	/*
	 * Area filter
	 */
	@Override
	protected String toSql(AreaFilter filter, IQueryEngine engine){
		StringBuilder sb = new StringBuilder();
		if (filter.getType() != null) {
			if (filter.getGeometryType() == AreaFilterGeometryType.WAYPOINT){
				sb.append("smart.pointinpolygon(" );  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Waypoint.class) + ".x, ");  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Waypoint.class) + ".y, ");  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Waypoint.class) + ".distance, ");  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Waypoint.class) + ".direction, ");  //$NON-NLS-1$
				sb.append( filter.getType().name() + "_" + filter.getKey() + ".geom");  //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(")");  //$NON-NLS-1$
			}
		}else {
			if (filter.getGeometryType() == AreaFilterGeometryType.WAYPOINT) {
				sb.append("smart.pointinpolygon(" );  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Waypoint.class) + ".x, ");  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Waypoint.class) + ".y, ");  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Waypoint.class) + ".distance, ");  //$NON-NLS-1$
				sb.append(engine.tablePrefix(Waypoint.class) + ".direction, ");  //$NON-NLS-1$
				String param = engine.addParameterValue(WKBReader.hexToBytes(filter.getCustomArea()));
				sb.append( param );
				sb.append(")");  //$NON-NLS-1$
			}
		}
		return sb.toString();
	}
	

	/*
	 * Asset Filter
	 */
	protected String toSql(AssetFilter filter, IQueryEngine engine) throws SQLException{
		FilterTable t = ((AssetQueryEngine)engine).filterTables.get(filter);
		if (t != null) {
			return  t.tablename + "." + t.primarykey + " is not null";  //$NON-NLS-1$//$NON-NLS-2$			
		}
		
		throw new SQLException(Messages.AssetFilterSqlGenerator_AssetFilterError);	
	}
	
	
	/*
	 * Asset Attribute filter
	 */
	public String toSql(AssetAttributeFilter filter, String valuePrefix, IQueryEngine engine) throws SQLException{
		
		if (filter.getAttributeType() == AttributeType.BOOLEAN){
			return " (" + valuePrefix + ".double_value1 > 0.5 ) ";			//$NON-NLS-1$ //$NON-NLS-2$
		}else if (filter.getAttributeType() == AttributeType.NUMERIC){			
			return " (" + valuePrefix + ".double_value1 " + asSql(filter.getOperator()) + " " + engine.addParameterValue((Double)filter.getValue()) +" ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}else if (filter.getAttributeType() == AttributeType.TEXT){
			
			String queryStr = ""; //$NON-NLS-1$
			String val = (String)filter.getValue();
			if (filter.getOperator() == Operator.STR_CONTAINS || 
					filter.getOperator() == Operator.STR_NOTCONTAINS){
				String param = engine.addParameterValue("%" + val + "%"); //$NON-NLS-1$ //$NON-NLS-2$ 
				queryStr = "( LOWER(" + valuePrefix + ".string_value ) " + asSql(filter.getOperator()) + " LOWER(" + param + ") )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 	
			}else if (filter.getOperator() == Operator.STR_EQUALS){
				String param = engine.addParameterValue(val); 
				queryStr = "( LOWER(" + valuePrefix + ".string_value) " + asSql(filter.getOperator()) + " LOWER(" + param + ") )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			}
			return queryStr;
		}else if (filter.getAttributeType() == AttributeType.DATE){
			String p1 = engine.addParameterValue((String) filter.getValue()); 
			String p2 = engine.addParameterValue((String) filter.getValue2()); 
			return "( " + valuePrefix + ".string_value is not null AND DATE(" + valuePrefix + ".string_value) " + " " + asSql(filter.getOperator()) + " CAST(" + p1 + " as DATE) " + asSql(Operator.AND) + " CAST(" + p2 + " as DATE) )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		}else if (filter.getAttributeType() == AttributeType.LIST ){
			if (filter.getValue().equals(AttributeFilter.ANY_OPTION_KEY)){
				//any option
				return "( "+ valuePrefix + ".keyid is not null )";  //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				String p = engine.addParameterValue(filter.getValue()); 
				return "( "+ valuePrefix + ".keyid " + asSql(filter.getOperator()) + " " + p + ")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			}		
		}
		return ""; //$NON-NLS-1$
	}
	
	
}
