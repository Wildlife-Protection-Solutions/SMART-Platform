package org.wcs.smart.connect.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "org.wcs.smart.connect.i18n.messages"; //$NON-NLS-1$


	private Messages() {
	}

	public static String getString(String key, Locale locale) {
		try {
			String value = ResourceBundle.getBundle(BUNDLE_NAME,locale).getString(key);
			if (value == null){
				value = ResourceBundle.getBundle(BUNDLE_NAME).getString(key);
			}
			return value;
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
