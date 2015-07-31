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
package org.wcs.smart.patrol.query.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.patrol.query.model.IExtensionOption;
import org.wcs.smart.patrol.query.parser.IGroupByPatrolContribution;
import org.wcs.smart.patrol.query.parser.IPatrolContributionFinder;
import org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.query.QueryPlugIn;

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
	
	
	public List<IQueryFilterPatrolContribution> getFilterContributions() {
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
			return items;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
			return Collections.emptyList();
		}
	}
	
	public List<IGroupByPatrolContribution> getGroupByContributions() {
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
			return items;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
			return Collections.emptyList();
		}
	}
	
	public static IExtensionOption getPatrolOptionData(Class source){
		if (Platform.getExtensionRegistry() == null) return null;
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("FilterItem")){ //$NON-NLS-1$
					IQueryFilterPatrolContribution contribution = 
							(IQueryFilterPatrolContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
					if (contribution.getClass().equals(source)){
						return (IExtensionOption)e.createExecutableExtension("FilterItemUi");
					}
				}
			}
			return null;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
			return null;
		}
	}
	public static List<IExtensionOption> getExtensions(){
		if (Platform.getExtensionRegistry() == null) return null;
		List<IExtensionOption> items = new ArrayList<IExtensionOption>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("FilterItem")){ //$NON-NLS-1$
					items.add ((IExtensionOption)e.createExecutableExtension("FilterItemUi"));
				}
			}
			return items;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.PatrolContributionFactory_ParseContribution_Error, ex);
			return null;
		}
	}
}
