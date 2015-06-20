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

import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;

/**
 * Tree viewer that displays a tree in a box similar
 * to a combo box.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class DmTreeDropDownViewer extends TreeDropDownViewer {

	public DmTreeDropDownViewer(Shell parent, DmTreeContentProvider contentProvider, SmartCategoryLabelProvider labelProvider) {
		super(parent, contentProvider, labelProvider);
	}
	
	/**
	 * Sets the current datamodel attribute to display
	 * in the tree.
	 * 
	 * @param att
	 */
	public void setInput(DataModel model) {
		getDmTreeViewer().setInput(model.getCategories().getCategories());
		getDmTreeViewer().expandToLevel(1);
		getDmTreeViewer().refresh();
	}
}
