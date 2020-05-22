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
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.paws.model.PawsWorkspace;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for configuring PAWS Azure and AI servers
 * @author Emily
 *
 */
public class ServerConfigurationDialog extends SmartStyledDialog {

	private Text txtServiceHeatmapApi;
	private Text txtServiceTaskApi;
	private Text txtServiceKey;
	
	private Text txtWorkspaceLoginURL;
	private Text txtWorkspaceClientId;
	private Text txtWorkspaceStorageUrl;
	private Text txtWorkspaceContainer;
	
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
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$
				if (service == null) {
					service = new PawsService();
					service.setConservationArea(SmartDB.getCurrentConservationArea());
				}
				service.setApiKey(txtServiceKey.getText());
				service.setHeatmapApi(txtServiceHeatmapApi.getText());
				service.setTaskApi(txtServiceTaskApi.getText());
				session.saveOrUpdate(service);
				
				PawsWorkspace ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$
				if (ws == null) {
					ws = new PawsWorkspace();
					ws.setConservationArea(SmartDB.getCurrentConservationArea());
				}
				ws.setClientId(txtWorkspaceClientId.getText());
				ws.setUrl(txtWorkspaceLoginURL.getText());
				ws.setStorageAccountUrl(txtWorkspaceStorageUrl.getText());
				ws.setContainer(txtWorkspaceContainer.getText());
				session.saveOrUpdate(ws);
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				PawsPlugIn.displayLog(Messages.ServerConfigurationDialog_SaveError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return;
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(false);
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
		
		SmartUiUtils.createHeaderLabel(main, Messages.ServerConfigurationDialog_ServiceSection);
		
		Composite paws = new Composite(main, SWT.NONE);
		paws.setLayout(new GridLayout(2, false));
		((GridLayout)paws.getLayout()).marginWidth = 0;
		((GridLayout)paws.getLayout()).marginHeight = 0;
		paws.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label infoMessage = new Label(paws, SWT.NONE);
		infoMessage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		infoMessage.setText(Messages.ServerConfigurationDialog_ServiceMsg);
		
		Label l = new Label(paws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_APILbl);
		l.setToolTipText(Messages.ServerConfigurationDialog_APITooltip);
		
		txtServiceHeatmapApi = new Text(paws, SWT.BORDER);
		txtServiceHeatmapApi.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtServiceHeatmapApi.setText(DialogConstants.LOADING_TEXT);
		txtServiceHeatmapApi.addListener(SWT.Modify, modifiedlistener);
		
		l = new Label(paws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_TaskAPI);
		l.setToolTipText(Messages.ServerConfigurationDialog_TaskRequestURL);
		
		txtServiceTaskApi = new Text(paws, SWT.BORDER);
		txtServiceTaskApi.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtServiceTaskApi.setText(DialogConstants.LOADING_TEXT);
		txtServiceTaskApi.addListener(SWT.Modify, modifiedlistener);
		
		l = new Label(paws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_APIKey);
		
		txtServiceKey = new Text(paws, SWT.BORDER  | SWT.PASSWORD);
		txtServiceKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtServiceKey.setText(DialogConstants.LOADING_TEXT);
		txtServiceKey.addListener(SWT.Modify, modifiedlistener);
		
		
		SmartUiUtils.createHeaderLabel(main, Messages.ServerConfigurationDialog_StorageSection);
		
		Composite pawws = new Composite(main, SWT.NONE);
		pawws.setLayout(new GridLayout(2, false));
		((GridLayout)pawws.getLayout()).marginWidth = 0;
		((GridLayout)pawws.getLayout()).marginHeight = 0;
		pawws.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		infoMessage = new Label(pawws, SWT.WRAP);
		infoMessage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		infoMessage.setText(Messages.ServerConfigurationDialog_StorageMessage);
		((GridData)infoMessage.getLayoutData()).widthHint = 350;
		
		l = new Label(pawws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_OauthLbl);
		
		txtWorkspaceLoginURL = new Text(pawws, SWT.BORDER);
		txtWorkspaceLoginURL.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWorkspaceLoginURL.setText(DialogConstants.LOADING_TEXT);
		txtWorkspaceLoginURL.setText("https://login.microsoftonline.com/common/oauth2"); //$NON-NLS-1$
		txtWorkspaceLoginURL.addListener(SWT.Modify, modifiedlistener);
		
		l = new Label(pawws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_ClientId);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtWorkspaceClientId = new Text(pawws, SWT.BORDER );
		txtWorkspaceClientId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWorkspaceClientId.setText(DialogConstants.LOADING_TEXT);
		txtWorkspaceClientId.addListener(SWT.Modify, modifiedlistener);
		
		l = new Label(pawws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_Url);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtWorkspaceStorageUrl = new Text(pawws, SWT.BORDER );
		txtWorkspaceStorageUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWorkspaceStorageUrl.setText(DialogConstants.LOADING_TEXT);
		txtWorkspaceStorageUrl.addListener(SWT.Modify, modifiedlistener);
		
		l = new Label(pawws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_CName);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtWorkspaceContainer = new Text(pawws, SWT.BORDER );
		txtWorkspaceContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWorkspaceContainer.setText(DialogConstants.LOADING_TEXT);
		txtWorkspaceContainer.addListener(SWT.Modify, modifiedlistener);
	
	
		getShell().setText(Messages.ServerConfigurationDialog_Title);
		loadingJob.schedule();
		return main;
	}
	
	
	private Job loadingJob = new Job(Messages.ServerConfigurationDialog_LoadJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			PawsService service = null;
			PawsWorkspace ws = null;
			try(Session session = HibernateManager.openSession()){
				service = QueryFactory.buildQuery(session, PawsService.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$
				ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$
			}
			final PawsService fservice = service;
			final PawsWorkspace fws = ws;
			Display.getDefault().asyncExec(()->{
				txtServiceHeatmapApi.setText(" https://paws-api-backend-api-mgmt.azure-api.net/paws/predict-risk"); //$NON-NLS-1$
				txtServiceTaskApi.setText("https://paws-api-backend-api-mgmt.azure-api.net/taskmanagement/task"); //$NON-NLS-1$
				txtServiceKey.setText(""); //$NON-NLS-1$
				if (fservice != null) {
					txtServiceKey.setText(fservice.getApiKey() == null ? "" : fservice.getApiKey()); //$NON-NLS-1$
					if (fservice.getHeatmapApi() != null) txtServiceHeatmapApi.setText(fservice.getHeatmapApi());
					if (fservice.getTaskApi() != null) txtServiceTaskApi.setText(fservice.getTaskApi());
				}
				
				txtWorkspaceStorageUrl.setText("https://<yourblob>.blob.core.windows.net"); //$NON-NLS-1$
				txtWorkspaceClientId.setText("<Application/Client ID>"); //$NON-NLS-1$
				txtWorkspaceContainer.setText("<Container Name>"); //$NON-NLS-1$
				if (fws != null) {
					if (fws.getUrl() != null) txtWorkspaceClientId.setText(fws.getClientId());
					if (fws.getUrl() != null) txtWorkspaceLoginURL.setText(fws.getUrl());
					
					if (fws.getContainer() != null)  txtWorkspaceContainer.setText(fws.getContainer());
					if (fws.getStorageAccountUrl() != null) txtWorkspaceStorageUrl.setText(fws.getStorageAccountUrl());
				}
				
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
