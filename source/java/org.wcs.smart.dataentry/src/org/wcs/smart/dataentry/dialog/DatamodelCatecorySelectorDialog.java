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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;

/**
 * Dialog with selector for Datamodel Category
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class DatamodelCatecorySelectorDialog  extends AbstractPropertyJHeaderDialog {

	private DataModel datamodel;
	private Category category;

	private TreeViewer dmTreeViewer;
	
	protected DatamodelCatecorySelectorDialog(DataModel datamodel) {
		super(Display.getDefault().getActiveShell(), "Datamodel Category Selector");
		this.datamodel = datamodel;
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		dmTreeViewer = new TreeViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 400;
		dmTreeViewer.getControl().setLayoutData(gd);
		dmTreeViewer.setContentProvider(new DataModelContentProvider(true, true));
		dmTreeViewer.setLabelProvider(new DataModelLabelProvider());
		dmTreeViewer.setAutoExpandLevel(3);
		dmTreeViewer.setInput(datamodel);
		dmTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				category = null;
				IStructuredSelection selection = (IStructuredSelection) dmTreeViewer.getSelection();
				Object obj = selection.getFirstElement();
				if (obj instanceof Category) {
					category = (Category) obj;
				}
				getButton(IDialogConstants.OK_ID).setEnabled(category != null);

			}
		});
		
		setTitle("Datamodel Category Selector");
		setMessage("Select Datamodel Category to add to Configurable Model");
		
		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Add", true);
		createButton(parent, IDialogConstants.CLOSE_ID,IDialogConstants.CANCEL_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CLOSE_ID).setFocus();
		
		super.setReturnCode(IDialogConstants.CLOSE_ID);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		setReturnCode(buttonId);
		close();
	}

	@Override
	protected boolean performSave() {
		return true;
	}

	public Category getCategory() {
		return category;
	}
}
