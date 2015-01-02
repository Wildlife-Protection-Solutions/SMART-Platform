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
package org.wcs.smart.dataentry.dialog;

import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;

/**
 * Drop handler for moving nodes inside a configurable model tree attribute.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CmAttributeTreeDropListener extends ViewerDropAdapter {

	private TreeViewer viewer;
	
	/**
	 * @param viewer
	 */
	protected CmAttributeTreeDropListener(TreeViewer viewer) {
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
		
		if (obj instanceof CmAttributeTreeNode && getCurrentTarget() instanceof CmAttributeTreeNode){
			moveAttributePosition((CmAttributeTreeNode)obj, (CmAttributeTreeNode)getCurrentTarget(), getCurrentLocation() == LOCATION_BEFORE);
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {

		StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null){
			return false;
		}
		Object obj = selection.getFirstElement();

		if (obj instanceof CmAttributeTreeNode) {
			CmAttributeTreeNode sourceNode = (CmAttributeTreeNode) obj;
			if (target instanceof CmAttributeTreeNode) {
				CmAttributeTreeNode targetNode = (CmAttributeTreeNode) target;
				CmAttributeTreeNode p1 = sourceNode.getParent();
				CmAttributeTreeNode p2 = targetNode.getParent();
				if ((p1 == null && p2 != null) || (p1 != null && p2 == null))
					return false;
				return (p1 == p2) || (p1.equals(p2));
			}
		}
		return false;
	}

	private void moveAttributePosition(CmAttributeTreeNode source, CmAttributeTreeNode target, boolean moveBefore) {
		if (source == target || source.equals(target)) {
			return;
		}

		List<CmAttributeTreeNode> attrList = source.getParent() != null ?
					source.getParent().getChildren() :
					((CmAttribute)viewer.getInput()).getCurrentTree();

		attrList.remove(source);
		if (moveBefore) {
			attrList.add(attrList.indexOf(target), source);
		} else {
			attrList.add(attrList.indexOf(target) + 1, source);
		}
		
		for (int i = 0; i < attrList.size(); i ++){
			attrList.get(i).setNodeOrder(i);
		}
	}

}
