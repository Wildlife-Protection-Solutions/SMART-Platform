package org.wcs.smart.i2.patrol.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.i2.patrol.internal.messages"; //$NON-NLS-1$
	public static String AddPluginJob_ErrorMsg;
	public static String AddPluginJob_jobName;
	public static String DatabaseUpgrader_UpgradeMessage;
	public static String DeleteCaHandler_tasktitle;
	public static String PatrolEditorContribution_AddError;
	public static String PatrolEditorContribution_ConfirmRemove;
	public static String PatrolEditorContribution_DeleteError;
	public static String PatrolEditorContribution_PatrolMotiviated;
	public static String PatrolProfileRecordPatrolData_AnyOption;
	public static String PatrolQueryFilterViewer_MotivatedByName;
	public static String PatrolQueryFilterViewer_ParseError;
	public static String PatrolQueryFilterViewer_RecordNotFoundError;
	public static String RecordSelectorDialog_Message;
	public static String RecordSelectorDialog_Title;
	public static String RemovePluginJob_Error;
	public static String RemovePluginJob_JobName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
