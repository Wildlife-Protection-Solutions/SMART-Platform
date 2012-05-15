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
package org.wcs.smart.query;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class QueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.query"; //$NON-NLS-1$

	/**
	 * The small 8x8 delete icon
	 */
	public static final String DELETE_MINI_ICON = "org.wcs.smart.query.deleteminiicon";
	
	/**
	 * The main query icon
	 */
	public static final String QUERY_ICON = "org.wcs.smart.query.queryicon";
	
	/**
	 * The query folder
	 */
	public static final String FOLDER_ICON = "org.wcs.smart.query.folder";
	
	/**
	 * The waypoint query
	 */
	public static final String WAYPOINT_QUERY_ICON = "org.wcs.smart.query.waypointquery";
	
	/**
	 * The waypoint query
	 */
	public static final String SUMMARY_QUERY_ICON = "org.wcs.smart.query.summaryquery";
	
	
	/**
	 * The calendar
	 */
	public static final String CALENDAR_ICON = "org.wcs.smart.query.calendar";
	
	/**
	 * The calendar day icon
	 */
	public static final String CALENDAR_DAY_ICON = "org.wcs.smart.query.calendarday";
	
	/**
	 * The calendar week icon
	 */
	public static final String CALENDAR_WEEK_ICON = "org.wcs.smart.query.calendarweek";
	
	/**
	 * The calendar month icon
	 */
	public static final String CALENDAR_MONTH_ICON = "org.wcs.smart.query.calendarmonth";
	
	/**
	 * The calendar year icon
	 */
	public static final String CALENDAR_YEAR_ICON = "org.wcs.smart.query.calendaryear";
	
	/**
	 * The group by icon
	 */
	public static final String GROUPBY_ICON = "org.wcs.smart.query.groupby";
	
	/**
	 * The value icon
	 */
	public static final String VALUE_ICON = "org.wcs.smart.query.value";
	

	
	/*
	 * Load images
	 */
	static {
		ImageDescriptor descriptor = AbstractUIPlugin
				.imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/delete.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(DELETE_MINI_ICON, descriptor);
		}

		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/querypatrol.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(QUERY_ICON, descriptor);
		}

		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/summary_query.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(SUMMARY_QUERY_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/folder.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(FOLDER_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/waypoint_query.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(WAYPOINT_QUERY_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/calendar.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(CALENDAR_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/calendar_day.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(CALENDAR_DAY_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/calendar_week.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(CALENDAR_WEEK_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/calendar_month.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(CALENDAR_MONTH_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/calendar_year.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(CALENDAR_YEAR_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/group_by.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(GROUPBY_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
				"images/icons/obj16/values.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(VALUE_ICON, descriptor);
		}
	}
	
	// The shared instance
	private static QueryPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public QueryPlugIn() {
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
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
	public static QueryPlugIn getDefault() {
		return plugin;
	}

	/**
	 * Logs the given error to the error log.
	 * 
	 * @param message message 
	 * @param t error
	 */
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	/**
	 * Logs the given error to the error log.
	 * 
	 * @param message message 
	 * @param t error
	 */
	public static void logSql(String sql){
		int status = IStatus.INFO;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, sql, null));
	}
	
	/**
	 * Displays an error message to the user and logs the
	 * message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message);
			}});
		
	}
}
