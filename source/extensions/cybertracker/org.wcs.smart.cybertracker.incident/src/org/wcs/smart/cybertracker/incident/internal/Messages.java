package org.wcs.smart.cybertracker.incident.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.cybertracker.incident.internal.messages"; //$NON-NLS-1$
	
	public static String CtIncidentDatabaseUpgrader_Progress;
	public static String CtIncidentPackageConfigurator_ConfigLabel;
	public static String CtIncidentPackageConfigurator_ConfigurableModelLabel;
	public static String CtIncidentPackageConfigurator_ConfigurationSectionHeader;
	public static String CtIncidentPackageConfigurator_DateLabel;
	public static String CtIncidentPackageConfigurator_DefaultName;
	public static String CtIncidentPackageConfigurator_DetailsLabel;
	public static String CtIncidentPackageConfigurator_DeviceSettingsLabel;
	public static String CtIncidentPackageConfigurator_EmployeeListDetails;
	public static String CtIncidentPackageConfigurator_Employees;
	public static String CtIncidentPackageConfigurator_ErrorLoadingSettings;
	public static String CtIncidentPackageConfigurator_InvalidType;
	public static String CtIncidentPackageConfigurator_ModelRequired;
	public static String CtIncidentPackageConfigurator_NameLabel;
	public static String CtIncidentPackageConfigurator_NoPackageOp;
	public static String CtIncidentPackageConfigurator_ObserverHeader;
	public static String CtIncidentPackageConfigurator_ObserverRequired;
	public static String CtIncidentPackageConfigurator_OriginalDmOption;
	public static String CtIncidentPackageConfigurator_PackageRequired;
	public static String CtIncidentPackageConfigurator_SettingLabel;
	public static String CtIncidentPackageConfigurator_SettingsRequired;
	public static String CtIncidentPackageConfigurator_SettingsTabName;
	public static String CtIncidentPackageConfigurator_Settingstooltip;
	public static String CtIncidentPackageConfigurator_ShowOnlyChecked;
	public static String CtIncidentPackageConfigurator_Teams;
	public static String CtIncidentPackageConfigurator_UnknownOp;
	public static String CtIncidentPackageConfigurator_VersionLabel;
	public static String IncidentCyberTrackerLabelProvider_CreatedMessage;
	public static String IncidentCyberTrackerLabelProvider_ModifiedMessage;
	public static String IncidentJsonProcessor_CanceledMsg;
	
	public static String IncidentJsonProcessor_NotificationError;
	public static String IncidentJsonProcessor_NotificationError2;
	public static String IncidentJsonProcessor_WaringsTitle;
	public static String IncidentJsonProcessor_WarningsMessage;
	public static String IncidentPackageContribution_CmModelLabel;
	public static String IncidentPackageContribution_CollectIncidentsOp;
	public static String IncidentPackageContribution_ConfigurationGroupLablel;
	public static String IncidentPackageContribution_LoadingCmJobname;
	public static String IncidentPackageContribution_ModelLabel;
	public static String IncidentPackageContribution_NoneOption;
	public static String IncidentPackageContribution_OriginalDmLabel;
	public static String IncidentPackageContribution_TaskName;
	public static String IncidentPackageExporter_TaskName;
	public static String IncidentPackageManager_CancelMessage;
	public static String IncidentPackageManager_CancelTitle;
	public static String IncidentPackageManager_ExportTask;
	public static String IncidentPackageManager_IncidentPackageDefaultName;
	public static String IncidentPackageManager_IncidentPackageName;
	public static String IncidentPackageManager_Progress;
	public static String IncidentPackageUiContribution_cmRequired;
	public static String IncidentPackageUiContribution_CmRequired;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
