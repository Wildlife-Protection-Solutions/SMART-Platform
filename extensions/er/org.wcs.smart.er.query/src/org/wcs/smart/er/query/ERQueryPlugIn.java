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

	public static final String OBSERVATION_ICON = "org.wcs.smart.er.query.observation"; //$NON-NLS-1$
	
	public static final String ALL_SURVEY_ICON = "org.wcs.smart.er.query.allsurvey"; //$NON-NLS-1$
	
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
    	reg.put(ALL_SURVEY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/all_survey.png")); //$NON-NLS-1$
    }
    

}
