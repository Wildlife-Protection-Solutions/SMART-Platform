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
package org.wcs.smart;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.DeleteConservationAreaHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.startup.SmartStartUp;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;
import org.wcs.smart.udig.catalog.smart.ISmartMapLabelProvider;
import org.wcs.smart.udig.catalog.smart.ui.DesktopSessionProvider;
import org.wcs.smart.udig.catalog.smart.ui.DesktopSmartServiceLabelProvider;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * The activator class controls the plug-in life cycle
 */
public class SmartPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart"; //$NON-NLS-1$
	
	// The shared instance
	private static SmartPlugIn plugin;
	
    /**
     * Identifies the error overlay image.
     */
    public final static String IMG_DEC_FIELD_ERROR = "IMG_DEC_FIELD_ERROR"; //$NON-NLS-1$

    /**
     * Identifies the warning overlay image.
     */
    public final static String IMG_DEC_FIELD_WARNING = "IMG_DEC_FIELD_WARNING"; //$NON-NLS-1$

    
	/**
	 * dialog setting key for csv file delimiter
	 */
	public static final String DEFAULT_DELIMITER_KEY = "CSV_FILE_DELIMITER"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for smart employee
	 */
	public static final String SMART_EMPLOYEE_ICON = "org.wsc.smart.SMART_EMPLOYEE_ICON"; //$NON-NLS-1$

	/**
	 * Image descriptor key for non-smart user employee
	 */
	public static final String EMPLOYEE_ICON = "org.wsc.smart.EMPLOYEE_ICON"; //$NON-NLS-1$
	

	/**
	 * Image descriptor key for station
	 */
	public static final String STATION_ICON = "org.wsc.smart.STATION_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for smart 48x48 icon
	 */
	public static final String SMART_48_ICON = "org.wcs.smart.SMART_48_ICON"; //$NON-NLS-1$
	
	
	/**
	 * Image descriptor for category icon
	 */
	public static final String CATEGORY_ICON = "org.wsc.smart.datamodel.CATEGORY_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute text icon
	 */
	public static final String ATTRIBUTE_TEXT_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_TEXT_ICON"; //$NON-NLS-1$

	/**
	 * Image descriptor for attribute location icon
	 */
	public static final String ATTRIBUTE_LOCATION_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_LOCATION_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for attribute boolean icon
	 */
	public static final String ATTRIBUTE_BOOLEAN_ICON = "org.wsc.smart.datamodel.ATTRIBUTE_BOOLEAN_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute number icon
	 */
	public static final String ATTRIBUTE_NUMBER_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_NUMBER_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute list icon
	 */
	public static final String ATTRIBUTE_LIST_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_LIST_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute tree icon
	 */
	public static final String ATTRIBUTE_TREE_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_TREE_ICON"; //$NON-NLS-1$	
	/**
	 * Image descriptor for attribute date icon
	 */
	public static final String ATTRIBUTE_DATE_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_DATE_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for data model icon
	 */
	public static final String DATA_MODEL_ICON= "org.wsc.smart.datamodel.DATAMODEL_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for warning icon
	 */
	public static final String WARN_ICON= "org.wsc.smart.WARN_ICON"; //$NON-NLS-1$
	public static final String INFO_ICON= "org.wsc.smart.INFO_ICON"; //$NON-NLS-1$
	public static final String ERROR_ICON= "org.wsc.smart.ERROR_ICON"; //$NON-NLS-1$
		
	/**
	 * Image descriptor for warning icon
	 */
	public static final String BULLET_BLACK = "org.wsc.smart.BULLET_BLACK"; //$NON-NLS-1$

	/**
	 * Image descriptor for map  icon
	 */
	public static final String MAP_ICON = "org.wsc.smart.MAP_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for map  icon
	 */
	public static final String STYLE_ICON = "org.wsc.smart.STYLE_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for map  icon
	 */
	public static final String DELETE_ICON = "org.wsc.smart.DELETE_ICON"; //$NON-NLS-1$
	public static final String ADD_ICON = "org.wsc.smart.ADD_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for map  icon
	 */
	public static final String RENAME_ICON = "org.wsc.smart.RENAME_ICON"; //$NON-NLS-1$
	
	/**
	 * Cross CA Icon
	 */
	public static final String CROSSCA_ICON = "org.wsc.smart.CROSSCA_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for wizard banner export
	 */
	public static final String WIZBAN_EXPORT_IMAGE = "org.wsc.smart.WIZBAN_EXPORT_IMAGE"; //$NON-NLS-1$
	/**
	 * Image descriptor for wizard banner export
	 */
	public static final String ZOOM_IMAGE = "org.wsc.smart.ZOOM_IMAGE"; //$NON-NLS-1$
	
	/**
	 * Browser images
	 */
	public static final String BROWSER_FORWARD = "org.wsc.smart.browser.forward"; //$NON-NLS-1$
	public static final String BROWSER_BACKWARD = "org.wsc.smart.browser.backward"; //$NON-NLS-1$
	public static final String BROWSER_GO = "org.wsc.smart.browser.refersh"; //$NON-NLS-1$
	public static final String BROWSER_HOME = "org.wsc.smart.browser.home"; //$NON-NLS-1$
	public static final String BROWSER_STOP = "org.wsc.smart.browser.stop"; //$NON-NLS-1$
	
	/**
	 * Goto Icon
	 */
	public static final String GOTO_ICON = "org.wsc.smart.GOTO"; //$NON-NLS-1$
	/**
	 * Edit Icon
	 */
	public static final String EDIT_ICON = "org.wsc.smart.EDIT_ICON"; //$NON-NLS-1$
	/**
	 * Clear selection icon
	 */
	public static final String CLEAR_SELECTION_ICON = "org.wsc.smart.map.selection.clear"; //$NON-NLS-1$
	/**
	 * Refresh icon
	 */
	public static final String REFRESH_ICON = "org.wsc.smart.action.refresh"; //$NON-NLS-1$
	
	/**
	 * Mutex to ensure that jobs will not be conflicting as simultaneous jobs execution
	 * might result in SQLException. This will ensure that jobs are running one by one.
	 */
	public static final ISchedulingRule PLUGIN_START_MUTEX = new ISchedulingRule() {
        public boolean contains(ISchedulingRule rule) {
            return (rule == this);
        }
        public boolean isConflicting(ISchedulingRule rule) {
            return (rule == this);
        }
	};

	public BasemapDefinition defaultDefinition = null;

	/**
	 * IEventBroker event topic that is fired when 
	 * data is changed in the database without details about the 
	 * specific data that was change.  This is currently used
	 * by the replication process after replicated data
	 * is applied to the database.
	 * 
	 * Payload is undefined.
	 */
	public static final String E4_DATABASE_CHANGED_EVENT = "SMARTDATA"; //$NON-NLS-1$
	
	/**
	 * The constructor
	 */
	public SmartPlugIn() {

	}
	
	public BasemapDefinition getBasemapSelection(){
		return this.defaultDefinition;
	}
	public void setBasemapSelection(BasemapDefinition definition){
		this.defaultDefinition = definition;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		System.setProperty("org.wcs.smart.version", context.getBundle().getVersion().toString()); //$NON-NLS-1$

		//lock on the plugin start mutex until the
		//database has been initialized
		//We don't want anything running until after the splash screen
		//has initialized the Database.  See initializeDatabase for code
		//that unlocks this rule
		Job.getJobManager().beginRule(SmartPlugIn.PLUGIN_START_MUTEX, null);

		ConservationAreaManager.getInstance().addDeleteHandler(new DeleteConservationAreaHandler(), DeleteConservationAreaHandler.EXECUTE_ORDER);
		SmartContext.INSTANCE.setClass(ICoreLabelProvider.class, new SmartLabelProvider());
		SmartContext.INSTANCE.setFilestoreLocation(SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE));
		
		SmartContext.INSTANCE.setClass(IDatabaseConnectionProvider.class, new DesktopSessionProvider());
		SmartContext.INSTANCE.setClass(ISmartMapLabelProvider.class, new DesktopSmartServiceLabelProvider());
	}

	public static void initializeDatabase(){
		boolean exit = false;
		try{
			SmartStartUp.initDb();
		}catch (final Exception ex){	
			SmartPlugIn.displayLog(ex.getMessage(), ex);		
			exit = true;
		}
		if (!exit){
			try{
				SmartStartUp.connectToDb();	
			}catch (final Exception ex){
				SmartPlugIn.displayLog(ex.getMessage(), ex);		
				exit= true;
			}
		}
		
		if (exit){
			System.exit(1);
		}
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				IJobManager manager = Job.getJobManager();
				if (manager.currentRule() == SmartPlugIn.PLUGIN_START_MUTEX){
					manager.endRule(SmartPlugIn.PLUGIN_START_MUTEX);
				}
			}});
		
	}
	
	/**
	 * Checks the current database version against the expected version.  If an exception
	 * is thrown, the calling function should terminate the application.
	 *  
	 * @return
	 * @throws exception if the version are incorrect
	 */
	public static void versionCheck() throws Exception{
		boolean isokay = false;
		String dbVersion = Messages.SmartPlugIn_UnknownVersion;
		String smartDbVersion = SmartProperties.getInstance().getProperty(SmartProperties.DB_VERSION_KEY);
		Session s = HibernateManager.openSession();
		try{
			SQLQuery q = s.createSQLQuery("SELECT version FROM smart.db_version WHERE plugin_id = ?"); //$NON-NLS-1$
			q.setParameter(0, SmartPlugIn.PLUGIN_ID);
			@SuppressWarnings("rawtypes")
			List results = q.list();
			if (results.size() > 0){
				dbVersion = (String)results.get(0);
				if (dbVersion.equals(smartDbVersion) ){
					isokay = true;
				}
			}
		}catch (Exception ex){
			//we cannot determine db version so we don't let the user login
			throw new Exception(Messages.SmartPlugIn_CouldNotconnect + ex.getMessage(), ex);
			
		}finally{
			s.close();
		}
		if (!isokay){
			throw new Exception(MessageFormat.format(Messages.SmartPlugIn_VersionErrorMessage, new Object[]{dbVersion, smartDbVersion}));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			// clean out the catalog
			List<IResolve> members = CatalogPlugin.getDefault().getLocalCatalog().members(null);
			for (IResolve member : members) {
				CatalogPlugin.getDefault().getLocalCatalog().remove((IService) member.resolve(IService.class, null));
			}
		} catch (Exception ex) {
			log(ex.getMessage(), ex);
		}

		plugin = null;
		super.stop(context);
	}

	/**
	 * Get image descriptors for the clear button.
	 */
	@Override
	 protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
	     reg.put(CATEGORY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/category_obj.gif")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_TEXT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_text.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_LOCATION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_location.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_NUMBER_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_number.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_BOOLEAN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_boolean.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_LIST_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_list.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_TREE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_tree.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_DATE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_date.png")); //$NON-NLS-1$
	     reg.put(DATA_MODEL_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/smart16.gif")); //$NON-NLS-1$
	     
	     reg.put(SMART_48_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "/images/icons/smart48.gif")); //$NON-NLS-1$
	     reg.put(SMART_EMPLOYEE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/user_orange.png")); //$NON-NLS-1$
	     reg.put(EMPLOYEE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/user_green.png")); //$NON-NLS-1$
	     reg.put(STATION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/station.png")); //$NON-NLS-1$
	     
	     reg.put(WARN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/warn.png")); //$NON-NLS-1$
	     reg.put(ERROR_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/error_icon.png")); //$NON-NLS-1$
	     reg.put(INFO_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/information_icon.png")); //$NON-NLS-1$
	     reg.put(BULLET_BLACK, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/bullet_black.png")); //$NON-NLS-1$
	     reg.put(MAP_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/map.png")); //$NON-NLS-1$
	     
	     reg.put(WIZBAN_EXPORT_IMAGE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/wizban/exportimage_wiz.gif")); //$NON-NLS-1$
	     reg.put(CROSSCA_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/eview16/crossca.png")); //$NON-NLS-1$
	     
	     reg.put(DELETE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/etool16/delete.png")); //$NON-NLS-1$
	     
	     reg.put(IMG_DEC_FIELD_ERROR, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/ovr16/error_ovr.png")); //$NON-NLS-1$
	     reg.put(IMG_DEC_FIELD_WARNING, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/ovr16/warning_ovr.png")); //$NON-NLS-1$
	     reg.put(STYLE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/style.png")); //$NON-NLS-1$
	     reg.put(RENAME_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/pencil.png")); //$NON-NLS-1$
	     reg.put(ADD_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/add.png")); //$NON-NLS-1$
	     
	     reg.put(BROWSER_FORWARD, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/arrow_right.png")); //$NON-NLS-1$
	     reg.put(BROWSER_BACKWARD, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/arrow_left.png")); //$NON-NLS-1$
	     reg.put(BROWSER_GO, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/arrow_refresh.png")); //$NON-NLS-1$
	     reg.put(BROWSER_HOME, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/home.png")); //$NON-NLS-1$
	     reg.put(BROWSER_STOP, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/stop.png")); //$NON-NLS-1$
	     
	     reg.put(ZOOM_IMAGE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/elcl16/zoom_tool.png")); //$NON-NLS-1$
	 
	     reg.put(GOTO_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/arrow_right.png")); //$NON-NLS-1$
	     reg.put(EDIT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/edit.png")); //$NON-NLS-1$
	     
	     reg.put(CLEAR_SELECTION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/etool16/clear_selection.png")); //$NON-NLS-1$
	     reg.put(REFRESH_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/etool16/arrow_refresh.png")); //$NON-NLS-1$
	}
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SmartPlugIn getDefault() {
		return plugin;
	}
	
	public static void log(int status, String message, Throwable t){
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
		log(status, message, t);
	}
	
	public static void logInfo(String message){
		getDefault().getLog().log(new Status(IStatus.OK, PLUGIN_ID, IStatus.INFO, message, null));
	}


	/**
	 * Displays an error message to the user and logs the message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		displayError(message, t);
	}
	
	/**
	 * Displays an error message to the user without logging.
	 * 
	 * @param message
	 * @param t
	 */
	public static void displayError(final String message, Throwable t){
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.SmartPlugIn_Error_Dialog_Title, message);
			}
		});
	}
	/**
	 * Displays an error message to the user, logs the error and
	 * exits the program with an error code of 1.
	 * 
	 * @param message the message to display to the user
	 * @param t optionally exception
	 */
	public static void displayLogExit(String message, Throwable t){
		log(message, t);
		MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SmartPlugIn_Error_Dialog_Title, message);
		System.exit(1);
	}
}
