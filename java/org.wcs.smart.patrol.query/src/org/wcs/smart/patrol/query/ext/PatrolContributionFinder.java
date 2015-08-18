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
package org.wcs.smart.patrol.query.ext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.summary.IValueItem;

/**
 * Factory for providing all patrol contribution related information added via extension point.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolContributionFinder implements IPatrolContributionFinder{
	
	/**
	 * Extension id
	 */
	public static final String EXTENSION_ID = "org.wcs.smart.patrol.query.contribution"; //$NON-NLS-1$
	
	private static List<IExtensionFilterViewer> filterViewers = null;
	private static List<IExtensionGroupByViewer> groupbyViewers = null;
	
	public List<IExtensionFilter> getFilterContributions() {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IExtensionFilter> items = new ArrayList<IExtensionFilter>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("FilterItem")){ //$NON-NLS-1$
					IExtensionFilter contribution = (IExtensionFilter)e.createExecutableExtension("filter"); //$NON-NLS-1$
					items.add(contribution);
				}
			}
			return items;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
			return Collections.emptyList();
		}
	}
	
	public List<IExtensionGroupBy> getGroupByContributions() {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IExtensionGroupBy> items = new ArrayList<IExtensionGroupBy>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("GroupByItem")){ //$NON-NLS-1$
					IExtensionGroupBy contribution = (IExtensionGroupBy)e.createExecutableExtension("groupby"); //$NON-NLS-1$
					items.add(contribution);
				}
			}
			return items;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
			return Collections.emptyList();
		}
	}
	
	public static List<IExtensionFilterViewer> getFilterUiContributions() {
		if (filterViewers != null) return filterViewers;
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IExtensionFilterViewer> items = new ArrayList<IExtensionFilterViewer>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("FilterItem")){ //$NON-NLS-1$
					IExtensionFilterViewer contribution = (IExtensionFilterViewer)e.createExecutableExtension("viewer"); //$NON-NLS-1$
					items.add(contribution);
				}
			}
			filterViewers = items;
			return items;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
			return Collections.emptyList();
		}
	}
	
	public static List<IExtensionGroupByViewer> getGroupByUiContributions() {
		if (groupbyViewers != null) return groupbyViewers;
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IExtensionGroupByViewer> items = new ArrayList<IExtensionGroupByViewer>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("GroupByItem")){ //$NON-NLS-1$
					IExtensionGroupByViewer contribution = (IExtensionGroupByViewer)e.createExecutableExtension("viewer"); //$NON-NLS-1$
					items.add(contribution);
				}
			}
			groupbyViewers = items;
			return items;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
			return Collections.emptyList();
		}
	}

	/**
	 * Creates the sql string for the given filter
	 * which should represent a contribution item
	 *  
	 * @return the sql for the given filter or null if filter cannot be processed
	 */
	public static String getSql(IQueryEngine engine, Session session, IExtensionFilter filter){
		for (IExtensionFilterViewer contribution : getFilterUiContributions()) {
			if (contribution.getFilterClass().isAssignableFrom(filter.getClass())){
				return contribution.asSql(engine, session, filter);
			}
		}
		return null;
	}
	
	/**
	 * Creates the sql string for the given filter
	 * which should represent a contribution item
	 *  
	 * @return the sql for the given filter or null if filter cannot be processed
	 */
	public static void addGroupBySql(IExtensionGroupBy groupBy,
			StringBuilder fromSql,
			StringBuilder groupBySql, 
			StringBuilder groupByInnerSql, 
			IValueItem value, ConservationAreaFilter caFilter,
			int itemCnt,
			IQueryEngine engine) throws SQLException{
		for (IExtensionGroupByViewer contribution : getGroupByUiContributions()) {
			if (contribution.getGroupByClass().isAssignableFrom(groupBy.getClass())){
				contribution.addGroupBySql(groupBy, fromSql, groupBySql, groupByInnerSql, value, caFilter, itemCnt, engine);
				return;
			}
		}
	}
}
