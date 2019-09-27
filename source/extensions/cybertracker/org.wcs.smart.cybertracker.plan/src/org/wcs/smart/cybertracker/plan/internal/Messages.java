package org.wcs.smart.cybertracker.plan.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.cybertracker.plan.internal.messages"; //$NON-NLS-1$
	public static String PlanTargetNavigationTargetProvider_TypeName;
	public static String PlanTargetWizardPage_Message;
	public static String PlanTargetWizardPage_PlanLAbel;
	public static String PlanTargetWizardPage_SelectaPlan;
	public static String PlanTargetWizardPage_TargetsLabel;
	public static String PlanTargetWizardPage_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
