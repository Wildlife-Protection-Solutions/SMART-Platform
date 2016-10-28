/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui;

import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Viewer filter that filters based on the values returned by one or more
 * label providers.  If any one of the label provides matches the text, the value
 * of true is returned.
 */
public class TableColumnViewerFilter extends TextViewerFilter {

	private ColumnLabelProvider[] columns;
	
	public TableColumnViewerFilter(Viewer viewer, ColumnLabelProvider column){
		super(viewer);
		this.columns = new ColumnLabelProvider[]{column};
	}
	
	public TableColumnViewerFilter(Viewer viewer, ColumnLabelProvider... column){
		super(viewer);
		this.columns = column;
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (filter == null || filter.length() == 0) {
			return true;
		}
		String search = ".*" + Pattern.quote(filter.toLowerCase()) + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
		for (ColumnLabelProvider col : columns){
			String text = col.getText(element);
			if (text != null && text.toLowerCase().matches(search)) return true;
		}
		return false;
	}

}
