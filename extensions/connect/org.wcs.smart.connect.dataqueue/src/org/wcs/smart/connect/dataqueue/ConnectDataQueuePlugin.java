package org.wcs.smart.connect.dataqueue;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartPlugIn;

/**
 * The activator class controls the plug-in life cycle
 */
public class ConnectDataQueuePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.connect.dataqueue"; //$NON-NLS-1$

	// The shared instance
	private static ConnectDataQueuePlugin plugin;
	
	/**
	 * The constructor
	 */
	public ConnectDataQueuePlugin() {
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
	public static ConnectDataQueuePlugin getDefault() {
		return plugin;
	}

	/**
	 * Displays an error message to the user and logs the message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		SmartPlugIn.displayError(message, t);
	}
	
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
		getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
}
