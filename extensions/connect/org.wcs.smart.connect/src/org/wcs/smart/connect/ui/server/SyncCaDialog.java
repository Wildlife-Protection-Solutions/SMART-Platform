package org.wcs.smart.connect.ui.server;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.replication.changelog.UploadSyncEngine;

public class SyncCaDialog extends ConnectDialog {

	
	public SyncCaDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Upload local changes to CONNECT");
		getShell().setText("Upload local change CONNECT");
		setMessage("Upload local changes to a connect instance.");
		
		return super.createDialogArea(parent);
	}
	
	protected void onComplete(final SmartConnect connect){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					UploadSyncEngine engine = new UploadSyncEngine(connect);
					
					try{
						engine.syncUpload(monitor);
					}catch (Exception ex){
						ConnectPlugIn.displayLog(ex.getMessage(), ex);
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			ConnectPlugIn.displayLog(e.getMessage(), e);
			return;
		}
	}
}
