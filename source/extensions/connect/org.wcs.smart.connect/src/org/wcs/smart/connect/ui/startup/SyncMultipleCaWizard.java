package org.wcs.smart.connect.ui.startup;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.server.replication.SyncChangesRunnable;
import org.wcs.smart.connect.server.replication.SyncHistoryManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.UserNamePasswordDialog;

public class SyncMultipleCaWizard extends Wizard {

	protected LocalCaListPage page1 = null; 
	protected UsernamePasswordWizardPage page2 = null;
	
	private List<String> errors;
	
	public SyncMultipleCaWizard(){
		super();
		setWindowTitle("Sync Multiple Conservation Areas");
		addPage(page1 = new LocalCaListPage());
		super.setNeedsProgressMonitor(true);
	}
	
	@Override
	public boolean performFinish() {
		
		final Shell activeShell = Display.getDefault().getActiveShell();
		final List<ConservationArea> allCas = page1.getSelection();
		final String username = page1.getUsername();
		final String password = page1.getPassword();
		
		errors = new ArrayList<String>();
		
		if (allCas.isEmpty()) return true;
		
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("Syncing conservation areas", allCas.size() + 1);
					try{
						monitor.subTask("Validating username / passwords");
						
						List<CaUserDetails> details = validateCaUsers(activeShell, allCas, username, password);
						monitor.worked(1 + (allCas.size() - details.size()));
						
						HibernateManager.endSessionFactory(true);
						HibernateManager.setUserName(SmartDB.DbUser.ADMIN.getUserName(), SmartDB.DbUser.ADMIN.getPassword());
						try{
							for (CaUserDetails cainfo : details){
								monitor.subTask(cainfo.ca.getNameLabel());
								syncCa(cainfo, monitor);
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
			errors.add(MessageFormat.format("Error Processing Conservation Areas - {0}.", e.getMessage()));
			ConnectPlugIn.log(e.getMessage(), e);
			return false;
		} 
		if (errors.size() > 0){
			WarningDialog wd = new WarningDialog(activeShell, "Sync Warnings", "The following errors were generated while replicating Conservation Area data", errors);
			wd.open();
		}else{
			MessageDialog.openInformation(activeShell, "Complete", "Sync Complete. {0} Conservation areas sync'd.");
		}
		return false;
		
	}
	
	private void syncCa(CaUserDetails details, IProgressMonitor monitor) {
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
				errors.add(MessageFormat.format("{0} - ERROR - {1}.", details.ca.getNameLabel(), status));	
			}else{
				errors.add(MessageFormat.format("{0} - COMPLETE.", details.ca.getNameLabel()));
			}
			
		} catch (Exception e) {
			ConnectPlugIn.log(e.getMessage(), e);
			errors.add(MessageFormat.format("{0} - ERROR - {1}.", details.ca.getNameLabel(), e.getMessage()));
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
									"Username/Password",
									MessageFormat.format("Invalid desktop username/password for Conservation Area {0}.  Re-enter username/password or skip", ca.getNameLabel()),
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
				errors.add(MessageFormat.format("{0} - SKIPPED - Valid desktop account not provided.", ca.getNameLabel()));
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
				errors.add(MessageFormat.format("{0} - SKIPPED - No connect server found.", ca.getNameLabel()));
				continue;
			}
			if (cu != null && cu.getConnectPassword() != null){
				try{
					connectPassword = ConnectPlugIn.decryptPassword(cu, desktopPass);
				}catch (Exception ex){
					//TODO:
					ex.printStackTrace();
				}
			}
			if (connectPassword == null){
				//TODO: get connect username/password for ca
				cu = null;
				//validate password
				final String[] values = new String[]{null, null};
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						UserNamePasswordDialog userPassDialog = new UserNamePasswordDialog(activeShell, 
								"Username/Password",
								MessageFormat.format("Invalid SMART Connect username/password for Conservation Area {0}.  Re-enter username/password or skip", ca.getNameLabel()),
								IDialogConstants.OK_LABEL,
								IDialogConstants.SKIP_LABEL);
						if (userPassDialog.OK == Window.OK){
							values[0] = userPassDialog.getUserName();
							values[1] = userPassDialog.getPassword();
						}		
					}
					
				});
				if (values[0] == null){
					errors.add(MessageFormat.format("{0} - SKIPPED - No valid Connect user account found.", ca.getNameLabel()));
					break;
				}
				cu = new ConnectUser();
				cu.setConnectUsername(values[0]);
				cu.setConnectPassword(values[1]);
				connectPassword = values[1];
				
				//TODO: validate
			}
			if (cu == null) continue;
				
			details.add( new CaUserDetails(ca, server, e, cu, connectPassword) );
		}
		return details;
	}
	private class CaUserDetails{
		private ConservationArea ca;
		private Employee desktopUser;
		private ConnectUser connectUser;
		private ConnectServer server;
		private String connectPassword;
		
		public CaUserDetails(ConservationArea ca, ConnectServer server, Employee desktop, ConnectUser cu, String connectPassword){
			this.ca = ca;
			this.desktopUser = desktop;
			this.server = server;
			this.connectUser = cu;
			this.connectPassword = connectPassword;
		}
	}
}
