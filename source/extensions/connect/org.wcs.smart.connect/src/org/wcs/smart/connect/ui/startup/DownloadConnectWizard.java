package org.wcs.smart.connect.ui.startup;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.server.DownloadCaEngine;
import org.wcs.smart.connect.ui.server.configure.ConnectServerWizard;
import org.wcs.smart.connect.ui.server.configure.ServerWizardPage;
import org.wcs.smart.connect.ui.server.configure.UserWizardPage;

public class DownloadConnectWizard extends ConnectServerWizard implements IPageChangingListener{

	protected CaListPage page4;
	
	public DownloadConnectWizard(){
		super();
		setWindowTitle("Download from SMART Connect Server");
		addPage(page4 = new CaListPage());
		setNeedsProgressMonitor(true);
		
	}
	@Override
    public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		super.addPages();
	}
	
	@Override
	public boolean performFinish() {
		ConservationAreaInfo info = page4.getSelection();

		if (info == null) return false;
		
		String url = ((ServerWizardPage)getPage(ServerWizardPage.NAME)).getServerName();
		String user = ((UserWizardPage)getPage(UserWizardPage.NAME)).getUsername();
		String pass = ((UserWizardPage)getPage(UserWizardPage.NAME)).getPassword();
		
		ConnectServer server = new ConnectServer();
		server.setServerUrl(url);
		try(SmartConnect connect = new SmartConnect(server, user, pass)){
		
			final DownloadCaEngine installer = new DownloadCaEngine(info, connect);
			installer.preDownload(getShell());
		
			final List<Exception> errors = new ArrayList<Exception>();
			try{
				getContainer().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						try{
							if (!installer.downloadImport(monitor)){
								errors.add(new Exception("Process cancelled by user."));
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
			ConnectPlugIn.displayLog("Error downloading and importing conservation area." + "\n\n" + errors.get(0).getMessage(), errors.get(0));
		}
		return false;
	}

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == page3 && event.getTargetPage() == page4){
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
						page4.initList(url, user, pass);
						monitor.done();
					}
				});
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
			
		}
		if (event.getCurrentPage() == page4 && event.getTargetPage() == page3){
			page4.clearList();
		}
	}
}
