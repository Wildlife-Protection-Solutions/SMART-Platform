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
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
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
	
	private ControlDecoration cdUser;
	private ControlDecoration cdPassword;
	private ControlDecoration cdServer;
	
	private SmartConnect connect;
	private boolean hideConfigure;
	
	public ConnectDialog(Shell parentShell) {
		this(parentShell, false);
	}

	public ConnectDialog(Shell parentShell, boolean hideConfigure) {
		super(parentShell);
		this.hideConfigure = hideConfigure;
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		validate();
		
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
		l.setText("Server:");
		
		lblServer = new Label(main, SWT.NONE);
		lblServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cdServer = createControlDecoration(lblServer);
		cdServer.setDescriptionText("Connect server not configured.");
		
		if (hideConfigure){
			lblServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		}else{
			Button btnConfigure = new Button(main, SWT.PUSH);
			btnConfigure.setText("Configure...");
			btnConfigure.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					(new ShowServerConfigurationHandler()).execute(getParentShell());
					initData();
				}
			});
		}
		l = new Label(main, SWT.NONE);
		l.setText("Username:");
		
		txtUser = new Text(main, SWT.BORDER);
		txtUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		cdUser = createControlDecoration(txtUser);
		cdUser.setDescriptionText("User required.");
		
		l = new Label(main, SWT.NONE);
		l.setText("Password:");
		
		txtPassword = new Text(main, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		cdPassword = createControlDecoration(txtPassword);
		cdPassword.setDescriptionText("Password required.");
		
		new Label(main, SWT.NONE);
		chSavePassword = new Button(main, SWT.CHECK);
		chSavePassword.setText("Save Password");
		chSavePassword.setToolTipText("Selecting will save the password to the database, unselecting will clear the password");
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
	private void initData(){

		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			cs = (ConnectServer)s.createCriteria(ConnectServer.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.uniqueResult();
			
			user = (ConnectUser) s.get(ConnectUser.class, SmartDB.getCurrentEmployee().getUuid());
			
			s.getTransaction().commit();
		}finally{
			s.close();
		}
		
		lblServer.setText("");
		txtUser.setText("");
		txtPassword.setText("");
		
		if (cs != null){
			lblServer.setText(cs.getServerUrl());
		}
		if (user != null){
			txtUser.setText(user.getConnectUsername());
			String decryptPass = "";
			try{
				decryptPass  = ConnectPlugIn.decryptPassword(user);
			}catch (Exception ex){
				ConnectPlugIn.log("Error decrypting password." + ex.getMessage(), ex);
			}
			if (decryptPass == null){
				txtPassword.setText("");
				txtPassword.setData("");
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
					monitor.beginTask("Connecting to SMART Connect", 2);
					monitor.worked(1);
					
					if (server.isEmpty()){
						msgerror[0] = "Connect server required.  Use configure button to setup the connect server.";
						return;
					}
					if (user.isEmpty()){
						msgerror[0] = "Connect user name required.";
						return;
					}
					if (pass.isEmpty()){
						msgerror[0] = "Connect password required.";
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
							existingPassword = "";
						}
						String newPassword = null;
						if (savePass){
							newPassword = ConnectPlugIn.encryptPassword(pass);
						
							if (!strequals(existingPassword, newPassword == null ? null : pass)){
								Session s = HibernateManager.openSession();
								try{
									s.beginTransaction();
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
						}
					}catch (Exception ex){
						ConnectPlugIn.displayLog("Unable to save password." + "\n" + ex.getMessage(), ex);
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
			MessageDialog.openError(getShell(), "Error", msgerror[0]);
			return;
		}
		super.okPressed();
	}
	
	public SmartConnect getConnection(){
		return this.connect;
	}
}
