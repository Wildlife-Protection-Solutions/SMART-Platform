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
package org.wcs.smart.intelligence.informant.editor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * Dialog to set user password.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class SetPasswordDialog extends TitleAreaDialog {
	
	private static final String PASSWORD_REGEXP = "^.*(?=.{8,})(?=..*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=]).*$"; //$NON-NLS-1$

	private Text txtPassword;
	private Text txtRePassword;
	private char[] password;
	
	protected SetPasswordDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		this.password = txtPassword.getTextChars();
		super.buttonPressed(buttonId);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label lblPassword = new Label(composite, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblPassword.setText(Messages.SetPasswordDialog_NewPassword);
		
		txtPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtPassword.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				inputChanged();
			}
		});

		Label lblRePassword = new Label(composite, SWT.NONE);
		lblRePassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblRePassword.setText(Messages.SetPasswordDialog_ConfirmPassword);
		
		txtRePassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		txtRePassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtRePassword.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				inputChanged();
			}
		});
		
		Label lblPolicy = new Label(composite, SWT.NONE);
		lblPolicy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		lblPolicy.setText(Messages.SetPasswordDialog_Policy);
		
		super.getShell().setText(Messages.SetPasswordDialog_Title);
		super.setTitle(Messages.SetPasswordDialog_Title);
		super.setMessage(Messages.SetPasswordDialog_Message);
		return parent;
	}

	
	protected void inputChanged() {
		String error = validatePassword(txtPassword.getText(), txtRePassword.getText());
		getButton(IDialogConstants.OK_ID).setEnabled(error == null);
		setErrorMessage(error);
	}

	private String validatePassword(String pwd, String re) {
		if (pwd.equals(re)) {
			if (!pwd.matches(PASSWORD_REGEXP)) {
				return Messages.SetPasswordDialog_PolicyUnmatched;
			} else {
				return null;
			}
		}
		return Messages.SetPasswordDialog_ConfirmationUnmatched;
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true).setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	/** 
	 * This dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	public char[] getPassword() {
		return password;
	}
}
