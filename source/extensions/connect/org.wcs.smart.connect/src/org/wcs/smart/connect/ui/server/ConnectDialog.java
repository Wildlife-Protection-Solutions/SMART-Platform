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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
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
	
	protected ConnectServer cs = null;
	protected ConnectUser user = null;
	
	private SmartConnect connect;
	
	public ConnectDialog(Shell parentShell) {
		super(parentShell);
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
		
		Button btnConfigure = new Button(main, SWT.PUSH);
		btnConfigure.setText("Configure...");
		btnConfigure.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				(new ShowServerConfigurationHandler()).execute(getParentShell());
				initData();
			}
		});
		l = new Label(main, SWT.NONE);
		l.setText("Username:");
		
		txtUser = new Text(main, SWT.BORDER);
		txtUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		
		l = new Label(main, SWT.NONE);
		l.setText("Password:");
		
		txtPassword = new Text(main, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		
		initData();
		
		return parent;
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
			txtPassword.setText(user.getConnectPassword());
		}
		
	}
	@Override
	public boolean isResizable(){
		return true;
	}
	
	protected void okPressed(){
		
		final boolean[] ret = new boolean[]{false};
		final String server = cs.getServerUrl();
		final String user = txtUser.getText().trim();
		final String pass = txtPassword.getText().trim();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("Connecting to SMART Connect", 2);
					monitor.worked(1);
					
					if (server.isEmpty()){
						MessageDialog.openError(getShell(), "Error", "Connect server required. User configure button to setup the connect server.");
						return;
					}
					if (user.isEmpty()){
						MessageDialog.openError(getShell(), "Error", "Connect user name required.");
						return;
					}
					if (pass.isEmpty()){
						MessageDialog.openError(getShell(), "Error", "Connect password required.");
						return;
					}
					
					SmartConnect connect = SmartConnect.findInstance(cs, user, pass);
					try{
						String error = connect.validateUser();
						if (error != null){
							MessageDialog.openError(getShell(), "Error", error);
							return;
						}
					}catch (Exception ex){
						ConnectPlugIn.displayLog(ex.getMessage(), ex);
						return;
					}
					
					ConnectDialog.this.connect = SmartConnect.findInstance(cs, user, pass);
					ret[0] = true;
					monitor.done();
				}
			});
		}catch(Exception ex){
			ConnectPlugIn.displayLog(ex.getMessage(), ex);
			return;
		}
		
		if (ret[0]) super.okPressed();
	}
	
	public SmartConnect getConnection(){
		return this.connect;
	}
}
