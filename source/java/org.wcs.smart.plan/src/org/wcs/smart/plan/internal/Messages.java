package org.wcs.smart.plan.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.plan.internal.messages"; //$NON-NLS-1$
	public static String SpatialPlanTarget_CategoryName;
	public static String SpatialPlanTarget_Points_Label;
	public static String SpatialPlanTargetPropertyPage_Description_Label;
	public static String SpatialPlanTargetPropertyPage_Description_Required_Error;
	public static String SpatialPlanTargetPropertyPage_Name_Label;
	public static String SpatialPlanTargetPropertyPage_Name_Required_Error;
	public static String SpatialPlanTargetPropertyPage_PageName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
