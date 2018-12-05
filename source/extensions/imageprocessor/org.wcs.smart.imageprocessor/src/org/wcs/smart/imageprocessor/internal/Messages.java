package org.wcs.smart.imageprocessor.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.imageprocessor.internal.messages"; //$NON-NLS-1$
	public static String ImageResizeProcessor_invalidTargetSize;
	public static String ImageResizeProcessor_JobName;
	public static String ImageResizeProcessor_NoReaderFound;
	public static String ImageResizeProcessor_NothingToDo;
	public static String ProcessingStatusDialog_CancelledMessage;
	public static String ProcessingStatusDialog_FileColumnName;
	public static String ProcessingStatusDialog_Message;
	public static String ProcessingStatusDialog_MessageColumName;
	public static String ProcessingStatusDialog_SearchingStatus;
	public static String ProcessingStatusDialog_StatusColumnName;
	public static String ProcessingStatusDialog_Title;
	public static String ResizeAttachmentDialog_CustomLabel;
	public static String ResizeAttachmentDialog_Height;
	public static String ResizeAttachmentDialog_InvalidHeight;
	public static String ResizeAttachmentDialog_InvalidMaxSize;
	public static String ResizeAttachmentDialog_InvalidWidth;
	public static String ResizeAttachmentDialog_MaxSize;
	public static String ResizeAttachmentDialog_MaxSizeMessage;
	public static String ResizeAttachmentDialog_Message;
	public static String ResizeAttachmentDialog_NewSize;
	public static String ResizeAttachmentDialog_NewSizeMessage;
	public static String ResizeAttachmentDialog_OneTypeRequired;
	public static String ResizeAttachmentDialog_Title;
	public static String ResizeAttachmentDialog_typesLabel;
	public static String ResizeAttachmentDialog_typesmessage;
	public static String ResizeAttachmentDialog_Width;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
