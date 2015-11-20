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

import java.nio.file.Paths;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.server.replication.AutoReplicationStartUp;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Dialog for modifying connect server account details.
 * 
 * @author Emily
 *
 */
public class EditConnectServerInfoDialog extends TitleAreaDialog{

	private ServerPanel serverpnl;
	private AutoOptionsPanel autoPnl;
	private ServerOptionsPanel optionsPnl;
	private ConnectServer server;
	private boolean changesMade = false;
	
	public EditConnectServerInfoDialog(Shell parentShell, ConnectServer server) {
		super(parentShell);
		
		this.server = server;
	}

	protected void okPressed() {
		boolean isAutoReplicationPrev = server.getOptionAsBoolean(ConnectServerOption.Option.SYNC_AUTOMATICALLY);
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			server = (ConnectServer) s.merge(server);
			
			server.setServerUrl(serverpnl.getServerUrl());
			if (serverpnl.getCertificateFile() != null){
				String certificateFile = serverpnl.getCertificateFile();
				if (certificateFile.trim().isEmpty()){
					//clear
					certificateFile = null;
				}
				try{
					server.setCertificateFile(certificateFile == null ?  null : Paths.get(certificateFile));
				}catch (Exception ex){
					ConnectPlugIn.displayLog(Messages.EditConnectServerInfoDialog_CertificationError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
					return;
				}
			}
			
			
			optionsPnl.updateServer(server);
			autoPnl.updateServer(server);
			
			s.getTransaction().commit();
			
			boolean isAutoReplication = server.getOptionAsBoolean(ConnectServerOption.Option.SYNC_AUTOMATICALLY);
			if (!isAutoReplicationPrev && isAutoReplication){
				//auto replication state has been updated; we need to intiaite auto replication
				int delay = server.getOptionAsInt(ConnectServerOption.Option.SYNC_MINUTE);
				AutoReplicationStartUp.INSTANCE.enableAutoReplication(delay);
			}
		}catch (Exception ex){
			ConnectPlugIn.displayLog(Messages.EditConnectServerInfoDialog_ServerError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return;
		}finally{
			s.close();
		}
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(OK).setEnabled(false);
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ModifyListener validateListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				changesMade = true;
				validate();
			}
		};
		Group g = new Group(main, SWT.NONE);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		g.setText(Messages.EditConnectServerInfoDialog_ConfigLabel);
		serverpnl = new ServerPanel(g);
		
		serverpnl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		TabFolder tabConfig = new TabFolder(g, SWT.NONE);
		tabConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TabItem ti = new TabItem(tabConfig, SWT.DEFAULT);
		ti.setText(Messages.EditConnectServerInfoDialog_AutoLabel);
		ti.setControl(autoPnl = new AutoOptionsPanel(tabConfig));
		
		ti = new TabItem(tabConfig, SWT.DEFAULT);
		ti.setText(Messages.EditConnectServerInfoDialog_ConnectLabel);
		ti.setControl(optionsPnl = new ServerOptionsPanel(tabConfig));
		
		serverpnl.initValues(server);
		optionsPnl.initValues(server);
		autoPnl.initValues(server);
		
		serverpnl.addChangeListener(validateListener);
		optionsPnl.addChangeListener(validateListener);
		autoPnl.addChangeListener(validateListener);
		
		validate();
		
		setTitle(Messages.EditConnectServerInfoDialog_Title);
		getShell().setText(Messages.EditConnectServerInfoDialog_Shell);
		setMessage(Messages.EditConnectServerInfoDialog_Message);
		
		return main;
	}
	
	private void enableOk(boolean value){
		if (getButton(IDialogConstants.OK_ID) != null){
			if (changesMade){
				getButton(IDialogConstants.OK_ID).setEnabled(value);
			}else{
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}
		}
	}
	
	private void validate(){
		setErrorMessage(null);
		if (!optionsPnl.isValid()){
			setErrorMessage(Messages.EditConnectServerInfoDialog_connectError);
			enableOk(false);
			return;
		}
		if (!autoPnl.isValid()){
			setErrorMessage(Messages.EditConnectServerInfoDialog_autoError);
			enableOk(false);
			return;
		}
		
		if (!serverpnl.isValid()){
			setErrorMessage(Messages.EditConnectServerInfoDialog_serverError);
			enableOk(false);
			return;
		}
//		if (!url.getText().trim().startsWith("https://")){
//			setErrorMessage("URL must start with https://");
//			enableOk(false);
//			return;
//		}
		enableOk(true);
		
	}
	@Override
	public boolean isResizable(){
		return true;
	}
}
