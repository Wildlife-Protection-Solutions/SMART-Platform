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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Dialog for creating new configurable model.
 * 
 * @author Emily
 *
 */
public class CreateNewOpDialog extends TitleAreaDialog {

	private boolean blank = false;
	private Button opBlank, opDm;
	private Session currentSession;
	private ConfigurableModel initModel;
	private String name = null;
	private Text txtName;
	
	protected CreateNewOpDialog(Shell parentShell, Session currentSession) {
		super(parentShell);
		this.currentSession = currentSession;
	}

	protected void okPressed() {
		blank = opBlank.getSelection();
		name = txtName.getText();
		super.okPressed();
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 20;
		Label lblName = new Label(panel, SWT.NONE);
		lblName.setText(Messages.CreateNewOpDialog_NameLabel);
		
		txtName = new Text(panel, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.setText(Messages.ConfigurableModelPropertyDialog_ConfigurableModelDeafultName);
		
		Label lblOp = new Label(panel, SWT.NONE);
		lblOp.setText(Messages.CreateNewOpDialog_TemplateLabel);
		
		opBlank = new Button(panel, SWT.RADIO);
		opBlank.setText(Messages.CreateNewOpDialog_BlankCmOp);
		opBlank.setToolTipText(Messages.CreateNewOpDialog_BlankCmToolTip);
		
		new Label(panel, SWT.NONE);
		
		opDm = new Button(panel, SWT.RADIO);
		opDm.setText(Messages.CreateNewOpDialog_DmTemplateOp);
		opDm.setToolTipText(Messages.CreateNewOpDialog_DmTemplateToolTip);
		
		getShell().setText(Messages.CreateNewOpDialog_DialogTitle);
		setTitle(Messages.CreateNewOpDialog_DialogTitle);
		setMessage(Messages.CreateNewOpDialog_DialogMessage);
		
		return parent;
	}
	
	public ConfigurableModel getDefaultConfigurableModel() throws Exception{
		if (blank){
			return ConfigurableModelFactory.createBlankModel(name);
		}else{
			//TODO: this should probably be run within a progress dialog
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
	
}
