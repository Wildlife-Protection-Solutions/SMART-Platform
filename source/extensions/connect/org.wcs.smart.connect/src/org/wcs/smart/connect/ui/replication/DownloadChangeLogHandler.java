package org.wcs.smart.connect.ui.replication;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.wcs.smart.connect.replication.changelog.DerbyChangeLogDeserializer;

public class DownloadChangeLogHandler {

	@Execute
	public void execute() {
		
		return ;
	}


	public static class DownloadChangeLogHandlerWrapper extends DIHandler<DownloadChangeLogHandler>{
		public DownloadChangeLogHandlerWrapper() {
			super(DownloadChangeLogHandler.class);
		}
		
	}
}
