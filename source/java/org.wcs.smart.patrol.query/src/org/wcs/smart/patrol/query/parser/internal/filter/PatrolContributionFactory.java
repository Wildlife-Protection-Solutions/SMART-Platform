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
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.IGroupByPatrolContribution;
import org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.summary.IGroupBy;

/**
 * Factory for providing all patrol contribution related information added via extension point.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolContributionFactory {
	
	/**
	 * Extension id
	 */
	public static final String EXTENSION_ID = "org.wcs.smart.patrol.query.contribution"; //$NON-NLS-1$
	
	
	private static List<IQueryFilterPatrolContribution> filterContributions = null;
	private static List<IGroupByPatrolContribution> groupByContributions = null;
	
	private PatrolContributionFactory() {}

	public static List<IQueryFilterPatrolContribution> getFilterContributions() {
		if (filterContributions == null) {
			if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
			List<IQueryFilterPatrolContribution> items = new ArrayList<IQueryFilterPatrolContribution>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					if (e.getName().equals("FilterItem")){ //$NON-NLS-1$
						IQueryFilterPatrolContribution contribution = (IQueryFilterPatrolContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
						items.add(contribution);
					}
				}
				filterContributions = items;
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
				return Collections.emptyList();
			}
		}
		return filterContributions;
	}
	
	public static List<IGroupByPatrolContribution> getGroupByContributions() {
		if (groupByContributions == null) {
			if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
			List<IGroupByPatrolContribution> items = new ArrayList<IGroupByPatrolContribution>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					if (e.getName().equals("GroupByItem")){ //$NON-NLS-1$
						IGroupByPatrolContribution contribution = (IGroupByPatrolContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
						items.add(contribution);
					}
				}
				groupByContributions = items;
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
				return Collections.emptyList();
			}
		}
		return groupByContributions;
	}
	
	/**
	 * Key is of the form:
	 * patrol:contribution:key:items
	 * @param key
	 * @return
	 */
	public static IGroupBy createGroupBy(String key){
		for (IGroupByPatrolContribution contribution: getGroupByContributions()){
			IGroupBy gb = contribution.createGroupBy(key);
			if (gb != null){
				return gb;
			}
		}
		return null;
	}
	
	/**
	 * Key is of the form:
	 * patrol:contribution:key:items
	 * @param key
	 * @return
	 */
	public static IGroupByPatrolContribution findGroupByContribution(String key){
		for (IGroupByPatrolContribution contribution: getGroupByContributions()){
			IGroupBy gb = contribution.createGroupBy(key);
			if (gb != null){
				return contribution;
			}
		}
		return null;
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
		for (IQueryFilterPatrolContribution contribution : getFilterContributions()) {
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
		for (IQueryFilterPatrolContribution contribution : getFilterContributions()) {
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
		for (IQueryFilterPatrolContribution contribution : getFilterContributions()) {
			String sql = contribution.asSql(engine, filter);
			if (sql != null){
				return sql;
			}
		}
		return null;
	}
	
}
