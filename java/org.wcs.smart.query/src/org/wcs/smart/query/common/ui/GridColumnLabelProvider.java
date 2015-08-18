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
package org.wcs.smart.query.common.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.query.common.model.GridResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * An table column in the results table that represents an attribute.
 * <p>
 * There should be one column for each attribute defined in the data model
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */

public class GridColumnLabelProvider extends ColumnLabelProvider {


	private QueryColumn column;

	public GridColumnLabelProvider(QueryColumn column) {
		this.column = column;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof GridResultItem) {
			Object value = column.getValue((GridResultItem) element);
			if (value == null) {
				return ""; //$NON-NLS-1$
			} else {
				return value.toString();
			}
		}
		return element == null ? "" : element.toString();//$NON-NLS-1$
	}

}
