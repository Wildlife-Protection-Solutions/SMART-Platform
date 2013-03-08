package org.wcs.smart.data.oda.smart.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.data.oda.smart.ui.internal.messages"; //$NON-NLS-1$
	public static String Activator_Error_DialogTitle;
	public static String CustomDataSetWizardPage_DataSetName_Postfix;
	public static String CustomDataSetWizardPage_DefaultDataSetName;
	public static String CustomDataSetWizardPage_Error_CouldNotCloseConnection;
	public static String CustomDataSetWizardPage_Error_QueryMustSelected;
	public static String CustomDataSetWizardPage_LinkToParameters_ToReportParameters;
	public static String CustomDataSetWizardPage_LoadQueryJobName;
	public static String CustomDataSetWizardPage_PickQuery_Message;
	public static String CustomDataSetWizardPage_SelectQuery_Label;
	public static String SmartTableDataSetWizardPage_Error_CouldNoClose;
	public static String SmartTableDataSetWizardPage_Error_MustLinkReportParameters;
	public static String SmartTableDataSetWizardPage_Error_MustSelectTable;
	public static String SmartTableDataSetWizardPage_PickSmartTable_Message;
	public static String SmartTableDataSetWizardPage_SelectTableName_Label;
	public static String SmartDataSourcePropertyPage_Label;
	public static String SmartDataSourcePropertyPage_Title;
	public static String SmartDataSourceWizardPage_Label;
	public static String SmartDataSourceWizardPage_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
