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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.intelligence.informant.aes.InformantAesManager;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * Dialog used to change password.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class ChangePasswordDialog extends SetPasswordDialog {
	
	private Text txtOldPassword;

	protected ChangePasswordDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite result = (Composite)super.createDialogArea(parent);

		super.getShell().setText(Messages.ChangePasswordDialog_Title);
		super.setTitle(Messages.ChangePasswordDialog_Title);
		super.setMessage(Messages.ChangePasswordDialog_Message);
		return result;
	}
	
	@Override
	protected void createInnerContent(Composite composite) {
		Label lblPassword = new Label(composite, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblPassword.setText(Messages.ChangePasswordDialog_OldPassword);
		
		txtOldPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		txtOldPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtOldPassword.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				inputChanged();
			}
		});

		super.createInnerContent(composite);
	}
	
	@Override
	protected String validate() {
		String error = super.validate();
		if (error == null && !validateOldPassword()) {
			error = Messages.ChangePasswordDialog_OldPassword_Error;
		}
		return error;
	}
	
	private boolean validateOldPassword() {
		return InformantAesManager.getInstance().validatePassword(txtOldPassword.getTextChars());
	}
}
