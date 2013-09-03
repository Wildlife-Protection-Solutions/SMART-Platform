package org.wcs.smart.dataentry.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.dataentry.internal.messages"; //$NON-NLS-1$
	public static String ConfigurableModelPropertyDialog_Message;
	public static String ConfigurableModelPropertyDialog_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
