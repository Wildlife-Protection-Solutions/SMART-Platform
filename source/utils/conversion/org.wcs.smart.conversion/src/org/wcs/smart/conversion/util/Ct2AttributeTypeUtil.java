package org.wcs.smart.conversion.util;

import org.wcs.smart.conversion.model.CategoryMap;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.model.MappedAttributeValue;

public class Ct2AttributeTypeUtil {

	public static final boolean canMap(MappedAttributeType type) {
		if (type == null) return false;
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
	
	public static String getN(MappedAttribute a) {
		return a.getN() != null ? a.getN() : a.getI();
	}

	public static String getN(MappedAttributeValue a) {
		return a.getN() != null ? a.getN() : a.getI();
	}

	public static String getVn(CategoryMap c) {
		return c.getVn() != null ? c.getVn() : c.getVi();
	}

	public static String getAn(CategoryMap c) {
		return c.getAn() != null ? c.getAn() : c.getAi();
	}
}
