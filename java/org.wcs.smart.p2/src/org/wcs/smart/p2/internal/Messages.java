package org.wcs.smart.p2.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.p2.internal.messages"; //$NON-NLS-1$
	public static String InstalledSoftwarePage_ButtonName;
	public static String InstalledSoftwarePage_ConfirmDelete;
	public static String InstalledSoftwarePage_DialogTitle;
	public static String InstalledSoftwarePage_ErrordialogTitle;
	public static String InstalledSoftwarePage_InvalidUserName;
	public static String InstalledSoftwarePage_UserNamePassordConfirm;
	public static String InstallNewSoftwareHandler_ProgressTaskName;
	public static String PreferenceInitializer_PreferenceError;
	public static String PreloadingRepositoryHandler_CannotCompleteRequest;
	public static String PreloadingRepositoryHandler_SoftwareUpdates;
	public static String SitesPreferencePage_InvalidUser;
	public static String SmartPolicy_InstallQuestion;
	public static String SmartPolicy_PreferencePageName;
	public static String SmartPolicy_Question;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
