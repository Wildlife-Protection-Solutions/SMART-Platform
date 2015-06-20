package org.wcs.smart.conversion.ui.support;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;

public class Ct2AttributeTypeLabelProvider extends ColumnLabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof MappedAttribute) {
			MappedAttribute a = (MappedAttribute) element;
			return typeToString(a.getType());
		} else if (element instanceof MappedAttributeType) {
			return typeToString((MappedAttributeType)element);
		}
		return super.getText(element);
	}

	private String typeToString(MappedAttributeType type) {
		if (type == null) return "UNKNOWN";
		switch (type) {
			case IGNORE: return "Ignore";
			case TEXT: return "Text";
			case NUMERIC: return "Numeric";
			case BOOL: return "Boolean";
			case REF: return "List or Tree";
			case REF_BOOL: return "Boolean Reference";
			case CATEGORY: return "Category";
			case WP_DATE: return "Date";
			case WP_TIME: return "Time";
			case WP_LAT: return "Latitude";
			case WP_LON: return "Longitude";
			case WP_COMMENT: return "Waypoint comment";
			case META_MEMBERS: return "Members";
			case META_MANDATE: return "Mandate";					
			case META_COMMENT: return "Comment";	
			case META_OBJECT_ID: return "Object ID";
			case TRANSECT_ID: return "Transect ID";
			case TRANSECT_START_LAT: return "Transect start latitude";
			case TRANSECT_START_LON: return "Transect start longitude";
			case TRANSECT_END_LAT: return "Transect end latitude";
			case TRANSECT_END_LON: return "Transect end longitude";
		}
		return type.toString();
	}
}
