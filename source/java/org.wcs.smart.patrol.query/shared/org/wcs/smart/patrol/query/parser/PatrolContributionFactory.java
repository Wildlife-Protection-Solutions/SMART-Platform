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

import java.util.List;

import org.wcs.smart.SmartContext;
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
			filterContributions = SmartContext.INSTANCE.getClass(IPatrolContributionFinder.class).getFilterContributions();
		}
		return filterContributions;
	}
	
	public static List<IGroupByPatrolContribution> getGroupByContributions() {
		if (groupByContributions == null) {
			groupByContributions = SmartContext.INSTANCE.getClass(IPatrolContributionFinder.class).getGroupByContributions();
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
