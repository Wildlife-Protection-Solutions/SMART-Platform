package org.wcs.smart.asset.query.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.asset.query.internal.messages"; //$NON-NLS-1$
	public static String AttributeValueItem_AggNoSupported;
	public static String DeleteObservationResultInfoProvider_CancelBtn;
	public static String DeleteObservationResultInfoProvider_DeleteMsg;
	public static String DeleteObservationResultInfoProvider_DeleteObsBtn;
	public static String DeleteObservationResultInfoProvider_DeleteObsError;
	public static String DeleteObservationResultInfoProvider_DeleteTitle;
	public static String DeleteObservationResultInfoProvider_DeleteWaypointMsg;
	public static String DeleteObservationResultInfoProvider_DeleteWaypointTitle;
	public static String DeleteObservationResultInfoProvider_DeleteWpBtn;
	public static String DeleteObservationResultInfoProvider_DeleteWpError;
	public static String DerbyObservationEngine_FindDataColumns;
	public static String DerbyObservationEngine_LoadingResultTask;
	public static String DerbyObservationEngine_Progress_CaInfo;
	public static String DerbyObservationEngine_Progress_CategoryData;
	public static String DerbyObservationEngine_Progress_FetchSize;
	public static String DerbyObservationEngine_Progress_LeaderPilotData;
	public static String DerbyObservationEngine_Progress_ListAttributesData;
	public static String DerbyObservationEngine_Progress_MandateData;
	public static String DerbyObservationEngine_Progress_StationData;
	public static String DerbyObservationEngine_Progress_TeamData;
	public static String DerbyObservationEngine_Progress_TransportData;
	public static String DerbyObservationEngine_Progress_TreeAttributesData;
	public static String DerbyQueryEngine2_Progress_RunningQuery;
	public static String DerbySummaryEngine_InvalidRateFilterComputation;
	public static String DerbySummaryEngine_Progress_CreatingObservationTable;
	public static String DerbySummaryEngine_Progress_CreatingTempTable;
	public static String DerbySummaryEngine_Progress_LoadingHeaders;
	public static String DerbySummaryEngine_Progress_RunningQuery;
	public static String DerbySummaryEngine_ProgressValueProgressLabel;
	public static String FilterValidator_AssetFilter_EmployeeError;
	public static String FilterValidator_AssetFilter_UnqiueIdMatchingError;
	public static String FilterValidator_AssetFilter_ValueMatchingError;
	public static String FilterValidator_AssetFilterError;
	public static String FilterValidator_AssetFilterErrorB;
	public static String FixedQueryColumn_CaIdColumnName;
	public static String FixedQueryColumn_CaNameColumnName;
	public static String FixedQueryColumn_CommentColumnName;
	public static String FixedQueryColumn_DirectionColumnName;
	public static String FixedQueryColumn_DistanceColumnName;
	public static String FixedQueryColumn_WaypointDateColumnName;
	public static String FixedQueryColumn_WaypointIdColumnName;
	public static String FixedQueryColumn_WaypointTimeColumnName;
	public static String FixedQueryColumn_xColumnName;
	public static String FixedQueryColumn_yColumnName;
	public static String AssetAttributeValueItem_AttributeNotFound;
	public static String AssetAttributeValueItem_CategoryNotFound;
	public static String AssetAttributeValueItem_ListItemNotFound;
	public static String AssetAttributeValueItem_TreeNodeNotFound;
	public static String AssetCategoryValueItem_CategoryNotFound;
	public static String AssetFilter_InvalidPrefix;
	public static String AssetFilterTreeItem_AssetFiltersTreeItem;
	public static String AssetGroupBy_CouldNotParse;
	public static String AssetGroupBy_CouldNotResolveFilter;
	public static String AssetGroupBy_Error_NoEmployee;
	public static String AssetGroupBy_Error_NoMatchingValue;
	public static String AssetGroupBy_Error_NotUniqueId;
	public static String AssetGroupBy_KeyNotFoundError;
	public static String AssetGroupBy_AssetOptionParseError;
	public static String AssetGroupByDropItem_AllLabel;
	public static String AssetGroupByDropItem_Error_LoadingListItems;
	public static String AssetGroupByDropItem_FiltersLabel;
	public static String AssetGroupByDropItem_IncludedLabel;
	public static String AssetGroupByTreeItem_AssetGroupBys;
	public static String AssetQueryOptions_CaGroupByOptionName;
	public static String AssetQueryOptions_QueryOpStation;
	public static String AssetQueryValidator_CouldNotMatchFilter;
	public static String AssetObservationQueryType_AssetObservationQueryTypeName;
	public static String AssetSummaryGroupByValuePanel_GroupByPanelTitle;
	public static String AssetSummaryGroupByValuePanel_ValueError;
	public static String AssetSummaryQueryType_SummaryQueryTypeName;
	public static String AssetWaypointQueryType_WaypointQueryTypeName;
	public static String Query_DefaultQueryName;
	public static String QueryColumn_LoadingObservationColumnJobName;
	public static String QueryColumn_LoadingAssetColumnJobName;
	public static String QueryColumn_ObservationCategoryTableHeader1;
	public static String QueryColumnCache_LoadingWPQueryColumnJobName;
	public static String QueryDataSource_SchemaError;
	public static String QueryDataSourceFactory_Description;
	public static String QueryDataSourceFactory_DisplayName;
	public static String QueryDataSourceFactory_queryUuidParameterName;
	public static String QueryDataSourceFactory_ReadOnlyError;
	public static String QueryFilterPanel_RefreshTree_JobTitle;
	public static String QueryFilterPanel_TreeNodeLabel;
	public static String QueryFilterView_AddToQueryButton;
	public static String QueryGeoResource_Error_NoDatasource;
	public static String QueryGeoResource_ServiceError;
	public static String QueryPlugIn_Error_DialogTitle;
	public static String QueryServiceInfo_Description;
	public static String QueryServiceInfo_Keyword1;
	public static String QueryServiceInfo_Keyword2;
	public static String QueryServiceInfo_Keyword3;
	public static String QueryServiceInfo_Keyword4;
	public static String QueryServiceInfo_Keyword5;
	public static String QueryServiceInfo_Keyword6;
	public static String QueryServiceInfo_ServiceName;
	public static String QueryTemplateCloner_ProgressCopyObservation;
	public static String QueryTemplateCloner_ProgressCopySummary;
	public static String QueryTemplateCloner_ProgressCopyWaypoint;
	public static String QueryTemplateCloner_ProgressQuery;
	public static String SimpleQuery_DropItemParseError;
	public static String SummaryFilterPanel_RefreshTreeJobName;
	public static String SummaryFilterPanel_TreeNodeLabel;
	public static String SummaryQuery_DefaultQueryName;
	public static String SummaryValueGroupByPanel_GroupByValuePanelTitle;
	public static String EditObservationResultInfoProvider_DeleteError;
	public static String EditObservationResultInfoProvider_EditLabel;
	public static String MapWaypointEditManager_MoveError;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
