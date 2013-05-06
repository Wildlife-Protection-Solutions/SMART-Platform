package org.wcs.smart.cybertracker.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.cybertracker.internal.messages"; //$NON-NLS-1$
	public static String CyberTrackerExportDialog_Button_Browse;
	public static String CyberTrackerExportDialog_Button_Export;
	public static String CyberTrackerExportDialog_Err_Dialog_Message;
	public static String CyberTrackerExportDialog_ErrDialog_Title;
	public static String CyberTrackerExportDialog_Label_File;
	public static String CyberTrackerExportDialog_Message;
	public static String CyberTrackerExportDialog_Title;
	public static String CyberTrackerExportHandler_ErrDialog_Message;
	public static String CyberTrackerExportHandler_ErrDialog_Title;
	public static String CyberTrackerExportHandler_InfoDialog_Message;
	public static String CyberTrackerExportHandler_InfoDialog_Title;
	public static String CyberTrackerExportHandler_TaskName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
