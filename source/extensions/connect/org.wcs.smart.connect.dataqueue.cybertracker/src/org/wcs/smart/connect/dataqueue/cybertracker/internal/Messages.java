package org.wcs.smart.connect.dataqueue.cybertracker.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.connect.dataqueue.cybertracker.internal.messages"; //$NON-NLS-1$
	public static String CybertrackerItemProcessor_Cancelled2;
	public static String CybertrackerItemProcessor_CancelledMsg;
	public static String CybertrackerItemProcessor_CompleteMsg;
	public static String CybertrackerItemProcessor_DataProcessingError;
	public static String CybertrackerItemProcessor_ErrorTitle;
	public static String CybertrackerItemProcessor_NoData;
	public static String CybertrackerItemProcessor_ProcessedMsg;
	public static String CybertrackerItemProcessor_WarningTitle;
	public static String JsonCtParser_AttributeNotFound;
	public static String JsonCtParser_CatAttributeNotFound;
	public static String JsonCtParser_CouldNotImportPhoto;
	public static String JsonCtParser_CouldNotParseValue;
	public static String JsonCtParser_ListAttributeNotFound;
	public static String JsonCtParser_MultiValuesSameAttribute;
	public static String JsonCtParser_NoCateogyr;
	public static String JsonCtParser_NoFeatureFound;
	public static String JsonCtParser_ObserverNotFound;
	public static String JsonCtParser_ParseError;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
