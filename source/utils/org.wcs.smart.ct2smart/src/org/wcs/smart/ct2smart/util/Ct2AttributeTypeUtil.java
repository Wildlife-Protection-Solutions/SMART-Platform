package org.wcs.smart.ct2smart.util;

import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;

public class Ct2AttributeTypeUtil {

	public static final boolean canMap(Ct2AttributeType type) {
		switch (type) {
			case TEXT:
			case NUMERIC:
			case BOOL:
			case REF:
				return true;
			default:
				return false;
		}
	}
}
