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

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.dialog.composite.CmListItemLabelProvider;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttributeItem;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Rename dialog for providing aliases for configurable model list attribute
 * items
 * 
 * @author Emily
 * 
 */
public class RenameListDialog extends AbstractRenameDialog {

	public RenameListDialog(Shell parentShell, Attribute attribute,
			ConfigurableModel editModel, Session currentSession) {
		super(parentShell, attribute, editModel, currentSession);
	}

	protected Viewer createItemViewer(Composite parent) {
		Composite tableComp = new Composite(parent, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tableComp.getLayoutData()).heightHint = 300;
		
		final TableViewer tree = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		tree.setContentProvider(ArrayContentProvider.getInstance());
		tree.setLabelProvider(new CmListItemLabelProvider(currentSession, editModel));
		tree.setInput(attribute.getActiveListItems());
		
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((StructuredSelection) tree.getSelection()).getFirstElement();
				AttributeListItem currentNode = null;
				CmAttributeListItem currentCmNode = null;
				if (x instanceof AttributeListItem) {
					currentNode = (AttributeListItem) x;
					currentCmNode = getConfiguredNode(x);
				}
				RenameListDialog.this.setCurrentSelection(currentNode, currentCmNode);
			}
		});
		
		TableColumnLayout ll = new TableColumnLayout();
		ll.setColumnData(new TableColumn(tree.getTable(),SWT.NONE), new ColumnWeightData(100));
		tableComp.setLayout(ll);
		
		return tree;
	}

	@Override
	protected CmAttributeItem createNewAlaisItem(NamedItem dmItem) {
		CmAttributeListItem currentCmNode = new CmAttributeListItem();
		currentCmNode.setConfigurableModel(editModel);
		currentCmNode.setListItem((AttributeListItem) dmItem);
		currentCmNode.setIsActive(true);
		return currentCmNode;

	}

	@Override
	protected String getDialogMessage() {
		return Messages.RenameListDialog_DialogMessage;
	}
	
	private CmAttributeListItem getConfiguredNode(Object x){
		if (x instanceof AttributeListItem) {
			AttributeListItem tmp = (AttributeListItem) x;
			List<?> items = currentSession.createCriteria(CmAttributeListItem.class)
				.add(Restrictions.eq("listItem", tmp))  //$NON-NLS-1$
				.add(Restrictions.eq("configurableModel", editModel)).list(); //$NON-NLS-1$
			if (items.size() > 0) {
				return (CmAttributeListItem) items.get(0);
			}
		}
		return null;
	}
	
	@Override
	protected void enableItem(NamedItem dmNode, boolean enable){
		CmAttributeListItem item = getConfiguredNode(dmNode);
		if (item == null){
			item = (CmAttributeListItem) createNewAlaisItem(dmNode);
			setCurrentSelection(dmNode, item); //we need this in order to update cmNode from parent class
		}
		item.setIsActive(enable);
		currentSession.saveOrUpdate(item);
	}
}
