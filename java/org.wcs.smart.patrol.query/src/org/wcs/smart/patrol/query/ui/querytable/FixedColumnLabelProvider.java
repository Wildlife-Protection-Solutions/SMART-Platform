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
package org.wcs.smart.patrol.query.ui.querytable;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

/**
 * Class represents one of the fixed table columns that do not change from
 * conservation area to conservation area.
 * 
 * <p>
 * This includes items such as the patrol id, patrol type etc but not items
 * related to the datamodel.
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class FixedColumnLabelProvider extends ColumnLabelProvider {

	private QueryColumn column;

	/**
	 * Creates a new fixed table column.
	 * 
	 * @param column
	 *            the column definition
	 */
	public FixedColumnLabelProvider(QueryColumn column) {
		this.column = column;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object )
	 */
	public String getText(Object element) {
		if (element instanceof PatrolQueryResultItem) {
			return asString(column.getValue((PatrolQueryResultItem) element),
					column.getType());
		}
		return element == null ? "" : element.toString();//$NON-NLS-1$
	}

	private static String asString(Object value, ColumnType type) {
		if (type == ColumnType.BOOLEAN) {
			if ((Boolean) value) {
				return Attribute.BOOLEAN_TRUE_LABEL;
			} else {
				return Attribute.BOOLEAN_FALSE_LABEL;
			}
		} else if (type == ColumnType.DATE) {
			if ((Date)value == null){
				return ""; //$NON-NLS-1$
			}
			return DateFormat.getDateInstance().format((Date) value);
		} else if (type == ColumnType.TIME) {
			if ((Date)value == null){
				return ""; //$NON-NLS-1$
			}
			return DateFormat.getTimeInstance().format((Date) value);
		} else if (type == ColumnType.STRING) {
			return (String) value;
		} else if (type == ColumnType.INTEGER) {
			return String.valueOf((Integer) value);
		} else if (type == ColumnType.LONG) {
			return String.valueOf((Long) value);
		} else if (type == ColumnType.NUMBER) {
			if (value instanceof Double) {
				return String.valueOf((Double) value);
			} else if (value instanceof Float) {
				return String.valueOf((Float) value);
			} else if (value instanceof Integer) {
				return String.valueOf((Integer) value);
			}
		}
		return ""; //$NON-NLS-1$

	}
}
