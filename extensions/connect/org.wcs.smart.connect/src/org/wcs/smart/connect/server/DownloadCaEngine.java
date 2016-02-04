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
package org.wcs.smart.connect.server;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.ca.in.CaImporter;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.UserNamePasswordDialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Engine for downloading and installing a Conservation Area.
 * 
 * This works in the display thread; other tasks cannot be completed
 * while this task is working.
 * 
 * @author Emily
 *
 */
public class DownloadCaEngine {
	
	private ConservationAreaProxy info;
	private SmartConnect connect;
	
	public DownloadCaEngine(ConservationAreaProxy info, SmartConnect connect){
		this.info = info;
		this.connect = connect;
	}
	
	/**
	 * Downloads the Conservation Area export package and imports it.
	 * 
	 * @param monitor
	 * @return true if download and install completed. false if user cancelled
	 * @throws Exception
	 */
	public boolean downloadImport(IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.DownloadCaEngine_TaskName, 4);
		
		/* request ca */
		monitor.subTask(Messages.DownloadCaEngine_InitSubtaskName);
		String statusUrl = connect.startConservationAreaDownload(info.getUuid());
		monitor.worked(1);
		if (monitor.isCanceled()) return false;
		
		/* wait for ca export to be created */
		monitor.subTask(Messages.DownloadCaEngine_WaitSubTaskName);
		Long start = System.nanoTime();
		WorkItemStatus status = null ;
		int waitTime = ConnectServerOption.ConnectionOption.RETY_WAIT_TIME.getIntegerValue(connect.getServer());
		while(status == null || status.getStatus() == WorkItemStatus.Status.PROCESSING){
			Long current = System.nanoTime();
			
			if ( (current - start) > ConnectServerOption.ConnectionOption.MAX_PROCESSING_WAIT_TIME.getIntegerValue(connect.getServer()) * 1000000l) throw new Exception(Messages.DownloadCaEngine_Timeout);
			Thread.sleep(waitTime);
			try{
				status = connect.getWorkItemStatus(statusUrl);
			}catch (Exception ex){
				ConnectPlugIn.log("Error requesting ca download status.", ex); //$NON-NLS-1$
			}
			if (monitor.isCanceled()) return false;
		}
		monitor.worked(1);
		
		if (status.getStatus() == WorkItemStatus.Status.ERROR){
			throw new Exception(Messages.DownloadCaEngine_CaDownloadError + SmartConnect.parseErrorMessage(status.getMessage()));
		}

		/* download file */
		monitor.subTask(Messages.DownloadCaEngine_DownloadSubtaskName);
		String message = status.getMessage();
		JsonNode nd = (new ObjectMapper()).readTree(message);
		String downloadUrl = nd.get("file_url").asText(); //$NON-NLS-1$
		if (monitor.isCanceled()) return false;
		Path p = connect.downloadFileFromUrl(downloadUrl, null, new SubProgressMonitor(monitor, 1));
		
		/* import file */
		monitor.subTask(Messages.DownloadCaEngine_InstallSubtaskName);
		try{
			if (monitor.isCanceled()) return false;
			CaImporter.importCa(p.toFile(), new SubProgressMonitor(monitor, 1));
		}finally{
			Files.delete(p);
		}
		
		monitor.done();
		return true;
	}
	
	/**
	 * To be executed before downloading and importing
	 * Conservation Area engine. This ensures the Conservation Area
	 * does not already exist on the desktop and gives the user
	 * the options of deleting if it does exist
	 * 
	 * @param activeShell
	 * @return true if download install process should continue, false if we should stop
	 */
	public boolean preDownload(final Shell activeShell){
		ConservationArea desktopCa;
		Employee smartUser= null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			
			desktopCa = (ConservationArea)s.get(ConservationArea.class, info.getUuid());
			if (desktopCa != null){
				if (!MessageDialog.openQuestion(activeShell, Messages.DownloadCaEngine_ImportCaDialogTitle, 
						MessageFormat.format(Messages.DownloadCaEngine_ImportCaDialogMessage, desktopCa.getNameLabel()))){
					//user does not want to override
					return false;
				}
			}
			
			int cnt = 0;
			while (desktopCa != null && cnt < 3){
				UserNamePasswordDialog dialog = new UserNamePasswordDialog(activeShell,
						Messages.DownloadCaEngine_DeleteConfirmTitle,
						Messages.DownloadCaEngine_DeleteConfirmMessage,
						Messages.DownloadCaEngine_DeleteConfirmButtonLabel);
				if (dialog.open() == Window.CANCEL){
					return false;
				}
				
				String userName = dialog.getUserName();
				String password = dialog.getPassword();
				
				smartUser = (Employee)s.createCriteria(Employee.class)
						.add(Restrictions.eq("conservationArea", desktopCa)) //$NON-NLS-1$
						.add(Restrictions.eq("smartUserId", userName)) //$NON-NLS-1$
						.uniqueResult();
				if (smartUser != null &&  HibernateManager.validatePassword(password, smartUser)){
					break;
				}else if (smartUser != null && smartUser.getSmartUserLevel() != SmartUserLevel.ADMIN){
					MessageDialog.openError(activeShell, Messages.DownloadCaEngine_ErrorDialogTitle, Messages.DownloadCaEngine_InvalidPermission);
					return false;
				}
				MessageDialog.openError(activeShell, Messages.DownloadCaEngine_ErrorDialogTitle, Messages.DownloadCaEngine_InvalidUser);
				cnt++;
			}
			if (cnt >= 3){
				//no valid username/password was entered
				return false;
			}
			s.getTransaction().rollback();
		}finally{
			s.close();
		}
		
		if (desktopCa != null){
			//need to login with user 
			//delete ca
			if (smartUser == null){
				return false;
			}

			if (!deleteCa(desktopCa, activeShell)){
				return false;
			}
		}
		
		//ensure ca is removed
		return validateCaDeleted();
	}

	public boolean validateCaDeleted(){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			
			ConservationArea desktopCa = (ConservationArea)s.get(ConservationArea.class, info.getUuid());
			if (desktopCa != null){
				//at some point something went wrong
				return false;
			}
			s.getTransaction().rollback();
		}finally{
			s.close();
		}
		return true;
	}
	public boolean deleteCa(final ConservationArea ca, final Shell activeShell){
		//we don't revert back here; that will be done after download complete
		HibernateManager.endSessionFactory(true);
		HibernateManager.setUserName(SmartDB.DbUser.ADMIN.getUserName(), SmartDB.DbUser.ADMIN.getPassword());
		
		//delete ca
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		final boolean[] cont = new boolean[]{true};
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					DisplayAccess.accessDisplayDuringStartup();
					monitor.setTaskName(Messages.DownloadCaEngine_DeleteTaskName);
					try{
						ConservationAreaManager.getInstance().deleteConservationArea(ca, monitor, false);
					}catch (final Exception ex){
						cont[0] = false;
						SmartPlugIn.displayLog(Messages.DownloadCaEngine_CaDataError, ex);	
					}		
				}
			});
		}catch (Exception ex){
			SmartPlugIn.displayLog( Messages.DownloadCaEngine_CaDataError, ex);
			return false;
		}
		if (!cont[0]) return false;
		return true;
	}
}
