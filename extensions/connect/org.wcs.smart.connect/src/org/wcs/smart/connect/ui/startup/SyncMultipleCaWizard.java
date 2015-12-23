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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.server.replication.SyncChangesRunnable;
import org.wcs.smart.connect.server.replication.SyncHistoryManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.UserNamePasswordDialog;

/**
 * Wizard for syncing multiple conservation areas at once
 */
public class SyncMultipleCaWizard extends Wizard {

	protected LocalCaListPage page1 = null; 
	
	private List<String> errors;
	
	public SyncMultipleCaWizard(){
		super();
		setWindowTitle(Messages.SyncMultipleCaWizard_Title);
		addPage(page1 = new LocalCaListPage());
		super.setNeedsProgressMonitor(true);
	}
	
	@Override
	public boolean performFinish() {
		
		final Shell activeShell = Display.getDefault().getActiveShell();
		final List<ConservationArea> allCas = page1.getSelection();
		final String username = page1.getUsername();
		final String password = page1.getPassword();
		final int[] okCnt = new int[]{0};
		errors = new ArrayList<String>();
		
		if (allCas.isEmpty()) return true;
		
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.SyncMultipleCaWizard_SyncTaskName, allCas.size() + 1);
					try{
						monitor.subTask(Messages.SyncMultipleCaWizard_ValidateSubtaskName);
						
						List<CaUserDetails> details = validateCaUsers(activeShell, allCas, username, password);
						monitor.worked(1 + (allCas.size() - details.size()));
						
						HibernateManager.endSessionFactory(true);
						HibernateManager.setUserName(SmartDB.DbUser.ADMIN.getUserName(), SmartDB.DbUser.ADMIN.getPassword());
						
						try{
							for (CaUserDetails cainfo : details){
								if (monitor.isCanceled()){
									errors.add(Messages.SyncMultipleCaWizard_Cancelled);
									break;
								}
								monitor.subTask(cainfo.ca.getNameLabel());
								if (syncCa(cainfo, monitor)){
									okCnt[0] ++;
								}
							}
						}finally{
							HibernateManager.endSessionFactory(true);
							HibernateManager.setUserName(SmartDB.DbUser.LOGIN.getUserName(), SmartDB.DbUser.LOGIN.getPassword());	
						}
					}finally{
						monitor.done();
					}
					
				}
			});
		} catch (Exception e) {
			errors.add(MessageFormat.format(Messages.SyncMultipleCaWizard_CaError, e.getMessage()));
			ConnectPlugIn.log(e.getMessage(), e);
			return false;
		} 
		String message = Messages.SyncMultipleCaWizard_WarningsDialogMessage;
		errors.add(0, MessageFormat.format(Messages.SyncMultipleCaWizard_CompleteMessage, okCnt[0], allCas.size()));
		
		WarningDialog wd = new WarningDialog(activeShell, Messages.SyncMultipleCaWizard_WarningsDialogTitle, message, errors);
		wd.open();
		return true;
		
	}
	
	/**
	 * Returns true if completed okay, false if error.  Adds to error log.
	 *  
	 * @param details
	 * @param monitor
	 * @return
	 */
	private boolean syncCa(CaUserDetails details, IProgressMonitor monitor) {
		try {
			SmartConnect connect = SmartConnect.findInstance(details.server, 
					details.connectUser.getConnectUsername(),
					details.connectPassword);
			
			//first we need to error any active uploads or downloads so they don't cause
			SyncHistoryManager.INSTANCE.errorActiveDownloadRecords(SmartDB.getCurrentConservationArea());
			SyncHistoryManager.INSTANCE.errorActiveUploadRecords(SmartDB.getCurrentConservationArea());
			
			//then we can sync 
			SyncChangesRunnable runnable = new SyncChangesRunnable(connect, details.ca, true);
			runnable.run(new SubProgressMonitor(monitor, 1));
			
			String status = runnable.getStatus();
			if (status != null){
				errors.add(MessageFormat.format(Messages.SyncMultipleCaWizard_CaError1, details.ca.getNameLabel(), status));
				return false;
			}else{
				errors.add(MessageFormat.format(Messages.SyncMultipleCaWizard_CaComplete, details.ca.getNameLabel()));
				return true;
			}
			
		} catch (Exception e) {
			ConnectPlugIn.log(e.getMessage(), e);
			errors.add(MessageFormat.format(Messages.SyncMultipleCaWizard_CaError1, details.ca.getNameLabel(), e.getMessage()));
			return false;
		}
	}
	
	private List<CaUserDetails> validateCaUsers(Shell activeShell, List<ConservationArea> allCas, 
			String username, String password){

		
		final List<CaUserDetails> details = new ArrayList<CaUserDetails>();
		
		for (ConservationArea ca : allCas){
			//validate employee 
			Employee e = null;
			String desktopPass = password;
			try{
				e = HibernateManager.validateUser(username, password, ca);
			}catch (Exception ex){}
			if (e == null){
				while(true){
					//validate password
					final String[] values = new String[]{null, null};
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							UserNamePasswordDialog userPassDialog = new UserNamePasswordDialog(activeShell, 
									Messages.SyncMultipleCaWizard_UserpassDialogTitle,
									MessageFormat.format(Messages.SyncMultipleCaWizard_UserpassDialogMessage, ca.getNameLabel()),
									IDialogConstants.OK_LABEL,
									IDialogConstants.SKIP_LABEL);
							if (userPassDialog.open() == Window.OK){
								values[0] = userPassDialog.getUserName();
								values[1] = userPassDialog.getPassword();
							}
						}
					});
					if (values[0] == null) break;
					try{
						e = HibernateManager.validateUser(values[0], values[1], ca);
						desktopPass = values[1];
					}catch (Exception ex){}
					if (e != null) break;
				}
			}
			if (e == null){
				errors.add(MessageFormat.format(Messages.SyncMultipleCaWizard_SkippedDesktopInvalid, ca.getNameLabel()));
				continue;
			}
			
			ConnectServer server = null;
			ConnectUser cu = null;
			Session s = HibernateManager.openSession();
			String connectPassword = null;
			try{
				cu = ConnectHibernateManager.getConnectUser(e, s);
				server = ConnectHibernateManager.getConnectServer(s,ca);
			}finally{
				s.close();
			}
			if (server == null){
				errors.add(MessageFormat.format(Messages.SyncMultipleCaWizard_SkippedServerInvalid, ca.getNameLabel()));
				continue;
			}
			if (cu != null && cu.getConnectPassword() != null){
				try{
					connectPassword = ConnectPlugIn.decryptPassword(cu, desktopPass);
				}catch (Exception ex){
					SmartPlugIn.displayLog(ex.getMessage(), ex);
				}
			}
			if (connectPassword == null){
				cu = null;
				//validate password
				final String[] values = new String[]{null, null};
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						UserNamePasswordDialog userPassDialog = new UserNamePasswordDialog(activeShell, 
								Messages.SyncMultipleCaWizard_UserpassDialogTitle,
								MessageFormat.format(Messages.SyncMultipleCaWizard_ConnectUserpassDialogMessage, ca.getNameLabel()),
								IDialogConstants.OK_LABEL,
								IDialogConstants.SKIP_LABEL);
						if (userPassDialog.open() == Window.OK){
							values[0] = userPassDialog.getUserName();
							values[1] = userPassDialog.getPassword();
						}		
					}
					
				});
				if (values[0] == null){
					errors.add(MessageFormat.format(Messages.SyncMultipleCaWizard_SkippedServerAccountInvalid, ca.getNameLabel()));
					break;
				}
				cu = new ConnectUser();
				cu.setConnectUsername(values[0]);
				cu.setConnectPassword(values[1]);
				connectPassword = values[1];
			}
			if (cu == null) continue;
				
			details.add( new CaUserDetails(ca, server, cu, connectPassword) );
		}
		return details;
	}
	
	/*
	 * map ca to connect user details
	 */
	private class CaUserDetails{
		private ConservationArea ca;
		private ConnectUser connectUser;
		private ConnectServer server;
		private String connectPassword;
		
		public CaUserDetails(ConservationArea ca, ConnectServer server, ConnectUser cu, String connectPassword){
			this.ca = ca;
			this.server = server;
			this.connectUser = cu;
			this.connectPassword = connectPassword;
		}
	}
}
