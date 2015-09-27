package org.wcs.smart.cybertracker;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class CyberTrackerPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.cybertracker"; //$NON-NLS-1$

	public static final String DB_VERSION = "3.0"; //$NON-NLS-1$
	
	//image registry key for cybertracker dialog image
	public static final String CT_WIZARD_BANNER = "org.wcs.smart.cybertracker.wizban"; //$NON-NLS-1$

	/**
	 * Extension id
	 */
	public static final String EXTENSION_ID = "org.wcs.smart.cybertracker.datasource"; //$NON-NLS-1$
	
	// The shared instance
	private static CyberTrackerPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public CyberTrackerPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		ConservationAreaManager.getInstance().addDeleteHandler(new CyberTrackerCaDeleteHandler(), CyberTrackerCaDeleteHandler.EXECUTE_ORDER);

		Job j = new CyberTrackerStartupJob();
		j.setRule(SmartPlugIn.PLUGIN_START_MUTEX);
		j.schedule();
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
	public static CyberTrackerPlugIn getDefault() {
		return plugin;
	}

	/**
	 * Log an error message
	 * 
	 * @param message error message
	 * @param t exception thrown or null
	 */
	public static void log(String message, Throwable t){
        getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
	}
	
	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
	     reg.put(CT_WIZARD_BANNER, imageDescriptorFromPlugin(PLUGIN_ID, "images/wizban/cybertracker.png")); //$NON-NLS-1$
	}
	
	public static void displayInfo(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}

	public static void displayError(final String title, final String message, Throwable ex) {
		log(message, ex);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}
	
}
