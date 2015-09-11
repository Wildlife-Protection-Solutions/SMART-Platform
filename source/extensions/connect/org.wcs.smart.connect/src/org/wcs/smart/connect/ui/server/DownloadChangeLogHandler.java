package org.wcs.smart.connect.ui.server;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.replication.changelog.DerbyChangeLogDeserializer;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartHibernateManager;

public class DownloadChangeLogHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
		DownloadChangeLogDialog dialog = new DownloadChangeLogDialog(activeShell);
		dialog.open();
	}


	public static class DownloadChangeLogHandlerWrapper extends DIHandler<DownloadChangeLogHandler>{
		public DownloadChangeLogHandlerWrapper() {
			super(DownloadChangeLogHandler.class);
		}
		
	}
}
