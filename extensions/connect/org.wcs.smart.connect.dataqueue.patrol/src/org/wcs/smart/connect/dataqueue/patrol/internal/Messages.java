package org.wcs.smart.connect.dataqueue.patrol.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.connect.dataqueue.patrol.internal.messages"; //$NON-NLS-1$
	public static String PatrolDataQueueProcessor_Status1;
	public static String PatrolDataQueueProcessor_Status2;
	public static String PatrolProcessorOptionPanel_OptionPanel;
	public static String PatrolProcessorOptionPanel_PidOptionLabel;
	public static String PatrolProcessorOptionPanel_PidOptionTooltip;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
