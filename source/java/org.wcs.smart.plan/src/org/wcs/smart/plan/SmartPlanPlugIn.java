package org.wcs.smart.plan;


import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.Query;
import org.hibernate.Session;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.IPatrolDeleteHandler;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.plan.internal.Messages;

/**
 * The activator class controls the plug-in life cycle
 */
public class SmartPlanPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.plan"; //$NON-NLS-1$
	
	// The current supported DB Version
	public static final String DB_VERSION = "3.0"; //$NON-NLS-1$

	// The shared instance
	private static SmartPlanPlugIn plugin;
	
	/**
	 * Directory for storing plan data in filestore
	 */
	public static final String PLAN_DIR = "plans"; //$NON-NLS-1$
	
	/**
	 * General Image descriptor key for plan 
	 */
	public static final String PLAN_ICON = "org.wsc.smart.patrol.PLAN"; //$NON-NLS-1$

	/**
	 * Image descriptor key for conservation area plan 
	 */
	public static final String CA_PLAN_ICON = "org.wsc.smart.patrol.CA_PLAN"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for conservation area plan 
	 */
	public static final String TEAM_PLAN_ICON = "org.wsc.smart.patrol.TEAM_PLAN"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for conservation area plan 
	 */
	public static final String STATION_PLAN_ICON = "org.wsc.smart.patrol.STATION_PLAN"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for conservation area plan 
	 */
	public static final String PATROL_PLAN_ICON = "org.wsc.smart.patrol.PATROL_PLAN"; //$NON-NLS-1$

	/**
	 * Image descriptor key for conservation area plan 
	 */
	public static final String STATUS_COMPLETE = "org.wsc.smart.patrol.STATUS_COMPLETE"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for conservation area plan 
	 */
	public static final String STATUS_INCOMPLETE = "org.wsc.smart.patrol.STATUS_INCOMPLETE"; //$NON-NLS-1$
	
	/**
	 * Preference key for plan distance to completion
	 */
	public static final String SYSPROP_PLAN_DISTANCE_TO_COMPLETE = "PLAN_DISTANCE_TO_COMPLETE"; //$NON-NLS-1$
	
	/**
	 * Default value for distance to complete
	 */
	private static final int DISTANCE_COMPLETE_DEFAULT_VALUE = 250;
	
	private PlanCaDeleteHandler deleteCa;
	
	private IPatrolDeleteHandler deletePatrol =  new IPatrolDeleteHandler() {
		@Override
		public boolean beforeDelete(Patrol patrol, Session session,
				IProgressMonitor monitor) throws Exception {
			Query q = session.createQuery("DELETE FROM PatrolPlan where id.patrol = :patrol").setParameter("patrol", patrol);  //$NON-NLS-1$//$NON-NLS-2$
			q.executeUpdate();
			return true;
		}
		
		@Override
		public void afterDelete(Patrol patrol, IProgressMonitor monitor) {
		}
	};
	
	/**
	 * The constructor
	 */
	public SmartPlanPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		deleteCa = new PlanCaDeleteHandler();
		ConservationAreaManager.getInstance().addDeleteHandler(deleteCa,PlanCaDeleteHandler.EXECUTE_ORDER );
		PatrolManager.getInstance().addDeleteHandler(deletePatrol,0 );
		
		// init default preferences
		DefaultScope.INSTANCE.getNode(getBundle().getSymbolicName()).putInt(SYSPROP_PLAN_DISTANCE_TO_COMPLETE, DISTANCE_COMPLETE_DEFAULT_VALUE);
	}

	/*
	 * (non-Javadoc)
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
	public static SmartPlanPlugIn getDefault() {
		return plugin;
	}

	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	public static void logInfo(String message){
		getDefault().getLog().log(new Status(IStatus.OK, PLUGIN_ID, IStatus.INFO, message, null));
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.SmartPlanPlugIn_Error, message);
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
		MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SmartPlanPlugIn_Error, message);
		System.exit(1);
	}
	
	@Override
    protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(PLAN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/view16/plan.png")); //$NON-NLS-1$

		reg.put(CA_PLAN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/ca_plan.png")); //$NON-NLS-1$
		reg.put(STATION_PLAN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/station_plan.png")); //$NON-NLS-1$
		reg.put(TEAM_PLAN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/team_plan.png")); //$NON-NLS-1$
		reg.put(PATROL_PLAN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol_plan.png")); //$NON-NLS-1$
		
		reg.put(STATUS_COMPLETE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/status_complete.png")); //$NON-NLS-1$
		reg.put(STATUS_INCOMPLETE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/status_incomplete.png")); //$NON-NLS-1$
    }
	
	/**
	 * 
	 * @param key
	 * @return default preference value for given key as integer
	 */
	public int getDefaultPreferenceInt(String key){
		return DefaultScope.INSTANCE.getNode(getBundle().getSymbolicName()).getInt(SYSPROP_PLAN_DISTANCE_TO_COMPLETE, 0);
	}
	
	/**
	 * Gets the location of plan files within the smart file store.
	 * If the directory does not exist it will create it.
	 * @return
	 */
	public File getPlanDirectory() {
		File f =  new File(SmartDB.getCurrentConservationArea()
				.getFileDataStoreLocation()
				+ File.separator
				+ SmartPlanPlugIn.PLAN_DIR + File.separator);
		if (!f.exists()){
			f.mkdir();
		}
		return f;
	}
}
