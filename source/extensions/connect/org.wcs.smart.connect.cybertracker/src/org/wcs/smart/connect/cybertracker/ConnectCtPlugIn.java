package org.wcs.smart.connect.cybertracker;

import java.net.URI;
import java.net.URL;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.Session;
import org.osgi.framework.BundleContext;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.cybertracker.ctpackage.ConnectUrlContribution;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.cybertracker.ctpackage.ui.CtPackageExtensionPointManager;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.util.UuidUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class ConnectCtPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.connect.cybertracker"; //$NON-NLS-1$

	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION_2 = "2.0"; //$NON-NLS-1$
	public static final String DB_VERSION_3 = "3.0"; //$NON-NLS-1$  8.0
	public static final String DB_VERSION = DB_VERSION_3; //current version 
	
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

	public static URL generagePackageConnectUrl(Session session, ICtPackage ctpackage)  {
		
		String endpoint = CtPackageExtensionPointManager.INSTANCE.findManager(ctpackage).getConnectEndPoint();
		if (endpoint == null) {
			endpoint = ConnectUrlContribution.PACKAGE_URL;
		}
				
		
		ConnectServer cs = ConnectHibernateManager.getConnectServer(session);
		if (cs == null) return null;

		String surl = cs.getServerUrl();
		surl += endpoint + UuidUtils.uuidToString(ctpackage.getUuid());
				
		try {
			return URI.create(surl).toURL();
		}catch (Exception ex) {
			ConnectPlugIn.log(ex.getMessage(), ex);
		}
		return null;
	}
	
}
