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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.MatrixNode;
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
	private ConfigurableModelTreeContentProvider provider;
	
	/**
	 * The viewer must have a ConfigurableModelTreeContentProvider contentProvider.
	 * @param viewer
	 */
	protected ConfigurableModelTreeDropListener(TreeViewer viewer) {
		super(viewer);
		this.viewer = viewer;
		this.provider = (ConfigurableModelTreeContentProvider)viewer.getContentProvider();
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
			moveAttributePosition((CmAttribute)obj, (CmAttribute)getCurrentTarget(), getCurrentLocation() == LOCATION_BEFORE);
		} else if (obj instanceof CmAttribute && getCurrentTarget() instanceof MatrixNode) {
			//MatrixNode target = (MatrixNode)getCurrentTarget();
			CmAttribute source = (CmAttribute)obj;
			
			boolean isBefore = getCurrentLocation() == LOCATION_BEFORE;
			if (!isBefore) return false;
			
			List<CmAttribute> attrList = source.getNode().getCmAttributes();
			attrList.remove(source);
			int index = 0;
			for (int i = 0; i < attrList.size(); i ++) {
				if (attrList.get(i).isGrouped()) {
					index = i;
					break;
				}
			}
			attrList.add(index, source);
			
			for (int i = 0; i < attrList.size(); i ++){
				attrList.get(i).setOrder(i);
			}

			viewer.refresh();
			return true;
		} else if (obj instanceof MatrixNode && getCurrentTarget() instanceof CmAttribute) {
			
			CmAttribute target = (CmAttribute)getCurrentTarget();
			MatrixNode matrix = (MatrixNode)obj;
			List<CmAttribute> attrList = new ArrayList<>();
			boolean isBefore = getCurrentLocation() == LOCATION_BEFORE;
			
			for(CmAttribute a : matrix.getParent().getCmAttributes()) {
				if (a == target) {
					if (!isBefore) attrList.add(a);
					for (CmAttribute x : matrix.getKids()) {
						attrList.add(x);
					}
					if (isBefore)attrList.add(a);
				}else if (!matrix.getKids().contains(a) && !attrList.contains(a)) {
					attrList.add(a);
				}
			}
			int order = 1;
			target.getNode().getCmAttributes().clear();
			for (CmAttribute a : attrList) {
				a.setOrder(order++);
				target.getNode().getCmAttributes().add(a);
			}
			viewer.refresh();
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
				provider.removeFromGroup(Collections.singletonList(source), false);
			}
			if (!source.isGrouped() && target.isGrouped()) {
				provider.addToGroup(Collections.singletonList(source), false);
			}
			viewer.refresh();
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
			CmNode targetNode = null;
			if (target instanceof MatrixNode) {
				targetNode = ((MatrixNode)target).getParent();
			}else if (target instanceof CmAttribute) {
				targetNode = ((CmAttribute)target).getNode();
			}
			
			if (!sourceAttr.getNode().equals(targetNode)) return false;
			
			boolean isBefore = getCurrentLocation() == LOCATION_BEFORE;
			
			if (provider.getParent(sourceAttr) instanceof MatrixNode) {
				//moving attribute inside a matrix node
				if (sourceAttr.getAttribute().getType() == Attribute.AttributeType.LIST) {
					//if this is the only list in the matrix node you can't move it
					MatrixNode mn = (MatrixNode) provider.getParent(sourceAttr);
					int listcnt = 0;
					for (CmAttribute a : mn.getKids()) {
						if (a.getAttribute().getType() == Attribute.AttributeType.LIST) listcnt ++;
					}
					if (listcnt == 1) return false;
				
					//target must be after matrixnode ; after list or before list
					if (provider.getParent(target) instanceof MatrixNode) {
						if (isBefore && (target instanceof CmAttribute) ) {
							CmAttribute trg = (CmAttribute)target;
							if (trg.getAttribute().getType() == Attribute.AttributeType.LIST) return true;
							return false;
						}else if (!isBefore && (target instanceof CmAttribute) ) {
							CmAttribute trg = (CmAttribute)target;
							if (trg.getAttribute().getType() == Attribute.AttributeType.LIST) return true;
							return false;
						}else if (!isBefore && (target instanceof MatrixNode) ) {
							return true;
						}
						return false;
					}
				}else {
					//if moving out of matrix make sure there is at least one other non-list node
					if (!(provider.getParent(target) instanceof MatrixNode)) {
						MatrixNode mn = (MatrixNode) provider.getParent(sourceAttr);
						int listcnt = 0;
						for (CmAttribute a : mn.getKids()) {
							if (a.getAttribute().getType() != Attribute.AttributeType.LIST) listcnt ++;
						}
						if (listcnt == 1) return false;
					}
				}
			}
			
			
			if (target instanceof CmAttribute) {
				CmAttribute targetAttr = (CmAttribute) target;
				//if target is in matrix 
				if (provider.getParent(targetAttr) instanceof MatrixNode) {
					if (sourceAttr.getAttribute().getType() != Attribute.AttributeType.LIST) {
						//if target is not list then it can only be added after list
						//cannot be before list
						if (target instanceof CmAttribute) {
							if (((CmAttribute)target).getAttribute().getType() == Attribute.AttributeType.LIST) return false;
						}
					}else {
						//have to add a list  next to another list
						if (((CmAttribute)target).getAttribute().getType() != Attribute.AttributeType.LIST) return false;
					}
					
					if (!provider.canAddToGroup(sourceAttr)) return false;
				}
				return sourceAttr.getNode().equals(targetAttr.getNode());
			}else if (target instanceof MatrixNode) {
				return true;
			}
			return false;
		} else if (obj instanceof MatrixNode) {
			MatrixNode node = (MatrixNode)obj;
			if (target instanceof CmAttribute) {
				if (((CmAttribute) target).isGrouped()) return false;
				return ((CmAttribute)target).getNode() == node.getParent();
			}
		}
		return false;
	}
	
}
