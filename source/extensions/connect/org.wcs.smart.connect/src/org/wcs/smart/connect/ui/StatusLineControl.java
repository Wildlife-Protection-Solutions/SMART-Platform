package org.wcs.smart.connect.ui;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.eclipse.ui.progress.WorkbenchJob;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;

public class StatusLineControl extends WorkbenchWindowControlContribution {

	private Label localStatus;
	private Label serverStatus;
	
	public StatusLineControl() {
		WorkbenchJob job = new WorkbenchJob("wait") {
			
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench()
						.getService(IEclipseContext.class);
				EModelService modelService = ctx.get(EModelService.class);
				
//				//this gets hidden by some visibility changes events that I don't have control over;
//				//so instead we ensure it is visible here.
				MTrimBar statusBar = (MTrimBar) modelService.find("org.eclipse.ui.trim.status", ctx.get(MApplication.class)); //$NON-NLS-1$
				
				MTrimElement toMove = null;
				for (MTrimElement trim : statusBar.getChildren()){
					if (trim.getElementId().equals("org.wcs.smart.connect.status")){
						toMove = trim;
					}
				}
				if (toMove != null){
					statusBar.getChildren().remove(toMove);
					statusBar.getChildren().add(toMove);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	

	@Override
	protected Control createControl(Composite parent) {
		
		Composite status = new Composite(parent, SWT.NONE);
		status.setLayout(new GridLayout(2, true));

		
		serverStatus = new Label(status, SWT.NONE);
		serverStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_ERROR_ICON));
		
		localStatus = new Label(status, SWT.NONE);
		localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_ERROR_ICON));
		
		
		updateLocalChanges.schedule();
		return status;
	}

	
	private Job updateLocalChanges = new Job("update local changes status"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String message = "Connect server not configured.";
			Boolean hasChanges = null;
			if (DerbyReplicationManager.INSTANCE.getLocalReplicationState()){
				Session session = HibernateManager.openSession();
				try{
					hasChanges = DerbyReplicationManager.INSTANCE.hasLocalChanges(session);
					if (hasChanges == null){
						message = "Error determining local changes state.";
					}else if (hasChanges){
						message = "There are local changes that need to be uploaded to server.";
					}else{
						message = "All local changes have been applied to the server.";
					}
				}finally{
					session.close();
				}
			}
			
			final String lmessage = message;
			final Boolean lhasChanges = hasChanges;
			
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
//					Shell active = Display.getDefault().getActiveShell();
//					if ((active.getStyle() & SWT.APPLICATION_MODAL) == SWT.APPLICATION_MODAL){
//						System.out.println("wait");
//					}
//					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "TEST", "TEST");
					if (lhasChanges == null){
						localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_ERROR_ICON));
					}else if (lhasChanges){
						localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_CHANGES_ICON));
					}else{
						localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_OK_ICON));
					}
					localStatus.setToolTipText(lmessage);
				}
				
				
			});
			//schedule every 30 seconds
			schedule(30 * 1000);
			return Status.OK_STATUS;
		}	
	};
}
