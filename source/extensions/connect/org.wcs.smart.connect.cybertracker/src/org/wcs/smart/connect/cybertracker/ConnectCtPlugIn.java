package org.wcs.smart.connect.cybertracker;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ConnectCtPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.connect.cybertracker"; //$NON-NLS-1$

	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION_2 = "2.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_2; //current version
	
	// The shared instance
	private static ConnectCtPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public ConnectCtPlugIn() {
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
	public static ConnectCtPlugIn getDefault() {
		return plugin;
	}

	
}
