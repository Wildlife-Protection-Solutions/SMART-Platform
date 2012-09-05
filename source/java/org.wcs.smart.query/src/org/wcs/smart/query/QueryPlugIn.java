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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationAreaManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class QueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.query"; //$NON-NLS-1$

	/**
	 * Location for raster query files to be placed
	 */
	private static final String QUERY_TEMP_FOLDER = "query_temp";
	
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
	 * The patrol query
	 */
	public static final String PATROL_QUERY_ICON = "org.wcs.smart.query.patrolquery";

	/**
	 * The summary query
	 */
	public static final String SUMMARY_QUERY_ICON = "org.wcs.smart.query.summaryquery";
	

	/**
	 * The gridded query
	 */
	public static final String GRIDDED_SUMMARY_QUERY_ICON = "org.wcs.smart.query.griddedquery";
	
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
	
	/**
	 * The value distance icon
	 */
	public static final String VALUE_DISTANCE_ICON = "org.wcs.smart.query.valuedistance";
	
	/**
	 * The value num days icon
	 */
	public static final String VALUE_NUM_DAYS_ICON = "org.wcs.smart.query.valuenumdays";
	
	/**
	 * The value number of employees icon
	 */
	public static final String VALUE_NUM_EMPLOYEES_ICON = "org.wcs.smart.query.valuenumemployees";
	/**
	 * The value number of number of hours icon
	 */
	public static final String VALUE_NUM_HOURS_ICON = "org.wcs.smart.query.valuenumhours";
	/**
	 * The value number of nights icon
	 */
	public static final String VALUE_NUM_NIGHTS_ICON = "org.wcs.smart.query.valuenumnights";
	/**
	 * The value number of patrols icon
	 */
	public static final String VALUE_NUM_PATROLS_ICON = "org.wcs.smart.query.valuenumpatrols";
	
	/**
	 * Person days icons
	 */
	public static final String VALUE_PERSON_DAYS_ICON = "org.wcs.smart.query.valuepersondays";
	
	/**
	 * Person hours icons
	 */
	public static final String VALUE_PERSON_HOURS_ICON = "org.wcs.smart.query.valuepersonhours";
	
	
	/**
	 * Exclamation
	 */
	public static final String EXCLAMATION_ICON = "org.wcs.smart.query.exclamation";
	
	/**
	 * Row Header
	 */
	public static final String ROW_HEADER_ICON = "org.wcs.smart.query.rowheader";
	
	/**
	 * Column Header
	 */
	public static final String COLUMN_HEADER_ICON = "org.wcs.smart.query.columheader";
	
	/**
	 * Area Filter Icon
	 */
	public static final String AREA_FILTER_ICON = "org.wcs.smart.query.areafilter";
	
	
	/**
	 * Area Polygon Filter Icon
	 */
	public static final String AREA_POLYGON_FILTER_ICON = "org.wcs.smart.query.areapolyfilter";

	/**
	 * Grid ICon
	 */
	public static final String GRID_ICON = "org.wcs.smart.query.grid";
	
	/**
	 * Query property extension id
	 */
	private static final String QUERY_PROPERTY_EXTENSION_ID = "org.wcs.smart.query.property";
	/*
	 * Load images
	 */
	static {
		addImage("images/icons/obj16/delete.png",DELETE_MINI_ICON);
		
		addImage("images/icons/obj16/querypatrol.gif",QUERY_ICON);
		addImage("images/icons/obj16/summary_query.png",SUMMARY_QUERY_ICON);
		addImage("images/icons/obj16/folder.png",FOLDER_ICON);
		addImage("images/icons/obj16/waypoint_query.png",WAYPOINT_QUERY_ICON);	
		addImage("images/icons/obj16/patrol_query.png",PATROL_QUERY_ICON);
		addImage("images/icons/obj16/gridded_query.png", GRIDDED_SUMMARY_QUERY_ICON);
		addImage("images/icons/obj16/grid.png", GRID_ICON);
		
		addImage("images/icons/obj16/calendar.png",CALENDAR_ICON);
		addImage("images/icons/obj16/calendar_day.png",CALENDAR_DAY_ICON);
		addImage("images/icons/obj16/calendar_week.png",CALENDAR_WEEK_ICON);
		addImage("images/icons/obj16/calendar_month.png",CALENDAR_MONTH_ICON);
			
		addImage("images/icons/obj16/calendar_year.png",CALENDAR_YEAR_ICON);
		addImage("images/icons/obj16/group_by.png",GROUPBY_ICON);
		addImage("images/icons/obj16/values.png",VALUE_ICON);
		
		addImage("images/icons/obj16/value_distance.png",VALUE_DISTANCE_ICON);
		addImage("images/icons/obj16/value_numDays.png",VALUE_NUM_DAYS_ICON);
		addImage("images/icons/obj16/value_numEmployees.png",VALUE_NUM_EMPLOYEES_ICON);
		addImage("images/icons/obj16/value_numHours.png",VALUE_NUM_HOURS_ICON);
		addImage("images/icons/obj16/value_numNights.png",VALUE_NUM_NIGHTS_ICON);
		addImage("images/icons/obj16/value_numPatrol.png",VALUE_NUM_PATROLS_ICON);
		addImage("images/icons/obj16/value_personDays.png",VALUE_PERSON_DAYS_ICON);
		addImage("images/icons/obj16/value_personHours.png",VALUE_PERSON_HOURS_ICON);
		addImage("images/icons/obj16/exclamation.png",EXCLAMATION_ICON);
		addImage("images/icons/obj16/row_header.png",ROW_HEADER_ICON);
		addImage("images/icons/obj16/column_header.png",COLUMN_HEADER_ICON);
		
		addImage("images/icons/obj16/area_filter.png",AREA_FILTER_ICON);
		addImage("images/icons/obj16/area_polygon.png",AREA_POLYGON_FILTER_ICON);	
	}
	
	private static List<AbstractQueryPropertyProvider>  propertyProviders = null;
	
	private static void addImage(String path, String icon){
		ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path); 
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(icon, descriptor);
		}
	}
	
	// The shared instance
	private static QueryPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public QueryPlugIn() {
	}

	
	public File getQueryTempDirectory(){
		return new File(SmartProperties.getInstance().getProperty(SmartProperties.FILESTORE_KEY), QUERY_TEMP_FOLDER);
	}
	
	
	
	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		//add required listeners
		ConservationAreaManager.getInstance().addDeleteHandler(new QueryCaDeleteHandler(),QueryCaDeleteHandler.EXECUTE_ORDER);
	

		//empty query temp directory
		Job j = new Job("Cleaning queries directory"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				File dir = getQueryTempDirectory();
				File[] toDel = dir.listFiles();
				for (int i = 0; i < toDel.length; i ++){
					toDel[i].delete();
				}
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
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
	
	
	
	/**
	 * This list is only loaded once
	 * @return list of query property providers
	 */
	public synchronized static List<AbstractQueryPropertyProvider> getPropertyProviders(){
		if (propertyProviders == null){
			propertyProviders = new ArrayList<AbstractQueryPropertyProvider>();
			if (Platform.getExtensionRegistry() == null) return propertyProviders;
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(QUERY_PROPERTY_EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					AbstractQueryPropertyProvider prop = (AbstractQueryPropertyProvider) e.createExecutableExtension("propertyProvider");
					prop.setName(e.getAttribute("propertyName"));
					propertyProviders.add( prop);
				}
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
		return propertyProviders;
		
	}
}
