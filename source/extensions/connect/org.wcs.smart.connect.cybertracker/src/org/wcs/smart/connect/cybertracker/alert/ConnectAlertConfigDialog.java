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
package org.wcs.smart.connect.cybertracker.alert;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Dialog that provides SMART Connect server data (url, username, password) data that will be used
 * to configure alerts for CyberTracker application.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectAlertConfigDialog extends ConnectDialog {
	
	private String server;
	private String username;
	private String password;

	public ConnectAlertConfigDialog(Shell parentShell) {
		super(parentShell, true);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.ConnectAlertConfigDialog_Title);
		getShell().setText(Messages.ConnectAlertConfigDialog_Title);
		setMessage(Messages.ConnectAlertConfigDialog_Message);
		Control control = super.createDialogArea(parent);
		getChSavePassword().setVisible(false);
		return control;
	}
	
	protected void loadDatabaseInformation(){
		Thread thread = new Thread() {
			@Override
			public void run() {
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
		};
		thread.start();
		try {
			thread.join(); //we need to wait till thread is completed
		} catch (InterruptedException e) {
			SmartPlugIn.displayLog(Messages.ConnectAlertConfigDialog_DataExtractError, e);
		}
	}

	@Override
	protected void saveUserInfo(final String user, String newPassword) {
		//this dialog do not save anything
	}
	
	@Override
	protected void okPressed() {
		server = cs.getServerUrl();
		username = getTxtUser().getText().trim();
		password = getTxtPassword().getText().trim();
		setReturnCode(Window.OK);
		close();
	}
	
	public String getServerUrl() {
		return server;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
}
