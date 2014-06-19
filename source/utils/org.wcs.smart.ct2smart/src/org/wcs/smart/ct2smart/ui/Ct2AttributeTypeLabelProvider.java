package org.wcs.smart.ct2smart.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;

public class Ct2AttributeTypeLabelProvider extends ColumnLabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof Ct2Attribute) {
			Ct2Attribute a = (Ct2Attribute) element;
			return typeToString(a.getType());
		} else if (element instanceof Ct2AttributeType) {
			return typeToString((Ct2AttributeType)element);
		}
		return super.getText(element);
	}

	private String typeToString(Ct2AttributeType type) {
		switch (type) {
			case IGNORE: return "Ignore";
			case TEXT: return "Text";
			case NUMERIC: return "Numeric";
			case BOOL: return "Boolean";
			case REF: return "List or Tree";
			case CATEGORY: return "Category";
			case META_DATE: return "Date";
			case META_TIME: return "Time";
			case META_LAT: return "Latitude";
			case META_LON: return "Longitude";
			case META_MEMBERS: return "Members";
			case META_MANDATE: return "Mandate";					
		}
		return type.toString();
	}
}
