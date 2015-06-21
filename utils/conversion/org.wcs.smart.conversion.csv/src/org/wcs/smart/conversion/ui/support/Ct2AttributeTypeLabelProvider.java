/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
