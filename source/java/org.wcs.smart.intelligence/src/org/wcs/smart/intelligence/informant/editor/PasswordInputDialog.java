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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.ui.UserNamePasswordDialog;

/**
 * Dialog to input user password.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class PasswordInputDialog extends Dialog {

	private Text txtPassword;
	private char[] password;

	
	protected PasswordInputDialog(Shell parentShell) {
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
		lblPassword.setText(Messages.PasswordInputDialog_Password);
		
		txtPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)txtPassword.getLayoutData()).widthHint = 200;
		
		Link lnkForgotPwd = new Link(composite, SWT.NONE);
		lnkForgotPwd.setText("<a>" + Messages.PasswordInputDialog_forgotpasslink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkForgotPwd.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false,2,1));
		lnkForgotPwd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (handleForgotPassword()){
					password = null;
					close();
				}
			}
		});

		super.getShell().setText(Messages.PasswordInputDialog_Title);
		return parent;
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
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
	
	private boolean handleForgotPassword(){
		if (MessageDialog.openQuestion(getShell(), Messages.InformantDataEditor_ResetDialog_Title,
				Messages.InformantDataEditor_ResetDialog_Message)) {
			//user need to enter login/password to remove secure data
			UserNamePasswordDialog dialog = new UserNamePasswordDialog(Display.getCurrent().getActiveShell(),
					Messages.InformantDataEditor_UserNameConfirmation_Title,
					Messages.InformantDataEditor_UserNameConfirmation_Message,
					Messages.InformantDataEditor_UserNameConfirmation_Button);
			if (dialog.open() == Window.CANCEL) {
				return false;
			}
			
			if (!(dialog.getUserName().equalsIgnoreCase(SmartDB.getCurrentEmployee().getSmartUserId())
				&& dialog.getPassword().equals(SmartDB.getCurrentEmployee().getSmartPassword())	)) {
				
				MessageDialog.openError(Display.getCurrent().getActiveShell(), 
						Messages.InformantDataEditor_ConfirmationError_Title, 
						Messages.InformantDataEditor_ConfirmationError_Message);
				return false;
			}
			
			IntelligenceHibernateManager.clearInformantsEncrptedData();
			return true;
		}
		return false;
	}
	
}
