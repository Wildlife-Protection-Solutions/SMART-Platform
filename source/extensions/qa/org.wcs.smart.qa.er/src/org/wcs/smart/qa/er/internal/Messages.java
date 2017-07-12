package org.wcs.smart.qa.er.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.qa.er.internal.messages"; //$NON-NLS-1$
	public static String DeleteErTrackAction_ActionName;
	public static String DeleteErTrackAction_DeleteError;
	public static String DeleteErTrackAction_DeleteErrorNotFound;
	public static String DeleteErTrackAction_DeleteMessage;
	public static String DeleteErTrackAction_DeleteMsg;
	public static String DeleteErTrackAction_DeleteTitle;
	public static String DeleteErWaypointAction_ActionName;
	public static String DeleteErWaypointAction_DeleteConfirmMessage;
	public static String DeleteErWaypointAction_DeleteError;
	public static String DeleteErWaypointAction_DeleteErrorNotfound;
	public static String DeleteErWaypointAction_DeleteMessage;
	public static String DeleteErWaypointAction_DeleteTitle;
	public static String EditTrackAction_ActionName;
	public static String EditTrackAction_FixMessage;
	public static String EditTrackAction_NotFoundTitle;
	public static String EditTrackAction_ParseError;
	public static String EditTrackAction_TrackNotFoundError;
	public static String EditWaypointAction_ActionName;
	public static String EditWaypointAction_MovedMessage;
	public static String ErEditWaypointDialog_interpolateTooltip;
	public static String ErEditWaypointDialog_NotFoundError;
	public static String ErLabelProvider_MissionTrackDataProviderName;
	public static String ErLabelProvider_MissionWpDataProviderName;
	public static String ErLabelProvider_TrackNotFoundError;
	public static String ErLabelProvider_WaypointIdLbl;
	public static String ErLabelProvider_WaypointNotFoundError;
	public static String OpenSourceMissionAction_ActionName;
	public static String OpenSourceMissionAction_NotFoundTitle;
	public static String OpenSourceMissionAction_TrackNotFoundMsg;
	public static String OpenSourceMissionAction_WpNotfoundMsg;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
