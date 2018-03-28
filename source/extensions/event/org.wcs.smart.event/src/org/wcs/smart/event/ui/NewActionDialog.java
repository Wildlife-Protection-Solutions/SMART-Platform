/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.event.ActionTypeManager;
import org.wcs.smart.event.ActionTypeManagerInternal;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.event.ui.model.IActionParameterCollector;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for creating new action or updating an existing action
 * 
 * @author Emily
 *
 */
public class NewActionDialog extends TitleAreaDialog {

	private static final String COLLECTOR_KEY = "COLLECTOR"; //$NON-NLS-1$
	
	private ComboViewer cmbActionType;
	private EAction toUpdate;
	private Composite parameterPanel;
	private Text txtId;
	
	public NewActionDialog(Shell parentShell) {
		this(parentShell, null);
	}

	public NewActionDialog(Shell parentShell, EAction toUpdate) {
		super(parentShell);
		this.toUpdate = toUpdate;
	}
	
	@Override
	protected void okPressed() {
		if (toUpdate == null) {
			toUpdate = new EAction();
			toUpdate.setConservationArea(SmartDB.getCurrentConservationArea());
			toUpdate.setParameters(new ArrayList<>());
		}
		
		toUpdate.setId(txtId.getText());
		Object x = cmbActionType.getStructuredSelection().getFirstElement();
		if (!(x instanceof IActionType))  {
			MessageDialog.openError(getParentShell(), Messages.NewActionDialog_ActionTypeRequiredTitle, Messages.NewActionDialog_ActionTypeRequiredMsg);
			return;
		}
		toUpdate.setActionTypeKey( ((IActionType)x).getKey() );
		IActionParameterCollector paramCollector = (IActionParameterCollector) parameterPanel.getData(COLLECTOR_KEY); 
		if (paramCollector != null) {
			paramCollector.updateParameters(toUpdate);
		}else {
			toUpdate.getParameters().clear();
		}
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(toUpdate);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				EventPlugIn.log(ex.getMessage(), ex);
				return;
			}
		}
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button saveBtn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		saveBtn.setEnabled(false);
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));
		
		Composite header = new Composite(main, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(header, SWT.NONE);
		l.setText(Messages.NewActionDialog_IDLabel);
		
		txtId = new Text(header, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtId.getLayoutData()).widthHint = 100;
		txtId.setTextLimit(EAction.MAX_ID_LENGTH);
		txtId.addModifyListener(e->validate());
		
		l = new Label(header, SWT.NONE);
		l.setText(Messages.NewActionDialog_TypeLabel);
		
		cmbActionType = new ComboViewer(header, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbActionType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbActionType.setContentProvider(ArrayContentProvider.getInstance());
		cmbActionType.setLabelProvider(new AssetTypeLabelProvider());
		List<IActionType> types = ActionTypeManager.INSTANCE.getActionTypes();
		cmbActionType.setInput(types);
		
		Group g = new Group(main, SWT.NONE);
		g.setText(Messages.NewActionDialog_ParameterLabel);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		parameterPanel = new Composite(g, SWT.NONE);
		parameterPanel.setLayout(new GridLayout());
		parameterPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)parameterPanel.getLayoutData()).heightHint = 300;
		
		if (toUpdate != null) {
			cmbActionType.getControl().setEnabled(false);
			for (IActionType t : types) {
				if (t.getKey().equalsIgnoreCase(toUpdate.getActionTypeKey())) {
					cmbActionType.setSelection(new StructuredSelection(t));
					break;
				}
			}
			changeActionPanelType();
			txtId.setText(toUpdate.getId());
		}else {
			cmbActionType.addSelectionChangedListener(e->changeActionPanelType());
		}
		
		setTitle(Messages.NewActionDialog_Title);
		getShell().setText(Messages.NewActionDialog_Title);
		setMessage(Messages.NewActionDialog_Message);
		return parent;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private void validate() {
		String id = txtId.getText().trim();
		if (id.isEmpty()) {
			setError(Messages.NewActionDialog_IdRequired);
			return;
		}
		Object x = cmbActionType.getStructuredSelection().getFirstElement();
		if (!(x instanceof IActionType)) {
			setError(Messages.NewActionDialog_ActionRequired);
			return;
		}
		
		IActionParameterCollector paramCollector = (IActionParameterCollector) parameterPanel.getData(COLLECTOR_KEY); 
		if (paramCollector != null) {
			String error = paramCollector.validate();
			if (error != null) {
				setError(error);
				return;
			}
		}
		setErrorMessage(null);
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) btn.setEnabled(true);
	}
	
	private void setError(String error) {
		setErrorMessage(error);
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) btn.setEnabled(false);
	}
	
	
	private void changeActionPanelType() {
		for (Control c : parameterPanel.getChildren()) c.dispose();
		
		Object x = cmbActionType.getStructuredSelection().getFirstElement();
		if (!(x instanceof IActionType)) {
			Label l = new Label(parameterPanel, SWT.NONE);
			l.setText(Messages.NewActionDialog_ValidActionRequired);
			parameterPanel.setData(COLLECTOR_KEY, null); 
		}else {
			IActionParameterCollector paramCollector = ActionTypeManagerInternal.INSTANCE.createParameterCollector((IActionType)x);
			if (paramCollector == null) {
				parameterPanel.setData(COLLECTOR_KEY, null); 
				Label l = new Label(parameterPanel, SWT.NONE);
				l.setText(Messages.NewActionDialog_NoParameters);
			}else {
				paramCollector.addModifyListener(e->validate());
				Composite inner = paramCollector.createComposite(parameterPanel);
				inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				parameterPanel.setData(COLLECTOR_KEY, paramCollector); 
				if (toUpdate != null) paramCollector.initParameters(toUpdate);
			}
		}
		parameterPanel.layout();
		validate();
	}
}
