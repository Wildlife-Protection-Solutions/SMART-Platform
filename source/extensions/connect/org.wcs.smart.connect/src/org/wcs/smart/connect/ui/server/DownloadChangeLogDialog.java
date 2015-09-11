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
import org.wcs.smart.connect.replication.changelog.DownloadChangeLogEngine;
import org.wcs.smart.connect.replication.changelog.UploadChangeLogEngine;

public class DownloadChangeLogDialog extends ConnectDialog {

	
	public DownloadChangeLogDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Download changes from CONNECT");
		getShell().setText("Download local changes FROM CONNECT");
		setMessage("Download changes from a CONNECT instance to your local database.");
		
		return super.createDialogArea(parent);
	}
	
	protected void onComplete(final SmartConnect connect){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					DownloadChangeLogEngine engine = new DownloadChangeLogEngine(connect);
					try{
						engine.download(monitor);
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
