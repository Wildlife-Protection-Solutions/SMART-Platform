package org.wcs.smart.cybertracker.incident.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.cybertracker.incident.internal.messages"; //$NON-NLS-1$
	public static String IncidentPackageContribution_CmModelLabel;
	public static String IncidentPackageContribution_CollectIncidentsOp;
	public static String IncidentPackageContribution_ConfigurationGroupLablel;
	public static String IncidentPackageContribution_LoadingCmJobname;
	public static String IncidentPackageContribution_OriginalDmLabel;
	public static String IncidentPackageContribution_TaskName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
