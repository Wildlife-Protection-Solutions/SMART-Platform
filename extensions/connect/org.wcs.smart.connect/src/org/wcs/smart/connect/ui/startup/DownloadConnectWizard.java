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
package org.wcs.smart.connect.ui.startup;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.server.DownloadCaEngine;
import org.wcs.smart.connect.ui.server.configure.ConnectServerWizard;
import org.wcs.smart.connect.ui.server.configure.ServerOptionsPanel;
import org.wcs.smart.connect.ui.server.configure.ServerOptionsWizardPage;
import org.wcs.smart.connect.ui.server.configure.ServerWizardPage;
import org.wcs.smart.connect.ui.server.configure.UserWizardPage;

/**
 * Import Conservation Area from connect wizard.
 * 
 * @author Emily
 *
 */
public class DownloadConnectWizard extends ConnectServerWizard implements IPageChangingListener{

	protected ConnectCaListPage page5;
	
	public DownloadConnectWizard(){
		super(false);
		setWindowTitle(Messages.DownloadConnectWizard_Name);
		addPage(page5 = new ConnectCaListPage());
		setNeedsProgressMonitor(true);
		
	}
	@Override
    public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		super.addPages();
	}
	
	public ConnectServer createServer(){
		String url = ((ServerWizardPage)getPage(ServerWizardPage.NAME)).getServerName();
		String certificateFile = ((ServerWizardPage)getPage(ServerWizardPage.NAME)).getCertificateFile();
				
		ConnectServer server = new ConnectServer(){
			public Path getLocalCertificateFile(){
				return Paths.get(getCertificateFileName());
			}
		};
		server.setServerUrl(url);
		server.setOptions(new HashMap<String, ConnectServerOption>());
		((ServerOptionsWizardPage)getPage(ServerOptionsPanel.class.getCanonicalName())).updateServer(server);
		if (!certificateFile.trim().isEmpty()){
			server.setCertificateFileName(certificateFile);
		}
		
		return server;
	}
	
	@Override
	public boolean performFinish() {
		ConservationAreaProxy info = page5.getSelection();

		if (info == null) return false;
	
		ConnectServer server = createServer();
		String user = ((UserWizardPage)getPage(UserWizardPage.NAME)).getUsername();
		String pass = ((UserWizardPage)getPage(UserWizardPage.NAME)).getPassword();
		
		SmartConnect connect = SmartConnect.findInstance(server, user, pass);
		final DownloadCaEngine installer = new DownloadCaEngine(info, connect);
		if (!installer.preDownload(getShell())){
			return false;
		}
		final List<Exception> errors = new ArrayList<Exception>();
		try{
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try{
						if (!installer.downloadImport(monitor)){
							errors.add(new Exception(Messages.DownloadConnectWizard_Cancelled));
						}
						
					}catch (Exception ex){
						errors.add(ex);
					}
				}
			});
		}catch (Exception ex){
			errors.add(ex);
		}
		if (errors.isEmpty()){
			return true;
		}
		ConnectPlugIn.displayLog(Messages.DownloadConnectWizard_DownloadError + "\n\n" + errors.get(0).getMessage(), errors.get(0)); //$NON-NLS-1$
		
		return false;
	}

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == page4 && event.getTargetPage() == page5){
			final ConnectServer temp = createServer();
			final String user = ((UserWizardPage)getPage(UserWizardPage.NAME)).getUsername();
			final String pass = ((UserWizardPage)getPage(UserWizardPage.NAME)).getPassword();
			
			try{
				getContainer().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask(Messages.DownloadConnectWizard_LoadingTaskName, 2);
						monitor.worked(1);
						page5.initList(temp, user, pass);
						monitor.done();
					}
				});
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
			
		}
		if (event.getCurrentPage() == page5 && event.getTargetPage() == page4){
			page5.clearList();
		}
	}
}
