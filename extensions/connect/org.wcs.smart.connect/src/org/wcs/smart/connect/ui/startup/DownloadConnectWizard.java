package org.wcs.smart.connect.ui.startup;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.ui.server.configure.ConnectServerWizard;
import org.wcs.smart.connect.ui.server.configure.ServerWizardPage;
import org.wcs.smart.connect.ui.server.configure.UserWizardPage;
import org.wcs.smart.hibernate.HibernateManager;

public class DownloadConnectWizard extends ConnectServerWizard implements IPageChangingListener{

	protected CaListPage page3;
	
	public DownloadConnectWizard(){
		super();
		setWindowTitle("Download from SMART Connect Server");
		addPage(page3 = new CaListPage());
		setNeedsProgressMonitor(true);
		
	}
	@Override
    public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		super.addPages();
	}
	@Override
	public boolean performFinish() {
		
		ConservationAreaInfo info = page3.getSelection();
		
		
		if (info == null) return false;
		
		String url = ((ServerWizardPage)getPage(ServerWizardPage.NAME)).getServerName();
		String user = ((UserWizardPage)getPage(UserWizardPage.NAME)).getUsername();
		String pass = ((UserWizardPage)getPage(UserWizardPage.NAME)).getPassword();
		
		ConnectServer server = new ConnectServer();
		server.setServerUrl(url);
		try(SmartConnect connect = new SmartConnect(server, user, pass)){
		
			DownloadInstallEngine installer = new DownloadInstallEngine(info, connect);
			installer.preDownload(getShell());
		
			try{
				installer.download();
			}catch (Exception ex){
				SmartPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		return false;
	}

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == page2 && event.getTargetPage() == page3){
			final String url = ((ServerWizardPage)getPage(ServerWizardPage.NAME)).getServerName();
			final String user = ((UserWizardPage)getPage(UserWizardPage.NAME)).getUsername();
			final String pass = ((UserWizardPage)getPage(UserWizardPage.NAME)).getPassword();

			
			try{
				getContainer().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask("Loading Conservation Areas", 2);
						monitor.worked(1);
						page3.initList(url, user, pass);
						monitor.done();
					}
				});
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
			
		}
		if (event.getCurrentPage() == page3 && event.getTargetPage() == page2){
			page3.clearList();
		}
	}
}
