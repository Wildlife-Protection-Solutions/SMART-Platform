/*
 * Copyright (C) 2015 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect.i18n;

import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class Messages {
	private static final String BUNDLE_NAME = "org.wcs.smart.connect.i18n.messages"; //$NON-NLS-1$

	//these are for getting patrol metadata; this identifiers
	//which languages i18n labels are provided for
	private static Set<Locale> supportedLocales = new HashSet<>();
	static{
			supportedLocales.add(new Locale("ar")); //$NON-NLS-1$
			supportedLocales.add(new Locale("es")); //$NON-NLS-1$
			supportedLocales.add(new Locale("ka")); //$NON-NLS-1$
			supportedLocales.add(new Locale("th")); //$NON-NLS-1$
	};

	private Messages() {
	}

	/**
	 * If the locale is one of the supported languages returns
	 * the translated string or else returns null;
	 * @param key
	 * @param locale
	 * @return
	 */
	public static String getLocaleString(String key, Locale locale) {
		if (locale != null && supportedLocales.contains(locale)){
			return getString(key, locale);
		}
		return null;
	}

		
	public static String getString(String key, Locale locale) {
		try {
			String value = null;
			if (locale != null){
				value = ResourceBundle.getBundle(BUNDLE_NAME,locale).getString(key);
			}
			if (value == null){
				value = ResourceBundle.getBundle(BUNDLE_NAME).getString(key);
			}
			if (value == null){
				value = '!' + key + '!';
			}
			return value;
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
