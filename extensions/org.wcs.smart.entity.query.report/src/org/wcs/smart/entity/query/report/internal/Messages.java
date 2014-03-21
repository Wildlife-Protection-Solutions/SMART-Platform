package org.wcs.smart.entity.query.report.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.observation.query.report.internal.messages"; //$NON-NLS-1$
	public static String ObservationReportQuery_MetadataError;
	public static String ObservationReportQuery_SummaryDateGroupByInvalid;
	public static String ObservationSummaryQueryResultSetMetadata_JobName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
