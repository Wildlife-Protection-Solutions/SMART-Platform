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
package org.wcs.smart.patrol.query.parser;

import java.sql.SQLException;

import org.wcs.smart.patrol.query.model.IExtensionOption;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;

/**
 * Extension point for providing group by contributions to
 * patrol queries.
 * 
 * @author Emily
 *
 */
public interface IGroupByPatrolContribution {

	/**
	 * Creates the group by item for the extension.  Returns
	 * null if extension does not support the given key.
	 * Keys are of the format "patrol:contribution:<key>:<items:>
	 * 
	 * @param key the contribution key 
	 * @return group by item or null if key not supported
	 */
	public IExtensionGroupBy createGroupBy(String key);
	
	/**
	 * 
	 * @return the extension option 
	 */
	public IExtensionOption getOption();
	
	
	/**
	 * Creates the group by sql for the 
	 * given group by item.  
	 * <p>
	 * Should do nothing if
	 * the IGroupBy is not supported by the extension point.
	 * </p>
	 * @param groupBy 
	 * @param fromSql the from clause
	 * @param groupBySql the 'outer' group by clause 
	 * @param groupByInnerSql the 'inner' group by clause
	 * @param value the value being computed
	 * @param caFilter the ca filter
	 * @param itemCnt the current group by count
	 * @param engine query engine
	 * @return
	 * @throws SQLException
	 */
	public void addGroupBySql(IGroupBy groupBy,
			StringBuilder fromSql,
			StringBuilder groupBySql, 
			StringBuilder groupByInnerSql, 
			IValueItem value, ConservationAreaFilter caFilter,
			int itemCnt,
			IQueryEngine engine) throws SQLException;

}
