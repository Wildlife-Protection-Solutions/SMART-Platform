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
import java.util.UUID;

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
	
	
	private static ConnectAlert.Level DEFAULT_IMPORTANCE_LEVEL = ConnectAlert.Level.THREE;
	
	private Label sourceObj;
	private ComboViewer typeViewer;
	private ComboViewer importanceViewer;
	
	private ConnectAlert alert;
	private List<ConnectAlertType> alertTypes;
	private boolean isNew;
	
	private ConnectAlertSourceLabelProvider sourceLabelProvider;

	protected AlertEditDialog(Shell parent, boolean isNew, ConnectAlert alert, List<ConnectAlertType> alertTypes) {
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
		lblSource.setLayoutData(new GridData(SWT.TOP, SWT.FILL, true, false));
		
		sourceObj = new Label(main, SWT.WRAP);
		String srcText = sourceLabelProvider.getText(alert);
		//need to replace all '&' with '&&' so it won't be treated as mnemonic indication
		sourceObj.setText(srcText.replaceAll("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
		sourceObj.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)sourceObj.getLayoutData()).widthHint = 450;
		Label lblType = new Label(main, SWT.NONE);
		lblType.setText(Messages.AlertEditDialog_Type);
		
        typeViewer = new ComboViewer(main, SWT.READ_ONLY);
        typeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        typeViewer.setContentProvider(ArrayContentProvider.getInstance());
        typeViewer.setLabelProvider(new LabelProvider(){
        	@Override
        	public String getText(Object element){
        		if (element instanceof ConnectAlertType){
        			return ((ConnectAlertType)element).getLabel();
        		}
        		return super.getText(element);
        	
        	}
        });

        typeViewer.setInput(alertTypes);
        if (alert.getType() == null) {
        	alert.setType(alertTypes.isEmpty() ? null : alertTypes.get(0).getUuid());
        }
        UUID selection = alert.getType();
        for (ConnectAlertType t : alertTypes){
        	if (t.getUuid().equals(selection)){
        		typeViewer.setSelection(new StructuredSelection(t));
        		break;
        	}
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
		importanceViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof ConnectAlert.Level){
					return String.valueOf(((ConnectAlert.Level) element).value);
				}
				return super.getText(element);
			}
		});
 
		importanceViewer.setInput(ConnectAlert.Level.values());
		ConnectAlert.Level initValue = null;
        if (alert.getLevel() == null) {
        	initValue = DEFAULT_IMPORTANCE_LEVEL;
        }else{
        	for (ConnectAlert.Level l : ConnectAlert.Level.values()){
        		if (alert.getLevel() == l.value){
        			initValue = l;
        			break;
        		}
        	}
        }
        importanceViewer.setSelection(new StructuredSelection(initValue));

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
		getButton(IDialogConstants.CLOSE_ID).setText(IDialogConstants.CANCEL_LABEL);
		getButton(IDialogConstants.OK_ID).setText(IDialogConstants.OK_LABEL);
		getButton(IDialogConstants.OK_ID).setEnabled(this.isNew); //this will enable "Save" button when new alert is created
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (performSave()) {
				super.setReturnCode(IDialogConstants.OK_ID);
				close();
			}
		} else if (IDialogConstants.CLOSE_ID == buttonId) {
			//we don't care in this case
			changesMade = false;
			close();
		}
	}
	
	@Override
	protected boolean performSave() {
		IStructuredSelection selType = (IStructuredSelection) typeViewer.getSelection();
		if (selType != null && !selType.isEmpty()) {
			alert.setType( ((ConnectAlertType)selType.getFirstElement()).getUuid() );
		}

		IStructuredSelection selLvl = (IStructuredSelection) importanceViewer.getSelection();
		if (selLvl != null && !selLvl.isEmpty()) {
			alert.setLevel( ((ConnectAlert.Level) selLvl.getFirstElement()).value);
		}
		
		setChangesMade(false);
		return true;
	}

}
