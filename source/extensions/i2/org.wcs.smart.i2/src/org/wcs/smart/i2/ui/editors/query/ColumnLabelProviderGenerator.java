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
package org.wcs.smart.i2.ui.editors.query;

import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;

/**
 * Generates a label provider for query columns
 * 
 * @author Emily
 *
 */
public class ColumnLabelProviderGenerator {

	public static ColumnLabelProvider createLabelProvider(IQueryColumn column){
		return new ColumnLabelProvider(){
			public String getText(Object element){
				if (element == null) return "";
				if (element instanceof IResultItem){
					return column.getValue((IResultItem)element, Locale.getDefault());	
				}
				return super.getText(element);
			}
		};
	}
}
