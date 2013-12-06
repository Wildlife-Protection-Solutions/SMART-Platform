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
package org.wcs.smart.patrol.query.parser.internal.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;

/**
 * Factory for providing all patrol contribution related information added via extension point.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolContributionFactory {
	
	private static List<IQueryFilterPatrolContribution> contributions = null;
	
	private PatrolContributionFactory() {}

	public static List<IQueryFilterPatrolContribution> getContributions() {
		if (contributions == null) {
			if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
			List<IQueryFilterPatrolContribution> items = new ArrayList<IQueryFilterPatrolContribution>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IQueryFilterPatrolContribution.EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					IQueryFilterPatrolContribution contribution = (IQueryFilterPatrolContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
					items.add(contribution);
				}
				contributions = items;
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
				return Collections.emptyList();
			}
		}
		return contributions;
	}
	
	/**
	 * Creates a patrol filter
	 * 
	 * @param key patrol filter key
	 * @param op patrol filter operator 
	 * @param value patrol filter value
	 * @return
	 */
	public static IFilter createFilter(String key, Operator op, Object value) {
		for (IQueryFilterPatrolContribution contribution : getContributions()) {
			IFilter filter = contribution.createFilter(key, op, value);
			if (filter != null) {
				return filter;
			}
		}
		return EmptyFilter.INSTANCE;
	}
	
	/**
	 * Creates a patrol filter for a boolean patrol filter option
	 * 
	 * @param key the patrol key 
	 * @return
	 */
	public static IFilter createFilter(String key){
		for (IQueryFilterPatrolContribution contribution : getContributions()) {
			IFilter filter = contribution.createFilter(key);
			if (filter != null) {
				return filter;
			}
		}
		return EmptyFilter.INSTANCE;
	}
	
	/**
	 * Creates the sql string for the given filter
	 * which should represent a contribution item
	 *  
	 * @return the sql for the given filter or null if filter cannot be processed
	 */
	public static String getSql(IQueryEngine engine, IFilter filter){
		for (IQueryFilterPatrolContribution contribution : getContributions()) {
			String sql = contribution.asSql(engine, filter);
			if (sql != null){
				return sql;
			}
		}
		return null;
	}
	
}
