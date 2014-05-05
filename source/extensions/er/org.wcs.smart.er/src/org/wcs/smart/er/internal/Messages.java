package org.wcs.smart.er.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.er.internal.messages"; //$NON-NLS-1$
	public static String AddERJob_Title;
	public static String ERDatabaseUpgrader_UpgradeTask;
	public static String RemoveERJob_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
