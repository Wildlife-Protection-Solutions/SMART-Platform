package org.wcs.smart.cybertracker.util;

import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.SmartDB;

public class LanguageUtil {

	public static String getName(NamedItem i, Language language) {
		if (language == null) {
			return i.getName();
		} else {
			String x = i.findNameNull(language);
			if (x == null) {
				x = i.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			}
			return x;
		}
	}
}
