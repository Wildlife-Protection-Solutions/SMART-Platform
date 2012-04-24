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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Class that creates an attribute tree viewer for
 * a given attribute.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTree {

	private TreeViewer viewer = null;
	private AttributeTreeChangeListener listener = null;
	
	
	/**
	 * Sets the listener fired when modifications occur.  
	 * @param listener
	 */
	public void setListener(AttributeTreeChangeListener listener){
		this.listener = listener;
	}
	
	private void fireChangeListener(){
		if (listener != null){
			listener.treeModified();
		}
	}
	/**
	 * Sets the attribute input to the attribute tree
	 * 
	 * @param attribute
	 */
	public void setInput(Attribute attribute){
		viewer.setInput(attribute);
		refreshTree(viewer);
	}

	
	/**
	 * 
	 * @return attribute associated with attribute tree
	 */
	public Attribute getAttribute(){
		return (Attribute)viewer.getInput();
	}
	
	/**
	 * 
	 * @param parent
	 * @param currentLanguage
	 * 
	 * @return composite that describes the attribute tree
	 */
	public Composite createTree(Composite parent, final Language currentLanguage){
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		comp.setLayout(new GridLayout(2, false));
		
		PatternFilter patternFilter = new PatternFilter(){			
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((AttributeTreeContentProvider)((TreeViewer)viewer).getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((AttributeTreeLabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
			}
			
		};
		FilteredTree fTree = new FilteredTree(comp, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL, patternFilter, true);
		
		//viewer = new TreeViewer(comp, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer = fTree.getViewer();
		viewer.setContentProvider(new AttributeTreeContentProvider( ));
		viewer.setLabelProvider(new AttributeTreeLabelProvider(currentLanguage));
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,true));
		((GridData)viewer.getTree().getLayoutData()).heightHint = 80;
		((GridData)viewer.getTree().getLayoutData()).widthHint = 100;
		viewer.setAutoExpandLevel(2);
		viewer.setInput(new Attribute());
		
		
		int operations = DND.DROP_MOVE;
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		viewer.addDragSupport(operations, transferTypes , new AttributeTreeDragListener(viewer));
		viewer.addDropSupport(operations, transferTypes, new AttributeTreeDropListener(viewer));
		
		Composite buttonPanel = new Composite(comp, SWT.NONE);
		buttonPanel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		buttonPanel.setLayout(new GridLayout(1, false));
		
		final Button btnAdd = new Button(buttonPanel, SWT.NONE);
		btnAdd.setText("Add");
		btnAdd.setEnabled(true);
		btnAdd.setToolTipText("Add category to attribute tree.");
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				addItem(viewer, currentLanguage);
			}
		});

		final Button btnEdit = new Button(buttonPanel, SWT.NONE);
		btnEdit.setEnabled(false);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setToolTipText("Modify selected category.");
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				editItem(viewer, currentLanguage);
			}
		});
		
		final Button btnDisable = new Button(buttonPanel, SWT.NONE);
		btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		btnDisable.setToolTipText("Disable selected category.");
		btnDisable.setEnabled(false);
		btnDisable.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				disableItem(viewer, !btnDisable.getText().equals(DialogConstants.DISABLE_BUTTON_TEXT));
				
				if (btnDisable.getText().equals(DialogConstants.DISABLE_BUTTON_TEXT)){
					btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}else{
					btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}
			}
		});
		
		Label lbl = new Label(buttonPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		final Button btnDisableAll = new Button(buttonPanel, SWT.NONE);
		btnDisableAll.setText(DialogConstants.DISABLE_BUTTON_TEXT + " All");
		btnDisableAll.setToolTipText("Disable all categories.");
		btnDisableAll.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Attribute att = (Attribute) viewer.getInput();
				for (AttributeTreeNode node : att.getTree()){
					disableNode(node, false);
				}
				viewer.refresh();
				fireChangeListener();
			}
		});
		
		
		final Button btnEnableeAll = new Button(buttonPanel, SWT.NONE);
		btnEnableeAll.setText(DialogConstants.ENABLE_BUTTON_TEXT + " All");
		btnEnableeAll.setToolTipText("Enable all categories.");
		btnEnableeAll.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Attribute att = (Attribute) viewer.getInput();
				if (att.getTree() == null) return;
				for (AttributeTreeNode node : att.getTree()){
					enableAll(node);
				}
				viewer.refresh();
				fireChangeListener();
			}
		});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
				if (x instanceof AttributeTreeContentProvider.RootNode){
					btnEdit.setEnabled(false);
					btnDisable.setEnabled(false);
				}else if (x instanceof AttributeTreeNode){
					btnEdit.setEnabled(true);
					btnDisable.setEnabled(true);
					if (((AttributeTreeNode)x).getIsActive()){
						btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					}else{
						btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					}
				}
				
			}
		});
		
		return comp;
	}
	
	/*
	 * Enables the attribute tree node and all its children
	 */
	private void enableAll(AttributeTreeNode node){
		node.setIsActive(true);
		if (node.getChildren() == null) return;
		for (AttributeTreeNode child: node.getChildren()){
			enableAll(child);
		}		
	}
	/*
	 * disable items in the tree
	 */
	private void disableItem(TreeViewer viewer, boolean enabled){
		for (Iterator iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof AttributeTreeNode){
				disableNode((AttributeTreeNode)x, enabled);	
			}	
		}
		viewer.refresh();
		fireChangeListener();
	}
	
	/*
	 * disables or enables a tree node
	 * 
	 * @param enabled true if the node is to be activated; false to de-activate
	 */
	public void disableNode(AttributeTreeNode node, boolean enabled){
		if (!enabled){
			//disable node and all children
			node.setIsActive(enabled);
			if (node.getChildren() != null){
				for(AttributeTreeNode child: node.getChildren()){
					disableNode(child, enabled);
				}
			}
		}else{
			//enable category and parent
			if (node.getIsActive()){
				return ;
			}
			node.setIsActive(enabled);
			if (node.getParent() != null){
				disableNode(node.getParent(), enabled);
			}
		}
	}
	
	/*
	 * edits an item in the tree
	 */
	private void editItem(TreeViewer viewer, Language currentLanguage){
		Object x = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		Attribute a = (Attribute)viewer.getInput();
		if (x instanceof AttributeTreeNode){
			List<? extends DmObject> siblings = null;
			if ( ((AttributeTreeNode)x).getParent() == null){
				siblings = a.getTree();
			}else{
				siblings = ((AttributeTreeNode)x).getParent().getChildren();
			}
			AttributeItemDialog dd = new AttributeItemDialog(Display.getCurrent().getActiveShell(), (AttributeTreeNode)x, siblings, currentLanguage);
			int ret = dd.open();
			if (ret == Window.CANCEL){
				return;
			}
		}
		refreshTree(viewer);
		fireChangeListener();
	}
	
	/*
	 * adds item to tree
	 */
	private void addItem(TreeViewer viewer, Language currentLanguage){
		Object x = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		Attribute a = (Attribute)viewer.getInput();
		if (x instanceof AttributeTreeContentProvider.RootNode || x instanceof AttributeTreeNode){
			List<? extends DmObject> siblings = null;
			AttributeTreeNode parent = null;
			if (x instanceof AttributeTreeContentProvider.RootNode){
				siblings = a.getTree();
			}else{
				siblings = ((AttributeTreeNode)x).getChildren();
				parent = ((AttributeTreeNode)x);
			}
			
			AttributeTreeNode it = new AttributeTreeNode();
			it.setParent(parent);
			it.setAttribute(a);
			
			AttributeItemDialog dd = new AttributeItemDialog(Display.getCurrent().getActiveShell(), it, siblings, currentLanguage);
			int ret = dd.open();
			if (ret == Window.CANCEL){
				return;
			}
			
			
			if (parent != null){
				if (parent.getChildren() == null){
					parent.setChildren(new ArrayList<AttributeTreeNode>());
				}
				parent.getChildren().add(it);
			}else{
				if (a.getTree() == null){
					a.setTree(new ArrayList<AttributeTreeNode>());
				}
				a.getTree().add(it);
			}
			viewer.setExpandedState(x, true);
			
			it.setIsActive(true);
			if (siblings == null){
				it.setNodeOrder(0);
			}else{
				it.setNodeOrder(siblings.size());
			}
		
			refreshTree(viewer);
			fireChangeListener();
		}
	}
	
	/*
	 * refresh the tree
	 */
	private void refreshTree(TreeViewer viewer) {
		viewer.refresh();
	}
	
	
}

interface AttributeTreeChangeListener{
	public void treeModified();
}

/**
 * Drag listener for attribute tree
 * 
 * @author Emily
 * 
 */
class AttributeTreeDragListener implements DragSourceListener {

	private TreeViewer viewer;

	public AttributeTreeDragListener(TreeViewer viewer) {
		this.viewer = viewer;
	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
		LocalSelectionTransfer.getTransfer()
				.setSelection(viewer.getSelection());
		event.doit = true;

	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		if (LocalSelectionTransfer.getTransfer()
				.isSupportedType(event.dataType)) {
			event.data = viewer.getSelection();
		}

	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragFinished(DragSourceEvent event) {
		LocalSelectionTransfer.getTransfer().setSelection(null);
		viewer.refresh();
	}

}

/**
 * Drop listener for attribute tree
 * 
 * @author Emily
 * 
 */
class AttributeTreeDropListener extends ViewerDropAdapter {

	private TreeViewer viewer;

	/**
	 * @param viewer
	 */
	protected AttributeTreeDropListener(TreeViewer viewer) {
		super(viewer);
		this.viewer = viewer;

	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@Override
	public boolean performDrop(Object data) {

		StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer
				.getTransfer().getSelection();
		if (selection == null) {
			return false;
		}
		Object obj = selection.getFirstElement();
		int loc = getCurrentLocation();
		if (obj instanceof AttributeTreeNode
				&& getCurrentTarget() instanceof AttributeTreeNode) {
			((Attribute) viewer.getInput()).moveAttributeTreeNode(
					(AttributeTreeNode) obj,
					(AttributeTreeNode) getCurrentTarget(),
					loc == LOCATION_BEFORE);
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object,
	 *      int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {

		StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer
				.getTransfer().getSelection();
		if (selection == null) {
			return false;
		}
		Object obj = selection.getFirstElement();

		if (obj instanceof AttributeTreeNode) {
			AttributeTreeNode toMove = (AttributeTreeNode) obj;

			if (target instanceof AttributeTreeNode) {
				AttributeTreeNode toMoveTo = (AttributeTreeNode) target;
				if ((toMoveTo.getParent() == null && toMove.getParent() == null)
						|| (toMoveTo.getParent() != null
								&& toMove.getParent() != null && toMoveTo
								.getParent().equals(toMove.getParent()))) {
					return true;
				}
			}
		}
		return false;
	}

}
