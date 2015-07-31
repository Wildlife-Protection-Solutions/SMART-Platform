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
package org.wcs.smart.intelligence.query;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.patrol.query.model.IExtensionOption;
import org.wcs.smart.patrol.query.parser.IExtensionFilter;
import org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * Intelligence contribution for the Patrol section of a "Query Filter" view.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceQueryFilterPatrolContribution implements IQueryFilterPatrolContribution {

	private PatrolIntelligenceOption intelligenceOption = new PatrolIntelligenceOption(new IntelligencePatrolQueryOption());
	
	/**
	 * @see org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution#getOptions()
	 * @return single option for filtering on intelligence
	 */
	@Override
	public List<IExtensionOption> getOptions() {
		List<IExtensionOption> ops = new ArrayList<IExtensionOption>();
		ops.add(intelligenceOption);
		return ops;
	}

	/**
	 * This option does not support filter with key only.
	 * 
	 * @return null
	 */
	@Override
	public IExtensionFilter createFilter(String key) {
		return null;
	}

	/**
	 * @see org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution#createFilter(java.lang.String, org.wcs.smart.query.model.filter.Operator, java.lang.Object)
	 * @return new PatrolIntelligenceQueryFilter
	 */
	@Override
	public IExtensionFilter createFilter(String key, Operator op, Object value) {
		String opKey = "patrol:" + intelligenceOption.getOption().getKey(); //$NON-NLS-1$
		if (opKey.equals(key)) {
			return new PatrolIntelligenceQueryFilter(intelligenceOption.getOption(), op, value);
		}
		return null;
	}
	
	/**
	 * @see org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution#asSql(java.util.HashMap, org.wcs.smart.query.model.filter.IFilter)
	 */
	@Override
	public String asSql(IQueryEngine engine, IFilter filter ){
		if (!(filter instanceof PatrolIntelligenceQueryFilter)){
			return null;
		}
		
		PatrolIntelligenceQueryFilter qFilter = (PatrolIntelligenceQueryFilter)filter;
		String prefix = engine.tablePrefix(intelligenceOption.getOption().getPatrolAttributeClass());
		String v = SharedUtils.stripQuotes((String)qFilter.getValue());
		//if v is empty this means that this is "Any Plan" case
		String intelPart = !qFilter.isAnyIntelligence(v) ? " AND p2i.intelligence_uuid = x'" + v + "'" : "";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String sql = "EXISTS (SELECT * FROM smart.patrol_intelligence p2i WHERE p2i.patrol_uuid = " + prefix + ".uuid" + intelPart + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return sql;
	}


}
