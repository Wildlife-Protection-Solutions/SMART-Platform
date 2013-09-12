package org.wcs.smart.dataentry.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.dataentry.internal.messages"; //$NON-NLS-1$
	public static String AbstractInfoComposite_Button_AddCategory;
	public static String AbstractInfoComposite_Button_AddGroup;
	public static String AbstractInfoComposite_DisplayName;
	public static String CmNodeInfoComposite_Button_Delete;
	public static String CmNodeInfoComposite_Category;
	public static String CmNodeInfoComposite_Key;
	public static String ConfigurableModelEditDialog_Message;
	public static String ConfigurableModelEditDialog_Title;
	public static String ConfigurableModelPropertyDialog_Button_Create;
	public static String ConfigurableModelPropertyDialog_Button_Edit;
	public static String ConfigurableModelPropertyDialog_LoadModelsListError;
	public static String ConfigurableModelPropertyDialog_Message;
	public static String ConfigurableModelPropertyDialog_Title;
	public static String DataentryHibernateManager_ConfigurableModel_Save_Error;
	public static String TranslatableNameComposite_Button_Translate;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
