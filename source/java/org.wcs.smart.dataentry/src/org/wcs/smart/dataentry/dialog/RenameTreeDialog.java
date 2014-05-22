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
import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.dialog.composite.CmTreeLabelProvider;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttributeItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;

/**
 * Rename dialog for providing aliases for configurable model tree attribute nodes 
 * @author Emily
 *
 */
public class RenameTreeDialog extends AbstractRenameDialog{

	public RenameTreeDialog(Shell parentShell, Attribute attribute, ConfigurableModel editModel, Session currentSession) {
		super(parentShell, attribute, editModel, currentSession);
		
		
	}

	protected Viewer createItemViewer(Composite parent) {
		final TreeViewer tree = new TreeViewer(parent, SWT.MULTI | SWT.BORDER);
		tree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tree.getTree().getLayoutData()).heightHint = 300;
		
		tree.setContentProvider(new AttributeTreeContentProvider(true, false));
		tree.setLabelProvider(new CmTreeLabelProvider(currentSession, editModel));
		tree.setInput(attribute);
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((StructuredSelection)tree.getSelection()).getFirstElement();
				AttributeTreeNode currentNode = null;
				CmAttributeTreeNode currentCmNode = null;
				if (x instanceof AttributeTreeNode){
					currentNode = (AttributeTreeNode) x;
					currentCmNode = getConfiguredNode(x);
				}
				RenameTreeDialog.this.setCurrentSelection(currentNode, currentCmNode);
			}
		});
		tree.expandToLevel(2);
		
		return tree;
	}
	
	private CmAttributeTreeNode getConfiguredNode(Object x){
		if (x instanceof AttributeTreeNode) {
			AttributeTreeNode tmp = (AttributeTreeNode) x;
			List<?> items = currentSession.createCriteria(CmAttributeTreeNode.class)
				.add(Restrictions.eq("dmTreeNode", tmp)) //$NON-NLS-1$
				.add(Restrictions.eq("configurableModel", editModel)).list(); //$NON-NLS-1$
			if (items.size() > 0){
				return  (CmAttributeTreeNode) items.get(0);
			}
		}
		return null;
	}
	

	@Override
	protected CmAttributeItem createNewAlaisItem(NamedItem dmItem) {
		CmAttributeTreeNode currentCmNode = new CmAttributeTreeNode();
		currentCmNode.setConfigurableModel(editModel);
		currentCmNode.setDmTreeNode((AttributeTreeNode) dmItem);
		currentCmNode.setIsActive(true);
		return currentCmNode;
		
	}

	@Override
	protected String getDialogMessage() {
		return Messages.RenameTreeDialog_DialogMessage;
	}
	
	private void processItem(NamedItem dmNode, boolean enable, boolean updateSelection){
		CmAttributeTreeNode item = getConfiguredNode(dmNode);
		if (item == null){
			item = (CmAttributeTreeNode) createNewAlaisItem(dmNode);
			if (updateSelection) {
				setCurrentSelection(dmNode, item);
			}
		}
		item.setIsActive(enable);
		currentSession.saveOrUpdate(item);
	}
	
	@Override
	protected void enableItem(NamedItem dmNode, boolean enable){
		processItem(dmNode, enable, true);
		
		if (!enable){
			//process all children
			AttributeTreeNode node = (AttributeTreeNode) dmNode;
			ArrayList<AttributeTreeNode> itemsToProcess = new ArrayList<AttributeTreeNode>();
			itemsToProcess.addAll(node.getActiveChildren());
			while(itemsToProcess.size() > 0){
				AttributeTreeNode kid = itemsToProcess.remove(0);
				processItem(kid, enable, false);
				itemsToProcess.addAll(kid.getActiveChildren());
			}
		
		}else{
			//need to enable all parents
			AttributeTreeNode parent = ((AttributeTreeNode)dmNode).getParent();
			while(parent != null){
				processItem(parent, enable, false);
				parent = parent.getParent();
			}
		}
	}
}
