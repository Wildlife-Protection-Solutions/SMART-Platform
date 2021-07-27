package org.wcs.smart.qa.incident.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.qa.incident.internal.messages"; //$NON-NLS-1$
	public static String DeleteIncidentAction_ActionName;
	public static String DeleteIncidentAction_DeleteDialogConfirmMsg;
	public static String DeleteIncidentAction_DeleteDialogTitle;
	public static String DeleteIncidentAction_DeleteError;
	public static String DeleteIncidentAction_DeleteErrorWpNotFound;
	public static String DeleteIncidentAction_DeleteMsg;
	public static String DeleteIncidentAction_FilestoreDeleteError;
	public static String EditIncidentAction_ActionName;
	public static String EditIncidentAction_MovedMsg;
	public static String IncidentEditWaypointDialog_WpNotFound;
	public static String IncidentLabelProvider_DataProviderName;
	public static String IncidentLabelProvider_IntegrateDataProviderName;
	public static String IncidentLabelProvider_NotFoundError;
	public static String IncidentLabelProvider_WaypointLbl;
	public static String OpenIncidentAction_ActionName;
	public static String OpenIncidentAction_NotFoundMsg;
	public static String OpenIncidentAction_NotFoundTitle;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
