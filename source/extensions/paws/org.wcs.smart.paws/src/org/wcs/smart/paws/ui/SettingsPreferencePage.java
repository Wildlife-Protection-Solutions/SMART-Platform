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
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hibernate.Session;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Settings preference page for PAWS server settings
 * @author Emily
 *
 */
public class SettingsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Text txtServiceHeatmapApi;
	private Text txtServiceTaskApi;
	private Text txtServiceKey;
	
	private Text txtWorkspaceLoginURL;
	private Text txtWorkspaceClientId;
	private Text txtWorkspaceStorageUrl;
	
	
	public SettingsPreferencePage() {
	}

	public SettingsPreferencePage(String title) {
		super(title);
	}

	public SettingsPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		loadingJob.schedule();
	}

	@Override
    protected void performDefaults() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				PawsManager.INSTANCE.createDefaultSettings(session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				PawsPlugIn.displayLog("Unable to reset PAWS settings: " + ex.getMessage(), ex); //$NON-NLS-1$
			}
			
		}
		loadingJob.schedule();
	}

    	
	@Override
	public boolean performOk() {
		
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
				service.setPawsApiUrl(txtServiceHeatmapApi.getText());
				service.setTaskApiUrl(txtServiceTaskApi.getText());
				service.setClientId(txtWorkspaceClientId.getText());
				service.setOAuthUrl(txtWorkspaceLoginURL.getText());
				service.setStorageUrl(txtWorkspaceStorageUrl.getText());
				
				session.saveOrUpdate(service);
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				PawsPlugIn.displayLog(Messages.ServerConfigurationDialog_SaveError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}
	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label infoMessage1 = new Label(main, SWT.WRAP);
		infoMessage1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		infoMessage1.setText(Messages.SettingsPreferencePage_WarningMessage);
		((GridData)infoMessage1.getLayoutData()).widthHint = 350;

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

		l = new Label(paws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_TaskAPI);
		l.setToolTipText(Messages.ServerConfigurationDialog_TaskRequestURL);
		
		txtServiceTaskApi = new Text(paws, SWT.BORDER);
		txtServiceTaskApi.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtServiceTaskApi.setText(DialogConstants.LOADING_TEXT);

		l = new Label(paws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_APIKey);
		
		txtServiceKey = new Text(paws, SWT.BORDER  | SWT.PASSWORD);
		txtServiceKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtServiceKey.setText(DialogConstants.LOADING_TEXT);

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

		l = new Label(pawws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_ClientId);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtWorkspaceClientId = new Text(pawws, SWT.BORDER );
		txtWorkspaceClientId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWorkspaceClientId.setText(DialogConstants.LOADING_TEXT);

		l = new Label(pawws, SWT.NONE);
		l.setText(Messages.ServerConfigurationDialog_Url);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtWorkspaceStorageUrl = new Text(pawws, SWT.BORDER );
		txtWorkspaceStorageUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWorkspaceStorageUrl.setText(DialogConstants.LOADING_TEXT);

		getShell().setText(Messages.ServerConfigurationDialog_Title);

		
		return parent;
	}
	
	
	private Job loadingJob = new Job(Messages.ServerConfigurationDialog_LoadJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			PawsService service = null;
			try(Session session = HibernateManager.openSession()){
				service = QueryFactory.buildQuery(session, PawsService.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$

				if (service == null) {
					session.beginTransaction();
					try {
						PawsManager.INSTANCE.createDefaultSettings(session);
						session.getTransaction().commit();
					}catch (Exception ex) {
						PawsPlugIn.displayLog(Messages.SettingsPreferencePage_DefaultSettingsError, ex);
					}
				}
				
				service = QueryFactory.buildQuery(session, PawsService.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$

			}
			final PawsService fservice = service;
			Display.getDefault().asyncExec(()->{
				
				if (fservice != null) {
					txtServiceKey.setText(fservice.getApiKey());
					txtServiceHeatmapApi.setText(fservice.getPawsApiUrl());
					txtServiceTaskApi.setText(fservice.getTaskApiUrl());
				
					txtWorkspaceStorageUrl.setText(fservice.getStorageUrl()); 
					txtWorkspaceClientId.setText(fservice.getClientId()); 
					txtWorkspaceLoginURL.setText(fservice.getOAuthUrl());
				}
			});
			
			return Status.OK_STATUS;
		}
		
	};

}
