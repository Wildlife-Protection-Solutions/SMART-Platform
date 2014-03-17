package org.wcs.smart.entity.query;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.entity.query.internal.Messages;

/**
 * The activator class controls the plug-in life cycle
 */
public class EntityQueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.entity.query"; //$NON-NLS-1$

	/**
	 * Entity grid query icon
	 */
	public static final String GRID_ICON = "org.wcs.smart.entity.query.grid.icon"; //$NON-NLS-1$
	/**
	 * Entity summary grid query icon
	 */
	public static final String SUMMARY_ICON = "org.wcs.smart.entity.query.summary.icon"; //$NON-NLS-1$
	/**
	 * Entity observation query icon
	 */
	public static final String OBSERVATION_ICON = "org.wcs.smart.entity.query.observation.icon"; //$NON-NLS-1$
	/**
	 * Entity waypoint query icon
	 */
	public static final String WAYPOINT_ICON = "org.wcs.smart.entity.query.waypoint.icon"; //$NON-NLS-1$

	// The shared instance
	private static EntityQueryPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public EntityQueryPlugIn() {
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
	public static EntityQueryPlugIn getDefault() {
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
    protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		
		reg.put(GRID_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity_gridded_query.png")); //$NON-NLS-1$
		reg.put(OBSERVATION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity_observation_query.png")); //$NON-NLS-1$
		reg.put(SUMMARY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity_summary_query.png")); //$NON-NLS-1$
		reg.put(WAYPOINT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity_waypoint_query.png")); //$NON-NLS-1$
					
    }
}
