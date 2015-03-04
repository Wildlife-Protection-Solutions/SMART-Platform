package org.wcs.smart.conversion.util;

import org.wcs.smart.conversion.model.MappedAttributeType;

public class Ct2AttributeTypeUtil {

	public static final boolean canMap(MappedAttributeType type) {
		switch (type) {
			case TEXT:
			case NUMERIC:
			case BOOL:
			case REF:
			case REF_BOOL:
				return true;
			default:
				return false;
		}
	}
}
