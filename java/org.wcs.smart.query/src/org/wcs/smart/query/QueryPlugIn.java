package org.wcs.smart.query;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.AbstractQueryPropertyProvider;
import org.wcs.smart.query.ui.MultiCaQueryPerspective;
import org.wcs.smart.query.ui.QueryPerspective;

/**
 * The activator class controls the plug-in life cycle
 */
public class QueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.query"; //$NON-NLS-1$

	// The shared instance
	private static QueryPlugIn plugin;

	/**
	 * Location for raster query files to be placed
	 */
	private static final String QUERY_TEMP_FOLDER = "query_temp"; //$NON-NLS-1$
	
	/**
	 * The small 8x8 delete icon
	 */
	public static final String DELETE_MINI_ICON = "org.wcs.smart.query.deleteminiicon"; //$NON-NLS-1$
	
	/**
	 * The main query icon
	 */
	public static final String QUERY_ICON = "org.wcs.smart.query.queryicon"; //$NON-NLS-1$
	
	/**
	 * The query folder
	 */
	public static final String FOLDER_ICON = "org.wcs.smart.query.folder"; //$NON-NLS-1$
	
	/**
	 * The calendar
	 */
	public static final String CALENDAR_ICON = "org.wcs.smart.query.calendar"; //$NON-NLS-1$
	
	/**
	 * The calendar day icon
	 */
	public static final String CALENDAR_DAY_ICON = "org.wcs.smart.query.calendarday"; //$NON-NLS-1$
	
	/**
	 * The calendar week icon
	 */
	public static final String CALENDAR_WEEK_ICON = "org.wcs.smart.query.calendarweek"; //$NON-NLS-1$
	
	/**
	 * The calendar month icon
	 */
	public static final String CALENDAR_MONTH_ICON = "org.wcs.smart.query.calendarmonth"; //$NON-NLS-1$
	
	/**
	 * The calendar year icon
	 */
	public static final String CALENDAR_YEAR_ICON = "org.wcs.smart.query.calendaryear"; //$NON-NLS-1$
	
	/**
	 * The group by icon
	 */
	public static final String GROUPBY_ICON = "org.wcs.smart.query.groupby"; //$NON-NLS-1$
	
	/**
	 * The value icon
	 */
	public static final String VALUE_ICON = "org.wcs.smart.query.value"; //$NON-NLS-1$
	
	/**
	 * The value distance icon
	 */
	public static final String VALUE_DISTANCE_ICON = "org.wcs.smart.query.valuedistance"; //$NON-NLS-1$
	
	/**
	 * The value num days icon
	 */
	public static final String VALUE_NUM_DAYS_ICON = "org.wcs.smart.query.valuenumdays"; //$NON-NLS-1$
	
	/**
	 * The value number of number of hours icon
	 */
	public static final String VALUE_NUM_HOURS_ICON = "org.wcs.smart.query.valuenumhours"; //$NON-NLS-1$
	/**
	 * The value number of nights icon
	 */
	public static final String VALUE_NUM_NIGHTS_ICON = "org.wcs.smart.query.valuenumnights"; //$NON-NLS-1$
	
	/**
	 * Exclamation
	 */
	public static final String EXCLAMATION_ICON = "org.wcs.smart.query.exclamation"; //$NON-NLS-1$
	
	/**
	 * Row Header
	 */
	public static final String ROW_HEADER_ICON = "org.wcs.smart.query.rowheader"; //$NON-NLS-1$
	
	/**
	 * Column Header
	 */
	public static final String COLUMN_HEADER_ICON = "org.wcs.smart.query.columheader"; //$NON-NLS-1$
	
	/**
	 * Area Filter Icon
	 */
	public static final String AREA_FILTER_ICON = "org.wcs.smart.query.areafilter"; //$NON-NLS-1$
	
	
	/**
	 * Area Polygon Filter Icon
	 */
	public static final String AREA_POLYGON_FILTER_ICON = "org.wcs.smart.query.areapolyfilter"; //$NON-NLS-1$

	/**
	 * Grid ICon
	 */
	public static final String GRID_ICON = "org.wcs.smart.query.grid"; //$NON-NLS-1$
	
	private static List<AbstractQueryPropertyProvider>  propertyProviders = null;
	private QueryEmployeeListener employeeListener = new QueryEmployeeListener();

	/**
	 * Query property extension id
	 */
	private static final String QUERY_PROPERTY_EXTENSION_ID = "org.wcs.smart.query.property"; //$NON-NLS-1$

	
	/**
	 * The constructor
	 */
	public QueryPlugIn() {
	}

	public File getQueryTempDirectory(){
		return new File(SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE), QUERY_TEMP_FOLDER);		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		//add required listeners
		ConservationAreaManager.getInstance().addDeleteHandler(new QueryCaDeleteHandler(),QueryCaDeleteHandler.EXECUTE_ORDER);
		ConservationAreaManager.getInstance().addEmployeeListener(employeeListener);
		
		QueryCleanUpJob cleanUp = new QueryCleanUpJob();
		cleanUp.setRule(SmartPlugIn.PLUGIN_START_MUTEX);
		cleanUp.schedule();
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		
		ConservationAreaManager.getInstance().removeEmployeeListener(employeeListener);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static QueryPlugIn getDefault() {
		return plugin;
	}

	@Override
	public void initializeImageRegistry(ImageRegistry reg){	
		reg.put(DELETE_MINI_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/delete.png")); //$NON-NLS-1$
		reg.put(QUERY_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/querypatrol.gif"));//$NON-NLS-1$
		reg.put(FOLDER_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/folder.png"));//$NON-NLS-1$
		reg.put(GRID_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/grid.png"));//$NON-NLS-1$
		reg.put(CALENDAR_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/calendar.png")); //$NON-NLS-1$
		reg.put(CALENDAR_DAY_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/calendar_day.png"));//$NON-NLS-1$
		reg.put(CALENDAR_WEEK_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/calendar_week.png"));//$NON-NLS-1$
		reg.put(CALENDAR_MONTH_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/calendar_month.png"));//$NON-NLS-1$
		reg.put(CALENDAR_YEAR_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/calendar_year.png"));//$NON-NLS-1$
		reg.put(GROUPBY_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/group_by.png"));//$NON-NLS-1$
		reg.put(VALUE_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/values.png"));//$NON-NLS-1$
		reg.put(VALUE_DISTANCE_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/value_distance.png"));//$NON-NLS-1$
		reg.put(VALUE_NUM_DAYS_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/value_numDays.png"));//$NON-NLS-1$
		reg.put(VALUE_NUM_HOURS_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/value_numHours.png"));//$NON-NLS-1$
		reg.put(VALUE_NUM_NIGHTS_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/value_numNights.png"));//$NON-NLS-1$
		reg.put(EXCLAMATION_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/exclamation.png"));//$NON-NLS-1$
		reg.put(ROW_HEADER_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/row_header.png"));//$NON-NLS-1$
		reg.put(COLUMN_HEADER_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/column_header.png"));//$NON-NLS-1$
		reg.put(AREA_FILTER_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/area_filter.png"));//$NON-NLS-1$
		reg.put(AREA_POLYGON_FILTER_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/obj16/area_polygon.png"));//$NON-NLS-1$
		super.initializeImageRegistry(reg);
		
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
	 * Displays an error message to the user and logs the
	 * message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.QueryPlugIn_Error_DialogTitle, message);
			}});
	}
	
	/**
	 * 
	 * @return the id of the query perspective to use
	 */
	public static String getActivePerspectiveId(){
		if (SmartDB.isMultipleAnalysis()){
			return MultiCaQueryPerspective.ID;
		}else{
			return QueryPerspective.ID;
		}
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
					AbstractQueryPropertyProvider prop = (AbstractQueryPropertyProvider) e.createExecutableExtension("propertyProvider"); //$NON-NLS-1$
					prop.setName(e.getAttribute("propertyName")); //$NON-NLS-1$
					propertyProviders.add( prop);
				}
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
		return propertyProviders;
		
	}
}
