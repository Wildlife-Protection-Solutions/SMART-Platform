package org.wcs.smart.connect.ui.replication;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.wcs.smart.connect.replication.changelog.ChangeLogApplier;
import org.wcs.smart.connect.replication.changelog.ChangeLogPackager;

public class DownloadChangeLogHandler {

	@Execute
	public void execute() {
		try{
			ChangeLogApplier.applyChangeLog();
		}catch (Exception ex){
			ex.printStackTrace();
		}
		return ;
	}


	public static class DownloadChangeLogHandlerWrapper extends DIHandler<DownloadChangeLogHandler>{
		public DownloadChangeLogHandlerWrapper() {
			super(DownloadChangeLogHandler.class);
		}
		
	}
}
