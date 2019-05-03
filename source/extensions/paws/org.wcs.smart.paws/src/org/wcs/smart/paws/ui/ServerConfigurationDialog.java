/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.paws.model.PawsWorkspace;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DialogConstants;

public class ServerConfigurationDialog extends SmartStyledDialog {

	private Text txtServerURL;
	private Text txtWorkspaceURL;
	
	private Text txtServerKey;
	private Text txtWorkspaceKey;
	
	protected ServerConfigurationDialog(Shell parent) {
		super(parent);
	}

	private void modified() {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok != null) {
			ok.setEnabled(true);
		}
	}
	
	@Override
	public void okPressed() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				PawsService service = QueryFactory.buildQuery(session, PawsService.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult();
				if (service == null) {
					service = new PawsService();
					service.setConservationArea(SmartDB.getCurrentConservationArea());
				}
				service.setApiKey(txtServerKey.getText());
				service.setUrl(txtServerURL.getText());
				
				PawsWorkspace ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult();
				if (ws == null) {
					ws = new PawsWorkspace();
					ws.setConservationArea(SmartDB.getCurrentConservationArea());
				}
				ws.setApiKey(txtWorkspaceKey.getText());
				ws.setUrl(txtWorkspaceURL.getText());
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				PawsPlugIn.displayLog("Unable to save changes to PAWS Server configurations." + "\n\n" + ex.getMessage(), ex);
			}
		}
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		Listener modifiedlistener = e->modified();
		
		Composite main = (Composite) super.createDialogArea(parent);
		
		SmartUiUtils.createHeaderLabel(main, "PAWS Service");
		
		Composite paws = new Composite(main, SWT.NONE);
		paws.setLayout(new GridLayout(2, false));
		((GridLayout)paws.getLayout()).marginWidth = 0;
		((GridLayout)paws.getLayout()).marginHeight = 0;
		paws.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label infoMessage = new Label(paws, SWT.NONE);
		infoMessage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		infoMessage.setText("This is the PAWS service URL and API key.");
		
		Label l = new Label(paws, SWT.NONE);
		l.setText("URL:");
		
		txtServerURL = new Text(paws, SWT.BORDER);
		txtServerURL.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtServerURL.setText(DialogConstants.LOADING_TEXT);
		txtServerURL.addListener(SWT.Modify, modifiedlistener);
		
		
		l = new Label(paws, SWT.NONE);
		l.setText("API Key:");
		
		txtServerKey = new Text(paws, SWT.BORDER);
		txtServerKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtServerKey.setText(DialogConstants.LOADING_TEXT);
		txtServerKey.addListener(SWT.Modify, modifiedlistener);
		
		
		SmartUiUtils.createHeaderLabel(main, "PAWS Workspace");
		
		Composite pawws = new Composite(main, SWT.NONE);
		pawws.setLayout(new GridLayout(2, false));
		((GridLayout)pawws.getLayout()).marginWidth = 0;
		((GridLayout)pawws.getLayout()).marginHeight = 0;
		pawws.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		infoMessage = new Label(pawws, SWT.NONE);
		infoMessage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		infoMessage.setText("This must be a link to a Microsoft Azure cloud service");
		
		
		l = new Label(pawws, SWT.NONE);
		l.setText("URL:");
		
		txtWorkspaceURL = new Text(pawws, SWT.BORDER);
		txtWorkspaceURL.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWorkspaceURL.setText(DialogConstants.LOADING_TEXT);
		txtWorkspaceURL.addListener(SWT.Modify, modifiedlistener);
		
		l = new Label(pawws, SWT.NONE);
		l.setText("API Key:");
		
		txtWorkspaceKey = new Text(pawws, SWT.BORDER);
		txtWorkspaceKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWorkspaceKey.setText(DialogConstants.LOADING_TEXT);
		txtWorkspaceKey.addListener(SWT.Modify, modifiedlistener);
	
		getShell().setText("PAWS Server Configurations");
		loadingJob.schedule();
		return main;
	}
	
	
	private Job loadingJob = new Job("loading settings") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			PawsService service = null;
			PawsWorkspace ws = null;
			try(Session session = HibernateManager.openSession()){
				service = QueryFactory.buildQuery(session, PawsService.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult();
				ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult();
			}
			final PawsService fservice = service;
			final PawsWorkspace fws = ws;
			Display.getDefault().asyncExec(()->{
				if (fservice == null) {
					txtServerKey.setText("");
					txtServerURL.setText("");
				}else {
					txtServerKey.setText(fservice.getApiKey());
					txtServerURL.setText(fservice.getUrl());
				}
				
				if (fws == null) {
					txtWorkspaceKey.setText("");
					txtWorkspaceURL.setText("");
				}else {
					txtWorkspaceKey.setText(fws.getApiKey());
					txtWorkspaceURL.setText(fws.getUrl());
				}
				
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
