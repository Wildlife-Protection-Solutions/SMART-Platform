package org.wcs.smart.connect.dataqueue.i2.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.connect.dataqueue.i2.internal.messages"; //$NON-NLS-1$
	public static String RecordProcessor_cancelled;
	public static String RecordProcessor_invaliduser;
	public static String RecordProcessor_unknowncode;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
