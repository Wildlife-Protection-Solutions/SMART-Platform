package org.wcs.smart.observation.query;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.observation.query.internal.Messages;

/**
 * The activator class controls the plug-in life cycle
 */
public class ObservationQueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.observation.query"; //$NON-NLS-1$

	/**
	 * The waypoint query
	 */
	public static final String WAYPOINT_QUERY_ICON = "org.wcs.smart.query.waypointquery"; //$NON-NLS-1$

	/**
	 * The observation query
	 */
	public static final String OBSERVATION_QUERY_ICON = "org.wcs.smart.query.observationquery"; //$NON-NLS-1$

	/**
	 * The summary query
	 */
	public static final String SUMMARY_QUERY_ICON = "org.wcs.smart.query.summaryquery"; //$NON-NLS-1$

	/**
	 * The gridded query
	 */
	public static final String GRIDDED_SUMMARY_QUERY_ICON = "org.wcs.smart.query.griddedquery"; //$NON-NLS-1$

	
	// The shared instance
	private static ObservationQueryPlugIn plugin;
	
	
	/**
	 * The constructor
	 */
	public ObservationQueryPlugIn() {
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
	public static ObservationQueryPlugIn getDefault() {
		return plugin;
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
		reg.put(GRIDDED_SUMMARY_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/gridded_query.png"));//$NON-NLS-1$

		super.initializeImageRegistry(reg);
	}
}
