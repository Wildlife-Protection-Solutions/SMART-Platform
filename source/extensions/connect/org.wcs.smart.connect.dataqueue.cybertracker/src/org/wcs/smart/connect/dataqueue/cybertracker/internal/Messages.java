package org.wcs.smart.connect.dataqueue.cybertracker.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.connect.dataqueue.cybertracker.internal.messages"; //$NON-NLS-1$
	public static String CloseMsgDialog_Msg;
	public static String CybertrackerItemProcessor_Cancelled2;
	public static String CybertrackerItemProcessor_CancelledMsg;
	public static String CybertrackerItemProcessor_CompleteMsg;
	public static String CybertrackerItemProcessor_DataProcessingError;
	public static String CybertrackerItemProcessor_ErrorTitle;
	public static String CybertrackerItemProcessor_NoData;
	public static String CybertrackerItemProcessor_ProcessedMsg;
	public static String CybertrackerItemProcessor_TaskName;
	public static String CybertrackerItemProcessor_WarningTitle;
	public static String SmartMobileDataQueueOptionPanel_Description;
	public static String SmartMobileDataQueueOptionPanel_Name;
	public static String SmartMobileDataQueueOptionPanel_OptionsSection;
	public static String SmartMobileDataQueueOptionPanel_ProcessingOp;
	public static String SmartMobileDataQueueOptionPanel_ProcessingOpTooltip;
	public static String SmartMobileDataQueueOptionPanel_SettingsInfo;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
