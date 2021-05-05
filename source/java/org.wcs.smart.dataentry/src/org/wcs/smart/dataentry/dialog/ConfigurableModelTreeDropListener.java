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

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Drop handler for moving items in a configurable model tree.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ConfigurableModelTreeDropListener extends ViewerDropAdapter {

	private TreeViewer viewer;
	
	/**
	 * @param viewer
	 */
	protected ConfigurableModelTreeDropListener(TreeViewer viewer) {
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
		
		if (obj instanceof CmNode && getCurrentTarget() instanceof CmNode){
			ConfigurableModel cm = (ConfigurableModel)viewer.getInput();
			cm.moveNodePosition((CmNode)obj, (CmNode)getCurrentTarget(), getCurrentLocation() == LOCATION_BEFORE);
			return true;
		} else if (obj instanceof CmAttribute && getCurrentTarget() instanceof CmAttribute) {
//			ConfigurableModel cm = (ConfigurableModel)viewer.getInput();
			moveAttributePosition((CmAttribute)obj, (CmAttribute)getCurrentTarget(), getCurrentLocation() == LOCATION_BEFORE);
			return true;
		}
		return false;
	}

	/**
	 * Moves {@link CmAttribute} to a new position in the sibling list.
	 * 
	 * @param source the attribute to move
	 * @param target the attribute to move it to
	 * @param moveBefore if it should be moved before or after the <b>source</b> parameter
	 */
	public void moveAttributePosition(CmAttribute source, CmAttribute target, boolean moveBefore) {
		if (source == target || source.equals(target)) {
			return;
		}
		if (source.getNode() != null) {
			
			
			
			List<CmAttribute> attrList = source.getNode().getCmAttributes();
			attrList.remove(source);
			if (moveBefore) {
				attrList.add(source.getNode().getCmAttributes().indexOf(target), source);
			} else {
				attrList.add(source.getNode().getCmAttributes().indexOf(target) + 1, source);
			}
			
			for (int i = 0; i < attrList.size(); i ++){
				attrList.get(i).setOrder(i);
			}

			if (source.isGrouped() && !target.isGrouped()) {
				((ConfigurableModelTreeContentProvider)viewer.getContentProvider()).removeFromGroup(Collections.singletonList(source));
			}
			if (!source.isGrouped() && target.isGrouped()) {
				((ConfigurableModelTreeContentProvider)viewer.getContentProvider()).addToGroup(Collections.singletonList(source));
			}
			((ConfigurableModelTreeContentProvider)viewer.getContentProvider()).reset(source.getNode());
			viewer.refresh();
			Object groupnode = ((ConfigurableModelTreeContentProvider)viewer.getContentProvider()).findGroupNode(source.getNode());
			if (groupnode != null) viewer.setExpandedState(groupnode, true);
		}
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

		if (obj instanceof CmNode) {
			CmNode sourceNode = (CmNode) obj;
			if (target instanceof CmNode) {
				CmNode targetNode = (CmNode) target;
				CmNode p1 = sourceNode.getParent();
				CmNode p2 = targetNode.getParent();
				if ((p1 == null && p2 != null) || (p1 != null && p2 == null))
					return false;
				return (p1 == p2) || (p1.equals(p2));
			}
		} else if (obj instanceof CmAttribute) {
			CmAttribute sourceAttr = (CmAttribute) obj;
			if (target instanceof CmAttribute) {
				CmAttribute targetAttr = (CmAttribute) target;
				return sourceAttr.getNode().equals(targetAttr.getNode());
			}
		}
		return false;
	}
	
}
