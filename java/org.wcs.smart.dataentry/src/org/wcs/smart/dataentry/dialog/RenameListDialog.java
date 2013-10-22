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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.dialog.composite.CmListItemLabelProvider;
import org.wcs.smart.dataentry.internal.Messages;
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
		final ListViewer tree = new ListViewer(parent);
		tree.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setContentProvider(ArrayContentProvider.getInstance());
		tree.setLabelProvider(new CmListItemLabelProvider(currentSession));
		tree.setInput(attribute.getActiveListItems());

		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((StructuredSelection) tree.getSelection())
						.getFirstElement();
				AttributeListItem currentNode = null;
				CmAttributeListItem currentCmNode = null;
				if (x instanceof AttributeListItem) {
					currentNode = (AttributeListItem) x;
					@SuppressWarnings("rawtypes")
					List items = currentSession
							.createCriteria(CmAttributeListItem.class)
							.add(Restrictions.eq("listItem", currentNode)).list(); //$NON-NLS-1$
					if (items.size() > 0) {
						currentCmNode = (CmAttributeListItem) items.get(0);
					}
				}
				RenameListDialog.this.setCurrentSelection(currentNode,
						currentCmNode);
			}
		});
		return tree;
	}

	@Override
	protected NamedItem createNewAlaisItem() {
		CmAttributeListItem currentCmNode = new CmAttributeListItem();
		currentCmNode.setConfigurableModel(editModel);
		currentCmNode
				.setListItem((AttributeListItem) getCurrentDataModelSelection());
		return currentCmNode;

	}

	@Override
	protected String getDialogMessage() {
		return Messages.RenameListDialog_DialogMessage;
	}
}
