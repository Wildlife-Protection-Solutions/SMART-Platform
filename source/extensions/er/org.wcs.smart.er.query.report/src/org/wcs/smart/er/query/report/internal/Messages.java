package org.wcs.smart.er.query.report.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.er.query.report.internal.messages"; //$NON-NLS-1$
	public static String SurveyDesignPropertyTable_DescriptionLabel;
	public static String SurveyDesignPropertyTable_EndDateLabel;
	public static String SurveyDesignPropertyTable_KeyLabel;
	public static String SurveyDesignPropertyTable_LongTableName;
	public static String SurveyDesignPropertyTable_NameLabel;
	public static String SurveyDesignPropertyTable_StartDateLabel;
	public static String SurveyDesignPropertyTable_StatusLabel;
	public static String SurveyReportQuery_SummaryQueryError;
	public static String SurveyReportQuery_UnsupportedQueryType;
	public static String SurveySamplingUnitTable_IDColumnLabel;
	public static String SurveySamplingUnitTable_LongDisplayName;
	public static String SurveySamplingUnitTable_StateColumnLabel;
	public static String SurveySummaryQueryResultSetMetadata_QueryJobName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
