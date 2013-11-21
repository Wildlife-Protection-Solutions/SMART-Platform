package org.wcs.smart.data.oda.smart.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.data.oda.smart.internal.messages"; //$NON-NLS-1$
	public static String SmartConnection_Error_DatasetNotSupported;
	public static String SmartDatasetMetadata_SmartDataSourceName;
	public static String SmartDriver_Underfined_MappingType;
	public static String SmartQuery_Error_CouldNoLoadMetadata;
	public static String SmartQuery_Error_CouldNoLoadQuery;
	public static String SmartQuery_LoadQuery_JobName;
	public static String SmartQuery_QueryTypeNotSupported;
	public static String SmartQuery_QueryTypeNotSupportedReports;
	public static String SmartQuery_Warning_SummaryGroupByDates;
	public static String SummaryQueryResultSetMetadata_ParseQueryJob;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
