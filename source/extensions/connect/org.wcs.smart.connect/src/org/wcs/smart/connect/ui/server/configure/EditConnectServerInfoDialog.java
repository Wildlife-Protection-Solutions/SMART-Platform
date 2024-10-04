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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for modifying connect server account details.
 * 
 * @author Emily
 *
 */
public class EditConnectServerInfoDialog extends SmartStyledTitleDialog{

	private ServerPanel serverpnl;
	
	private ConnectServer server;
	private boolean changesMade = false;
	private IServerOptionsPanel[]  optionPanels = OptionPanelManager.createOptionPanels(SmartDB.getCurrentConservationArea());
	
	public EditConnectServerInfoDialog(Shell parentShell, ConnectServer server) {
		super(parentShell);
		
		this.server = server;
	}

	private boolean validateServer(){
		ConnectServer temp = new ConnectServer(){
			private static final long serialVersionUID = 1L;
			Path cert = null;
			@Override
			public void setCertificateFile(Path newFile) throws Exception{
				cert = newFile;
			}
			
			@Override
			public String getCertificateFileName(){
				if (cert == null) return null;
				return cert.getFileName().toString();
			}
			@Override
			public Path getLocalCertificateFile(){
				return cert;
			}
		};
		
		
		temp.setConservationArea(SmartDB.getCurrentConservationArea());
		try {
			if (serverpnl.getCertificateFile() != null && !serverpnl.getCertificateFile().isBlank()){
				temp.setCertificateFile(Paths.get(serverpnl.getCertificateFile()));
			}else{
				temp.setCertificateFile(server.getLocalCertificateFile());
			}
			temp.setServerUrl(serverpnl.getServerUrl());

			final String[] error = new String[]{null};
			ConnectDialog cd = new ConnectDialog(getShell(), true, SmartDB.getCurrentEmployee()){
				@Override
				protected void loadDatabaseInformation(){
					cs = temp;
					try(Session s = HibernateManager.openSession()){
					
						s.beginTransaction();
						try {
							user = ConnectHibernateManager.getConnectUser(employee, s);			
							s.getTransaction().commit();
						}catch (Exception ex) {
							s.getTransaction().rollback();
							throw ex;
						}
					}
				}
				@Override
				protected Control createDialogArea(Composite parent) {
					Control c = super.createDialogArea(parent);
					getShell().setText(Messages.EditConnectServerInfoDialog_ValidateUrlTitle);
					setTitle(Messages.EditConnectServerInfoDialog_ValidateUrlTitle);
					setMessage(Messages.EditConnectServerInfoDialog_ValidateUrlMsg);
					return c;
				}
				@Override
				protected void okPressed(){
					final String server = cs.getServerUrl();
					final String user = txtUser.getText().trim();
					final String pass = txtPassword.getText().trim();
					final boolean savePass = chSavePassword.getSelection();
					error[0] = validateConnection(server, user, pass, savePass);
					setReturnCode(OK);
					close();
				}
			};
			
			if (cd.open() != Window.OK){
				MessageDialog md = new MessageDialog(getShell(), 
						Messages.EditConnectServerInfoDialog_ValidateErrorTitle, null, Messages.EditConnectServerInfoDialog_ValidateError1, 
						MessageDialog.ERROR,
						new String[]{IDialogConstants.NO_LABEL, IDialogConstants.YES_LABEL}, 0);
				if (md.open() == 0)
					return false;
					
			}else if(error[0] != null){
				MessageDialog md = new MessageDialog(getShell(), 
						Messages.EditConnectServerInfoDialog_ValidateErrorTitle, null, 
						 MessageFormat.format(Messages.EditConnectServerInfoDialog_ValidateError2, error[0] ), 
						MessageDialog.ERROR,
						new String[]{IDialogConstants.NO_LABEL, IDialogConstants.YES_LABEL}, 0);
				if (md.open() == 0)
					return false;
			}
		} catch (Exception e) {
			MessageDialog md = new MessageDialog(getShell(), 
					Messages.EditConnectServerInfoDialog_ValidateErrorTitle, null, 
					 MessageFormat.format(Messages.EditConnectServerInfoDialog_ValidateError2, e.getMessage() ), 
					MessageDialog.ERROR,
					new String[]{IDialogConstants.NO_LABEL, IDialogConstants.YES_LABEL}, 0);
			if (md.open() == 0)
				return false;
		}
		return true;
	}
	
	protected void okPressed() {
		//validate server url
		if (!server.getServerUrl().toLowerCase().equals(serverpnl.getServerUrl().toLowerCase())){
			if (!validateServer()) return;
		}
		
		try(Session s = HibernateManager.openSession()){
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
				
				for (IServerOptionsPanel pnl : optionPanels){
					pnl.updateServer(server, s);
				}
				
				s.getTransaction().commit();
			}catch (Exception ex){
				ConnectPlugIn.displayLog(Messages.EditConnectServerInfoDialog_ServerError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return;
			}
		}
		
		for (IServerOptionsPanel pnl : optionPanels){
			pnl.afterSave(server);
		}
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(OK).setEnabled(false);
		getButton(OK).setText(DialogConstants.SAVE_TEXT);
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
		
		SmartUiUtils.createHeaderLabel(main, Messages.EditConnectServerInfoDialog_ConfigLabel);
		
		Composite g = new Composite(main, SWT.FLAT );
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		g.setLayout(new GridLayout(3, false));
		
		
		serverpnl = new ServerPanel(g);
		serverpnl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SmartUiUtils.createHeaderLabel(main, Messages.EditConnectServerInfoDialog_SettingsSection);
		
		Composite partComposite = new Composite(main, SWT.FLAT );
		partComposite.setLayout(new GridLayout(2, false));
		partComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
		ListViewer lstViewer = new ListViewer(partComposite, SWT.BORDER | SWT.V_SCROLL);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IServerOptionsPanel) return ((IServerOptionsPanel) element).getName();
				return super.getText(element);
			}
		});
		lstViewer.setInput(optionPanels);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite rightPanel = new Composite(partComposite, SWT.BORDER);
		rightPanel.setLayout(new StackLayout());
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((StackLayout)rightPanel.getLayout()).marginWidth = 0;
		((StackLayout)rightPanel.getLayout()).marginHeight = 0;

		HashMap<IServerOptionsPanel, Composite> panels = new HashMap<>();
		for (IServerOptionsPanel p : optionPanels){
			ScrolledComposite scroll = new ScrolledComposite(rightPanel, SWT.V_SCROLL | SWT.H_SCROLL);
			Composite part = p.createComposite(scroll, true);
			scroll.setContent(part);
			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			scroll.setMinSize(part.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			panels.put(p,  scroll);
			
			try(Session session = HibernateManager.openSession()){
				p.initValues(server, session);
			}
			p.addChangeListener(validateListener);
		}
		
		lstViewer.addSelectionChangedListener(e->{
			IServerOptionsPanel pp = (IServerOptionsPanel)lstViewer.getStructuredSelection().getFirstElement();
			((StackLayout)rightPanel.getLayout()).topControl = panels.get(pp);	
			rightPanel.layout();
		});
		
		lstViewer.setSelection(new StructuredSelection(optionPanels[0]));
		
//		TabFolder tabConfig = new TabFolder(g, SWT.NONE);
//		tabConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		
//		for(IServerOptionsPanel pnl : optionPanels){
//			TabItem ti = new TabItem(tabConfig, SWT.DEFAULT);
//			ti.setText(pnl.getName());
//						
//			ScrolledComposite scroll = new ScrolledComposite(tabConfig, SWT.V_SCROLL | SWT.BORDER | SWT.H_SCROLL);
//			Composite part = pnl.createComposite(scroll, true);
//			scroll.setContent(part);
//			scroll.setExpandHorizontal(true);
//			scroll.setExpandVertical(true);
//			scroll.setMinSize(part.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//			
//			ti.setControl(scroll);
//			
//			pnl.initValues(server);
//			pnl.addChangeListener(validateListener);
//		}
		
		serverpnl.initValues(server, null);
		serverpnl.addChangeListener(validateListener);
		
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
		for (IServerOptionsPanel pnl : optionPanels){
			if (!pnl.isValid()){
				setErrorMessage(MessageFormat.format(Messages.EditConnectServerInfoDialog_PanelErrorMessage, pnl.getName()));
				enableOk(false);
				return;
			}
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
