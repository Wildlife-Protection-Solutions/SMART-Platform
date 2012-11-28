package org.wcs.smart.patrol.internal.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.patrol.internal.ui.messages"; //$NON-NLS-1$
	public static String CommentWizardPage_PageMessage;
	public static String CommentWizardPage_PageName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
