package org.wcs.smart.cybertracker.incident;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.cybertracker.incident.model.IIncidentCyberTrackerLabelProvider;

/**
 * The activator class controls the plug-in life cycle
 */
public class CtIncidentPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.cybertracker.incident"; //$NON-NLS-1$

	public static final String DB_VERSION_2 = "3.0"; //$NON-NLS-1$
	public static final String DB_VERSION_1 = "2.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_2;
	
	
	// The shared instance
	private static CtIncidentPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public CtIncidentPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		SmartContext.INSTANCE.setClass(IIncidentCyberTrackerLabelProvider.class, new IncidentCyberTrackerLabelProvider());
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
	public static CtIncidentPlugIn getDefault() {
		return plugin;
	}

}
