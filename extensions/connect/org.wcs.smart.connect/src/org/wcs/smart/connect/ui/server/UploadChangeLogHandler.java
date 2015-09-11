package org.wcs.smart.connect.ui.server;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

public class UploadChangeLogHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
		UploadChangeLogDialog dialog = new UploadChangeLogDialog(activeShell);
		dialog.open();
	}


	public static class UploadChangeLogHandlerWrapper extends DIHandler<UploadChangeLogHandler>{
		public UploadChangeLogHandlerWrapper() {
			super(UploadChangeLogHandler.class);
		}
		
	}
}
