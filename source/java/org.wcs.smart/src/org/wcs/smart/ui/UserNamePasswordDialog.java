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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.internal.Messages;

/**
 * 
 * Dialog that prompts the user for their username and password information.
 * 
 * <p>Used when deleting conservation areas as an extra measure
 * against accidently deleting ca.</p>
 * 
 * @author Emily
 *
 */
public class UserNamePasswordDialog extends Dialog{

	private Text txtUsername;
	private Text txtPassword;
	
	private String username;
	private String password;
	
	private String dialogTitle;
	private String confirmationMessage;
	private String okText;
	private String cancelText;
	
	public UserNamePasswordDialog(Shell parentShell, 
			String dialogTitle, 
			String confirmationMessage,
			String okText, String cancelText) {
		super(parentShell);
	
		this.dialogTitle = dialogTitle;
		this.confirmationMessage = confirmationMessage;
		this.okText = okText;
		this.cancelText = cancelText;
	}
	
	public UserNamePasswordDialog(Shell parentShell, 
			String dialogTitle, 
			String confirmationMessage,
			String okText) {
		this(parentShell, dialogTitle, confirmationMessage, okText, IDialogConstants.CANCEL_LABEL);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		this.password = txtPassword.getText();
		this.username = txtUsername.getText();
		
		super.buttonPressed(buttonId);
	}
	
	public String getUserName(){
		return this.username;
	}
	
	public String getPassword(){
		return this.password;
	}
	
	
	/**
	 * Create contents of the dialog.
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label lbl = new Label(composite, SWT.WRAP);
		lbl.setText(confirmationMessage);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 400;
		
		lbl = new Label(composite, SWT.NONE);
		lbl.setText(""); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		Label lblUsername = new Label(composite, SWT.NONE);
		lblUsername.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUsername.setText(Messages.UserNamePasswordDialog_Username_Label);
		
		txtUsername = new Text(composite, SWT.BORDER);
		txtUsername.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblPassword = new Label(composite, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText(Messages.UserNamePasswordDialog_Password_Label);
		
		txtPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		super.getShell().setText(dialogTitle);
		return parent;
	}
	

	/**
	 * Creates a delete and cancel button
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, okText,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				cancelText, false);
	}
	
	/** 
	 * This dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}
}
