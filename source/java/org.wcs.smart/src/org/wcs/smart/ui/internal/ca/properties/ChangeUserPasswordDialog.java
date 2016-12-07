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
package org.wcs.smart.ui.internal.ca.properties;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * Create a change user password dialog where users can
 * change their username or password.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ChangeUserPasswordDialog extends AbstractPropertyJHeaderDialog{

	private Employee toUpdate = null;
	private Text txtPassword1;
	private Text txtPassword2;
	private Text txtCurrentPassword;
	
	private ControlDecoration cdPassword1;
	private ControlDecoration cdPassword2;
	private ControlDecoration cdCurrentPassword;

	/**
	 * Create a new user password dialog.
	 * 
	 * @param parent the parent shell
	 * 
	 */
	public ChangeUserPasswordDialog(Shell parent) {
		super(parent, Messages.ChangeUserPasswordDialog_DialogTitle);
		toUpdate = SmartDB.getCurrentEmployee();
	}
	
	@Override
	public boolean close(){
		changesMade = false;
		return super.close();  
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		
		ModifyListener validateListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
				validate();
			}
		};
		Composite data = new Composite(parent, SWT.NONE);
		data.setLayout(new GridLayout(3, false));
		
		Label lbl = new Label(data, SWT.NONE);
		lbl.setText(Messages.ChangeUserPasswordDialog_Username_Label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		final Text txtUserName = new Text(data, SWT.BORDER);
		txtUserName.setText(toUpdate.getSmartUserId());
		txtUserName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtUserName.setEditable(false);
		Button modifyUser = new Button(data, SWT.PUSH);
		modifyUser.setText(Messages.ChangeUserPasswordDialog_Modify_Button);
		modifyUser.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				InputDialog dialog = new InputDialog(ChangeUserPasswordDialog.this.getParentShell(), Messages.ChangeUserPasswordDialog_ChangeUser_DialogTitle, Messages.ChangeUserPasswordDialog_ChangeUser_DialogMessage, toUpdate.getSmartUserId(), 
						new IInputValidator() {
							@Override
							public String isValid(String newText) {
								if (!SmartUtils.isSimpleString(newText, RegExLevel.ALLOWED_CHARS_MED_REGEX, Employee.MAX_SMART_ID_LENGTH, Employee.MIN_SMART_ID_LENGTH)){
									return MessageFormat.format(Messages.ChangeUserPasswordDialog_UserNameValidationError, new Object[]{RegExLevel.ALLOWED_CHARS_MED_REGEX.textDesc, Employee.MIN_SMART_ID_LENGTH, Employee.MAX_SMART_ID_LENGTH});
								}
								return null;
							}
						});
				if (dialog.open() == Window.CANCEL){
					return;
				}
				
				//check to ensure that user name does not already exist
				String newUserName = dialog.getValue();
				if (newUserName.equals(toUpdate.getSmartUserId())){
					return;
				}
				
				Session s = HibernateManager.openSession();
				try{
					List<?> otherUsers = s.createCriteria(Employee.class).add(Restrictions.eq("smartUserId", newUserName)).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$ //$NON-NLS-2$
					if (otherUsers.size() > 0){
						MessageDialog.openError(ChangeUserPasswordDialog.this.getShell(), Messages.ChangeUserPasswordDialog_Error_DialogTitle, MessageFormat.format(Messages.ChangeUserPasswordDialog_Error_UserExists, new Object[]{ newUserName }));
						return;
					}else{
						String old = toUpdate.getSmartUserId();
						s.beginTransaction();
						try {
							s.update(toUpdate);
							toUpdate.setSmartUserId(newUserName);
							s.getTransaction().commit();
						} catch (Exception ex) {
							toUpdate.setSmartUserId(old);
							s.getTransaction().rollback();
							SmartPlugIn.displayLog(Messages.ChangeUserPasswordDialog_Error_CouldNoUpdateUser + ex.getLocalizedMessage(), ex);
						}
					
						txtUserName.setText(toUpdate.getSmartUserId());
					}
				}finally{
					s.close();
				}
				
			}
			
		});
		
		lbl = new Label(data, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		lbl = new Label(data, SWT.NONE);
		lbl.setText(Messages.ChangeUserPasswordDialog_PasswordLabel);		
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		txtCurrentPassword = new Text(data, SWT.BORDER | SWT.PASSWORD);
		txtCurrentPassword.setText(""); //$NON-NLS-1$
		txtCurrentPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)txtCurrentPassword.getLayoutData()).horizontalIndent = 5;
		txtCurrentPassword.addModifyListener(validateListener);
		cdCurrentPassword = createDecoration(txtCurrentPassword);
		
		lbl = new Label(data, SWT.NONE);
		lbl.setText(Messages.ChangeUserPasswordDialog_NewPassword_Label);		
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		txtPassword1 = new Text(data, SWT.BORDER | SWT.PASSWORD);
		txtPassword1.setText(""); //$NON-NLS-1$
		txtPassword1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)txtPassword1.getLayoutData()).horizontalIndent = 5;
		txtPassword1.addModifyListener(validateListener);
		cdPassword1 = createDecoration(txtPassword1);
		
		lbl = new Label(data, SWT.NONE);
		lbl.setText(Messages.ChangeUserPasswordDialog_NewPassword2_Label);		
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		txtPassword2 = new Text(data, SWT.BORDER | SWT.PASSWORD);
		txtPassword2.setText(""); //$NON-NLS-1$
		txtPassword2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)txtPassword2.getLayoutData()).horizontalIndent = 5;
		txtPassword2.addModifyListener(validateListener);
		cdPassword2 = createDecoration(txtPassword2);
		
		setTitle(Messages.ChangeUserPasswordDialog_PageTitle);
		setMessage(Messages.ChangeUserPasswordDialog_Dialog_Message);
		validate();
		return data;
	}

	
	/**
	 * Overrides button press.
	 * 
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (performSave()){
				super.setReturnCode(IDialogConstants.OK_ID);
				close();
			}
		} else if (IDialogConstants.CLOSE_ID == buttonId) {
			//super.setReturnCode(IDialogConstants.CLOSE_ID);
			close();
		}
	}
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	/**
	 * Validates the current input
	 */
	private void validate(){
		
		boolean error = false;
		
		if (txtCurrentPassword.getText().length() == 0){
			cdCurrentPassword.setDescriptionText(Messages.ChangeUserPasswordDialog_Error_MustEnterPass);
			cdCurrentPassword.show();
			error = true;
		}else{
			cdCurrentPassword.hide();
		}
		
		if (txtPassword1.getText().length() == 0){
			cdPassword1.setDescriptionText(Messages.ChangeUserPasswordDialog_Error_MustEnterNewPass);
			cdPassword1.show();
			error = true;
		}else if (txtPassword1.getText().length() < Employee.MIN_SMART_PASSWORD_LENGTH || txtPassword1.getText().length() > Employee.MAX_SMART_PASSWORD_LENGTH){
			cdPassword1.setDescriptionText(MessageFormat.format(Messages.ChangeUserPasswordDialog_PasswordValidationError, new Object[]{Employee.MIN_SMART_PASSWORD_LENGTH, Employee.MAX_SMART_PASSWORD_LENGTH}));
			cdPassword1.show();
			error = true;
		}else if (txtPassword1.getText().equals(txtCurrentPassword.getText())){
			cdPassword1.setDescriptionText(Messages.ChangeUserPasswordDialog_PasswordDifferentError);
			error = true;
			cdPassword1.show();
		}else{
			cdPassword1.hide();
		}
		
		boolean hide = true;
		if (txtPassword2.getText().length() == 0){
			cdPassword2.setDescriptionText(Messages.ChangeUserPasswordDialog_Error_MustEnterNewPass2);
			cdPassword2.show();
			hide = false;
			error = true;
		}

		if (!txtPassword1.getText().equals(txtPassword2.getText())){
			cdPassword2.setDescriptionText(Messages.ChangeUserPasswordDialog_Error_PassNotSame);
			cdPassword2.show();
			error = true;
			hide = false;
		}
		if (hide){
			cdPassword2.hide();
		}
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			btn.setEnabled(!error);
		}
		
		
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		setErrorMessage(null);
		if (txtPassword1.getText().equals(txtPassword2.getText())){
			//check to ensure old password is correct
			if (HibernateManager.validatePassword(txtCurrentPassword.getText(), toUpdate)){
				String old = toUpdate.getSmartPassword();
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try {
					s.update(toUpdate);
					toUpdate.setSmartPassword(HibernateManager.generatePassword(txtPassword1.getText()));
					s.getTransaction().commit();
					
					MessageDialog.openInformation(ChangeUserPasswordDialog.this.getShell(), Messages.ChangeUserPasswordDialog_Updated_DialogTitle, Messages.ChangeUserPasswordDialog_Updated_DialogMessage);
					setChangesMade(false);
					return true;
				} catch (Exception ex) {
					if (s.getTransaction().isActive()) s.getTransaction().rollback();
					toUpdate.setSmartPassword(old);	//reset password
					SmartPlugIn.displayLog(Messages.ChangeUserPasswordDialog_Error_CouldNotUpdatePass + ex.getLocalizedMessage(), ex);
				}finally{
					s.close();
				}
			}else{
				setErrorMessage(Messages.ChangeUserPasswordDialog_Error_NewPassNotSame);
			}	
		}
		return false;
	}

}
