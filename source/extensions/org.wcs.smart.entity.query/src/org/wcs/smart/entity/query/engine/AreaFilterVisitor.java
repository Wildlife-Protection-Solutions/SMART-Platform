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

import java.util.HashSet;

import org.wcs.smart.ca.Area;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

/**
 * Filter processor for processing area filters.  Adds the
 * necessary table joins to the provided sql string.
 * 
 * @author Emily
 *
 */
public class AreaFilterVisitor implements IFilterVisitor{

	private HashSet<String> addedTableNames = new HashSet<String>();
	private StringBuilder sql;
	private IQueryEngine engine;
	
	/**
	 * Creates a new visitor
	 * @param sql sql to append to
	 * @param engine query engine
	 * @param usedTables list of tables already added to sql (Track.class)
	 * only needs to be added once
	 */
	public AreaFilterVisitor(StringBuilder sql, IQueryEngine engine){
		this.sql = sql;
		this.engine = engine;
	}
	
	
	@Override
	public void visit(IFilter filter) {
		if (filter instanceof AreaFilter){
			AreaFilter ff = (AreaFilter)filter;
			String areaTableName = ff.getType().name() + "_" + ff.getKey(); //$NON-NLS-1$
			if (!addedTableNames.contains(areaTableName)) {
				addedTableNames.add(areaTableName);
				
				String p1 = engine.addParameterValue(ff.getType().name());
				String p2 = engine.addParameterValue(ff.getKey());
				
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(engine.tableName(Area.class));
				sql.append(" as "); //$NON-NLS-1$
				sql.append( areaTableName);
				sql.append(" on "); //$NON-NLS-1$
				sql.append( areaTableName +".ca_uuid = " + engine.tablePrefix(Waypoint.class) + ".ca_uuid and "); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append( areaTableName +".area_type = " + p1 + " and "); //$NON-NLS-1$ //$NON-NLS-2$ 
				sql.append(areaTableName + ".keyid = " + p2 + " "); //$NON-NLS-1$ //$NON-NLS-2$
				
				
			}
		}
	}

}
