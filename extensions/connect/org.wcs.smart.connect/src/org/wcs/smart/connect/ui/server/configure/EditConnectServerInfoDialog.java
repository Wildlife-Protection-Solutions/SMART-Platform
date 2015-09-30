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
package org.wcs.smart.connect.ui.server.configure;

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
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.hibernate.HibernateManager;
/**
 * Dialog for modifying connect server account details.
 * 
 * @author Emily
 *
 */
public class EditConnectServerInfoDialog extends TitleAreaDialog{

	private Text url;
	private ServerOptionsPanel options;
	private ConnectServer server;
	
	public EditConnectServerInfoDialog(Shell parentShell, ConnectServer server) {
		super(parentShell);
		
		this.server = server;
	}

	protected void okPressed() {
		//TODO:: validate the server actually exists??? 
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			server.setServerUrl(url.getText());
			options.updateServer(server);
			s.saveOrUpdate(server);
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectPlugIn.displayLog("Could not update connect server information." + "\n\n" + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		super.okPressed();
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("URL:");
		
		ModifyListener validateListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		
		url = new Text(main, SWT.BORDER);
		url.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		url.setText(server.getServerUrl());
		url.addModifyListener(validateListener);
		options = new ServerOptionsPanel(parent);
		options.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		options.addChangeListener(validateListener);
		options.initValues(server);
		
		setTitle("Update SMART Connect Server Configuration");
		getShell().setText("Update SMART Connect Configurations");
		setMessage("Updates the details used to connect to a SMART Connect Server.");
		
		return main;
	}
	
	private void enableOk(boolean value){
		if (getButton(IDialogConstants.OK_ID) != null){
			getButton(IDialogConstants.OK_ID).setEnabled(value);
		}
	}
	private void validate(){
		setErrorMessage(null);
		if (!options.isValid()){
			enableOk(false);
			return;
		}
		if (url.getText().trim().isEmpty()){
			enableOk(false);
			return;
		}
		if (!url.getText().trim().startsWith("https://")){
			setErrorMessage("URL must start with https://");
			enableOk(false);
			return;
		}
		enableOk(true);
		
	}
	@Override
	public boolean isResizable(){
		return true;
	}
}
