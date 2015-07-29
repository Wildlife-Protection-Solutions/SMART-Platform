package org.wcs.smart.cybertracker.survey.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.cybertracker.survey.internal.messages"; //$NON-NLS-1$
	public static String SurveyCTExportDialog_LoadSurveyDesigns_Error;
	public static String SurveyCTExportDialog_SurveyDesign;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
