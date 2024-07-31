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
package org.wcs.smart.ui.internal.ca.properties;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;

/**
 * Drop handler for moving items in a data model tree. Only supports Categories.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DmTableDropListener extends ViewerDropAdapter {

	private TreeViewer viewer;
	
	/**
	 * @param viewer
	 */
	protected DmTableDropListener(TreeViewer viewer) {
		super(viewer);
		this.viewer = viewer;
		
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@Override
	public boolean performDrop(Object data) {
		
		StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null){
			return false;
		}
		Object obj = selection.getFirstElement();
		
		int loc = getCurrentLocation();
		if (obj instanceof Category && getCurrentTarget() instanceof Category){
			((DataModel)viewer.getInput()).moveCategoryPosition((Category)obj, (Category)getCurrentTarget(), loc == LOCATION_BEFORE);
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {

		StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null){
			return false;
		}
		Object obj = selection.getFirstElement();
		
		if (obj instanceof Category){
			Category c1 = (Category)obj;
			
			if (target instanceof Category){
				Category c2 = (Category)target;
				if ((c1.getParent() == null && c2.getParent() != null) || 
						(c2.getParent() == null && c1.getParent() != null)){
					return false;
				}
				if ( (c1.getParent() == null && c2.getParent() == null) ||
					c1.getParent().equals(c2.getParent())) {
					return true;
				}
			}
		}
	
		return false;
	}

}
