package org.wcs.smart.patrol.internal.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.patrol.internal.ui.messages"; //$NON-NLS-1$
	public static String MovePatrolLegDialog_CreateNewPatrol;
	public static String MovePatrolLegDialog_NewPatrolID;
	public static String MovePatrolLegDialog_SelectNewID;
	public static String MovePatrolLegDialog_SplitLegsButton;
	public static String MovePatrolLegDialog_SplitText;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
