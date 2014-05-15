package org.wcs.smart.observation.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.observation.internal.messages"; //$NON-NLS-1$
	
	public static String AttachmentCellEditor_NoAttachment_Label;
	public static String AttachmentCellEditor_TableCell_Label;
	public static String AttachmentDialog_DialogMessage;
	public static String AttachmentDialog_DialogTitle;

	public static String AttachmentDialog_ObservationAttachmentLbl;
	public static String AttributeDMAdvisor_Error_AttributeNotDeletable;
	public static String AttributeDMAdvisor_Error_InvalidObjectType;
	public static String AttributeListItemDMAdvisor_DeleteError;
	public static String AttributeListItemDMAdvisor_InvalidObjectType;
	public static String AttributeTable_AttachmentsColumnName;

	public static String AttributeTable_AttachmentsFileCountLabel;

	public static String AttributeTreeNodeDMAdvisor_DeleteError;
	public static String AttributeTreeNodeDMAdvisor_InvalidObjectType;
	public static String AttributeWizardPage_AddObservation_Button;

	public static String AttributeWizardPage_AttachmentsLabel;
	public static String AttributeWizardPage_CannotCreateObservationError_DialogMessage;
	public static String AttributeWizardPage_CategoryObservations_Label;
	public static String AttributeWizardPage_DataObservations_DialogMessage;
	public static String AttributeWizardPage_DataObservations_DialogTitle;
	public static String AttributeWizardPage_Error_DialogTitle;

	public static String AttributeWizardPage_FileNotFoundError;
	public static String AttributeWizardPage_PageMessage;
	public static String AttributeWizardPage_PageName;
	public static String AttributeWizardPage_PageNumberLabel;
	public static String AttributeWizardPage_PageTitle;
	public static String AttributeWizardPage_SaveModificationsWarningMessage;
	public static String AttributeWizardPage_UpdateObsButton;
	public static String AttributeWizardPage_Warning_DialogTitle;
	
	public static String CaDeleteHandler_ProgressDeleteWp;

	public static String CategoryAttributeDMAdvisor_DeleteError;
	public static String CategoryAttributeDMAdvisor_InvalidObjectType;
	public static String CategoryDMAdvisor_DeleteError;
	public static String CategoryDMAdvisor_Error_InvalidObjectType;
	
	public static String ObservationCellEditor_LoadDataModel_JobName;
	public static String ObservationCellEditor_NoObservations_Label;
	public static String ObservationCloner_ProgressName;

	public static String ObservationCloner_TaskName;

	public static String ObservationOptionsPropertyPage_Projection_LoadError;

	public static String ObservationOptionsPropertyPage_ViewProjectionDescr;

	public static String ObservationOptionsPropertyPage_ViewProjectionGroupTitle;

	public static String PatrolHibernateManager_Error_CouldNoLoadPatrolOptions;
	public static String ObservationSummaryWizardPage_PageMessage;
	public static String ObservationSummaryWizardPage_PageName;
	public static String ObservationSummaryWizardPage_PageTitle;
	public static String ObservationWizard_ConfirmCancel_DialogMessage;
	public static String ObservationWizard_ConfirmCancel_DialogTitle;
	public static String ObservationWizard_PageName;

	public static String ObservationWizard_SaveError;
	public static String ObservationWizardPage_PageMessage;
	public static String ObservationWizardPage_PageName;
	public static String ObservationWizardPage_PageTitle;
	
	public static String PatrolOptionsPropertyPage_DialogMessage;
	public static String PatrolOptionsPropertyPage_DialogTitle;
	public static String PatrolOptionsPropertyPage_DistanceDirection_DescLabel;
	public static String PatrolOptionsPropertyPage_DistanceDirection_OpLabel;
	public static String PatrolOptionsPropertyPage_Error_CouldNotSave;
	public static String PatrolOptionsPropertyPage_Error_EditTime;
	public static String PatrolOptionsPropertyPage_PageName;
	public static String PatrolOptionsPropertyPage_PatrolEditOptions_DaysLabel;
	public static String PatrolOptionsPropertyPage_PatrolEditOptions_DescLabel;
	public static String PatrolOptionsPropertyPage_PatrolEditOptions_Label;
	public static String PatrolOptionsPropertyPage_RecordDistanceDirectory_Op;
	public static String SearchTree_AddCategories_ToolTip;
	public static String SearchTree_ClearSelection_Tooltip;
	public static String SearchTree_DefaultText;
	public static String SearchTree_ItemsMatchedLabel;
	public static String SearchTree_RemoveAllCategories_Tooltip;
	public static String SearchTree_RemoveCategories_ToolTip;
	public static String SmartPatrolPlugIn_Error_DialogMessage1;

	public static String Waypoint_CloneError;

	public static String WaypointInfoView_DateTimeLabel;
	public static String WaypointInfoView_LoadingThumbnails;
	public static String WaypointInfoView_UpdateJobName;
	public static String WaypointInfoView_WaypointIdLabel;

	public static String WaypointObservation_CloneError;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
