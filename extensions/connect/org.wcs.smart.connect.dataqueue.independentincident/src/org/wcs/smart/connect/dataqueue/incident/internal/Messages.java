package org.wcs.smart.connect.dataqueue.incident.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.connect.dataqueue.incident.internal.messages"; //$NON-NLS-1$
	public static String IncidentProcessor_;
	public static String IncidentProcessor_incidentloaded;
	public static String IncidentProcessor_nothingloaded;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
