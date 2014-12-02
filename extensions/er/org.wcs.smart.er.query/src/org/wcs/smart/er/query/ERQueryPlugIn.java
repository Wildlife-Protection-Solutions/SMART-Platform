package org.wcs.smart.er.query;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.er.query.internal.Messages;

/**
 * The activator class controls the plug-in life cycle
 */
public class ERQueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.er.query"; //$NON-NLS-1$
	//The current db version
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION_2 = "2.0"; //$NON-NLS-1$

	public static final String OBSERVATION_ICON = "org.wcs.smart.er.query.observation"; //$NON-NLS-1$
	public static final String WAYPOINT_ICON = "org.wcs.smart.er.query.waypoint"; //$NON-NLS-1$
	public static final String GRIDDED_ICON = "org.wcs.smart.er.query.grid"; //$NON-NLS-1$
	public static final String SUMMARY_ICON = "org.wcs.smart.er.query.summary"; //$NON-NLS-1$
	public static final String MISSION_ICON = "org.wcs.smart.er.query.mission"; //$NON-NLS-1$
	public static final String MISSIONTRACK_ICON = "org.wcs.smart.er.query.missiontrack"; //$NON-NLS-1$
	public static final String ALL_SURVEY_ICON = "org.wcs.smart.er.query.allsurvey"; //$NON-NLS-1$
	public static final String TRACK_DISTANCE_ICON = "org.wcs.smart.er.query.mission.track"; //$NON-NLS-1$
	public static final String MISSION_COUNT_ICON = "org.wcs.smart.er.query.mission.count"; //$NON-NLS-1$
	public static final String SURVEY_COUNT_ICON = "org.wcs.smart.er.query.survey.count"; //$NON-NLS-1$
	
	// The shared instance
	private static ERQueryPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public ERQueryPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
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
	public static ERQueryPlugIn getDefault() {
		return plugin;
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.ERQueryPlugIn_ErrorDialogTitle, message);
			}
			
		});
		
	}
	
	/**
	 * Logs error message 
	 * @param message
	 * @param t
	 */
    public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
    	reg.put(OBSERVATION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_observation_query.png")); //$NON-NLS-1$)
    	reg.put(WAYPOINT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_waypoint_query.png")); //$NON-NLS-1$)
    	reg.put(GRIDDED_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_gridded_query.png")); //$NON-NLS-1$)
    	reg.put(SUMMARY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_summary_query.png")); //$NON-NLS-1$)
    	reg.put(MISSION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_mission_query.png")); //$NON-NLS-1$)
    	reg.put(MISSIONTRACK_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_missiontrack_query.png")); //$NON-NLS-1$)
    	reg.put(ALL_SURVEY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/all_survey.png")); //$NON-NLS-1$
    	reg.put(TRACK_DISTANCE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/value_distance.png")); //$NON-NLS-1$
    	reg.put(MISSION_COUNT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/value_missioncnt.png")); //$NON-NLS-1$
    	reg.put(SURVEY_COUNT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/value_surveycnt.png")); //$NON-NLS-1$
    }
    

}
