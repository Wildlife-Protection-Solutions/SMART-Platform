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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Dialog for creating new configurable model.
 * 
 * @author Emily, Evgeniy
 *
 */
public class CreateNewOpDialog extends TitleAreaDialog {
	
	private enum CreateCmOption {
		BLANK,
		CM,
		DM
	}

	private CreateCmOption option = CreateCmOption.BLANK;
	private Button opBlank, opCm, opDm;
	private ComboViewer cbCm;
	private Session currentSession;
	private ConfigurableModel initModel;
	private String name = null;
	private ConfigurableModel cmTemplate = null;
	private Text txtName;
	
	protected CreateNewOpDialog(Shell parentShell, Session currentSession) {
		super(parentShell);
		this.currentSession = currentSession;
	}

	protected void okPressed() {
		name = txtName.getText();
		IStructuredSelection selection = (IStructuredSelection)cbCm.getSelection();
		cmTemplate = !selection.isEmpty() ? (ConfigurableModel) selection.getFirstElement() : null;
		String error = validate();
		setErrorMessage(error);
		if (error == null) {
			super.okPressed();
		}
	}
	
	private String validate() {
		if (CreateCmOption.CM.equals(option) && cbCm.getSelection().isEmpty()) {
			return Messages.CreateNewOpDialog_NoConfigurableModelSelected_Error;
		}
		return null;
	}

	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(3, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 20;
		Label lblName = new Label(panel, SWT.NONE);
		lblName.setText(Messages.CreateNewOpDialog_NameLabel);
		
		txtName = new Text(panel, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		txtName.setText(Messages.ConfigurableModelPropertyDialog_ConfigurableModelDeafultName);
		
		Label lblOp = new Label(panel, SWT.NONE);
		lblOp.setText(Messages.CreateNewOpDialog_TemplateLabel);
		
		opBlank = new Button(panel, SWT.RADIO);
		opBlank.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
		opBlank.setSelection(true);
		opBlank.setText(Messages.CreateNewOpDialog_BlankCmOp);
		opBlank.setToolTipText(Messages.CreateNewOpDialog_BlankCmToolTip);
		opBlank.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged(CreateCmOption.BLANK);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		new Label(panel, SWT.NONE);

		opCm = new Button(panel, SWT.RADIO);
		opCm.setText(Messages.CreateNewOpDialog_CmTemplateOp);
		opCm.setToolTipText(Messages.CreateNewOpDialog_CmTemplateToolTip);
		opCm.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged(CreateCmOption.CM);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		cbCm = new ComboViewer(panel, SWT.READ_ONLY);
		cbCm.getControl().setEnabled(false);
		cbCm.getControl().setToolTipText(Messages.CreateNewOpDialog_CmTemplateToolTip);
		cbCm.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cbCm.setContentProvider(ArrayContentProvider.getInstance());
		cbCm.setLabelProvider(new ConfigurableModelLabelProvider());
 		cbCm.setInput(getCmList());
		cbCm.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				//changesMade();
			}
		});
		
		new Label(panel, SWT.NONE);
		
		opDm = new Button(panel, SWT.RADIO);
		opDm.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
		opDm.setText(Messages.CreateNewOpDialog_DmTemplateOp);
		opDm.setToolTipText(Messages.CreateNewOpDialog_DmTemplateToolTip);
		opDm.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged(CreateCmOption.DM);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		getShell().setText(Messages.CreateNewOpDialog_DialogTitle);
		setTitle(Messages.CreateNewOpDialog_DialogTitle);
		setMessage(Messages.CreateNewOpDialog_DialogMessage);
		
		return parent;
	}

	private List<ConfigurableModel> getCmList() {
		List<ConfigurableModel> modelList = new ArrayList<ConfigurableModel>();
		try {
			modelList = DataentryHibernateManager.getConfigurableModels(currentSession);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		}
		return modelList;
	}
	
	protected void optionChanged(CreateCmOption op) {
		option = op;
		cbCm.getControl().setEnabled(CreateCmOption.CM.equals(option));
	}
	
	public ConfigurableModel getDefaultConfigurableModel() throws Exception {
		switch (option) {
		case BLANK:
			return ConfigurableModelFactory.createBlankModel(name);
		case CM:
		{
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					initModel = ConfigurableModelFactory.createConfigurableModelClone(cmTemplate, name, monitor);
					
				}
			});
			return initModel;
		}
		case DM:
		{
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					initModel = ConfigurableModelFactory.createModelFromDataModel(name, currentSession, monitor);
					
				}
			});
			return initModel;
		}
		}
		//this line should never be reached
		throw new IllegalStateException("Unknown template option for creaing a configurable model: " + option); //$NON-NLS-1$
	}
	
}
