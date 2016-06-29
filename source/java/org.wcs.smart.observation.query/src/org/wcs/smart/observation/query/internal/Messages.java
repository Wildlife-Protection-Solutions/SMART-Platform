package org.wcs.smart.observation.query.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.observation.query.internal.messages"; //$NON-NLS-1$
	public static String AbstractObservationInfoProvider_ProviderNotFound;
	public static String AbstractObservationInfoProvider_SourceNull;
	public static String AbstractObservationInfoProvider_WaypointNull;
	public static String DerbyGridEngine_Progress_CalculatingGridValue;
	public static String DerbyGridEngine_Progress_CreatingObservationTable;
	public static String DerbyGridEngine_Progress_RunningQuery;
	public static String DerbyObservationEngine_Observers;
	public static String DerbyObservationEngine_Progress_CaInfo;
	public static String DerbyObservationEngine_Progress_CategoryData;
	public static String DerbyObservationEngine_Progress_FetchSize;
	public static String DerbyObservationEngine_Progress_ListAttributesData;
	public static String DerbyObservationEngine_Progress_TreeAttributesData;
	public static String DerbyQueryEngine2_Progress_ProcessingAttribute;
	public static String DerbyQueryEngine2_Progress_ProcessingAttributes;
	public static String DerbyQueryEngine2_Progress_RunningQuery;
	public static String DerbySummaryEngine_Progress_CreatingObservationTable;
	public static String DerbySummaryEngine_Progress_CreatingTempTable;
	public static String DerbySummaryEngine_Progress_LoadingHeaders;
	public static String DerbySummaryEngine_Progress_ProcessingValue;
	public static String DerbySummaryEngine_Progress_RunningQuery;
	public static String FixedQueryColumn_CaIdColumnName;
	public static String FixedQueryColumn_CaNameColumnName;
	public static String FixedQueryColumn_CommentColumnName;
	public static String FixedQueryColumn_DirectionColumnName;
	public static String FixedQueryColumn_DistanceColumnName;
	public static String FixedQueryColumn_ObserverColumnName;
	public static String FixedQueryColumn_WaypointDateColumnName;
	public static String FixedQueryColumn_WaypointIdColumnName;
	public static String FixedQueryColumn_WaypointSourceColumnName;
	public static String FixedQueryColumn_WaypointTimeColumnName;
	public static String FixedQueryColumn_xColumnName;
	public static String FixedQueryColumn_yColumnName;
	public static String GeneralContentProvider_ConservationAreaLabel;
	public static String GeneralContentProvider_ObserverLabel;
	public static String GriddedFilterPanel_CopyError;
	public static String GriddedFilterPanel_RefreshJobName;
	public static String GriddedQuery_DefaultQueryName;
	public static String GriddedQueryDefinitionComposite_GridValueDefinitionSectionHeader;
	public static String PatrolGriddedQueryDefinitionPanel_CRSError;
	public static String PatrolGriddedQueryDefinitionPanel_GridSizeError1;
	public static String PatrolGriddedQueryDefinitionPanel_GridSizeError2;
	public static String PatrolGriddedQueryDefinitionPanel_ValuePanelTitle;
	public static String PatrolGriddedQueryDefinitionPanel_ValueRequiredError;
	public static String PatrolSummaryGroupByValuePanel_GroupByPanelTitle;
	public static String PatrolSummaryGroupByValuePanel_ValueError;
	public static String Query_DefaultQueryName;
	public static String QueryColumn_LoadingObservationColumnJobName;
	public static String QueryColumn_ObservationCategoryTableHeader;
	public static String QueryColumnCache_LoadingWPQueryColumnJobName;
	public static String QueryDataSource_SchemaError;
	public static String QueryDataSourceFactory_Description;
	public static String QueryDataSourceFactory_DisplayName;
	public static String QueryDataSourceFactory_queryUuidParameterName;
	public static String QueryDataSourceFactory_ReadOnlyError;
	public static String QueryFilterContentProvider_WaypointSourceName;
	public static String QueryFilterPanel_AreaFilters;
	public static String QueryFilterPanel_GeneralFilters;
	public static String QueryFilterPanel_RefreshTree_JobTitle;
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
	public static String SimpleQuery_DropItemParseError;
	public static String SummaryFilterPanel_AreaGroupBy;
	public static String SummaryFilterPanel_GeneralItemGroupBy;
	public static String SummaryFilterPanel_RefreshTreeJobName;
	public static String SummaryQuery_DefaultQueryName;
	public static String SummaryValueGroupByPanel_GroupByValuePanelTitle;
	public static String WaypointFilterProcessor_filterProgress;
	public static String WaypointFilterProcessor_progress1;
	public static String WaypointSourceFilter_InvalidSourceFilter;
	public static String WaypointSourceFilterDropItem_IncidentSourceDropItem;
	public static String WaypointSourceGroupByDropItem_AllLabel;
	public static String WaypointSourceGroupByDropItem_FiltersLabel;
	public static String WaypointSourceGroupByDropItem_WaypointSourceLabel;
	public static String DerbyGridEngine_Error_GridValueNotSupported;
	public static String ObservationDropItemFactory_QueryItemNotSupported;
	public static String ObservationGridQueryType_QueryName;
	public static String ObservationQueryTemplateCloner_GridProgress;
	public static String ObservationQueryTemplateCloner_IncidentProgress;
	public static String ObservationQueryTemplateCloner_ObservationProgress;
	public static String ObservationQueryTemplateCloner_SummaryProgress;
	public static String ObservationQueryTemplateCloner_TaskName;
	public static String ObservationQueryType_QueryName;
	public static String ObservationSummaryQueryType_QueryName;
	public static String ObservationWaypointQueryType_QueryName;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
