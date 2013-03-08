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
package org.wcs.smart.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Dialog for changing the current conservation areas used
 * in the cross ca analysis
 * @author Emily
 *
 */
public class SelectCaDialog extends TitleAreaDialog {

	private static final String ERROR_DIALOG_TITLE = Messages.SelectCaDialog_ErrorDialogtitle;

	private static final String CA_SELECT_MULTI_ERROR = Messages.SelectCaDialog_CaError;

	private CheckboxTableViewer caList; 
	
	private ConservationAreaConfiguration newConfiguration;
	
	public SelectCaDialog(Shell parentShell) {
		super(parentShell);
	}

	public ConservationAreaConfiguration getNewConfiguration(){
		return this.newConfiguration;
	}
	
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId){
			List<ConservationArea> cas = new ArrayList<ConservationArea>();
			Object[] selections = caList.getCheckedElements();
			if (selections.length <= 1){
				MessageDialog.openInformation(getShell(), ERROR_DIALOG_TITLE, CA_SELECT_MULTI_ERROR);
				return;
			}
			for (Object x : selections){
				ConservationArea ca = (ConservationArea)x;
				cas.add(ca);
			}
			//employee list remains the same as you want to 
			//access all queries/reports saved by any user
			newConfiguration = new ConservationAreaConfiguration(cas, SmartDB.getConservationAreaConfiguration().getEmployees());
		}
		super.buttonPressed(buttonId);
		
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);
		
		Composite comp = new Composite(main, SWT.NONE);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(comp, SWT.NONE);
		l.setText(Messages.SelectCaDialog_CaLabel);
		
		caList = CheckboxTableViewer.newCheckList(comp, SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		caList.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof ConservationArea){
					return ((ConservationArea)element).getNameLabel();
				}
				return super.getText(element);
				
			}
		});
		caList.setContentProvider(ArrayContentProvider.getInstance());
		caList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)caList.getControl().getLayoutData()).heightHint = 100;
		caList.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				String error = null;
				
				if (caList.getCheckedElements().length < 2){
					error = CA_SELECT_MULTI_ERROR;
				}
				setErrorMessage(error);
				getButton(IDialogConstants.OK_ID).setEnabled(error == null);
			}
		});
		
		
		setMessage(Messages.SelectCaDialog_SelectLabel);
		setTitle(Messages.SelectCaDialog_DialogTitle);
		getShell().setText(Messages.SelectCaDialog_ShellTitle);
		
		
		try {
			List<ConservationArea> cas = HibernateManager.findConservationAreas(SmartDB.getCurrentEmployee().getSmartUserId(), SmartDB.getCurrentEmployee().getSmartPassword());
			caList.setInput(cas);
		} catch (Exception e) {
			SmartPlugIn.log(e.getMessage(), e);
		}
		
		caList.setAllChecked(false);
		for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
			caList.setChecked(ca, true);
		}
		
		
		return comp;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
