package org.wcs.smart.connect.ui.replication;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.ui.server.SyncCaDialog;

public class UploadChangeLogHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
		SyncCaDialog dialog = new SyncCaDialog(activeShell);
		dialog.open();
	}


	public static class UploadChangeLogHandlerWrapper extends DIHandler<UploadChangeLogHandler>{
		public UploadChangeLogHandlerWrapper() {
			super(UploadChangeLogHandler.class);
		}
		
	}
}
