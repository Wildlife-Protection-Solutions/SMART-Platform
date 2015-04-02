package org.wcs.smart.intelligence.query.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.intelligence.query.internal.messages"; //$NON-NLS-1$
	public static String FixedQueryColumn_CaIdCol;
	public static String FixedQueryColumn_CaNameCol;
	public static String FixedQueryColumn_DescriptionCol;
	public static String FixedQueryColumn_FromDateColumn;
	public static String FixedQueryColumn_InformantCol;
	public static String FixedQueryColumn_LocationsColumn;
	public static String FixedQueryColumn_NameCol;
	public static String FixedQueryColumn_PatrolColumn;
	public static String FixedQueryColumn_ReceivedDateCol;
	public static String FixedQueryColumn_SourceColumn;
	public static String FixedQueryColumn_ToDateColumn;
	public static String IntelligenceQueryFactory_DefaultNameRecord;
	public static String IntelligenceQueryFactory_DefaultNameSummary;
	public static String IntelligenceRecordQueryType_QueryTypeName;
	public static String IntelligenceSummaryEditor_ErrorMsg;
	public static String IntelligenceSummaryEditor_ErrorTitle;
	public static String IntelligenceSummaryEditor_InvalidQueryMessage;
	public static String IntelligenceSummaryEditor_queryJobName;
	public static String IntelligenceSummaryQueryType_TypeName;
	public static String RecievedDateFilter_ReceivedDateFilterName;
	public static String SummaryIntelligenceQueryEngine_FollowedUpHeaderLongName;
	public static String SummaryIntelligenceQueryEngine_FollowedUpHeaderShortName;
	public static String SummaryIntelligenceQueryEngine_NoFollowedUpHeaderShortName;
	public static String SummaryIntelligenceQueryEngine_NotFollowedUpHeaderLongName;
	public static String SummaryIntelligenceQueryEngine_NumberRecordLongName;
	public static String SummaryIntelligenceQueryEngine_NumberRecordShortName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
