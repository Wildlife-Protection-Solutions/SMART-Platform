package org.wcs.smart.connect.dataqueue.er.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.connect.dataqueue.er.internal.messages"; //$NON-NLS-1$
	public static String ErProcessorOptionPanel_NewIdOp;
	public static String ErProcessorOptionPanel_NewIdTooltip;
	public static String ErProcessorOptionPanel_PanelName;
	public static String MissionProcessor_MissionImported;
	public static String MissionProcessor_NotImported;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
