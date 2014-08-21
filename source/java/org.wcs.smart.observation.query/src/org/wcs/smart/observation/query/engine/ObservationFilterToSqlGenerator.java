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
package org.wcs.smart.observation.query.engine;


import java.sql.SQLException;
import java.util.ArrayList;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.query.model.filter.ObserverFilter;
import org.wcs.smart.observation.query.model.filter.WaypointSourceFilter;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.CategoryAttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.util.SmartUtils;

/**
 * Converts filters to sql for the Derby query engine.
 * 
 * @author Emily
 *
 */
public class ObservationFilterToSqlGenerator extends DerbyFilterToSqlGenerator  {

	public static final ObservationFilterToSqlGenerator INSTANCE = new ObservationFilterToSqlGenerator();
	
	private ObservationFilterToSqlGenerator(){
		
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
			return asSql((ConservationAreaFilter)filter, engine.tablePrefix(Waypoint.class));
		}else if (filter instanceof WaypointSourceFilter){
			return asSql((WaypointSourceFilter)filter, engine);
		}else if (filter instanceof ObserverFilter){
			return asSql((ObserverFilter)filter, engine);
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
		//TODO: escape waypoint source key
		sb.append(" '" + SmartUtils.stripQuotes(filter.getWaypointSourceKey()) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		return sb.toString();
	}
	
	/*
	 * Observer source filter
	 */
	protected String asSql(ObserverFilter filter, IQueryEngine engine) throws SQLException{
		StringBuilder sb = new StringBuilder();
		sb.append(engine.tablePrefix(WaypointObservation.class));
		sb.append(".employee_uuid "); //$NON-NLS-1$
		sb.append(" = x'"); //$NON-NLS-1$
		sb.append(filter.getValue());
		sb.append("'"); //$NON-NLS-1$
		return sb.toString();
	}
	
	/*
	 * Attribute filter
	 */
	@Override
	protected String asSql(AttributeFilter filter, IQueryEngine engine) throws SQLException{
		String col = ((DerbyObservationQueryEngine)engine).filterTables.get(filter);
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
		String col = ((DerbyObservationQueryEngine)engine).filterTables.get(filter);
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
		String col = ((DerbyObservationQueryEngine)engine).filterTables.get(filter);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		return super.asSql(filter, engine);	
	}
	
	/**
	 * Converts a conservation area filter to sql
	 * 
	 * @param filter the filter
	 * @param caTablePrefix the prefix of the table name that contains the ca_uuid field to filter on
	 * @return
	 * @throws SQLException
	 */
	@Override
	public String asSql(ConservationAreaFilter filter, String caTablePrefix) throws SQLException{
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
}
