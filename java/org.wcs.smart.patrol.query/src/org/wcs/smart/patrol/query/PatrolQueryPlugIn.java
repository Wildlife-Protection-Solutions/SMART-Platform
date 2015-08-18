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
package org.wcs.smart.patrol.query;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.patrol.query.ext.IPatrolContributionFinder;
import org.wcs.smart.patrol.query.ext.PatrolContributionFinder;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IPatrolQueryColumnProvider;
import org.wcs.smart.patrol.query.model.observation.PatrolQueryColumnProvider;
import org.wcs.smart.patrol.query.ui.PatrolQueryLabelProvider;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;

/**
 * The activator class controls the plug-in life cycle
 */
public class PatrolQueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.patrol.query"; //$NON-NLS-1$

	// The shared instance
	private static PatrolQueryPlugIn plugin;

	/**
	 * The waypoint query
	 */
	public static final String WAYPOINT_QUERY_ICON = "org.wcs.smart.query.waypointquery"; //$NON-NLS-1$

	/**
	 * The observation query
	 */
	public static final String OBSERVATION_QUERY_ICON = "org.wcs.smart.query.observationquery"; //$NON-NLS-1$

	/**
	 * The patrol query
	 */
	public static final String PATROL_QUERY_ICON = "org.wcs.smart.query.patrolquery"; //$NON-NLS-1$

	/**
	 * The summary query
	 */
	public static final String SUMMARY_QUERY_ICON = "org.wcs.smart.query.summaryquery"; //$NON-NLS-1$

	/**
	 * The gridded query
	 */
	public static final String GRIDDED_SUMMARY_QUERY_ICON = "org.wcs.smart.query.griddedquery"; //$NON-NLS-1$

	/**
	 * The value number of employees icon
	 */
	public static final String VALUE_NUM_EMPLOYEES_ICON = "org.wcs.smart.query.valuenumemployees"; //$NON-NLS-1$
	
	/**
	 * The value number of patrols icon
	 */
	public static final String VALUE_NUM_PATROLS_ICON = "org.wcs.smart.query.valuenumpatrols"; //$NON-NLS-1$

	/**
	 * Person days icons
	 */
	public static final String VALUE_PERSON_DAYS_ICON = "org.wcs.smart.query.valuepersondays"; //$NON-NLS-1$

	/**
	 * Person hours icons
	 */
	public static final String VALUE_PERSON_HOURS_ICON = "org.wcs.smart.query.valuepersonhours"; //$NON-NLS-1$

	/**
	 * The constructor
	 */
	public PatrolQueryPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		SmartContext.INSTANCE.setClass(IPatrolContributionFinder.class, new PatrolContributionFinder());
		SmartContext.INSTANCE.setClass(IQueryPatrolLabelProvider.class, new PatrolQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IPatrolQueryColumnProvider.class, new PatrolQueryColumnProvider());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static PatrolQueryPlugIn getDefault() {
		return plugin;
	}

	@Override
	public void initializeImageRegistry(ImageRegistry reg) {
		reg.put(SUMMARY_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/summary_query.png"));//$NON-NLS-1$
		reg.put(WAYPOINT_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/waypoint_query.png"));//$NON-NLS-1$
		reg.put(OBSERVATION_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/observation_query.png"));//$NON-NLS-1$
		reg.put(PATROL_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/patrol_query.png"));//$NON-NLS-1$
		reg.put(GRIDDED_SUMMARY_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/gridded_query.png"));//$NON-NLS-1$
		reg.put(VALUE_NUM_EMPLOYEES_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/value_numEmployees.png"));//$NON-NLS-1$
		reg.put(VALUE_NUM_PATROLS_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/value_numPatrol.png"));//$NON-NLS-1$
		reg.put(VALUE_PERSON_DAYS_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/value_personDays.png"));//$NON-NLS-1$
		reg.put(VALUE_PERSON_HOURS_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/value_personHours.png"));//$NON-NLS-1$
		super.initializeImageRegistry(reg);
	}

	/**
	 * Logs the given error to the error log.
	 * 
	 * @param message
	 *            message
	 * @param t
	 *            error
	 */
	public static void log(String message, Throwable t) {
		int status = t instanceof Exception || message != null ? IStatus.ERROR
				: IStatus.WARNING;
		getDefault().getLog().log(
				new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}

	/**
	 * Displays an error message to the user and logs the message.
	 * 
	 * @param message
	 *            Error message to display
	 * @param t
	 *            exception to log
	 */
	public static void displayLog(final String message, Throwable t) {
		log(message, t);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						Messages.QueryPlugIn_Error_DialogTitle, message);
			}
		});
	}


}
