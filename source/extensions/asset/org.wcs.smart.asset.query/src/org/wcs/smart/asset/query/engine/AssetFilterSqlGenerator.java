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

import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
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
			return asSql((AssetFilter)filter, engine);
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
	@Override
	protected String asSql(AttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((AssetQueryEngine)engine).filterTables.get(filter);
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
		String col = ((AssetQueryEngine)engine).filterTables.get(filter);
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
		String col = ((AssetQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		return super.asSql(filter, engine);	
	}
		
	/*
	 * Asset Filter
	 */
	protected String asSql(AssetFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((AssetQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		throw new SQLException(Messages.AssetFilterSqlGenerator_AssetFilterError);	
	}
	
	
	/*
	 * Date Filter
	 */
	@Override
	protected String asSql(DateFilter filter, IQueryEngine engine) throws SQLException{
		String table = engine.tablePrefix(Waypoint.class);
		String field = "datetime"; //$NON-NLS-1$
		
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
			f = " ( cast( " + field + " as date ) >= " + p1 + " and cast( " + field  + " as date) <= " + p2 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else if (bits.length == 2){
			String p1 = engine.addParameterValue(bits[0].toString());
			String p2 = engine.addParameterValue(bits[1].toString());
			f = " ( cast ( " + field + " as date ) >= " + p1 + " and cast( " + field  + " as date ) < " + p2 + " ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		return f;
	}
	
}
