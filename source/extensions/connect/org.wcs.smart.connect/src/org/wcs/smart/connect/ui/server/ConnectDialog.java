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
package org.wcs.smart.connect.ui.server;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.wcs.smart.ca.Employee;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.ui.server.configure.ShowServerConfigurationHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Generate connect server dialog which asks for username and password.
 * 
 * @author Emily
 *
 */
public class ConnectDialog extends TitleAreaDialog {

	private Label lblServer;
	
	private Text txtUser;
	private Text txtPassword;
	private Button chSavePassword;
	
	protected ConnectServer cs = null;
	protected ConnectUser user = null;
	protected Employee employee;
	
	private ControlDecoration cdUser;
	private ControlDecoration cdPassword;
	private ControlDecoration cdServer;
	
	private SmartConnect connect;
	private boolean hideConfigure;
	
	public ConnectDialog(Shell parentShell) {
		this(parentShell, false, SmartDB.getCurrentEmployee());
	}

	public ConnectDialog(Shell parentShell, boolean hideConfigure) {
		this(parentShell, hideConfigure, SmartDB.getCurrentEmployee());
	}
	
	public ConnectDialog(Shell parentShell, boolean hideConfigure, Employee e){
		super(parentShell);
		this.hideConfigure = hideConfigure;
		this.employee = e;
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		validate();
		getButton(OK).setFocus();
	}
	
	protected ControlDecoration createControlDecoration(Control widget){
		ControlDecoration cd = new ControlDecoration(widget, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.ConnectDialog_ServerLabel);
		
		lblServer = new Label(main, SWT.NONE);
		lblServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cdServer = createControlDecoration(lblServer);
		cdServer.setDescriptionText(Messages.ConnectDialog_ServerNotConfiguredError);
		
		if (hideConfigure){
			lblServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		}else{
			Button btnConfigure = new Button(main, SWT.PUSH);
			btnConfigure.setText(Messages.ConnectDialog_ConfigureLabel);
			btnConfigure.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					(new ShowServerConfigurationHandler()).execute(getParentShell());
					initData();
				}
			});
		}
		l = new Label(main, SWT.NONE);
		l.setText(Messages.ConnectDialog_UsernameLabel);
		
		txtUser = new Text(main, SWT.BORDER);
		txtUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		cdUser = createControlDecoration(txtUser);
		cdUser.setDescriptionText(Messages.ConnectDialog_UsernameRequired);
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.ConnectDialog_Password);
		
		txtPassword = new Text(main, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		cdPassword = createControlDecoration(txtPassword);
		cdPassword.setDescriptionText(Messages.ConnectDialog_PasswordRequired);
		
		new Label(main, SWT.NONE);
		chSavePassword = new Button(main, SWT.CHECK);
		chSavePassword.setText(Messages.ConnectDialog_SaveText);
		chSavePassword.setToolTipText(Messages.ConnectDialog_SaveToolTip);
		chSavePassword.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		initData();
		
		ModifyListener validate = new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		txtPassword.addModifyListener(validate);
		txtUser.addModifyListener(validate);
		return parent;
	}

	private void validate(){
		boolean ok = true;
		if (lblServer.getText().trim().isEmpty()){
			ok = false;
			cdServer.show();
		}else{
			cdServer.hide();
		}
		if (txtUser.getText().trim().isEmpty()){
			ok = false;
			cdUser.show();
		}else{
			cdUser.hide();
		}
		if (txtPassword.getText().trim().isEmpty()){
			ok = false;
			cdPassword.show();
		}else{
			cdPassword.hide();
		}
		
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			btn.setEnabled(ok);
		}		
	}
	
	/**
	 * Loads the server info from the database
	 */
	protected void loadDatabaseInformation(){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			cs = ConnectHibernateManager.getConnectServer(s);
			user = ConnectHibernateManager.getConnectUser(employee, s);			
			s.getTransaction().commit();
		}finally{
			s.close();
		}
	}
	
	/**
	 * Updates the database with the saved user info
	 * @param user
	 * @param newPassword
	 * @throws Exception
	 */
	protected void saveUserInfo(final String user, String newPassword)
			throws Exception {
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			if (ConnectDialog.this.user == null){
				ConnectUser newuser = new ConnectUser();
				newuser.setConnectUsername(user);
				newuser.setServer(cs);
				newuser.setSmartUser(employee);
				ConnectDialog.this.user = newuser;
				s.save(newuser);
			}
			ConnectDialog.this.user.setConnectPassword(newPassword);
			s.saveOrUpdate(ConnectDialog.this.user);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}finally{
			s.close();
		}
	}
	
	private void initData(){
		loadDatabaseInformation();
		
		lblServer.setText(""); //$NON-NLS-1$
		txtUser.setText(""); //$NON-NLS-1$
		txtPassword.setText(""); //$NON-NLS-1$
		
		if (cs != null){
			lblServer.setText(cs.getServerUrl());
		}
		if (user != null){
			txtUser.setText(user.getConnectUsername());
			String decryptPass = ""; //$NON-NLS-1$
			try{
				decryptPass  = ConnectPlugIn.decryptPassword(user);
			}catch (Exception ex){
				ConnectPlugIn.log(Messages.ConnectDialog_DecryptError + ex.getMessage(), ex);
			}
			if (decryptPass == null){
				txtPassword.setText(""); //$NON-NLS-1$
				txtPassword.setData(""); //$NON-NLS-1$
				chSavePassword.setSelection(false);
			}else{
				txtPassword.setText(decryptPass);
				chSavePassword.setSelection(true);
			}
		}
		
		validate();
	}
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private boolean strequals(String s1, String s2){
		if (s1 == null && s2 == null) return true;
		if (s1 != null && s2 != null) return s1.equals(s2);
		return false;
	}
	
	protected void okPressed(){
		
		final String[] msgerror = new String[]{null};
		final String server = cs.getServerUrl();
		final String user = txtUser.getText().trim();
		
		final String pass = txtPassword.getText().trim();
		final boolean savePass = chSavePassword.getSelection();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.ConnectDialog_ConnectingTask, 2);
					monitor.worked(1);
					
					if (server.isEmpty()){
						msgerror[0] = Messages.ConnectDialog_ServerRequiredError;
						return;
					}
					if (user.isEmpty()){
						msgerror[0] = Messages.ConnectDialog_UserRequiredError;
						return;
					}
					if (pass.isEmpty()){
						msgerror[0] = Messages.ConnectDialog_PasswordRequiredError;
						return;
					}
					
					SmartConnect connect = SmartConnect.findInstance(cs, user, pass);
					try{
						String error = connect.validateUser();
						if (error != null){
							msgerror[0] = error;
							return;
						}
					}catch (Exception ex){
						ConnectPlugIn.log(ex.getMessage(), ex);
						msgerror[0] = ex.getMessage();
						return;
					}
					
					//save password
					try{
						String existingPassword = null;
						try{
							existingPassword = ConnectPlugIn.decryptPassword(ConnectDialog.this.user);
						}catch (Exception ex){
							existingPassword = ""; //$NON-NLS-1$
						}
						String newPassword = null;
						if (savePass){
							newPassword = ConnectPlugIn.encryptPassword(pass);
						}
						if (ConnectDialog.this.user == null || savePass){
							if (!strequals(existingPassword, newPassword == null ? null : pass)){
								saveUserInfo(user, newPassword);
							}
						}
						
					}catch (Exception ex){
						ConnectPlugIn.displayLog(Messages.ConnectDialog_SavePasswordError + "\n" + ex.getMessage(), ex); //$NON-NLS-1$
					}
					
					
					ConnectDialog.this.connect = connect;
					monitor.done();
				}
			});
		}catch(Exception ex){
			ConnectPlugIn.displayLog(ex.getMessage(), ex);
			return;
		}
		
		if (msgerror[0] != null){
			MessageDialog.openError(getShell(), Messages.ConnectDialog_ErrorDialogTitle, msgerror[0]);
			return;
		}
		super.okPressed();
	}
	
	public SmartConnect getConnection(){
		return this.connect;
	}
}
