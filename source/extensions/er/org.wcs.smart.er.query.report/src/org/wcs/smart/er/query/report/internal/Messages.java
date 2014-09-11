package org.wcs.smart.er.query.report.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.er.query.report.internal.messages"; //$NON-NLS-1$
	public static String SurveyReportQuery_SummaryQueryError;
	public static String SurveyReportQuery_UnsupportedQueryType;
	public static String SurveySummaryQueryResultSetMetadata_QueryJobName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
