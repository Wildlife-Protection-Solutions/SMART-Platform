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
package org.wcs.smart.connect.cybertracker.dataentry;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog for creating/editing {@link ConnectAlert} items.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class AlertEditDialog extends AbstractPropertyJHeaderDialog {
	
	private static Integer[] IMPORTANCE_TYPES = {1, 2, 3, 4, 5};
	private static int DEFAULT_IMPORTANCE_LEVEL = 3;
	
	private Label sourceObj;
	private ComboViewer typeViewer;
	private ComboViewer importanceViewer;
	
	private ConnectAlert alert;
	private List<String> alertTypes;
	private boolean isNew;
	
	private ConnectAlertSourceLabelProvider sourceLabelProvider;

	protected AlertEditDialog(Shell parent, boolean isNew, ConnectAlert alert, List<String> alertTypes) {
		super(parent, isNew ? Messages.AlertEditDialog_NewAlertTitle : Messages.AlertEditDialog_EditAlertTitle);
		this.alert = alert;
		this.alertTypes = alertTypes;
		this.isNew = isNew;
		this.sourceLabelProvider = new ConnectAlertSourceLabelProvider(alert.getModel());
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblSource = new Label(main, SWT.NONE);
		lblSource.setText(Messages.AlertEditDialog_Source);
		
		sourceObj = new Label(main, SWT.NONE);
		String srcText = sourceLabelProvider.getText(alert);
		//need to replace all '&' with '&&' so it won't be treated as mnemonic indication
		sourceObj.setText(srcText.replaceAll("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$

		Label lblType = new Label(main, SWT.NONE);
		lblType.setText(Messages.AlertEditDialog_Type);
		
        typeViewer = new ComboViewer(main, SWT.READ_ONLY);
        typeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        typeViewer.setContentProvider(ArrayContentProvider.getInstance());
        typeViewer.setLabelProvider(new LabelProvider());

        typeViewer.setInput(alertTypes);
        if (alert.getType() == null) {
        	alert.setType(alertTypes.isEmpty() ? "" : alertTypes.get(0)); //$NON-NLS-1$
        }
        if (alertTypes.contains(alert.getType())) {
            typeViewer.setSelection(new StructuredSelection(alert.getType()));
        } else {
        	//TODO: what to do?
        }
 
        typeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				handleChangesMade();
			}
		});

		Label lblImportance = new Label(main, SWT.NONE);
		lblImportance.setText(Messages.AlertEditDialog_Importance);
		
		importanceViewer = new ComboViewer(main, SWT.READ_ONLY);
		importanceViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		importanceViewer.setContentProvider(ArrayContentProvider.getInstance());
		importanceViewer.setLabelProvider(new LabelProvider());
 
		importanceViewer.setInput(IMPORTANCE_TYPES);
        if (alert.getLevel() == null) {
        	alert.setLevel(DEFAULT_IMPORTANCE_LEVEL);
        }
        importanceViewer.setSelection(new StructuredSelection(alert.getLevel()));

        importanceViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				handleChangesMade();
			}
		});
        
        setChangesMade(isNew);
        setTitle(isNew ? Messages.AlertEditDialog_NewAlertTitle : Messages.AlertEditDialog_EditAlertTitle);
        setMessage(isNew ? Messages.AlertEditDialog_NewAlertMessage : Messages.AlertEditDialog_EditAlertMessage);
		return main;
	}

	private void handleChangesMade() {
		setChangesMade(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText(IDialogConstants.OK_LABEL);
		getButton(IDialogConstants.OK_ID).setEnabled(this.isNew); //this will enable "Save" button when new alert is created
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (performSave()) {
				super.setReturnCode(IDialogConstants.OK_ID);
				if (isNew) {
					close();
				}
			}
		} else if (IDialogConstants.CLOSE_ID == buttonId) {
			close();
		}
	}
	
	@Override
	protected boolean performSave() {
		IStructuredSelection selType = (IStructuredSelection) typeViewer.getSelection();
		if (selType != null && !selType.isEmpty()) {
			alert.setType((String) selType.getFirstElement());
		}

		IStructuredSelection selLvl = (IStructuredSelection) importanceViewer.getSelection();
		if (selLvl != null && !selLvl.isEmpty()) {
			alert.setLevel((Integer) selLvl.getFirstElement());
		}
		
		setChangesMade(false);
		return true;
	}

}
