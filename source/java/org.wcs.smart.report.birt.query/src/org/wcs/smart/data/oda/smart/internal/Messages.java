package org.wcs.smart.data.oda.smart.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.data.oda.smart.internal.messages"; //$NON-NLS-1$
	
	public static String Report400Upgrader_ErrorMessage;

	public static String Report400Upgrader_ErrorTitle;

	public static String Report400Upgrader_UpgradeError;

	public static String ReportQueryListener_BeforeSave_QueryUsedWarning;
	public static String ReportQueryListener_QuerySaveError;
	public static String ReportQueryListener_Warning_DialogTitle;
	public static String SmartDatasetMetadata_SmartDataSourceName;
	public static String SmartQuery_QueryTypeNotSupported;
	public static String SmartQuery_QueryTypeNotSupportedReports;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
